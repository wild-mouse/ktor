package io.ktor.client.features.websocket

import io.ktor.http.cio.websocket.*
import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.io.js.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import kotlin.coroutines.*

internal class JsWebSocketSession(
    websocket: WebSocket
) : DefaultWebSocketSession {
    override val coroutineContext: CoroutineContext = Dispatchers.Default

    private val _incoming: Channel<Frame> = Channel(Channel.UNLIMITED)
    private val _outgoing: Channel<Frame> = Channel(Channel.UNLIMITED)

    override val incoming: ReceiveChannel<Frame> = _incoming
    override val outgoing: SendChannel<Frame> = _outgoing

    init {
        websocket.onmessage = { event ->
            val frame: Frame = when (event.type) {
                "BINARY" -> Frame.Binary(false, event.data as ByteArray)
                "TEXT" -> Frame.Text(event.data as String)
                else -> error("")
            }
            event.data
            launch {
                _incoming.offer(frame)
            }
        }

        websocket.onerror = { _incoming.close(WebSocketException("$it")) }

        websocket.onclose = {
            launch {
                val event = it as CloseEvent
                _incoming.send(Frame.Close(CloseReason(event.code, event.reason)))
                _incoming.close()
            }
        }

        launch {
            _outgoing.consumeEach {
            }
        }
    }

    override val closeReason: Deferred<CloseReason?>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override suspend fun flush() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun terminate() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @KtorExperimentalAPI
    override suspend fun close(cause: Throwable?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
