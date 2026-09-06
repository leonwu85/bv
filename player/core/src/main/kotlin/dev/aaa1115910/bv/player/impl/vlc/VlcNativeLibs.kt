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
    private val logger = KotlinLogging.logger { }

    /** Component name used in [NativeCxxRuntime] diagnostics. */
    const val COMPONENT_NAME = "VLC"

    /** Load order matters: libvlc depends on libc++_shared, libvlcjni depends on libvlc. */
    val requiredLibs: List<String> = listOf(NativeCxxRuntime.LIBRARY_NAME, "libvlc.so", "libvlcjni.so")

    /** Downloadable `libvlc-all` line the app ships with by default (VLC 3). */
    val defaultVersion: String
        get() = BuildConfig.libVLCVersion

    /** VLC 4 preview line. */
    val vlc4Version: String
        get() = BuildConfig.libVLC4Version

    /**
     * Every `libvlc-all` version the Java layer in `:player:libvlcjni` can drive. Anything else
     * (including the "latest" a mirror might hand out) is refused by [load].
     */
    val supportedVersions: List<String>
        get() = listOf(defaultVersion, vlc4Version)

    fun isSupportedVersion(version: String?): Boolean = version != null && version in supportedVersions

    /** SHA-256 of the `libvlc-all` AAR for a supported [version], or null for unknown versions. */
    fun aarSha256(version: String): String? = when (version) {
        defaultVersion -> BuildConfig.libVLCAarSha256
        vlc4Version -> BuildConfig.libVLC4AarSha256
        else -> null
    }

    /** Human readable label for a version choice (major line + stability). */
    fun describeVersion(version: String): String = when {
        version == vlc4Version -> "$version（VLC 4 预览版）"
        version == defaultVersion -> "$version（VLC 3 稳定版）"
        else -> version
    }

    /**
     * Backwards compatible alias of [defaultVersion]. The Java layer no longer targets a single
     * version; callers that need the user's choice should read it from preferences.
     */
    @Deprecated("Use defaultVersion or the selected version from preferences", ReplaceWith("defaultVersion"))
    val expectedVersion: String
        get() = defaultVersion

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
     * accepted (legacy 3.6.x installs only recorded the version in preferences and share the VLC 3
     * JNI surface); a version outside [supportedVersions] is not.
     */
    fun isInstalledVersionUsable(context: Context): Boolean {
        val libsDir = libsDir(context)
        if (!hasCompatibleLibraries(libsDir)) return false
        val installed = readInstalledVersion(libsDir) ?: return true
        return isSupportedVersion(installed)
    }

    /**
     * The `libvlc-all` version whose libraries are loaded in this process, or null before [load].
     * Legacy installs without a version file report [defaultVersion]'s major line as "3.x".
     */
    @Volatile
    var loadedVersion: String? = null
        private set

    /** True once VLC 4 native libraries are loaded in this process. */
    val isVlc4Loaded: Boolean
        get() = libsLoaded && runCatching { !LibVLC.isVlc3() }.getOrDefault(false)

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
                downloadedReady && installedVersion != null && !isSupportedVersion(installedVersion) -> {
                    throw UnsatisfiedLinkError(
                        "Installed VLC libraries ($installedVersion) are not supported by this build " +
                            "(${supportedVersions.joinToString()}); the component must be downloaded again"
                    )
                }

                downloadedReady -> {
                    VlcNativeCompatibility.unsupportedReason(
                        installedVersion,
                        NativeLibraryAbi.currentProcessAbi(),
                    )?.let { throw UnsatisfiedLinkError(it) }
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
                    loadedVersion = installedVersion ?: "3.x"
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
                    loadedVersion = "bundled"
                }
            }

            // The libraries were loaded by absolute path; tell the (forked) Java layer so that
            // LibVLC(Context, List) does not run System.loadLibrary("vlc") a second time.
            LibVLC.markLibrariesLoaded()
            libsLoaded = true
            logger.info {
                "VLC natives ready: version=$loadedVersion, core=${runCatching { LibVLC.version() }.getOrDefault("?")}"
            }
        }
    }
}
