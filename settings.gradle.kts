pluginManagement {
    includeBuild("build-logic")
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

rootProject.name = "FraseAFrase"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")

include(":feature:session")
include(":feature:settings")

include(":core:designsystem")
include(":core:data")
include(":core:platform")
include(":core:domain")
include(":core:testing")
