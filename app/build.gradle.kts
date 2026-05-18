plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashalytics.plugin)
}

android {
    namespace  = "com.pabl3st.rutapp"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.pabl3st.rutapp"
        minSdk        = libs.versions.minSdk.get().toInt()
        targetSdk     = libs.versions.targetSdk.get().toInt()
        versionCode   = 66
        versionName   = "1.0.0-s28"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        buildConfigField("String", "API_BASE_URL",
            "\"https://mejoresiagratis.com/rutasproapk/api.php\"")
    }

    buildTypes {
        debug {
            // applicationIdSuffix removido — google-services.json requiere package exacto
            versionNameSuffix   = "-debug"
            isDebuggable        = true
        }
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility            = JavaVersion.VERSION_17
        targetCompatibility            = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions  { jvmTarget = "17" }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues    = true
        }
    }
    buildFeatures  { compose = true; buildConfig = true }

    packaging      { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }

}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.bundles.lifecycle)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.android.compiler)

    // Network — S01: auth contra API
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi)
    ksp(libs.moshi.codegen)

    // Security — token cifrado en Keystore
    implementation(libs.security.crypto)

    // Room — BD local offline-first
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // WorkManager + Hilt integration
    implementation(libs.work.runtime)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)

    // DataStore — preferencias reactivas
    implementation(libs.datastore)

    // GPS y Mapas — MapLibre OSM (sin key, sin cuenta, offline-capable)
    implementation(libs.maplibre)
    implementation(libs.play.services.location)

    // Cámara — captura de fotos en visita (S05)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // Imágenes — carga y caché (previews fotos visita)
    implementation(libs.coil.compose)

    // Permissions — solicitud de permisos con Compose
    implementation(libs.accompanist.permissions)

    // Firebase — FCM (push) + Crashlytics + Analytics
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.fcm)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    // Desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    // Debug
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // ── Unit Tests ──────────────────────────────────────────
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.arch.core.testing)
    testImplementation(libs.room.testing)
    testImplementation(libs.hilt.testing)
    kspTest(libs.hilt.testing.compiler)

    // ── Android Instrumented Tests ───────────────────────────
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}







