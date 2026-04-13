package dev.aaa1115910.biliapi.entity.video

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VideoDetailMappingTest {
    @Test
    fun `upower badge text stays exclusive when current user can play`() {
        assertEquals(
            "充电专属",
            upowerBadgeText(isUpowerExclusive = true)
        )
        assertEquals(
            "充电专属",
            upowerBadgeText(isUpowerExclusive = true)
        )
    }

    @Test
    fun `upower badge text is empty when video is not exclusive`() {
        assertEquals(
            "",
            upowerBadgeText(isUpowerExclusive = false)
        )
    }

    @Test
    fun `upower play state is only meaningful for exclusive video`() {
        assertEquals(true, upowerPlayState(isUpowerExclusive = true, isUpowerPlay = true))
        assertEquals(false, upowerPlayState(isUpowerExclusive = true, isUpowerPlay = false))
        assertNull(upowerPlayState(isUpowerExclusive = false, isUpowerPlay = true))
    }
}