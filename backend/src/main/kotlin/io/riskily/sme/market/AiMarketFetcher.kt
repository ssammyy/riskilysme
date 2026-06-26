package io.riskily.sme.market

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import io.riskily.sme.cbk.CbkRate
import io.riskily.sme.cbk.CbkRateRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

data class MarketPrices(
    val usdkes: Double,
    val fuel: Double,
    val cbkRate: Double,
    val unga: Double,
)

/**
 * Fetches live Kenya market prices via Claude API (web_search tool) and persists them to
 * cbk_rates — the single source of truth for market rates consumed by [MarketDataRefreshJob].
 *
 * When AI is unavailable (disabled flag, missing key, or network failure) the method returns
 * null silently; [MarketDataRefreshJob] then falls back to the last admin-entered cbk_rates rows.
 */
@Component
class AiMarketFetcher(
    private val cbkRates: CbkRateRepository,
    private val mapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${riskily.ai.claude-api-key:}")
    private lateinit var apiKey: String

    @Value("\${riskily.ai.model:claude-haiku-4-5-20251001}")
    private lateinit var model: String

    @Value("\${riskily.ai.enabled:true}")
    private var enabled: Boolean = true

    private val client: RestClient = RestClient.builder()
        .baseUrl("https://api.anthropic.com")
        .build()

    fun fetchAndStore(): MarketPrices? {
        if (!enabled || apiKey.isBlank()) {
            log.info("AiMarketFetcher: skipping — AI disabled or API key not set")
            return null
        }
        val prices = callClaude() ?: return null
        persistToCbkRates(prices)
        log.info(
            "AiMarketFetcher: cbk_rates updated — USD/KES={} fuel={} CBR={} unga={}",
            prices.usdkes, prices.fuel, prices.cbkRate, prices.unga,
        )
        return prices
    }

    // ─── Persist to cbk_rates (single source of truth) ───────────────────────

    private fun persistToCbkRates(prices: MarketPrices) {
        val today = LocalDate.now()
        listOf(
            "USD_KES"  to prices.usdkes,
            "FUEL_KES" to prices.fuel,
            "CBR"      to prices.cbkRate,
            "UNGA_KES" to prices.unga,
        ).forEach { (type, value) ->
            cbkRates.save(
                CbkRate(
                    rateType      = type,
                    rateValue     = BigDecimal(value).setScale(4, RoundingMode.HALF_UP),
                    effectiveDate = today,
                    setBy         = "ai-refresh",
                )
            )
        }
    }

    // ─── Claude API call ─────────────────────────────────────────────────────

    private fun callClaude(): MarketPrices? {
        val prompt = """
Search the web for today's Kenya financial market data.
Return ONLY this JSON object — no markdown, no explanation, nothing else:
{"usdkes": <number>, "fuel": <number>, "cbkRate": <number>, "unga": <number>}

Definitions:
- usdkes  : USD to KES buying rate from CBK or a major Kenyan bank today
- fuel    : Kenya Super Petrol EPRA regulated price in KES per litre
- cbkRate : CBK Central Bank Rate (Base Lending Rate) as a plain percentage (e.g. 12.5)
- unga    : current retail price of 1 kg Unga (Jogoo maize flour) in Nairobi, KES

All four fields are required and must be JSON numbers.
""".trimIndent()

        return try {
            val body = mapOf(
                "model"      to model,
                "max_tokens" to 300,
                "tools"      to listOf(
                    mapOf("type" to "web_search_20250305", "name" to "web_search", "max_uses" to 3),
                ),
                "messages" to listOf(mapOf("role" to "user", "content" to prompt)),
            )

            val response = client.post()
                .uri("/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("anthropic-beta", "web-search-2025-03-05")
                .header("content-type", "application/json")
                .body(body)
                .retrieve()
                .body(ClaudeResponse::class.java)

            val text = response?.content
                ?.filter { it.type == "text" }
                ?.lastOrNull()
                ?.text ?: return null

            parseMarketPrices(text)
        } catch (ex: Exception) {
            log.error("AiMarketFetcher: Claude API call failed", ex)
            null
        }
    }

    // ─── Parsing ─────────────────────────────────────────────────────────────

    private fun parseMarketPrices(raw: String): MarketPrices? {
        val start = raw.indexOf('{')
        val end   = raw.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) {
            log.warn("AiMarketFetcher: no JSON object found in response")
            return null
        }
        return try {
            val dto = mapper.readValue(raw.substring(start, end + 1), MarketPricesDto::class.java)
            if (dto.usdkes == null || dto.fuel == null || dto.cbkRate == null || dto.unga == null) {
                log.warn("AiMarketFetcher: incomplete market prices in response: {}", raw)
                null
            } else {
                MarketPrices(usdkes = dto.usdkes, fuel = dto.fuel, cbkRate = dto.cbkRate, unga = dto.unga)
            }
        } catch (ex: Exception) {
            log.warn("AiMarketFetcher: failed to parse market prices — {}", ex.message)
            null
        }
    }

    // ─── Internal DTOs ───────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class ClaudeResponse(val content: List<ContentBlock>?)

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class ContentBlock(val type: String?, val text: String?)

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class MarketPricesDto(
        val usdkes: Double? = null,
        val fuel: Double? = null,
        val cbkRate: Double? = null,
        val unga: Double? = null,
    )
}
