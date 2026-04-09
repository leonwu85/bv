package dev.aaa1115910.bv.entity.carddata

import dev.aaa1115910.biliapi.entity.pgc.PgcBadge
import dev.aaa1115910.biliapi.http.entity.web.Hover
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.resizedImageUrl

data class SeasonCardData(
    val seasonId: Int,
    val title: String,
    val subTitle: String? = null,
    val cover: String,
    val rating: String? = null,
    val hover: Hover? = null,
    val badge: PgcBadge? = null,
    val coverLabel: String? = null,
) {
    companion object {
        fun fromPgcItem(pgcItem: dev.aaa1115910.biliapi.entity.pgc.PgcItem): SeasonCardData {
            return SeasonCardData(
                seasonId = pgcItem.seasonId,
                title = pgcItem.title,
                subTitle = pgcItem.subTitle,
                cover = pgcItem.cover.resizedImageUrl(ImageSize.SeasonCoverThumbnail),
                rating = pgcItem.rating,
                hover = pgcItem.hover,
                badge = pgcItem.badge,
                coverLabel = pgcItem.indexShow
            )
        }

        fun fromFollowingSeason(followingSeason: dev.aaa1115910.biliapi.entity.season.FollowingSeason): SeasonCardData {
            return SeasonCardData(
                seasonId = followingSeason.seasonId,
                title = followingSeason.title,
                cover = followingSeason.cover,
                rating = null,
                hover = null,
                badge = null,
                coverLabel = null
            )
        }
    }
}
