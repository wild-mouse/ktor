//@file:kotlin.jvm.JvmMultifileClass
//@file:kotlin.jvm.JvmName("FrameKt")

package io.ktor.http.cio.websocket

import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.io.core.*
import kotlinx.io.core.ByteOrder
import java.nio.*

/**
 * A frame received or ready to be sent. It is not reusable and not thread-safe
 * @property fin is it final fragment, should be always `true` for control frames and if no fragmentation is used
 * @property frameType enum value
 * @property data - a frame content or fragment content
 * @property disposableHandle could be invoked when the frame is processed
 */
actual sealed class Frame private actual constructor(
    actual val fin: Boolean,
    actual val frameType: FrameType,
    actual val data: ByteArray,
    actual val disposableHandle: DisposableHandle
) {
    /**
     * Frame content
     */
    val buffer: ByteBuffer = ByteBuffer.wrap(data)

    /**
     * Represents an application level binary frame.
     * In a RAW web socket session a big text frame could be fragmented
     * (separated into several text frames so they have [fin] = false except the last one).
     * Note that usually there is no need to handle fragments unless you have a RAW web socket session.
     */
    actual class Binary actual constructor(fin: Boolean, data: ByteArray) : Frame(fin, FrameType.BINARY, data) {
        constructor(fin: Boolean, buffer: ByteBuffer) : this(fin, buffer.moveToByteArray())

        actual constructor(fin: Boolean, packet: ByteReadPacket) : this(fin, packet.readBytes())
    }

    /**
     * Represents an application level text frame.
     * In a RAW web socket session a big text frame could be fragmented
     * (separated into several text frames so they have [fin] = false except the last one).
     * Please note that a boundary between fragments could be in the middle of multi-byte (unicode) character
     * so don't apply String constructor to every fragment but use decoder loop instead of concatenate fragments first.
     * Note that usually there is no need to handle fragments unless you have a RAW web socket session.
     */
    actual class Text actual constructor(fin: Boolean, data: ByteArray) : Frame(fin, FrameType.TEXT, data) {
        actual constructor(text: String) : this(false, text.toByteArray())
        actual constructor(fin: Boolean, packet: ByteReadPacket) : this(false, packet.readBytes())
        constructor(fin: Boolean, buffer: ByteBuffer) : this(fin, buffer.moveToByteArray())
    }

    /**
     * Represents a low-level level close frame. It could be sent to indicate web socket session end.
     * Usually there is no need to send/handle it unless you have a RAW web socket session.
     */
    actual class Close actual constructor(data: ByteArray) : Frame(true, FrameType.CLOSE, data) {
        actual constructor(reason: CloseReason) : this(buildPacket {
            byteOrder = ByteOrder.BIG_ENDIAN
            writeShort(reason.code)
            writeStringUtf8(reason.message)
        })

        actual constructor(packet: ByteReadPacket) : this(packet.readBytes())
        actual constructor() : this(Empty)

        constructor(buffer: ByteBuffer) : this(buffer.moveToByteArray())
    }

    /**
     * Represents a low-level ping frame. Could be sent to test connection (peer should reply with [Pong]).
     * Usually there is no need to send/handle it unless you have a RAW web socket session.
     */
    actual class Ping actual constructor(data: ByteArray) : Frame(true, FrameType.PING, data) {
        actual constructor(packet: ByteReadPacket) : this(packet.readBytes())
        constructor(buffer: ByteBuffer) : this(buffer.moveToByteArray())
    }

    /**
     * Represents a low-level pong frame. Should be sent in reply to a [Ping] frame.
     * Usually there is no need to send/handle it unless you have a RAW web socket session.
     */
    actual class Pong actual constructor(
        data: ByteArray,
        disposableHandle: DisposableHandle
    ) : Frame(true, FrameType.PONG, data, disposableHandle) {
        actual constructor(packet: ByteReadPacket) : this(packet.readBytes())

        constructor(
            buffer: ByteBuffer,
            disposableHandle: DisposableHandle = NonDisposableHandle
        ) : this(buffer.moveToByteArray(), disposableHandle)
    }

    override fun toString() = "Frame $frameType (fin=$fin, buffer len = ${data.size})"

    /**
     * Creates a frame copy
     */
    actual fun copy(): Frame = byType(fin, frameType, data.copyOf())

    actual companion object {
        private val Empty: ByteArray = ByteArray(0)

        /**
         * Create a particular [Frame] instance by frame type
         */
        actual fun byType(
            fin: Boolean,
            frameType: FrameType,
            data: ByteArray
        ): Frame = when (frameType) {
            FrameType.BINARY -> Binary(fin, data)
            FrameType.TEXT -> Text(fin, data)
            FrameType.CLOSE -> Close(data)
            FrameType.PING -> Ping(data)
            FrameType.PONG -> Pong(data)
        }

        /**
         * Create a particular [Frame] instance by frame type
         */
        fun byType(fin: Boolean, frameType: FrameType, buffer: ByteBuffer): Frame =
            byType(fin, frameType, buffer.moveToByteArray())
    }
}

/**
 * Read close reason from close frame or null if no close reason provided
 */
fun Frame.Close.readReason(): CloseReason? {
    if (data.size < 2) {
        return null
    }

    buffer.mark()
    val code = buffer.short
    val message = buffer.decodeString(Charsets.UTF_8)
    buffer.reset()

    return CloseReason(code, message)
}
