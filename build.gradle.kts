
plugins {
    alias(libs.plugins.spotless)
    alias(libs.plugins.kover)
}

spotless {
    kotlin {
        target("**/src/**/*.kt")
        targetExclude("**/build/**", "**/bin/**", "**/.gradle/**")
        ktlint("1.2.1").editorConfigOverride(
            mapOf(
                "ktlint_standard_no-wildcard-imports" to "disabled", // allow Compose imports
                "ktlint_standard_filename" to "disabled",
                "ktlint_standard_property-naming" to "disabled",
                "ktlint_standard_backing-property-naming" to "disabled",
                "ktlint_disabled_rules" to "filename,property-naming,backing-property-naming",
                "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
            ),
        )
    }
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "**/BuildConfig",
                    "**/BuildConfig.*",
                    "**/R",
                    "**/R$*",
                    "**/*_*Factory*",
                )
            }
        }
    }
}
