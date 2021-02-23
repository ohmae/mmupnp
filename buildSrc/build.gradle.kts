plugins {
    kotlin("jvm") version "1.4.30"
    `kotlin-dsl`
}

repositories {
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.github.ben-manes:gradle-versions-plugin:0.36.0")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}
