package build.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.artifacts.dsl.ArtifactHandler
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named

private val Project.sourceSets: SourceSetContainer get() = (this as ExtensionAware).extensions.getByName("sourceSets") as SourceSetContainer

private fun ArtifactHandler.archives(artifactNotation: Any): PublishArtifact = add("archives", artifactNotation)

internal fun Project.sourcesJarSettings() {
    tasks.create("sourcesJar", Jar::class) {
        dependsOn("classes")
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

    artifacts {
        archives(tasks.named<Jar>("sourcesJar"))
    }
}
