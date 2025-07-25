// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.0" // Add Detekt plugin
    id("org.jlleitschuh.gradle.ktlint") version "11.3.2" // Add ktlint plugin
    alias(libs.plugins.compose.compiler) apply false
}

detekt {
    toolVersion = "1.23.0"
    config = files("$rootDir/detekt-config.yml") // Optional: Specify a custom config file
    buildUponDefaultConfig = true
    allRules = false
}

ktlint {
    version.set("0.48.2") // Specify ktlint version
    enableExperimentalRules.set(true) // Enable experimental rules if needed
    verbose.set(true) // Enable verbose output
}
