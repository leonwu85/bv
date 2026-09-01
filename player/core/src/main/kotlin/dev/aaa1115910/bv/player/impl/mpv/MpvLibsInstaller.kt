package dev.aaa1115910.bv.player.impl.mpv

import android.content.Context
import dev.aaa1115910.bv.util.NativeLibraryAbi
import `is`.xyz.mpv.MPVLib
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.util.zip.ZipInputStream

object MpvLibsInstaller {
    private const val MPV_LIBS_DIR = "mpv_libs"
    private const val VERSION_FILE = "version.txt"
    private val logger = KotlinLogging.logger { }

    private val requiredLibs = listOf("libmpv.so", "libplayer.so")
    private val preferredLoadOrder = listOf(
        "libc++_shared.so",
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

    fun getLibsDir(context: Context): File = File(context.filesDir, MPV_LIBS_DIR)

    fun isInstalled(context: Context): Boolean {
        val libsDir = getLibsDir(context)
        if (!libsDir.exists()) return false

        return requiredLibs.all { libraryName ->
            NativeLibraryAbi.isCompatibleWithCurrentProcess(File(libsDir, libraryName))
        }
    }

    fun needsInstall(context: Context): Boolean = !isInstalled(context)

    fun getInstalledVersion(context: Context): String {
        val versionFile = File(getLibsDir(context), VERSION_FILE)
        return versionFile.takeIf { it.isFile }?.readText()?.trim().orEmpty()
    }

    fun getTargetAbi(): String {
        return NativeLibraryAbi.currentProcessAbi()
    }

    fun installFromApk(
        apkFile: File,
        targetDir: File,
        targetAbi: String,
        version: String
    ) {
        require(apkFile.isFile) { "APK does not exist: ${apkFile.absolutePath}" }

        val tempDir = File(targetDir.parentFile, "${targetDir.name}_tmp")
        tempDir.deleteRecursively()
        tempDir.mkdirs()

        ZipInputStream(apkFile.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.startsWith("lib/$targetAbi/") && entry.name.endsWith(".so")) {
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

    @Synchronized
    fun loadNativeLibs(context: Context) {
        if (libsLoaded) {
            logger.debug { "MPV libs already loaded" }
            return
        }

        val libsDir = getLibsDir(context)
        if (isInstalled(context)) {
            logger.info { "Loading MPV libs from: $libsDir" }
            loadFromDirectory(libsDir)
            MPVLib.markLibrariesLoaded()
        } else {
            logger.info { "Loading MPV libs from APK" }
            MPVLib.loadLibraries()
        }
        libsLoaded = true
    }

    private fun loadFromDirectory(libsDir: File) {
        val libraries = libsDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".so") }
            .orEmpty()
            .associateBy { it.name }

        val loaded = mutableSetOf<String>()
        preferredLoadOrder.forEach { libName ->
            libraries[libName]?.let { libFile ->
                System.load(libFile.absolutePath)
                loaded += libName
            }
        }

        libraries
            .filterKeys { it !in loaded }
            .values
            .sortedBy { it.name }
            .forEach { libFile ->
                System.load(libFile.absolutePath)
            }
    }
}
