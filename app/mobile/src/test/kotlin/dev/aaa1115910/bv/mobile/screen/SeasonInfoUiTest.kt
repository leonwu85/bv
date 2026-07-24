package dev.aaa1115910.bv.mobile.screen

import kotlin.test.Test
import kotlin.test.assertEquals

class SeasonInfoUiTest {
    @Test
    fun durationNormalizerSupportsSecondsAndMilliseconds() {
        assertEquals(1_200_000L, normalizedEpisodeDurationMillis(1_200))
        assertEquals(1_200_000L, normalizedEpisodeDurationMillis(1_200_000))
    }

    @Test
    fun episodeProgressUsesNormalizedDuration() {
        assertEquals(
            expected = 0.5f,
            actual = episodeProgress(lastTimeSeconds = 600, duration = 1_200),
            absoluteTolerance = 0.0001f
        )
        assertEquals(
            expected = 0.5f,
            actual = episodeProgress(lastTimeSeconds = 600, duration = 1_200_000),
            absoluteTolerance = 0.0001f
        )
    }

    @Test
    fun appBarTransitionHasNoJumpWhenHeroLeavesViewport() {
        val threshold = 176

        assertEquals(
            expected = 1f,
            actual = appBarTransitionFraction(
                hasContent = true,
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = threshold,
                threshold = threshold
            )
        )
        assertEquals(
            expected = 1f,
            actual = appBarTransitionFraction(
                hasContent = true,
                firstVisibleItemIndex = 1,
                firstVisibleItemScrollOffset = 0,
                threshold = threshold
            )
        )
    }

    @Test
    fun resumeLabelFormatsNumericEpisodeIndex() {
        assertEquals("第2集", resumeEpisodeLabel(progressIndex = "2", episodeTitle = "备用标题"))
        assertEquals("电影版", resumeEpisodeLabel(progressIndex = "电影版", episodeTitle = "备用标题"))
        assertEquals("第3集", resumeEpisodeLabel(progressIndex = null, episodeTitle = "3"))
    }
}
