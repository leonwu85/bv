package dev.aaa1115910.bv.player.impl

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ElfToolchainVersionTest {
    @Test
    fun picksHighestClangVersionFromComment() {
        val comment = listOf(
            "Android (13989888, +pgo, +bolt, +lto, +mlgo, based on r563880c) clang version 21.0.0 (https://android.googlesource.com/toolchain/llvm-project 5e96669f)",
            "Android (12896553, +pgo, +bolt, +lto, +mlgo, based on r530567c) clang version 19.0.0 (https://android.googlesource.com/toolchain/llvm-project 97a699bf)",
            "Linker: LLD 21.0.0 (/mnt/disks/build-disk/src/llvm-project/llvm 5e96669f)"
        ).joinToString("\u0000", postfix = "\u0000").toByteArray(Charsets.ISO_8859_1)

        assertEquals(ToolchainVersion(21, 0, 0), ElfToolchainVersion.parseComment(comment))
    }

    @Test
    fun returnsNullWhenCommentHasNoClangVersion() {
        assertNull(ElfToolchainVersion.parseComment("GCC: (GNU) 4.9.x 20150123\u0000".toByteArray()))
        assertNull(ElfToolchainVersion.parseComment(ByteArray(0)))
    }

    @Test
    fun readsCommentSectionFromElf64() {
        val file = writeTempElf(is64 = true, comment = "Android clang version 18.0.1 (x)\u0000")
        assertEquals(ToolchainVersion(18, 0, 1), ElfToolchainVersion.read(file))
    }

    @Test
    fun readsCommentSectionFromElf32() {
        val file = writeTempElf(is64 = false, comment = "clang version 17.0.2\u0000clang version 19.0.1\u0000")
        assertEquals(ToolchainVersion(19, 0, 1), ElfToolchainVersion.read(file))
    }

    @Test
    fun returnsNullForNonElfOrMissingFile() {
        val notElf = File.createTempFile("not-elf", ".so").apply {
            deleteOnExit()
            writeText("definitely not an elf")
        }
        assertNull(ElfToolchainVersion.read(notElf))
        assertNull(ElfToolchainVersion.read(File(notElf.parentFile, "does-not-exist.so")))
    }

    @Test
    fun versionsCompareNumerically() {
        assertTrue(ToolchainVersion(21, 0, 0) > ToolchainVersion(19, 0, 1))
        assertTrue(ToolchainVersion(18, 10, 0) > ToolchainVersion(18, 9, 9))
        assertTrue(ToolchainVersion(18, 0, 1) < ToolchainVersion(18, 1, 0))
        assertEquals(ToolchainVersion(1, 2, 3), ToolchainVersion(1, 2, 3))
    }

    /**
     * Builds a minimal little-endian ELF with three sections: the mandatory null section,
     * `.comment` and `.shstrtab`. Layout: header, comment payload, string table, section headers.
     */
    private fun writeTempElf(is64: Boolean, comment: String): File {
        val headerSize = if (is64) 64 else 52
        val sectionHeaderSize = if (is64) 64 else 40
        val commentBytes = comment.toByteArray(Charsets.ISO_8859_1)
        val stringTable = "\u0000.comment\u0000.shstrtab\u0000".toByteArray(Charsets.ISO_8859_1)
        val commentNameOffset = 1
        val shstrtabNameOffset = 1 + ".comment".length + 1

        val commentOffset = headerSize
        val stringTableOffset = commentOffset + commentBytes.size
        val sectionHeaderOffset = stringTableOffset + stringTable.size
        val sectionCount = 3
        val total = sectionHeaderOffset + sectionHeaderSize * sectionCount

        val buffer = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN)
        // e_ident
        buffer.put(0x7F).put('E'.code.toByte()).put('L'.code.toByte()).put('F'.code.toByte())
        buffer.put(if (is64) 2 else 1) // EI_CLASS
        buffer.put(1) // EI_DATA little-endian
        buffer.put(1) // EI_VERSION
        buffer.position(16)
        buffer.putShort(3) // e_type ET_DYN
        buffer.putShort(if (is64) 0xB7 else 0x28) // e_machine (aarch64 / arm)
        buffer.putInt(1) // e_version
        if (is64) {
            buffer.putLong(0) // e_entry
            buffer.putLong(0) // e_phoff
            buffer.putLong(sectionHeaderOffset.toLong()) // e_shoff
            buffer.putInt(0) // e_flags
            buffer.putShort(headerSize.toShort()) // e_ehsize
            buffer.putShort(0) // e_phentsize
            buffer.putShort(0) // e_phnum
            buffer.putShort(sectionHeaderSize.toShort()) // e_shentsize
            buffer.putShort(sectionCount.toShort()) // e_shnum
            buffer.putShort(2) // e_shstrndx
        } else {
            buffer.putInt(0) // e_entry
            buffer.putInt(0) // e_phoff
            buffer.putInt(sectionHeaderOffset) // e_shoff
            buffer.putInt(0) // e_flags
            buffer.putShort(headerSize.toShort())
            buffer.putShort(0)
            buffer.putShort(0)
            buffer.putShort(sectionHeaderSize.toShort())
            buffer.putShort(sectionCount.toShort())
            buffer.putShort(2)
        }
        assertEquals(headerSize, buffer.position())

        buffer.put(commentBytes)
        buffer.put(stringTable)
        assertEquals(sectionHeaderOffset, buffer.position())

        fun sectionHeader(nameOffset: Int, type: Int, offset: Int, size: Int) {
            buffer.putInt(nameOffset)
            buffer.putInt(type)
            if (is64) {
                buffer.putLong(0) // sh_flags
                buffer.putLong(0) // sh_addr
                buffer.putLong(offset.toLong())
                buffer.putLong(size.toLong())
                buffer.putInt(0) // sh_link
                buffer.putInt(0) // sh_info
                buffer.putLong(1) // sh_addralign
                buffer.putLong(0) // sh_entsize
            } else {
                buffer.putInt(0)
                buffer.putInt(0)
                buffer.putInt(offset)
                buffer.putInt(size)
                buffer.putInt(0)
                buffer.putInt(0)
                buffer.putInt(1)
                buffer.putInt(0)
            }
        }
        sectionHeader(0, 0, 0, 0) // SHT_NULL
        sectionHeader(commentNameOffset, 1, commentOffset, commentBytes.size) // SHT_PROGBITS
        sectionHeader(shstrtabNameOffset, 3, stringTableOffset, stringTable.size) // SHT_STRTAB
        assertEquals(total, buffer.position())

        return File.createTempFile("synthetic", ".so").apply {
            deleteOnExit()
            writeBytes(buffer.array())
        }
    }
}
