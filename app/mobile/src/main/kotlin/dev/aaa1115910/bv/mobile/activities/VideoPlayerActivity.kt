package dev.aaa1115910.bv.mobile.activities

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.MainThread
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.kuaishou.akdanmaku.render.SimpleRenderer
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.ugc.UgcItem
import dev.aaa1115910.biliapi.entity.user.DynamicItem as BiliDynamicItem
import dev.aaa1115910.biliapi.entity.user.DynamicVideo
import dev.aaa1115910.biliapi.entity.user.SpaceVideo
import dev.aaa1115910.biliapi.entity.video.RelatedVideo
import dev.aaa1115910.biliapi.entity.video.VideoDetail
import dev.aaa1115910.biliapi.entity.video.season.Episode
import dev.aaa1115910.biliapi.entity.video.season.SeasonDetail
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.PlayerType
import dev.aaa1115910.bv.entity.carddata.VideoCardData
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
import dev.aaa1115910.bv.util.removeHtmlTags
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.CommentViewModel
import dev.aaa1115910.bv.viewmodel.SeasonViewModel
import dev.aaa1115910.bv.viewmodel.VideoPlayerV3ViewModel
import dev.aaa1115910.bv.viewmodel.video.VideoDetailViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel

data class VideoLaunchArgs(
    val isLive: Boolean,
    val aid: Long,
    val cid: Long,
    val fromSeason: Boolean,
    val fromToView: Boolean,
    val playOfflineCache: Boolean,
    val cover: String,
    val partTitle: String,
    val epid: Int?,
    val seasonId: Int?,
    val liveRoomId: Int,
    val title: String,
    val upName: String,
    val upFace: String,
    val upMid: Long,
    val play: Long,
    val danmaku: Int,
    val pubTime: String,
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
                playOfflineCache = intent.getBooleanExtra("playOfflineCache", false),
                cover = intent.getStringExtra("cover") ?: "",
                partTitle = intent.getStringExtra("partTitle") ?: "",
                epid = intent.getIntExtra("epid", 0).takeIf { it != 0 },
                seasonId = intent.getIntExtra("seasonId", 0).takeIf { it != 0 },
                liveRoomId = intent.getIntExtra("liveRoomId", 0),
                title = intent.getStringExtra("title") ?: "",
                upName = intent.getStringExtra("upName") ?: "",
                upFace = intent.getStringExtra("upFace") ?: "",
                upMid = intent.getLongExtra("upMid", 0L),
                play = intent.getLongExtra("play", 0L),
                danmaku = intent.getIntExtra("danmaku", 0),
                pubTime = intent.getStringExtra("pubTime") ?: "",
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

        private fun parseDynamicStatText(text: String): Long {
            val normalized = text.trim().replace(",", "")
            if (normalized.isBlank()) return 0L

            val multiplier = when {
                "亿" in normalized -> 100_000_000.0
                "万" in normalized -> 10_000.0
                else -> 1.0
            }
            val value = Regex("""\d+(?:\.\d+)?""").find(normalized)?.value?.toDoubleOrNull() ?: 0.0
            return (value * multiplier).toLong()
        }

        fun actionStart(
            context: Context,
            aid: Long,
            cid: Long = 0L,
            fromSeason: Boolean = false,
            fromToView: Boolean = false,
            cover: String = "",
            title: String = "",
            partTitle: String = "",
            upName: String = "",
            upFace: String = "",
            upMid: Long = 0L,
            play: Long = 0L,
            danmaku: Int = 0,
            pubTime: String = "",
            epid: Int? = null,
            seasonId: Int? = null,
            playOfflineCache: Boolean = false,
        ) {
            context.startActivity(
                Intent(context, VideoPlayerActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("aid", aid)
                    putExtra("cid", cid)
                    putExtra("fromSeason", fromSeason)
                    putExtra("fromToView", fromToView)
                    putExtra("playOfflineCache", playOfflineCache)
                    putExtra("cover", cover)
                    putExtra("title", title)
                    putExtra("partTitle", partTitle)
                    putExtra("upName", upName)
                    putExtra("upFace", upFace)
                    putExtra("upMid", upMid)
                    putExtra("play", play)
                    putExtra("danmaku", danmaku)
                    putExtra("pubTime", pubTime)
                    epid?.let { putExtra("epid", it) }
                    seasonId?.let { putExtra("seasonId", it) }
                }
            )
        }

        fun actionStart(
            context: Context,
            video: VideoCardData,
            fromToView: Boolean = false
        ) {
            actionStart(
                context = context,
                aid = video.avid,
                fromSeason = video.jumpToSeason,
                fromToView = fromToView,
                cover = video.cover,
                title = video.title,
                upName = video.upName,
                upFace = video.upFace,
                upMid = video.upId,
                play = video.play ?: 0L,
                danmaku = video.danmaku ?: 0,
                pubTime = video.pubTime.orEmpty(),
                epid = video.epId,
                seasonId = video.seasonId
            )
        }

        fun actionStart(
            context: Context,
            video: UgcItem
        ) {
            actionStart(
                context = context,
                aid = video.aid,
                cover = video.cover,
                title = video.title.removeHtmlTags(),
                upName = video.author,
                upFace = video.authorFace,
                upMid = video.authorId,
                play = video.play.coerceAtLeast(0L),
                danmaku = video.danmaku.coerceAtLeast(0),
                pubTime = video.pubTime.orEmpty()
            )
        }

        fun actionStart(
            context: Context,
            video: DynamicVideo
        ) {
            actionStart(
                context = context,
                aid = video.aid,
                fromSeason = video.seasonId != null && video.seasonId != 0,
                cover = video.cover,
                title = video.title,
                upName = video.author,
                upFace = video.authorFace.ifBlank { video.avatar },
                upMid = video.authorId,
                play = video.play.coerceAtLeast(0L),
                danmaku = video.danmaku.coerceAtLeast(0),
                pubTime = video.pubTime.orEmpty(),
                epid = video.epid,
                seasonId = video.seasonId
            )
        }

        fun actionStart(
            context: Context,
            dynamicItem: BiliDynamicItem
        ) {
            dynamicItem.video?.let { video ->
                actionStart(
                    context = context,
                    aid = video.aid,
                    fromSeason = video.seasonId != null && video.seasonId != 0,
                    cover = video.cover,
                    title = video.title,
                    upName = dynamicItem.author.author,
                    upFace = dynamicItem.author.avatar,
                    upMid = dynamicItem.author.mid,
                    play = parseDynamicStatText(video.play),
                    danmaku = parseDynamicStatText(video.danmaku)
                        .coerceAtMost(Int.MAX_VALUE.toLong())
                        .toInt(),
                    pubTime = dynamicItem.author.pubTime,
                    epid = video.epid,
                    seasonId = video.seasonId
                )
                return
            }

            dynamicItem.pgc?.let { pgc ->
                actionStart(
                    context = context,
                    aid = pgc.aid,
                    fromSeason = true,
                    cover = pgc.cover,
                    title = pgc.title,
                    epid = pgc.epid,
                    seasonId = pgc.seasonId
                )
            }
        }

        fun actionStart(
            context: Context,
            video: SpaceVideo
        ) {
            actionStart(
                context = context,
                aid = video.aid,
                cover = video.cover,
                title = video.title,
                upName = video.author,
                upMid = video.authorId,
                play = video.play.coerceAtLeast(0L),
                danmaku = video.danmaku.coerceAtLeast(0),
                pubTime = video.publishDate.formatPubTimeString(context)
            )
        }

        fun actionStart(
            context: Context,
            relatedVideo: RelatedVideo
        ) {
            actionStart(
                context = context,
                aid = relatedVideo.aid,
                fromSeason = relatedVideo.jumpToSeason,
                cover = relatedVideo.cover,
                title = relatedVideo.title,
                upName = relatedVideo.author?.name.orEmpty(),
                upFace = relatedVideo.author?.face.orEmpty(),
                upMid = relatedVideo.author?.mid ?: 0L,
                play = relatedVideo.view.coerceAtLeast(0L),
                danmaku = relatedVideo.danmaku.coerceAtLeast(0),
                pubTime = relatedVideo.pubTime.orEmpty(),
                epid = relatedVideo.epid
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
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
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
    private var pipModeActive by mutableStateOf(false)
    private var pgcEpisodeRefreshJob: Job? = null
    private var vodLaunchJob: Job? = null

    private val pictureInPictureSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

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

        // Resolve every Activity-scoped ViewModel on the main thread before parseIntent can
        // launch asynchronous work. Passing the same instances into Compose also prevents
        // koinViewModel() and the background startup path from racing to create a ViewModel.
        val activityPlayerViewModel = playerViewModel
        val activityCommentViewModel = commentViewModel
        val activitySeasonViewModel = seasonViewModel
        val activityVideoDetailViewModel = videoDetailViewModel

        val launchArgs = VideoLaunchArgs.fromIntent(intent)
        initVideoPlayer(launchArgs = launchArgs)
        if (!launchArgs.isLive) initDanmakuPlayer()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            BVMobileTheme(themeBackgroundEnabled = !activityPlayerViewModel.isLive) {
                VideoPlayerScreen(
                    playerViewModel = activityPlayerViewModel,
                    commentVideModel = activityCommentViewModel,
                    seasonVideModel = activitySeasonViewModel,
                    videoDetailViewModel = activityVideoDetailViewModel,
                    windowSizeClass = windowSizeClass,
                    isInPictureInPictureMode = pipModeActive,
                    pictureInPictureSupported = pictureInPictureSupported,
                    onEnterPictureInPicture = ::enterPlayerPictureInPicture,
                    onPlayPgcEpisode = ::playPgcEpisode
                )
            }
        }
    }

    private fun enterPlayerPictureInPicture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (!pictureInPictureSupported || pipModeActive || isFinishing) return

        val width = playerViewModel.currentVideoWidth.takeIf { it > 0 }
            ?: playerViewModel.videoPlayer?.videoWidth?.takeIf { it > 0 }
            ?: 16
        val height = playerViewModel.currentVideoHeight.takeIf { it > 0 }
            ?: playerViewModel.videoPlayer?.videoHeight?.takeIf { it > 0 }
            ?: 9
        val aspectRatio = when {
            width.toFloat() / height > 2.39f -> Rational(239, 100)
            width.toFloat() / height < 1f / 2.39f -> Rational(100, 239)
            else -> Rational(width, height)
        }

        runCatching {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build()
            )
        }.onFailure {
            logger.warn(it) { "Enter picture-in-picture failed" }
            "无法进入画中画：${it.localizedMessage ?: "系统不支持"}".toast(this)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipModeActive = isInPictureInPictureMode
    }

    private fun playPgcEpisode(episode: Episode) {
        val season = seasonViewModel.seasonData
        val episodeId = episode.epid ?: episode.id.takeIf { it > 0 }
        if (season == null || episode.aid <= 0L || episode.cid <= 0L || episodeId == null) {
            "剧集信息无效，无法播放".toast(this)
            return
        }

        seasonViewModel.epId = episodeId
        seasonViewModel.seasonId = season.seasonId
        playerViewModel.fromSeason = true
        playerViewModel.epid = episodeId
        playerViewModel.seasonId = season.seasonId
        playerViewModel.subType = season.subType
        applySeasonMetadata(season, episode)
        commentViewModel.setCommentTarget(commentId = episode.aid, commentType = 1)
        playerViewModel.loadPlayUrl(
            avid = episode.aid,
            cid = episode.cid,
            epid = episodeId,
            seasonId = season.seasonId,
            continuePlayNext = true
        )

        pgcEpisodeRefreshJob?.cancel()
        pgcEpisodeRefreshJob = lifecycleScope.launch {
            launch {
                runCatching {
                    videoDetailViewModel.loadDetail(
                        aid = episode.aid,
                        fromPgcSeason = true
                    )
                }.onFailure {
                    if (it is CancellationException) throw it
                    logger.fInfo { "Refresh selected PGC episode detail failed: ${it.stackTraceToString()}" }
                    it.message?.toast(this@VideoPlayerActivity)
                }
            }
            launch {
                commentViewModel.loadMoreComment()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val launchArgs = VideoLaunchArgs.fromIntent(intent)
        logger.fInfo { "Handle new video player intent: isLive=${launchArgs.isLive}" }
        if (playerViewModel.videoPlayer == null) {
            initVideoPlayer(launchArgs = launchArgs)
            if (!launchArgs.isLive) initDanmakuPlayer()
            return
        }

        if (playerViewModel.isLive != launchArgs.isLive) {
            releasePlayerForRelaunch()
            initVideoPlayer(launchArgs = launchArgs)
            if (!launchArgs.isLive) initDanmakuPlayer()
            return
        }

        if (!launchArgs.isLive) initDanmakuPlayer()
        parseIntent(launchArgs)
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
        // WEB is also the fallback for APP playback, so every Bilibili media request
        // must be ready to use a WEB play URL.
        val playbackReferer = getString(R.string.video_player_referer)
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
            mpvHardwareDecodeCodecs = settings.mpvHardwareDecodeCodecs,
            mpvVideoOutput = settings.mpvVideoOutput,
            mpvGpuContext = settings.mpvGpuContext,
            mpvGpuApi = settings.mpvGpuApi,
            mpvCache = settings.mpvCache,
            mpvDemuxerMaxBytes = settings.mpvDemuxerMaxBytes,
            mpvDemuxerMaxBackBytes = settings.mpvDemuxerMaxBackBytes,
            mpvVdQueueEnable = settings.mpvVdQueueEnable,
            superResolutionType = settings.superResolutionType,
            audioOutputDevices = settings.audioOutputDevices,
            isLive = launchArgs.isLive
        )
        logger.fInfo {
            "Mobile video player options: player=${settings.playerType}, " +
                    "superResolution=${options.superResolutionType}, " +
                    "enableHardwareDecode=${options.enableHardwareDecode}, " +
                    "hardwareDecodeMode=${options.hardwareDecodeMode}, " +
                    "mpvVideoOutput=${options.mpvVideoOutput}, mpvGpuContext=${options.mpvGpuContext}"
        }
        val videoPlayer = when (settings.playerType) {
            PlayerType.Media3 -> ExoPlayerFactory().create(this, options)
            PlayerType.VLC -> VlcPlayerFactory().create(this, options)
            PlayerType.MPV -> MpvPlayerFactory().create(this, options)
        }
        playerViewModel.videoPlayer = videoPlayer
        //TODO 还没处理旋转后的一些判断，就先放这了
        parseIntent(launchArgs)
    }

    private fun releasePlayerForRelaunch() {
        playerViewModel.videoPlayer?.release()
        playerViewModel.videoPlayer = null
        playerViewModel.danmakuPlayer?.release()
        playerViewModel.danmakuPlayer = null
        playerViewModel.liveDanmakuPlayer?.release()
        playerViewModel.liveDanmakuPlayer = null
    }

    private fun initDanmakuPlayer() {
        if (playerViewModel.danmakuPlayer != null) return
        logger.fInfo { "initDanmakuPlayer" }
        playerViewModel.danmakuPlayer = DanmakuPlayer(SimpleRenderer())
    }

    @MainThread
    private fun parseIntent(launchArgs: VideoLaunchArgs = VideoLaunchArgs.fromIntent(intent)) {
        val settings = PlayerSettingsProvider.current
        if (launchArgs.isLive) {
            vodLaunchJob?.cancel()
            pgcEpisodeRefreshJob?.cancel()
            seasonViewModel.clearSeasonData()
            logger.fInfo { "Launch live parameter: [roomId=${launchArgs.liveRoomId}, watchedNum=${launchArgs.liveWatchedNum}]" }
            playerViewModel.apply {
                fromSeason = false
                epid = 0
                seasonId = 0
                subType = 0
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

        playerViewModel.isLive = false
        playerViewModel.cover = launchArgs.cover
        playerViewModel.title = launchArgs.title
        playerViewModel.partTitle = launchArgs.partTitle
        playerViewModel.upName = launchArgs.upName
        playerViewModel.upFace = launchArgs.upFace
        playerViewModel.upId = launchArgs.upMid
        playerViewModel.play = launchArgs.play
        playerViewModel.danmaku = launchArgs.danmaku
        playerViewModel.pubTime = launchArgs.pubTime

        var aid = launchArgs.aid
        var cid = launchArgs.cid
        val fromSeason = launchArgs.fromSeason
        val epid = launchArgs.epid
        val seasonId = launchArgs.seasonId

        vodLaunchJob?.cancel()
        pgcEpisodeRefreshJob?.cancel()
        seasonViewModel.clearSeasonData()
        playerViewModel.fromSeason = false
        playerViewModel.epid = 0
        playerViewModel.seasonId = 0
        playerViewModel.subType = 0

        // Coordinate player and Compose state on Main. Only blocking network/file work is
        // dispatched to IO below, so state creation and mutation cannot race initial composition.
        vodLaunchJob = lifecycleScope.launch {
            if (aid == 0L && cid == 0L) {
                runCatching {
                    val acid = withContext(Dispatchers.IO) {
                        BiliHttpApi.getAidCidByEpid(epid ?: 0)!!
                    }
                    aid = acid.first
                    cid = acid.second
                }.onFailure {
                    if (it is CancellationException) throw it
                    logger.fInfo { "get avid & cid by epid failed: ${it.stackTraceToString()}" }
                    it.message?.toast(this@VideoPlayerActivity)
                }
            }

            commentViewModel.setCommentTarget(
                commentId = aid,
                commentType = 1
            )

            val offlineEntry = if (launchArgs.playOfflineCache && aid > 0L && cid > 0L) {
                withContext(Dispatchers.IO) {
                    playerViewModel.completedOfflineCacheEntry(aid, cid)
                }
            } else {
                null
            }

            suspend fun applyOfflineDetailFallback() {
                val entry = offlineEntry ?: return
                val entries = withContext(Dispatchers.IO) {
                    playerViewModel.completedOfflineCacheEntries(entry.aid)
                }
                videoDetailViewModel.applyOfflineCacheFallback(
                    entry = entry,
                    entries = entries
                )
            }

            applyOfflineDetailFallback()
            videoDetailViewModel.setInToView(launchArgs.fromToView)

            val useOfflineOnly = offlineEntry != null
            if (useOfflineOnly) {
                logger.fInfo { "Use offline detail without online detail request: [avid=$aid, cid=$cid]" }
            } else {
                runCatching {
                    videoDetailViewModel.loadDetail(aid, fromSeason)
                }.onFailure {
                    if (it is CancellationException) throw it
                    it.message?.toast(this@VideoPlayerActivity)
                }
            }
            runCatching {
                val target = if (useOfflineOnly) {
                    resolveUgcPlaybackTarget(
                        aid = aid,
                        cid = cid,
                        settings = settings
                    )
                } else if (fromSeason) {
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

                commentViewModel.setCommentTarget(
                    commentId = target.aid,
                    commentType = 1
                )
                if (!useOfflineOnly && target.aid > 0L && target.aid != aid) {
                    runCatching {
                        videoDetailViewModel.loadDetail(target.aid, fromSeason)
                    }.onFailure {
                        if (it is CancellationException) throw it
                        it.message?.toast(this@VideoPlayerActivity)
                    }
                }

                playerViewModel.fromSeason = fromSeason && !useOfflineOnly
                playerViewModel.lastPlayed = target.playedMs
                playerViewModel.subType = target.subType
                playerViewModel.epid = target.epid ?: 0
                playerViewModel.seasonId = target.seasonId ?: 0
                if (fromSeason && !useOfflineOnly) {
                    seasonViewModel.epId = target.epid
                    seasonViewModel.seasonId = target.seasonId
                }
                playerViewModel.loadPlayUrl(
                    avid = target.aid,
                    cid = target.cid,
                    epid = target.epid,
                    seasonId = target.seasonId,
                    forceStartPlayback = launchArgs.playOfflineCache,
                    preferOfflineCache = launchArgs.playOfflineCache
                )
            }.onFailure {
                if (it is CancellationException) throw it
                it.message?.toast(this@VideoPlayerActivity)
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
            val targetEpisodeId = targetEpisode.epid ?: targetEpisode.id.takeIf { it > 0 }
            applySeasonMetadata(season, targetEpisode)
            InitialVodPlaybackTarget(
                aid = targetEpisode.aid,
                cid = targetEpisode.cid,
                epid = targetEpisodeId,
                seasonId = season.seasonId,
                subType = season.subType,
                playedMs = if (
                    settings.playerDefaultStartPosition == PlayerDefaultStartPosition.History &&
                    historyEpisode != null &&
                    (historyEpisode.epid ?: historyEpisode.id) == targetEpisodeId
                ) {
                    history.lastTime.coerceAtLeast(0) * 1000
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
        val formattedPubTime = detail.publishDate
            .takeIf { it.time > 0L }
            ?.formatPubTimeString(this@VideoPlayerActivity)
        playerViewModel.apply {
            title = detail.title.ifBlank { title }
            partTitle = page?.title?.ifBlank { partTitle } ?: partTitle
            cover = detail.cover.ifBlank { cover }
            playerIconIdle = detail.playerIcon?.idle ?: ""
            playerIconMoving = detail.playerIcon?.moving ?: ""
            play = detail.stat.view.takeIf { it > 0L } ?: play
            danmaku = detail.stat.danmaku.takeIf { it > 0 } ?: danmaku
            like = detail.stat.like
            coin = detail.stat.coin
            favorite = detail.stat.favorite
            upName = detail.author.name.ifBlank { upName }
            upId = detail.author.mid.takeIf { it > 0L } ?: upId
            upFace = detail.author.face.ifBlank { upFace }
            pubTime = formattedPubTime ?: pubTime
            isVerticalVideo = page?.dimension?.isVertical ?: false
        }
    }

    private fun applySeasonMetadata(season: SeasonDetail, episode: Episode) {
        playerViewModel.apply {
            title = season.title
            partTitle = episode.longTitle.ifBlank { episode.title }
            cover = episode.cover.ifBlank { season.cover }
            playerIconIdle = season.playerIcon?.idle ?: ""
            playerIconMoving = season.playerIcon?.moving ?: ""
            play = episode.viewCount
            danmaku = episode.danmakuCount
            isVerticalVideo = episode.dimension?.isVertical ?: false
        }
    }

    private fun SeasonDetail.findEpisodeByEpId(epId: Int?): Episode? {
        if (epId == null) return null
        return allEpisodes().firstOrNull {
            it.epid == epId || (it.epid == null && it.id == epId)
        }
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
        if (pipModeActive || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode)) {
            return
        }
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
