package dev.aaa1115910.bv.player.impl.vlc

import dev.aaa1115910.bv.player.entity.DashRepresentation
import dev.aaa1115910.bv.player.entity.DashStreamInfo
import java.io.File
import java.util.Locale

/**
 * Builds a minimal static DASH MPD for one video + one audio representation.
 *
 * Why: VLC 4 (libvlcjni master) raises the input clock's discontinuity threshold to 300 ms and
 * lets every input source feed PCR, so playing the audio track as an input slave (`addSlave`)
 * makes the two fragmented-MP4 demuxers alternate PCRs 5 s apart, each one flagged as a "clock
 * gap" and resetting the clock — the visible result is playback stalling every few seconds.
 * Handing both files to libvlc's `adaptive` demuxer as a single DASH input keeps synchronisation
 * inside one demuxer, which is how VLC plays DASH anyway.
 *
 * Why `SegmentList` instead of `SegmentBase@indexRange`: when libvlc parses the sidx itself,
 * `SegmentInformation::SplitUsingIndex` emits the last subsegment with an inverted byte range
 * (`start-1` as end). Bilibili's CDN answers that Range with the whole file, so at the end of the
 * video the demuxer restarts from byte 0, re-downloads everything and never reaches EOF. We parse
 * the sidx ourselves ([DashRepresentation.segmentIndex]) and spell out every segment's exact range.
 */
object VlcDashManifest {
    private const val MPD_NAMESPACE = "urn:mpeg:dash:schema:mpd:2011"
    private const val PROFILE_ON_DEMAND = "urn:mpeg:dash:profile:isoff-on-demand:2011"
    private const val FILE_PREFIX = "bv-dash-"

    /** Whether [info] carries everything the manifest path needs. */
    fun isApplicable(info: DashStreamInfo?): Boolean {
        return info != null && info.audio != null && info.hasSegmentIndexes && info.durationMs > 0L
    }

    /** MPD document for [info]; caller must have checked [isApplicable]. */
    fun build(info: DashStreamInfo): String {
        val seconds = info.durationMs / 1000.0
        val sb = StringBuilder(8192)
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
        sb.append("<MPD xmlns=\"$MPD_NAMESPACE\" profiles=\"$PROFILE_ON_DEMAND\" type=\"static\"")
        sb.append(" mediaPresentationDuration=\"").append(formatDuration(seconds)).append('"')
        sb.append(" minBufferTime=\"PT1.5S\">\n")
        sb.append("  <Period id=\"0\" start=\"PT0S\">\n")
        appendAdaptationSet(sb, info.video, "video")
        info.audio?.let { appendAdaptationSet(sb, it, "audio") }
        sb.append("  </Period>\n")
        sb.append("</MPD>\n")
        return sb.toString()
    }

    /**
     * Writes the manifest for [info] into [directory] and returns the file. The file name is
     * derived from the content so re-preparing the same streams reuses one file; stale manifests
     * from earlier sessions are pruned.
     */
    fun write(directory: File, info: DashStreamInfo): File {
        directory.mkdirs()
        val content = build(info)
        val file = File(directory, "$FILE_PREFIX${content.hashCode().toUInt().toString(16)}.mpd")
        directory.listFiles { candidate ->
            candidate.name.startsWith(FILE_PREFIX) && candidate.name != file.name
        }?.forEach { it.delete() }
        if (!file.isFile || file.readText() != content) {
            file.writeText(content)
        }
        return file
    }

    private fun appendAdaptationSet(sb: StringBuilder, rep: DashRepresentation, id: String) {
        val index = requireNotNull(rep.segmentIndex) { "segment index required for $id" }
        sb.append("    <AdaptationSet id=\"").append(id).append("\" mimeType=\"")
            .append(escape(rep.mimeType.ifBlank { if (id == "video") "video/mp4" else "audio/mp4" }))
            .append("\" segmentAlignment=\"true\" startWithSAP=\"1\">\n")
        sb.append("      <Representation id=\"").append(id).append("-0\"")
        sb.append(" bandwidth=\"").append(rep.bandwidth.coerceAtLeast(1)).append('"')
        rep.codecs?.takeIf { it.isNotBlank() }?.let { sb.append(" codecs=\"").append(escape(it)).append('"') }
        if (rep.width > 0 && rep.height > 0) {
            sb.append(" width=\"").append(rep.width).append("\" height=\"").append(rep.height).append('"')
        }
        sb.append(">\n")
        sb.append("        <BaseURL>").append(escape(rep.url)).append("</BaseURL>\n")
        // startNumber=1: libvlc numbers SegmentTimeline entries from 1 when unspecified but SegmentURLs
        // from 0; pinning both keeps timeline lookups aligned with the URL list.
        sb.append("        <SegmentList timescale=\"").append(index.timescale).append("\" startNumber=\"1\">\n")
        val initRange = rep.initRange?.takeIf { it.isNotBlank() } ?: defaultInitRange(rep.indexRange, index.segments.first().startByte)
        sb.append("          <Initialization range=\"").append(escape(initRange)).append("\"/>\n")
        sb.append("          <SegmentTimeline>\n")
        appendTimeline(sb, index.segments.map { it.durationTicks })
        sb.append("          </SegmentTimeline>\n")
        index.segments.forEach { segment ->
            sb.append("          <SegmentURL mediaRange=\"").append(segment.startByte).append('-')
                .append(segment.endByte).append("\"/>\n")
        }
        sb.append("        </SegmentList>\n")
        sb.append("      </Representation>\n")
        sb.append("    </AdaptationSet>\n")
    }

    /**
     * One `<S d>` per segment, deliberately without `r` compression: libvlc's
     * `SegmentList::getMediaSegment` looks the SegmentURL up by the *timeline element index*
     * (`getElementIndexBySequence`), so a repeated element would map every segment of the run to the
     * run's first SegmentURL — the seek lands on the right timestamp but fetches the wrong bytes.
     */
    private fun appendTimeline(sb: StringBuilder, durations: List<Long>) {
        durations.forEachIndexed { index, d ->
            sb.append("            <S")
            if (index == 0) sb.append(" t=\"0\"")
            sb.append(" d=\"").append(d).append("\"/>\n")
        }
    }

    /** Init segment = everything before the sidx (or before the first media segment) when the API left it out. */
    private fun defaultInitRange(indexRange: String?, firstSegmentStart: Long): String {
        val sidxStart = indexRange?.substringBefore('-')?.toLongOrNull()
        val end = when {
            sidxStart != null && sidxStart > 0 -> sidxStart - 1
            else -> (firstSegmentStart - 1).coerceAtLeast(0)
        }
        return "0-$end"
    }

    private fun formatDuration(seconds: Double): String {
        return "PT" + String.format(Locale.US, "%.3f", seconds.coerceAtLeast(0.001)) + "S"
    }

    private fun escape(text: String): String = buildString(text.length + 16) {
        text.forEach { ch ->
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(ch)
            }
        }
    }
}
