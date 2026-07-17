package dev.aaa1115910.bv.tv.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeetestTvVerifyDialogTest {
    @Test
    fun bindPanelKeepsViewportCenteredPositioning() {
        val html = buildGeetestHtml(
            gt = "test-gt",
            challenge = "test-challenge",
        )

        assertTrue(html.contains(".geetest_panel { position: fixed !important; }"))
        assertTrue(html.contains(".geetest_panel_box { position: absolute !important; }"))
        assertFalse(html.contains(".geetest_panel { position: relative !important; }"))
        assertFalse(html.contains(".geetest_panel_box { position: relative !important; }"))
    }

    @Test
    fun switchingVerificationModeRequiresFreshChallenge() {
        assertTrue(
            shouldRefreshGeetestChallenge(
                currentMode = GeetestVerifyMode.TvRemote,
                requestedMode = GeetestVerifyMode.PhoneCompanion,
                mockMode = false,
                refreshAvailable = true,
            )
        )
        assertTrue(
            shouldRefreshGeetestChallenge(
                currentMode = GeetestVerifyMode.PhoneCompanion,
                requestedMode = GeetestVerifyMode.TvRemote,
                mockMode = false,
                refreshAvailable = true,
            )
        )
    }

    @Test
    fun failedRefreshKeepsCurrentModeAndAllowsRetry() {
        val currentMode = GeetestVerifyMode.TvRemote
        val requestedMode = GeetestVerifyMode.PhoneCompanion

        assertEquals(
            expected = currentMode,
            actual = resolveGeetestModeAfterRefresh(
                currentMode = currentMode,
                requestedMode = requestedMode,
                refreshSucceeded = false,
            )
        )
        assertTrue(
            shouldRefreshGeetestChallenge(
                currentMode = currentMode,
                requestedMode = requestedMode,
                mockMode = false,
                refreshAvailable = true,
            )
        )
    }

    @Test
    fun successfulRefreshCommitsRequestedMode() {
        assertEquals(
            expected = GeetestVerifyMode.PhoneCompanion,
            actual = resolveGeetestModeAfterRefresh(
                currentMode = GeetestVerifyMode.TvRemote,
                requestedMode = GeetestVerifyMode.PhoneCompanion,
                refreshSucceeded = true,
            )
        )
    }

    @Test
    fun currentReadyModeDoesNotRefreshOnFocusOrConfirm() {
        assertFalse(
            shouldRefreshGeetestChallenge(
                currentMode = GeetestVerifyMode.TvRemote,
                requestedMode = GeetestVerifyMode.TvRemote,
                mockMode = false,
                refreshAvailable = true,
            )
        )
    }

    @Test
    fun mockModeDoesNotRequireGaiaRefresh() {
        assertFalse(
            shouldRefreshGeetestChallenge(
                currentMode = GeetestVerifyMode.TvRemote,
                requestedMode = GeetestVerifyMode.PhoneCompanion,
                mockMode = true,
                refreshAvailable = true,
            )
        )
    }
}
