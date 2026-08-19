package com.envy.maxspeedvpn.core

import com.envy.maxspeedvpn.routing.RoutingPolicy

internal interface SingBoxGateway {
    suspend fun start(config: String)
    suspend fun stop()
    fun version(): String
}

internal class SingBoxEngine(
    private val gateway: SingBoxGateway,
    private val routingPolicy: RoutingPolicy = RoutingPolicy(),
) : CoreEngine {
    override val kind = EngineKind.SING_BOX
    override val startupOrder = EngineStartupOrder.ENGINE_FIRST

    override suspend fun validate(config: String): ValidationResult = runCatching {
        SingBoxConfigConverter.convert(config, routingPolicy)
    }.fold(
        onSuccess = { ValidationResult.Valid },
        onFailure = { ValidationResult.Invalid(it.message ?: "Invalid sing-box configuration") },
    )

    override suspend fun start(config: String, tunFileDescriptor: Int) {
        val converted = runCatching { SingBoxConfigConverter.convert(config, routingPolicy) }
            .getOrElse { throw IllegalArgumentException(it.message ?: "Invalid sing-box configuration", it) }
        gateway.start(converted)
    }

    override suspend fun stop() = gateway.stop()

    fun version(): String = gateway.version()
}
