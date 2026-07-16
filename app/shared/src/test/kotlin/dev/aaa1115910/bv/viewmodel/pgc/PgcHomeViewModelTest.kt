package dev.aaa1115910.bv.viewmodel.pgc

import dev.aaa1115910.biliapi.entity.season.Timeline
import dev.aaa1115910.biliapi.entity.season.TimelineEp
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PgcHomeViewModelTest {
    @Test
    fun mergeTimelinesDeduplicatesSeasonsAlreadyRepeatedInOneSource() {
        val result = mergeTimelines(
            first = listOf(
                timeline(
                    date = "7-16",
                    episodes = listOf(
                        episode(seasonId = 1, episodeId = 11, publishAt = 100),
                        episode(seasonId = 1, episodeId = 12, publishAt = 200),
                        episode(seasonId = 2, episodeId = 21, publishAt = 300)
                    )
                )
            ),
            second = emptyList()
        )

        assertEquals(listOf(1, 2), result.single().episodes.map { it.seasonId })
        assertEquals(11, result.single().episodes.first().episodeId)
    }

    @Test
    fun mergeTimelinesCombinesRepeatedDateGroupsAndPreservesUniqueDays() {
        val result = mergeTimelines(
            first = listOf(
                timeline(
                    date = "7-16",
                    dateTimestamp = 1000,
                    episodes = listOf(episode(seasonId = 1, episodeId = 11, publishAt = 100))
                )
            ),
            second = listOf(
                timeline(
                    date = "7-16",
                    dateTimestamp = 1000,
                    isToday = true,
                    episodes = listOf(
                        episode(seasonId = 1, episodeId = 12, publishAt = 200),
                        episode(seasonId = 2, episodeId = 21, publishAt = 300)
                    )
                ),
                timeline(
                    date = "7-17",
                    dateTimestamp = 2000,
                    episodes = listOf(episode(seasonId = 3, episodeId = 31, publishAt = 400))
                )
            )
        )

        assertEquals(listOf("7-16", "7-17"), result.map { it.dateString })
        assertEquals(listOf(1, 2), result.first().episodes.map { it.seasonId })
        assertTrue(result.first().isToday)
    }

    private fun timeline(
        date: String,
        dateTimestamp: Long = 0,
        isToday: Boolean = false,
        episodes: List<TimelineEp>
    ) = Timeline(
        dateString = date,
        date = Date(dateTimestamp),
        dayOfWeek = 1,
        isToday = isToday,
        episodes = episodes
    )

    private fun episode(
        seasonId: Int,
        episodeId: Int,
        publishAt: Long
    ) = TimelineEp(
        cover = "",
        title = "season-$seasonId",
        seasonId = seasonId,
        episodeId = episodeId,
        publishIndex = "",
        publishTime = "",
        publishDate = Date(publishAt)
    )
}
