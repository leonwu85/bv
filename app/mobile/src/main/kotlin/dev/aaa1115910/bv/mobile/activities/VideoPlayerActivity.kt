package dev.aaa1115910.bv.mobile.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.lifecycle.lifecycleScope
import com.kuaishou.akdanmaku.render.SimpleRenderer
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.video.VideoDetail
import dev.aaa1115910.biliapi.entity.video.season.Episode
import dev.aaa1115910.biliapi.entity.video.season.SeasonDetail
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.PlayerType
import dev.aaa1115910.bv.mobile.screen.VideoPlayerScreen
import dev.aaa1115910.bv.mobile.settings.MobileRuntime
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme
import dev.aaa1115910.bv.player.entity.PlayerDefaultStartPosition
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.player.impl.exo.ExoPlayerFactory
import dev.aaa1115910.bv.player.impl.mpv.MpvPlayerFactory
import dev.aaa1115910.bv.player.impl.vlc.VlcPlayerFactory
import dev.aaa1115910.bv.settings.PlayerSettingsProvider
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.formatPubTimeString
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.CommentViewModel
import dev.aaa1115910.bv.viewmodel.SeasonViewModel
import dev.aaa1115910.bv.viewmodel.VideoPlayerV3ViewModel
import dev.aaa1115910.bv.viewmodel.video.VideoDetailViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel

data class VideoLaunchArgs(
    val isLive: Boolean,
    val aid: Long,
    val cid: Long,
    val fromSeason: Boolean,
    val fromToView: Boolean,
    val epid: Int?,
    val seasonId: Int?,
    val liveRoomId: Int,
    val title: String,
    val upName: String,
    val upFace: String,
    val upMid: Long,
    val liveWatchedNum: Int,
) {
    companion object {
        fun fromIntent(intent: Intent): VideoLaunchArgs {
            return VideoLaunchArgs(
                isLive = intent.getBooleanExtra("isLive", false),
                aid = intent.getLongExtra("aid", 0),
                cid = intent.getLongExtra("cid", 0),
                fromSeason = intent.getBooleanExtra("fromSeason", false),
                fromToView = intent.getBooleanExtra("fromToView", false),
                epid = intent.getIntExtra("epid", 0).takeIf { it != 0 },
                seasonId = intent.getIntExtra("seasonId", 0).takeIf { it != 0 },
                liveRoomId = intent.getIntExtra("liveRoomId", 0),
                title = intent.getStringExtra("title") ?: "Unknown Title",
                upName = intent.getStringExtra("upName") ?: "",
                upFace = intent.getStringExtra("upFace") ?: "",
                upMid = intent.getLongExtra("upMid", 0L),
                liveWatchedNum = intent.getIntExtra("liveWatchedNum", 0)
            )
        }
    }
}

class VideoPlayerActivity : ComponentActivity() {
    companion object {
        private fun formatPopularity(count: Int): String {
            return when {
                count >= 100_000_000 -> String.format("%.1f亿人气", count / 100_000_000.0)
                count >= 10_000 -> String.format("%.1f万人气", count / 10_000.0)
                else -> "${count}人气"
            }
        }

        fun actionStart(
            context: Context,
            aid: Long,
            //cid: Long,
            fromSeason: Boolean = false,
            fromToView: Boolean = false,
            epid: Int? = null,
            seasonId: Int? = null,
        ) {
            context.startActivity(
                Intent(context, VideoPlayerActivity::class.java).apply {
                    putExtra("aid", aid)
                    //putExtra("cid", cid)
                    putExtra("fromSeason", fromSeason)
                    putExtra("fromToView", fromToView)
                    epid?.let { putExtra("epid", it) }
                    seasonId?.let { putExtra("seasonId", it) }
                }
            )
        }

        fun actionStartLive(
            context: Context,
            roomId: Int,
            title: String,
            upName: String = "",
            upFace: String = "",
            upMid: Long = 0L,
            watchedNum: Int = 0
        ) {
            context.startActivity(
                Intent(context, VideoPlayerActivity::class.java).apply {
                    putExtra("isLive", true)
                    putExtra("liveRoomId", roomId)
                    putExtra("title", title)
                    putExtra("upName", upName)
                    putExtra("upFace", upFace)
                    putExtra("upMid", upMid)
                    putExtra("liveWatchedNum", watchedNum)
                }
            )
        }
    }

    private val playerViewModel: VideoPlayerV3ViewModel by viewModel()
    private val commentViewModel: CommentViewModel by viewModel()
    private val seasonViewModel: SeasonViewModel by viewModel()
    private val videoDetailViewModel: VideoDetailViewModel by viewModel()
    private val logger = KotlinLogging.logger {}

    private data class InitialVodPlaybackTarget(
        val aid: Long,
        val cid: Long,
        val epid: Int? = null,
        val seasonId: Int? = null,
        val subType: Int = 0,
        val playedMs: Int = 0,
    )

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        MobileRuntime.install()
        super.onCreate(savedInstanceState)
        val launchArgs = VideoLaunchArgs.fromIntent(intent)
        initVideoPlayer(launchArgs = launchArgs)
        if (!launchArgs.isLive) initDanmakuPlayer()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            BVMobileTheme {
                VideoPlayerScreen(
                    windowSizeClass = windowSizeClass
                )
            }
        }
    }

    private fun initVideoPlayer(launchArgs: VideoLaunchArgs) {
        if (playerViewModel.videoPlayer != null) return
        logger.fInfo { "initVideoPlayer: isLive=${launchArgs.isLive}" }
        val settings = PlayerSettingsProvider.current
        val enableTunneling = settings.enableMobileTunneling
        playerViewModel.currentDanmakuScale = settings.defaultMobileDanmakuScale
        val playbackUserAgent = if (launchArgs.isLive) {
            dev.aaa1115910.biliapi.BiliApiConstants.USER_AGENT_WEB
        } else {
            when (settings.apiType) {
                ApiType.Web -> dev.aaa1115910.biliapi.BiliApiConstants.USER_AGENT_WEB
                ApiType.App -> dev.aaa1115910.biliapi.BiliApiConstants.USER_AGENT_APP
            }
        }
        val playbackReferer = if (launchArgs.isLive) {
            getString(R.string.video_player_referer)
        } else {
            when (settings.apiType) {
                ApiType.Web -> getString(R.string.video_player_referer)
                ApiType.App -> null
            }
        }
        val options = VideoPlayerOptions(
            userAgent = playbackUserAgent,
            referer = playbackReferer,
            enableFfmpegAudioRenderer = settings.enableFfmpegAudioRenderer,
            enableAsyncQueueing = settings.enableAsyncQueueing,
            enableTunneling = enableTunneling,
            enableAudioPlaybackParams = settings.enableAudioPlaybackParams,
            enableHardwareDecode = settings.enableHardwareDecode,
            expandBuffer = settings.expandBuffer,
            autoSync = settings.autoSync,
            videoSync = settings.videoSync,
            hardwareDecodeMode = settings.hardwareDecodeMode,
            audioOutputDevices = settings.audioOutputDevices,
            isLive = launchArgs.isLive
        )
        val videoPlayer = when (settings.playerType) {
            PlayerType.Media3 -> ExoPlayerFactory().create(this, options)
            PlayerType.VLC -> VlcPlayerFactory().create(this, options)
            PlayerType.MPV -> MpvPlayerFactory().create(this, options)
        }
        playerViewModel.videoPlayer = videoPlayer
        //TODO 还没处理旋转后的一些判断，就先放这了
        parseIntent(launchArgs)
    }

    private fun initDanmakuPlayer() {
        if (playerViewModel.danmakuPlayer != null) return
        logger.fInfo { "initDanmakuPlayer" }
        playerViewModel.danmakuPlayer = DanmakuPlayer(SimpleRenderer())
    }

    private fun parseIntent(launchArgs: VideoLaunchArgs = VideoLaunchArgs.fromIntent(intent)) {
        val settings = PlayerSettingsProvider.current
        if (launchArgs.isLive) {
            logger.fInfo { "Launch live parameter: [roomId=${launchArgs.liveRoomId}, watchedNum=${launchArgs.liveWatchedNum}]" }
            playerViewModel.apply {
                this.title = launchArgs.title
                this.upName = launchArgs.upName
                this.upFace = launchArgs.upFace
                this.upId = launchArgs.upMid
                this.isLive = true
                this.liveRoomId = launchArgs.liveRoomId
                this.livePopularityText = if (launchArgs.liveWatchedNum > 0) formatPopularity(launchArgs.liveWatchedNum) else ""
                loadLiveStreamWithQuality(launchArgs.liveRoomId)
            }
            return
        }

        var aid = launchArgs.aid
        var cid = launchArgs.cid
        val fromSeason = launchArgs.fromSeason
        val epid = launchArgs.epid
        val seasonId = launchArgs.seasonId

        lifecycleScope.launch(Dispatchers.IO) {
            if (aid == 0L && cid == 0L) {
                runCatching {
                    val acid = BiliHttpApi.getAidCidByEpid(epid ?: 0)!!
                    aid = acid.first
                    cid = acid.second
                }.onFailure {
                    logger.fInfo { "get avid & cid by epid failed: ${it.stackTraceToString()}" }
                    withContext(Dispatchers.Main) {
                        it.message?.toast(this@VideoPlayerActivity)
                    }
                }
            }

            commentViewModel.commentType = 1
            commentViewModel.commentId = aid

            runCatching {
                videoDetailViewModel.setInToView(launchArgs.fromToView)
                videoDetailViewModel.loadDetail(aid, fromSeason)
            }.onFailure {
                withContext(Dispatchers.Main) {
                    it.message?.toast(this@VideoPlayerActivity)
                }
            }
            runCatching {
                val target = if (fromSeason) {
                    resolveSeasonPlaybackTarget(
                        aid = aid,
                        cid = cid,
                        epid = epid,
                        seasonId = seasonId,
                        settings = settings
                    )
                } else {
                    resolveUgcPlaybackTarget(
                        aid = aid,
                        cid = cid,
                        settings = settings
                    )
                }

                playerViewModel.fromSeason = fromSeason
                playerViewModel.lastPlayed = target.playedMs
                playerViewModel.subType = target.subType
                playerViewModel.epid = target.epid ?: 0
                playerViewModel.seasonId = target.seasonId ?: 0
                playerViewModel.loadPlayUrl(
                    avid = target.aid,
                    cid = target.cid,
                    epid = target.epid,
                    seasonId = target.seasonId
                )
            }.onFailure {
                withContext(Dispatchers.Main) {
                    it.message?.toast(this@VideoPlayerActivity)
                }
            }
        }
    }

    private suspend fun resolveUgcPlaybackTarget(
        aid: Long,
        cid: Long,
        settings: dev.aaa1115910.bv.settings.PlayerSettingsSource
    ): InitialVodPlaybackTarget {
        val detail = videoDetailViewModel.videoDetail
            ?: return InitialVodPlaybackTarget(aid = aid, cid = cid)

        val historyCid = detail.history.lastPlayedCid.takeIf { historyCid ->
            historyCid != 0L && detail.pages.any { it.cid == historyCid }
        }
        val targetCid = historyCid ?: cid.takeIf { it != 0L } ?: detail.cid
        val targetPage = detail.pages.firstOrNull { it.cid == targetCid }
            ?: detail.pages.firstOrNull()

        applyUgcDetailMetadata(detail, targetPage?.cid ?: targetCid)
        updateUgcSeasonVideoList(detail, targetPage?.cid ?: targetCid)

        return InitialVodPlaybackTarget(
            aid = detail.aid,
            cid = targetPage?.cid ?: targetCid,
            playedMs = if (
                settings.playerDefaultStartPosition == PlayerDefaultStartPosition.History &&
                historyCid == targetPage?.cid
            ) {
                detail.history.progress.coerceAtLeast(0) * 1000
            } else {
                0
            }
        )
    }

    private suspend fun resolveSeasonPlaybackTarget(
        aid: Long,
        cid: Long,
        epid: Int?,
        seasonId: Int?,
        settings: dev.aaa1115910.bv.settings.PlayerSettingsSource
    ): InitialVodPlaybackTarget {
        seasonViewModel.epId = epid
        seasonViewModel.seasonId = seasonId
        seasonViewModel.updateSeasonData()
        val season = seasonViewModel.seasonData

        val history = season?.userStatus?.progress
        val historyEpisode = history?.let { progress ->
            season.findEpisodeByEpId(progress.lastEpId)
        }
        val requestedEpisode = season?.findEpisodeByEpId(epid)
            ?: season?.findEpisodeByAidCid(aid, cid)
        val targetEpisode = historyEpisode ?: requestedEpisode ?: season?.episodes?.firstOrNull()

        return if (targetEpisode != null && season != null) {
            applySeasonMetadata(season, targetEpisode)
            InitialVodPlaybackTarget(
                aid = targetEpisode.aid,
                cid = targetEpisode.cid,
                epid = targetEpisode.epid,
                seasonId = season.seasonId,
                subType = season.subType,
                playedMs = if (
                    settings.playerDefaultStartPosition == PlayerDefaultStartPosition.History &&
                    historyEpisode?.epid == targetEpisode.epid
                ) {
                    history?.lastTime?.coerceAtLeast(0)?.times(1000) ?: 0
                } else {
                    0
                }
            )
        } else {
            InitialVodPlaybackTarget(
                aid = aid,
                cid = cid,
                epid = epid,
                seasonId = seasonId,
                subType = season?.subType ?: 0,
            )
        }
    }

    private fun updateUgcSeasonVideoList(detail: VideoDetail, targetCid: Long) {
        val ugcSeason = detail.ugcSeason ?: return
        val sectionIndex = ugcSeason.sections.indexOfFirst { section ->
            section.episodes.any { episode ->
                episode.cid == targetCid || episode.pages.any { page -> page.cid == targetCid }
            }
        }
        if (sectionIndex >= 0) {
            videoDetailViewModel.updateUgcSeasonSectionVideoList(sectionIndex)
        }
    }

    private fun applyUgcDetailMetadata(detail: VideoDetail, targetCid: Long) {
        val page = detail.pages.firstOrNull { it.cid == targetCid } ?: detail.pages.firstOrNull()
        playerViewModel.apply {
            title = detail.title
            partTitle = page?.title ?: ""
            playerIconIdle = detail.playerIcon?.idle ?: ""
            playerIconMoving = detail.playerIcon?.moving ?: ""
            play = detail.stat.view
            danmaku = detail.stat.danmaku
            like = detail.stat.like
            coin = detail.stat.coin
            favorite = detail.stat.favorite
            upName = detail.author.name
            upId = detail.author.mid
            upFace = detail.author.face
            pubTime = detail.publishDate.formatPubTimeString(this@VideoPlayerActivity)
            isVerticalVideo = page?.dimension?.isVertical ?: false
        }
    }

    private fun applySeasonMetadata(season: SeasonDetail, episode: Episode) {
        playerViewModel.apply {
            title = season.title
            partTitle = episode.longTitle.ifBlank { episode.title }
            playerIconIdle = season.playerIcon?.idle ?: ""
            playerIconMoving = season.playerIcon?.moving ?: ""
            play = episode.viewCount
            danmaku = episode.danmakuCount
            isVerticalVideo = episode.dimension?.isVertical ?: false
        }
    }

    private fun SeasonDetail.findEpisodeByEpId(epId: Int?): Episode? {
        if (epId == null) return null
        return allEpisodes().firstOrNull { it.epid == epId }
    }

    private fun SeasonDetail.findEpisodeByAidCid(aid: Long, cid: Long): Episode? {
        return allEpisodes().firstOrNull { episode ->
            (aid != 0L && episode.aid == aid) || (cid != 0L && episode.cid == cid)
        }
    }

    private fun SeasonDetail.allEpisodes(): List<Episode> {
        return episodes + sections.flatMap { it.episodes }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerViewModel.videoPlayer?.release()
        playerViewModel.liveDanmakuPlayer?.release()
        if (isFinishing) {
            playerViewModel.videoPlayer = null
            playerViewModel.danmakuPlayer = null
            playerViewModel.liveDanmakuPlayer = null
        }
    }

    override fun onPause() {
        super.onPause()
        if (playerViewModel.isLive) {
            playerViewModel.stopLiveDanmaku()
        } else {
            playerViewModel.videoPlayer?.pause()
            playerViewModel.danmakuPlayer?.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        playerViewModel.resumeLiveDanmakuIfNeeded()
    }
}
