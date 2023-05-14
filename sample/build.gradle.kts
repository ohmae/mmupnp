import build.ProjectProperties
import build.dependencyUpdatesSettings

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.ben-manes.versions")
}

group = ProjectProperties.groupId
version = ProjectProperties.versionName

kotlin {
    jvmToolchain(11)
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

dependencies {
    implementation(project(":mmupnp"))
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    testImplementation("junit:junit:4.13.2")
}

dependencyUpdatesSettings()
