rootProject.name = "CineCat"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

// shared: tot el codi comú (models, xarxa, estat i UI de Compose Multiplatform).
// androidApp: l'app Android, que només arrenca la UI de shared.
// L'app iOS (iosApp/) és un projecte Xcode que enllaça shared com a framework.
include(":shared")
include(":androidApp")
