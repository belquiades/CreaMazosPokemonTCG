plugins {
    id("com.android.application") apply false
    id("org.jetbrains.kotlin.android") apply false
    id("androidx.navigation.safeargs") apply false
    id("com.google.gms.google-services") apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}
