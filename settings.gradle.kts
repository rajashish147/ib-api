rootProject.name = "ib-trader"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

// Core modules (no Spring Boot dependency)
include("domain")
include("application")

// Infrastructure & integration modules
include("infrastructure")
include("risk")
include("strategy-engine")
include("scheduler")

// Presentation layer
include("api")

// Bootstrap (Spring Boot main class + auto-config assembly)
include("bootstrap")
