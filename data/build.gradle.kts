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

fun Project.secretValue(key: String): String {
    val propertyValue = (findProperty(key) as? String)?.takeIf { value ->
        value.isNotBlank() && !value.startsWith("dummy", ignoreCase = true)
    }
    val envValue = System.getenv(key)?.takeIf { it.isNotBlank() && !it.startsWith("dummy", ignoreCase = true) }
    return propertyValue ?: envValue ?: defaultSecrets.getProperty(key).orEmpty()
}

private fun String.escapeForBuildConfig(): String = replace("\"", "\\\"")

android {
    namespace = "com.v7lthronyx.scamynx.data"
    compileSdk = 34

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

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

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    coreLibraryDesugaring(libs.android.desugar)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
