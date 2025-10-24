import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    kotlin("jvm") version "2.2.0"
    id("org.jetbrains.intellij.platform") version "2.10.1"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        create("IC", "2025.2")
        testFramework(TestFrameworkType.Platform)
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

intellijPlatform {
    pluginConfiguration {
        id = "com.kaiserssin.code_navigator"
        name = "Code Navigator"
        version = "0.0.1"
        description = "Test task for JetBrains internship"
        vendor { name = "Artem Smirnov" }
    }
}

tasks.test { useJUnitPlatform() }
kotlin { jvmToolchain(21) }
