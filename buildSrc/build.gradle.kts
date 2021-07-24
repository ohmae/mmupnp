plugins {
    kotlin("jvm") version "1.4.31"
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.github.ben-manes:gradle-versions-plugin:0.39.0")
}
