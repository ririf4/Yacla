import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.tasks.DokkaBaseTask
import org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.HttpURLConnection
import java.net.URI

plugins {
    alias(libs.plugins.kotlin)
    id("org.jetbrains.dokka") version "2.0.0"

    `maven-publish`
}

val coreVer = "1.0.0"
val yamlVer = "1.0.0"

tasks.named<DokkaBaseTask>("dokkaGenerate") {
    doLast {
        val outputDir = layout.buildDirectory.asFile.get().resolve("dokka/html")
        outputDir.mkdirs()

        subprojects.forEach { subproject ->
            val sourceDir = subproject.layout.buildDirectory.asFile.get().resolve("dokka/html")
            val targetDir = outputDir.resolve(subproject.name)
            logger.lifecycle("Copying Dokka output from $sourceDir to $targetDir")

            project.copy {
                from(sourceDir)
                into(targetDir)
            }
        }
    }
}

allprojects {
    group = "net.ririfa"
    version = when (name) {
        "yacla-core" -> coreVer
        "yacla-yaml" -> yamlVer
        else -> "1.0.0"
    }

    repositories {
        mavenCentral()
    }

    afterEvaluate {
        dependencies {
            implementation(libs.slf4j.api)
            implementation(libs.kotlin.reflect)
        }
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "maven-publish")

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
        doFirst {
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

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                logger.lifecycle("Artifact already exists at $artifactUrl, skipping publish.")
                this.enabled = false // skip task
            } else {
                logger.lifecycle("Artifact not found at $artifactUrl, proceeding with publish.")
            }

            connection.disconnect()
        }
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = project.group.toString()
                artifactId = project.name
                version = project.version.toString()

                from(components["java"])

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

project(":yacla-yaml") {
    afterEvaluate {
        dependencies {
            implementation(libs.yaml)
            compileOnly(project(":yacla-core"))
        }
    }
}