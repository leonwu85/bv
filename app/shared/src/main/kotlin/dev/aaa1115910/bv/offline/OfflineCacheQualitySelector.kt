package dev.aaa1115910.bv.offline

import dev.aaa1115910.bv.player.entity.Resolution

object OfflineCacheQualitySelector {
    fun select(
        availableQualities: Collection<Resolution>,
        preferredQuality: Resolution,
    ): Resolution? {
        val sortedQualities = availableQualities
            .distinct()
            .sortedByDescending { it.code }

        return sortedQualities.firstOrNull { it == preferredQuality }
            ?: sortedQualities.firstOrNull { it.code < preferredQuality.code }
            ?: sortedQualities.lastOrNull()
    }
}
