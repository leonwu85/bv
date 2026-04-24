package dev.aaa1115910.bv.viewmodel

import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ext.RETAINER_BILIBILI
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import com.kuaishou.akdanmaku.ui.LiveDanmakuPlayer
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.PlayData
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMaskSegment
import dev.aaa1115910.biliapi.http.entity.video.ClipInfo
import dev.aaa1115910.biliapi.entity.video.HeartbeatVideoType
import dev.aaa1115910.biliapi.entity.video.InteractiveNode
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.entity.video.SubtitleAiStatus
import dev.aaa1115910.biliapi.entity.video.SubtitleAiType
import dev.aaa1115910.biliapi.entity.video.SubtitleType
import dev.aaa1115910.biliapi.entity.video.VideoShot
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.BiliLiveHttpApi
import dev.aaa1115910.biliapi.http.entity.live.DanmakuEvent
import dev.aaa1115910.biliapi.http.entity.live.LiveEvent
import dev.aaa1115910.biliapi.http.entity.live.OnlineRankCountEvent
import dev.aaa1115910.biliapi.http.entity.live.PopularityChangeEvent
import dev.aaa1115910.biliapi.entity.sponsorblock.SponsorSegment
import dev.aaa1115910.biliapi.http.SponsorBlockHttpApi
import dev.aaa1115910.biliapi.repositories.LiveRepository
import dev.aaa1115910.biliapi.repositories.VideoDetailRepository
import dev.aaa1115910.biliapi.repositories.VideoPlayRepository
import dev.aaa1115910.biliapi.util.AvBvConverter
import dev.aaa1115910.biliapi.websocket.LiveDataWebSocket
import dev.aaa1115910.bilisubtitle.SubtitleParser
import dev.aaa1115910.bilisubtitle.entity.SubtitleItem
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.entity.LiveQualityPreference
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.player.autoplay.AutoPlayCandidate
import dev.aaa1115910.bv.player.autoplay.PreparedAutoPlayTarget
import dev.aaa1115910.bv.player.autoplay.PreparedAutoPlayTransitionContext
import dev.aaa1115910.bv.player.autoplay.toPreparedAutoPlayTransitionContext
import dev.aaa1115910.bv.player.renderer.OptimizedTextRenderer
import dev.aaa1115910.bv.player.renderer.SimpleRenderer
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.DanmakuType
import dev.aaa1115910.bv.player.entity.PlaybackMediaMode
import dev.aaa1115910.bv.player.entity.PlayMode
import dev.aaa1115910.bv.player.entity.PlayerDefaultStartPosition
import dev.aaa1115910.bv.player.entity.PortraitVideoFixMode
import dev.aaa1115910.bv.player.entity.RequestState
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoAspectRatio
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.player.entity.LiveCodec
import dev.aaa1115910.bv.player.entity.VideoListInteractiveNode
import dev.aaa1115910.bv.player.entity.VideoListItemData
import dev.aaa1115910.bv.player.entity.VideoRotation
import dev.aaa1115910.bv.player.entity.SponsorBlockSkipMode
import dev.aaa1115910.bv.repository.VideoInfoRepository
import dev.aaa1115910.bv.util.DanmakuSegmentMergeResult
import dev.aaa1115910.bv.util.MergedDanmakuEntry
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.VodDanmakuMergeState
import dev.aaa1115910.bv.util.VodDanmakuMerger
import dev.aaa1115910.bv.util.fError
import dev.aaa1115910.bv.util.fException
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.fWarn
import dev.aaa1115910.bv.util.LiveStreamUrlFetcher
import dev.aaa1115910.bv.util.fDebug
import dev.aaa1115910.bv.util.swapList
import dev.aaa1115910.bv.util.swapListWithMainContext
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.KoinViewModel
import java.net.URI

@KoinViewModel
class VideoPlayerV3ViewModel(
    private val videoInfoRepository: VideoInfoRepository,
    private val videoPlayRepository: VideoPlayRepository,
    private val videoDetailRepository: VideoDetailRepository,
    private val liveRepository: LiveRepository,
) : ViewModel() {
    private val logger = KotlinLogging.logger { }

    private enum class SubtitleSlot(val logPrefix: String, val debugName: String) {
        Primary("", "primary"),
        Secondary("副", "secondary")
    }

    var videoPlayer: AbstractVideoPlayer? by mutableStateOf(null)
    var danmakuPlayer: DanmakuPlayer? by mutableStateOf(null)
    var liveDanmakuPlayer: LiveDanmakuPlayer? by mutableStateOf(null)
    var show by mutableStateOf(false)
    
    override fun onCleared() {
        super.onCleared()
        logger.fInfo { "VideoPlayerV3ViewModel onCleared" }

        invalidatePreparedAutoPlayTarget()
        cancelVodPlayUrlAutoRefresh()

        // 清理直播重连任务
        liveRetryJob?.cancel()
        liveRetryJob = null

        // 清理直播URL刷新任务
        liveUrlRefreshJob?.cancel()
        liveUrlRefreshJob = null

        liveRoomEntryReportJob?.cancel()
        liveRoomEntryReportJob = null

        // 清理直播弹幕资源
        stopLiveDanmaku()
        
        try {
            videoPlayer?.release()
            videoPlayer = null
        } catch (e: Exception) {
            logger.fError { "Error releasing video player: ${e.message}" }
        }

        try {
            danmakuPlayer?.release()
            danmakuPlayer = null
            liveDanmakuPlayer?.release()
            liveDanmakuPlayer = null
            danmakuData.clear()
            danmakuMasks.clear()
        } catch (e: Exception) {
            logger.fError { "Error releasing danmaku player: ${e.message}" }
        }

        // 清除可能未被GC回收的资源
        currentSubtitleData.clear()
        currentSecondarySubtitleData.clear()
        clearDanmakuSliceState()
    }

    var loadState by mutableStateOf(RequestState.Ready)
    var errorMessage by mutableStateOf("")

    private var playData: PlayData? by mutableStateOf(null)
    val danmakuData: MutableList<DanmakuItemData> = ArrayList()
    val danmakuMasks = mutableStateListOf<DanmakuMaskSegment>()
    var videoShot: VideoShot? by mutableStateOf(null)
    var clipInfoList: List<ClipInfo> by mutableStateOf(emptyList())

    var availableQuality = mutableStateListOf<Resolution>()
    var availableVideoCodec = mutableStateListOf<VideoCodec>()
    var availableSubtitle = mutableStateListOf<Subtitle>()
    var availableAudio = mutableStateListOf<Audio>()
    val availableVideoList get() = videoInfoRepository.videoList
    val relatedVideos get() =  videoInfoRepository.relatedVideos
    val isInteractivePlayback get() = videoInfoRepository.interactivePlaybackContext != null
    val interactiveOptions get() = availableVideoList.filterIsInstance<VideoListInteractiveNode>()
    var showInteractiveOptionDialog by mutableStateOf(false)
    var interactiveOptionsLoading by mutableStateOf(false)
    var interactiveOptionsFromQuestions by mutableStateOf(false)
    private var pendingInteractiveOptionDialogRequest by mutableStateOf(false)

    var currentVideoHeight by mutableIntStateOf(0)
    var currentVideoWidth by mutableIntStateOf(0)

    var currentQuality by mutableStateOf(Prefs.defaultQuality)
    var currentVideoCodec by mutableStateOf(Prefs.defaultVideoCodec)
    var currentPlaySpeed by mutableFloatStateOf(Prefs.currentPlaySpeed)
    var currentVideoAspectRatio by mutableStateOf(VideoAspectRatio.Default)
    var currentVideoRotation by mutableStateOf(VideoRotation.Original)
    var currentPlaybackMediaMode by mutableStateOf(PlaybackMediaMode.Normal)
    var currentAudio by mutableStateOf(Prefs.defaultAudio)
    var currentDanmakuScale by mutableFloatStateOf(Prefs.defaultDanmakuScale)
    var currentDanmakuOpacity by mutableFloatStateOf(Prefs.defaultDanmakuOpacity)
    var currentDanmakuEnabled by mutableStateOf(Prefs.defaultDanmakuEnabled)
    val currentDanmakuTypes = mutableStateListOf<DanmakuType>().apply {
        addAll(Prefs.defaultDanmakuTypes)
    }
    var currentDanmakuArea by mutableFloatStateOf(Prefs.defaultDanmakuArea)
    private var currentDanmakuMaskState by mutableStateOf(Prefs.defaultDanmakuMask)
    var currentDanmakuMask: Boolean
        get() = currentDanmakuMaskState
        set(value) {
            if (currentDanmakuMaskState == value) return

            currentDanmakuMaskState = value

            if (isLive) return

            if (!value) return

            if (currentAid <= 0 || currentCid <= 0) return
            if (hasResolvedCurrentDanmakuMask()) return

            viewModelScope.launch(Dispatchers.Default) {
                updateDanmakuMask()
            }
        }
    var currentDanmakuFilterLevel by mutableIntStateOf(Prefs.defaultDanmakuFilterLevel)
    var currentDanmakuMergeEnabled by mutableStateOf(Prefs.defaultDanmakuMergeEnabled)
    var currentLiveDanmakuFilterLevel by mutableIntStateOf(Prefs.defaultLiveDanmakuFilterLevel)
    var currentSubtitleId by mutableLongStateOf(-1L)
    var currentSubtitleData = mutableStateListOf<SubtitleItem>()
    var currentSubtitleType by mutableStateOf(SubtitleType.CC)
    var currentSubtitleFontSize by mutableStateOf(Prefs.defaultSubtitleFontSize)
    var currentSubtitleBackgroundOpacity by mutableFloatStateOf(Prefs.defaultSubtitleBackgroundOpacity)
    var currentSubtitleBottomPadding by mutableStateOf(Prefs.defaultSubtitleBottomPadding)
    var currentSecondarySubtitleId by mutableLongStateOf(-1L)
    var currentSecondarySubtitleData = mutableStateListOf<SubtitleItem>()
    var currentSecondarySubtitleType by mutableStateOf(SubtitleType.CC)
    var currentSecondarySubtitleFontSize by mutableStateOf(Prefs.defaultSecondarySubtitleFontSize)
    var currentSecondarySubtitleBackgroundOpacity by mutableFloatStateOf(Prefs.defaultSecondarySubtitleBackgroundOpacity)
    var currentSecondarySubtitleBottomPadding by mutableStateOf(Prefs.defaultSecondarySubtitleBottomPadding)
    private var currentPrimarySubtitleLoadToken = 0L
    private var currentSecondarySubtitleLoadToken = 0L

    var currentPlayMode by mutableStateOf(Prefs.defaultPlayMode)

    var title by mutableStateOf("")
    var partTitle by mutableStateOf("")
    var lastPlayed by mutableIntStateOf(0)
    var fromSeason by mutableStateOf(false)
    var subType by mutableIntStateOf(0)
    var epid by mutableIntStateOf(0)
    var seasonId by mutableIntStateOf(0)
    var isVerticalVideo by mutableStateOf(false)
    var proxyArea by mutableStateOf(ProxyArea.MainLand)
    var play by mutableLongStateOf(0)
    var danmaku by mutableStateOf(0)
    var like by mutableStateOf(0)
    
    // 直播相关属性
    var isLive by mutableStateOf(false)
    var liveRoomId by mutableIntStateOf(0)
    var liveStreamUrl by mutableStateOf("")

    // 直播画质管理
    var availableLiveQualities = mutableStateListOf<Pair<Int, String>>() // qn -> description
    var currentLiveQn by mutableIntStateOf(Prefs.defaultLiveQn)
    var currentLiveQualityDescription by mutableStateOf("")
    private var liveQnDescMap: Map<Int, String> = emptyMap()

    // 直播编码管理
    var currentLiveCodec by mutableStateOf(Prefs.defaultLiveCodec)

    // 直播流URL过期时间（毫秒时间戳）
    var liveStreamExpiresAt by mutableLongStateOf(0L)

    // 直播自动重连
    private var liveRetryJob: Job? = null

    // 直播历史单次上报
    private var liveRoomEntryReportJob: Job? = null
    private var reportedLiveRoomId: Int? = null
    private var reportingLiveRoomId: Int? = null

    // 直播URL主动刷新
    private var liveUrlRefreshJob: Job? = null
    private var consecutiveRefreshFailures = 0

    // 点播播放地址主动刷新
    private var vodPlayUrlRefreshJob: Job? = null
    private var vodPlayUrlRefreshToken by mutableLongStateOf(0L)
    private var vodPlaybackSessionToken by mutableLongStateOf(0L)

    // 自动连播提前准备
    private var preparedAutoPlayTarget by mutableStateOf<PreparedAutoPlayTarget?>(null)
    private var autoPlayPrepareJob: Job? = null
    private var autoPlayPrefetchJob: Job? = null
    private var autoPlayPrepareToken by mutableLongStateOf(0L)

    companion object {
        // 提前刷新的时间（毫秒），默认60秒
        private const val REFRESH_BEFORE_EXPIRY_MS = 60_000L
        // 最小刷新间隔（毫秒），防止频繁刷新
        private const val MIN_REFRESH_INTERVAL_MS = 30_000L
        // 刷新失败后的重试间隔（毫秒）
        private const val REFRESH_RETRY_INTERVAL_MS = 10_000L
        // 最大连续刷新失败次数
        private const val MAX_REFRESH_FAILURES = 3
        private const val DANMAKU_SEGMENT_DURATION_MS = 6 * 60 * 1000L
        private const val INITIAL_DANMAKU_SEGMENT_PREFETCH = 2
        private const val DANMAKU_BATCH_SIZE = 600
        private const val DANMAKU_SLICE_SIZE = 2000
        private const val DANMAKU_SLICE_EMIT_DELAY_MS = 12L
        private const val AUTO_PLAY_PREPARE_WAIT_MS = 1_200L
        private const val PREPARED_AUTO_PLAY_PLAY_DATA_TTL_MS = 2 * 60_000L
    }

    // 直播人气值与高能观众
    var livePopularityText by mutableStateOf("")   // "2.5万人气" (POPULARITY_CHANGE)
    var liveOnlineCount by mutableStateOf("")      // "4333 高能观众" (ONLINE_RANK_COUNT)

    // 人气和高能观众更新频率限制（至少间隔 10 秒）
    private var lastPopularityUpdateTime = 0L
    private var lastOnlineCountUpdateTime = 0L

    // 直播弹幕管理
    private var liveWebSocket: Job? = null
    private var liveWebSocketInner: Job? = null
    private var liveDanmakuConsumer: Job? = null
    private var liveDanmakuChannel: Channel<DanmakuEvent>? = null
    
    var coin by mutableStateOf(0)
    var favorite by mutableStateOf(0)
    var upName by mutableStateOf("")
    var upFace by mutableStateOf("")
    var pubTime by mutableStateOf("")
    var upId by mutableLongStateOf(0L)
    var isLoop by mutableStateOf(Prefs.isLoop)
    var showDanmaku by mutableStateOf(Prefs.showDanmaku)
    var showRelatedVideos by mutableStateOf(false)
    var isFollowingUp by mutableStateOf(false)

    var needPay by mutableStateOf(false)

    var logs by mutableStateOf("")
    var lastChangedLog by mutableLongStateOf(System.currentTimeMillis())
    var showBuffering by mutableStateOf(false)

    var playerIconIdle by mutableStateOf("")
    var playerIconMoving by mutableStateOf("")

    var currentAid = 0L
    var currentCid by mutableLongStateOf(0L)
    var currentInteractiveNodeId by mutableLongStateOf(0L)
    var currentInteractiveEdgeId by mutableLongStateOf(0L)
    private var currentEpid = 0
    private var resolvedDanmakuMaskAid = 0L
    private var resolvedDanmakuMaskCid by mutableLongStateOf(0L)
    private var hasResolvedDanmakuMask by mutableStateOf(false)

    // SponsorBlock 相关状态
    var enableSponsorBlock by mutableStateOf(Prefs.enableSponsorBlock)
    var sponsorBlockSkipMode by mutableStateOf(Prefs.sponsorBlockSkipMode)
    var sponsorSegments by mutableStateOf<List<SponsorSegment>>(emptyList())
    var showSponsorBlockTip by mutableStateOf(false)
    var currentSponsorSegment by mutableStateOf<SponsorSegment?>(null)

    private var danmakuLoadJob: Job? = null
    private var danmakuCatchUpJob: Job? = null
    private var danmakuLoadToken by mutableLongStateOf(0L)
    private var lastDanmakuCatchUpPositionMs by mutableLongStateOf(-1L)
    private var lastDanmakuCatchUpSegment by mutableIntStateOf(-1)
    private var danmakuLoadSession: DanmakuLoadSession? = null

    private data class DanmakuLoadSession(
        val token: Long,
        val loadedDanmakuIds: LinkedHashSet<Long> = LinkedHashSet(),
        val danmakuSlicesBySegment: LinkedHashMap<Int, List<DanmakuSlice>> = LinkedHashMap(),
        val nextDanmakuSliceIndexBySegment: LinkedHashMap<Int, Int> = LinkedHashMap(),
        val vodDanmakuMergeState: VodDanmakuMergeState = VodDanmakuMergeState()
    )

    private data class DanmakuSlice(
        val segmentIndex: Int,
        val sliceIndex: Int,
        val startPositionMs: Long,
        val endPositionMs: Long,
        val items: List<DanmakuItemData>
    )

    private fun hasResolvedCurrentDanmakuMask(): Boolean {
        return hasResolvedDanmakuMask &&
                resolvedDanmakuMaskAid == currentAid &&
                resolvedDanmakuMaskCid == currentCid
    }

    private fun resetResolvedDanmakuMask(clearMasks: Boolean) {
        hasResolvedDanmakuMask = false
        resolvedDanmakuMaskAid = 0L
        resolvedDanmakuMaskCid = 0L

        if (clearMasks) {
            viewModelScope.launch(Dispatchers.Main.immediate) {
                danmakuMasks.clear()
            }
        }
    }

    private suspend fun ensureDanmakuPlayer(isLive: Boolean = false) = withContext(Dispatchers.Main) {
        danmakuLoadJob?.cancel()
        danmakuCatchUpJob?.cancel()
        danmakuPlayer?.release()
        danmakuPlayer = if (isLive) {
            // 直播模式：LiveDanmakuPlayer 需要在 Compose 中创建（需要 DanmakuSurfaceView）
            // 这里设置为 null，实际 player 由 Compose 回调设置
            logger.fInfo { "Live mode: LiveDanmakuPlayer will be created in Compose" }
            null
        } else {
            // 普通模式
            DanmakuPlayer(SimpleRenderer()).also {
                logger.fInfo { "(Re)create DanmakuPlayer: $it" }
            }
        }
    }

    /**
     * 设置从 Compose 创建的 LiveDanmakuPlayer
     */
    fun setLivePlayer(player: LiveDanmakuPlayer) {
        liveDanmakuPlayer = player
        logger.fInfo { "LiveDanmakuPlayer set from Compose: $player" }
    }

    fun prepareAutoPlayTarget(candidate: AutoPlayCandidate?) {
        if (candidate == null || isLive) {
            invalidatePreparedAutoPlayTarget()
            return
        }

        val currentPreparedTarget = preparedAutoPlayTarget
        if (currentPreparedTarget?.candidate == candidate) {
            if (
                !currentPreparedTarget.isSupported ||
                currentPreparedTarget.transitionContext != null ||
                autoPlayPrepareJob?.isActive == true
            ) {
                return
            }
        } else {
            autoPlayPrefetchJob?.cancel()
            autoPlayPrefetchJob = null
        }
                clearDanmakuLoadSession()

        val reusablePreparedTarget = currentPreparedTarget?.takeIf {
            it.candidate == candidate && hasFreshPreparedAutoPlayPlayData(it)
        }

        autoPlayPrepareJob?.cancel()
        val prepareToken = ++autoPlayPrepareToken
        preparedAutoPlayTarget = PreparedAutoPlayTarget(
            candidate = candidate,
            playData = reusablePreparedTarget?.playData,
            playDataFetchedAtMs = reusablePreparedTarget?.playDataFetchedAtMs ?: 0L,
        )

        autoPlayPrepareJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val transitionContext = resolvePreparedAutoPlayTransitionContext(candidate) ?: run {
                    markPreparedAutoPlayTargetUnsupported(candidate, prepareToken)
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    if (!isPreparedAutoPlayCandidateActive(candidate, prepareToken)) return@withContext
                    preparedAutoPlayTarget = preparedAutoPlayTarget?.copy(
                        transitionContext = transitionContext,
                        isSupported = true,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.fWarn { "Prepare auto play target failed: ${e.stackTraceToString()}" }
                markPreparedAutoPlayTargetUnsupported(candidate, prepareToken)
            }
        }
    }

    fun prefetchPreparedAutoPlayTarget(candidate: AutoPlayCandidate?) {
        if (candidate == null || isLive) return

        prepareAutoPlayTarget(candidate)

        val currentPreparedTarget = preparedAutoPlayTarget
        if (currentPreparedTarget?.candidate == candidate) {
            if (!currentPreparedTarget.isSupported || hasFreshPreparedAutoPlayPlayData(currentPreparedTarget)) {
                return
            }
        }
        if (autoPlayPrefetchJob?.isActive == true && currentPreparedTarget?.candidate == candidate) return

        autoPlayPrefetchJob?.cancel()
        val prepareToken = autoPlayPrepareToken
        autoPlayPrefetchJob = viewModelScope.launch(Dispatchers.IO) {
            val transitionContext = awaitPreparedAutoPlayTransitionContext(candidate, prepareToken)
                ?: return@launch
            if (!isPreparedAutoPlayCandidateActive(candidate, prepareToken)) return@launch

            try {
                val playData = videoPlayRepository.getPlayData(
                    aid = transitionContext.aid,
                    cid = transitionContext.cid,
                    preferApiType = Prefs.apiType,
                )

                withContext(Dispatchers.Main) {
                    if (!isPreparedAutoPlayCandidateActive(candidate, prepareToken)) return@withContext
                    preparedAutoPlayTarget = preparedAutoPlayTarget?.copy(
                        playData = playData,
                        playDataFetchedAtMs = System.currentTimeMillis(),
                        isSupported = true,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.fWarn { "Prefetch auto play target failed: ${e.stackTraceToString()}" }
            }
        }
    }

    suspend fun consumePreparedAutoPlayTarget(candidate: AutoPlayCandidate): Boolean {
        if (isLive) return false

        val prepareToken = autoPlayPrepareToken
        val transitionContext = awaitPreparedAutoPlayTransitionContext(candidate, prepareToken)
            ?: run {
                logger.fInfo { "Prepared auto play target unavailable, resolve on demand: $candidate" }
                withContext(Dispatchers.IO) {
                    resolvePreparedAutoPlayTransitionContext(candidate)
                }?.also {
                    logger.fInfo { "Resolved auto play target on demand: $candidate" }
                } ?: run {
                    logger.fInfo { "Resolve auto play target on demand failed: $candidate" }
                    return false
                }
            }
        val currentPreparedTarget = awaitPreparedAutoPlayTarget(candidate, prepareToken)
        val preparedPlayData = currentPreparedTarget?.takeIf(::hasFreshPreparedAutoPlayPlayData)?.playData

        applyPreparedAutoPlayTransitionContext(transitionContext)
        loadPlayUrl(
            avid = transitionContext.aid,
            cid = transitionContext.cid,
            epid = transitionContext.epid,
            seasonId = transitionContext.seasonId,
            continuePlayNext = true,
            preparedPlayData = preparedPlayData,
        )
        return true
    }

    fun loadPlayUrl(
        avid: Long,
        cid: Long,
        epid: Int? = null,
        seasonId: Int? = null,
        continuePlayNext: Boolean = false,
        initialSeekPositionMs: Long? = null,
        preparedPlayData: PlayData? = null,
    ) {
        val playbackSessionToken = beginVodPlaybackSession()
        val videoChanged = currentAid != avid || currentCid != cid
        showInteractiveOptionDialog = false
        if (continuePlayNext) {
            lastPlayed = 0
        }
        if (videoChanged) {
            invalidatePreparedAutoPlayTarget()
            resetResolvedDanmakuMask(clearMasks = true)
        }
        currentAid = avid
        currentCid = cid
        currentEpid = epid ?: 0
        clearVodDanmakuMergeState()
        syncCurrentInteractivePointersFromList()
        if (!isInteractivePlayback && videoInfoRepository.videoList.none { it is VideoListInteractiveNode }) {
            currentInteractiveNodeId = 0L
            currentInteractiveEdgeId = 0L
        }
        epid?.let { this.epid = it }
        seasonId?.let { this.seasonId = it }
        viewModelScope.launch(Dispatchers.Default) {
            addLogs("加载视频中")
            ensureDanmakuPlayer()
            addLogs("弹幕引擎已就绪")
            if (epid != null || seasonId != null) {
                addLogs("av$avid，cid:$cid, epid:$epid, seasonId:$seasonId")
            } else {
                addLogs("av$avid，cid:$cid")
            }

            val lastPlayEnabledSubtitle = currentSubtitleId != -1L
            if (lastPlayEnabledSubtitle) {
                logger.info { "Subtitle is enabled, next video will enable subtitle automatic" }
            }

            updateSubtitle()
            loadPlayUrl(
                avid,
                cid,
                epid ?: 0,
                preferApi = Prefs.apiType,
                proxyArea = proxyArea,
                initialSeekPositionMs = initialSeekPositionMs,
                playbackSessionToken = playbackSessionToken,
                preparedPlayData = preparedPlayData,
            )
            if (isInteractivePlayback && (!interactiveOptionsFromQuestions || interactiveOptions.isEmpty())) {
                refreshInteractiveBranches(currentInteractiveEdgeId.takeIf { it > 0L })
            }
            // addLogs("加载弹幕中")
            loadDanmaku(cid, playData?.timeLength ?: 0)
            updateDanmakuMask()

            // 加载 SponsorBlock 片段
            loadSponsorSegments(AvBvConverter.av2bv(avid), cid)

            updateVideoShot()

            //如果是继续播放下一集，且之前开启了字幕，就会自动加载第一条字幕，主要用于观看番剧时自动加载字幕
            if (continuePlayNext) {
                if (lastPlayEnabledSubtitle) enableFirstSubtitle()
            }
        }
    }

    private suspend fun loadPlayUrl(
        avid: Long,
        cid: Long,
        epid: Int = 0,
        preferApi: ApiType = Prefs.apiType,
        proxyArea: ProxyArea = ProxyArea.MainLand,
        initialSeekPositionMs: Long? = null,
        targetQuality: Resolution? = null,
        targetVideoCodec: VideoCodec? = null,
        targetAudio: Audio? = null,
        targetMediaMode: PlaybackMediaMode = currentPlaybackMediaMode,
        playbackSessionToken: Long = vodPlaybackSessionToken,
        preparedPlayData: PlayData? = null,
    ): Boolean {
        logger.fInfo { "Load play url: [av=$avid, cid=$cid, preferApi=$preferApi, proxyArea=$proxyArea, usePreparedPlayData=${preparedPlayData != null}]" }
        withContext(Dispatchers.Main) {
            if (isVodPlaybackSessionActive(playbackSessionToken)) {
                loadState = RequestState.Ready
            }
        }
        logger.fInfo { "Set request state: ready" }
        logger.fInfo { "fromSeason: $fromSeason" }
        return try {
            val playData = preparedPlayData ?: if (fromSeason) {
                videoPlayRepository.getPgcPlayData(
                    aid = avid,
                    cid = cid,
                    epid = epid,
                    preferCodec = Prefs.defaultVideoCodec.toBiliApiCodeType(),
                    preferApiType = Prefs.apiType,
                    enableProxy = proxyArea != ProxyArea.MainLand,
                    proxyArea = when (proxyArea) {
                        ProxyArea.MainLand -> ""
                        ProxyArea.HongKong -> "hk"
                        ProxyArea.TaiWan -> "tw"
                    }
                )
            } else {
                videoPlayRepository.getPlayData(
                    aid = avid,
                    cid = cid,
                    preferApiType = Prefs.apiType
                )
            }

            ensureVodPlaybackSessionActive(playbackSessionToken)

            //检查是否需要购买，如果未购买，则正片返回的dash为null，非正片例如可以免费观看的预告片等则会返回数据，此时不做提示
            withContext(Dispatchers.Main) {
                if (isVodPlaybackSessionActive(playbackSessionToken)) {
                    needPay = playData.needPay
                }
            }
            if (playData.needPay) return false

            withContext(Dispatchers.Main) {
                if (!isVodPlaybackSessionActive(playbackSessionToken)) return@withContext
                this@VideoPlayerV3ViewModel.playData = playData
                this@VideoPlayerV3ViewModel.clipInfoList = playData.clipInfoList
            }
            logger.fInfo { "Load play data response success" }
            //logger.info { "Play data: $playData" }

            //读取清晰度
            val resolutionList = mutableListOf<Resolution>()
            playData.dashVideos.forEach {
                Resolution.fromCode(it.quality)?.let { resolution ->
                    if (!resolutionList.contains(resolution)) resolutionList.add(resolution)
                }
            }

            logger.fInfo { "Video available resolution: $resolutionList" }
            availableQuality.swapListWithMainContext(resolutionList)

            ensureVodPlaybackSessionActive(playbackSessionToken)

            //读取音频
            val audioList = mutableListOf<Audio>()
            playData.dashAudios.forEach {
                Audio.fromCode(it.codecId)?.let { audio ->
                    if (!audioList.contains(audio)) audioList.add(audio)
                }
            }
            playData.dolby?.let {
                Audio.fromCode(it.codecId)?.let { audio ->
                    audioList.add(audio)
                }
            }
            playData.flac?.let {
                Audio.fromCode(it.codecId)?.let { audio ->
                    audioList.add(audio)
                }
            }

            logger.fInfo { "Video available audio: $audioList" }
            availableAudio.swapListWithMainContext(audioList)

            ensureVodPlaybackSessionActive(playbackSessionToken)

            // 确定使用哪个默认分辨率
            val defaultQualityToUse = if (
                isVerticalVideo &&
                Prefs.portraitVideoFixMode == PortraitVideoFixMode.LimitResolution1080P &&
                Prefs.defaultQuality >= Resolution.R4K
            ) {
                // 如果是竖屏视频且用户设置了竖屏视频限制最高使用1080P
                Resolution.R1080P60
            } else {
                // 否则使用普通设置
                Prefs.defaultQuality
            }

            val preferredQuality = targetQuality ?: defaultQualityToUse
            val selectedQuality = resolutionList.find { it == preferredQuality }
                ?: resolutionList.sortedByDescending { it.code }
                    .firstOrNull { it.code < preferredQuality.code }
                ?: resolutionList.last()
            withContext(Dispatchers.Main) {
                if (isVodPlaybackSessionActive(playbackSessionToken)) {
                    currentQuality = selectedQuality
                }
            }

            val preferredAudio = targetAudio ?: Prefs.defaultAudio
            val selectedAudio = when {
                availableAudio.contains(preferredAudio) -> preferredAudio
                preferredAudio == Audio.ADolbyAtoms && availableAudio.contains(Audio.AHiRes) -> Audio.AHiRes
                preferredAudio == Audio.AHiRes && availableAudio.contains(Audio.ADolbyAtoms) -> Audio.ADolbyAtoms
                availableAudio.contains(Audio.A192K) -> Audio.A192K
                availableAudio.contains(Audio.A132K) -> Audio.A132K
                availableAudio.contains(Audio.A64K) -> Audio.A64K
                else -> availableAudio.first()
            }
            withContext(Dispatchers.Main) {
                if (isVodPlaybackSessionActive(playbackSessionToken)) {
                    currentAudio = selectedAudio
                }
            }

            //再确认最终所选视频编码
            updateAvailableCodec(targetVideoCodec)

            addLogs("加载视频地址成功")
            ensureVodPlaybackSessionActive(playbackSessionToken)
            playQuality(
                qn = currentQuality.code,
                codec = currentVideoCodec,
                audio = currentAudio,
                mediaMode = targetMediaMode,
                initialSeekPositionMs = initialSeekPositionMs,
                playbackSessionToken = playbackSessionToken
            )

            withContext(Dispatchers.Main) {
                if (isVodPlaybackSessionActive(playbackSessionToken)) {
                    loadState = RequestState.Success
                }
            }
            logger.fInfo { "Load play url success" }
            true
        } catch (e: kotlinx.coroutines.CancellationException) {
            logger.fDebug { "Skip stale vod play url load: ${e.message}" }
            false
        } catch (e: Exception) {
            addLogs("加载视频地址失败：${e.localizedMessage}")
            withContext(Dispatchers.Main) {
                if (isVodPlaybackSessionActive(playbackSessionToken)) {
                    errorMessage = e.localizedMessage ?: "Unknown error"
                    loadState = RequestState.Failed
                }
            }
            logger.fException(e) { "Load video failed" }
            false
        }
    }

    private suspend fun updateAvailableCodec(preferredCodec: VideoCodec? = null) {
        if (Prefs.apiType == ApiType.App && playData!!.codec.isEmpty()) {
            // 纠正当前实际播放的编码
            val videoItem = playData!!.dashVideos
                .find { it.quality == currentQuality.code }
                ?: playData!!.dashVideos.first()
            withContext(Dispatchers.Main) {
                currentVideoCodec = VideoCodec.fromCodecId(videoItem.codecId)
            }
            logger.fInfo { "App API fixed, Select codec: $currentVideoCodec" }
            return
        }

        val supportedCodec = playData!!.codec
        val codecList =
            supportedCodec[currentQuality.code]?.mapNotNull { VideoCodec.fromCodecString(it) } ?: emptyList()

        availableVideoCodec.swapListWithMainContext(codecList)
        logger.fInfo { "Video available codec: ${availableVideoCodec.toList()}" }

        val requestedCodec = preferredCodec ?: currentVideoCodec
        logger.fInfo { "Default codec: $requestedCodec" }
        val currentVideoCodec = if (codecList.contains(requestedCodec)) {
            requestedCodec
        } else if (codecList.contains(Prefs.defaultVideoCodec)) {
            Prefs.defaultVideoCodec
        } else {
            codecList.minByOrNull { it.ordinal }!!
        }
        withContext(Dispatchers.Main) {
            this@VideoPlayerV3ViewModel.currentVideoCodec = currentVideoCodec
        }
        logger.fInfo { "Select codec: $currentVideoCodec" }
    }

    suspend fun playQuality(
        qn: Resolution = currentQuality,
        codec: VideoCodec = currentVideoCodec,
        audio: Audio = currentAudio,
        mediaMode: PlaybackMediaMode = currentPlaybackMediaMode,
        initialSeekPositionMs: Long? = null,
        playbackSessionToken: Long = vodPlaybackSessionToken
    ) {
        if (qn != currentQuality) {
            // 更新清晰度后需要先设置清晰度再更新编码列表
            withContext(Dispatchers.Main) { currentQuality = qn }
            updateAvailableCodec()
            playQuality(qn.code, currentVideoCodec, audio, mediaMode, initialSeekPositionMs, playbackSessionToken)
        } else {
            playQuality(qn.code, codec, audio, mediaMode, initialSeekPositionMs, playbackSessionToken)
        }
    }

    private suspend fun playQuality(
        qn: Int = currentQuality.code,
        codec: VideoCodec = currentVideoCodec,
        audio: Audio = currentAudio,
        mediaMode: PlaybackMediaMode = currentPlaybackMediaMode,
        initialSeekPositionMs: Long? = null,
        playbackSessionToken: Long = vodPlaybackSessionToken
    ) {
        logger.fInfo {
            "Select resolution: $qn, codec: $codec, audio: $audio, mediaMode: $mediaMode, initialSeekPositionMs: $initialSeekPositionMs"
        }
        if(playData == null) {
            return
        }

        val audioOnlyMode = mediaMode == PlaybackMediaMode.AudioOnly

        val videoItem = playData!!.dashVideos.find {
            when (Prefs.apiType) {
                ApiType.Web -> it.quality == qn && it.codecs!!.startsWith(codec.prefix)
                ApiType.App -> {
                    if (playData!!.codec.isEmpty()) it.quality == qn
                    else it.quality == qn && it.codecs!!.startsWith(codec.prefix)
                }
            }
        }
        var videoUrl = if (audioOnlyMode) null else videoItem?.baseUrl ?: playData!!.dashVideos.firstOrNull()?.baseUrl
        if (!audioOnlyMode && videoUrl == null) {
            logger.fError { "Failed to get video URL" }
            errorMessage = "获取视频地址失败"
            loadState = RequestState.Failed
            return
        }
        val videoUrls = mutableListOf<String?>()
        if (!audioOnlyMode) {
            videoUrls.add(videoItem?.baseUrl)
            videoUrls.addAll(videoItem?.backUrl ?: emptyList())
        }

        val audioItem = playData!!.dashAudios.find { it.codecId == audio.code }
            ?: playData!!.dolby.takeIf { it?.codecId == audio.code }
            ?: playData!!.flac.takeIf { it?.codecId == audio.code }
            ?: playData!!.dashAudios.minByOrNull { it.codecId }
        var audioUrl = audioItem?.baseUrl ?: playData!!.dashAudios.firstOrNull()?.baseUrl
        if (audioUrl == null) {
            logger.fError { "Failed to get audio URL" }
            errorMessage = "获取音频地址失败" 
            loadState = RequestState.Failed
            return
        }
        val audioUrls = mutableListOf<String?>()
        audioUrls.add(audioItem?.baseUrl)
        audioUrls.addAll(audioItem?.backUrl ?: emptyList())

        if (videoUrls.isNotEmpty()) {
            logger.fInfo { "all video hosts: ${videoUrls.filterNotNull().map { with(URI(it)) { "$scheme://$authority" } }}" }
        }
        logger.fInfo { "all audio hosts: ${audioUrls.map { with(URI(it)) { "$scheme://$authority" } }}" }

        //replace cdn
        if (Prefs.enableProxy && proxyArea != ProxyArea.MainLand) {
            if (videoUrl != null) {
                videoUrl = videoUrl.replaceUrlDomainWithAliCdn()
            }
            audioUrl = audioUrl.replaceUrlDomainWithAliCdn()
        } else {
            // 如果未通过网络代理获得播放地址，才判断是否应该替换为官方 cdn
            if (videoUrls.isNotEmpty()) {
                videoUrl = selectOfficialCdnUrl(videoUrls.filterNotNull())
            }
            audioUrl = selectOfficialCdnUrl(audioUrls.filterNotNull())
        }

        addLogs(
            "播放模式：${if (audioOnlyMode) "音频模式" else "正常模式"}，播放清晰度：${availableQuality.firstOrNull { it.code == qn }}, " +
                    "视频编码：${codec.getDisplayName(BVApp.context)}, " +
                    "音频编码：${(Audio.fromCode(audioItem?.codecId ?: 0))?.getDisplayName(BVApp.context) ?: "未知"}"
        )
        if (videoUrl != null) {
            addLogs("video host: ${with(URI(videoUrl)) { "$scheme://$authority" }}")
        } else {
            addLogs("video host: audio only")
        }
        addLogs("audio host: ${with(URI(audioUrl)) { "$scheme://$authority" }}")

        logger.fInfo { "Select audio: $audioItem" }

        val playUrlExpiresAt = pickEarliestUrlExpiryEpochMs(videoUrl, audioUrl)

        withContext(Dispatchers.Main) {
            if (!isVodPlaybackSessionActive(playbackSessionToken)) return@withContext
            currentVideoHeight = videoItem?.height ?: 0
            currentVideoWidth = videoItem?.width ?: 0
            logger.info { "Video url: $videoUrl" }
            logger.info { "Audio url: $audioUrl" }
            videoPlayer!!.playUrl(videoUrl, audioUrl)
            // 根据 DefaultStartPosition 设置初始跳转位置，避免在 onReady 中 seekTo 导致的状态抖动
            if (initialSeekPositionMs != null && initialSeekPositionMs > 0L) {
                logger.info { "Set initial seek position to current: ${initialSeekPositionMs}ms" }
                videoPlayer!!.setInitialSeekPosition(initialSeekPositionMs)
            } else if (lastPlayed > 0 && Prefs.playerDefaultStartPosition == PlayerDefaultStartPosition.History) {
                logger.info { "Set initial seek position to history: ${lastPlayed}ms" }
                videoPlayer!!.setInitialSeekPosition(lastPlayed.toLong())
            }
            videoPlayer!!.prepare()
            showBuffering = true
        }

        if (playUrlExpiresAt > 0L) {
            scheduleVodPlayUrlAutoRefresh(playUrlExpiresAt, playbackSessionToken)
        } else {
            cancelVodPlayUrlAutoRefresh()
            logger.fWarn { "Skip vod URL auto refresh because no deadline/expires was parsed" }
        }
    }

    fun refreshInteractiveBranches(edgeId: Long? = null) {
        val interactiveContext = videoInfoRepository.interactivePlaybackContext ?: return
        interactiveOptionsLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                BiliHttpApi.getInteractiveEdgeInfo(
                    bvid = interactiveContext.bvid,
                    graphVersion = interactiveContext.graphVersion,
                    edgeId = edgeId,
                ).getResponseData().let { response ->
                    val questionNodes = response.edges?.questions
                        ?.flatMap { question -> question.choices }
                        ?.mapIndexed { index, choice ->
                            InteractiveNode.fromChoice(choice, "选项 ${index + 1}")
                        }
                        .orEmpty()
                    Triple(
                        response.edgeId,
                        if (questionNodes.isNotEmpty()) {
                            questionNodes
                        } else {
                            response.storyList.map(InteractiveNode::fromStoryNode)
                        },
                        questionNodes.isNotEmpty()
                    )
                }
            }.onSuccess { (responseEdgeId, nodes, fromQuestionChoices) ->
                if (nodes.isEmpty()) {
                    logger.fWarn { "Refresh interactive branches returned empty nodes, edgeId=$edgeId" }
                    withContext(Dispatchers.Main) {
                        interactiveOptionsLoading = false
                        pendingInteractiveOptionDialogRequest = false
                    }
                    return@onSuccess
                }
                val interactiveVideoList = nodes.mapIndexed { index, node ->
                    VideoListInteractiveNode(
                        aid = currentAid,
                        cid = node.cid,
                        title = title,
                        partTitle = node.title,
                        index = index,
                        nodeId = node.nodeId,
                        edgeId = node.edgeId,
                        startPos = node.startPos,
                        isCurrent = node.isCurrent,
                    )
                }
                withContext(Dispatchers.Main) {
                    interactiveOptionsLoading = false
                    interactiveOptionsFromQuestions = fromQuestionChoices
                    videoInfoRepository.videoList.clear()
                    videoInfoRepository.videoList.addAll(interactiveVideoList)
                    currentInteractiveNodeId = nodes.firstOrNull { it.isCurrent }?.nodeId
                        ?: currentInteractiveNodeId
                    currentInteractiveEdgeId = responseEdgeId
                        ?: nodes.firstOrNull { it.isCurrent }?.edgeId
                        ?: edgeId
                        ?: currentInteractiveEdgeId
                    if (pendingInteractiveOptionDialogRequest && fromQuestionChoices) {
                        showInteractiveOptionDialog = true
                        pendingInteractiveOptionDialogRequest = false
                    } else {
                        if (pendingInteractiveOptionDialogRequest && !fromQuestionChoices) {
                            showInteractiveOptionDialog = false
                            pendingInteractiveOptionDialogRequest = false
                        }
                    }
                }
            }.onFailure {
                interactiveOptionsLoading = false
                pendingInteractiveOptionDialogRequest = false
                logger.fWarn { "Refresh interactive branches failed: ${it.stackTraceToString()}" }
            }
        }
    }

    fun requestInteractiveOptionDialog(): Boolean {
        if (!isInteractivePlayback) return false
        pendingInteractiveOptionDialogRequest = true
        syncCurrentInteractivePointersFromList()
        val hasOptions = interactiveOptionsFromQuestions && interactiveOptions.isNotEmpty()
        if (hasOptions) {
            showInteractiveOptionDialog = true
            pendingInteractiveOptionDialogRequest = false
            return true
        }
        refreshInteractiveBranches(currentInteractiveEdgeId.takeIf { it > 0L })
        return false
    }

    fun dismissInteractiveOptionDialog() {
        showInteractiveOptionDialog = false
        pendingInteractiveOptionDialogRequest = false
    }

    fun selectInteractiveNode(nodeId: Long) {
        currentInteractiveNodeId = nodeId
    }

    fun playInteractiveOption(option: VideoListInteractiveNode) {
        showInteractiveOptionDialog = false
        pendingInteractiveOptionDialogRequest = false
        title = option.title
        partTitle = option.partTitle
        currentInteractiveEdgeId = option.edgeId ?: currentInteractiveEdgeId
        selectInteractiveNode(option.nodeId)
        loadPlayUrl(
            avid = option.aid,
            cid = option.cid,
            epid = option.epid,
            seasonId = option.seasonId,
            continuePlayNext = true,
            initialSeekPositionMs = option.startPos?.times(1000L)
        )
        refreshInteractiveBranches(option.edgeId)
    }

    private fun syncCurrentInteractivePointersFromList() {
        val currentInteractiveOption = interactiveOptions.firstOrNull {
            it.isCurrent || it.cid == currentCid || it.nodeId == currentInteractiveNodeId
        } ?: return
        currentInteractiveNodeId = currentInteractiveOption.nodeId
        currentInteractiveEdgeId = currentInteractiveOption.edgeId ?: currentInteractiveEdgeId
    }

    suspend fun loadDanmaku(
        cid: Long,
        durationMs: Long = 0,
        initialPositionMsOverride: Long? = null
    ) {
        danmakuLoadJob?.cancel()
        danmakuCatchUpJob?.cancel()
        val loadToken = ++danmakuLoadToken
        val loadSession = DanmakuLoadSession(token = loadToken)
        danmakuLoadSession = loadSession
        runCatching {
            withContext(Dispatchers.Main) {
                danmakuData.clear()
                lastDanmakuCatchUpPositionMs = -1L
                lastDanmakuCatchUpSegment = -1
            }

            val initialPositionMs = initialPositionMsOverride ?: withContext(Dispatchers.Main) {
                videoPlayer?.currentPosition?.takeIf { it > 0L }
            } ?: calculateInitialDanmakuPositionMs()

            val maxSegments = calculateDanmakuMaxSegments(durationMs)
            val startSegment = calculateInitialDanmakuSegment(initialPositionMs)
            val segmentOrder = buildDanmakuSegmentOrder(startSegment, maxSegments)
            if (segmentOrder.isEmpty()) {
                addLogs("未找到可加载的弹幕分段")
                return@runCatching
            }

            var cachedSegments = 0
            var initialEmittedDanmaku = 0
            val initialSegments = segmentOrder.take(INITIAL_DANMAKU_SEGMENT_PREFETCH)

            initialSegments.forEach { segmentIndex ->
                ensureDanmakuLoadActive(loadToken)
                val slices = loadDanmakuSegmentSlices(
                    cid = cid,
                    segmentIndex = segmentIndex,
                    loadToken = loadToken,
                    loadSession = loadSession
                )
                if (slices.isNotEmpty()) {
                    cachedSegments++
                }
            }

            val startSegmentSlices = loadSession.danmakuSlicesBySegment[startSegment].orEmpty()
            if (startSegmentSlices.isNotEmpty()) {
                val initialSliceIndex = findDanmakuSliceIndexForPosition(
                    slices = startSegmentSlices,
                    positionMs = initialPositionMs
                )
                initialEmittedDanmaku += emitDanmakuSlices(
                    segmentIndex = startSegment,
                    slices = startSegmentSlices,
                    loadSession = loadSession,
                    startSliceIndex = initialSliceIndex,
                    endExclusive = (initialSliceIndex + 1).coerceAtMost(startSegmentSlices.size),
                    loadToken = loadToken
                )
            }

            if (initialEmittedDanmaku == 0) {
                val fallbackSegmentIndex = initialSegments.firstOrNull {
                    loadSession.danmakuSlicesBySegment[it]?.isNotEmpty() == true
                }
                if (fallbackSegmentIndex != null) {
                    initialEmittedDanmaku += emitDanmakuSlices(
                        segmentIndex = fallbackSegmentIndex,
                        slices = loadSession.danmakuSlicesBySegment.getValue(fallbackSegmentIndex),
                        loadSession = loadSession,
                        startSliceIndex = 0,
                        endExclusive = 1,
                        loadToken = loadToken
                    )
                }
            }

            addLogs("已缓存 $cachedSegments 个弹幕分段，优先投喂 ${initialEmittedDanmaku} 条")

            if (segmentOrder.isNotEmpty()) {
                danmakuLoadJob = viewModelScope.launch(Dispatchers.Default) {
                    runCatching {
                        var backgroundEmittedDanmaku = 0
                        segmentOrder.forEach { segmentIndex ->
                            ensureDanmakuLoadActive(loadToken)
                            val slices = loadDanmakuSegmentSlices(
                                cid = cid,
                                segmentIndex = segmentIndex,
                                loadToken = loadToken,
                                loadSession = loadSession
                            )
                            if (slices.isEmpty()) {
                                return@forEach
                            }
                            val emittedCount = emitDanmakuSlices(
                                segmentIndex = segmentIndex,
                                slices = slices,
                                loadSession = loadSession,
                                startSliceIndex = loadSession.nextDanmakuSliceIndexBySegment[segmentIndex] ?: 0,
                                loadToken = loadToken
                            )
                            if (emittedCount > 0) {
                                backgroundEmittedDanmaku += emittedCount
                                addLogs("后台已发射第 $segmentIndex 段切片，累计 ${danmakuData.size} 条")
                            }
                        }
                        backgroundEmittedDanmaku += flushPendingMergedDanmaku(loadSession, loadToken)
                    }.onFailure {
                        if (it !is kotlinx.coroutines.CancellationException) {
                            addLogs("后台补齐弹幕失败：${it.localizedMessage}")
                            logger.fWarn { "Background danmaku loading failed: ${it.stackTraceToString()}" }
                        }
                    }
                }
            }
        }.onFailure {
            if (it !is kotlinx.coroutines.CancellationException) {
                addLogs("加载弹幕失败：${it.localizedMessage}")
                logger.fWarn { "Load danmaku filed: ${it.stackTraceToString()}" }
            }
        }.onSuccess {
            addLogs("已启动弹幕切片加载，当前 ${danmakuData.size} 条")
            logger.fInfo { "Load danmaku slices started, size=${danmakuData.size}" }
        }
    }

    fun reloadDanmakuAfterSeek(positionMs: Long, shouldPlay: Boolean) {
        if (isLive || currentCid <= 0L) return

        val durationMs = playData?.timeLength
            ?.takeIf { it > 0L }
            ?: videoPlayer?.duration
            ?: 0L

        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                addLogs("跳转后重载弹幕")
                val reusedExistingPlayer = withContext(Dispatchers.Main) {
                    danmakuPlayer?.let {
                        it.pause()
                        it.clearData()
                        true
                    } ?: false
                }
                if (!reusedExistingPlayer) {
                    ensureDanmakuPlayer()
                }
                loadDanmaku(
                    cid = currentCid,
                    durationMs = durationMs,
                    initialPositionMsOverride = positionMs
                )
                withContext(Dispatchers.Main) {
                    danmakuPlayer?.seekTo(positionMs)
                    if (shouldPlay) {
                        danmakuPlayer?.start()
                    } else {
                        danmakuPlayer?.pause()
                    }
                }
            }.onFailure {
                if (it !is kotlinx.coroutines.CancellationException) {
                    logger.fWarn { "Reload danmaku after seek failed: ${it.stackTraceToString()}" }
                    addLogs("跳转后重载弹幕失败：${it.localizedMessage}")
                }
            }
        }
    }

    fun ensureDanmakuCoverage(positionMs: Long) {
        if (isLive || currentCid <= 0L || positionMs < 0L) return

        val normalizedPosition = positionMs.coerceAtLeast(0L)
        val currentSegment = calculateInitialDanmakuSegment(normalizedPosition)
        val shouldSkip = currentSegment == lastDanmakuCatchUpSegment &&
            lastDanmakuCatchUpPositionMs >= 0L &&
            normalizedPosition - lastDanmakuCatchUpPositionMs < 15_000L
        if (shouldSkip) return

        lastDanmakuCatchUpPositionMs = normalizedPosition
        lastDanmakuCatchUpSegment = currentSegment

        if (danmakuCatchUpJob?.isActive == true) return

        val loadToken = danmakuLoadToken
        val durationMs = playData?.timeLength
            ?.takeIf { it > 0L }
            ?: videoPlayer?.duration
            ?: 0L
        val maxSegments = calculateDanmakuMaxSegments(durationMs)
        val loadSession = danmakuLoadSession?.takeIf { it.token == loadToken } ?: return

        danmakuCatchUpJob = viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                val targetSegments = buildList {
                    add(currentSegment.coerceIn(1, maxSegments.coerceAtLeast(1)))
                    val nextSegment = currentSegment + 1
                    if (nextSegment <= maxSegments) add(nextSegment)
                }
                targetSegments.forEachIndexed { index, segmentIndex ->
                    ensureDanmakuLoadActive(loadToken)
                    val anchorPosition = if (index == 0) normalizedPosition else segmentStartPositionMs(segmentIndex)
                    emitDanmakuSlicesAroundPosition(
                        cid = currentCid,
                        segmentIndex = segmentIndex,
                        positionMs = anchorPosition,
                        loadToken = loadToken,
                        loadSession = loadSession
                    )
                }
            }.onFailure {
                if (it !is CancellationException) {
                    logger.fDebug { "Danmaku catch-up skipped: ${it.message}" }
                }
            }
        }
    }

    private fun calculateDanmakuMaxSegments(durationMs: Long): Int {
        return if (durationMs > 0) {
            kotlin.math.ceil(durationMs / DANMAKU_SEGMENT_DURATION_MS.toDouble()).toInt().coerceAtLeast(1)
        } else {
            20
        }
    }

    private fun calculateInitialDanmakuPositionMs(): Long {
        return if (
            lastPlayed > 0 &&
            Prefs.playerDefaultStartPosition == PlayerDefaultStartPosition.History
        ) {
            lastPlayed.toLong()
        } else {
            0L
        }
    }

    private fun calculateInitialDanmakuSegment(initialPositionMs: Long): Int {
        return (initialPositionMs / DANMAKU_SEGMENT_DURATION_MS).toInt() + 1
    }

    private fun buildDanmakuSegmentOrder(startSegment: Int, maxSegments: Int): List<Int> {
        if (maxSegments <= 0) return emptyList()
        val normalizedStart = startSegment.coerceIn(1, maxSegments)
        val ordered = LinkedHashSet<Int>()
        ordered.add(normalizedStart)
        if (normalizedStart + 1 <= maxSegments) {
            ordered.add(normalizedStart + 1)
        }
        if (normalizedStart - 1 >= 1) {
            ordered.add(normalizedStart - 1)
        }
        for (segment in (normalizedStart + 2)..maxSegments) {
            ordered.add(segment)
        }
        for (segment in 1 until (normalizedStart - 1).coerceAtLeast(1)) {
            ordered.add(segment)
        }
        return ordered.toList()
    }

    private fun segmentStartPositionMs(segmentIndex: Int): Long {
        return ((segmentIndex - 1).coerceAtLeast(0)) * DANMAKU_SEGMENT_DURATION_MS
    }

    private suspend fun loadDanmakuSegmentSlices(
        cid: Long,
        segmentIndex: Int,
        loadToken: Long,
        loadSession: DanmakuLoadSession
    ): List<DanmakuSlice> {
        loadSession.danmakuSlicesBySegment[segmentIndex]?.let { return it }

        ensureDanmakuLoadActive(loadToken)
        val segmentData = BiliHttpApi.getDanmakuSeg(
            cid = cid,
            avid = currentAid,
            segmentIndex = segmentIndex,
            sessData = Prefs.sessData
        )
        ensureDanmakuLoadActive(loadToken)
        if (segmentData.isEmpty()) {
            loadSession.danmakuSlicesBySegment[segmentIndex] = emptyList()
            loadSession.nextDanmakuSliceIndexBySegment[segmentIndex] = 0
            return emptyList()
        }

        var deduplicatedCount = 0
        var filteredCount = 0
        val newDanmaku = ArrayList<dev.aaa1115910.biliapi.http.entity.danmaku.DanmakuData>(segmentData.size)
        segmentData.forEach { danmaku ->
            if (!loadSession.loadedDanmakuIds.add(danmaku.dmid)) {
                deduplicatedCount++
                return@forEach
            }
            if (danmaku.level < currentDanmakuFilterLevel) {
                filteredCount++
                return@forEach
            }
            newDanmaku.add(danmaku)
        }

        if (deduplicatedCount > 0) {
            logger.fInfo { "Deduplicated $deduplicatedCount danmaku in segment $segmentIndex" }
        }
        if (filteredCount > 0) {
            logger.fInfo { "Filtered $filteredCount danmaku in segment $segmentIndex with level < $currentDanmakuFilterLevel" }
        }
        if (newDanmaku.isEmpty()) {
            loadSession.danmakuSlicesBySegment[segmentIndex] = emptyList()
            loadSession.nextDanmakuSliceIndexBySegment[segmentIndex] = 0
            return emptyList()
        }

        ensureDanmakuLoadActive(loadToken)
        val convertedItems = if (currentDanmakuMergeEnabled) {
            val mergeResult = VodDanmakuMerger.processSegment(
                segmentDanmaku = newDanmaku,
                segmentIndex = segmentIndex,
                segmentDurationMs = DANMAKU_SEGMENT_DURATION_MS,
                state = loadSession.vodDanmakuMergeState
            )
            logDanmakuMergeResult(segmentIndex, mergeResult)
            convertMergedDanmakuItems(mergeResult.emittedDanmaku)
        } else {
            convertDanmakuItems(newDanmaku)
        }

        val slices = buildDanmakuSlices(segmentIndex, convertedItems)
        loadSession.danmakuSlicesBySegment[segmentIndex] = slices
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            if (!loadSession.nextDanmakuSliceIndexBySegment.containsKey(segmentIndex)) {
                loadSession.nextDanmakuSliceIndexBySegment[segmentIndex] = 0
            }
        } else {
            loadSession.nextDanmakuSliceIndexBySegment.putIfAbsent(segmentIndex, 0)
        }
        if (slices.isNotEmpty()) {
            logger.fInfo {
                "Prepared ${slices.size} danmaku slices for segment $segmentIndex, total=${convertedItems.size}, first=${slices.first().startPositionMs}, last=${slices.last().endPositionMs}"
            }
        }
        return slices
    }

    fun updateDanmakuMergeEnabled(enabled: Boolean) {
        if (currentDanmakuMergeEnabled == enabled) return

        val wasEnabled = currentDanmakuMergeEnabled
        currentDanmakuMergeEnabled = enabled

        if (wasEnabled) {
            viewModelScope.launch(Dispatchers.Default) {
                flushPendingMergedDanmaku()
            }
        } else {
            clearVodDanmakuMergeState()
        }
    }

    private fun convertDanmakuItems(
        rawDanmaku: List<dev.aaa1115910.biliapi.http.entity.danmaku.DanmakuData>
    ): List<DanmakuItemData> {
        return rawDanmaku
            .sortedBy { it.time }
            .map { it.toDanmakuItemData(it.text) }
    }

    private fun convertMergedDanmakuItems(
        mergedDanmaku: List<MergedDanmakuEntry>
    ): List<DanmakuItemData> {
        return mergedDanmaku
            .sortedBy { it.source.time }
            .map { it.source.toDanmakuItemData(it.content) }
    }

    private fun dev.aaa1115910.biliapi.http.entity.danmaku.DanmakuData.toDanmakuItemData(
        content: String
    ): DanmakuItemData {
        return DanmakuItemData(
            danmakuId = dmid,
            position = (time * 1000).toLong(),
            content = content,
            mode = when (type) {
                4 -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                5 -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                else -> DanmakuItemData.DANMAKU_MODE_ROLLING
            },
            textSize = size,
            textColor = Color(color).toArgb()
        )
    }

    private fun buildDanmakuSlices(
        segmentIndex: Int,
        items: List<DanmakuItemData>
    ): List<DanmakuSlice> {
        if (items.isEmpty()) return emptyList()

        return items.chunked(DANMAKU_SLICE_SIZE).mapIndexed { sliceIndex, sliceItems ->
            DanmakuSlice(
                segmentIndex = segmentIndex,
                sliceIndex = sliceIndex,
                startPositionMs = sliceItems.first().position,
                endPositionMs = sliceItems.last().position,
                items = sliceItems
            )
        }
    }

    private fun findDanmakuSliceIndexForPosition(
        slices: List<DanmakuSlice>,
        positionMs: Long
    ): Int {
        if (slices.isEmpty() || positionMs <= 0L) return 0

        val directHit = slices.indexOfFirst {
            positionMs >= it.startPositionMs && positionMs <= it.endPositionMs
        }
        if (directHit >= 0) return directHit

        val nearestPrevious = slices.indexOfLast { it.startPositionMs <= positionMs }
        return nearestPrevious.coerceAtLeast(0)
    }

    private suspend fun emitDanmakuSlicesAroundPosition(
        cid: Long,
        segmentIndex: Int,
        positionMs: Long,
        loadToken: Long,
        loadSession: DanmakuLoadSession
    ): Int {
        val slices = loadDanmakuSegmentSlices(
            cid = cid,
            segmentIndex = segmentIndex,
            loadToken = loadToken,
            loadSession = loadSession
        )
        if (slices.isEmpty()) return 0

        val targetSliceIndex = findDanmakuSliceIndexForPosition(slices, positionMs)
        val nextSliceIndex = loadSession.nextDanmakuSliceIndexBySegment[segmentIndex] ?: 0
        val startSliceIndex = maxOf(nextSliceIndex, targetSliceIndex)
        val endExclusive = (targetSliceIndex + 2).coerceAtMost(slices.size)
        if (startSliceIndex >= endExclusive) return 0

        return emitDanmakuSlices(
            segmentIndex = segmentIndex,
            slices = slices,
            loadSession = loadSession,
            startSliceIndex = startSliceIndex,
            endExclusive = endExclusive,
            loadToken = loadToken
        )
    }

    private suspend fun emitDanmakuSlices(
        segmentIndex: Int,
        slices: List<DanmakuSlice>,
        loadSession: DanmakuLoadSession,
        startSliceIndex: Int? = null,
        endExclusive: Int = slices.size,
        loadToken: Long? = null
    ): Int {
        if (slices.isEmpty()) {
            loadSession.nextDanmakuSliceIndexBySegment[segmentIndex] = 0
            return 0
        }

        val normalizedStart = (startSliceIndex ?: (loadSession.nextDanmakuSliceIndexBySegment[segmentIndex] ?: 0))
            .coerceIn(0, slices.size)
        val normalizedEnd = endExclusive.coerceIn(normalizedStart, slices.size)
        var emittedCount = 0

        for (sliceIndex in normalizedStart until normalizedEnd) {
            loadToken?.let { ensureDanmakuLoadActive(it) }
            val slice = slices[sliceIndex]
            emitDanmakuItems(slice.items, loadToken)
            loadSession.nextDanmakuSliceIndexBySegment[segmentIndex] = sliceIndex + 1
            emittedCount += slice.items.size
            logger.fDebug {
                "Emitted danmaku slice segment=${slice.segmentIndex}, slice=${slice.sliceIndex}, size=${slice.items.size}, range=${slice.startPositionMs}-${slice.endPositionMs}"
            }
            if (sliceIndex + 1 < normalizedEnd) {
                delay(DANMAKU_SLICE_EMIT_DELAY_MS)
            }
        }

        return emittedCount
    }

    private suspend fun emitDanmakuItems(
        items: List<DanmakuItemData>,
        loadToken: Long? = null
    ) {
        if (items.isEmpty()) return

        items.chunked(DANMAKU_BATCH_SIZE).forEach { chunk ->
            loadToken?.let { ensureDanmakuLoadActive(it) }
            danmakuData.addAll(chunk)
            danmakuPlayer?.updateData(chunk)
            delay(8)
        }
    }

    private suspend fun flushPendingMergedDanmaku(
        loadSession: DanmakuLoadSession? = danmakuLoadSession,
        loadToken: Long? = null
    ): Int {
        if (loadSession == null) return 0

        if (!currentDanmakuMergeEnabled) {
            loadSession.vodDanmakuMergeState.clear()
            return 0
        }

        val pendingDanmaku = VodDanmakuMerger.flushPending(loadSession.vodDanmakuMergeState)
        if (pendingDanmaku.isEmpty()) {
            return 0
        }

        val convertedItems = convertMergedDanmakuItems(pendingDanmaku)
        val pendingSegmentIndex = Int.MAX_VALUE
        val slices = buildDanmakuSlices(pendingSegmentIndex, convertedItems)
        emitDanmakuSlices(
            segmentIndex = pendingSegmentIndex,
            slices = slices,
            loadSession = loadSession,
            startSliceIndex = 0,
            loadToken = loadToken
        )
        logger.fInfo { "Flushed ${convertedItems.size} pending merged danmaku" }
        return convertedItems.size
    }

    private fun clearDanmakuSliceState() {
        danmakuLoadSession?.danmakuSlicesBySegment?.clear()
        danmakuLoadSession?.nextDanmakuSliceIndexBySegment?.clear()
    }

    private fun clearVodDanmakuMergeState() {
        danmakuLoadSession?.vodDanmakuMergeState?.clear()
    }

    private fun clearDanmakuLoadSession() {
        danmakuLoadSession = null
    }

    private fun logDanmakuMergeResult(
        segmentIndex: Int,
        mergeResult: DanmakuSegmentMergeResult
    ) {
        if (mergeResult.mergedDuplicateCount <= 0) return
        logger.fInfo {
            "Merged ${mergeResult.mergedDuplicateCount} duplicate danmaku in segment $segmentIndex, emitted=${mergeResult.emittedDanmaku.size}"
        }
    }

    private suspend fun ensureDanmakuLoadActive(loadToken: Long) {
        kotlinx.coroutines.currentCoroutineContext().ensureActive()
        if (loadToken != danmakuLoadToken) {
            throw kotlinx.coroutines.CancellationException("Danmaku load token expired")
        }
    }

    /**
     * 加载 SponsorBlock 片段数据
     */
    fun loadSponsorSegments(bvid: String, cid: Long) {
        if (!enableSponsorBlock) {
            logger.fInfo { "SponsorBlock is disabled, skip loading segments" }
            return
        }

        SponsorBlockHttpApi.updateBaseUrl(Prefs.sponsorBlockApiServer)

        viewModelScope.launch(Dispatchers.IO) {
            addLogs("加载 SponsorBlock 片段")
            logger.fInfo { "Loading SponsorBlock segments for $bvid/$cid" }

            SponsorBlockHttpApi.getSkipSegments(
                bvid = bvid,
                cid = cid,
                categories = listOf("sponsor")  // 暂时只获取赞助广告类别
            ).fold(
                onSuccess = { segments ->
                    sponsorSegments = segments
                    addLogs("加载到 ${segments.size} 个片段")
                    logger.fInfo { "Loaded ${segments.size} sponsor segments" }
                },
                onFailure = { error ->
                    sponsorSegments = emptyList()
                    addLogs("加载片段失败: ${error.message}")
                    logger.fWarn { "Failed to load sponsor segments: ${error.message}" }
                }
            )
        }
    }

    /**
     * 检查当前播放位置是否需要显示SponsorBlock提示
     * @param currentPositionMs 当前播放位置（毫秒）
     */
    fun checkSponsorBlockPosition(currentPositionMs: Long) {
        if (!enableSponsorBlock || sponsorSegments.isEmpty() || showSponsorBlockTip) {
            return
        }

        val thresholdMs = -500L  // 提前 0.5 秒开始提示
        val segment = sponsorSegments.firstOrNull {
            currentPositionMs >= (it.startTime + thresholdMs) &&
            currentPositionMs < it.endTime
        }

        if (segment != null) {
            currentSponsorSegment = segment
            showSponsorBlockTip = true
            logger.fDebug { "Showing SponsorBlock tip for segment: ${segment.category}" }
        }
    }

    /**
     * 跳过当前SponsorBlock片段
     */
    fun skipSponsorSegment(segmentOverride: dev.aaa1115910.biliapi.entity.sponsorblock.SponsorSegment? = null) {
        val segment = segmentOverride ?: currentSponsorSegment
        if (segment != null) {
            currentSponsorSegment = segment
            val targetPosition = segment.endTime
            videoPlayer?.seekTo(targetPosition)
            viewModelScope.launch(Dispatchers.Main) {
                addLogs("跳过片段: ${segment.category}")
            }
            logger.fInfo { "Skipped sponsor segment, seeking to $targetPosition ms" }
        } else {
            logger.fWarn { "Skip sponsor segment ignored because no active segment was available" }
        }
        dismissSponsorBlockTip()
    }

    /**
     * 关闭SponsorBlock提示
     */
    fun dismissSponsorBlockTip() {
        showSponsorBlockTip = false
        currentSponsorSegment = null
    }

    private fun nextSubtitleLoadToken(slot: SubtitleSlot): Long {
        return when (slot) {
            SubtitleSlot.Primary -> {
                currentPrimarySubtitleLoadToken += 1
                currentPrimarySubtitleLoadToken
            }

            SubtitleSlot.Secondary -> {
                currentSecondarySubtitleLoadToken += 1
                currentSecondarySubtitleLoadToken
            }
        }
    }

    private fun invalidateSubtitleLoad(slot: SubtitleSlot) {
        when (slot) {
            SubtitleSlot.Primary -> currentPrimarySubtitleLoadToken += 1
            SubtitleSlot.Secondary -> currentSecondarySubtitleLoadToken += 1
        }
    }

    private fun isCurrentSubtitleLoad(slot: SubtitleSlot, token: Long): Boolean {
        return when (slot) {
            SubtitleSlot.Primary -> currentPrimarySubtitleLoadToken == token
            SubtitleSlot.Secondary -> currentSecondarySubtitleLoadToken == token
        }
    }

    private fun clearSubtitleState(slot: SubtitleSlot, invalidateRequest: Boolean = false) {
        if (invalidateRequest) {
            invalidateSubtitleLoad(slot)
        }
        when (slot) {
            SubtitleSlot.Primary -> {
                currentSubtitleId = -1L
                currentSubtitleType = SubtitleType.CC
                currentSubtitleData.clear()
            }

            SubtitleSlot.Secondary -> {
                currentSecondarySubtitleId = -1L
                currentSecondarySubtitleType = SubtitleType.CC
                currentSecondarySubtitleData.clear()
            }
        }
    }

    private fun applySubtitleState(
        slot: SubtitleSlot,
        id: Long,
        type: SubtitleType,
        subtitleData: List<SubtitleItem>
    ) {
        when (slot) {
            SubtitleSlot.Primary -> {
                currentSubtitleId = id
                currentSubtitleType = type
                currentSubtitleData.swapList(subtitleData)
            }

            SubtitleSlot.Secondary -> {
                currentSecondarySubtitleId = id
                currentSecondarySubtitleType = type
                currentSecondarySubtitleData.swapList(subtitleData)
            }
        }
    }

    private suspend fun updateSubtitle() {
        clearSubtitleState(SubtitleSlot.Primary, invalidateRequest = true)
        clearSubtitleState(SubtitleSlot.Secondary, invalidateRequest = true)

        runCatching {
            val subtitleData = videoPlayRepository.getSubtitle(
                aid = currentAid,
                cid = currentCid,
                preferApiType = Prefs.apiType
            )
            withContext(Dispatchers.Main) {
                availableSubtitle.clear()
                availableSubtitle.add(
                    Subtitle(
                        id = -1,
                        lang = "",
                        langDoc = "关闭",
                        url = "",
                        type = SubtitleType.CC,
                        aiType = SubtitleAiType.Normal,
                        aiStatus = SubtitleAiStatus.None
                    )
                )
                availableSubtitle.addAll(subtitleData)
                availableSubtitle.sortBy { it.id }
            }
            addLogs("获取到 ${subtitleData.size} 条字幕: ${subtitleData.map { it.langDoc }}")
            logger.fInfo { "Update subtitle size: ${subtitleData.size}" }
        }.onFailure {
            addLogs("获取字幕失败：${it.localizedMessage}")
            logger.fWarn { "Update subtitle failed: ${it.stackTraceToString()}" }
        }
    }

    private fun enableFirstSubtitle() {
        runCatching {
            logger.info { "Load first subtitle" }
            logger.info { "availableSubtitle: ${availableSubtitle.toList()}" }
            loadSubtitle(
                availableSubtitle
                    .firstOrNull { it.id != -1L }?.id
                    ?: throw IllegalStateException("No available subtitle")
            )
        }.onFailure {
            logger.error { "Load first subtitle failed: ${it.stackTraceToString()}" }
        }
    }

    private suspend fun addLogs(text: String) {
        logger.fInfo { text }
        if (!Prefs.playerShowDebugInfo) {
            return
        }
        val lines = logs.lines().toMutableList()
        lines.add(text)
        while (lines.size > 8) {
            lines.removeAt(0)
        }
        var newTip = ""
        lines.forEach {
            newTip += if (newTip == "") it else "\n$it"
        }
        withContext(Dispatchers.Main) {
            logs = newTip
            lastChangedLog = System.currentTimeMillis()
        }
    }

    suspend fun uploadHistory(time: Int) {
        runCatching {
            if (!fromSeason) {
                logger.info { "Send heartbeat: [avid=$currentAid, cid=$currentCid, time=$time]" }
                videoPlayRepository.sendHeartbeat(
                    aid = currentAid,
                    cid = currentCid,
                    time = time,
                    preferApiType = Prefs.apiType
                )
            } else {
                logger.info { "Send heartbeat: [avid=$currentAid, cid=$currentCid, epid=$epid, sid=$seasonId, time=$time]" }
                videoPlayRepository.sendHeartbeat(
                    aid = currentAid,
                    cid = currentCid,
                    time = time,
                    type = HeartbeatVideoType.Season,
                    subType = subType,
                    epid = epid,
                    seasonId = seasonId,
                    preferApiType = Prefs.apiType
                )
            }
        }.onSuccess {
            logger.info { "Send heartbeat success" }
        }.onFailure {
            logger.warn { "Send heartbeat failed: ${it.stackTraceToString()}" }
        }
    }

    private fun ensureLiveRoomEntryReported(roomId: Int) {
        if (!isLive || roomId <= 0) return
        if (Prefs.liveIncognitoMode) {
            logger.fInfo { "Skip live room entry report in live incognito mode: roomId=$roomId" }
            return
        }
        if (!liveRepository.canReportRoomEntryAction()) {
            logger.fInfo { "Skip live room entry report because auth is unavailable: roomId=$roomId" }
            return
        }
        if (reportedLiveRoomId == roomId || reportingLiveRoomId == roomId) {
            return
        }

        liveRoomEntryReportJob?.cancel()
        reportingLiveRoomId = roomId
        liveRoomEntryReportJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                liveRepository.reportRoomEntryAction(roomId)
            }.onSuccess {
                reportedLiveRoomId = roomId
                logger.fInfo { "Reported live room entry successfully: roomId=$roomId" }
            }.onFailure {
                logger.fWarn { "Report live room entry failed: roomId=$roomId, error=${it.message}" }
            }
            reportingLiveRoomId = null
        }
    }

    fun loadSubtitle(id: Long) {
        loadSubtitle(SubtitleSlot.Primary, id)
    }

    fun loadSecondarySubtitle(id: Long) {
        loadSubtitle(SubtitleSlot.Secondary, id)
    }

    private fun loadSubtitle(slot: SubtitleSlot, id: Long) {
        val requestToken = nextSubtitleLoadToken(slot)
        viewModelScope.launch(Dispatchers.IO) {
            if (id == -1L) {
                withContext(Dispatchers.Main) {
                    if (!isCurrentSubtitleLoad(slot, requestToken)) {
                        return@withContext
                    }
                    clearSubtitleState(slot)
                    if (slot == SubtitleSlot.Primary) {
                        clearSubtitleState(SubtitleSlot.Secondary, invalidateRequest = true)
                    }
                }
                return@launch
            }
            var subtitleName = ""
            try {
                val subtitle = availableSubtitle.find { it.id == id }
                    ?: throw IllegalArgumentException("Subtitle $id not found")
                subtitleName = subtitle.langDoc
                val isAI = subtitle.type == SubtitleType.AI
                logger.info { "Load ${slot.debugName} subtitle url: ${subtitle.url}, isAI: $isAI" }
                if (slot == SubtitleSlot.Secondary && currentSubtitleId == id) {
                    withContext(Dispatchers.Main) {
                        if (!isCurrentSubtitleLoad(slot, requestToken)) {
                            return@withContext
                        }
                        clearSubtitleState(SubtitleSlot.Secondary)
                    }
                    return@launch
                }
                val client = HttpClient(OkHttp)
                val responseText = try {
                    client.get(subtitle.url).bodyAsText()
                } finally {
                    client.close()
                }
                currentCoroutineContext().ensureActive()
                val subtitleData = SubtitleParser.fromBccString(responseText, isAI)
                currentCoroutineContext().ensureActive()
                withContext(Dispatchers.Main) {
                    if (!isCurrentSubtitleLoad(slot, requestToken)) {
                        return@withContext
                    }
                    applySubtitleState(slot, id, subtitle.type, subtitleData)
                    if (slot == SubtitleSlot.Primary && currentSecondarySubtitleId == id) {
                        clearSubtitleState(SubtitleSlot.Secondary, invalidateRequest = true)
                    }
                }
                logger.fInfo { "Load ${slot.debugName} subtitle $subtitleName success" }
                addLogs("加载${slot.logPrefix}字幕 $subtitleName 成功，数量: ${subtitleData.size}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isCurrentSubtitleLoad(slot, requestToken)) {
                        return@withContext
                    }
                    clearSubtitleState(slot)
                    if (slot == SubtitleSlot.Primary) {
                        clearSubtitleState(SubtitleSlot.Secondary, invalidateRequest = true)
                    }
                }
                logger.fInfo { "Load ${slot.debugName} subtitle failed: ${e.stackTraceToString()}" }
                addLogs("加载${slot.logPrefix}字幕 $subtitleName 失败: ${e.localizedMessage}")
            }
        }
    }

    private fun String.replaceUrlDomainWithAliCdn(): String {
        val replaceDomainKeywords = listOf(
            "mirroraliov",
            "mirrorakam"
        )
        if (replaceDomainKeywords.none { this.contains(it) }) return this

        return Uri.parse(this)
            .buildUpon()
            .authority("upos-sz-mirrorali.bilivideo.com")
            .build()
            .toString()
    }

    private fun selectOfficialCdnUrl(urls: List<String>): String {
        if (urls.isEmpty()) {
            logger.fInfo { "doesn't find any url, select a random url" }
            return urls.randomOrNull() ?: ""
        }

        // 判定是否为“官方” CDN 的简单规则，和之前逻辑保持一致
        val isOfficialCdn: (String) -> Boolean = {
            !it.contains(".mcdn.bilivideo.") &&
            !it.contains(".szbdyd.com") &&
            !Regex("^(https?://)?(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(:\\d{1,5})?)(/[a-zA-Z0-9_./-]*)?(\\?.*)?$")
                .matches(it)
        }

        if (!Prefs.preferOfficialCdn) {
            // 当用户不偏好官方 CDN 时，使用加权随机：官方权重 0.8，非官方权重 1.2（基准为 1）
            logger.fInfo { "doesn't need to filter official cdn url, select a weighted random url (favor non-official)" }

            val weights = urls.map { url -> if (isOfficialCdn(url)) 0.8 else 1.2 }
            val total = weights.sum()
            // 如果权重计算异常，退回随机
            if (total <= 0.0) return urls.randomOrNull() ?: ""

            val r = kotlin.random.Random.Default.nextDouble() * total
            var acc = 0.0
            for (i in urls.indices) {
                acc += weights[i]
                if (r <= acc) return urls[i]
            }
            return urls.randomOrNull() ?: ""
        }

        val filteredUrls = urls.filter{isOfficialCdn(it)}
        if (filteredUrls.isEmpty()) {
            logger.fInfo { "doesn't find any official cdn url, select a random url" }
            return urls.random()
        } else {
            logger.fInfo { "filtered official cdn urls: $filteredUrls" }
            return filteredUrls.random()
        }
    }

    private fun applyPreparedAutoPlayTransitionContext(transitionContext: PreparedAutoPlayTransitionContext) {
        videoInfoRepository.replacePlaybackContext(
            videoList = transitionContext.availableVideoList,
            relatedVideos = transitionContext.relatedVideos,
            interactivePlaybackContext = transitionContext.interactivePlaybackContext,
        )
        title = transitionContext.title
        partTitle = transitionContext.partTitle
        fromSeason = false
        subType = 0
        epid = transitionContext.epid ?: 0
        seasonId = transitionContext.seasonId ?: 0
        isVerticalVideo = transitionContext.isVerticalVideo
        play = transitionContext.play
        danmaku = transitionContext.danmaku
        like = transitionContext.like
        coin = transitionContext.coin
        favorite = transitionContext.favorite
        upName = transitionContext.upName
        upId = transitionContext.upId
        upFace = transitionContext.upFace
        pubTime = transitionContext.pubTime
        playerIconIdle = transitionContext.playerIconIdle
        playerIconMoving = transitionContext.playerIconMoving
        showRelatedVideos = false
    }

    private suspend fun resolvePreparedAutoPlayTransitionContext(
        candidate: AutoPlayCandidate,
    ): PreparedAutoPlayTransitionContext? {
        val detail = when (candidate) {
            is AutoPlayCandidate.CrossVideoPart -> videoDetailRepository.getVideoDetail(
                aid = candidate.item.aid,
                preferApiType = Prefs.apiType,
                withUserActions = false,
            )

            is AutoPlayCandidate.RelatedVideo -> videoDetailRepository.getVideoDetail(
                aid = candidate.video.avid,
                preferApiType = Prefs.apiType,
                withUserActions = false,
            )
        }

        if (detail.redirectToEp || detail.isInteractive || detail.pages.isEmpty()) return null

        val targetCid = when (candidate) {
            is AutoPlayCandidate.CrossVideoPart -> candidate.item.cid ?: return null
            is AutoPlayCandidate.RelatedVideo -> detail.pages.first().cid
        }
        val preferredPartTitle = when (candidate) {
            is AutoPlayCandidate.CrossVideoPart -> candidate.item.partTitle.takeIf { it.isNotBlank() }
            is AutoPlayCandidate.RelatedVideo -> null
        }

        return detail.toPreparedAutoPlayTransitionContext(
            targetCid = targetCid,
            preferredPartTitle = preferredPartTitle,
        )
    }

    private suspend fun awaitPreparedAutoPlayTransitionContext(
        candidate: AutoPlayCandidate,
        prepareToken: Long,
    ): PreparedAutoPlayTransitionContext? {
        val currentPreparedTarget = preparedAutoPlayTarget
        if (currentPreparedTarget?.candidate == candidate) {
            if (!currentPreparedTarget.isSupported) return null
            currentPreparedTarget.transitionContext?.let { return it }
        }

        val prepareJob = autoPlayPrepareJob
        if (prepareJob != null && prepareJob.isActive && isPreparedAutoPlayCandidateActive(candidate, prepareToken)) {
            withTimeoutOrNull(AUTO_PLAY_PREPARE_WAIT_MS) {
                prepareJob.join()
            }
        }

        val updatedPreparedTarget = preparedAutoPlayTarget
        if (
            !isPreparedAutoPlayCandidateActive(candidate, prepareToken) ||
            updatedPreparedTarget?.candidate != candidate ||
            !updatedPreparedTarget.isSupported
        ) {
            return null
        }
        return updatedPreparedTarget.transitionContext
    }

    private suspend fun awaitPreparedAutoPlayTarget(
        candidate: AutoPlayCandidate,
        prepareToken: Long,
    ): PreparedAutoPlayTarget? {
        val currentPreparedTarget = preparedAutoPlayTarget
        if (currentPreparedTarget?.candidate == candidate) {
            if (!currentPreparedTarget.isSupported || hasFreshPreparedAutoPlayPlayData(currentPreparedTarget)) {
                return currentPreparedTarget
            }
        }

        val prefetchJob = autoPlayPrefetchJob
        if (prefetchJob != null && prefetchJob.isActive && isPreparedAutoPlayCandidateActive(candidate, prepareToken)) {
            withTimeoutOrNull(AUTO_PLAY_PREPARE_WAIT_MS) {
                prefetchJob.join()
            }
        }

        val updatedPreparedTarget = preparedAutoPlayTarget
        if (!isPreparedAutoPlayCandidateActive(candidate, prepareToken) || updatedPreparedTarget?.candidate != candidate) {
            return null
        }
        return updatedPreparedTarget
    }

    private fun isPreparedAutoPlayCandidateActive(
        candidate: AutoPlayCandidate,
        prepareToken: Long,
    ): Boolean {
        return autoPlayPrepareToken == prepareToken && preparedAutoPlayTarget?.candidate == candidate
    }

    private fun hasFreshPreparedAutoPlayPlayData(target: PreparedAutoPlayTarget): Boolean {
        if (target.playData == null || target.playDataFetchedAtMs <= 0L) return false
        return System.currentTimeMillis() - target.playDataFetchedAtMs <= PREPARED_AUTO_PLAY_PLAY_DATA_TTL_MS
    }

    private suspend fun markPreparedAutoPlayTargetUnsupported(
        candidate: AutoPlayCandidate,
        prepareToken: Long,
    ) {
        withContext(Dispatchers.Main) {
            if (!isPreparedAutoPlayCandidateActive(candidate, prepareToken)) return@withContext
            preparedAutoPlayTarget = preparedAutoPlayTarget?.copy(isSupported = false)
        }
    }

    private fun invalidatePreparedAutoPlayTarget() {
        autoPlayPrepareJob?.cancel()
        autoPlayPrepareJob = null
        autoPlayPrefetchJob?.cancel()
        autoPlayPrefetchJob = null
        autoPlayPrepareToken += 1
        preparedAutoPlayTarget = null
    }

    private fun beginVodPlaybackSession(): Long {
        vodPlaybackSessionToken += 1
        cancelVodPlayUrlAutoRefresh()
        return vodPlaybackSessionToken
    }

    private fun isVodPlaybackSessionActive(token: Long): Boolean {
        return token == vodPlaybackSessionToken
    }

    private fun ensureVodPlaybackSessionActive(token: Long) {
        if (!isVodPlaybackSessionActive(token)) {
            throw kotlinx.coroutines.CancellationException("Stale vod playback session")
        }
    }

    private fun cancelVodPlayUrlAutoRefresh() {
        vodPlayUrlRefreshJob?.cancel()
        vodPlayUrlRefreshJob = null
        vodPlayUrlRefreshToken += 1
    }

    private fun scheduleVodPlayUrlAutoRefresh(
        expiresAtMs: Long,
        playbackSessionToken: Long = vodPlaybackSessionToken,
    ) {
        vodPlayUrlRefreshJob?.cancel()

        if (isLive || expiresAtMs <= 0L || !isVodPlaybackSessionActive(playbackSessionToken)) {
            logger.fDebug {
                "Skip vod URL refresh scheduling: isLive=$isLive, expiresAtMs=$expiresAtMs, sessionActive=${isVodPlaybackSessionActive(playbackSessionToken)}"
            }
            return
        }

        val refreshDelay = (expiresAtMs - System.currentTimeMillis() - REFRESH_BEFORE_EXPIRY_MS)
            .coerceAtLeast(MIN_REFRESH_INTERVAL_MS)
        val refreshToken = ++vodPlayUrlRefreshToken

        logger.fInfo { "Scheduling vod URL refresh in ${refreshDelay}ms (expires at $expiresAtMs)" }

        vodPlayUrlRefreshJob = viewModelScope.launch(Dispatchers.IO) {
            delay(refreshDelay)
            if (refreshToken != vodPlayUrlRefreshToken || !isVodPlaybackSessionActive(playbackSessionToken)) {
                return@launch
            }
            reloadVodPlayUrl(playbackSessionToken)
        }
    }

    private fun scheduleVodPlayUrlRefreshRetry(
        playbackSessionToken: Long = vodPlaybackSessionToken,
    ) {
        vodPlayUrlRefreshJob?.cancel()
        if (isLive || !isVodPlaybackSessionActive(playbackSessionToken)) {
            return
        }

        val refreshToken = ++vodPlayUrlRefreshToken
        logger.fWarn { "Scheduling vod URL refresh retry in ${REFRESH_RETRY_INTERVAL_MS}ms" }
        vodPlayUrlRefreshJob = viewModelScope.launch(Dispatchers.IO) {
            delay(REFRESH_RETRY_INTERVAL_MS)
            if (refreshToken != vodPlayUrlRefreshToken || !isVodPlaybackSessionActive(playbackSessionToken)) {
                return@launch
            }
            reloadVodPlayUrl(playbackSessionToken)
        }
    }

    private suspend fun reloadVodPlayUrl(
        playbackSessionToken: Long = vodPlaybackSessionToken,
    ) {
        if (isLive || currentAid <= 0L || currentCid <= 0L || !isVodPlaybackSessionActive(playbackSessionToken)) {
            return
        }

        val resumePositionMs = withContext(Dispatchers.Main) {
            videoPlayer?.currentPosition?.takeIf { it > 0L }
        }

        logger.fInfo {
            "Refreshing vod play url: aid=$currentAid, cid=$currentCid, epid=$currentEpid, resumePositionMs=$resumePositionMs"
        }
        addLogs("播放地址即将过期，正在刷新")

        val refreshed = loadPlayUrl(
            avid = currentAid,
            cid = currentCid,
            epid = currentEpid,
            preferApi = Prefs.apiType,
            proxyArea = proxyArea,
            initialSeekPositionMs = resumePositionMs,
            targetQuality = currentQuality,
            targetVideoCodec = currentVideoCodec,
            targetAudio = currentAudio,
            targetMediaMode = currentPlaybackMediaMode,
            playbackSessionToken = playbackSessionToken
        )

        if (refreshed) {
            addLogs("播放地址刷新成功")
        } else if (isVodPlaybackSessionActive(playbackSessionToken)) {
            addLogs("播放地址刷新失败，稍后重试")
            scheduleVodPlayUrlRefreshRetry(playbackSessionToken)
        }
    }

    private fun pickEarliestUrlExpiryEpochMs(vararg urls: String?): Long {
        return urls.mapNotNull { url ->
            url?.takeIf { it.isNotBlank() }?.let(::parseUrlExpiryEpochMs)
                ?.takeIf { it > 0L }
        }.minOrNull() ?: 0L
    }

    private fun parseUrlExpiryEpochMs(url: String): Long {
        val rawValue = runCatching {
            val parsedUri = Uri.parse(url)
            parsedUri.getQueryParameter("deadline")
                ?: parsedUri.getQueryParameter("expires")
        }.getOrNull() ?: Regex("""[?&](?:deadline|expires)=(\\d+)""")
            .find(url)
            ?.groupValues
            ?.getOrNull(1)

        val epochValue = rawValue?.toLongOrNull() ?: return 0L
        return if (epochValue >= 10_000_000_000L) epochValue else epochValue * 1000L
    }

    private suspend fun updateDanmakuMask() {
        // 直播模式不获取蒙版数据
        if (isLive) return

        if (!currentDanmakuMask) return
        if (hasResolvedCurrentDanmakuMask()) return

        val targetAid = currentAid
        val targetCid = currentCid

        runCatching {
            val masks = videoPlayRepository.getDanmakuMask(
                aid = targetAid,
                cid = targetCid,
                preferApiType = Prefs.apiType
            )

            if (targetAid != currentAid || targetCid != currentCid) {
                return@runCatching
            }

            danmakuMasks.swapListWithMainContext(masks)
            resolvedDanmakuMaskAid = targetAid
            resolvedDanmakuMaskCid = targetCid
            hasResolvedDanmakuMask = true
            logger.fInfo { "Load danmaku mask size: ${danmakuMasks.size}" }
        }.onFailure {
            if (targetAid == currentAid && targetCid == currentCid) {
                hasResolvedDanmakuMask = false
                resolvedDanmakuMaskAid = 0L
                resolvedDanmakuMaskCid = 0L
            }
            logger.fWarn { "Load danmaku mask failed: ${it.stackTraceToString()}" }
        }
    }

    private suspend fun updateVideoShot() {
        withContext(Dispatchers.Main) { videoShot = null }
        runCatching {
            val videoShot = videoPlayRepository.getVideoShot(
                aid = currentAid,
                cid = currentCid,
                preferApiType = Prefs.apiType
            )
            withContext(Dispatchers.Main) { this@VideoPlayerV3ViewModel.videoShot = videoShot }
            logger.fInfo { "Load video shot success" }
        }.onFailure {
            logger.fWarn { "Load video shot failed: ${it.stackTraceToString()}" }
        }
    }

    fun playNextVideo() {
        logger.fInfo { "Video finished" }
        when (currentPlayMode) {
            PlayMode.Single -> {
                logger.info { "Play mode: $currentPlayMode, do nothing" }
            }

            PlayMode.Sequential -> {
                logger.info { "Play mode: $currentPlayMode, play next video in list" }
                playNextVideoInList()
            }

            PlayMode.SingleLoop -> {
                logger.info { "Play mode: $currentPlayMode, replay current video" }
                danmakuPlayer?.seekTo(0L)
                danmakuPlayer?.pause()
                videoPlayer?.seekTo(0L)
            }

            PlayMode.ListLoop -> {
                logger.info { "Play mode: $currentPlayMode, play next video in list or loop to first" }
                playNextVideoInList(loop = true)
            }
        }
    }

    private fun playNextVideoInList(loop: Boolean = false) {
        if (isInteractivePlayback || availableVideoList.any { it is VideoListInteractiveNode }) {
            logger.info { "Interactive branches detected, skip auto playing next branch" }
            return
        }
        val currentIndex = availableVideoList
            .indexOfFirst {
                when (it) {
                    is VideoListItemData -> it.cid == currentCid
                    else -> false
                }
            }
        if (currentIndex + 1 < availableVideoList.size) {
            val nextVideos = availableVideoList.subList(
                currentIndex + 1,
                availableVideoList.size
            )
            val nextVideo =
                nextVideos.firstOrNull { it is VideoListItemData }!! as VideoListItemData
            logger.info { "Play next video: $nextVideo" }
            partTitle = nextVideo.title
            loadPlayUrl(
                avid = nextVideo.aid,
                cid = nextVideo.cid!!,
                epid = nextVideo.epid,
                seasonId = nextVideo.seasonId,
                continuePlayNext = true
            )
        } else if (loop) {
            //loop to first
            val firstVideo =
                availableVideoList.firstOrNull { it is VideoListItemData }!! as VideoListItemData
            logger.info { "Loop to first video: $firstVideo" }
            partTitle = firstVideo.title
            loadPlayUrl(
                avid = firstVideo.aid,
                cid = firstVideo.cid!!,
                epid = firstVideo.epid,
                seasonId = firstVideo.seasonId,
                continuePlayNext = true
            )
        }
    }
    
    /**
     * 加载直播流（带画质信息）
     * @param roomId 直播间ID
     * @param qn 请求的画质编号，默认使用用户配置的直播清晰度
     */
    fun loadLiveStreamWithQuality(roomId: Int, qn: Int = Prefs.defaultLiveQn) {
        cancelVodPlayUrlAutoRefresh()
        // 取消之前的重连任务
        liveRetryJob?.cancel()
        liveRetryJob = null
        // 取消之前的URL刷新任务
        liveUrlRefreshJob?.cancel()
        liveUrlRefreshJob = null
        // 重置刷新失败计数
        consecutiveRefreshFailures = 0

        viewModelScope.launch(Dispatchers.IO) {
            logger.fInfo { "Load live stream with quality: roomId=$roomId, qn=$qn" }
            withContext(Dispatchers.Main) { loadState = RequestState.Doing }

            // 仅在首次加载时初始化弹幕播放器，画质切换时不重复创建
            if (danmakuPlayer == null) {
                ensureDanmakuPlayer(isLive = true)
            }

            val playInfo = fetchResolvedLivePlayInfo(roomId, qn, currentLiveCodec)
            if (playInfo == null) {
                withContext(Dispatchers.Main) {
                    loadState = RequestState.Failed
                    errorMessage = "获取直播流失败"
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                liveStreamUrl = playInfo.streamUrl
                liveStreamExpiresAt = playInfo.expiresAt
                currentLiveQn = playInfo.currentQn
                liveQnDescMap = playInfo.qnDescMap

                // 更新可用画质列表（按 qn 降序，即最高画质在前）
                val qualities = playInfo.acceptQn
                    .sortedDescending()
                    .map { qualityQn ->
                        qualityQn to (playInfo.qnDescMap[qualityQn] ?: "未知画质 $qualityQn")
                    }
                availableLiveQualities.clear()
                availableLiveQualities.addAll(qualities)

                currentLiveQualityDescription = playInfo.qnDescMap[playInfo.currentQn] ?: "未知画质"
                logger.fInfo { "Live quality: current=${playInfo.currentQn} ($currentLiveQualityDescription), available=$qualities" }
                logger.fDebug { "Live stream URL expires at: ${playInfo.expiresAt}" }
            }

            runCatching {
                withContext(Dispatchers.Main) {
                    videoPlayer?.playUrl(videoUrl = playInfo.streamUrl)
                    videoPlayer?.prepare()
                    videoPlayer?.start()
                    loadState = RequestState.Success
                }
                logger.fInfo { "Live stream loaded successfully with quality ${playInfo.currentQn}" }
                ensureLiveRoomEntryReported(roomId)
                // 播放成功后自动启动直播弹幕（仅首次加载，画质切换时不重启弹幕）
                if (liveWebSocket == null) {
                    startLiveDanmaku(roomId)
                }
                // 调度URL刷新
                scheduleLiveUrlRefresh()
            }.onFailure { e ->
                logger.fError { "Failed to load live stream: ${e.message}" }
                withContext(Dispatchers.Main) {
                    loadState = RequestState.Failed
                    errorMessage = "加载直播流失败: ${e.message}"
                }
            }
        }
    }

    /**
     * 切换直播画质
     * @param qn 目标画质编号
     */
    fun changeLiveQuality(qn: Int) {
        logger.fInfo { "Change live quality to: $qn" }
        loadLiveStreamWithQuality(liveRoomId, qn)
    }

    /**
     * 切换直播编码格式
     * @param codec 目标编码格式
     */
    fun changeLiveCodec(codec: LiveCodec) {
        logger.fInfo { "Change live codec to: $codec" }
        currentLiveCodec = codec
        loadLiveStreamWithQuality(liveRoomId, currentLiveQn)
    }

    /**
     * 直播流错误时自动重连
     * 延迟 2 秒后重新获取直播流 URL 并播放。
     * 使用 liveRetryJob 做防抖：新的重连请求会取消上一次未执行的延迟重试。
     * 当直播间已关闭（liveStatus != 1）时，fetchLiveStreamUrl 返回 null，自动停止重试。
     */
    fun retryLiveStream() {
        if (!isLive) return
        logger.fInfo { "Scheduling live stream retry in 2s for room $liveRoomId" }

        // 防抖：取消上一次待执行的重试
        liveRetryJob?.cancel()
        // 取消URL刷新任务
        liveUrlRefreshJob?.cancel()
        liveUrlRefreshJob = null

        liveRetryJob = viewModelScope.launch(Dispatchers.IO) {
            delay(2000)
            // 仅在播放器未在播放时重试
            val playing = withContext(Dispatchers.Main) { videoPlayer?.isPlaying == true }
            if (playing) {
                logger.fInfo { "Player is already playing, skip retry" }
                return@launch
            }
            logger.fInfo { "Retrying live stream for room $liveRoomId, qn=$currentLiveQn" }
            withContext(Dispatchers.Main) {
                loadState = RequestState.Doing
                // 重连时先清除错误状态，让 UI 不再显示错误
                errorMessage = ""
            }
            val playInfo = fetchResolvedLivePlayInfo(liveRoomId, currentLiveQn, currentLiveCodec)
            if (playInfo == null) {
                // fetchLiveStreamUrl 内部已判断 liveStatus != 1 并 Toast "主播未开播"
                // 此时不再继续重试
                logger.fInfo { "Live stream fetch returned null, live may have ended" }
                withContext(Dispatchers.Main) {
                    loadState = RequestState.Failed
                    errorMessage = "直播已结束或获取直播流失败"
                }
                return@launch
            }
            // 成功获取新 URL，重新播放
            withContext(Dispatchers.Main) {
                liveStreamUrl = playInfo.streamUrl
                liveStreamExpiresAt = playInfo.expiresAt
                currentLiveQn = playInfo.currentQn
                videoPlayer?.playUrl(videoUrl = playInfo.streamUrl)
                videoPlayer?.prepare()
                videoPlayer?.start()
                loadState = RequestState.Success
            }
            logger.fInfo { "Live stream retry successful, new URL loaded" }
            // 重置刷新失败计数并重新调度刷新
            consecutiveRefreshFailures = 0
            scheduleLiveUrlRefresh()
        }
    }

    /**
     * 调度直播流URL的主动刷新
     * 在URL过期前REFRESH_BEFORE_EXPIRY_MS毫秒自动刷新
     */
    private fun scheduleLiveUrlRefresh() {
        // 取消之前的刷新任务
        liveUrlRefreshJob?.cancel()

        if (!isLive || liveStreamExpiresAt <= 0) {
            logger.fDebug { "No need to schedule refresh: isLive=$isLive, expiresAt=$liveStreamExpiresAt" }
            return
        }

        val now = System.currentTimeMillis()
        val timeUntilExpiry = liveStreamExpiresAt - now
        val refreshDelay = (timeUntilExpiry - REFRESH_BEFORE_EXPIRY_MS)
            .coerceAtLeast(MIN_REFRESH_INTERVAL_MS)

        logger.fInfo { "Scheduling live URL refresh in ${refreshDelay}ms (expires at $liveStreamExpiresAt)" }

        liveUrlRefreshJob = viewModelScope.launch(Dispatchers.IO) {
            delay(refreshDelay)
            refreshLiveStreamUrl()
        }
    }

    /**
     * 刷新直播流URL（无缝切换）
     */
    private suspend fun refreshLiveStreamUrl() {
        if (!isLive) return

        logger.fInfo { "Refreshing live stream URL for room $liveRoomId" }

        try {
            val playInfo = fetchResolvedLivePlayInfo(
                liveRoomId,
                currentLiveQn,
                currentLiveCodec
            )

            if (playInfo == null) {
                // 刷新失败，可能是直播已结束
                consecutiveRefreshFailures++
                logger.fWarn { "Failed to refresh live URL (attempt $consecutiveRefreshFailures), live may have ended" }

                if (consecutiveRefreshFailures >= MAX_REFRESH_FAILURES) {
                    // 多次失败，可能直播已结束，停止刷新
                    logger.fWarn { "Max refresh failures reached, stopping refresh" }
                    withContext(Dispatchers.Main) {
                        loadState = RequestState.Failed
                        errorMessage = "直播可能已结束"
                    }
                    return
                }

                // 如果直播未结束但刷新失败，稍后重试
                delay(REFRESH_RETRY_INTERVAL_MS)
                scheduleLiveUrlRefresh()
                return
            }

            // 重置失败计数
            consecutiveRefreshFailures = 0

            // 更新URL和过期时间
            withContext(Dispatchers.Main) {
                liveStreamUrl = playInfo.streamUrl
                liveStreamExpiresAt = playInfo.expiresAt
                currentLiveQn = playInfo.currentQn
            }

            // 无缝切换：更新播放器URL
            withContext(Dispatchers.Main) {
                videoPlayer?.playUrl(videoUrl = playInfo.streamUrl)
            }

            logger.fInfo { "Live URL refreshed successfully, new expiresAt=$liveStreamExpiresAt" }

            // 调度下一次刷新
            scheduleLiveUrlRefresh()
        } catch (e: Exception) {
            logger.fError { "Error refreshing live URL: ${e.message}" }
            consecutiveRefreshFailures++
            if (consecutiveRefreshFailures < MAX_REFRESH_FAILURES) {
                delay(REFRESH_RETRY_INTERVAL_MS)
                scheduleLiveUrlRefresh()
            }
        }
    }

    private suspend fun fetchResolvedLivePlayInfo(
        roomId: Int,
        preferredQn: Int,
        codec: LiveCodec
    ): dev.aaa1115910.bv.util.LivePlayInfo? {
        val initialPlayInfo = LiveStreamUrlFetcher.fetchLiveStreamUrl(roomId, preferredQn, codec)
            ?: return null
        val resolvedQn = LiveQualityPreference.resolveRequestedQn(preferredQn, initialPlayInfo.acceptQn)

        if (resolvedQn == initialPlayInfo.currentQn) {
            return initialPlayInfo
        }

        logger.fInfo {
            "Preferred live quality $preferredQn unavailable or downgraded, retry with resolved qn=$resolvedQn"
        }
        return LiveStreamUrlFetcher.fetchLiveStreamUrl(roomId, resolvedQn, codec) ?: initialPlayInfo
    }

    /**
     * 加载直播流
     */
    fun loadLiveStream(streamUrl: String) {
        cancelVodPlayUrlAutoRefresh()
        viewModelScope.launch(Dispatchers.IO) {
            logger.fInfo { "Load live stream: $streamUrl" }
            withContext(Dispatchers.Main) { loadState = RequestState.Doing }
            
            // 初始化弹幕播放器
            ensureDanmakuPlayer()
            logger.fInfo { "Danmaku player initialized for live stream" }
            
            runCatching {
                withContext(Dispatchers.Main) {
                    videoPlayer?.playUrl(videoUrl = streamUrl)
                    videoPlayer?.prepare()
                    videoPlayer?.start()
                    loadState = RequestState.Success
                }
                logger.fInfo { "Live stream loaded successfully" }
            }.onFailure { e ->
                logger.fError { "Failed to load live stream: ${e.message}" }
                withContext(Dispatchers.Main) {
                    loadState = RequestState.Failed
                    errorMessage = "加载直播流失败: ${e.message}"
                }
            }
        }
    }
    
    /**
     * 启动直播弹幕
     */
    fun startLiveDanmaku(roomId: Int) {
        if (roomId <= 0) {
            logger.fWarn { "Invalid room id: $roomId" }
            return
        }
        
        logger.fInfo { "Starting live danmaku for room $roomId" }
        stopLiveDanmaku()
        
        // 连接 WebSocket
        liveWebSocket = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                logger.fInfo { "Getting live danmaku info for room $roomId" }
                val danmuInfo = BiliLiveHttpApi.getLiveDanmuInfo(roomId, sessData = Prefs.sessData)
                logger.fInfo { "Danmaku info response: code=${danmuInfo.code}, message=${danmuInfo.message}" }
                
                if (danmuInfo.data == null) {
                    logger.fError { "Failed to get danmaku info: data is null" }
                    return@launch
                }
                
                logger.fInfo { "Getting live room play info for room $roomId" }
                val playInfo = BiliLiveHttpApi.getLiveRoomPlayInfo(roomId)
                logger.fInfo { "Play info response: code=${playInfo.code}, message=${playInfo.message}" }
                
                val realRoomId = playInfo.data?.roomId
                if (realRoomId == null) {
                    logger.fError { "Failed to get real room id: data.roomId is null" }
                    return@launch
                }
                
                logger.fInfo { "Real room id: $realRoomId, starting WebSocket connection" }
                
                // 创建 Channel 和单消费者协程，避免每条弹幕创建一个协程
                val channel = Channel<DanmakuEvent>(capacity = Channel.BUFFERED)
                liveDanmakuChannel = channel
                liveDanmakuConsumer = viewModelScope.launch(Dispatchers.IO) {
                    for (event in channel) {
                        addLiveDanmaku(event)
                    }
                }
                
                logger.fInfo { "Connecting to live danmaku WebSocket for room $realRoomId" }
                // 使用预取的 token 和 hostList，避免 connectLiveEvent 内部重复调用 API
                liveWebSocketInner = LiveDataWebSocket.connectLiveEvent(
                    realRoomId = realRoomId,
                    token = danmuInfo.data!!.token,
                    hostList = danmuInfo.data!!.hostList,
                    uid = Prefs.uid,
                    parseEmoji = Prefs.showLiveDanmakuEmoji,
                ) { event ->
                    when (event) {
                        is DanmakuEvent -> channel.trySend(event)
                        is PopularityChangeEvent -> {
                            val now = System.currentTimeMillis()
                            if (now - lastPopularityUpdateTime >= 10_000) {
                                livePopularityText = event.popularityText
                                lastPopularityUpdateTime = now
                            }
                        }
                        is OnlineRankCountEvent -> {
                            val now = System.currentTimeMillis()
                            if (now - lastOnlineCountUpdateTime >= 10_000) {
                                liveOnlineCount = "${event.count} 高能观众"
                                lastOnlineCountUpdateTime = now
                            }
                        }
                    }
                }
            }.onFailure { e ->
                logger.fError { "Live danmaku connection failed: ${e.message}\n${e.stackTraceToString()}" }
            }
        }

        logger.fInfo { "Live danmaku started" }
    }
    
    /**
     * 停止直播弹幕
     */
    fun stopLiveDanmaku() {
        logger.fInfo { "Stopping live danmaku" }

        liveWebSocket?.cancel()
        liveWebSocket = null
        liveWebSocketInner?.cancel()
        liveWebSocketInner = null
        liveDanmakuChannel?.close()
        liveDanmakuChannel = null
        liveDanmakuConsumer?.cancel()
        liveDanmakuConsumer = null

        logger.fInfo { "Live danmaku stopped" }
    }
    
    /**
     * 实时发送直播弹幕
     */
    private fun addLiveDanmaku(event: DanmakuEvent) {
        // 添加用户等级过滤逻辑
        if (event.userLevel < currentLiveDanmakuFilterLevel) {
            logger.fInfo { "Filtered live danmaku: userLevel=${event.userLevel} < $currentLiveDanmakuFilterLevel" }
            return
        }

        if (!showDanmaku) {
            return
        }

        val danmakuItem = DanmakuItemData(
            danmakuId = System.currentTimeMillis(),
            position = 0L,
            content = event.content,
            mode = when (event.mode) {
                4 -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                5 -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                else -> DanmakuItemData.DANMAKU_MODE_ROLLING
            },
            textSize = event.fontSize,
            textColor = Color(event.color).toArgb(),
            emojiMap = event.emojiMap
        )

        // 直播模式
        viewModelScope.launch(Dispatchers.Main) {
            liveDanmakuPlayer?.emit(danmakuItem)
        }
    }
}
