package dev.aaa1115910.bv.util

import android.os.Build
import android.os.Process
import java.io.File

/** Selects native libraries for the bitness of this app process, not the device as a whole. */
object NativeLibraryAbi {
    private val known64BitAbis = setOf("arm64-v8a", "x86_64")
    private val known32BitAbis = setOf("armeabi-v7a", "x86")

    fun currentProcessAbi(): String {
        return selectProcessAbi(
            is64Bit = Process.is64Bit(),
            supportedAbis = Build.SUPPORTED_ABIS.asIterable(),
            supported64BitAbis = Build.SUPPORTED_64_BIT_ABIS.asIterable(),
            supported32BitAbis = Build.SUPPORTED_32_BIT_ABIS.asIterable(),
        ) ?: throw IllegalStateException(
            "Unsupported process ABI (64-bit=${Process.is64Bit()}): " +
                Build.SUPPORTED_ABIS.joinToString(),
        )
    }

    fun isCompatibleWithCurrentProcess(file: File): Boolean {
        return isElfCompatible(file, Process.is64Bit())
    }

    internal fun selectProcessAbi(
        is64Bit: Boolean,
        supportedAbis: Iterable<String>,
        supported64BitAbis: Iterable<String>,
        supported32BitAbis: Iterable<String>,
    ): String? {
        val validAbis = if (is64Bit) known64BitAbis else known32BitAbis
        val processAbis = if (is64Bit) supported64BitAbis else supported32BitAbis

        return (processAbis.asSequence() + supportedAbis.asSequence())
            .firstOrNull { it in validAbis }
    }

    internal fun isElfCompatible(file: File, is64Bit: Boolean): Boolean {
        if (!file.isFile) return false

        return runCatching {
            file.inputStream().buffered().use { input ->
                val header = ByteArray(ELF_CLASS_INDEX + 1)
                if (input.read(header) != header.size) return@use false

                val isElf = header[0] == 0x7f.toByte() &&
                    header[1] == 'E'.code.toByte() &&
                    header[2] == 'L'.code.toByte() &&
                    header[3] == 'F'.code.toByte()
                val expectedClass = if (is64Bit) ELF_CLASS_64 else ELF_CLASS_32
                isElf && header[ELF_CLASS_INDEX] == expectedClass
            }
        }.getOrDefault(false)
    }

    private const val ELF_CLASS_INDEX = 4
    private const val ELF_CLASS_32: Byte = 1
    private const val ELF_CLASS_64: Byte = 2
}
