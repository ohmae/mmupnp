import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm

plugins {
    id("mmupnp.jvm")
    `java-library`
    id("org.jetbrains.dokka")
    id("org.jetbrains.dokka-javadoc")
    id("org.jetbrains.kotlinx.kover")
    id("com.vanniktech.maven.publish")
}

dokka {
    dokkaPublications.html {
        outputDirectory.set(rootProject.layout.projectDirectory.dir("docs/${project.name}"))
    }
    dokkaPublications.named("javadoc") {
        outputDirectory.set(layout.buildDirectory.dir("docs/javadoc"))
    }
}

mavenPublishing {
    configure(KotlinJvm(javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationJavadoc")))
    publishToMavenCentral()
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        versionMapping {
            allVariants {
                fromResolutionResult()
            }
        }
    }
}
