// ── scheduler module ──────────────────────────────────────────────────────────
// Quartz-backed scheduled jobs: portfolio snapshot capture, strategy evaluation
// polling, market data refresh, position reconciliation, outbox processing.
plugins {
    id("spring-conventions")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation(project(":strategy-engine"))

    implementation(libs.spring.boot.starter.web)

    // Spring Scheduling (built-in) — Quartz is used for persistent jobs
    implementation("org.springframework.boot:spring-boot-starter-quartz")

    implementation(libs.micrometer.core)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    compileOnly(libs.spotbugs.annotations)

    testImplementation(libs.bundles.testing.base)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}
