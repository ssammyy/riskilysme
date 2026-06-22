package io.riskily.sme.scoring

/** Score band (same thresholds as corporate PRISM; SME labels live in the language files). */
enum class Band { ALL_GOOD, WATCH_OUT, ACT_NOW, URGENT }

/** Whether a score is derived from profile only (Phase 1) or refined by the user's live data. */
enum class DataConfidence { PROFILE, LIVE }

/** The 7 SME risk-module codes (mirror risk_modules.code). */
enum class ModuleCode { FX, LIQUIDITY, COUNTERPARTY, COMMODITY, CREDIT, REGULATORY, MACRO }
