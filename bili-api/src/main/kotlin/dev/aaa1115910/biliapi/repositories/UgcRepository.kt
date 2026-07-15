package dev.aaa1115910.biliapi.repositories

import dev.aaa1115910.biliapi.entity.CarouselData
import dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2
import dev.aaa1115910.biliapi.entity.ugc.region.UgcFeedData
import dev.aaa1115910.biliapi.entity.ugc.region.UgcFeedPage
import dev.aaa1115910.biliapi.http.BiliHttpApi
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single

@Single
class UgcRepository(
    private val authRepository: AuthRepository
) {
    private val logger = KotlinLogging.logger {}

    suspend fun getCarousel(ugcType: UgcTypeV2): CarouselData {
        return runCatching {
            val regionBanner = BiliHttpApi.getRegionBanner(ugcType.tid).getResponseData()
            CarouselData.fromRegionBanner(regionBanner)
        }.getOrElse {
            logger.warn(it) { "load $ugcType carousel failed, fallback to empty carousel" }
            CarouselData(emptyList())
        }
    }

    suspend fun getRegionFeedRcmd(ugcType: UgcTypeV2, page: UgcFeedPage): UgcFeedData {
        val responseData = BiliHttpApi.getRegionFeedRcmd(
            displayId = page.nextPage,
            fromRegion = ugcType.tid,
            sessData = authRepository.sessionData
        ).getResponseData()
        val ugcFeedData = UgcFeedData.fromRegionFeedRcmd(responseData)
        ugcFeedData.nextPage = UgcFeedPage(page.nextPage + 1)
        return ugcFeedData
    }
}
