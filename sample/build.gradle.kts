plugins {
    id("mmupnp.jvm")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":mmupnp"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.swing)
    implementation(libs.serialization.json)
    testImplementation(libs.junit)
}
