@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(libs.coroutines.core)
    api(libs.kotlin.serialization.core)
    api(libs.kotlinx.datetime)
    
    implementation("javax.inject:javax.inject:1")

    testImplementation(libs.junit)
}
