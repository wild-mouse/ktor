package io.ktor.network.tls

import java.security.*
import java.security.cert.*
import javax.net.ssl.*

/**
 * @property trustManager: Custom [X509TrustManager] to verify server authority. Use system by default.
 */
class TLSConfig(
    val random: SecureRandom,
    val chains: List<CertificateAndKey>,
    val trustManager: TrustManager,
    val cipherSuites: List<CipherSuite>,
    val serverName: String?
)

class CertificateAndKey(val chain: Array<X509Certificate>, val key: PrivateKey)

class TLSConfigBuilder {
    val certificates: MutableList<CertificateAndKey> = mutableListOf<CertificateAndKey>()

    fun addCertificateChain(chain: Array<X509Certificate>, key: PrivateKey) {
        certificates += CertificateAndKey(chain, key)
    }

    var random: SecureRandom? = null

    /**
     * Custom [X509TrustManager] to verify server authority.
     *
     * Use system by default.
     */
    var trustManager: TrustManager? = null

    var cipherSuites: List<CipherSuite> = CIOCipherSuites.SupportedSuites

    var serverName: String? = null

    fun build() = TLSConfig(
        random ?: SecureRandom.getInstanceStrong(),
        certificates, trustManager ?: findTrustManager(),
        cipherSuites, serverName
    )
}

fun TLSConfigBuilder.addKeyStore(store: KeyStore, password: CharArray) {
    val keyManagerAlgorithm = KeyManagerFactory.getDefaultAlgorithm()!!
    val keyManagerFactory = KeyManagerFactory.getInstance(keyManagerAlgorithm)!!

    keyManagerFactory.init(store, password)
    val managers = keyManagerFactory.keyManagers.filterIsInstance<X509KeyManager>()


    val aliases = store.aliases()!!
    loop@ for (alias in aliases) {
        val chain = store.getCertificateChain(alias)

        val allX509 = chain.all { it is X509Certificate }
        check(allX509) { "Fail to add key store $store. Only X509 certificate format supported." }

        for (manager in managers) {
            val key = manager.getPrivateKey(alias) ?: continue

            val map = chain.map { it as X509Certificate }
            addCertificateChain(map.toTypedArray(), key)
            continue@loop
        }

        throw NoPrivateKeyException(alias, store)
    }
}

/**
 * Throws if failed to find [PrivateKey] for any alias in [KeyStore].
 */
class NoPrivateKeyException(
    alias: String, store: KeyStore
) : IllegalStateException("Failed to find private key for alias $alias. Please check your key store: $store")

private fun findTrustManager(): TrustManager {
    val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())!!
    factory.init(null as KeyStore?)
    val manager = factory.trustManagers!!

    return manager.filterIsInstance<X509TrustManager>().first()
}

