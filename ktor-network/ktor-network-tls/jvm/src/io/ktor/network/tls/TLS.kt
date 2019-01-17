package io.ktor.network.tls

import io.ktor.network.sockets.*
import kotlinx.coroutines.io.*
import kotlin.coroutines.*

/**
 * Make [Socket] connection secure with TLS using [TLSConfig].
 */
suspend fun Socket.tls(
    coroutineContext: CoroutineContext, config: TLSConfig
): Socket {
    val reader = openReadChannel()
    val writer = openWriteChannel()

    return try {
        openTLSSession(this, reader, writer, config, coroutineContext)
    } catch (cause: Throwable) {
        reader.cancel(cause)
        writer.close(cause)
        close()
        throw cause
    }
}

/**
 * Make [Socket] connection secure with TLS configured with [block].
 */
suspend fun Socket.tls(coroutineContext: CoroutineContext, block: TLSConfigBuilder.() -> Unit = {}): Socket =
    tls(coroutineContext, TLSConfigBuilder().apply(block).build())
