// ── api module ────────────────────────────────────────────────────────────────
// Spring MVC REST controllers, OpenAPI documentation, request/response DTOs.
// Controllers delegate to application-layer use case interfaces ONLY —
// no business logic inside controllers.
plugins {
    id("spring-conventions")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.micrometer.registry.prometheus)

    // OpenAPI / Swagger UI
    implementation(libs.springdoc.openapi.ui)

    // Jackson
    implementation(libs.bundles.jackson)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    compileOnly(libs.spotbugs.annotations)

    // MapStruct: domain models ↔ API response DTOs
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
    annotationProcessor(libs.lombok.mapstruct.binding)

    testImplementation(libs.bundles.testing.base)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}
