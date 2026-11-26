import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
}

// URL de l'API segons el build:
//  - debug   → el backend local vist des de l'emulador (10.0.2.2:8080);
//  - release → producció a Railway (HTTPS).
// Es pot forçar per a qualsevol build amb -Pcinecat.apiUrl=https://… (p. ex.
// per provar un debug contra producció).
val prodApiUrl = "https://backend-production-e587.up.railway.app"
val apiUrlOverride = providers.gradleProperty("cinecat.apiUrl").orNull

android {
    namespace = "cat.cinecat.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "cat.cinecat.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildFeatures {
        buildConfig = true
    }
    buildTypes {
        getByName("debug") {
            buildConfigField("String", "API_URL", "\"${apiUrlOverride ?: "http://10.0.2.2:8080"}\"")
        }
        getByName("release") {
            isMinifyEnabled = false
            buildConfigField("String", "API_URL", "\"${apiUrlOverride ?: prodApiUrl}\"")
            // Signada amb la clau de debug perquè es pugui instal·lar a
            // l'emulador per provar-la. Publicar a Google Play necessitaria
            // una clau pròpia (fora d'abast, vegeu el handoff).
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
