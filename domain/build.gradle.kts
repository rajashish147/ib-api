// ── domain module ─────────────────────────────────────────────────────────────
// Pure Java domain layer. Zero Spring, Zero JPA, Zero IBKR dependencies.
// Only Lombok for reducing boilerplate. This ensures the domain is
// independently testable and portable.
plugins {
    id("java-conventions")
}

dependencies {
    // Lombok only — no frameworks in domain
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs annotations for @NonNull etc. — compile-time only, no runtime cost
    compileOnly(libs.spotbugs.annotations)

    // Testing
    testImplementation(libs.bundles.testing.base)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}
