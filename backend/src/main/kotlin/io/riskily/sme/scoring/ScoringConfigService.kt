package io.riskily.sme.scoring

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicReference

/**
 * Loads active scoring constants from `scoring_config` and exposes them to the scoring engine
 * (Sprint 2). Cached in memory; call [reload] after an admin re-calibration.
 *
 * Keeps the engine free of magic numbers — every constant resolves through here.
 */
@Service
class ScoringConfigService(
    private val repository: ScoringConfigRepository,
    private val objectMapper: ObjectMapper,
) {
    private val cache = AtomicReference<Map<String, JsonNode>>(emptyMap())

    init {
        reload()
    }

    /** Refresh the in-memory cache from the database. */
    fun reload() {
        val map = repository.findAllByActiveTrue().associate { row ->
            row.configKey to objectMapper.readTree(row.valueJson)
        }
        cache.set(map)
    }

    /** Returns the JSON node for a key, or throws if absent (a missing constant is a bug). */
    fun node(key: String): JsonNode =
        cache.get()[key] ?: error("Missing scoring_config key: $key")

    /** Scalar double at a top-level key (e.g. a single-number constant). */
    fun double(key: String): Double = node(key).asDouble()

    /** Nested double, e.g. double("pressure.fx", "depr7dCoef"). */
    fun double(key: String, field: String): Double {
        val n = node(key).get(field) ?: error("Missing field '$field' in scoring_config key: $key")
        return n.asDouble()
    }

    fun keys(): Set<String> = cache.get().keys
}
