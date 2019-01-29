package io.ktor.client.features.websocket

import io.ktor.client.tests.utils.*
import io.ktor.http.cio.websocket.*
import kotlinx.io.core.*
import kotlin.test.*

class WebSocketRemoteTest {

    @Test
    fun testRemotePingPong(): Unit = clientTest {
        val remote = "echo.websocket.org"

        config {
            install(WebSockets)
        }

        test { client ->
            client.ws(host = remote) {
                repeat(10) {
                    ping(it.toString())
                }
            }
        }
    }

    @Test
    fun testSecureRemotePingPong(): Unit = clientTest {
        val remote = "echo.websocket.org"

        config {
            install(WebSockets)
        }

        test { client ->
            client.wss(host = remote) {
                repeat(10) {
                    ping(it.toString())
                }
            }
        }
    }

    private suspend fun WebSocketSession.ping(salt: String) {
        outgoing.send(Frame.Text("text: $salt"))
        val frame = incoming.receive()
        check(frame is Frame.Text)
        assertEquals("text: $salt", (frame as Frame.Text).readText())

        val data = "text: $salt".toByteArray()
        outgoing.send(Frame.Binary(true, data))
        val binaryFrame = incoming.receive()
        check(binaryFrame is Frame.Binary)

        val buffer = binaryFrame.data
        assertEquals(data.contentToString(), buffer.contentToString())
    }
}
