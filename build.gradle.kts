buildscript {
    repositories {
        mavenCentral()
        jcenter()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", version = "1.4.0"))
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.4.0-rc")

        classpath("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.5")
        classpath("com.github.ben-manes:gradle-versions-plugin:0.28.0")
    }
}

allprojects {
    repositories {
        mavenCentral()
        jcenter()
    }
}

tasks.create("clean", Delete::class) {
    delete(rootProject.buildDir)
}
