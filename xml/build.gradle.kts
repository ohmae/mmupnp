plugins {
    id("mmupnp.library")
}

dependencies {
    implementation(libs.kotlin.stdlib)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
}
