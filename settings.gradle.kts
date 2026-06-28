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
rootProject.name = "ReasonTouch"
include(":app")
// Core
include(":core:core-data")
include(":core:core-midi")
include(":core:core-audio")
include(":core:core-ui")
include(":core:core-composition")
include(":core:core-music")
// Features
include(":feature:feature-chords")
include(":feature:feature-pianoroll")
include(":feature:feature-export")
include(":feature:feature-drums")
include(":core:core-playback")