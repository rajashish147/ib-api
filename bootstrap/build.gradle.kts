// ── bootstrap module ──────────────────────────────────────────────────────────
// Spring Boot application entry point. Assembles all modules, provides
// auto-configuration, application.yml, and acts as the single deployable JAR.
plugins {
    id("spring-conventions")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation(project(":infrastructure"))
    implementation(project(":api"))
    implementation(project(":strategy-engine"))
    implementation(project(":risk"))
    implementation(project(":scheduler"))

    // Full Spring Boot stack
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.aop)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.configuration.processor)

    // Database
    implementation(libs.postgresql)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)

    // Observability
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.springdoc.openapi.ui)

    // Jackson
    implementation(libs.bundles.jackson)

    // Resilience4j
    implementation(libs.bundles.resilience4j)

    // State Machine
    implementation(libs.spring.statemachine.core)
    implementation(libs.spring.statemachine.data.jpa)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Integration tests for the full application
    testImplementation(libs.bundles.testing.base)
    testImplementation(libs.bundles.testcontainers)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

// Produce a fat (executable) JAR for Docker deployment
springBoot {
    buildInfo()
}

tasks.bootJar {
    archiveFileName.set("ib-trader.jar")
}

// Disable plain JAR when bootJar is active
tasks.jar {
    enabled = false
}


tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile.readLines().forEach {
            if (it.isNotBlank() && !it.startsWith("#")) {
                val parts = it.split("=", limit = 2)
                if (parts.size == 2) {
                    val value = parts[1].substringBefore("#").trim()
                    environment(parts[0].trim(), value)
                }
            }
        }
    }
}
