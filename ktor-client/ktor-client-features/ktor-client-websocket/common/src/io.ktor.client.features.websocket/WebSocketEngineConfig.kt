package io.ktor.client.features.websocket

const val WEBSOCKET_DEFAULT_THREADS = 4

class WebSocketEngineConfig {
    var threadsCount: Int = WEBSOCKET_DEFAULT_THREADS
}
