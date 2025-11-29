import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.dokka.gradle.tasks.DokkaGeneratePublicationTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.HttpURLConnection
import java.net.URI
import java.util.jar.JarFile

plugins {
    alias(libs.plugins.kotlin)
    id("org.jetbrains.dokka") version "2.1.0"
    id("com.gradleup.shadow") version "9.2.2" apply false
    `maven-publish`
}

val coreVer = "2.1.0"
val yamlVer = "2.0.0"
val jsonVer = "2.0.0"

dependencies {
    testImplementation(project(":yacla-core"))
    testImplementation(project(":yacla-yaml"))
    testImplementation(project(":yacla-json"))
}

allprojects {
    group = "net.ririfa"
    version = when (name) {
        "yacla-core" -> coreVer
        "yacla-yaml" -> yamlVer
        "yacla-json" -> jsonVer
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
        dependsOn("classes")
        group = "ririfa"
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
                val moduleName = artifact.moduleVersion.id.name.replace("-", "_")
                val jarFile = artifact.file

                val classPackages = JarFile(jarFile).use { jar ->
                    jar.entries().asSequence()
                        .filter { it.name.endsWith(".class") && !it.name.startsWith("META-INF") }
                        .mapNotNull { entry ->
                            entry.name
                                .replace('/', '.')
                                .removeSuffix(".class")
                                .substringBeforeLast('.', "")
                        }
                        .toSet()
                }

                if (classPackages.any { it.startsWith("net.ririfa") }) {
                    logger.lifecycle("Skipping relocation for ${artifact.moduleVersion.id} (self package detected)")
                    return@forEach
                }

                classPackages.forEach { pkg ->
                    val relocated = "net.ririfa.shaded.$moduleName.${pkg.replace('.', '_')}"
                    logger.lifecycle("Relocating $pkg → $relocated")
                    relocate(pkg, relocated)
                }
            }
        }
    }

    tasks.register<Jar>("dokkaHtmlJar") {
        group = "dokka"
        description = "Generates HTML documentation using Dokka"
        dependsOn(tasks.named("dokkaGeneratePublicationHtml"))
        from(tasks.named<DokkaGeneratePublicationTask>("dokkaGeneratePublicationHtml").flatMap { it.outputDirectory })
        archiveClassifier.set("javadoc")
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = project.group.toString()
                artifactId = project.name
                version = project.version.toString()

                artifact(tasks.named<Jar>("plainJar"))
                artifact(tasks.named<ShadowJar>("relocatedFatJar"))
                artifact(tasks.named<Jar>("sourcesJar"))
                artifact(tasks.named<Jar>("dokkaHtmlJar"))

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

project(":yacla-core") {
    val shadedAPI = configurations.create("shadedAPI") {
        isTransitive = false
        isCanBeConsumed = false
        isCanBeResolved = true
        extendsFrom(configurations.compileOnly.get())
    }

    afterEvaluate {
        dependencies {
            api(libs.slf4j.api)
            api(libs.kotlin.reflect)
        }
    }

    tasks.named("publishMavenPublicationToMavenRepository") {
        dependsOn("jar")
    }
}

project(":yacla-yaml") {
    val shadedAPI = configurations.create("shadedAPI") {
        isTransitive = false
        isCanBeConsumed = false
        isCanBeResolved = true
        extendsFrom(configurations.compileOnly.get())
    }

    afterEvaluate {
        dependencies {
            shadedAPI(libs.yaml)
            compileOnly(project(":yacla-core"))
        }
    }

    tasks.named("dokkaGeneratePublicationHtml") {
        dependsOn(":yacla-core:plainJar")
    }
    tasks.named("publishMavenPublicationToMavenRepository") {
        dependsOn("jar")
    }
}

project(":yacla-json") {
    val shadedAPI = configurations.create("shadedAPI") {
        isTransitive = false
        isCanBeConsumed = false
        isCanBeResolved = true
        extendsFrom(configurations.compileOnly.get())
    }

    afterEvaluate {
        dependencies {
            shadedAPI(libs.jackson)
            shadedAPI(libs.jackson.kotlin)
            compileOnly(project(":yacla-core"))
        }
    }

    tasks.named("dokkaGeneratePublicationHtml") {
        dependsOn(":yacla-core:plainJar")
    }
    tasks.named("publishMavenPublicationToMavenRepository") {
        dependsOn("jar")
    }
}