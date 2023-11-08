import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("multiplatform") version "1.9.20"
    alias(libs.plugins.kotest)
    alias(libs.plugins.loggingCapabilities)
}

kotlin {
    jvm {
        jvmToolchain(11)
        project.tasks.named("jvmJar", Jar::class).configure {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            from(
                listOf(
                    project.configurations["jvmCompileClasspath"],
                    project.configurations["jvmRuntimeClasspath"],
                ).map { config ->
                    config.map {
                        if (it.isDirectory) it
                        else project.zipTree(it)
                    }
                },
            )
        }
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
                implementation("io.netty:netty-transport:4.1.91.Final")
                implementation("io.netty:netty-codec-redis:4.1.99.Final")
            }
        }
    }
}

loggingCapabilities {
    enforceLogback()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
