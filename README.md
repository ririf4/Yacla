# Yacla

**Yet Another Config Loading API** –
Yacla is a flexible, type-safe, annotation-driven configuration loading library for **Java** and **Kotlin**.

✅ Supports **YAML** and **JSON** out of the box
<p>
✅ Comment-preserving auto-update (YAML)
<p>
✅ Field validation via annotations
<p>
✅ Default value injection
<p>
✅ Auto-update with version checking
<p>
✅ Works with **Java records** and **Kotlin data classes**
<p>

---

## Installation

### Latest version:

Core:
![Core Version](https://img.shields.io/badge/dynamic/xml?url=https://repo.ririfa.net/repository/maven-public/net/ririfa/yacla-core/maven-metadata.xml&query=/metadata/versioning/latest&style=plastic&logo=sonatype&label=Nexus)

YAML:
![YAML Version](https://img.shields.io/badge/dynamic/xml?url=https://repo.ririfa.net/repository/maven-public/net/ririfa/yacla-yaml/maven-metadata.xml&query=/metadata/versioning/latest&style=plastic&logo=sonatype&label=Nexus)

JSON:
![JSON Version](https://img.shields.io/badge/dynamic/xml?url=https://repo.ririfa.net/repository/maven-public/net/ririfa/yacla-json/maven-metadata.xml&query=/metadata/versioning/latest&style=plastic&logo=sonatype&label=Nexus)


Add the repository to your build file:

Gradle (Groovy):

```groovy
repositories {
    maven { url "https://repo.ririfa.net/maven2" }
}

dependencies {
    implementation 'net.ririfa:yacla-core:[Version]'
    //implementation 'net.ririfa:yacla-yaml:[Version]'
    //implementation 'net.ririfa:yacla-json:[Version]'
}
```

Gradle (Kotlin):

```kotlin
repositories {
    maven("https://repo.ririfa.net/maven2")
}

dependencies {
    implementation("net.ririfa:yacla-core:[Version]")
    //implementation("net.ririfa:yacla-yaml:[Version]")
    //implementation("net.ririfa:yacla-json:[Version]")
}
```

Maven:

```xml
<repositories>
  <repository>
    <id>ririfa-repo</id>
    <url>https://repo.ririfa.net/maven2</url>
  </repository>
</repositories>

<dependency>
  <groupId>net.ririfa</groupId>
  <artifactId>yacla-core</artifactId>
  <version>[Version]</version>
</dependency>
<!-- And YAML/JSON if needed -->
```

💡 All versions available at [RiriFa Repo](https://repo.ririfa.net/service/rest/repository/browse/maven-public/net/ririfa/).

---

## Usage

⚠️ **Important: Supported config types**

| Language | Class type required                                                             |
|----------|---------------------------------------------------------------------------------|
| Kotlin   | ✅ `data class` (primary constructor required)                                   |
| Java     | ✅ `record` (with `@NamedRecord` annotations unless compiled with `-parameters`) |

> 📝 Java: safer to **always use `@NamedRecord`** to ensure field mapping.

---

### 1️⃣ Define your configuration class

Kotlin:

```kotlin
import net.ririfa.yacla.annotation.Default
import net.ririfa.yacla.annotation.Required
import net.ririfa.yacla.annotation.Range

data class AppConfig(
    @Required
    val apiKey: String? = null,

    @Default("8080")
    @Range(min = 1, max = 65535)
    val port: Int = 0
)
```

Java:

```java
import net.ririfa.yacla.annotation.Default;
import net.ririfa.yacla.annotation.Required;
import net.ririfa.yacla.annotation.Range;
import net.ririfa.yacla.annotation.NamedRecord;

public record AppConfig(
    @NamedRecord("apiKey") @Required String apiKey,
    @NamedRecord("port") @Default("8080") @Range(min = 1, max = 65535) int port
) {}
```

---

### 2️⃣ Load configuration

Kotlin (YAML):

```kotlin
import net.ririfa.yacla.Yacla
import net.ririfa.yacla.yaml.YamlParser
import java.nio.file.Paths

fun main() {
    val loader = Yacla.loader<AppConfig>()
        .fromResource("/defaults/config.yml")
        .toFile(Paths.get("config.yml"))
        .parser(YamlParser())
        .autoUpdateIfOutdated(true)
        .load()

    loader.nullCheck().validate()

    val config = loader.config
    println("API Key: ${config.apiKey}")
    println("Port: ${config.port}")
}
```

Kotlin (JSON):

```kotlin
import net.ririfa.yacla.Yacla
import net.ririfa.yacla.json.JsonParser
import java.nio.file.Paths

fun main() {
    val loader = Yacla.loader<AppConfig>()
        .fromResource("/defaults/config.json")
        .toFile(Paths.get("config.json"))
        .parser(JsonParser())
        .autoUpdateIfOutdated(true)
        .load()

    loader.nullCheck().validate()

    val config = loader.config
    println("API Key: ${config.apiKey}")
    println("Port: ${config.port}")
}
```

Java (JSON):

```java
import net.ririfa.yacla.Yacla;
import net.ririfa.yacla.json.JsonParser;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        var loader = Yacla.loader(AppConfig.class)
                .fromResource("/defaults/config.json")
                .toFile(Paths.get("config.json"))
                .parser(new JsonParser())
                .autoUpdateIfOutdated(true)
                .load();

        loader.nullCheck().validate();

        AppConfig config = loader.getConfig();
        System.out.println("API Key: " + config.apiKey());
        System.out.println("Port: " + config.port());
    }
}
```

✅ If the file doesn’t exist → copies from resource
✅ If version outdated → merges missing keys while preserving user values
✅ JSON and YAML both support auto-update (YAML preserves root-level comments)

---

## Annotations

| Annotation     | Description                                                            |
|----------------|------------------------------------------------------------------------|
| `@Required`    | Field must not be null or blank (throws if missing unless `soft=true`) |
| `@Default`     | Fills field with default if null/blank                                 |
| `@Range`       | Validates numeric field within range                                   |
| `@NamedRecord` | Maps record component to config key (required if no `-parameters`)     |

---

## Auto Update

For both YAML and JSON:

* compares `version` fields in default resource and user config
* if current version < default → updates config, preserves existing values, adds new keys

Example `version` field:

```yaml
version: 1.2.0
```

Trigger manually:

```java
loader.updateConfig().reload();
```

✅ YAML: keeps root-level comments
✅ JSON: uses structural merge

---

## Logging

No logger by default.

Provide your own or use SLF4J:

```java
import net.ririfa.yacla.logger.impl.SLF4JYaclaLogger;

var loader = Yacla.loader(AppConfig.class)
    .withLogger(SLF4JYaclaLogger.INSTANCE);
```

---

## Extending

**YAML and JSON are built-in.**

For other formats (e.g., TOML):

1. Implement `ConfigParser`
2. Implement `UpdateStrategy`
3. Register:

```java
UpdateStrategyRegistry.register(TomlParser.class, new TomlUpdateStrategy());
```

✅ Then use `.parser(new TomlParser())`

---

## 📌 Notes

* ✅ Kotlin: `data class` (primary constructor required)
* ✅ Java: `record` (use `@NamedRecord` unless compiled with `-parameters`)
* ❌ Regular classes not supported
* ✅ YAML and JSON supported out of the box
* ✅ Constructor-based mapping
* ✅ Uses SnakeYAML Engine v2 and Jackson