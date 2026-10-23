package dev.aaa1115910.bv.offline

import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.util.MediaQualitySelector

object OfflineCacheQualitySelector {
    fun select(
        availableQualities: Collection<Resolution>,
        preferredQuality: Resolution,
    ): Resolution? = MediaQualitySelector.video(availableQualities, preferredQuality)
}
