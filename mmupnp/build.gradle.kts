plugins {
    id("mmupnp.library")
}

// MockK spies require lambda classes instead of invokedynamic-generated hidden classes.
tasks.compileTestKotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xlambdas=class")
    }
}

dependencies {
    api(project(":xml"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
}
