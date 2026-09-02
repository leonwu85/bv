package dev.aaa1115910.bv.player.impl

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** A `major.minor.patch` compiler version, e.g. the clang release an NDK ships. */
data class ToolchainVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<ToolchainVersion> {
    override fun compareTo(other: ToolchainVersion): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })

    override fun toString(): String = "$major.$minor.$patch"
}

/**
 * Reads the compiler version an ELF shared object was built with.
 *
 * Every NDK toolchain writes a `.comment` section such as
 * `Android (13989888, ... ) clang version 21.0.0 (...)` into the objects it produces; the section
 * survives `strip` and is not loaded at runtime. Because NDK `libc++_shared.so` is built by the
 * same toolchain it belongs to, the highest clang version in the section identifies the libc++
 * release, which is what decides whether one copy of libc++ can satisfy binaries linked against
 * another (newer libc++ is backwards compatible, older is not).
 */
object ElfToolchainVersion {
    private const val ELF_MAGIC = 0x464C457F // "\x7fELF" little-endian
    private const val ELF_CLASS_32: Byte = 1
    private const val ELF_CLASS_64: Byte = 2
    private const val ELF_DATA_LSB: Byte = 1
    private const val COMMENT_SECTION = ".comment"

    /** Refuse absurd tables so a corrupt file cannot make us allocate gigabytes. */
    private const val MAX_SECTIONS = 4096
    private const val MAX_STRTAB_BYTES = 1 shl 20
    private const val MAX_COMMENT_BYTES = 1 shl 16

    private val clangVersionRegex = Regex("""clang version (\d+)\.(\d+)\.(\d+)""")

    /** Highest clang version recorded in [file]'s `.comment`, or null if absent or unreadable. */
    fun read(file: File): ToolchainVersion? {
        if (!file.isFile) return null
        return runCatching {
            RandomAccessFile(file, "r").use { raf ->
                readCommentSection(raf)?.let(::parseComment)
            }
        }.getOrNull()
    }

    /** Highest clang version mentioned in a `.comment` payload (NUL separated strings). */
    fun parseComment(comment: ByteArray): ToolchainVersion? {
        val text = String(comment, Charsets.ISO_8859_1)
        return clangVersionRegex.findAll(text)
            .map { match ->
                ToolchainVersion(
                    match.groupValues[1].toInt(),
                    match.groupValues[2].toInt(),
                    match.groupValues[3].toInt()
                )
            }
            .maxOrNull()
    }

    private fun readCommentSection(raf: RandomAccessFile): ByteArray? {
        val ident = ByteArray(16)
        raf.seek(0)
        raf.readFully(ident)
        val identBuffer = ByteBuffer.wrap(ident).order(ByteOrder.LITTLE_ENDIAN)
        if (identBuffer.getInt(0) != ELF_MAGIC) return null
        // Every Android ABI is little-endian; big-endian objects are not something we can load anyway.
        if (ident[5] != ELF_DATA_LSB) return null
        val is64 = when (ident[4]) {
            ELF_CLASS_64 -> true
            ELF_CLASS_32 -> false
            else -> return null
        }

        val headerSize = if (is64) 64 else 52
        val header = ByteArray(headerSize)
        raf.seek(0)
        raf.readFully(header)
        val hdr = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        val sectionHeaderOffset: Long
        val sectionHeaderEntrySize: Int
        val sectionCount: Int
        val stringTableIndex: Int
        if (is64) {
            sectionHeaderOffset = hdr.getLong(0x28)
            sectionHeaderEntrySize = hdr.getShort(0x3A).toInt() and 0xFFFF
            sectionCount = hdr.getShort(0x3C).toInt() and 0xFFFF
            stringTableIndex = hdr.getShort(0x3E).toInt() and 0xFFFF
        } else {
            sectionHeaderOffset = (hdr.getInt(0x20).toLong()) and 0xFFFFFFFFL
            sectionHeaderEntrySize = hdr.getShort(0x2E).toInt() and 0xFFFF
            sectionCount = hdr.getShort(0x30).toInt() and 0xFFFF
            stringTableIndex = hdr.getShort(0x32).toInt() and 0xFFFF
        }
        val minEntrySize = if (is64) 64 else 40
        if (sectionHeaderOffset <= 0L || sectionHeaderEntrySize < minEntrySize) return null
        if (sectionCount == 0 || sectionCount > MAX_SECTIONS || stringTableIndex >= sectionCount) return null

        val table = ByteArray(sectionHeaderEntrySize * sectionCount)
        raf.seek(sectionHeaderOffset)
        raf.readFully(table)
        val sections = ByteBuffer.wrap(table).order(ByteOrder.LITTLE_ENDIAN)

        fun sectionName(index: Int): Int = sections.getInt(index * sectionHeaderEntrySize)
        fun sectionOffset(index: Int): Long {
            val base = index * sectionHeaderEntrySize
            return if (is64) sections.getLong(base + 0x18) else sections.getInt(base + 0x10).toLong() and 0xFFFFFFFFL
        }
        fun sectionSize(index: Int): Long {
            val base = index * sectionHeaderEntrySize
            return if (is64) sections.getLong(base + 0x20) else sections.getInt(base + 0x14).toLong() and 0xFFFFFFFFL
        }

        val stringTableSize = sectionSize(stringTableIndex)
        if (stringTableSize <= 0L || stringTableSize > MAX_STRTAB_BYTES) return null
        val stringTable = ByteArray(stringTableSize.toInt())
        raf.seek(sectionOffset(stringTableIndex))
        raf.readFully(stringTable)

        for (index in 0 until sectionCount) {
            val nameOffset = sectionName(index)
            if (nameOffset < 0 || nameOffset >= stringTable.size) continue
            if (!stringTable.startsWithCString(nameOffset, COMMENT_SECTION)) continue

            val size = sectionSize(index)
            if (size <= 0L || size > MAX_COMMENT_BYTES) return null
            val comment = ByteArray(size.toInt())
            raf.seek(sectionOffset(index))
            raf.readFully(comment)
            return comment
        }
        return null
    }

    private fun ByteArray.startsWithCString(offset: Int, expected: String): Boolean {
        if (offset + expected.length >= size) return false
        for (i in expected.indices) {
            if (this[offset + i] != expected[i].code.toByte()) return false
        }
        return this[offset + expected.length] == 0.toByte()
    }
}
