package dev.aaa1115910.bv.util

import android.content.Context
import dev.aaa1115910.bv.player.impl.vlc.VlcNativeLibs
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * Installs the on-demand VLC native libraries extracted from the `libvlc-all` AAR.
 *
 * Layout and validation rules live in [VlcNativeLibs] (`:player:core`) so the loader and the
 * installer always agree; this object adds download integrity checks and the preference bookkeeping
 * used by the settings UI.
 */
object VlcLibsInstaller {
    private val requiredLibs = VlcNativeLibs.requiredLibs

    /** 检查 VLC 库是否已安装（文件齐全且与当前进程位数一致） */
    fun isVlcLibsInstalled(context: Context): Boolean = VlcNativeLibs.isInstalled(context)

    /**
     * 获取已安装的 VLC 库版本：优先读取库目录中的版本文件，回退到旧版本写入的偏好值
     * @return 已安装的版本号，未安装返回空字符串
     */
    fun getInstalledVersion(context: Context): String {
        return VlcNativeLibs.readInstalledVersion(VlcNativeLibs.libsDir(context)) ?: Prefs.vlcLibsVersion
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

    /** 获取当前设备的目标 ABI */
    fun getTargetAbi(): String {
        return NativeLibraryAbi.currentProcessAbi()
    }

    /**
     * 校验下载的 AAR 与构建时为该版本固定的 SHA-256 一致（AppConfiguration.libVLCAarSha256 /
     * libVLC4AarSha256）。镜像源不可信，且原生库会被直接加载执行，因此不匹配时拒绝安装；
     * 不在支持列表内的版本没有可比对的校验值，同样拒绝。
     */
    @Throws(SecurityException::class)
    fun verifyAarChecksum(aarFile: File, version: String) {
        val expected = VlcNativeLibs.aarSha256(version)?.lowercase()
            ?: throw SecurityException(
                "不支持的 VLC 组件版本 $version（可用：${VlcNativeLibs.supportedVersions.joinToString()}）"
            )
        val actual = sha256Hex(aarFile)
        if (actual != expected) {
            throw SecurityException("VLC 组件校验失败：SHA-256 不匹配（expected=$expected, actual=$actual）")
        }
    }

    /**
     * 从 AAR 中提取并安装 so 文件。
     *
     * 先写入临时目录再整体 rename 替换，避免覆盖当前进程已 mmap 的旧库导致 SIGBUS；
     * 同时写入版本文件供加载侧校验。
     */
    @Throws(SecurityException::class, IllegalStateException::class)
    fun installFromAar(aarFile: File, targetDir: File, targetAbi: String, version: String) {
        require(aarFile.isFile) { "AAR does not exist: ${aarFile.absolutePath}" }
        verifyAarChecksum(aarFile, version)

        val tempDir = File(targetDir.parentFile, "${targetDir.name}_tmp")
        tempDir.deleteRecursively()
        tempDir.mkdirs()

        ZipInputStream(aarFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                // 只提取目标 ABI 的 so 文件，路径格式: jni/arm64-v8a/libvlc.so
                if (entry.name.startsWith("jni/$targetAbi/") && entry.name.endsWith(".so")) {
                    // substringAfterLast 同时防止条目名中的路径穿越
                    val fileName = entry.name.substringAfterLast("/")
                    val targetFile = File(tempDir, fileName)
                    targetFile.outputStream().use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        // 验证所有必需文件是否已安装
        val installedLibs = tempDir.listFiles()?.map { it.name } ?: emptyList()
        val missingLibs = requiredLibs.filter { it !in installedLibs }
        if (missingLibs.isNotEmpty()) {
            tempDir.deleteRecursively()
            throw IllegalStateException("Missing libs: $missingLibs")
        }

        VlcNativeLibs.versionFile(tempDir).writeText(version)

        targetDir.deleteRecursively()
        if (!tempDir.renameTo(targetDir)) {
            tempDir.copyRecursively(targetDir, overwrite = true)
            tempDir.deleteRecursively()
        }
    }

    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
