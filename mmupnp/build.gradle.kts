import build.*

plugins {
    `java-library`
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("org.jetbrains.dokka-javadoc")
    `maven-publish`
    signing
    jacoco
}

base.archivesName.set("mmupnp")
group = ProjectProperties.groupId
version = ProjectProperties.versionName

kotlin {
    jvmToolchain(11)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// MockK spies require lambda classes instead of invokedynamic-generated hidden classes.
tasks.compileTestKotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xlambdas=class")
    }
}

tasks.test {
    maxParallelForks = Runtime.getRuntime().availableProcessors()
}

dokka {
    dokkaPublications.html {
        outputDirectory.set(rootProject.layout.projectDirectory.dir("docs/${project.name}"))
    }
    dokkaPublications.named("javadoc") {
        outputDirectory.set(layout.buildDirectory.dir("docs/javadoc"))
    }
}

tasks.register<Jar>("javadocJar") {
    dependsOn("dokkaGeneratePublicationJavadoc")
    archiveClassifier.set("javadoc")
    from(layout.buildDirectory.dir("docs/javadoc"))
}

java {
    withSourcesJar()
}

dependencies {
    api(project(":xml"))
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.14.11")
    testImplementation("com.google.truth:truth:1.4.5")
}

publishingSettings()
jacocoSettings()
