package net.mm2d.ktor.server.httpu

import io.ktor.server.engine.*

object HTTPU : ApplicationEngineFactory<HTTPUApplicationEngine, HTTPUApplicationEngine.Configuration> {
    override fun create(
        environment: ApplicationEngineEnvironment,
        configure: HTTPUApplicationEngine.Configuration.() -> Unit
    ): HTTPUApplicationEngine = HTTPUApplicationEngine(environment, configure)
}
