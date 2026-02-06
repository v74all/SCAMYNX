pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "SCAMYNX"

include(
    ":app",
    ":domain",
    ":data",
    ":ml",
    ":networksecurity",
    ":report",
    ":common",
    ":baselineprofile",
)
