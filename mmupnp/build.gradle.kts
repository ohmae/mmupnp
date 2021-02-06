import build.*
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    `java-library`
    kotlin("jvm")
    id("org.jetbrains.dokka")
    maven
    `maven-publish`
    id("com.jfrog.bintray")
    jacoco
    id("com.github.ben-manes.versions")
}

base.archivesBaseName = "mmupnp"
group = ProjectProperties.groupId
version = ProjectProperties.versionName

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    maxParallelForks = Runtime.getRuntime().availableProcessors()
}

tasks.named<DokkaTask>("dokkaHtml") {
    outputDirectory.set(File(projectDir, "../docs"))
}

dependencies {
    implementation(kotlin("stdlib"))
    api("net.mm2d:log:0.9.3")

    testImplementation("junit:junit:4.13.1")
    testImplementation("io.mockk:mockk:1.10.5")
    testImplementation("com.google.truth:truth:1.1.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
}

commonSettings()
