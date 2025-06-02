pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "Yacla"

include(":core")
project(":core").name = "yacla-core"

include(":yaml")
project(":yaml").name = "yacla-yaml"

include(":json")
project(":json").name = "yacla-json"

include(":ext-db")
project(":ext-db").name = "yacla-ext-db"