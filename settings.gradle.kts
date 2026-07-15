rootProject.name = "drinkin"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":backend")
include(":shared")
include(":androidApp")
include(":webApp")
