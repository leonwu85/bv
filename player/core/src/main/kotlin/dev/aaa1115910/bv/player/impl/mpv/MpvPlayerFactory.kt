package dev.aaa1115910.bv.player.impl.mpv

import android.content.Context
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.player.factory.PlayerFactory

class MpvPlayerFactory : PlayerFactory<MpvMediaPlayer>() {
    override fun create(context: Context, options: VideoPlayerOptions): MpvMediaPlayer {
        return MpvMediaPlayer(context, options)
    }
}
