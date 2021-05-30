plugins {
    kotlin("jvm") version "1.5.10"
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.github.ben-manes:gradle-versions-plugin:0.38.0")
}
