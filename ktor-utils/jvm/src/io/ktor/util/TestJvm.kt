package io.ktor.util

import kotlinx.coroutines.*

/**
 * Multiplatform [runBlocking] alternative for running suspend tests.
 */

actual fun suspendTest(block: suspend () -> Unit) = runBlocking { block() }
