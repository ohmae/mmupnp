package build

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.named
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

private fun Project.jacoco(configure: JacocoPluginExtension.() -> Unit): Unit =
    (this as ExtensionAware).extensions.configure("jacoco", configure)

fun Project.jacocoSettings() {
    jacoco {
        toolVersion = "0.8.6"
    }

    tasks.named<JacocoReport>("jacocoTestReport") {
        reports {
            xml.isEnabled = true
            html.isEnabled = true
        }
    }
}
