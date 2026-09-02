package dev.aaa1115910.bv.player.impl.mpv

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dev.aaa1115910.bv.player.core.BuildConfig
import dev.aaa1115910.bv.player.impl.NativeCxxRuntime
import dev.aaa1115910.bv.util.NativeLibraryAbi
import `is`.xyz.mpv.MPVLib
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * Installs and loads the mpv-android native libraries (`libmpv.so` + `libplayer.so` JNI glue).
 *
 * The libraries are extracted from the official mpv-android release APK. Because they contain
 * executable code that is downloaded at runtime (possibly via third-party GitHub proxies), the APK
 * must be signed by the mpv-android release certificate before anything is extracted, and the
 * installed version must match the release tag the `is.xyz.mpv.MPVLib` JNI declarations in this
 * module were written against.
 */
object MpvLibsInstaller {
    private const val MPV_LIBS_DIR = "mpv_libs"
    private const val VERSION_FILE = "version.txt"
    private const val MPV_ANDROID_PACKAGE = "is.xyz.mpv"
    private val logger = KotlinLogging.logger { }

    private val requiredLibs = listOf("libmpv.so", "libplayer.so")
    /** `libc++_shared.so` is deliberately absent: it is loaded once per process by [NativeCxxRuntime]. */
    private val preferredLoadOrder = listOf(
        "libavutil.so",
        "libswresample.so",
        "libswscale.so",
        "libavcodec.so",
        "libavformat.so",
        "libavfilter.so",
        "libavdevice.so",
        "libpostproc.so",
        "libmpv.so",
        "libplayer.so"
    )

    @Volatile
    private var libsLoaded = false

    /** The mpv-android release tag this build expects (see AppConfiguration.mpvAndroidReleaseTag). */
    val expectedVersion: String
        get() = BuildConfig.mpvAndroidReleaseTag

    fun getLibsDir(context: Context): File = File(context.filesDir, MPV_LIBS_DIR)

    fun isInstalled(context: Context): Boolean {
        val libsDir = getLibsDir(context)
        if (!libsDir.exists()) return false

        return requiredLibs.all { libraryName ->
            NativeLibraryAbi.isCompatibleWithCurrentProcess(File(libsDir, libraryName))
        }
    }

    fun needsInstall(context: Context): Boolean = !isInstalled(context)

    /** Not installed, or installed from a different (or unknown/"latest") release than [expectedVersion]. */
    fun needsUpdate(context: Context): Boolean {
        if (!isInstalled(context)) return true
        return getInstalledVersion(context) != expectedVersion
    }

    fun getInstalledVersion(context: Context): String {
        val versionFile = File(getLibsDir(context), VERSION_FILE)
        return versionFile.takeIf { it.isFile }?.readText()?.trim().orEmpty()
    }

    fun getTargetAbi(): String {
        return NativeLibraryAbi.currentProcessAbi()
    }

    /**
     * Verifies [apkFile] is a genuine mpv-android release APK and extracts the `.so` entries under
     * `lib/<abi>/` into [targetDir] atomically (staging directory + rename), so a process that has
     * the previous libraries mapped never sees them overwritten in place.
     */
    @Throws(SecurityException::class, IllegalStateException::class)
    fun installFromApk(
        context: Context,
        apkFile: File,
        targetDir: File,
        targetAbi: String,
        version: String
    ) {
        require(apkFile.isFile) { "APK does not exist: ${apkFile.absolutePath}" }
        verifyApkSignature(context, apkFile)

        val tempDir = File(targetDir.parentFile, "${targetDir.name}_tmp")
        tempDir.deleteRecursively()
        tempDir.mkdirs()

        ZipInputStream(apkFile.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.startsWith("lib/$targetAbi/") && entry.name.endsWith(".so")) {
                    // substringAfterLast guards against path traversal in crafted entry names
                    val fileName = entry.name.substringAfterLast("/")
                    val targetFile = File(tempDir, fileName)
                    targetFile.outputStream().use { output ->
                        zip.copyTo(output)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val installedLibs = tempDir.listFiles()?.map { it.name }.orEmpty()
        val missingLibs = requiredLibs.filter { it !in installedLibs }
        if (missingLibs.isNotEmpty()) {
            tempDir.deleteRecursively()
            throw IllegalStateException("Missing MPV libs for $targetAbi: $missingLibs")
        }

        File(tempDir, VERSION_FILE).writeText(version)

        targetDir.deleteRecursively()
        if (!tempDir.renameTo(targetDir)) {
            tempDir.copyRecursively(targetDir, overwrite = true)
            tempDir.deleteRecursively()
        }
    }

    /**
     * Checks that the APK carries a valid signature (the platform verifies the v1/v2/v3 signature
     * over the whole file while parsing) whose certificate matches the pinned mpv-android release
     * certificate, and that it declares the mpv-android package name.
     */
    @Throws(SecurityException::class)
    fun verifyApkSignature(context: Context, apkFile: File) {
        val expected = BuildConfig.mpvAndroidSigningCertSha256.uppercase()
        val packageManager = context.packageManager
        val path = apkFile.absolutePath

        val signatures: List<ByteArray>
        val packageName: String?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = packageManager.getPackageArchiveInfo(path, PackageManager.GET_SIGNING_CERTIFICATES)
                ?: throw SecurityException("无法解析下载的 MPV 组件（签名校验失败）")
            packageName = info.packageName
            val signingInfo = info.signingInfo
                ?: throw SecurityException("下载的 MPV 组件没有签名信息")
            signatures = if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners.map { it.toByteArray() }
            } else {
                signingInfo.signingCertificateHistory.map { it.toByteArray() }
            }
        } else {
            @Suppress("DEPRECATION")
            val info = packageManager.getPackageArchiveInfo(path, PackageManager.GET_SIGNATURES)
                ?: throw SecurityException("无法解析下载的 MPV 组件（签名校验失败）")
            packageName = info.packageName
            @Suppress("DEPRECATION")
            signatures = info.signatures?.map { it.toByteArray() }.orEmpty()
        }

        if (packageName != MPV_ANDROID_PACKAGE) {
            throw SecurityException("下载的 APK 不是 mpv-android（package=$packageName）")
        }
        val fingerprints = signatures.map { sha256Hex(it) }
        if (fingerprints.isEmpty() || fingerprints.none { it == expected }) {
            throw SecurityException(
                "MPV 组件签名与官方证书不匹配（got=${fingerprints.joinToString().ifEmpty { "<none>" }}）"
            )
        }
        logger.info { "Verified mpv-android APK signature: ${fingerprints.first()}" }
    }

    private fun sha256Hex(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02X".format(it) }
    }

    /** Component name used in [NativeCxxRuntime] diagnostics. */
    const val COMPONENT_NAME = "MPV"

    /**
     * Loads the mpv libraries once per process. Installed libraries whose recorded version differs
     * from [expectedVersion] are refused (their JNI ABI may not match `MPVLib`); callers should
     * surface the resulting error and prompt for a re-download.
     *
     * @throws dev.aaa1115910.bv.player.impl.NativeRuntimeConflictException when another component
     *   already loaded an older libc++ into this process; only a process restart fixes that.
     */
    @Synchronized
    @Throws(IllegalStateException::class, UnsatisfiedLinkError::class)
    fun loadNativeLibs(context: Context) {
        if (libsLoaded) {
            logger.debug { "MPV libs already loaded" }
            return
        }

        val libsDir = getLibsDir(context)
        if (isInstalled(context)) {
            val installedVersion = getInstalledVersion(context)
            if (installedVersion != expectedVersion) {
                throw IllegalStateException(
                    "已安装的 MPV 组件（${installedVersion.ifBlank { "未知版本" }}）与当前应用需要的版本" +
                        "（$expectedVersion）不一致，请在设置中重新下载 MPV 组件"
                )
            }
            logger.info { "Loading MPV libs from: $libsDir (version=$installedVersion)" }
            NativeCxxRuntime.ensureLoadedFor(context, COMPONENT_NAME, File(libsDir, NativeCxxRuntime.LIBRARY_NAME))
            loadFromDirectory(libsDir)
            MPVLib.markLibrariesLoaded()
        } else {
            logger.info { "Loading MPV libs from APK" }
            MPVLib.loadLibraries()
        }
        libsLoaded = true
    }

    /** Whether the libraries in the install directory can be used without restarting the process. */
    fun restartRequiredReason(context: Context): String? {
        return NativeCxxRuntime.restartRequiredReason(
            COMPONENT_NAME,
            File(getLibsDir(context), NativeCxxRuntime.LIBRARY_NAME)
        )
    }

    private fun loadFromDirectory(libsDir: File) {
        val libraries = libsDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".so") && it.name != NativeCxxRuntime.LIBRARY_NAME }
            .orEmpty()
            .associateBy { it.name }

        val loaded = mutableSetOf<String>()
        preferredLoadOrder.forEach { libName ->
            libraries[libName]?.let { libFile ->
                loadLibrary(libFile)
                loaded += libName
            }
        }

        libraries
            .filterKeys { it !in loaded }
            .values
            .sortedBy { it.name }
            .forEach { libFile -> loadLibrary(libFile) }
    }

    private fun loadLibrary(libFile: File) {
        try {
            System.load(libFile.absolutePath)
        } catch (error: UnsatisfiedLinkError) {
            // dlopen's message names the missing symbol/library; keep it, but say which file we were loading.
            throw UnsatisfiedLinkError("加载 ${libFile.name} 失败：${error.message}").initCause(error) as UnsatisfiedLinkError
        }
    }
}
