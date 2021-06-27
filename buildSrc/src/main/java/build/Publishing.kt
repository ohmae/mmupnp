package build

import groovy.util.Node
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension
import java.net.URI

private fun Project.publishing(configure: PublishingExtension.() -> Unit): Unit =
    (this as ExtensionAware).extensions.configure("publishing", configure)

private val Project.base: BasePluginExtension
    get() = (this as ExtensionAware).extensions.getByName("base") as BasePluginExtension

private val NamedDomainObjectContainer<Configuration>.api: NamedDomainObjectProvider<Configuration>
    get() = named("api", Configuration::class.java)

private val NamedDomainObjectContainer<Configuration>.implementation: NamedDomainObjectProvider<Configuration>
    get() = named("implementation", Configuration::class.java)

fun Project.signing(configure: SigningExtension.() -> Unit): Unit =
    (this as ExtensionAware).extensions.configure("signing", configure)

val Project.publishing: PublishingExtension
    get() = (this as ExtensionAware).extensions.getByName("publishing") as PublishingExtension

fun Project.publishingSettings() {
    publishing {
        publications {
            create("mavenJava", MavenPublication::class.java) {
                artifact("$buildDir/libs/${base.archivesName.get()}-${version}.jar")
                artifact(tasks.getByName("sourcesJar"))
                artifact(tasks.getByName("javadocJar"))
                groupId = ProjectProperties.groupId
                artifactId = base.archivesName.get()
                version = ProjectProperties.versionName
                pom.withXml {
                    val node = asNode()
                    node.appendNode("name", ProjectProperties.name)
                    node.appendNode("description", ProjectProperties.description)
                    node.appendNode("url", ProjectProperties.Url.site)
                    node.appendNode("licenses").appendNode("license").apply {
                        appendNode("name", "The MIT License")
                        appendNode("url", "https://opensource.org/licenses/MIT")
                        appendNode("distribution", "repo")
                    }
                    node.appendNode("developers").appendNode("developer").apply {
                        appendNode("id", ProjectProperties.developerId)
                        appendNode("name", ProjectProperties.developerName)
                    }
                    node.appendNode("scm").apply {
                        appendNode("connection", ProjectProperties.Url.scm)
                        appendNode("developerConnection", ProjectProperties.Url.scm)
                        appendNode("url", ProjectProperties.Url.github)
                    }
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
        repositories {
            maven {
                url = URI("https://oss.sonatype.org/service/local/staging/deploy/maven2")
                credentials {
                    username = project.findProperty("sonatype_username") as? String ?: ""
                    password = project.findProperty("sonatype_password") as? String ?: ""
                }
            }
        }
    }
    signing {
        sign(publishing.publications.getByName("mavenJava"))
    }
    tasks.named("publish") {
        dependsOn("assemble")
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
