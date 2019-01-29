package io.ktor.util

import kotlinx.coroutines.*

/**
 * Multiplatform [runBlocking] alternative for running suspend tests.
 */
actual fun suspendTest(block: suspend () -> Unit): dynamic {
    return GlobalScope.async {
        block()
    }.asPromise()
}
