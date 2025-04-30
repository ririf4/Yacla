import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin)

    `maven-publish`
}

allprojects {
    apply(plugin = "kotlin")

    group = "net.ririfa"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }

    afterEvaluate {
        dependencies {
            implementation(libs.bundles.yacla)
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
    }
}

publishing {
    publications {
        //maven
        create<MavenPublication>("maven") {

            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])

            pom {
                name.set("Yacla")
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
                dependencies
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