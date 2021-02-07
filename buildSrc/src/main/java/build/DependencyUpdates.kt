package build

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.named

fun Project.dependencyUpdatesSettings() {
    tasks.named<DependencyUpdatesTask>("dependencyUpdates").configure {
        rejectVersionIf { !isStable(candidate.version) }
    }
}

private fun isStable(version: String): Boolean {
    val versionUpperCase = version.toUpperCase()
    val hasStableKeyword = listOf("RELEASE", "FINAL", "GA").any { versionUpperCase.contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    return hasStableKeyword || regex.matches(version)
}
