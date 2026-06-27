// ── infrastructure module ─────────────────────────────────────────────────────
// Adapters for all external systems: PostgreSQL/JPA, IBKR TWS API,
// Spring event bus. Implements all domain outbound port interfaces.
plugins {
    id("spring-conventions")
}

dependencies {
    api(project(":domain"))
    implementation(project(":application"))

    // Spring Data JPA + PostgreSQL + Flyway
    implementation(libs.bundles.spring.data)

    // Spring Web (for RestTemplate used in internal health endpoints)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)

    // IBKR TWS API — local JAR (download TwsApi.jar from IBKR website v10.19
    // and place it in the project root libs/ directory)
    // See: https://interactivebrokers.github.io/
    implementation(fileTree(mapOf("dir" to "${rootDir}/libs", "include" to listOf("TwsApi*.jar"))))

    // Resilience4j for circuit breakers around IB connections
    implementation(libs.bundles.resilience4j)

    // Observability
    implementation(libs.bundles.observability)

    // Jackson for JSON serialization
    implementation(libs.bundles.jackson)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    compileOnly(libs.spotbugs.annotations)

    // MapStruct: JPA entity ↔ Domain model mapping
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
    annotationProcessor(libs.lombok.mapstruct.binding)

    // Spring State Machine persistence adapter
    implementation(libs.spring.statemachine.data.jpa)

    testImplementation(libs.bundles.testing.base)
    testImplementation(libs.bundles.testcontainers)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}
