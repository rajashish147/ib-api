// ── risk module ───────────────────────────────────────────────────────────────
// Pre-trade risk pipeline: position size limits, futures exposure, daily loss
// limits, drawdown checks, concentration limits, circuit breakers.
// All checks are composable and run as a pipeline before any order submission.
plugins {
    id("spring-conventions")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.aop)

    // Resilience4j circuit breaker
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.micrometer)

    implementation(libs.micrometer.core)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    compileOnly(libs.spotbugs.annotations)

    testImplementation(libs.bundles.testing.base)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}
