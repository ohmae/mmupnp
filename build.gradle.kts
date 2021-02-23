buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", version = "1.4.30"))
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.4.20")

        classpath("com.github.ben-manes:gradle-versions-plugin:0.36.0")
    }
}

allprojects {
    repositories {
        mavenCentral()
    }
}

tasks.create("clean", Delete::class) {
    delete(rootProject.buildDir)
}
