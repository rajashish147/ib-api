plugins {
    `java-library`
    id("java-conventions")
    id("io.spring.dependency-management")
}

// Apply Spring BOM so all Spring Boot modules get consistent versions
// without needing explicit version numbers on Spring artifacts.
the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
        mavenBom("org.testcontainers:testcontainers-bom:${versionCatalogs.named("libs").findVersion("testcontainers").get()}")
    }
}
