@file:Suppress("PropertyName")

import org.jetbrains.dokka.gradle.tasks.DokkaGeneratePublicationTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.HttpURLConnection
import java.net.URI


plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
    alias(libs.plugins.dokka)
    `maven-publish`
}

val YACLA_CORE = "yacla-core"
val YACLA_YAML = "yacla-yaml"
val YACLA_JSON = "yacla-json"

val YACLA_CORE_VERSION = "3.0.0"
val YACLA_YAML_VERSION = "3.0.0"
val YACLA_JSON_VERSION = "3.0.0"

allprojects {
    group = "net.ririfa"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.dokka")

    when (name) {
        YACLA_CORE -> {
            version = YACLA_CORE_VERSION
            afterEvaluate {
                dependencies {
                    implementation(libs.kotlin.reflect)
                    compileOnly(libs.slf4j.api)
                }
            }
        }

        YACLA_YAML -> {
            version = YACLA_YAML_VERSION
            afterEvaluate {
                dependencies {
                    implementation(project(":$YACLA_CORE"))
                    implementation(libs.yaml)
                }
            }
        }

        YACLA_JSON -> {
            version = YACLA_JSON_VERSION
            afterEvaluate {
                dependencies {
                    implementation(project(":$YACLA_CORE"))
                    implementation(libs.jackson.core)
                    implementation(libs.jackson.kotlin)
                }
            }
        }
    }

    java { withSourcesJar() }

    kotlin {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    tasks.shadowJar {
        archiveClassifier.set("all")

        relocate("tools.jackson", "net.ririfa.yacla.libs.jackson")
        relocate("org.snakeyaml", "net.ririfa.yacla.libs.snakeyaml")

        dependencies {
            exclude(dependency("org.jetbrains.kotlin:.*"))
            exclude(dependency("org.jetbrains.kotlinx:.*"))
            exclude(dependency("org.jetbrains:annotations"))
        }

        exclude("META-INF/*.kotlin_module")
        exclude("META-INF/services/*kotlin*")
        exclude("META-INF/*kotlin*")
    }

    tasks.withType<JavaCompile>().configureEach { options.release.set(17) }

    tasks.withType<KotlinCompile>().configureEach { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

    tasks.withType<PublishToMavenRepository>().configureEach {
        onlyIf {
            val artifactId = project.name
            val ver = project.version.toString()
            val repoUrl = if (ver.endsWith("SNAPSHOT"))
                "https://repo.ririfa.net/maven2-snap/"
            else
                "https://repo.ririfa.net/maven2-rel/"
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

    tasks.register<Jar>("javadocJar") {
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

                from(components["java"])
                artifact(tasks.named("javadocJar"))

                pom {
                    name.set(project.name)
                    description.set("Yet Another Config Loading API")
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
                        developerConnection.set("scm:git:ssh://git@github.com/ririf4/Yacla.git")
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