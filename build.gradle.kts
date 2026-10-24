plugins {
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kover)
}

dependencies {
    kover(project(":mmupnp"))
    kover(project(":xml"))
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
