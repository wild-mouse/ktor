package io.ktor.client.features.websocket

interface WebSocketEngineFactory {
    fun create(): WebSocketEngine
}
