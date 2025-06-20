// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.0" // Add Detekt plugin
}

detekt {
    toolVersion = "1.23.0"
    config = files("$rootDir/detekt-config.yml") // Optional: Specify a custom config file
    buildUponDefaultConfig = true
    allRules = false
}