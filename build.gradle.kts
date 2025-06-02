import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.HttpURLConnection
import java.net.URI
import java.util.jar.JarFile

plugins {
    alias(libs.plugins.kotlin)
    id("org.jetbrains.dokka") version "2.0.0"
    id("com.gradleup.shadow") version "8.3.6" apply false
    `maven-publish`
}

val coreVer = "1.1.0+beta.5"
val yamlVer = "1.1.0+beta.2"
val jsonVer = "1.1.0+beta.2"
val extDbVer = "1.1.0+beta.6"

allprojects {
    group = "net.ririfa"
    version = when (name) {
        "yacla-core" -> coreVer
        "yacla-yaml" -> yamlVer
        "yacla-json" -> jsonVer
        "yacla-ext-db" -> extDbVer
        else -> "1.0.0"
    }

    repositories {
        mavenCentral()
        maven("https://repo.ririfa.net/maven2/")
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "maven-publish")
    apply(plugin = "com.gradleup.shadow")

    gradle.projectsEvaluated {
        val shadedAPI = configurations.getByName("shadedAPI")

        shadedAPI.forEach { logger.lifecycle("Shaded API: $it") }

        val artifacts = try {
            shadedAPI.resolvedConfiguration.resolvedArtifacts
        } catch (e: Exception) {
            //logger.warn("Could not resolve shadedAPI in ${project.name}: ${e.message}")
            emptySet<ResolvedArtifact>()
        }

        artifacts.forEach { artifact ->
            val id = artifact.moduleVersion.id
            val notation = "${id.group}:${id.name}:${id.version}"
            //logger.lifecycle("Automatically adding to api: $notation in ${project.name}")
            dependencies.add("api", notation)
        }
    }

    java {
        withSourcesJar()
        withJavadocJar()

        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    tasks.withType<JavaCompile> {
        options.release.set(17)
    }

    tasks.named<Jar>("jar") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveClassifier.set("")
    }

    tasks.withType<PublishToMavenRepository>().configureEach {
        onlyIf {
            val artifactId = project.name
            val ver = project.version.toString()
            val repoUrl = if (ver.endsWith("SNAPSHOT")) {
                "https://repo.ririfa.net/maven2-snap/"
            } else {
                "https://repo.ririfa.net/maven2-rel/"
            }

            val artifactUrl = "${repoUrl}net/ririfa/$artifactId/$ver/$artifactId-$ver.jar"
            logger.lifecycle("Checking existence of artifact at: $artifactUrl")

            val connection = URI(artifactUrl).toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000

            val exists = connection.responseCode == HttpURLConnection.HTTP_OK
            connection.disconnect()

            if (exists) {
                logger.lifecycle("Artifact already exists at $artifactUrl, skipping publish.")
                false
            } else {
                logger.lifecycle("Artifact not found at $artifactUrl, proceeding with publish.")
                true
            }
        }
    }

    tasks.register<Jar>("plainJar") {
        group = "ririfa"
        description = "Project classes only"
        dependsOn("classes")
        archiveClassifier.set("")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(sourceSets.main.get().output)
    }

    tasks.register<ShadowJar>("relocatedFatJar") {
        group = "build"
        description = "Creates a relocated fat jar containing shadedAPI dependencies"
        archiveClassifier.set("fat")
        configurations.add(project.configurations.getByName("shadedAPI"))
        from(sourceSets.main.get().output)

        doFirst {
            val shaded = project.configurations.getByName("shadedAPI")
            val artifacts = try {
                shaded.resolvedConfiguration.resolvedArtifacts
            } catch (e: Exception) {
                logger.warn("Could not resolve shadedAPI: ${e.message}")
                emptySet<ResolvedArtifact>()
            }

            artifacts.forEach { artifact ->
                val jarFile = artifact.file
                val classNames = JarFile(jarFile).use { jar ->
                    jar.entries().asSequence()
                        .filter { it.name.endsWith(".class") && !it.name.startsWith("META-INF") }
                        .map { it.name }
                        .toList()
                }

                val basePackage = inferCommonPackagePrefix(classNames)
                if (basePackage.isNotBlank()) {
                    val moduleName = artifact.moduleVersion.id.name
                    val relocated = "net.ririfa.shaded.$moduleName"
                    logger.lifecycle("Relocating $basePackage → $relocated")
                    relocate(basePackage, relocated)
                }
            }
        }
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = project.group.toString()
                artifactId = project.name
                version = project.version.toString()

                artifact(tasks.named<Jar>("plainJar"))

                artifact(tasks.named<ShadowJar>("relocatedFatJar"))

                pom {
                    name.set(project.name)
                    description.set("")
                    url.set("https://github.com/ririf4/Yacla")
                    licenses {
                        license {
                            name.set("MIT")
                            url.set("https://opensource.org/license/mit")
                        }
                    }
                    developers {
                        developer {
                            id.set("ririfa")
                            name.set("RiriFa")
                            email.set("main@ririfa.net")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/ririf4/Yacla.git")
                        developerConnection.set("scm:git:ssh://github.com/ririf4/Yacla.git")
                        url.set("https://github.com/ririf4/Yacla")
                    }
                }
            }
        }
        repositories {
            maven {
                val releasesRepoUrl = uri("https://repo.ririfa.net/maven2-rel/")
                val snapshotsRepoUrl = uri("https://repo.ririfa.net/maven2-snap/")
                url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl

                credentials {
                    username = findProperty("nxUN").toString()
                    password = findProperty("nxPW").toString()
                }
            }
        }
    }
}

fun inferCommonPackagePrefix(classNames: List<String>): String {
    val packages = classNames
        .mapNotNull {
            it.replace('/', '.')
                .takeIf { name -> name.endsWith(".class") }
                ?.removeSuffix(".class")
                ?.substringBeforeLast('.', "")
        }
        .map { it.split('.') }
        .filter { it.size >= 2 }

    if (packages.isEmpty()) return ""

    var common = packages.first().toMutableList()

    for (pkg in packages.drop(1)) {
        val minLength = minOf(common.size, pkg.size)
        for (i in 0 until minLength) {
            if (common[i] != pkg[i]) {
                common = common.take(i).toMutableList()
                break
            }
        }
        if (common.isEmpty()) break
    }

    return common.joinToString(".")
}

project(":yacla-core") {
    val shadedAPI = configurations.create("shadedAPI") {
        isTransitive = true
        isCanBeConsumed = false
        isCanBeResolved = true
    }

    afterEvaluate {
        dependencies {
            api(libs.slf4j.api)
            api(libs.kotlin.reflect)
        }
    }
}

project(":yacla-yaml") {
    val shadedAPI = configurations.create("shadedAPI") {
        isTransitive = false
        isCanBeConsumed = false
        isCanBeResolved = true
    }

    afterEvaluate {
        dependencies {
            shadedAPI(libs.yaml)
            compileOnly(project(":yacla-core"))
        }
    }
}

project(":yacla-json") {
    val shadedAPI = configurations.create("shadedAPI") {
        isTransitive = false
        isCanBeConsumed = false
        isCanBeResolved = true
    }

    afterEvaluate {
        dependencies {
            shadedAPI(libs.jackson)
            shadedAPI(libs.jackson.kotlin)
            compileOnly(project(":yacla-core"))
        }
    }
}

project(":yacla-ext-db") {
    val shadedAPI = configurations.create("shadedAPI") {
        isTransitive = false
        isCanBeConsumed = false
        isCanBeResolved = true
    }

    afterEvaluate {
        dependencies {
            compileOnly(project(":yacla-core"))
            shadedAPI(libs.cask)
            shadedAPI(libs.kryo)
        }
    }
}