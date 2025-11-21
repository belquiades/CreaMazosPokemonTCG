pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        id("com.android.application") version "8.3.2"
        id("com.android.library") version "8.3.2"
        id("org.jetbrains.kotlin.android") version "1.9.21"
        id("androidx.navigation.safeargs") version "2.7.7"
        id("com.google.gms.google-services") version "4.4.0"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CreaMazosPokeTCG"
include(":app")
