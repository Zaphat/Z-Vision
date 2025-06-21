pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Z Vision"
include(":app")

// Core modules
include(":core:common")
include(":core:data")
include(":core:design")
include(":core:domain")
include(":core:network")
include(":core:testing")

// Feature modules
include(":feature:qrscan")
include(":feature:qrcreate")
include(":feature:ocr")
