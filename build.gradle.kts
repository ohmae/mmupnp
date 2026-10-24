plugins {
    kotlin("jvm") version "2.4.21" apply false
    kotlin("plugin.serialization") version "2.4.21" apply false
    id("org.jetbrains.dokka") version "2.2.0" apply false
    id("org.jetbrains.dokka-javadoc") version "2.2.0" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
