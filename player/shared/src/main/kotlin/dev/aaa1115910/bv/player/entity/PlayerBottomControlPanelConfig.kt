package dev.aaa1115910.bv.player.entity

object PlayerBottomControlPanelButtonIds {
    const val Like = "like"
    const val Favorite = "fav"
    const val Coin = "coin"
    const val TripleLike = "tripleLike"
    const val Description = "description"
    const val Playlist = "playlist"
    const val Related = "related"

    const val Comment = "comment"
    const val PrevVideo = "prevVideo"
    const val NextVideo = "nextVideo"
    const val AudioMode = "audioMode"
    const val Danmaku = "danmaku"
    const val Subtitle = "subtitle"
    const val Loop = "loop"
    const val Speed = "speed"
    const val Refresh = "refresh"
    const val Rotation = "rotation"
    const val Settings = "settings"
}

data class PlayerBottomControlPanelConfig(
    val titleScale: Float = DefaultScale,
    val infoScale: Float = DefaultScale,
    val actionRowScale: Float = DefaultScale,
    val seekBarScale: Float = DefaultScale,
    val functionRowScale: Float = DefaultScale,
    val actionButtonOrder: List<String> = DefaultActionButtonOrder,
    val functionButtonOrder: List<String> = DefaultFunctionButtonOrder
) {
    fun normalized(): PlayerBottomControlPanelConfig {
        return copy(
            titleScale = titleScale.coerceTitleScale(),
            infoScale = infoScale.coerceScale(),
            actionRowScale = actionRowScale.coerceScale(),
            seekBarScale = seekBarScale.coerceScale(),
            functionRowScale = functionRowScale.coerceScale(),
            actionButtonOrder = normalizeOrder(actionButtonOrder, DefaultActionButtonOrder),
            functionButtonOrder = normalizeOrder(functionButtonOrder, DefaultFunctionButtonOrder)
        )
    }

    fun orderedActionButtons(availableIds: Collection<String>): List<String> {
        return orderedButtons(actionButtonOrder, DefaultActionButtonOrder, availableIds)
    }

    fun orderedFunctionButtons(availableIds: Collection<String>): List<String> {
        return orderedButtons(functionButtonOrder, DefaultFunctionButtonOrder, availableIds)
    }

    companion object {
        const val MinScale = 0.8f
        const val MaxScale = 1.4f
        const val TitleMaxScale = 2f
        const val ScaleStep = 0.1f
        const val DefaultScale = 1f

        val DefaultActionButtonOrder = listOf(
            PlayerBottomControlPanelButtonIds.Like,
            PlayerBottomControlPanelButtonIds.Favorite,
            PlayerBottomControlPanelButtonIds.Coin,
            PlayerBottomControlPanelButtonIds.TripleLike,
            PlayerBottomControlPanelButtonIds.Description,
            PlayerBottomControlPanelButtonIds.Playlist,
            PlayerBottomControlPanelButtonIds.Related
        )

        val DefaultFunctionButtonOrder = listOf(
            PlayerBottomControlPanelButtonIds.Comment,
            PlayerBottomControlPanelButtonIds.PrevVideo,
            PlayerBottomControlPanelButtonIds.NextVideo,
            PlayerBottomControlPanelButtonIds.AudioMode,
            PlayerBottomControlPanelButtonIds.Danmaku,
            PlayerBottomControlPanelButtonIds.Subtitle,
            PlayerBottomControlPanelButtonIds.Loop,
            PlayerBottomControlPanelButtonIds.Speed,
            PlayerBottomControlPanelButtonIds.Refresh,
            PlayerBottomControlPanelButtonIds.Rotation,
            PlayerBottomControlPanelButtonIds.Settings
        )

        val Default = PlayerBottomControlPanelConfig()

        fun stepScale(
            scale: Float,
            direction: Int,
            maxScale: Float = MaxScale
        ): Float {
            val next = scale + (ScaleStep * direction)
            return ((next * 10).toInt() / 10f).coerceIn(MinScale, maxScale)
        }

        fun normalizeOrder(order: List<String>, defaultOrder: List<String>): List<String> {
            val allowed = defaultOrder.toSet()
            val seen = mutableSetOf<String>()
            val configured = order.filter { id -> id in allowed && seen.add(id) }
            return configured + defaultOrder.filterNot { it in seen }
        }

        private fun orderedButtons(
            order: List<String>,
            defaultOrder: List<String>,
            availableIds: Collection<String>
        ): List<String> {
            val available = availableIds.toSet()
            if (available.isEmpty()) return emptyList()
            return normalizeOrder(order, defaultOrder).filter { it in available }
        }

        private fun Float.coerceScale(): Float = coerceIn(MinScale, MaxScale)

        private fun Float.coerceTitleScale(): Float = coerceIn(MinScale, TitleMaxScale)
    }
}
