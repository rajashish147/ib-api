import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    `java-library`
    `maven-publish`
    jacoco
    checkstyle
}

group = "com.ibtrader"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
}

// ── Lombok + MapStruct annotation processor ordering ─────────────────────────
configurations.configureEach {
    resolutionStrategy.sortArtifacts(ResolutionStrategy.SortOrder.DEPENDENCY_FIRST)
}

// ── Compilation ───────────────────────────────────────────────────────────────
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(
        listOf(
            "-parameters",                   // preserve method parameter names (needed by Spring MVC + Jackson)
            "-Xlint:all",
            "-Xlint:-processing",            // suppress annotation processing warnings
            "-Xlint:-serial",                // exception classes do not carry serialized state
            "-Werror"                        // treat warnings as errors in CI
        )
    )
}

// ── Testing ───────────────────────────────────────────────────────────────────
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs(
        "-XX:+EnableDynamicAgentLoading",  // suppress Mockito warning on JDK 21
        "-Xshare:off"
    )
    testLogging {
        events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = false
    }
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
    failFast = false
}

// ── JaCoCo Coverage ───────────────────────────────────────────────────────────
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()   // 80% line coverage gate
            }
        }
    }
}

// ── Checkstyle ────────────────────────────────────────────────────────────────
checkstyle {
    toolVersion = "10.17.0"
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    maxWarnings = 0
}

// ── Check task aggregation ────────────────────────────────────────────────────
tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
