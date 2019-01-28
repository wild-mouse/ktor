package io.ktor.http.cio.websocket

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

/**
 * Represents a web socket session between two peers
 */
actual interface WebSocketSession : CoroutineScope {
    /**
     * Incoming frames channel
     */
    actual val incoming: ReceiveChannel<Frame>
    /**
     * Outgoing frames channel. It could have limited capacity so sending too much frames may lead to suspension at
     * corresponding send invocations. It also may suspend if a peer doesn't read frames for some reason.
     */
    actual val outgoing: SendChannel<Frame>

    /**
     * Flush all outstanding messages and suspend until all earlier sent messages will be written. Could be called
     * at any time even after close. May return immediately if the connection is already terminated.
     * However it may also fail with an exception (or cancellation) at any point due to session failure.
     * Please note that [flush] doesn't guarantee that frames were actually delivered.
     */
    actual suspend fun flush()

    /**
     * Initiate connection termination immediately. Termination may complete asynchronously.
     */
    actual fun terminate()

    /**
     * Close session with the specified [cause] or with no reason if `null`
     */
    actual suspend fun close(cause: Throwable?)
}
