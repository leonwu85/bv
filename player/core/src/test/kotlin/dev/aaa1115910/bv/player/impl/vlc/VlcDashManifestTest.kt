package dev.aaa1115910.bv.player.impl.vlc

import dev.aaa1115910.bv.player.entity.DashRepresentation
import dev.aaa1115910.bv.player.entity.DashSegment
import dev.aaa1115910.bv.player.entity.DashSegmentIndex
import dev.aaa1115910.bv.player.entity.DashStreamInfo
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VlcDashManifestTest {
    private val videoIndex = DashSegmentIndex(
        timescale = 16000,
        segments = listOf(
            DashSegment(1735, 100_000, 80_000),
            DashSegment(100_001, 200_000, 80_000),
            DashSegment(200_001, 260_000, 3_733),
            DashSegment(260_001, 300_000, 80_000),
        ),
    )
    private val audioIndex = DashSegmentIndex(
        timescale = 44100,
        segments = listOf(
            DashSegment(1477, 50_000, 221_184),
            DashSegment(50_001, 90_000, 221_184),
        ),
    )
    private val video = DashRepresentation(
        url = "https://upos-sz.bilivideo.com/v.m4s?e=a&b=1",
        mimeType = "video/mp4",
        codecs = "hev1.1.6.L120.90",
        bandwidth = 2_000_000,
        width = 1080,
        height = 1920,
        initRange = "0-1082",
        indexRange = "1083-1734",
        segmentIndex = videoIndex,
    )
    private val audio = DashRepresentation(
        url = "https://upos-sz.bilivideo.com/a.m4s?e=b&c=2",
        mimeType = "audio/mp4",
        codecs = "mp4a.40.2",
        bandwidth = 192_000,
        indexRange = "837-1476",
        segmentIndex = audioIndex,
    )
    private val info = DashStreamInfo(durationMs = 246_713, video = video, audio = audio)

    @Test
    fun applicabilityRequiresAudioSegmentIndexesAndDuration() {
        assertTrue(VlcDashManifest.isApplicable(info))
        assertFalse(VlcDashManifest.isApplicable(null))
        assertFalse(VlcDashManifest.isApplicable(info.copy(audio = null)))
        assertFalse(VlcDashManifest.isApplicable(info.copy(durationMs = 0)))
        assertFalse(VlcDashManifest.isApplicable(info.copy(audio = audio.copy(segmentIndex = null))))
        assertFalse(
            VlcDashManifest.isApplicable(
                info.copy(video = video.copy(segmentIndex = DashSegmentIndex(16000, emptyList())))
            )
        )
    }

    @Test
    fun producesWellFormedSegmentListMpd() {
        val xml = VlcDashManifest.build(info)
        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(xml.byteInputStream())
        val mpd = doc.documentElement
        assertEquals("MPD", mpd.localName)
        assertEquals("static", mpd.getAttribute("type"))
        assertEquals("PT246.713S", mpd.getAttribute("mediaPresentationDuration"))

        assertEquals(2, doc.getElementsByTagNameNS("*", "AdaptationSet").length)
        val bases = doc.getElementsByTagNameNS("*", "BaseURL")
        // Ampersands in the signed URLs must round-trip through XML escaping
        assertEquals(video.url, bases.item(0).textContent)
        assertEquals(audio.url, bases.item(1).textContent)

        val lists = doc.getElementsByTagNameNS("*", "SegmentList")
        assertEquals(2, lists.length)
        assertEquals("16000", lists.item(0).attributes.getNamedItem("timescale").nodeValue)
        assertEquals("1", lists.item(0).attributes.getNamedItem("startNumber").nodeValue)
        assertEquals("44100", lists.item(1).attributes.getNamedItem("timescale").nodeValue)

        val inits = doc.getElementsByTagNameNS("*", "Initialization")
        assertEquals("0-1082", inits.item(0).attributes.getNamedItem("range").nodeValue)
        // No explicit init range for audio: everything before the sidx is the init segment
        assertEquals("0-836", inits.item(1).attributes.getNamedItem("range").nodeValue)

        // Every segment gets its exact, closed byte range: no open-ended or inverted ranges
        val urls = doc.getElementsByTagNameNS("*", "SegmentURL")
        assertEquals(6, urls.length)
        assertEquals("1735-100000", urls.item(0).attributes.getNamedItem("mediaRange").nodeValue)
        assertEquals("260001-300000", urls.item(3).attributes.getNamedItem("mediaRange").nodeValue)
        assertEquals("50001-90000", urls.item(5).attributes.getNamedItem("mediaRange").nodeValue)

        // One timeline element per segment (no r): libvlc maps SegmentURLs by timeline element index
        val videoTimeline = doc.getElementsByTagNameNS("*", "SegmentTimeline").item(0)
        val s = (0 until videoTimeline.childNodes.length).map { videoTimeline.childNodes.item(it) }
            .filter { it.localName == "S" }
        assertEquals(4, s.size)
        assertEquals("0", s[0].attributes.getNamedItem("t").nodeValue)
        assertEquals(listOf("80000", "80000", "3733", "80000"), s.map { it.attributes.getNamedItem("d").nodeValue })
        s.forEach { assertNull(it.attributes.getNamedItem("r")) }
        s.drop(1).forEach { assertNull(it.attributes.getNamedItem("t")) }

        val reps = doc.getElementsByTagNameNS("*", "Representation")
        assertEquals("hev1.1.6.L120.90", reps.item(0).attributes.getNamedItem("codecs").nodeValue)
        assertEquals("1080", reps.item(0).attributes.getNamedItem("width").nodeValue)
        assertNull(reps.item(1).attributes.getNamedItem("width"))
    }

    @Test
    fun writeReusesFileForSameContentAndPrunesOthers() {
        val dir = File(System.getProperty("java.io.tmpdir"), "vlc-dash-test-${System.nanoTime()}").apply { mkdirs() }
        try {
            val first = VlcDashManifest.write(dir, info)
            val again = VlcDashManifest.write(dir, info)
            assertEquals(first, again)
            assertEquals(VlcDashManifest.build(info), first.readText())

            val other = VlcDashManifest.write(dir, info.copy(durationMs = 1000))
            assertTrue(other.isFile)
            assertFalse(first.exists(), "manifests of other sessions are pruned")
        } finally {
            dir.deleteRecursively()
        }
    }
}
