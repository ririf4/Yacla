# Yacla

**Yet Another Config Loading API** — Yacla is a flexible, annotation-driven, type-safe configuration system for **Kotlin** and **Java**.

### ✅ Highlights

* Supports **YAML**, **JSON**, and **Database-backed configs**
* Automatic **version-based update** with **comment preservation** (YAML)
* **Field-level validation** via annotations
* **Default value injection** via annotations or registry
* Works with **Java records** and **Kotlin data classes**
* Seamless **DB support** via TTL cache + async sync

---

## Installation

### Core modules:

| Module         | Description                         |
| -------------- | ----------------------------------- |
| `yacla-core`   | Core DSL, validation, loader system |
| `yacla-yaml`   | YAML parser + update strategy       |
| `yacla-json`   | JSON parser + update strategy       |
| `yacla-ext-db` | DB-based config loader              |

**Add repository:**

```kotlin
repositories {
    maven("https://repo.ririfa.net/maven2")
}
```

**Gradle (Kotlin DSL):**

```kotlin
dependencies {
    implementation("net.ririfa:yacla-core:[Version]")
    implementation("net.ririfa:yacla-yaml:[Version]")
    implementation("net.ririfa:yacla-json:[Version]")
    implementation("net.ririfa:yacla-ext-db:[Version]")
}
```

### Latest version:

Core:
![Core Version](https://img.shields.io/badge/dynamic/xml?url=https://repo.ririfa.net/repository/maven-public/net/ririfa/yacla-core/maven-metadata.xml&query=/metadata/versioning/latest&style=plastic&logo=sonatype&label=Nexus)

YAML:
![YAML Version](https://img.shields.io/badge/dynamic/xml?url=https://repo.ririfa.net/repository/maven-public/net/ririfa/yacla-yaml/maven-metadata.xml&query=/metadata/versioning/latest&style=plastic&logo=sonatype&label=Nexus)

JSON:
![JSON Version](https://img.shields.io/badge/dynamic/xml?url=https://repo.ririfa.net/repository/maven-public/net/ririfa/yacla-json/maven-metadata.xml&query=/metadata/versioning/latest&style=plastic&logo=sonatype&label=Nexus)

DB:
![DB Version](https://img.shields.io/badge/dynamic/xml?url=https://repo.ririfa.net/repository/maven-public/net/ririfa/yacla-ext-db/maven-metadata.xml&query=/metadata/versioning/latest&style=plastic&logo=sonatype&label=Nexus)

---

## Usage Overview

### 1. Define Config Class

```kotlin
data class AppConfig(
    @Required val apiKey: String?,
    @Default("8080") @Range(min = 1, max = 65535) val port: Int
)
```

```java
public record AppConfig(
    @NamedRecord("apiKey") @Required String apiKey,
    @NamedRecord("port") @Default("8080") @Range(min = 1, max = 65535) int port
) {}
```

---

### 2. Load YAML / JSON Config

```kotlin
val config = Yacla.loader<AppConfig> {
    fromResource("/defaults/config.yml")
    toFile(Paths.get("config.yml"))
    parser(YamlParser())
    autoUpdateIfOutdated(true)
    withLogger(SLF4JYaclaLogger)
}.load().nullCheck().validate().config
```

YAML preserves root comments. JSON performs structural merge.

---

### 3. Load DB-backed Config

```kotlin
val config = Yacla.dbLoader<AppConfig> {
    dataSource(myDataSource)
    table("configs")
    key("app-1")
    ttl(Duration.ofMinutes(5))
}.load().config
```

Use `loader.update { ... }`, `save()`, `reload()`, or `withKey("other")`.

---

## Annotations

| Annotation               | Description                                 |
| ------------------------ | ------------------------------------------- |
| `@Required`              | Field must not be null/blank                |
| `@Default`               | Injects default if value is null/blank      |
| `@Range(min,max)`        | Ensures numeric field is within bounds      |
| `@NamedRecord`           | Maps record param to config key (Java only) |
| `@CustomLoader`          | Defines custom field loader                 |
| `@CustomValidateHandler` | Defines custom validator                    |
| `@IfNullEvenRequired`    | Runs handler if value is null               |

---

## Auto-Update

* Triggers if `version` in file < resource version
* Preserves existing user values
* Adds missing new keys

```yaml
version: 1.2.0
```

```kotlin
loader.updateConfig().reload()
```

---

## Logging

Use your own or built-in SLF4J integration:

```kotlin
.withLogger(SLF4JYaclaLogger)
```

---

## Extend Yacla

To support a new format (e.g. TOML):

```kotlin
class TomlParser : ConfigParser { ... }
class TomlUpdateStrategy : UpdateStrategy { ... }

UpdateStrategyRegistry.register(TomlParser::class.java, TomlUpdateStrategy())
```

---

## Requirements

* Kotlin: `data class`
* Java: `record` + `@NamedRecord`
* No support for reflection-mapped classes (no setters)
* Uses SnakeYAML v2, Jackson, Kryo internally

---

## License

MIT

---

## Links

* 🔗 [Documentation](https://docs.ririfa.net/)