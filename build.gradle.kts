buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", version = "1.8.21"))
        classpath(kotlin("serialization", version = "1.8.21"))
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.8.10")

        classpath("com.github.ben-manes:gradle-versions-plugin:0.46.0")
    }
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
    }
}

tasks.create("clean", Delete::class) {
    delete(rootProject.buildDir)
}
