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

rootProject.name = "Jeeves"

// Application module
include(":app")

// Core modules
include(":core:common")
include(":core:ui")
include(":core:data")
include(":core:domain")
include(":core:ai")
include(":core:ai-provider")

// Feature modules (future expansion)
// include(":feature:tasks")
// include(":feature:goals")
// include(":feature:calendar")
// include(":feature:briefings")
