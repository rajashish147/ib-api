// ── application module ────────────────────────────────────────────────────────
// Use-case orchestration layer. Spring-aware (for @Transactional, @Component)
// but contains zero infrastructure details. All external calls go through
// domain port interfaces.
plugins {
    id("spring-conventions")
}

dependencies {
    api(project(":domain"))
    implementation(project(":strategy-engine"))
    implementation(project(":risk"))

    // Spring context for @Component, @Transactional, @Validated
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.aop)
    implementation(libs.spring.boot.starter.data.jpa)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    compileOnly(libs.spotbugs.annotations)

    // MapStruct for command/query DTO mapping
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
    annotationProcessor(libs.lombok.mapstruct.binding)

    testImplementation(libs.bundles.testing.base)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}
