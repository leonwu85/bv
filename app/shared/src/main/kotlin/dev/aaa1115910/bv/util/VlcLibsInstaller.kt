package dev.aaa1115910.bv.util

import android.content.Context
import java.io.File
import java.util.zip.ZipInputStream

object VlcLibsInstaller {
    private const val VLC_LIBS_DIR = "vlc_libs"
    private val requiredLibs = listOf("libvlc.so", "libc++_shared.so", "libvlcjni.so")

    /**
     * 检查 VLC 库是否已安装
     */
    fun isVlcLibsInstalled(context: Context): Boolean {
        val vlcLibsDir = File(context.filesDir, VLC_LIBS_DIR)
        if (!vlcLibsDir.exists()) return false

        return requiredLibs.all { libraryName ->
            NativeLibraryAbi.isCompatibleWithCurrentProcess(File(vlcLibsDir, libraryName))
        }
    }

    /**
     * 获取已安装的 VLC 库版本
     * @return 已安装的版本号，未安装返回空字符串
     */
    fun getInstalledVersion(context: Context): String {
        return Prefs.vlcLibsVersion
    }

    /**
     * 检查是否需要更新 VLC 库
     * @param expectedVersion 期望的版本号
     * @return true 表示需要更新（未安装或版本不匹配）
     */
    fun needsUpdate(context: Context, expectedVersion: String): Boolean {
        if (!isVlcLibsInstalled(context)) return true
        return getInstalledVersion(context) != expectedVersion
    }

    /**
     * 获取当前设备的目标 ABI
     */
    fun getTargetAbi(): String {
        return NativeLibraryAbi.currentProcessAbi()
    }

    /**
     * 从 AAR 中提取并安装 so 文件
     */
    fun installFromAar(aarFile: File, targetDir: File, targetAbi: String) {
        targetDir.mkdirs()

        ZipInputStream(aarFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                // 只提取目标 ABI 的 so 文件
                // 路径格式: jni/arm64-v8a/libvlc.so
                if (entry.name.startsWith("jni/$targetAbi/") && entry.name.endsWith(".so")) {
                    val fileName = entry.name.substringAfterLast("/")
                    val targetFile = File(targetDir, fileName)
                    targetFile.outputStream().use { fos ->
                        zis.copyTo(fos)
                    }
                }
                entry = zis.nextEntry
            }
        }

        // 验证所有必需文件是否已安装
        val installedLibs = targetDir.listFiles()?.map { it.name } ?: emptyList()
        val missingLibs = requiredLibs.filter { it !in installedLibs }
        if (missingLibs.isNotEmpty()) {
            throw IllegalStateException("Missing libs: $missingLibs")
        }
    }
}
