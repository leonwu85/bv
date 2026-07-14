package dev.aaa1115910.bv.tv.component

import kotlin.test.Test
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
}
