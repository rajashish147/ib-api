plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    // Convention plugins need access to the Spring Boot plugin class for BOM coordinates
    implementation("org.springframework.boot:org.springframework.boot.gradle.plugin:3.3.1")
    implementation("io.spring.gradle:dependency-management-plugin:1.1.5")
}
