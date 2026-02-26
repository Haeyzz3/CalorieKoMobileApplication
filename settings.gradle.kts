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

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Mapbox Maven repository (requires secret download token)
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials.username = "mapbox"
            // Read the download token from local.properties (git-ignored)
            val localPropertiesFile = rootProject.projectDir.resolve("local.properties")
            val localProperties = java.util.Properties()
            if (localPropertiesFile.exists()) {
                localProperties.load(localPropertiesFile.inputStream())
            }
            credentials.password = localProperties.getProperty("MAPBOX_DOWNLOADS_TOKEN")
                ?: throw GradleException(
                    "MAPBOX_DOWNLOADS_TOKEN not found in local.properties. " +
                    "Please add: MAPBOX_DOWNLOADS_TOKEN=sk.your_token_here"
                )
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

rootProject.name = "CalorieKo Mobile Application"
include(":app")
