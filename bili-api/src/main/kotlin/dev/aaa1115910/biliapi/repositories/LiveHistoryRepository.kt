package dev.aaa1115910.biliapi.repositories

import dev.aaa1115910.biliapi.entity.live.LiveHistoryPage
import dev.aaa1115910.biliapi.entity.live.LiveHistoryCursor
import dev.aaa1115910.biliapi.entity.live.toLiveHistoryItem
import dev.aaa1115910.biliapi.http.BiliHttpApi
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single

@Single
class LiveHistoryRepository(
    private val authRepository: AuthRepository
) {
    private val logger = KotlinLogging.logger("LiveHistoryRepository")

    suspend fun getLiveHistories(
        max: Long,
        viewAt: Long,
        business: String,
        pageSize: Int = 20
    ): LiveHistoryPage {
        val sessData = authRepository.sessionData ?: return LiveHistoryPage(
            items = emptyList(),
            cursor = LiveHistoryCursor(),
            hasMore = false
        )

        var requestMax = max
        var requestViewAt = viewAt
        var requestBusiness = business
        var hasMore = true
        var attempts = 0
        val items = mutableListOf<dev.aaa1115910.biliapi.entity.live.LiveHistoryItem>()

        while (attempts < 5 && hasMore && items.isEmpty()) {
            attempts++
            val data = BiliHttpApi.getHistories(
                max = requestMax,
                business = requestBusiness,
                type = "live",
                viewAt = requestViewAt,
                pageSize = pageSize,
                sessData = sessData
            ).getResponseData()

            items += data.list.mapNotNull { it.toLiveHistoryItem() }

            val nextCursor = LiveHistoryCursor(
                max = data.cursor.max,
                viewAt = data.cursor.viewAt,
                business = data.cursor.business
            )
            hasMore = nextCursor.viewAt != 0L &&
                (nextCursor.max != requestMax || nextCursor.viewAt != requestViewAt)
            requestMax = nextCursor.max
            requestViewAt = nextCursor.viewAt
            requestBusiness = nextCursor.business
        }

        logger.info {
            "Loaded ${items.size} live history items, nextMax=$requestMax, nextViewAt=$requestViewAt, nextBusiness=$requestBusiness, hasMore=$hasMore"
        }

        return LiveHistoryPage(
            items = items.distinctBy { "${it.roomId}:${it.viewAt}" },
            cursor = if (hasMore) {
                LiveHistoryCursor(
                    max = requestMax,
                    viewAt = requestViewAt,
                    business = requestBusiness
                )
            } else {
                LiveHistoryCursor()
            },
            hasMore = hasMore
        )
    }
}