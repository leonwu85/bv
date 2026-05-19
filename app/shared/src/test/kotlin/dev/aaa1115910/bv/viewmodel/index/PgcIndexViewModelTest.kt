package dev.aaa1115910.bv.viewmodel.index

import dev.aaa1115910.biliapi.entity.pgc.PgcItem
import dev.aaa1115910.biliapi.http.SeasonIndexType
import kotlin.test.Test
import kotlin.test.assertEquals

class PgcIndexViewModelTest {
    @Test
    fun deduplicatePgcItemsBySeasonIdKeepsFirstIncomingItem() {
        val items = deduplicatePgcItemsBySeasonId(
            existingItems = emptyList(),
            newItems = listOf(
                pgcItem(seasonId = 142986, title = "飞驰人生3"),
                pgcItem(seasonId = 142986, title = "飞驰人生3"),
                pgcItem(seasonId = 142987, title = "飞驰人生4")
            )
        )

        assertEquals(listOf(142986, 142987), items.map { it.seasonId })
        assertEquals("飞驰人生3", items.first().title)
    }

    @Test
    fun deduplicatePgcItemsBySeasonIdSkipsItemsAlreadyLoaded() {
        val items = deduplicatePgcItemsBySeasonId(
            existingItems = listOf(pgcItem(seasonId = 142986, title = "飞驰人生3")),
            newItems = listOf(
                pgcItem(seasonId = 142986, title = "重复条目"),
                pgcItem(seasonId = 142987, title = "飞驰人生4")
            )
        )

        assertEquals(listOf(142987), items.map { it.seasonId })
    }

    private fun pgcItem(
        seasonId: Int,
        title: String
    ) = PgcItem(
        cover = "",
        title = title,
        subTitle = "",
        seasonId = seasonId,
        episodeId = seasonId,
        seasonType = SeasonIndexType.Anime,
        rating = "0"
    )
}
