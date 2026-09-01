package dev.aaa1115910.bv.player.impl.vlc

import android.content.Context
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.player.factory.PlayerFactory
import dev.aaa1115910.bv.player.impl.exo.ExoMediaPlayer
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * VLC 播放器工厂
 *
 * 用于创建 VlcMediaPlayer 实例的工厂类
 */
class VlcPlayerFactory : PlayerFactory<AbstractVideoPlayer>() {
    private val logger = KotlinLogging.logger { }

    override fun create(context: Context, options: VideoPlayerOptions): AbstractVideoPlayer {
        return try {
            VlcMediaPlayer(context, options)
        } catch (error: LinkageError) {
            logger.error(error) {
                "VLC native libraries are unavailable for this process; falling back to Media3"
            }
            ExoMediaPlayer(context, options)
        }
    }
}
