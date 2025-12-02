pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "Yacla"

include(":core", ":yaml", ":json")

project(":core").name = "yacla-core"
project(":yaml").name = "yacla-yaml"
project(":json").name = "yacla-json"