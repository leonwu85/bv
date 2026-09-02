package dev.aaa1115910.bv.player.impl.vlc

import android.content.Context
import dev.aaa1115910.bv.player.core.BuildConfig
import dev.aaa1115910.bv.player.impl.NativeCxxRuntime
import dev.aaa1115910.bv.player.impl.NativeRuntimeConflictException
import dev.aaa1115910.bv.util.NativeLibraryAbi
import io.github.oshai.kotlinlogging.KotlinLogging
import org.videolan.libvlc.LibVLC
import java.io.File

/**
 * Location and validation helpers for the on-demand VLC native libraries.
 *
 * The APK deliberately excludes `libvlc.so`/`libvlcjni.so`/`libc++_shared.so`; they are downloaded
 * from the `libvlc-all` AAR into [libsDir] by the installer in `app/shared`. This object is the
 * single source of truth for that layout so the installer and the loader cannot drift apart.
 */
object VlcNativeLibs {
    private const val LIBS_DIR = "vlc_libs"
    private const val VERSION_FILE = "version.txt"
    private const val LIBVLC_LOADED_FIELD = "sLoaded"
    private val logger = KotlinLogging.logger { }

    /** Component name used in [NativeCxxRuntime] diagnostics. */
    const val COMPONENT_NAME = "VLC"

    /** Load order matters: libvlc depends on libc++_shared, libvlcjni depends on libvlc. */
    val requiredLibs: List<String> = listOf(NativeCxxRuntime.LIBRARY_NAME, "libvlc.so", "libvlcjni.so")

    /** Version the Java layer in this build was compiled against. */
    val expectedVersion: String
        get() = BuildConfig.libVLCVersion

    @Volatile
    private var libsLoaded = false
    private val loadLock = Any()

    fun libsDir(context: Context): File = File(context.filesDir, LIBS_DIR)

    fun versionFile(libsDir: File): File = File(libsDir, VERSION_FILE)

    /** Version recorded by the installer, or null for directories written by older app versions. */
    fun readInstalledVersion(libsDir: File): String? {
        return versionFile(libsDir)
            .takeIf { it.isFile }
            ?.runCatching { readText().trim() }
            ?.getOrNull()
            ?.takeIf { it.isNotEmpty() }
    }

    /** All required libraries exist and match the bitness of this process. */
    fun hasCompatibleLibraries(libsDir: File): Boolean {
        return requiredLibs.all { NativeLibraryAbi.isCompatibleWithCurrentProcess(File(libsDir, it)) }
    }

    fun isInstalled(context: Context): Boolean = hasCompatibleLibraries(libsDir(context))

    /** Non-null when the installed libraries need a newer libc++ than this process already runs. */
    fun restartRequiredReason(context: Context): String? {
        return NativeCxxRuntime.restartRequiredReason(
            COMPONENT_NAME,
            File(libsDir(context), NativeCxxRuntime.LIBRARY_NAME)
        )
    }

    /**
     * Whether the installed libraries can be used by this build. A missing version file is
     * accepted (legacy installs only recorded the version in preferences); a mismatching one is not.
     */
    fun isInstalledVersionUsable(context: Context): Boolean {
        val libsDir = libsDir(context)
        if (!hasCompatibleLibraries(libsDir)) return false
        val installed = readInstalledVersion(libsDir) ?: return true
        return installed == expectedVersion
    }

    /**
     * Loads the VLC native libraries exactly once per process.
     *
     * Prefers the downloaded libraries in [libsDir]; falls back to libraries bundled in the APK.
     * Throws [UnsatisfiedLinkError] when neither is usable so that callers can fall back to another
     * player instead of ending up with a half-initialised LibVLC.
     */
    @Throws(UnsatisfiedLinkError::class)
    fun load(context: Context) {
        if (libsLoaded) return
        synchronized(loadLock) {
            if (libsLoaded) return

            val libsDir = libsDir(context)
            val downloaded = requiredLibs.map { File(libsDir, it) }
            val downloadedReady = hasCompatibleLibraries(libsDir)
            val installedVersion = readInstalledVersion(libsDir)

            when {
                downloadedReady && installedVersion != null && installedVersion != expectedVersion -> {
                    throw UnsatisfiedLinkError(
                        "Installed VLC libraries ($installedVersion) do not match the Java layer " +
                            "($expectedVersion); the component must be downloaded again"
                    )
                }

                downloadedReady -> {
                    logger.info { "Loading VLC libs from $libsDir (version=${installedVersion ?: "legacy"})" }
                    // libc++ is shared with MPV: exactly one copy per process, the newest installed one.
                    try {
                        NativeCxxRuntime.ensureLoadedFor(
                            context,
                            COMPONENT_NAME,
                            File(libsDir, NativeCxxRuntime.LIBRARY_NAME)
                        )
                    } catch (conflict: NativeRuntimeConflictException) {
                        // Keep the documented contract (LinkageError => Media3 fallback in VlcPlayerFactory).
                        throw UnsatisfiedLinkError(conflict.message).initCause(conflict)
                    }
                    downloaded
                        .filter { it.name != NativeCxxRuntime.LIBRARY_NAME }
                        .forEach { System.load(it.absolutePath) }
                }

                else -> {
                    if (downloaded.any { it.exists() }) {
                        logger.warn { "Ignoring incomplete or wrong-bitness VLC libraries in $libsDir" }
                    }
                    logger.info { "Loading VLC libs bundled in the APK" }
                    runCatching { System.loadLibrary("c++_shared") }
                    // Throws UnsatisfiedLinkError when the APK does not bundle VLC; VlcPlayerFactory
                    // turns that into a Media3 fallback.
                    System.loadLibrary("vlc")
                    System.loadLibrary("vlcjni")
                }
            }

            markLibVlcLibrariesLoaded()
            libsLoaded = true
        }
    }

    /**
     * `LibVLC(Context, List)` unconditionally runs the static `LibVLC.loadLibraries()`, which calls
     * `System.loadLibrary("vlc")` and, on `UnsatisfiedLinkError`, `System.exit(1)`. Because the APK
     * does not bundle the libraries and `System.loadLibrary` only started resolving already-loaded
     * sonames on Android 11, that path kills the process on Android 6–10. We have already loaded the
     * libraries by absolute path, so flip the guard field and skip the official loader entirely
     * (this also avoids a second `JNI_OnLoad` of libvlcjni on Android 11+).
     */
    private fun markLibVlcLibrariesLoaded() {
        try {
            val field = LibVLC::class.java.getDeclaredField(LIBVLC_LOADED_FIELD)
            field.isAccessible = true
            field.setBoolean(null, true)
        } catch (e: Exception) {
            logger.warn(e) { "Unable to mark LibVLC libraries as loaded; LibVLC.loadLibraries() will run" }
        }
    }
}
