package build.internal

import build.ProjectProperties
import groovy.util.Node
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getPluginByName
import org.gradle.kotlin.dsl.named

private fun Project.publishing(configure: PublishingExtension.() -> Unit): Unit =
    (this as ExtensionAware).extensions.configure("publishing", configure)

private val Project.base: BasePluginConvention
    get() = ((this as? Project)?.convention ?: (this as HasConvention).convention).getPluginByName("base")

private val NamedDomainObjectContainer<Configuration>.api: NamedDomainObjectProvider<Configuration>
    get() = named<Configuration>("api")

private val NamedDomainObjectContainer<Configuration>.implementation: NamedDomainObjectProvider<Configuration>
    get() = named<Configuration>("implementation")

internal fun Project.publishingSettings() {
    publishing {
        publications {
            create<MavenPublication>("bintray") {
                artifact("$buildDir/libs/${base.archivesBaseName}-${version}.jar")
                groupId = ProjectProperties.groupId
                artifactId = base.archivesBaseName
                version = ProjectProperties.versionName
                artifact(tasks["sourcesJar"])
                pom.withXml {
                    val node = asNode()
                    val licenses = node.appendNode("licenses")
                    appendLicense(licenses, "The MIT License", "https://opensource.org/licenses/MIT", "repo")
                    appendScm(node, ProjectProperties.Url.scm, ProjectProperties.Url.github)
                    val dependencies = node.appendNode("dependencies")
                    configurations.api.get().dependencies.forEach {
                        appendDependency(
                            dependencies,
                            groupId = it.group ?: "",
                            artifactId = it.name,
                            version = it.version ?: "",
                            scope = "compile"
                        )
                    }
                    configurations.implementation.get().dependencies.forEach {
                        appendDependency(
                            dependencies,
                            groupId = it.group ?: "",
                            artifactId = it.name,
                            version = it.version ?: "",
                            scope = "runtime"
                        )
                    }
                }
            }
        }
    }
}

private fun appendLicense(parentNode: Node, name: String, url: String, distribution: String) {
    parentNode.appendNode("license").apply {
        appendNode("name", name)
        appendNode("url", url)
        appendNode("distribution", distribution)
    }
}

private fun appendScm(parentNode: Node, connection: String, url: String) {
    parentNode.appendNode("scm").apply {
        appendNode("connection", connection)
        appendNode("url", url)
    }
}

private fun appendDependency(parentNode: Node, groupId: String, artifactId: String, version: String, scope: String) {
    parentNode.appendNode("dependency").apply {
        appendNode("groupId", groupId)
        appendNode("artifactId", artifactId)
        appendNode("version", version)
        appendNode("scope", scope)
    }
}
