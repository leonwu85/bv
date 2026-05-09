package dev.aaa1115910.bv.tv.activities.video

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import java.lang.ref.WeakReference
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.http.BiliPlusHttpApi
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.PlayerType
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.player.entity.PlaybackMediaMode
import dev.aaa1115910.bv.player.entity.RequestState
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.player.impl.exo.ExoPlayerFactory
import dev.aaa1115910.bv.player.impl.vlc.VlcPlayerFactory
import dev.aaa1115910.bv.tv.screens.VideoPlayerV3Screen
import dev.aaa1115910.bv.tv.manager.VideoUserActionManager
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.formatPubTimeString
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.viewmodel.VideoPlayerV3ViewModel
import dev.aaa1115910.bv.viewmodel.video.VideoDetailViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.androidx.viewmodel.ext.android.viewModel

class VideoPlayerV3Activity : ComponentActivity() {
    companion object {
        private const val EXTRA_DIRECT_OPEN = "direct_open"
        private val logger = KotlinLogging.logger { }
        private var currentInstance: WeakReference<VideoPlayerV3Activity>? = null

        private fun formatPopularity(count: Int): String {
            return when {
                count >= 100_000_000 -> String.format("%.1f亿人气", count / 100_000_000.0)
                count >= 10_000 -> String.format("%.1f万人气", count / 10_000.0)
                else -> "${count}人气"
            }
        }
        
        /**
         * 启动直播播放
         */
        fun actionStartLive(
            context: Context,
            roomId: Int,
            title: String,
            upName: String = "",
            upFace: String = "",
            upMid: Long = 0L,
            watchedNum: Int = 0
        ) {
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            logger.info { "Current memory usage VideoPlayerV3Activity.actionStartLive: ${usedMemory / 1024 / 1024} MB / ${maxMemory / 1024 / 1024} MB" }

            // 先关闭旧的播放页面
            currentInstance?.get()?.let { instance ->
                logger.info { "Closing previous video player instance" }
                instance.finish()
            }
            currentInstance = null
            
            context.startActivity(
                Intent(
                    context,
                    VideoPlayerV3Activity::class.java
                ).apply {
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
        
        fun actionStart(
            context: Context,
            avid: Long,
            cid: Long,
            title: String,
            partTitle: String,
            played: Int,
            fromSeason: Boolean,
            subType: Int? = null,
            epid: Int? = null,
            seasonId: Int? = null,
            isVerticalVideo: Boolean = false,
            proxyArea: ProxyArea = ProxyArea.MainLand,
            playerIconIdle: String = "",
            playerIconMoving: String = "",
            play: Long = 0,
            danmaku: Int = 0,
            like: Int = 0,
            coin: Int = 0,
            favorite: Int = 0,
            upName: String = "",
            upId: Long = 0L,
            upFace: String = "",
            pubTime: String = "",
            audioOnlyMode: Boolean = false
        ) {
            // 获取当前内存信息并打印到控制台
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            logger.info { "Current memory usage VideoPlayerV3Activity.actionStart: ${usedMemory / 1024 / 1024} MB / ${maxMemory / 1024 / 1024} MB" }

            // 先关闭旧的播放页面
            currentInstance?.get()?.let { instance ->
                logger.info { "Closing previous video player instance" }
                instance.finish()
            }
            currentInstance = null
            
            context.startActivity(
                Intent(
                    context,
                    dev.aaa1115910.bv.tv.activities.video.VideoPlayerV3Activity::class.java
                ).apply {
                    putExtra("avid", avid)
                    putExtra("cid", cid)
                    putExtra("title", title)
                    putExtra("partTitle", partTitle)
                    putExtra("played", played)
                    putExtra("fromSeason", fromSeason)
                    putExtra("subType", subType)
                    putExtra("epid", epid)
                    putExtra("seasonId", seasonId)
                    putExtra("isVerticalVideo", isVerticalVideo)
                    putExtra("proxy_area", proxyArea.ordinal)
                    putExtra("playerIconIdle", playerIconIdle)
                    putExtra("playerIconMoving", playerIconMoving)
                    putExtra("play", play)
                    putExtra("danmaku", danmaku)
                    putExtra("like", like)
                    putExtra("coin", coin)
                    putExtra("favorite", favorite)
                    putExtra("upName", upName)
                    putExtra("upId", upId)
                    putExtra("upFace", upFace)
                    putExtra("pubTime", pubTime)
                    putExtra("audioOnlyMode", audioOnlyMode)
                }
            )
        }

        fun actionStartDirect(
            context: Context,
            avid: Long,
            cid: Long? = null,
            proxyArea: ProxyArea = ProxyArea.MainLand,
            audioOnlyMode: Boolean = false
        ) {
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            logger.info { "Current memory usage VideoPlayerV3Activity.actionStartDirect: ${usedMemory / 1024 / 1024} MB / ${maxMemory / 1024 / 1024} MB" }

            currentInstance?.get()?.let { instance ->
                logger.info { "Closing previous video player instance" }
                instance.finish()
            }
            currentInstance = null

            context.startActivity(
                Intent(
                    context,
                    dev.aaa1115910.bv.tv.activities.video.VideoPlayerV3Activity::class.java
                ).apply {
                    putExtra(EXTRA_DIRECT_OPEN, true)
                    putExtra("avid", avid)
                    cid?.takeIf { it > 0L }?.let { putExtra("cid", it) }
                    putExtra("proxy_area", proxyArea.ordinal)
                    putExtra("audioOnlyMode", audioOnlyMode)
                }
            )
        }
    }

    private val playerViewModel: VideoPlayerV3ViewModel by viewModel()
    private val videoDetailViewModel: VideoDetailViewModel by viewModel()

    private fun resetSessionPlaySpeedToDefault() {
        val defaultPlaySpeed = Prefs.defaultPlaySpeed
        Prefs.currentPlaySpeed = defaultPlaySpeed
        playerViewModel.currentPlaySpeed = defaultPlaySpeed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置当前实例为弱引用
        currentInstance = WeakReference(this)

        resetSessionPlaySpeedToDefault()

        val isLive = intent.getBooleanExtra("isLive", false)
        initVideoPlayer(isLive = isLive)
        //initDanmakuPlayer()
        getParamsFromIntent()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            BVTheme(
                forceDark = true
            ) {
                VideoPlayerV3Screen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 清除当前实例引用
        if (currentInstance?.get() == this) {
            currentInstance = null
        }

        // 获取当前内存信息并打印到控制台
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        logger.info { "Current memory usage VideoPlayerV3Activity.onDestroy: ${usedMemory / 1024 / 1024} MB / ${maxMemory / 1024 / 1024} MB" }
    }

    override fun onPause() {
        super.onPause()
        playerViewModel.videoPlayer?.pause()
        playerViewModel.danmakuPlayer?.pause()
        
        // 暂停直播弹幕
        if (playerViewModel.isLive) {
            playerViewModel.stopLiveDanmaku()
        }
    }

    private fun initVideoPlayer(isLive: Boolean = false) {
        dev.aaa1115910.bv.tv.activities.video.VideoPlayerV3Activity.Companion.logger.info { "Init video player: ${Prefs.playerType.name}, isLive=$isLive" }
        val options = VideoPlayerOptions(
            userAgent = when (Prefs.apiType) {
                ApiType.Web -> dev.aaa1115910.biliapi.BiliApiConstants.USER_AGENT_WEB
                ApiType.App -> dev.aaa1115910.biliapi.BiliApiConstants.USER_AGENT_APP
            },
            referer = when (Prefs.apiType) {
                ApiType.Web -> getString(R.string.video_player_referer)
                ApiType.App -> null
            },
            enableFfmpegAudioRenderer = Prefs.enableFfmpegAudioRenderer,
            enableAsyncQueueing = Prefs.enableAsyncQueueing,
            enableTunneling = Prefs.enableTunneling,
            enableAudioPlaybackParams = Prefs.enableAudioPlaybackParams,
            enableVideoFrameRateStrategy = false,
            isLive = isLive
        )
        val videoPlayer = when (Prefs.playerType) {
            PlayerType.Media3 -> ExoPlayerFactory().create(this, options)
            PlayerType.VLC -> VlcPlayerFactory().create(this, options)
        }
        playerViewModel.videoPlayer = videoPlayer
    }

    private fun startVodPlayback(
        aid: Long,
        cid: Long,
        title: String,
        partTitle: String,
        played: Int,
        fromSeason: Boolean,
        subType: Int? = null,
        epid: Int? = null,
        seasonId: Int? = null,
        isVerticalVideo: Boolean = false,
        proxyArea: ProxyArea = ProxyArea.MainLand,
        playerIconIdle: String = "",
        playerIconMoving: String = "",
        play: Long = 0,
        danmaku: Int = 0,
        like: Int = 0,
        coin: Int = 0,
        favorite: Int = 0,
        upName: String = "",
        upId: Long = 0L,
        upFace: String = "",
        pubTime: String = "",
        audioOnlyMode: Boolean = false
    ) {
        playerViewModel.apply {
            lastPlayed = played
            currentPlaybackMediaMode = if (audioOnlyMode) PlaybackMediaMode.AudioOnly else PlaybackMediaMode.Normal
            this.title = title
            this.partTitle = partTitle
            this.fromSeason = fromSeason
            this.subType = subType ?: 0
            this.epid = epid ?: 0
            this.seasonId = seasonId ?: 0
            this.isVerticalVideo = isVerticalVideo
            this.proxyArea = proxyArea
            this.playerIconIdle = playerIconIdle
            this.playerIconMoving = playerIconMoving
            this.play = play
            this.danmaku = danmaku
            this.like = like
            this.coin = coin
            this.favorite = favorite
            this.upName = upName
            this.upId = upId
            this.upFace = upFace
            this.pubTime = pubTime
            loadPlayUrl(
                avid = aid,
                cid = cid,
                epid = epid,
                seasonId = seasonId
            )
        }
    }

    private fun launchDirectOpenPlayback(
        aid: Long,
        requestedCid: Long?,
        proxyArea: ProxyArea,
        audioOnlyMode: Boolean
    ) {
        playerViewModel.loadState = RequestState.Doing
        playerViewModel.errorMessage = ""
        playerViewModel.currentPlaybackMediaMode = if (audioOnlyMode) PlaybackMediaMode.AudioOnly else PlaybackMediaMode.Normal
        playerViewModel.proxyArea = proxyArea

        lifecycleScope.launch {
            runCatching {
                if (proxyArea != ProxyArea.MainLand) {
                    val redirectSeasonId = withContext(Dispatchers.IO) {
                        runCatching { BiliPlusHttpApi.getSeasonIdByAvid(aid) }.getOrNull()
                    }
                    if (redirectSeasonId != null) {
                        SeasonInfoActivity.actionStart(
                            context = this@VideoPlayerV3Activity,
                            seasonId = redirectSeasonId,
                            proxyArea = proxyArea
                        )
                        finish()
                        return@launch
                    }
                }

                withContext(Dispatchers.IO) {
                    videoDetailViewModel.loadDetail(aid)
                }
                val detail = videoDetailViewModel.videoDetail
                    ?: error("视频详情为空")

                VideoUserActionManager.updateFromLoadedData(
                    aid = detail.aid,
                    liked = detail.userActions.like,
                    favorited = detail.userActions.favorite,
                    coin = detail.userActions.coin
                )
                if (Prefs.isLogin) {
                    VideoUserActionManager.fetchFavoriteData(detail.aid, Prefs.uid)
                }

                if (detail.redirectToEp) {
                    SeasonInfoActivity.actionStart(
                        context = this@VideoPlayerV3Activity,
                        epId = detail.epid,
                        proxyArea = proxyArea
                    )
                    finish()
                    return@launch
                }

                val playPage = detail.pages.firstOrNull() ?: error("视频分P为空")
                val cid = requestedCid?.takeIf { targetCid ->
                    detail.pages.any { it.cid == targetCid }
                } ?: playPage.cid
                val currentPage = detail.pages.find { it.cid == cid } ?: playPage

                val ugcSeason = detail.ugcSeason
                if (ugcSeason != null) {
                    val sectionIndex = ugcSeason.sections.indexOfFirst { section ->
                        section.episodes.any { episode ->
                            episode.cid == cid || episode.pages.any { page -> page.cid == cid }
                        }
                    }
                    if (sectionIndex >= 0) {
                        videoDetailViewModel.updateUgcSeasonSectionVideoList(sectionIndex)
                    }
                }

                startVodPlayback(
                    aid = detail.aid,
                    cid = cid,
                    title = detail.title,
                    partTitle = currentPage.title,
                    played = if (detail.history.lastPlayedCid == cid) detail.history.progress * 1000 else 0,
                    fromSeason = false,
                    isVerticalVideo = currentPage.dimension.isVertical,
                    proxyArea = proxyArea,
                    playerIconIdle = detail.playerIcon?.idle ?: "",
                    playerIconMoving = detail.playerIcon?.moving ?: "",
                    play = detail.stat.view,
                    danmaku = detail.stat.danmaku,
                    like = detail.stat.like,
                    coin = detail.stat.coin,
                    favorite = detail.stat.favorite,
                    upName = detail.author.name,
                    upId = detail.author.mid,
                    upFace = detail.author.face,
                    pubTime = detail.publishDate.formatPubTimeString(),
                    audioOnlyMode = audioOnlyMode
                )
            }.onFailure {
                logger.warn(it) { "Direct open playback failed" }
                playerViewModel.errorMessage = it.localizedMessage ?: "未知错误"
                playerViewModel.loadState = RequestState.Failed
            }
        }
    }

    /*private fun initDanmakuPlayer() {
        logger.info { "Init danamku player" }
        runBlocking { playerViewModel.initDanmakuPlayer() }
    }*/

    private fun getParamsFromIntent() {
        // 检查是否为直播模式
        if (intent.getBooleanExtra("isLive", false)) {
            val roomId = intent.getIntExtra("liveRoomId", 0)
            val title = intent.getStringExtra("title") ?: "Unknown Title"
            val upName = intent.getStringExtra("upName") ?: ""
            val upFace = intent.getStringExtra("upFace") ?: ""
            val upMid = intent.getLongExtra("upMid", 0)
            val watchedNum = intent.getIntExtra("liveWatchedNum", 0)
            
            logger.fInfo { "Launch live parameter: [roomId=$roomId, watchedNum=$watchedNum]" }
            
            playerViewModel.apply {
                this.title = title
                this.upName = upName
                this.upFace = upFace
                this.upId = upMid
                this.isLive = true
                this.liveRoomId = roomId
                this.livePopularityText = if (watchedNum > 0) formatPopularity(watchedNum) else ""
                
                // 通过 ViewModel 加载直播流（带画质选择，加载成功后自动启动弹幕）
                loadLiveStreamWithQuality(roomId, Prefs.defaultLiveQn)
            }
            return
        }

        if (intent.getBooleanExtra(EXTRA_DIRECT_OPEN, false)) {
            launchDirectOpenPlayback(
                aid = intent.getLongExtra("avid", 0),
                requestedCid = intent.getLongExtra("cid", 0).takeIf { it > 0L },
                proxyArea = ProxyArea.entries[intent.getIntExtra("proxy_area", 0)],
                audioOnlyMode = intent.getBooleanExtra("audioOnlyMode", false)
            )
            return
        }
        
        if (intent.hasExtra("avid")) {
            val aid = intent.getLongExtra("avid", 170001)
            val cid = intent.getLongExtra("cid", 170001)
            val title = intent.getStringExtra("title") ?: "Unknown Title"
            val partTitle = intent.getStringExtra("partTitle") ?: "Unknown Part Title"
            val played = intent.getIntExtra("played", 0)
            val fromSeason = intent.getBooleanExtra("fromSeason", false)
            val subType = intent.getIntExtra("subType", 0)
            val epid = intent.getIntExtra("epid", 0)
            val seasonId = intent.getIntExtra("seasonId", 0)
            val isVerticalVideo = intent.getBooleanExtra("isVerticalVideo", false)
            val proxyArea = ProxyArea.entries[intent.getIntExtra("proxy_area", 0)]
            val playerIconIdle = intent.getStringExtra("playerIconIdle") ?: ""
            val playerIconMoving = intent.getStringExtra("playerIconMoving") ?: ""
            val play = intent.getLongExtra("play", 0)
            val danmaku = intent.getIntExtra("danmaku", 0)
            val like = intent.getIntExtra("like", 0)
            val coin = intent.getIntExtra("coin", 0)
            val favorite = intent.getIntExtra("favorite", 0)
            val upName = intent.getStringExtra("upName") ?: ""
            val upId = intent.getLongExtra("upId", 0)
            val upFace = intent.getStringExtra("upFace") ?: ""
            val pubTime = intent.getStringExtra("pubTime") ?: ""
            val audioOnlyMode = intent.getBooleanExtra("audioOnlyMode", false)
            dev.aaa1115910.bv.tv.activities.video.VideoPlayerV3Activity.Companion.logger.fInfo { "Launch parameter: [aid=$aid, cid=$cid]" }
            startVodPlayback(
                aid = aid,
                cid = cid,
                title = title,
                partTitle = partTitle,
                played = played,
                fromSeason = fromSeason,
                subType = subType.takeIf { it != 0 },
                epid = epid.takeIf { it != 0 },
                seasonId = seasonId.takeIf { it != 0 },
                isVerticalVideo = isVerticalVideo,
                proxyArea = proxyArea,
                playerIconIdle = playerIconIdle,
                playerIconMoving = playerIconMoving,
                play = play,
                danmaku = danmaku,
                like = like,
                coin = coin,
                favorite = favorite,
                upName = upName,
                upId = upId,
                upFace = upFace,
                pubTime = pubTime,
                audioOnlyMode = audioOnlyMode
            )
        } else {
            dev.aaa1115910.bv.tv.activities.video.VideoPlayerV3Activity.Companion.logger.fInfo { "Null launch parameter" }
        }
    }
}
