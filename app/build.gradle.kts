plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("com.google.gms.google-services")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

android {
    namespace = "com.antechrist.adherentsapp"
    compileSdk = 36

    signingConfigs {
        create("release") {
            val ksPath = project.findProperty("ADH_KEYSTORE")?.toString()
                ?: error("ADH_KEYSTORE manquant")
            storeFile = file(ksPath)
            storePassword = project.findProperty("ADH_STORE_PASSWORD")?.toString()
                ?: error("ADH_STORE_PASSWORD manquant")
            keyAlias = project.findProperty("ADH_KEY_ALIAS")?.toString()
                ?: error("ADH_KEY_ALIAS manquant")
            keyPassword = project.findProperty("ADH_KEY_PASSWORD")?.toString()
                ?: error("ADH_KEY_PASSWORD manquant")
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true          // on réactive R8
            isShrinkResources = true        // <-- bonne propriété en Kotlin DSL
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    defaultConfig {
        applicationId = "com.antechrist.adherentsapp"
        minSdk = 29
        targetSdk = 35
        versionCode = 24
        versionName = "2.4.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // >>> mets 17 ici
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}


dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4") // ajustable si besoin
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.activity:activity-compose:1.10.1")

    implementation(platform("androidx.compose:compose-bom:2025.08.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material:1.6.8")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")

    implementation("androidx.navigation:navigation-compose:2.9.3")

    implementation("io.coil-kt:coil-compose:2.7.0")

    // Firebase (BoM)
    implementation(platform("com.google.firebase:firebase-bom:33.2.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")// ✅
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.52")
    implementation(libs.androidx.compose.ui.geometry)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.foundation)
    implementation(libs.foundation)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material3)
//    implementation(libs.firebase.messaging.ktx)
    kapt("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.08.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation(platform("androidx.compose:compose-bom:2025.08.01"))

}

