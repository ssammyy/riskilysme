package io.riskily.sme.entitlement

/** Marks a controller method as gated behind a feature entitlement. Enforced by [FeatureGuardAspect]. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresFeature(val value: Feature)
