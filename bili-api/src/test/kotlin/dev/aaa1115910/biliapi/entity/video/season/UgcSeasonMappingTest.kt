package dev.aaa1115910.biliapi.entity.video.season

import dev.aaa1115910.biliapi.http.entity.video.Dimension
import dev.aaa1115910.biliapi.http.entity.video.UgcSeason as HttpUgcSeason
import dev.aaa1115910.biliapi.http.entity.video.VideoPage
import dev.aaa1115910.biliapi.http.entity.video.VideoRights
import dev.aaa1115910.biliapi.http.entity.video.VideoStat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UgcSeasonMappingTest {
    @Test
    fun `ugc season episodes inherit upower exclusive badge`() {
        val mappedSeason = UgcSeason.fromUgcSeason(
            ugcSeason = createHttpUgcSeason(
                isChargeableSeason = false,
                payFreeWatch = 0
            ),
            ugcSeasonIsChargingArc = true,
            ugcSeasonChargingArcBadge = "充电专属"
        )

        val episode = mappedSeason.sections.single().episodes.single()
        assertTrue(episode.isChargingArc)
        assertEquals("充电专属", episode.chargingArcBadge)
    }

    @Test
    fun `ugc season episodes keep limited free badge when arc says pay free watch`() {
        val mappedSeason = UgcSeason.fromUgcSeason(
            ugcSeason = createHttpUgcSeason(
                isChargeableSeason = true,
                payFreeWatch = 1
            )
        )

        val episode = mappedSeason.sections.single().episodes.single()
        assertTrue(episode.isChargingArc)
        assertEquals("限时免费", episode.chargingArcBadge)
    }

    private fun createHttpUgcSeason(
        isChargeableSeason: Boolean,
        payFreeWatch: Int,
    ) = HttpUgcSeason(
        id = 1,
        title = "合集",
        cover = "cover",
        mid = 1L,
        intro = "",
        signState = 0,
        attribute = 0,
        sections = listOf(
            HttpUgcSeason.Section(
                seasonId = 1,
                id = 1L,
                title = "正片",
                type = 0,
                episodes = listOf(
                    HttpUgcSeason.Section.Episode(
                        seasonId = 1,
                        sectionId = 1,
                        id = 1,
                        aid = 2L,
                        cid = 3L,
                        title = "第 1 集",
                        attribute = 0,
                        arc = HttpUgcSeason.Section.Episode.Arc(
                            aid = 2L,
                            videos = 1,
                            typeId = 0,
                            typeName = "",
                            copyright = 1,
                            pic = "cover",
                            title = "第 1 集",
                            pubDate = 0,
                            ctime = 0,
                            desc = "",
                            state = 0,
                            duration = 120,
                            rights = VideoRights(
                                bp = 0,
                                elec = 0,
                                download = 1,
                                movie = 0,
                                pay = 0,
                                hd5 = 1,
                                noReprint = 0,
                                autoplay = 0,
                                ugcPay = 0,
                                isCooperation = 0,
                                ugcPayPreview = 0,
                                noBackground = 0,
                                cleanMode = 0,
                                isSteinGate = 0,
                                is360 = 0,
                                noShare = 0,
                                arcPay = 0,
                                payFreeWatch = payFreeWatch
                            ),
                            stat = VideoStat(
                                aid = 2L,
                                view = 0,
                                danmaku = 0,
                                reply = 0,
                                favorite = 0,
                                coin = 0,
                                share = 0,
                                nowRank = 0,
                                hisRank = 0,
                                like = 0,
                                dislike = 0,
                                evaluation = "",
                                argueMsg = ""
                            ),
                            dynamic = "",
                            isChargeableSeason = isChargeableSeason,
                            isBlooper = false
                        ),
                        page = createVideoPage(cid = 3L),
                        bvid = "BV1xx411c7BF",
                        pages = listOf(createVideoPage(cid = 3L))
                    )
                )
            )
        )
    )

    private fun createVideoPage(cid: Long) = VideoPage(
        cid = cid,
        page = 1,
        from = "vupload",
        part = "第 1 集",
        duration = 120,
        vid = "",
        weblink = "",
        dimension = Dimension(width = 1920, height = 1080, rotate = 0)
    )
}