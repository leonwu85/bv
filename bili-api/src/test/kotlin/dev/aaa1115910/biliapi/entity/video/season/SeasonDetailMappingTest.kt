package dev.aaa1115910.biliapi.entity.video.season

import dev.aaa1115910.biliapi.http.entity.season.AppSeasonData
import dev.aaa1115910.biliapi.http.entity.season.WebSeasonData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SeasonDetailMappingTest {
    @Test
    fun `season episode mapping keeps entitlement fields`() {
        val mappedEpisode = Episode.fromEpisode(
            episode = dev.aaa1115910.biliapi.http.entity.season.Episode(
                aid = 1L,
                badge = "会员",
                badgeInfo = dev.aaa1115910.biliapi.http.entity.season.Episode.BadgeInfo(
                    bgColor = "#fb7299",
                    bgColorNight = "#fb7299",
                    text = "会员"
                ),
                bvid = "BV1xx411c7BF",
                cid = 2L,
                cover = "cover",
                enableVt = false,
                epId = 3,
                id = 3,
                isViewHide = false,
                link = "https://www.bilibili.com/bangumi/play/ep3",
                longTitle = "第一集",
                pubTime = 0L,
                pv = 0,
                shortLink = "https://b23.tv/ep3",
                status = 13,
                title = "1"
            )
        )

        assertEquals(13, mappedEpisode.status)
        assertEquals("https://b23.tv/ep3", mappedEpisode.shortLink)
    }

    @Test
    fun `web season user status maps vip info`() {
        val mappedStatus = SeasonDetail.UserStatus.fromUserStatus(
            userStatus = WebSeasonData.UserStatus(
                areaLimit = 0,
                banAreaShow = 0,
                follow = 0,
                followStatus = 0,
                login = 1,
                pay = 0,
                payPackPaid = 0,
                sponsor = 0,
                vipInfo = WebSeasonData.UserStatus.VipInfo(
                    dueDate = 0L,
                    status = 1,
                    type = 2
                )
            )
        )

        assertTrue(mappedStatus.isVip)
    }

    @Test
    fun `app season user status maps vip flag`() {
        val mappedVipStatus = SeasonDetail.UserStatus.fromUserStatus(
            userStatus = AppSeasonData.UserStatus(
                follow = 0,
                followBubble = 0,
                followStatus = 0,
                pay = 0,
                payFor = 0,
                sponsor = 0,
                vip = 1,
                vipFrozen = 0
            )
        )
        val mappedNormalStatus = SeasonDetail.UserStatus.fromUserStatus(
            userStatus = AppSeasonData.UserStatus(
                follow = 0,
                followBubble = 0,
                followStatus = 0,
                pay = 0,
                payFor = 0,
                sponsor = 0,
                vip = 0,
                vipFrozen = 0
            )
        )

        assertTrue(mappedVipStatus.isVip)
        assertFalse(mappedNormalStatus.isVip)
    }
}