package dev.aaa1115910.bv.tv.activities.video

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import dev.aaa1115910.biliapi.BiliApiConstants
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.PlayerType
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.player.entity.PlaybackMediaMode
import dev.aaa1115910.bv.player.impl.exo.ExoPlayerFactory
import dev.aaa1115910.bv.player.impl.mpv.MpvPlayerFactory
import dev.aaa1115910.bv.player.impl.vlc.VlcPlayerFactory
import dev.aaa1115910.bv.tv.activities.TvComponentActivity
import dev.aaa1115910.bv.tv.screens.OfflineVideoPlayerScreen
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.VideoPlayerV3ViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class OfflineVideoPlayerActivity : TvComponentActivity() {
    companion object {
        private const val EXTRA_AID = "offline_aid"
        private const val EXTRA_CID = "offline_cid"

        fun actionStart(context: Context, aid: Long, cid: Long) {
            context.startActivity(
                Intent(context, OfflineVideoPlayerActivity::class.java).apply {
                    putExtra(EXTRA_AID, aid)
                    putExtra(EXTRA_CID, cid)
                }
            )
            if (context is VideoPlayerV3Activity) {
                context.finish()
            }
        }
    }

    private val playerViewModel: VideoPlayerV3ViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initVideoPlayer()
        Prefs.currentPlaySpeed = Prefs.defaultPlaySpeed
        playerViewModel.currentPlaySpeed = Prefs.defaultPlaySpeed
        playerViewModel.currentPlaybackMediaMode = PlaybackMediaMode.Normal

        val aid = intent.getLongExtra(EXTRA_AID, 0L)
        val cid = intent.getLongExtra(EXTRA_CID, 0L)
        val preparation = playerViewModel.prepareOfflinePlayback(aid, cid)
        if (preparation.isFailure) {
            (preparation.exceptionOrNull()?.localizedMessage ?: "离线缓存不可用").toast(this)
            finish()
            return
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            BVTheme(forceDark = true) {
                OfflineVideoPlayerScreen()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        playerViewModel.videoPlayer?.pause()
        playerViewModel.danmakuPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun initVideoPlayer() {
        playerViewModel.currentDanmakuScale = Prefs.defaultTvDanmakuScale
        val options = VideoPlayerOptions(
            userAgent = BiliApiConstants.USER_AGENT_WEB,
            referer = getString(R.string.video_player_referer),
            enableFfmpegAudioRenderer = Prefs.enableFfmpegAudioRenderer,
            enableAsyncQueueing = Prefs.enableAsyncQueueing,
            enableTunneling = Prefs.enableTvTunneling,
            enableAudioPlaybackParams = Prefs.enableAudioPlaybackParams,
            hardwareDecodeMode = Prefs.tvMpvHardwareDecodeMode,
            mpvHardwareDecodeCodecs = Prefs.tvMpvHardwareDecodeCodecs,
            mpvVideoOutput = Prefs.tvMpvVideoOutput,
            mpvGpuContext = Prefs.tvMpvGpuContext,
            mpvGpuApi = Prefs.tvMpvGpuApi,
            mpvCache = Prefs.tvMpvCache,
            mpvDemuxerMaxBytes = Prefs.tvMpvDemuxerMaxBytes,
            mpvDemuxerMaxBackBytes = Prefs.tvMpvDemuxerMaxBackBytes,
            mpvVdQueueEnable = Prefs.tvMpvVdQueueEnable,
            superResolutionType = Prefs.superResolutionType,
            enableVideoFrameRateStrategy = false,
            isLive = false
        )
        playerViewModel.videoPlayer = when (Prefs.playerType) {
            PlayerType.Media3 -> ExoPlayerFactory().create(this, options)
            PlayerType.VLC -> VlcPlayerFactory().create(this, options)
            PlayerType.MPV -> MpvPlayerFactory().create(this, options)
        }
    }
}
