# Yacla

**Yet Another Config Loading API** — Yacla is a flexible, type-safe configuration system for **Kotlin** and **Java**, featuring schema-based config definitions,
multi-source loaders, validation, and automatic update strategies.

✨ Designed for production environments where configuration safety matters.

---

## Features

* 🧩 **Schema-based configuration DSL** (no reflection setters)
* 📄 **YAML / JSON** support with automatic **update & merge**
* 💬 YAML update preserves **user comments**
* 🔍 **Field-level validation** (required, ranges, custom validators)
* 🔌 **Custom loaders** for arbitrary types
* ♻ Works with **Kotlin data classes** and **Java records**

---

## Installation

```kotlin
repositories {
    maven("https://repo.ririfa.net/maven2")
}
```

```kotlin
dependencies {
    implementation("net.ririfa:yacla-core:[Version]")
    implementation("net.ririfa:yacla-yaml:[Version]")
    implementation("net.ririfa:yacla-json:[Version]")
}
```

### Latest version:

Core: ![Core Version](https://img.shields.io/badge/dynamic/xml?url=https://repo.ririfa.net/repository/maven-public/net/ririfa/yacla-core/maven-metadata.xml&query=/metadata/versioning/latest&style=plastic&logo=sonatype&label=Nexus)

YAML: ![YAML Version](https://img.shields.io/badge/dynamic/xml?url=https://repo.ririfa.net/repository/maven-public/net/ririfa/yacla-yaml/maven-metadata.xml&query=/metadata/versioning/latest&style=plastic&logo=sonatype&label=Nexus)

JSON: ![JSON Version](https://img.shields.io/badge/dynamic/xml?url=https://repo.ririfa.net/repository/maven-public/net/ririfa/yacla-json/maven-metadata.xml&query=/metadata/versioning/latest&style=plastic&logo=sonatype&label=Nexus)

---

## Quick Start

### 1. Config Class

```kotlin
data class AppConfig(
    val apiKey: String?,
    val port: Int
)
```

### 2. Schema Definition

```kotlin
object AppSchema : YaclaSchema<AppConfig> {
    override fun configure(def: FieldDefBuilder<AppConfig>) {
        def.field(AppConfig::apiKey) {
            required()
        }
        def.field(AppConfig::port) {
            default(8080)
            range(min = 1, max = 65535)
        }
    }
}
```

The schema describes validation, defaults, loaders, and YAML key mapping — not the annotations.

---

## Load YAML / JSON

```kotlin
val config = Yacla.loader(AppConfig::class, AppSchema) {
    fromResource("/defaults/config.yml")
    toFile(Paths.get("config.yml"))
    parser(YamlParser())
    autoUpdateIfOutdated(true)
}.load()
    .nullCheck()
    .validate()
    .config
```

* YAML updates **preserve comments**
* JSON updates **structurally merge fields**

---

## Field Options (DSL)

| Option        | Example                                |
|---------------|----------------------------------------|
| Required      | `required()` / `required(soft = true)` |
| Default value | `default("prod")`                      |
| Rename key    | `name("server.port")`                  |
| Range limit   | `range(1, 65535)`                      |
| Custom loader | `loader(EnumLoader(Mode::class))`      |
| Null handler  | `ifNull(MyNullHandler::class)`         |
| Validator     | `validate { check(it >= 0) }`          |

---

## Custom Format Support

```kotlin
class TomlParser : ConfigParser { ... }
class TomlUpdateStrategy : UpdateStrategy { ... }

UpdateStrategyRegistry.register(TomlParser::class.java, TomlUpdateStrategy())
```

---

## Logging

```kotlin
.withLogger(SLF4JYaclaLogger)
```

---

## Requirements

* Kotlin: data classes
* Java: records
* No setter-based reflection mapping
* Internal dependencies: SnakeYAML v2, Jackson, Kryo

---

## License

MIT

---

## Links

📘 Documentation: [https://docs.ririfa.net/](https://docs.ririfa.net/)