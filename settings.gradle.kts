<<<<<<< HEAD
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        id("com.android.application") version "8.2.2" apply false
        id("org.jetbrains.kotlin.android") version "1.9.21" apply false
        id("com.google.gms.google-services") version "4.4.0" apply false
        id("androidx.navigation.safeargs") version "2.7.7" apply false
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
=======
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
>>>>>>> 14deadeba905dde474a088b3ca52c255202ef862
    repositories {
        google()
        mavenCentral()
    }
<<<<<<< HEAD
}

rootProject.name = "CreaMazosPokeTCG"
include(":app")
=======
}
>>>>>>> 14deadeba905dde474a088b3ca52c255202ef862
