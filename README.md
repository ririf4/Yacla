# Yacla

**Yet Another Config Loading API** - Yacla is a lightweight, type-safe configuration loader for **Kotlin** and **Java**.

Yacla maps YAML or JSON files into Kotlin data classes using primary constructor parameters, Kotlin default values, and optional field annotations.

---

## Features

* Kotlin data class based config definitions
* YAML / JSON loading
* Automatic initial copy from bundled resources
* Version-based update and merge support
* YAML update support with comment-aware parsing
* Field annotations for key mapping, validation, conversion, warnings, and custom loaders
* Shared loader defaults for multi-config applications

---

## Installation

```kotlin
repositories {
    maven("https://repo.ririfa.net/maven2")
}
```

```kotlin
dependencies {
    implementation("net.ririfa:yacla-core:4.1.0")
    implementation("net.ririfa:yacla-yaml:4.0.0")
    implementation("net.ririfa:yacla-json:4.0.0")
}
```

---

## Quick Start

### 1. Define a config class

```kotlin
import net.ririfa.yacla.annotation.Key
import net.ririfa.yacla.annotation.NotBlank
import net.ririfa.yacla.annotation.Range

data class AppConfig(
    val version: String = "1.0.0",

    @Key("server-port")
    @Range(min = 1, max = 65535)
    val port: Int = 8080,

    @NotBlank
    val apiKey: String? = null,

    val debug: Boolean = false
)
```

### 2. Add a default YAML resource

Place a default file in your application resources, for example `src/main/resources/defaults/config.yml`.

```yaml
version: "1.0.0"
server-port: 8080
apiKey: "change-me"
debug: false
```

### 3. Load the config

```kotlin
import net.ririfa.yacla.Yacla
import net.ririfa.yacla.yaml.yaml
import java.nio.file.Paths

val config = Yacla.yaml<AppConfig>(
    resource = "/defaults/config.yml",
    file = Paths.get("config.yml"),
    autoUpdate = true
)
```

If `config.yml` does not exist, Yacla copies `/defaults/config.yml` from the classpath first, then loads it.

---

## Loader API

The shortcut APIs return the loaded config object directly.

```kotlin
val yamlConfig = Yacla.yaml<AppConfig>("/defaults/config.yml", Paths.get("config.yml"))
val jsonConfig = Yacla.json<AppConfig>("/defaults/config.json", Paths.get("config.json"))
```

Use `yamlLoader` or `jsonLoader` when you need reload/update access.

```kotlin
import net.ririfa.yacla.yaml.yamlLoader

val loader = Yacla.yamlLoader<AppConfig>(
    resource = "/defaults/config.yml",
    file = Paths.get("config.yml"),
    autoUpdate = true
)

val config = loader.config
loader.reload()
loader.updateConfig()
```

The lower-level builder API is still available.

```kotlin
import net.ririfa.yacla.Yacla
import net.ririfa.yacla.yaml.YamlParser
import java.nio.file.Paths

val loader = Yacla.loader<AppConfig>()
    .fromResource("/defaults/config.yml")
    .toFile(Paths.get("config.yml"))
    .parser(YamlParser())
    .autoUpdateIfOutdated(true)
    .load()
```

---

## Field Mapping

By default, Yacla resolves constructor parameters using these key forms:

```text
apiKey -> apikey
apiKey -> api_key
apiKey -> api-key
```

Use `@Key` when the config file uses a custom key.

```kotlin
data class ServerConfig(
    @Key("server-port")
    val port: Int = 8080
)
```

---

## Field Annotations

| Annotation                 | Purpose                                                        |
|----------------------------|----------------------------------------------------------------|
| `@Key("name")`             | Maps a parameter to a specific config key                      |
| `@Range(min, max)`         | Validates numeric values                                       |
| `@NotBlank`                | Rejects blank strings when present                             |
| `@BlankToNull`             | Converts blank strings to null                                 |
| `@SetOf`                   | Converts a collection to a set and treats an empty set as null |
| `@EnumList`                | Converts string collections to enum lists, case-insensitive    |
| `@EnumSet`                 | Converts string collections to enum sets, case-insensitive     |
| `@Loader(MyLoader::class)` | Uses a custom field loader                                     |
| `@Warn("message")`         | Logs a warning when the value is null                          |

---

## Custom Field Loader

```kotlin
import net.ririfa.yacla.annotation.Loader
import net.ririfa.yacla.loader.FieldLoader

class TrimLoader : FieldLoader {
    override fun load(raw: Any?): Any? = raw?.toString()?.trim()
}

data class AppConfig(
    @Loader(TrimLoader::class)
    val name: String? = null
)
```

---

## Shared Defaults

```kotlin
import net.ririfa.yacla.LoaderSettings
import net.ririfa.yacla.Yacla
import net.ririfa.yacla.logger.impl.SLF4JYaclaLogger
import net.ririfa.yacla.yaml.YamlParser
import java.nio.file.Paths

val settings = LoaderSettings(
    parser = YamlParser(),
    logger = SLF4JYaclaLogger,
    autoUpdate = true
)

val config = Yacla.withDefaults(settings) {
    loader<AppConfig> {
        fromResource("/defaults/config.yml")
        toFile(Paths.get("config.yml"))
    }.config
}
```

---

## Auto Update

When `autoUpdate` is enabled, Yacla compares the default resource version with the current file version.

* YAML looks for a `version` key case-insensitively.
* JSON looks for a `version` key.
* User-defined values are preserved when the default config adds new keys.

---

## Custom Format Support

```kotlin
class TomlParser : ConfigParser { ... }
class TomlUpdateStrategy : UpdateStrategy { ... }

UpdateStrategyRegistry.register(TomlParser::class.java, TomlUpdateStrategy())
```

---

## Requirements

* Java 17+
* Kotlin data classes with primary constructors
* Java records are planned, but the current implementation is primarily constructor-based

---

## License

MIT
