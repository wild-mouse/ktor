package io.ktor.client.features.websocket

import io.ktor.http.cio.websocket.*
import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.io.core.*
import org.khronos.webgl.*
import org.w3c.dom.*
import kotlin.coroutines.*

internal class JsWebSocketSession(
    override val coroutineContext: CoroutineContext,
    private val websocket: WebSocket
) : DefaultWebSocketSession {
    private val _closeReason: CompletableDeferred<CloseReason> = CompletableDeferred()
    private val _incoming: Channel<Frame> = Channel(Channel.UNLIMITED)
    private val _outgoing: Channel<Frame> = Channel(Channel.UNLIMITED)

    override val incoming: ReceiveChannel<Frame> = _incoming
    override val outgoing: SendChannel<Frame> = _outgoing

    override val closeReason: Deferred<CloseReason?> = _closeReason

    init {
        websocket.binaryType = BinaryType.ARRAYBUFFER

        websocket.onmessage = { event ->
            launch {
                val frame: Frame = when (event.type) {
                    "BINARY" -> Frame.Binary(false, event.data as ByteArray)
                    "TEXT" -> Frame.Text(event.data as String)
                    else -> error("Unknown frame type: ${event.type}")
                }
                _incoming.offer(frame)
            }
        }

        websocket.onerror = {
            _incoming.close(WebSocketException("$it"))
            _outgoing.cancel()
        }

        websocket.onclose = {
            launch {
                val event = it as CloseEvent
                _incoming.send(Frame.Close(CloseReason(event.code, event.reason)))
                _incoming.close()

                _outgoing.cancel()
            }
        }

        launch {
            _outgoing.consumeEach {
                when (it.frameType) {
                    FrameType.TEXT -> {
                        val text = it.data
                        websocket.send(String(text))
                    }
                    FrameType.BINARY -> {
                        websocket.send(it.data as ArrayBuffer)
                    }
                    FrameType.CLOSE -> {
                        val data = buildPacket { it.data }
                        websocket.close(data.readShort(), data.readText())
                    }
                }
            }
        }
    }

    override suspend fun flush() {
    }

    override fun terminate() {
        _incoming.cancel()
        _outgoing.cancel()
        websocket.close()
    }

    @KtorExperimentalAPI
    override suspend fun close(cause: Throwable?) {
        _incoming.send(Frame.Close())
    }
}
