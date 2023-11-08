rootProject.name = "redis-client-multiplatform"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("com.gradle.enterprise") version("3.15.1")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

