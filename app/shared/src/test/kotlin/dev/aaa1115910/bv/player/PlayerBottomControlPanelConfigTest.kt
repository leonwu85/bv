package dev.aaa1115910.bv.player

import dev.aaa1115910.bv.player.entity.PlayerBottomControlPanelButtonIds
import dev.aaa1115910.bv.player.entity.PlayerBottomControlPanelConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerBottomControlPanelConfigTest {
    @Test
    fun `normalizes missing and unknown action button ids`() {
        val config = PlayerBottomControlPanelConfig(
            actionButtonOrder = listOf(
                "unknown",
                PlayerBottomControlPanelButtonIds.Related,
                PlayerBottomControlPanelButtonIds.Like
            )
        ).normalized()

        assertEquals(
            listOf(
                PlayerBottomControlPanelButtonIds.Related,
                PlayerBottomControlPanelButtonIds.Like,
                PlayerBottomControlPanelButtonIds.Favorite,
                PlayerBottomControlPanelButtonIds.Cache,
                PlayerBottomControlPanelButtonIds.Coin,
                PlayerBottomControlPanelButtonIds.TripleLike,
                PlayerBottomControlPanelButtonIds.Description,
                PlayerBottomControlPanelButtonIds.Playlist
            ),
            config.actionButtonOrder
        )
    }

    @Test
    fun `normalizes duplicate and unknown function button ids`() {
        val config = PlayerBottomControlPanelConfig(
            functionButtonOrder = listOf(
                PlayerBottomControlPanelButtonIds.Settings,
                PlayerBottomControlPanelButtonIds.Settings,
                "unknown",
                PlayerBottomControlPanelButtonIds.Comment
            )
        ).normalized()

        assertEquals(
            listOf(
                PlayerBottomControlPanelButtonIds.Settings,
                PlayerBottomControlPanelButtonIds.Comment,
                PlayerBottomControlPanelButtonIds.PrevVideo,
                PlayerBottomControlPanelButtonIds.NextVideo,
                PlayerBottomControlPanelButtonIds.AudioMode,
                PlayerBottomControlPanelButtonIds.Danmaku,
                PlayerBottomControlPanelButtonIds.Subtitle,
                PlayerBottomControlPanelButtonIds.Loop,
                PlayerBottomControlPanelButtonIds.Speed,
                PlayerBottomControlPanelButtonIds.Refresh,
                PlayerBottomControlPanelButtonIds.Rotation
            ),
            config.functionButtonOrder
        )
    }

    @Test
    fun `inserts cache before coin for persisted legacy action order`() {
        val legacyOrder = listOf(
            PlayerBottomControlPanelButtonIds.Like,
            PlayerBottomControlPanelButtonIds.Favorite,
            PlayerBottomControlPanelButtonIds.Coin,
            PlayerBottomControlPanelButtonIds.TripleLike,
            PlayerBottomControlPanelButtonIds.Description,
            PlayerBottomControlPanelButtonIds.Playlist,
            PlayerBottomControlPanelButtonIds.Related
        )

        val normalized = PlayerBottomControlPanelConfig(
            actionButtonOrder = legacyOrder
        ).normalized()

        assertEquals(
            listOf(
                PlayerBottomControlPanelButtonIds.Like,
                PlayerBottomControlPanelButtonIds.Favorite,
                PlayerBottomControlPanelButtonIds.Cache,
                PlayerBottomControlPanelButtonIds.Coin,
                PlayerBottomControlPanelButtonIds.TripleLike,
                PlayerBottomControlPanelButtonIds.Description,
                PlayerBottomControlPanelButtonIds.Playlist,
                PlayerBottomControlPanelButtonIds.Related
            ),
            normalized.actionButtonOrder
        )
    }

    @Test
    fun `coerces scale values into supported range`() {
        val config = PlayerBottomControlPanelConfig(
            titleScale = 3f,
            infoScale = 2f
        ).normalized()

        assertEquals(PlayerBottomControlPanelConfig.TitleMaxScale, config.titleScale)
        assertEquals(PlayerBottomControlPanelConfig.MaxScale, config.infoScale)
    }

    @Test
    fun `steps title scale to title max`() {
        val nextScale = PlayerBottomControlPanelConfig.stepScale(
            scale = 1.9f,
            direction = 1,
            maxScale = PlayerBottomControlPanelConfig.TitleMaxScale
        )

        assertEquals(PlayerBottomControlPanelConfig.TitleMaxScale, nextScale)
    }
}
