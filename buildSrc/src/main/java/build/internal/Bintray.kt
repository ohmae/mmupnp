package build.internal

import build.ProjectProperties
import com.jfrog.bintray.gradle.BintrayExtension
import org.gradle.api.Project
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.closureOf
import org.gradle.kotlin.dsl.getPluginByName

private fun Project.bintray(configure: BintrayExtension.() -> Unit): Unit =
    (this as ExtensionAware).extensions.configure("bintray", configure)

private val Project.base: BasePluginConvention
    get() = ((this as? Project)?.convention ?: (this as HasConvention).convention).getPluginByName("base")

internal fun Project.bintraySettings() {
    bintray {
        user = project.findProperty("bintray_user") as? String ?: ""
        key = project.findProperty("bintray_key") as? String ?: ""
        setPublications("bintray")

        dryRun = true

        pkg(closureOf<BintrayExtension.PackageConfig> {
            repo = "maven"
            name = ProjectProperties.groupId + "." + base.archivesBaseName
            setLicenses("MIT")
            websiteUrl = ProjectProperties.Url.site
            vcsUrl = ProjectProperties.Url.github + ".git"
            issueTrackerUrl = ProjectProperties.Url.github + "/issues"
            publicDownloadNumbers = true
            version = VersionConfig().apply {
                name = ProjectProperties.versionName
            }
        })
    }

    tasks.named("bintrayUpload") {
        dependsOn("assemble")
    }
}
