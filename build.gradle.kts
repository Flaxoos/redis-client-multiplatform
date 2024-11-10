import org.gradle.jvm.tasks.Jar
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import java.util.Base64

plugins {
    kotlin("multiplatform") version "1.9.20"
    alias(libs.plugins.kotest)
    alias(libs.plugins.loggingCapabilities)
    alias(libs.plugins.nexusPublish)
    `maven-publish`
    signing
    alias(libs.plugins.dokka)
}

group = "io.github.flaxoos"
version = properties["version"].toString()

kotlin {
    jvm {
        jvmToolchain(11)
    }
    val nativeTarget = linuxX64("native")
    nativeTarget.apply {
        binaries.sharedLib()
        compilations.getByName("main") {
            cinterops {
                val hiredis by creating {
                    defFile("src/nativeInterop/cinterop/hiredis.def")
                    compilerOpts("-Isrc/nativeInterop/hiredis", "-o knedis")
                    includeDirs("src/nativeInterop/hiredis")
                }
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.logging)
                implementation(libs.kotlinx.io.core)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlin.reflect)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotest.framework.engine)
                implementation(libs.kotest.assertions.core)
            }
        }
        val jvmMain by getting {
            dependencies {
                api("io.netty:netty-transport:4.1.91.Final")
                api("io.netty:netty-codec-redis:4.1.99.Final")
                api(libs.kreds)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotest.runner.junit5)
                implementation(libs.mockk)
                implementation(libs.mockk.agent.jvm)
                implementation(libs.logback.classic)
                implementation("org.powermock:powermock-reflect:1.6.1")
            }
        }
    }
    targets.configureEach {
        withSourcesJar(true)
    }
}

loggingCapabilities {
    enforceLogback()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

val dokkaHtml = tasks.named<AbstractDokkaTask>("dokkaHtml")
val dokkaJar =
    tasks.register<Jar>("dokkaJar") {
        archiveClassifier.set("javadoc")
        from(dokkaHtml.get().outputDirectory)
    }

publishing {
    publications.withType<MavenPublication> {
        artifact(dokkaJar)
        pom {
            name.set(project.name)
            groupId = project.group.toString()
            version = project.version.toString()
            url.set("https://github.com/Flaxoos/redis-client-multiplatform")
            inceptionYear.set("2023")

            description = "Kotlin multiplatform redis client"

            scm {
                connection.set("scm:git:https://github.com/Flaxoos/redis-client-multiplatform.git")
                developerConnection.set("scm:git:https://github.com/Flaxoos/redis-client-multiplatform.git")
                url.set("https://github.com/Flaxoos/redis-client-multiplatform")
            }

            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://opensource.org/license/mit/")
                }
            }

            developers {
                developer {
                    id.set("flaxoos")
                    name.set("Ido Flax")
                    email.set("idoflax@gmail.com")
                }
            }
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username = ossrhUsername
            password = ossrhPassword
        }
    }
}

tasks.withType<AbstractPublishToMaven>().configureEach {
    val signingTasks = tasks.withType<Sign>()
    mustRunAfter(signingTasks)
}
signing {
    useInMemoryPgpKeys(Base64.getDecoder().decode(signingKeyArmorBase64).decodeToString(), signingPassword)
    sign(the<PublishingExtension>().publications)
}

private val Project.gprWriteToken
    get() = findProperty("gpr.write.key") as String? ?: System.getenv("GPR_WRITE_TOKEN")
private val Project.gprReadToken
    get() = findProperty("gpr.read.key") as String? ?: System.getenv("GPR_READ_TOKEN")
private val Project.gprUser
    get() = findProperty("gpr.user") as String? ?: System.getenv("GPR_USER")
private val Project.ossrhUsername
    get() = findProperty("ossrh.username") as String? ?: System.getenv("OSSRH_USERNAME")
private val Project.ossrhPassword
    get() = findProperty("ossrh.password") as String? ?: System.getenv("OSSRH_PASSWORD")
val Project.signingKeyArmorBase64: String
    get() = findProperty("signing.key.armor.base64") as String? ?: System.getenv("SIGNING_KEY_ARMOR_BASE64")
val Project.signingPassword: String
    get() = findProperty("signing.password") as String? ?: System.getenv("SIGNING_PASSWORD")
