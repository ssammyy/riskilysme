package io.riskily.sme.insight

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import io.riskily.sme.scoring.MarketSnapshot
import io.riskily.sme.scoring.ScoreResponse
import io.riskily.sme.user.User
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Service
class InsightService(
    private val insightRepo: InsightRepository,
    private val emailService: InsightEmailService,
    private val mapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val nairobi = ZoneId.of("Africa/Nairobi")

    @Value("\${riskily.ai.claude-api-key:}")
    private lateinit var apiKey: String

    @Value("\${riskily.ai.model:claude-haiku-4-5-20251001}")
    private lateinit var model: String

    @Value("\${riskily.ai.enabled:true}")
    private var enabled: Boolean = true

    private val client: RestClient = RestClient.builder()
        .baseUrl("https://api.anthropic.com")
        .build()

    fun generateForUser(user: User, score: ScoreResponse, snapshot: MarketSnapshot?) {
        val userId = user.id ?: return
        if (!enabled || apiKey.isBlank()) {
            log.warn("InsightService: skipping user {} — AI disabled or API key not set", userId)
            return
        }

        // Dedup: skip if we already generated insights today (EAT).
        val todayStart = Instant.now().atZone(nairobi).toLocalDate()
            .atStartOfDay(nairobi).toInstant()
        val todayEnd = todayStart.plus(1, ChronoUnit.DAYS)
        if (insightRepo.existsByUserIdAndGeneratedAtBetween(userId, todayStart, todayEnd)) {
            log.info("InsightService: insights already exist for user {} today", userId)
            return
        }

        val prompt = buildPrompt(user, score, snapshot)
        val rawJson = callClaude(prompt) ?: return

        val candidates = parseInsights(rawJson)
        if (candidates.isEmpty()) {
            log.warn("InsightService: Claude returned 0 parseable insights for user {}", userId)
            return
        }

        val saved = candidates.take(3).map { dto ->
            insightRepo.save(
                Insight(
                    userId     = userId,
                    title      = dto.title.take(200),
                    body       = dto.body,
                    actionText = dto.actionText,
                    moduleCode = dto.moduleCode?.takeIf { it != "GENERAL" }?.take(20),
                )
            )
        }
        log.info("InsightService: saved {} insight(s) for user {} (tier={})", saved.size, userId, user.subscriptionTier)
        emailService.sendDailyDigest(user, saved)
    }

    // ─── Prompt ──────────────────────────────────────────────────────────────

    private fun buildPrompt(user: User, score: ScoreResponse, snapshot: MarketSnapshot?): String {
        val name = user.businessName ?: user.firstName ?: "this business"
        val lang = if (user.language.name == "SW") "Swahili" else "English"

        val worstModules = score.modules.sortedBy { it.health }.take(2)
            .joinToString(", ") { "${moduleDisplayName(it.code)}: ${it.health}/100" }

        val marketLine = if (snapshot != null)
            "USD/KES ${snapshot.usdkesSpot}, Petrol KES ${snapshot.fuelPrice}/L, " +
            "CBK Rate ${snapshot.cbkRate}%, KRA deadline in ${snapshot.nextKraDeadlineDays} days, " +
            "active regulatory circulars: ${snapshot.activeCircularsCount}"
        else "Market data not yet available"

        return """
You are a financial risk advisor for small Kenyan businesses.

Business profile:
- Name: $name
- Buys in foreign currency / dollar-linked costs: ${yesNo(user.q1FxYes)}
- Has active bank loans or overdrafts: ${yesNo(user.q2LoansYes)}${if (user.q2LoansYes == true) " at ${user.q2bInterestRate ?: "unknown"} interest" else ""}
- Sells on credit (invoices >30 days): ${yesNo(user.q3CreditSalesYes)}
- High fuel / electricity / rent costs: ${yesNo(user.q4FixedCostsYes)}
- One customer or supplier ≥40% of revenue: ${yesNo(user.q5ConcentrationYes)}
- Has experienced cash timing gaps: ${yesNo(user.q6CashTimingYes)}
- Relies on 1-2 key suppliers: ${yesNo(user.q7SupplierDepYes)}
- Uses mobile loans or informal credit: ${yesNo(user.q8InformalCreditYes)}

Today's Business Health Score (100 = healthiest):
Overall: ${score.overallHealth}/100 — ${score.overallBand.replace("_", " ")}
Two weakest modules: $worstModules

Market today: $marketLine

Use the web_search tool to find 1-2 relevant Kenya business news items published in the last 7 days.
Focus on: CBK/MPC decisions, KRA announcements, fuel or food price changes, inflation data, NSSF/NHIF updates, or any sector-specific Kenya SME news.

Then write EXACTLY 3 insights in $lang for the owner of $name.

Rules:
- Each insight must reference this specific business's profile or scores — not generic advice
- Mention real figures where possible (e.g. actual USD/KES rate, actual fuel price, actual CBK rate)
- The actionText must be ONE concrete action they can take THIS WEEK
- Tone: direct, plain language, like a trusted advisor — not formal or corporate

Return ONLY a valid JSON array with no markdown fences:
[
  {
    "title": "Short title under 80 characters",
    "body": "2-3 sentence explanation specific to this business",
    "moduleCode": "FX|LIQUIDITY|COMMODITY|CREDIT|REGULATORY|COUNTERPARTY|MACRO|GENERAL",
    "actionText": "One concrete action sentence"
  }
]
""".trimIndent()
    }

    private fun yesNo(value: Boolean?) = when (value) { true -> "Yes"; false -> "No"; null -> "Not answered" }

    private fun moduleDisplayName(code: String) = when (code.uppercase()) {
        "FX"           -> "Foreign Exchange"
        "LIQUIDITY"    -> "Cash Flow"
        "COUNTERPARTY" -> "Customers & Suppliers"
        "COMMODITY"    -> "Commodity Prices"
        "CREDIT"       -> "Credit & Loans"
        "REGULATORY"   -> "Compliance"
        "MACRO"        -> "Economic Conditions"
        else           -> code
    }

    // ─── Claude API call ─────────────────────────────────────────────────────

    private fun callClaude(prompt: String): String? {
        return try {
            val requestBody = mapOf(
                "model"      to model,
                "max_tokens" to 1500,
                "tools"      to listOf(
                    mapOf(
                        "type"     to "web_search_20250305",
                        "name"     to "web_search",
                        "max_uses" to 3,
                    )
                ),
                "messages" to listOf(
                    mapOf("role" to "user", "content" to prompt)
                ),
            )

            val response = client.post()
                .uri("/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("anthropic-beta", "web-search-2025-03-05")
                .header("content-type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(ClaudeResponse::class.java)

            // Extract the final text block from the response content array.
            response?.content
                ?.filter { it.type == "text" }
                ?.lastOrNull()
                ?.text
        } catch (ex: Exception) {
            log.error("InsightService: Claude API call failed", ex)
            null
        }
    }

    // ─── Response parsing ────────────────────────────────────────────────────

    private fun parseInsights(raw: String): List<InsightDto> {
        // Claude may return JSON embedded in prose. Extract the JSON array.
        val start = raw.indexOf('[')
        val end   = raw.lastIndexOf(']')
        if (start == -1 || end == -1 || end <= start) return emptyList()
        return try {
            val jsonArray = raw.substring(start, end + 1)
            mapper.readValue(jsonArray, Array<InsightDto>::class.java).toList()
        } catch (ex: Exception) {
            log.warn("InsightService: failed to parse Claude JSON response: {}", ex.message)
            emptyList()
        }
    }

    // ─── Internal DTOs ───────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ClaudeResponse(
        val content: List<ContentBlock>?,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ContentBlock(
        val type: String?,
        val text: String?,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class InsightDto(
        val title: String = "",
        val body: String = "",
        val moduleCode: String? = null,
        val actionText: String? = null,
    )
}
