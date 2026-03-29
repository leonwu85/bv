package dev.aaa1115910.bv.player

import android.content.Context
import dev.aaa1115910.bv.player.util.IPv4PreferredDns
import okhttp3.OkHttpClient
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object OkHttpUtil {
    // 直播专用超时配置
    private const val LIVE_CONNECT_TIMEOUT = 8_000L
    private const val LIVE_READ_TIMEOUT = 15_000L
    private const val LIVE_WRITE_TIMEOUT = 15_000L

    // 点播默认超时配置
    private const val DEFAULT_CONNECT_TIMEOUT = 10_000L
    private const val DEFAULT_READ_TIMEOUT = 30_000L
    private const val DEFAULT_WRITE_TIMEOUT = 30_000L
    fun generateCustomSslOkHttpClient(context: Context): OkHttpClient {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val customCaMap = mapOf(
            "custom:r5" to "GlobalSign ECC Root CA R5.crt"
        )

        val keyStoreType = KeyStore.getDefaultType()
        val systemKeyStore = KeyStore.getInstance("AndroidCAStore").apply {
            load(null, null)
        }
        val customKeyStore = KeyStore.getInstance(keyStoreType).apply {
            load(null, null)

            systemKeyStore.aliases().toList().forEach {
                setCertificateEntry(it, systemKeyStore.getCertificate(it))
            }
            customCaMap.forEach { (alias, caFilename) ->
                val certificateInputStream = context.assets.open(caFilename)
                val certificate = certificateFactory.generateCertificate(certificateInputStream)
                setCertificateEntry(alias, certificate)
            }
        }

        val tmfAlgorithm: String = TrustManagerFactory.getDefaultAlgorithm()
        val trustManagerFactory: TrustManagerFactory =
            TrustManagerFactory.getInstance(tmfAlgorithm).apply {
                init(customKeyStore)
            }

        val sslContext: SSLContext = SSLContext.getInstance("TLS").apply {
            init(null, trustManagerFactory.trustManagers, null)
        }

        return OkHttpClient.Builder()
            .dns(IPv4PreferredDns)
            .sslSocketFactory(
                sslContext.socketFactory,
                trustManagerFactory.trustManagers[0] as X509TrustManager
            )
            .hostnameVerifier { hostname, session ->
                // 允许 bilivideo.com 和 bilivideo.cn 域名证书互通
                val biliDomains = listOf("bilivideo.com", "bilivideo.cn")
                val isBiliDomain = biliDomains.any { domain ->
                    hostname == domain || hostname.endsWith(".$domain")
                }
                if (isBiliDomain) {
                    true
                } else {
                    HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session)
                }
            }
            .build()
    }

    /**
     * 为直播创建优化的 OkHttpClient
     * 使用更短的超时时间，快速失败便于重试
     */
    fun generateLiveOkHttpClient(context: Context): OkHttpClient {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val customCaMap = mapOf(
            "custom:r5" to "GlobalSign ECC Root CA R5.crt"
        )

        val keyStoreType = KeyStore.getDefaultType()
        val systemKeyStore = KeyStore.getInstance("AndroidCAStore").apply {
            load(null, null)
        }
        val customKeyStore = KeyStore.getInstance(keyStoreType).apply {
            load(null, null)

            systemKeyStore.aliases().toList().forEach {
                setCertificateEntry(it, systemKeyStore.getCertificate(it))
            }
            customCaMap.forEach { (alias, caFilename) ->
                val certificateInputStream = context.assets.open(caFilename)
                val certificate = certificateFactory.generateCertificate(certificateInputStream)
                setCertificateEntry(alias, certificate)
            }
        }

        val tmfAlgorithm: String = TrustManagerFactory.getDefaultAlgorithm()
        val trustManagerFactory: TrustManagerFactory =
            TrustManagerFactory.getInstance(tmfAlgorithm).apply {
                init(customKeyStore)
            }

        val sslContext: SSLContext = SSLContext.getInstance("TLS").apply {
            init(null, trustManagerFactory.trustManagers, null)
        }

        return OkHttpClient.Builder()
            .dns(IPv4PreferredDns)
            .sslSocketFactory(
                sslContext.socketFactory,
                trustManagerFactory.trustManagers[0] as X509TrustManager
            )
            .hostnameVerifier { hostname, session ->
                val biliDomains = listOf("bilivideo.com", "bilivideo.cn")
                val isBiliDomain = biliDomains.any { domain ->
                    hostname == domain || hostname.endsWith(".$domain")
                }
                if (isBiliDomain) true
                else HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session)
            }
            // 直播专用超时配置
            .connectTimeout(LIVE_CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(LIVE_READ_TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(LIVE_WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}