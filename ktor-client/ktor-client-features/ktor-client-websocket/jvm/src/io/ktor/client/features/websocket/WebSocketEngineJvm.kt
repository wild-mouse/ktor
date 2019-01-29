package io.ktor.client.features.websocket

import java.util.*

internal actual fun findWebSocketEngine(): WebSocketEngine = FACTORY.create()

private val webSocketEngines: List<WebSocketEngineFactory> = WebSocketEngineFactory::class.java.let {
    ServiceLoader.load(it, it.classLoader).toList()
}

private val FACTORY by lazy {
    webSocketEngines.firstOrNull()
        ?: error("Failed to find WebSocket client engine implementation in the classpath: consider adding WebSocket engine dependency.")
}
