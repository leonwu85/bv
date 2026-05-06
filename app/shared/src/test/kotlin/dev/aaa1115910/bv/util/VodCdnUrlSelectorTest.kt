package dev.aaa1115910.bv.util

import dev.aaa1115910.bv.entity.CdnService
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VodCdnUrlSelectorTest {
    @Test
    fun selectsVideoBackupFromLogCandidatesByDefault() {
        val selection = VodCdnUrlSelector.select(
            urls = listOf(
                "https://xy122x246x0x131xy.mcdn.bilivideo.cn:8082/v1/resource/upgcxcode/77/26/video.m4s?os=08cbv",
                "https://upos-sz-mirror08c.bilivideo.com/upgcxcode/77/26/video.m4s?os=08cbv",
                "https://upos-sz-mirror08c.bilivideo.com/upgcxcode/77/26/video.m4s?os=08cbv"
            ),
            cdnService = CdnService.BackupUrl
        )

        assertEquals("upos-sz-mirror08c.bilivideo.com", URI(selection.url).host)
        assertFalse(selection.url.contains(".mcdn.bilivideo."))
        assertEquals(VodCdnSelectionReason.BackupUrl, selection.reason)
    }

    @Test
    fun selectsAudioBackupFromLogCandidatesByDefault() {
        val selection = VodCdnUrlSelector.select(
            urls = listOf(
                "https://xy116x196x140x208xy.mcdn.bilivideo.cn:8082/v1/resource/upgcxcode/77/26/audio.m4s?os=08hbv",
                "https://upos-sz-mirror08h.bilivideo.com/upgcxcode/77/26/audio.m4s?os=08hbv",
                "https://upos-sz-mirror08c.bilivideo.com/upgcxcode/77/26/audio.m4s?os=08cbv"
            ),
            cdnService = CdnService.BackupUrl
        )

        assertEquals("upos-sz-mirror08h.bilivideo.com", URI(selection.url).host)
        assertFalse(selection.url.contains(".mcdn.bilivideo."))
        assertEquals(VodCdnSelectionReason.BackupUrl, selection.reason)
    }

    @Test
    fun rewritesMirrorHostToSelectedCdnService() {
        val selection = VodCdnUrlSelector.select(
            urls = listOf(
                "https://upos-sz-mirror08h.bilivideo.com/upgcxcode/video.m4s?deadline=1",
                "https://upos-sz-mirror08c.bilivideo.com/upgcxcode/video.m4s?deadline=1"
            ),
            cdnService = CdnService.Ali
        )

        assertEquals("upos-sz-mirrorali.bilivideo.com", URI(selection.url).host)
        assertEquals(VodCdnSelectionReason.RewrittenMirror, selection.reason)
    }

    @Test
    fun baseUrlServiceReturnsFirstCandidate() {
        val mcdnUrl = "https://example.mcdn.bilivideo.cn/v1/resource/video.m4s"
        val selection = VodCdnUrlSelector.select(
            urls = listOf(
                mcdnUrl,
                "https://upos-sz-mirrorcos.bilivideo.com/upgcxcode/video.m4s"
            ),
            cdnService = CdnService.BaseUrl
        )

        assertEquals(mcdnUrl, selection.url)
        assertEquals(VodCdnSelectionReason.BaseUrl, selection.reason)
    }

    @Test
    fun audioDisableCdnUsesBackupEvenWhenSelectedServiceCanRewrite() {
        val selection = VodCdnUrlSelector.select(
            urls = listOf("https://upos-sz-mirror08h.bilivideo.com/upgcxcode/audio.m4s?deadline=1"),
            cdnService = CdnService.Ali,
            isAudio = true,
            disableAudioCdn = true
        )

        assertEquals("upos-sz-mirror08h.bilivideo.com", URI(selection.url).host)
        assertEquals(VodCdnSelectionReason.BackupUrl, selection.reason)
    }

    @Test
    fun proxiesMcdnResourceWhenNoMirrorBackupExists() {
        val mcdnUrl = "https://example.mcdn.bilivideo.cn:8082/v1/resource/upgcxcode/video.m4s?deadline=1"

        val selection = VodCdnUrlSelector.select(
            urls = listOf(mcdnUrl),
            cdnService = CdnService.BackupUrl
        )

        assertEquals("proxy-tf-all-ws.bilivideo.com", URI(selection.url).host)
        assertTrue(selection.url.startsWith("https://proxy-tf-all-ws.bilivideo.com?url="))
        assertEquals(VodCdnSelectionReason.ProxiedMcdnResource, selection.reason)
    }

    @Test
    fun rewritesMcdnUpgcxcodeWhenNoMirrorBackupExists() {
        val selection = VodCdnUrlSelector.select(
            urls = listOf("https://example.mcdn.bilivideo.cn/upgcxcode/video.m4s?deadline=1"),
            cdnService = CdnService.Cos
        )

        assertEquals("upos-sz-mirrorcos.bilivideo.com", URI(selection.url).host)
        assertTrue(selection.url.contains("/upgcxcode/video.m4s"))
        assertEquals(VodCdnSelectionReason.RewrittenMcdnUpgcxcode, selection.reason)
    }

    @Test
    fun rewritesUpgcxcodeWithMcdnQueryWhenNoMirrorBackupExists() {
        val selection = VodCdnUrlSelector.select(
            urls = listOf("https://upos-sz-mirrorcoso1.bilivideo.com/upgcxcode/video.m4s?os=mcdn&deadline=1"),
            cdnService = CdnService.Ali
        )

        assertEquals("upos-sz-mirrorali.bilivideo.com", URI(selection.url).host)
        assertTrue(selection.url.contains("os=mcdn"))
        assertEquals(VodCdnSelectionReason.RewrittenMcdnUpgcxcode, selection.reason)
    }

    @Test
    fun rewritesSzbdydToQuerySourceHost() {
        val selection = VodCdnUrlSelector.select(
            urls = listOf("https://foo.szbdyd.com/video.m4s?xy_usource=upos-sz-mirror08c.bilivideo.com&deadline=1"),
            cdnService = CdnService.Ali
        )

        assertEquals("upos-sz-mirror08c.bilivideo.com", URI(selection.url).host)
        assertEquals(443, URI(selection.url).port)
        assertEquals(VodCdnSelectionReason.RewrittenSzbdyd, selection.reason)
    }

    @Test
    fun rewritesSzbdydToSelectedServiceWithoutQuerySourceHost() {
        val selection = VodCdnUrlSelector.select(
            urls = listOf("https://foo.szbdyd.com/video.m4s?deadline=1"),
            cdnService = CdnService.Hw
        )

        assertEquals("upos-sz-mirrorhw.bilivideo.com", URI(selection.url).host)
        assertEquals(443, URI(selection.url).port)
        assertEquals(VodCdnSelectionReason.RewrittenSzbdyd, selection.reason)
    }

    @Test
    fun rawIpResourceFallsBackToMcdnProxy() {
        val selection = VodCdnUrlSelector.select(
            urls = listOf("https://1.2.3.4/v1/resource/video.m4s?deadline=1"),
            cdnService = CdnService.BackupUrl
        )

        assertEquals("proxy-tf-all-ws.bilivideo.com", URI(selection.url).host)
        assertEquals(VodCdnSelectionReason.ProxiedMcdnResource, selection.reason)
    }

    @Test
    fun emptyCandidatesReturnEmptySelection() {
        val selection = VodCdnUrlSelector.select(
            urls = listOf(null, "", "   "),
            cdnService = CdnService.BackupUrl
        )

        assertEquals("", selection.url)
        assertEquals(VodCdnSelectionReason.NoUrl, selection.reason)
    }

    @Test
    fun duplicatesDoNotPreventSelectingTheFirstMirrorBackup() {
        val selection = VodCdnUrlSelector.select(
            urls = listOf(
                "https://example.mcdn.bilivideo.cn/v1/resource/video.m4s",
                "https://upos-sz-mirror08h.bilivideo.com/upgcxcode/video.m4s",
                "https://upos-sz-mirror08h.bilivideo.com/upgcxcode/video.m4s",
                "https://upos-sz-mirror08c.bilivideo.com/upgcxcode/video.m4s"
            ),
            cdnService = CdnService.BackupUrl
        )

        assertEquals("upos-sz-mirror08h.bilivideo.com", URI(selection.url).host)
    }
}
