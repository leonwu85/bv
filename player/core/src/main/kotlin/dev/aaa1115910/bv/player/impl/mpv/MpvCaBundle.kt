package dev.aaa1115910.bv.player.impl.mpv

import android.content.Context
import android.os.Build
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.encoding.Base64

/**
 * Exports the Android system trust store as a PEM bundle for libmpv.
 *
 * mpv-android's libmpv talks HTTP(S) through libcurl + mbedtls. mbedtls has no notion of the
 * platform trust store, and mpv verifies TLS peers by default (`--tls-verify=yes`), so every
 * `https://` stream fails with "TLS certificate verification failed" unless `--tls-ca-file`
 * points at a PEM bundle. Rather than shipping (and having to update) a Mozilla bundle, we write
 * the device's own system CAs to a file and refresh it whenever the OS build changes.
 *
 * Only `system:` aliases are exported. User-installed CAs are deliberately skipped: on a TV box
 * they are almost always an interception proxy, which is exactly what verification should catch.
 */
object MpvCaBundle {
    private const val DIR_NAME = "mpv"
    private const val FILE_NAME = "cacert.pem"
    private const val STAMP_FILE_NAME = "cacert.stamp"
    private const val ANDROID_CA_STORE = "AndroidCAStore"
    private const val SYSTEM_ALIAS_PREFIX = "system:"
    private const val PEM_LINE_LENGTH = 64
    private const val FORMAT_VERSION = 1

    private val logger = KotlinLogging.logger { }
    private val refreshExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "mpv-ca-bundle").apply { isDaemon = true }
    }
    private val refreshing = AtomicBoolean(false)

    /**
     * Returns the bundle path, writing it synchronously when it does not exist yet (mpv reads the
     * file per connection, so it must be complete before the first `loadfile`). When a bundle exists
     * but was written for a different OS build, the stale file is returned immediately and replaced
     * atomically in the background. Returns null when the bundle cannot be produced at all.
     */
    fun ensure(context: Context): File? {
        val bundle = bundleFile(context)
        val stamp = currentStamp()
        val stampFile = File(bundle.parentFile, STAMP_FILE_NAME)
        val existingStamp = stampFile.takeIf { it.isFile }?.runCatching { readText() }?.getOrNull()

        if (bundle.isFile && bundle.length() > 0L) {
            if (existingStamp != stamp) refreshAsync(context, stamp)
            return bundle
        }

        return runCatching { writeBundle(bundle, stampFile, stamp) }
            .onFailure { logger.error(it) { "Failed to export system CA bundle for mpv" } }
            .getOrNull()
    }

    fun bundleFile(context: Context): File = File(File(context.filesDir, DIR_NAME), FILE_NAME)

    /** PEM encoding of a DER certificate: base64 wrapped at 64 columns between the standard markers. */
    fun toPem(der: ByteArray): String = buildString {
        append("-----BEGIN CERTIFICATE-----\n")
        Base64.Default.encode(der).chunked(PEM_LINE_LENGTH).forEach { line ->
            append(line)
            append('\n')
        }
        append("-----END CERTIFICATE-----\n")
    }

    private fun refreshAsync(context: Context, stamp: String) {
        if (!refreshing.compareAndSet(false, true)) return
        refreshExecutor.execute {
            try {
                val bundle = bundleFile(context)
                writeBundle(bundle, File(bundle.parentFile, STAMP_FILE_NAME), stamp)
            } catch (error: Exception) {
                logger.warn(error) { "Failed to refresh system CA bundle for mpv" }
            } finally {
                refreshing.set(false)
            }
        }
    }

    private fun writeBundle(bundle: File, stampFile: File, stamp: String): File {
        val certificates = loadSystemCertificates()
        require(certificates.isNotEmpty()) { "System trust store returned no certificates" }

        bundle.parentFile?.mkdirs()
        val temp = File(bundle.parentFile, "$FILE_NAME.tmp")
        temp.bufferedWriter().use { writer ->
            certificates.forEach { certificate ->
                writer.write("# ${certificate.subjectX500Principal.name}\n")
                writer.write(toPem(certificate.encoded))
            }
        }
        // Rename is atomic on the same filesystem; a concurrent mpv connection sees old or new, never partial.
        if (!temp.renameTo(bundle)) {
            temp.copyTo(bundle, overwrite = true)
            temp.delete()
        }
        stampFile.writeText(stamp)
        logger.info { "Exported ${certificates.size} system CA certificates for mpv to $bundle" }
        return bundle
    }

    private fun loadSystemCertificates(): List<X509Certificate> {
        val keyStore = KeyStore.getInstance(ANDROID_CA_STORE).apply { load(null) }
        val aliases = keyStore.aliases().toList().filter { it.startsWith(SYSTEM_ALIAS_PREFIX) }
        return aliases.mapNotNull { alias ->
            runCatching { keyStore.getCertificate(alias) as? X509Certificate }
                .onFailure { logger.debug { "Skipping unreadable CA $alias: ${it.message}" } }
                .getOrNull()
        }
    }

    private fun currentStamp(): String {
        return "v$FORMAT_VERSION|${Build.FINGERPRINT}|${Build.VERSION.SECURITY_PATCH}"
    }
}
