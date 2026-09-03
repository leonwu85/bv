package dev.aaa1115910.bv.network

import dev.aaa1115910.bv.player.entity.DashSegment
import dev.aaa1115910.bv.player.entity.DashSegmentIndex
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.readAvailable

/**
 * Reads the `sidx` of a DASH on-demand fMP4 (B 站 `.m4s`) and turns it into an exact segment table.
 *
 * The web playurl API spells out `SegmentBase{Initialization, indexRange}`, the gRPC one does not;
 * either way the sidx itself has to be fetched to know where each subsegment lives. B 站 segments are
 * `ftyp` + `moov` + `sidx` + fragments, so without a known range the sidx sits within the first few KiB.
 */
object DashIndexProbe {
    private const val HEADER_PROBE_BYTES = 16 * 1024
    private const val MAX_INDEX_BYTES = 512 * 1024
    private const val BOX_HEADER_SIZE = 8
    private val logger = KotlinLogging.logger { }

    /** Boxes that legitimately precede the sidx in an on-demand segment */
    private val headerBoxes = setOf("ftyp", "styp", "moov", "free", "skip", "uuid", "prft", "emsg")

    data class SegmentRanges(val initRange: String, val indexRange: String)

    /** Everything the manifest needs for one representation. */
    data class Result(
        val initRange: String,
        val indexRange: String,
        val segmentIndex: DashSegmentIndex,
    )

    private val client by lazy {
        HttpClient(OkHttp) {
            expectSuccess = false
            engine {
                config {
                    followRedirects(true)
                    callTimeout(java.time.Duration.ofSeconds(8))
                }
            }
        }
    }

    /**
     * Fetches and parses the sidx of [url]. [knownIndexRange] (from the API) saves the header walk;
     * otherwise the first 16 KiB are scanned for the box. Returns null on any failure, never throws.
     */
    suspend fun fetchSegmentIndex(
        url: String,
        referer: String?,
        userAgent: String?,
        knownInitRange: String?,
        knownIndexRange: String?,
    ): Result? {
        val known = parseRange(knownIndexRange)
        val ranges: SegmentRanges
        val indexBytes: ByteArray
        if (known != null) {
            val (start, end) = known
            if (end < start || end - start + 1 > MAX_INDEX_BYTES) return null
            indexBytes = fetch(url, referer, userAgent, start, end) ?: return null
            ranges = SegmentRanges(
                initRange = knownInitRange?.takeIf { it.isNotBlank() } ?: "0-${start - 1}",
                indexRange = "$start-$end",
            )
        } else {
            val header = fetch(url, referer, userAgent, 0, HEADER_PROBE_BYTES - 1L) ?: return null
            ranges = parseSegmentRanges(header) ?: return null
            val (start, end) = parseRange(ranges.indexRange) ?: return null
            if (end - start + 1 > MAX_INDEX_BYTES) return null
            indexBytes = if (end < header.size) {
                header.copyOfRange(start.toInt(), end.toInt() + 1)
            } else {
                fetch(url, referer, userAgent, start, end) ?: return null
            }
        }
        val sidxStart = parseRange(ranges.indexRange)?.first ?: return null
        val index = parseSidx(indexBytes, sidxStart) ?: run {
            logger.warn { "sidx at ${ranges.indexRange} of ${url.substringBefore('?')} could not be parsed" }
            return null
        }
        return Result(ranges.initRange, ranges.indexRange, index)
    }

    /** One HTTP Range request for bytes [start]..[end] (inclusive); null on transport/HTTP failure. */
    private suspend fun fetch(url: String, referer: String?, userAgent: String?, start: Long, end: Long): ByteArray? {
        return runCatching {
            client.prepareGet(url) {
                header(HttpHeaders.Range, "bytes=$start-$end")
                referer?.let { header(HttpHeaders.Referrer, it) }
                userAgent?.let { header(HttpHeaders.UserAgent, it) }
            }.execute { response ->
                if (response.status != HttpStatusCode.PartialContent && response.status != HttpStatusCode.OK) {
                    logger.warn { "sidx probe got HTTP ${response.status.value} for ${url.substringBefore('?')}" }
                    return@execute null
                }
                val wanted = (end - start + 1).toInt()
                val channel = response.bodyAsChannel()
                val buffer = ByteArray(wanted)
                var read = 0
                while (read < buffer.size) {
                    val n = channel.readAvailable(buffer, read, buffer.size - read)
                    if (n <= 0) break
                    read += n
                }
                // A 200 means the server ignored the Range; the bytes still start at 0, which is what
                // the header walk expects. For an offset range we must not mistake the file head for it.
                if (response.status == HttpStatusCode.OK && start != 0L) return@execute null
                buffer.copyOf(read)
            }
        }.onFailure { logger.warn { "sidx probe failed for ${url.substringBefore('?')}: ${it.message}" } }
            .getOrNull()
    }

    /**
     * Walks top-level ISO BMFF boxes until the `sidx`. Only box headers need to be inside [bytes];
     * a `moov` larger than the probe window is skipped by its declared size.
     */
    fun parseSegmentRanges(bytes: ByteArray): SegmentRanges? {
        var offset = 0L
        while (offset + BOX_HEADER_SIZE <= bytes.size) {
            val at = offset.toInt()
            var size = readUInt32(bytes, at)
            val type = String(bytes, at + 4, 4, Charsets.ISO_8859_1)
            var headerSize = BOX_HEADER_SIZE
            if (size == 1L) {
                if (at + 16 > bytes.size) return null
                size = readUInt64(bytes, at + 8)
                headerSize = 16
            }
            if (size == 0L || size < headerSize) return null // "to end of file" or corrupt: no index ahead

            if (type == "sidx") {
                if (offset == 0L) return null
                return SegmentRanges(
                    initRange = "0-${offset - 1}",
                    indexRange = "$offset-${offset + size - 1}"
                )
            }
            if (type !in headerBoxes) return null // moof/mdat before any sidx: not an indexed segment
            offset += size
        }
        return null
    }

    /**
     * Parses a `sidx` box ([bytes] = the whole box, located at [sidxOffset] in the file) into
     * absolute byte ranges. Returns null for anything but a flat index of media subsegments.
     */
    fun parseSidx(bytes: ByteArray, sidxOffset: Long): DashSegmentIndex? {
        if (bytes.size < 32) return null
        val boxSize = readUInt32(bytes, 0)
        if (String(bytes, 4, 4, Charsets.ISO_8859_1) != "sidx") return null
        if (boxSize != bytes.size.toLong()) return null
        val version = bytes[8].toInt() and 0xFF
        var at = 12 // after FullBox header (version + flags)
        at += 4 // reference_ID
        val timescale = readUInt32(bytes, at); at += 4
        if (timescale <= 0L) return null
        val firstOffset: Long
        if (version == 0) {
            at += 4 // earliest_presentation_time
            firstOffset = readUInt32(bytes, at); at += 4
        } else {
            if (bytes.size < 40) return null
            at += 8
            firstOffset = readUInt64(bytes, at); at += 8
        }
        at += 2 // reserved
        val referenceCount = ((bytes[at].toInt() and 0xFF) shl 8) or (bytes[at + 1].toInt() and 0xFF); at += 2
        if (referenceCount <= 0 || at + referenceCount * 12 > bytes.size) return null

        val segments = ArrayList<DashSegment>(referenceCount)
        var cursor = sidxOffset + boxSize + firstOffset
        repeat(referenceCount) {
            val sizeField = readUInt32(bytes, at)
            val referenceType = (sizeField ushr 31).toInt()
            val referencedSize = sizeField and 0x7FFF_FFFFL
            val duration = readUInt32(bytes, at + 4)
            at += 12
            if (referenceType != 0 || referencedSize <= 0L) return null // nested sidx: not supported
            segments += DashSegment(startByte = cursor, endByte = cursor + referencedSize - 1, durationTicks = duration)
            cursor += referencedSize
        }
        return DashSegmentIndex(timescale = timescale, segments = segments)
    }

    private fun parseRange(range: String?): Pair<Long, Long>? {
        if (range.isNullOrBlank()) return null
        val start = range.substringBefore('-').trim().toLongOrNull() ?: return null
        val end = range.substringAfter('-', "").trim().toLongOrNull() ?: return null
        if (start < 0 || end < start) return null
        return start to end
    }

    private fun readUInt32(bytes: ByteArray, at: Int): Long {
        return ((bytes[at].toLong() and 0xFF) shl 24) or
            ((bytes[at + 1].toLong() and 0xFF) shl 16) or
            ((bytes[at + 2].toLong() and 0xFF) shl 8) or
            (bytes[at + 3].toLong() and 0xFF)
    }

    private fun readUInt64(bytes: ByteArray, at: Int): Long {
        var value = 0L
        for (i in 0 until 8) value = (value shl 8) or (bytes[at + i].toLong() and 0xFF)
        return value
    }
}
