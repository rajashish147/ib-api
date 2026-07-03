// ── strategy-engine module ────────────────────────────────────────────────────
// Spring State Machine configuration, threshold evaluation, rebalance plan
// generation, and strategy execution. Supports FULL_REBALANCE (Option A)
// and FIXED_AMOUNT (Option B) strategy modes.
plugins {
    id("spring-conventions")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":risk"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.aop)

    // Spring State Machine
    implementation(libs.spring.statemachine.core)
    implementation(libs.spring.statemachine.data.jpa)

    // Observability
    implementation(libs.micrometer.core)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    compileOnly(libs.spotbugs.annotations)

    testImplementation(libs.bundles.testing.base)
    testImplementation(libs.spring.statemachine.test)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}
