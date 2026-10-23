plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Optional default game server baked into the APK. Players can always change it
// in the app's Settings screen.
val defaultServerUrl: String = providers.gradleProperty("chhupan.serverUrl")
    .orElse(providers.environmentVariable("CHHUPAN_SERVER_URL"))
    .getOrElse("")

// Release signing comes from environment variables (set as GitHub Actions
// secrets). Without them the release APK is signed with the debug key.
val releaseKeystore: String? = System.getenv("CHHUPAN_KEYSTORE_PATH")

android {
    namespace = "com.ssk.chhupanchhupai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ssk.chhupanchhupai"
        minSdk = 26
        targetSdk = 35
        versionCode = (System.getenv("CHHUPAN_VERSION_CODE") ?: "1").toInt()
        versionName = "1.0.0"
        buildConfigField("String", "DEFAULT_SERVER_URL", "\"$defaultServerUrl\"")
    }

    signingConfigs {
        if (releaseKeystore != null && file(releaseKeystore).exists()) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("CHHUPAN_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("CHHUPAN_KEY_ALIAS")
                keyPassword = System.getenv("CHHUPAN_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.play.services.location)
    implementation(libs.osmdroid)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
