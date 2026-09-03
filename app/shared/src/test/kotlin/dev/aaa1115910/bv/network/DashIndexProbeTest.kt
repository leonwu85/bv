package dev.aaa1115910.bv.network

import dev.aaa1115910.bv.player.entity.DashSegment
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DashIndexProbeTest {
    private fun ByteArrayOutputStream.u32(value: Long) {
        for (shift in 24 downTo 0 step 8) write(((value shr shift) and 0xFF).toInt())
    }

    private fun ByteArrayOutputStream.u16(value: Int) {
        write((value shr 8) and 0xFF); write(value and 0xFF)
    }

    private fun box(type: String, payloadSize: Int, largeSize: Boolean = false): ByteArray {
        val out = ByteArrayOutputStream()
        val total = payloadSize + if (largeSize) 16 else 8
        if (largeSize) {
            out.write(byteArrayOf(0, 0, 0, 1))
            out.write(type.toByteArray(Charsets.ISO_8859_1))
            for (shift in 56 downTo 0 step 8) out.write((total.toLong() shr shift).toInt() and 0xFF)
        } else {
            out.u32(total.toLong())
            out.write(type.toByteArray(Charsets.ISO_8859_1))
        }
        out.write(ByteArray(payloadSize))
        return out.toByteArray()
    }

    /** A version-0 sidx with the given (size, duration) references. */
    private fun sidx(timescale: Long, firstOffset: Long, refs: List<Pair<Long, Long>>, version: Int = 0): ByteArray {
        val out = ByteArrayOutputStream()
        val size = 8 + 4 + 4 + 4 + (if (version == 0) 8 else 16) + 4 + refs.size * 12
        out.u32(size.toLong())
        out.write("sidx".toByteArray(Charsets.ISO_8859_1))
        out.write(version); out.write(0); out.write(0); out.write(0)
        out.u32(1) // reference_ID
        out.u32(timescale)
        if (version == 0) {
            out.u32(0) // earliest_presentation_time
            out.u32(firstOffset)
        } else {
            out.u32(0); out.u32(0)
            out.u32(0); out.u32(firstOffset)
        }
        out.u16(0)
        out.u16(refs.size)
        refs.forEach { (refSize, duration) ->
            out.u32(refSize)
            out.u32(duration)
            out.u32(0x9000_0000L) // starts_with_SAP=1, SAP_type=1
        }
        return out.toByteArray()
    }

    private fun concat(vararg parts: ByteArray): ByteArray = parts.fold(ByteArray(0)) { acc, part -> acc + part }

    @Test
    fun findsSidxAfterFtypAndMoov() {
        val bytes = concat(box("ftyp", 24), box("moov", 1043), box("sidx", 644), box("moof", 100))
        val ranges = DashIndexProbe.parseSegmentRanges(bytes)
        // ftyp 32 bytes + moov 1051 bytes => sidx starts at 1083, 652 bytes long
        assertEquals(DashIndexProbe.SegmentRanges(initRange = "0-1082", indexRange = "1083-1734"), ranges)
    }

    @Test
    fun skipsMoovLargerThanProbeWindow() {
        val window = concat(box("ftyp", 24), box("moov", 40_000).copyOf(8))
        assertNull(DashIndexProbe.parseSegmentRanges(window), "sidx header outside window cannot be located")
    }

    @Test
    fun handlesLargeSizeBoxes() {
        val bytes = concat(box("ftyp", 16), box("free", 8, largeSize = true), box("sidx", 100), box("moof", 8))
        val ranges = DashIndexProbe.parseSegmentRanges(bytes)
        assertEquals("0-47", ranges?.initRange)
        assertEquals("48-155", ranges?.indexRange)
    }

    @Test
    fun rejectsSegmentsWithoutIndex() {
        assertNull(DashIndexProbe.parseSegmentRanges(concat(box("ftyp", 24), box("moov", 100), box("moof", 50))))
        assertNull(DashIndexProbe.parseSegmentRanges(ByteArray(4)))
        assertNull(DashIndexProbe.parseSegmentRanges(box("sidx", 10)), "an index at offset 0 has no init segment")
        assertNull(DashIndexProbe.parseSegmentRanges("not an mp4 at all!".toByteArray()))
    }

    @Test
    fun parsesSidxIntoAbsoluteClosedByteRanges() {
        val refs = listOf(95_000L to 80_000L, 120_000L to 80_000L, 30_000L to 3_733L)
        val bytes = sidx(timescale = 16000, firstOffset = 0, refs = refs)
        val index = DashIndexProbe.parseSidx(bytes, sidxOffset = 1083)
        requireNotNull(index)
        assertEquals(16000, index.timescale)
        val firstMedia = 1083L + bytes.size
        assertEquals(
            listOf(
                DashSegment(firstMedia, firstMedia + 95_000 - 1, 80_000),
                DashSegment(firstMedia + 95_000, firstMedia + 215_000 - 1, 80_000),
                DashSegment(firstMedia + 215_000, firstMedia + 245_000 - 1, 3_733),
            ),
            index.segments
        )
        assertEquals(163_733, index.totalDurationTicks)
    }

    @Test
    fun honoursFirstOffsetAndVersion1() {
        val bytes = sidx(timescale = 44100, firstOffset = 16, refs = listOf(500L to 221_184L), version = 1)
        val index = DashIndexProbe.parseSidx(bytes, sidxOffset = 837)
        requireNotNull(index)
        val start = 837L + bytes.size + 16
        assertEquals(DashSegment(start, start + 499, 221_184), index.segments.single())
    }

    @Test
    fun rejectsMalformedOrNestedSidx() {
        assertNull(DashIndexProbe.parseSidx(ByteArray(10), 0))
        assertNull(DashIndexProbe.parseSidx(box("moov", 40), 0), "not a sidx")
        val good = sidx(16000, 0, listOf(100L to 10L))
        assertNull(DashIndexProbe.parseSidx(good.copyOf(good.size - 1), 0), "declared size must match")
        val nested = sidx(16000, 0, listOf((0x8000_0000L or 100L) to 10L))
        assertNull(DashIndexProbe.parseSidx(nested, 0), "reference_type=1 points at another sidx")
    }
}
