plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("androidx.navigation.safeargs")
}

import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

        android {
            namespace = "com.cesar.creamazospoketcg"
            compileSdk = 34

            defaultConfig {
                applicationId = "com.cesar.creamazospoketcg"
                minSdk = 23
                targetSdk = 34
                versionCode = 1
                versionName = "0.1"

                // Leer las claves de local.properties si existen (no hacer fail si no están)
                val pokeKey: String = (project.findProperty("POKETCG_API_KEY") as? String).orEmpty()
                val googleKey: String = (project.findProperty("GOOGLE_API_KEY") as? String).orEmpty()
                val googleCx: String = (project.findProperty("GOOGLE_CX") as? String).orEmpty()

                buildConfigField("String", "POKETCG_API_KEY", "\"$pokeKey\"")
                buildConfigField("String", "GOOGLE_API_KEY", "\"$googleKey\"")
                buildConfigField("String", "GOOGLE_CX", "\"$googleCx\"")
            }

            buildFeatures {
                compose = true
                viewBinding = true
                buildConfig = true
            }

            composeOptions {
                kotlinCompilerExtensionVersion = "1.5.7"
            }

            kotlinOptions {
                jvmTarget = "17"
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }

            packaging {
                resources {
                    excludes += setOf(
                        "META-INF/DEPENDENCIES",
                        "META-INF/LICENSE"
                    )
                }
            }
        }

dependencies {

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.21")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Material
    implementation("com.google.android.material:material:1.12.0")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.15.1")

    // Fragment KTX
    implementation("androidx.fragment:fragment-ktx:1.6.1")

    // Firebase (BoM)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // COMPOSE
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.activity:activity-compose:1.8.2")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
