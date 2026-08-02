rootProject.name = "Wallosmobile"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
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

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":androidApp")
include(":composeApp")

include(":core:api")
include(":core:domain")
include(":core:storage")
include(":core:navigation")
include(":core:async-kmp")
include(":core:appinfo-api")
include(":core:logger")

include(":utils:ui")
include(":utils:formatter:decimal")
include(":utils:formatter:datetime")

include(":uikit")
include(":strings")
include(":testing")

include(":feature:setup:data")
include(":feature:setup:domain")
include(":feature:setup:dto")
include(":feature:setup:ui")

include(":feature:settings:ui")

include(":feature:subscriptions:data")
include(":feature:subscriptions:domain")
include(":feature:subscriptions:dto")
include(":feature:subscriptions:mapper")
include(":feature:subscriptions:ui")
