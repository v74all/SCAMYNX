import java.util.Properties
import org.gradle.api.Project

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.secrets)
}

val defaultSecrets = Properties().apply {
    val defaultsFile = rootProject.file("secrets.defaults.properties")
    if (defaultsFile.exists()) {
        defaultsFile.inputStream().use { load(it) }
    }
}

val localSecrets = Properties().apply {
    val secretsFile = rootProject.file("secrets.properties")
    if (secretsFile.exists()) {
        secretsFile.inputStream().use { load(it) }
    }
}

fun Project.secretValue(key: String): String {
    // 1. Check secrets.properties first (local secrets)
    val localValue = localSecrets.getProperty(key)?.takeIf { value ->
        value.isNotBlank() && !value.startsWith("dummy", ignoreCase = true)
    }
    // 2. Check gradle properties
    val propertyValue = (findProperty(key) as? String)?.takeIf { value ->
        value.isNotBlank() && !value.startsWith("dummy", ignoreCase = true)
    }
    // 3. Check environment variables
    val envValue = System.getenv(key)?.takeIf { it.isNotBlank() && !it.startsWith("dummy", ignoreCase = true) }
    // 4. Fall back to defaults
    return localValue ?: propertyValue ?: envValue ?: defaultSecrets.getProperty(key).orEmpty()
}

private fun String.escapeForBuildConfig(): String = replace("\"", "\\\"")

android {
    namespace = "com.v7lthronyx.scamynx.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField(
            "String",
            "VERSION_NAME",
            "\"${findProperty("app.versionName")}\"",
        )
        buildConfigField(
            "String",
            "VIRUSTOTAL_API_KEY",
            "\"${secretValue("VIRUSTOTAL_API_KEY").escapeForBuildConfig()}\"",
        )
        buildConfigField(
            "String",
            "GOOGLE_SAFE_BROWSING_API_KEY",
            "\"${secretValue("GOOGLE_SAFE_BROWSING_API_KEY").escapeForBuildConfig()}\"",
        )
        buildConfigField(
            "String",
            "URLSCAN_API_KEY",
            "\"${secretValue("URLSCAN_API_KEY").escapeForBuildConfig()}\"",
        )
        buildConfigField(
            "String",
            "SCAMYNX_TELEMETRY_ENDPOINT",
            "\"${secretValue("SCAMYNX_TELEMETRY_ENDPOINT").escapeForBuildConfig()}\"",
        )
        buildConfigField(
            "String",
            "OPENAI_API_KEY",
            "\"${secretValue("OPENAI_API_KEY").escapeForBuildConfig()}\"",
        )
        buildConfigField(
            "String",
            "GROQ_API_KEY",
            "\"${secretValue("GROQ_API_KEY").escapeForBuildConfig()}\"",
        )
        buildConfigField(
            "String",
            "OPENROUTER_API_KEY",
            "\"${secretValue("OPENROUTER_API_KEY").escapeForBuildConfig()}\"",
        )
        buildConfigField(
            "String",
            "HUGGINGFACE_API_KEY",
            "\"${secretValue("HUGGINGFACE_API_KEY").escapeForBuildConfig()}\"",
        )
        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${secretValue("SUPABASE_URL").escapeForBuildConfig()}\"",
        )
        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"${secretValue("SUPABASE_ANON_KEY").escapeForBuildConfig()}\"",
        )
        buildConfigField(
            "String",
            "SUPABASE_FUNCTION_JWT",
            "\"${secretValue("SUPABASE_FUNCTION_JWT").escapeForBuildConfig()}\"",
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    lint {
        abortOnError = false
    }
}

kotlin {
    jvmToolchain(21)
}

secrets {
    defaultPropertiesFileName = "secrets.defaults.properties"
    ignoreList.addAll(
        listOf(
            "VIRUSTOTAL_API_KEY",
            "GOOGLE_SAFE_BROWSING_API_KEY",
            "URLSCAN_API_KEY",
            "SCAMYNX_TELEMETRY_ENDPOINT",
        ),
    )
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(projects.domain)
    implementation(projects.common)

    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.kotlinx.datetime)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.kotlin.serialization.json)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.datastore.proto)
    implementation(libs.androidx.work.runtime)

    // Lifecycle process for app lifecycle
    implementation(libs.androidx.lifecycle.process)

    // CameraX for QR code scanning
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // ML Kit for barcode/QR code recognition
    implementation(libs.mlkit.barcode.scanning)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    coreLibraryDesugaring(libs.android.desugar)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
