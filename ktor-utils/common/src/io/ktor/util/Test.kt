package io.ktor.util

/**
 * Multiplatform [runBlocking] alternative for running suspend tests.
 */
@InternalAPI
expect fun suspendTest(block: suspend () -> Unit)
