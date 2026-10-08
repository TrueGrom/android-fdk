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

rootProject.name = "android-fdk"
include(":utils")
include(":state")
include(":repository")
include(":viewmodel")
include(":logging")
include(":http-error")
include(":crypto")
include(":network")
include(":datetime")
include(":ui-kit")
include(":screen")
include(":bom")
