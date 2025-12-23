<<<<<<< HEAD
// build.gradle.kts (root) - minimal, repositories are managed in settings.gradle.kts
tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

=======
buildscript {
    val kotlinVersion = "1.5.31"
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.4")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("com.google.gms:google-services:4.3.10")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
>>>>>>> 14deadeba905dde474a088b3ca52c255202ef862
