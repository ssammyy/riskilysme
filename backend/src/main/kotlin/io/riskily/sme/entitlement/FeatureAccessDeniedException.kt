package io.riskily.sme.entitlement

/** Thrown when an authenticated user's tier does not include a gated feature (HTTP 403). */
class FeatureAccessDeniedException(val feature: Feature) :
    RuntimeException("This feature requires a higher subscription tier: ${feature.key}")
