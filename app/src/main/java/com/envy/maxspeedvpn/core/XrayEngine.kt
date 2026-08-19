package com.envy.maxspeedvpn.core

import com.envy.maxspeedvpn.routing.RoutingPolicy
import com.envy.maxspeedvpn.routing.XrayRouting

internal interface XrayGateway {
    fun start(config: String, tunFileDescriptor: Int)
    fun stop()
    fun measureDelay(): Long
    fun version(): String
}

internal class XrayEngine(
    private val gateway: XrayGateway,
    private val validator: XrayConfigValidator = XrayConfigValidator,
    private val routingPolicy: RoutingPolicy = RoutingPolicy(),
) : CoreEngine {
    override val kind = EngineKind.XRAY

    override suspend fun validate(config: String): ValidationResult =
        validator.validate(XrayRouting.apply(config, routingPolicy))

    override suspend fun start(config: String, tunFileDescriptor: Int) {
        val validation = validate(config)
        require(validation is ValidationResult.Valid) {
            (validation as ValidationResult.Invalid).reason
        }
        gateway.start(XrayRouting.apply(config, routingPolicy), tunFileDescriptor)
    }

    override suspend fun stop() {
        gateway.stop()
    }

    fun measureDelay(): Long = gateway.measureDelay()

    fun version(): String = gateway.version()
}
