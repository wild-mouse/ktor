package io.ktor.client.features.websocket

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.websocket.*
import io.ktor.util.*

private const val WEBSOCKET_VERSION = "13"
private const val NONCE_SIZE = 16

internal class WebSocketContent : ClientUpgradeContent() {
    private val nonce: String = buildString {
        val nonce = generateNonce(NONCE_SIZE)
        append(nonce.encodeBase64())
    }

    override val headers: Headers = HeadersBuilder().apply {
        append(HttpHeaders.Upgrade, "websocket")
        append(HttpHeaders.Connection, "upgrade")

        append(HttpHeaders.SecWebSocketKey, nonce)
        append(HttpHeaders.SecWebSocketVersion, WEBSOCKET_VERSION)
    }.build()

    override fun verify(headers: Headers) {
        val serverAccept = headers[HttpHeaders.SecWebSocketAccept]
            ?: error("Server should specify header ${HttpHeaders.SecWebSocketAccept}")

        val expectedAccept = websocketServerAccept(nonce)
        check(expectedAccept == serverAccept) {
            "Failed to verify server accept header. Expected: $expectedAccept, received: $serverAccept"
        }
    }
}
