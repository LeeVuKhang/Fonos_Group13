import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

fun localProperty(name: String): String? = localProperties.getProperty(name)
    ?.trim()
    ?.takeIf { it.isNotEmpty() }

fun buildConfigString(value: String): String = "\"" + value
    .replace("\\", "\\\\")
    .replace("\"", "\\\"") + "\""

android {
    namespace = "com.example.fonos_group13"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.fonos_group13"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "BACKEND_BASE_URL",
                buildConfigString(localProperty("BACKEND_BASE_URL") ?: "http://10.0.2.2:8080")
            )
        }
        release {
            isMinifyEnabled = false
            buildConfigField(
                "String",
                "BACKEND_BASE_URL",
                buildConfigString(localProperty("BACKEND_BASE_URL") ?: "")
            )
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.androidx.media3.common)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.material)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.glide)
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
