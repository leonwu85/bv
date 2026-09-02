package dev.aaa1115910.bv.viewmodel

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.os.SystemClock
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
import com.kuaishou.akdanmaku.data.DanmakuItem
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ext.RETAINER_BILIBILI
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import com.kuaishou.akdanmaku.ui.LiveDanmakuPlayer
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.PlayData
import dev.aaa1115910.biliapi.entity.PlayDataUnavailableException
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMaskSegment
import dev.aaa1115910.biliapi.http.entity.video.ClipInfo
import dev.aaa1115910.biliapi.entity.video.HeartbeatVideoType
import dev.aaa1115910.biliapi.entity.video.InteractiveNode
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.entity.video.SubtitleAiStatus
import dev.aaa1115910.biliapi.entity.video.SubtitleAiType
import dev.aaa1115910.biliapi.entity.video.SubtitleType
import dev.aaa1115910.biliapi.entity.video.VideoPage
import dev.aaa1115910.biliapi.entity.video.VideoShot
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.BiliLiveHttpApi
import dev.aaa1115910.biliapi.http.entity.VVoucherException
import dev.aaa1115910.biliapi.http.entity.danmaku.DanmakuData
import dev.aaa1115910.biliapi.http.entity.live.DanmakuEvent
import dev.aaa1115910.biliapi.http.entity.live.LiveEvent
import dev.aaa1115910.biliapi.http.entity.live.LiveEmotePackage
import dev.aaa1115910.biliapi.http.entity.live.OnlineRankCountEvent
import dev.aaa1115910.biliapi.http.entity.live.PopularityChangeEvent
import dev.aaa1115910.biliapi.entity.sponsorblock.SponsorSegment
import dev.aaa1115910.biliapi.http.SponsorBlockHttpApi
import dev.aaa1115910.biliapi.repositories.AuthRepository
import dev.aaa1115910.biliapi.repositories.LiveRepository
import dev.aaa1115910.biliapi.repositories.VideoDetailRepository
import dev.aaa1115910.biliapi.repositories.VideoPlayRepository
import dev.aaa1115910.biliapi.util.AvBvConverter
import dev.aaa1115910.biliapi.websocket.LiveDataWebSocket
import dev.aaa1115910.bilisubtitle.SubtitleParser
import dev.aaa1115910.bilisubtitle.entity.SubtitleItem
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.entity.CdnService
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.entity.LiveQualityPreference
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.offline.OfflineVideoCacheEntry
import dev.aaa1115910.bv.offline.OfflineVideoCacheService
import dev.aaa1115910.bv.offline.OfflineVideoCacheStatus
import dev.aaa1115910.bv.offline.OfflineVideoCacheTaskRequest
import dev.aaa1115910.bv.offline.OfflineVideoCacheTaskState
import dev.aaa1115910.bv.offline.OfflineVideoCacheTarget
import dev.aaa1115910.bv.offline.OfflineVideoPlaybackSource
import dev.aaa1115910.bv.player.autoplay.AutoPlayCandidate
import dev.aaa1115910.bv.player.autoplay.PreparedAutoPlayTarget
import dev.aaa1115910.bv.player.autoplay.PreparedAutoPlayTransitionContext
import dev.aaa1115910.bv.player.autoplay.toPreparedAutoPlayTransitionContext
import dev.aaa1115910.bv.player.renderer.OptimizedTextRenderer
import dev.aaa1115910.bv.player.renderer.SimpleRenderer
import dev.aaa1115910.bv.player.util.TvDanmakuCompatibilityPolicy
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
import dev.aaa1115910.bv.player.entity.LiveStreamLine
import dev.aaa1115910.bv.player.entity.VideoListInteractiveNode
import dev.aaa1115910.bv.player.entity.VideoListItemData
import dev.aaa1115910.bv.player.entity.VideoListUgcEpisode
import dev.aaa1115910.bv.player.entity.VideoPlayerViewPoint
import dev.aaa1115910.bv.player.entity.VideoRotation
import dev.aaa1115910.bv.player.entity.SponsorBlockSkipMode
import dev.aaa1115910.bv.repository.VideoInfoRepository
import dev.aaa1115910.bv.settings.PlayerSettingsProvider
import dev.aaa1115910.bv.util.DanmakuSegmentMergeResult
import dev.aaa1115910.bv.util.DanmakuSmartFilterPolicy
import dev.aaa1115910.bv.util.DeviceUtil
import dev.aaa1115910.bv.util.MergedDanmakuEntry
import dev.aaa1115910.bv.util.NetworkUtil
import dev.aaa1115910.bv.util.PlaybackPreferenceSelector
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.SubtitleLanguagePreference
import dev.aaa1115910.bv.util.VodDanmakuMergeState
import dev.aaa1115910.bv.util.VodDanmakuMerger
import dev.aaa1115910.bv.util.VodCdnSelection
import dev.aaa1115910.bv.util.VodCdnUrlSelector
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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.koin.core.annotation.KoinViewModel
import kotlin.math.abs
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean

data class LiveDanmakuMessage(
    val id: Long,
    val username: String,
    val content: String,
    val medalName: String?,
    val medalLevel: Int?,
    val userLevel: Int,
    val color: Int,
    val fontSize: Int,
    val timestampMs: Long,
    val emojiMap: Map<String, String>
)

private const val DANMAKU_BYTES_IN_MB = 1024L * 1024L
private const val DANMAKU_BYTES_IN_GB = 1024L * DANMAKU_BYTES_IN_MB
private const val DANMAKU_SEGMENT_DURATION_MS = 6 * 60 * 1000L
private const val DEFAULT_DANMAKU_MAX_SEGMENTS = 20
private const val GEETEST_REFRESH_SOURCE_MAX_ATTEMPTS = 4
private const val GEETEST_REFRESH_RETRY_BASE_DELAY_MILLIS = 250L
private const val GEETEST_REFRESH_FLIGHT_TTL_MILLIS = 120_000L

internal data class InitialDanmakuLoadPlan(
    val startPositionMs: Long,
    val startSegment: Int,
    val maxSegments: Int,
    val segmentOrder: List<Int>,
    val backgroundPrefetchSegments: List<Int>
)

internal fun resolveVodPlaybackStartPositionMs(
    explicitPositionMs: Long?,
    historyPositionMs: Long?,
    durationMs: Long
): Long {
    val requestedPositionMs = (explicitPositionMs ?: historyPositionMs ?: 0L).coerceAtLeast(0L)
    if (durationMs <= 0L) return requestedPositionMs

    return requestedPositionMs.coerceAtMost((durationMs - 1L).coerceAtLeast(0L))
}

internal fun calculateDanmakuSegment(positionMs: Long): Int =
    (positionMs.coerceAtLeast(0L) / DANMAKU_SEGMENT_DURATION_MS + 1L)
        .coerceAtMost(Int.MAX_VALUE.toLong())
        .toInt()

internal fun calculateDanmakuMaxSegments(
    durationMs: Long,
    minimumPositionMs: Long = 0L
): Int {
    val minimumSegment = calculateDanmakuSegment(minimumPositionMs)
    val durationSegments = if (durationMs > 0L) {
        kotlin.math.ceil(durationMs / DANMAKU_SEGMENT_DURATION_MS.toDouble()).toInt().coerceAtLeast(1)
    } else {
        DEFAULT_DANMAKU_MAX_SEGMENTS
    }
    val minimumCoverageSegments = if (durationMs <= 0L && minimumSegment < Int.MAX_VALUE) {
        minimumSegment + 1
    } else {
        minimumSegment
    }
    return maxOf(durationSegments, minimumCoverageSegments)
}

internal fun buildDanmakuSegmentOrder(startSegment: Int, maxSegments: Int): List<Int> {
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

internal fun buildInitialDanmakuLoadPlan(
    requestedPositionMs: Long,
    durationMs: Long,
    initialSegmentPrefetch: Int
): InitialDanmakuLoadPlan {
    val startPositionMs = resolveVodPlaybackStartPositionMs(
        explicitPositionMs = requestedPositionMs,
        historyPositionMs = null,
        durationMs = durationMs
    )
    val maxSegments = calculateDanmakuMaxSegments(durationMs, startPositionMs)
    val startSegment = calculateDanmakuSegment(startPositionMs).coerceIn(1, maxSegments)
    val segmentOrder = buildDanmakuSegmentOrder(startSegment, maxSegments)
    val backgroundPrefetchSegments = segmentOrder.asSequence()
        .filter { it > startSegment }
        .take((initialSegmentPrefetch - 1).coerceAtLeast(0))
        .toList()
    return InitialDanmakuLoadPlan(
        startPositionMs = startPositionMs,
        startSegment = startSegment,
        maxSegments = maxSegments,
        segmentOrder = segmentOrder,
        backgroundPrefetchSegments = backgroundPrefetchSegments
    )
}

internal enum class DanmakuPreloadMode {
    WINDOW_ONLY,
    FULL_VIDEO
}

internal data class DanmakuPreloadPolicy(
    val mode: DanmakuPreloadMode,
    val cacheRadius: Int,
    val maxCachedSegments: Int,
    val maxRetainedDanmakuItems: Int,
    val maxLoadedDanmakuIds: Int,
    val minFreeHeapBytesForFullPreload: Long,
    val minAvailableMemoryBytesForFullPreload: Long
) {
    val fullVideoPreloadEnabled: Boolean
        get() = mode == DanmakuPreloadMode.FULL_VIDEO
}

internal fun resolveDanmakuPreloadPolicy(
    totalMemoryBytes: Long,
    availableMemoryBytes: Long,
    isLowRamDevice: Boolean,
    maxHeapBytes: Long
): DanmakuPreloadPolicy {
    val lowMemoryDevice = isLowRamDevice ||
        (totalMemoryBytes > 0L && totalMemoryBytes < 3L * DANMAKU_BYTES_IN_GB) ||
        (availableMemoryBytes > 0L && availableMemoryBytes < 512L * DANMAKU_BYTES_IN_MB) ||
        (maxHeapBytes > 0L && maxHeapBytes < 256L * DANMAKU_BYTES_IN_MB)

    if (lowMemoryDevice) {
        return DanmakuPreloadPolicy(
            mode = DanmakuPreloadMode.WINDOW_ONLY,
            cacheRadius = 1,
            maxCachedSegments = 3,
            maxRetainedDanmakuItems = 8_000,
            maxLoadedDanmakuIds = 20_000,
            minFreeHeapBytesForFullPreload = 96L * DANMAKU_BYTES_IN_MB,
            minAvailableMemoryBytesForFullPreload = 384L * DANMAKU_BYTES_IN_MB
        )
    }

    val highMemoryDevice = (totalMemoryBytes <= 0L || totalMemoryBytes >= 6L * DANMAKU_BYTES_IN_GB) &&
        (availableMemoryBytes <= 0L || availableMemoryBytes >= 1536L * DANMAKU_BYTES_IN_MB) &&
        (maxHeapBytes <= 0L || maxHeapBytes >= 384L * DANMAKU_BYTES_IN_MB)

    return if (highMemoryDevice) {
        DanmakuPreloadPolicy(
            mode = DanmakuPreloadMode.FULL_VIDEO,
            cacheRadius = 1,
            maxCachedSegments = 4,
            maxRetainedDanmakuItems = 30_000,
            maxLoadedDanmakuIds = 80_000,
            minFreeHeapBytesForFullPreload = 192L * DANMAKU_BYTES_IN_MB,
            minAvailableMemoryBytesForFullPreload = 768L * DANMAKU_BYTES_IN_MB
        )
    } else {
        DanmakuPreloadPolicy(
            mode = DanmakuPreloadMode.WINDOW_ONLY,
            cacheRadius = 1,
            maxCachedSegments = 3,
            maxRetainedDanmakuItems = 16_000,
            maxLoadedDanmakuIds = 40_000,
            minFreeHeapBytesForFullPreload = 128L * DANMAKU_BYTES_IN_MB,
            minAvailableMemoryBytesForFullPreload = 512L * DANMAKU_BYTES_IN_MB
        )
    }
}

private fun String.normalizedSubtitleLanguageKey(): String {
    return trim().lowercase().replace('_', '-')
}

private fun Subtitle.matchesLanguagePreference(preference: SubtitleLanguagePreference): Boolean {
    val preferredLang = preference.lang.normalizedSubtitleLanguageKey()
    val preferredLangDoc = preference.langDoc.trim()

    return (preferredLang.isNotBlank() && lang.normalizedSubtitleLanguageKey() == preferredLang) ||
        (preferredLangDoc.isNotBlank() && langDoc.trim() == preferredLangDoc)
}

data class DlnaMediaSource(
    val url: String,
    val mimeType: String,
    val title: String,
    val partTitle: String,
    val positionMs: Long,
) {
    val displayTitle: String
        get() = when {
            title.isBlank() -> partTitle.ifBlank { "哔哩哔哩视频" }
            partTitle.isBlank() || partTitle == title -> title
            else -> "$title - $partTitle"
        }
}

@KoinViewModel
class VideoPlayerV3ViewModel(
    private val videoInfoRepository: VideoInfoRepository,
    private val videoPlayRepository: VideoPlayRepository,
    private val videoDetailRepository: VideoDetailRepository,
    private val liveRepository: LiveRepository,
    private val offlineVideoCacheService: OfflineVideoCacheService,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val logger = KotlinLogging.logger { }
    private val settings get() = PlayerSettingsProvider.current

    private enum class SubtitleSlot(val logPrefix: String, val debugName: String) {
        Primary("", "primary"),
        Secondary("副", "secondary")
    }

    private data class PendingVodPlaybackSource(
        val videoUrl: String?,
        val audioUrl: String?,
        val startPositionMs: Long,
        val playbackSessionToken: Long,
    )

    private data class DlnaSourceSnapshot(
        val aid: Long,
        val cid: Long,
        val epid: Int?,
        val qn: Int,
        val title: String,
        val partTitle: String,
        val positionMs: Long,
    )

    private data class VideoDanmakuSendSnapshot(
        val aid: Long,
        val cid: Long,
        val progress: Int,
    )

    private data class GeetestPlaybackRetryRequest(
        val avid: Long,
        val cid: Long,
        val epid: Int?,
        val seasonId: Int?,
        val initialSeekPositionMs: Long?,
        val preferApi: ApiType,
        val proxyArea: ProxyArea,
        val fromSeason: Boolean,
        val playbackSessionToken: Long,
    )

    private data class PendingGeetestVerification(
        val vVoucher: String,
        val token: String,
        val challenge: String,
        val retryRequest: GeetestPlaybackRetryRequest,
    )

    private data class RegisteredGeetestChallenge(
        val token: String,
        val gt: String,
        val challenge: String,
    )

    private sealed interface GeetestChallengeRefreshSource {
        data class FreshVoucher(val value: String) : GeetestChallengeRefreshSource

        data class PlayableData(val value: PlayData) : GeetestChallengeRefreshSource
    }

    private sealed interface GeetestChallengeRefreshOutcome {
        data class FreshRegistration(
            val vVoucher: String,
            val registration: RegisteredGeetestChallenge,
        ) : GeetestChallengeRefreshOutcome

        data class PlayableData(val value: PlayData) : GeetestChallengeRefreshOutcome
    }

    private data class GeetestChallengeRefreshFlight(
        val generation: Long,
        val pending: PendingGeetestVerification,
        val startedAtMillis: Long,
        val deferred: Deferred<GeetestChallengeRefreshOutcome>,
    )

    private fun isCellularPlaybackNetwork(): Boolean =
        runCatching { NetworkUtil.isCellularNetwork() }.getOrDefault(false)

    private fun defaultQualityForCurrentNetwork(): Resolution =
        PlaybackPreferenceSelector.selectQuality(
            isCellular = isCellularPlaybackNetwork(),
            defaultQuality = settings.defaultQuality,
            cellularQuality = settings.defaultCellularQuality
        )

    private fun defaultAudioForCurrentNetwork(): Audio =
        PlaybackPreferenceSelector.selectAudio(
            isCellular = isCellularPlaybackNetwork(),
            defaultAudio = settings.defaultAudio,
            cellularAudio = settings.defaultCellularAudio
        )

    private fun defaultLiveQnForCurrentNetwork(): Int =
        PlaybackPreferenceSelector.selectLiveQuality(
            isCellular = isCellularPlaybackNetwork(),
            defaultLiveQn = settings.defaultLiveQn,
            cellularLiveQn = settings.defaultCellularLiveQn
        )

    var videoPlayer: AbstractVideoPlayer? by mutableStateOf(null)
    var danmakuPlayer: DanmakuPlayer? by mutableStateOf(null)
    var liveDanmakuPlayer: LiveDanmakuPlayer? by mutableStateOf(null)
    var show by mutableStateOf(false)
    
    override fun onCleared() {
        super.onCleared()
        logger.fInfo { "VideoPlayerV3ViewModel onCleared" }

        saveSubtitleSmartDisplayPreferenceIfNeeded()
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

    // 风控 Geetest 验证状态
    var showGeetestDialog by mutableStateOf(false)
    var geetestGt by mutableStateOf("")
    var geetestChallenge by mutableStateOf("")
    private var pendingGeetestVerification: PendingGeetestVerification? = null
    private var geetestValidationPending: PendingGeetestVerification? = null
    private var geetestRegistrationGeneration = 0L
    private val geetestVoucherRegisterMutex = Mutex()
    private val attemptedGeetestVVouchers = mutableSetOf<String>()
    private var geetestChallengeRefreshFlight: GeetestChallengeRefreshFlight? = null

    private var playData: PlayData? by mutableStateOf(null)
    val danmakuData: MutableList<DanmakuItemData> = ArrayList()
    val danmakuMasks = mutableStateListOf<DanmakuMaskSegment>()
    var videoShot: VideoShot? by mutableStateOf(null)
    var clipInfoList: List<ClipInfo> by mutableStateOf(emptyList())
    var viewPoints: List<VideoPlayerViewPoint> by mutableStateOf(emptyList())

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

    fun replaceRelatedVideos(videos: List<VideoCardData>) {
        videoInfoRepository.relatedVideos.clear()
        videoInfoRepository.relatedVideos.addAll(videos)
        if (videos.isEmpty()) showRelatedVideos = false
    }

    var currentQuality by mutableStateOf(defaultQualityForCurrentNetwork())
    var currentVideoCodec by mutableStateOf(settings.defaultVideoCodec)
    var currentPlaySpeed by mutableFloatStateOf(settings.currentPlaySpeed)
    var currentVideoAspectRatio by mutableStateOf(VideoAspectRatio.Default)
    var currentVideoRotation by mutableStateOf(VideoRotation.Original)
    var currentPlaybackMediaMode by mutableStateOf(PlaybackMediaMode.Normal)
    var currentAudio by mutableStateOf(defaultAudioForCurrentNetwork())
    var currentDanmakuScale by mutableFloatStateOf(settings.defaultDanmakuScale)
    var currentDanmakuOpacity by mutableFloatStateOf(settings.defaultDanmakuOpacity)
    var currentDanmakuEnabled by mutableStateOf(settings.defaultDanmakuEnabled)
    val currentDanmakuTypes = mutableStateListOf<DanmakuType>().apply {
        addAll(settings.defaultDanmakuTypes)
    }
    var currentDanmakuArea by mutableFloatStateOf(settings.defaultDanmakuArea)
    var currentDanmakuSpeedMode by mutableStateOf(settings.defaultDanmakuSpeedMode)
    var currentDanmakuPresentationSpeed by mutableFloatStateOf(settings.defaultDanmakuPresentationSpeed)
    private val danmakuCapabilities = TvDanmakuCompatibilityPolicy.resolve(
        sdkInt = Build.VERSION.SDK_INT,
        isTvDevice = DeviceUtil.isTvDevice(),
    )
    val danmakuMaskSupported: Boolean
        get() = danmakuCapabilities.danmakuMaskSupported
    private var currentDanmakuMaskState by mutableStateOf(
        settings.defaultDanmakuMask && danmakuMaskSupported
    )
    var currentDanmakuMask: Boolean
        get() = currentDanmakuMaskState && danmakuMaskSupported
        set(value) {
            if (!danmakuMaskSupported) {
                currentDanmakuMaskState = false
                resetResolvedDanmakuMask(clearMasks = true)
                return
            }

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
    var currentDanmakuFilterLevel by mutableIntStateOf(settings.defaultDanmakuFilterLevel)
    val danmakuSmartFilterSupported = DanmakuSmartFilterPolicy.isSupported()
    var currentDanmakuMergeEnabled by mutableStateOf(
        settings.defaultDanmakuMergeEnabled && danmakuSmartFilterSupported
    )
        private set
    var currentLiveDanmakuFilterLevel by mutableIntStateOf(settings.defaultLiveDanmakuFilterLevel)
    var currentSubtitleId by mutableLongStateOf(-1L)
    var currentSubtitleData = mutableStateListOf<SubtitleItem>()
    var currentSubtitleType by mutableStateOf(SubtitleType.CC)
    var currentSubtitleFontSize by mutableStateOf(settings.defaultSubtitleFontSize)
    var currentSubtitleBackgroundOpacity by mutableFloatStateOf(settings.defaultSubtitleBackgroundOpacity)
    var currentSubtitleBottomPadding by mutableStateOf(settings.defaultSubtitleBottomPadding)
    var currentSecondarySubtitleId by mutableLongStateOf(-1L)
    var currentSecondarySubtitleData = mutableStateListOf<SubtitleItem>()
    var currentSecondarySubtitleType by mutableStateOf(SubtitleType.CC)
    var currentSecondarySubtitleFontSize by mutableStateOf(settings.defaultSecondarySubtitleFontSize)
    var currentSecondarySubtitleBackgroundOpacity by mutableFloatStateOf(settings.defaultSecondarySubtitleBackgroundOpacity)
    var currentSecondarySubtitleBottomPadding by mutableStateOf(settings.defaultSecondarySubtitleBottomPadding)
    private var currentPrimarySubtitleLoadToken = 0L
    private var currentSecondarySubtitleLoadToken = 0L

    var currentPlayMode by mutableStateOf(settings.defaultPlayMode)

    var title by mutableStateOf("")
    var partTitle by mutableStateOf("")
    var cover by mutableStateOf("")
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
    var sendingVideoDanmaku by mutableStateOf(false)
    
    // 直播相关属性
    var isLive by mutableStateOf(false)
    var liveRoomId by mutableIntStateOf(0)
    var liveStreamUrl by mutableStateOf("")
    var liveCover by mutableStateOf("")
    var liveBackground by mutableStateOf("")
    var liveWatchedShow by mutableStateOf("")
    var liveTime by mutableStateOf<Int?>(null)
    var liveAnchorId by mutableLongStateOf(0L)
    var liveLikeClickCount by mutableIntStateOf(0)
    var sendingLiveDanmaku by mutableStateOf(false)
    var loadingLiveEmoticons by mutableStateOf(false)
    var liveEmoticonError by mutableStateOf<String?>(null)
    val liveEmotePackages = mutableStateListOf<LiveEmotePackage>()
    private var liveEmoticonRoomId: Int? = null
    private var liveDanmakuObjectPoolUsable = true
    var likingLiveRoom by mutableStateOf(false)

    // 直播画质管理
    var availableLiveQualities = mutableStateListOf<Pair<Int, String>>() // qn -> description
    var currentLiveQn by mutableIntStateOf(defaultLiveQnForCurrentNetwork())
    var currentLiveQualityDescription by mutableStateOf("")
    private var liveQnDescMap: Map<Int, String> = emptyMap()

    // 直播编码管理
    var currentLiveCodec by mutableStateOf(settings.defaultLiveCodec)

    // 直播线路管理
    var availableLiveLines = mutableStateListOf<LiveStreamLine>()
    var currentLiveLineIndex by mutableIntStateOf(0)
    private var preferredLiveLineIndex: Int? = null

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
    private var pendingVodPlaybackSource: PendingVodPlaybackSource? = null
    private var manualVodPlaybackRequested = false
    var resolvedVodStartPositionMs by mutableLongStateOf(0L)
        private set
    var hasResolvedVodStartPosition by mutableStateOf(false)
        private set
    private var resolvedVodStartPositionSessionToken = 0L
    private val danmakuPlayerDataMutex = Mutex()
    var currentPlaybackOffline by mutableStateOf(false)
        private set

    private val vodBufferRecoveryPolicy = VodBufferRecoveryPolicy()
    private var vodBufferRecoveryJob: Job? = null
    private var vodSourceTransitionPending = false
    var vodBufferRecoveryPrompt: VodBufferRecoveryPrompt? by mutableStateOf(null)
        private set
    var vodBufferRecoveryNotice: String? by mutableStateOf(null)
        private set

    val offlineCacheState: OfflineVideoCacheTaskState
        get() = offlineVideoCacheService.stateOf(currentAid, currentCid)

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
        private const val INITIAL_DANMAKU_SEGMENT_PREFETCH = 2
        private const val INITIAL_DANMAKU_SLICE_PREFETCH = 3
        private const val CATCH_UP_DANMAKU_SLICE_PREFETCH = 2
        private const val MAX_DANMAKU_SLICE_PREFETCH = 8
        private const val DANMAKU_COVERAGE_LOOKAHEAD_MS = 20_000L
        private const val DANMAKU_COVERAGE_RECHECK_INTERVAL_MS = 3_000L
        private const val DANMAKU_BATCH_SIZE = 600
        private const val DANMAKU_SLICE_SIZE = 2000
        private const val DANMAKU_SLICE_EMIT_DELAY_MS = 12L
        private const val DANMAKU_WEB_EMPTY_IMMEDIATE_RETRIES = 2
        private const val DANMAKU_WEB_EMPTY_RETRY_DELAY_MS = 180L
        private const val LIVE_DANMAKU_MESSAGE_LIMIT = 300
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
    private var currentLiveDanmakuRoomId: Int? = null
    val liveDanmakuMessages = mutableStateListOf<LiveDanmakuMessage>()
    private var nextLiveDanmakuMessageId = 0L
    
    var coin by mutableStateOf(0)
    var favorite by mutableStateOf(0)
    var upName by mutableStateOf("")
    var upFace by mutableStateOf("")
    var pubTime by mutableStateOf("")
    var upId by mutableLongStateOf(0L)
    var isLoop by mutableStateOf(settings.isLoop)
    var showDanmaku by mutableStateOf(settings.showDanmaku)
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
    var enableSponsorBlock by mutableStateOf(settings.enableSponsorBlock)
    var sponsorBlockSkipMode by mutableStateOf(settings.sponsorBlockSkipMode)
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
        var token: Long,
        val aid: Long,
        val cid: Long,
        val filterLevel: Int,
        val mergeEnabled: Boolean,
        var preloadPolicy: DanmakuPreloadPolicy,
        val loadedDanmakuIds: LinkedHashSet<Long> = LinkedHashSet(),
        val danmakuSegmentCacheBySegment: LinkedHashMap<Int, DanmakuSegmentCacheEntry<List<DanmakuSlice>>> = LinkedHashMap(),
        val nextDanmakuSliceIndexBySegment: LinkedHashMap<Int, Int> = LinkedHashMap(),
        val vodDanmakuMergeState: VodDanmakuMergeState = VodDanmakuMergeState(),
        val segmentLoadMutex: Mutex = Mutex(),
        val sliceEmitMutex: Mutex = Mutex(),
        val initialSegmentReady: AtomicBoolean = AtomicBoolean(false)
    )

    private data class DanmakuSlice(
        val segmentIndex: Int,
        val sliceIndex: Int,
        val startPositionMs: Long,
        val endPositionMs: Long,
        val items: List<DanmakuItemData>
    )

    private data class DanmakuSegmentFetchResult(
        val data: List<DanmakuData>,
        val source: String,
        val attempts: Int,
        val rawCount: Int
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
        danmakuData.clear()
        lastDanmakuCatchUpPositionMs = -1L
        lastDanmakuCatchUpSegment = -1
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
                val playData = fetchPreparedAutoPlayPlayData(transitionContext)
                if (!playData.needPay && !playData.hasPlayableVodStreams()) {
                    logger.fWarn {
                        "Prefetched play data has empty streams: [aid=${transitionContext.aid}, cid=${transitionContext.cid}]"
                    }
                    return@launch
                }

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
            forceStartPlayback = true,
        )
        return true
    }

    fun onVodRebufferingStarted(positionMs: Long) {
        if (
            isLive ||
            currentPlaybackOffline ||
            currentPlaybackMediaMode == PlaybackMediaMode.AudioOnly ||
            currentAid <= 0L ||
            currentCid <= 0L ||
            vodSourceTransitionPending ||
            vodBufferRecoveryJob?.isActive == true ||
            vodBufferRecoveryPrompt != null
        ) {
            return
        }

        val resumePositionMs = videoPlayer?.currentPosition
            ?.takeIf { it > 0L }
            ?: positionMs.coerceAtLeast(0L)

        when (vodBufferRecoveryPolicy.onRebuffering(SystemClock.elapsedRealtime())) {
            VodBufferRecoveryDecision.None -> Unit

            VodBufferRecoveryDecision.SuggestCdnSwitch -> {
                val currentService = Prefs.cdnService
                val targetService = nextVodRecoveryCdnService(currentService)
                videoPlayer?.pause()
                vodBufferRecoveryPrompt = VodBufferRecoveryPrompt.SwitchCdn(
                    resumePositionMs = resumePositionMs,
                    fromService = currentService,
                    toService = targetService,
                )
                addVodBufferRecoveryLog("频繁缓冲，建议从${currentService.displayName}切换到${targetService.displayName}")
            }

            VodBufferRecoveryDecision.SuggestResolutionDowngrade -> {
                val targetResolution = nextLowerVodResolution(currentQuality, availableQuality)
                if (targetResolution == null) {
                    vodBufferRecoveryPolicy.suppress()
                    vodBufferRecoveryNotice = "当前已是最低清晰度"
                    addVodBufferRecoveryLog("换源后仍缓冲，当前已是最低清晰度")
                    return
                }

                videoPlayer?.pause()
                vodBufferRecoveryPrompt = VodBufferRecoveryPrompt.LowerResolution(
                    resumePositionMs = resumePositionMs,
                    fromResolution = currentQuality,
                    toResolution = targetResolution,
                )
                addVodBufferRecoveryLog("换源后仍缓冲，建议从$currentQuality 降级到$targetResolution")
            }
        }
    }

    fun onVodPlaybackResumed() {
        vodSourceTransitionPending = false
    }

    /**
     * 播放器报告解码/渲染跟不上（连续多个窗口丢帧过半）。与网络缓冲不同，换 CDN 无济于事，
     * 直接建议降低清晰度；复用缓冲恢复的提示与切换流程。
     */
    fun onVodDecoderOverloaded(droppedFrames: Int, totalFrames: Int) {
        if (
            isLive ||
            currentPlaybackOffline ||
            currentPlaybackMediaMode == PlaybackMediaMode.AudioOnly ||
            currentAid <= 0L ||
            currentCid <= 0L ||
            vodSourceTransitionPending ||
            vodBufferRecoveryJob?.isActive == true ||
            vodBufferRecoveryPrompt != null
        ) {
            return
        }

        if (vodBufferRecoveryPolicy.onDecoderOverload() != VodBufferRecoveryDecision.SuggestResolutionDowngrade) {
            return
        }

        val resumePositionMs = videoPlayer?.currentPosition?.takeIf { it > 0L } ?: 0L

        // 丢帧统计来自 VO：超分 shader 在跑时更可能是 GPU 渲染跟不上，先建议关超分，降清晰度救不了它
        if (videoPlayer?.isSuperResolutionActive == true) {
            videoPlayer?.pause()
            vodBufferRecoveryPrompt = VodBufferRecoveryPrompt.DisableSuperResolution(
                resumePositionMs = resumePositionMs,
            )
            addVodBufferRecoveryLog("渲染跟不上（超分开启，最近窗口丢帧 $droppedFrames/$totalFrames），建议关闭超分辨率")
            return
        }

        val targetResolution = nextLowerVodResolution(currentQuality, availableQuality)
        if (targetResolution == null) {
            vodBufferRecoveryPolicy.suppress()
            vodBufferRecoveryNotice = "设备解码跟不上，且当前已是最低清晰度"
            addVodBufferRecoveryLog("解码跟不上（最近窗口丢帧 $droppedFrames/$totalFrames），当前已是最低清晰度")
            return
        }

        videoPlayer?.pause()
        vodBufferRecoveryPrompt = VodBufferRecoveryPrompt.LowerResolution(
            resumePositionMs = resumePositionMs,
            fromResolution = currentQuality,
            toResolution = targetResolution,
            reason = LowerResolutionReason.DecoderOverload,
        )
        addVodBufferRecoveryLog("解码跟不上（最近窗口丢帧 $droppedFrames/$totalFrames），建议从$currentQuality 降级到$targetResolution")
    }

    fun dismissVodBufferRecoveryPrompt() {
        if (vodBufferRecoveryPrompt == null) return
        vodBufferRecoveryPrompt = null
        vodBufferRecoveryPolicy.suppress()
        videoPlayer?.start()
        addVodBufferRecoveryLog("用户暂不执行播放恢复建议")
    }

    fun confirmVodBufferRecoveryPrompt() {
        when (val prompt = vodBufferRecoveryPrompt) {
            is VodBufferRecoveryPrompt.SwitchCdn -> confirmVodCdnSwitch(prompt)
            is VodBufferRecoveryPrompt.LowerResolution -> confirmVodResolutionDowngrade(prompt)
            is VodBufferRecoveryPrompt.DisableSuperResolution -> confirmDisableSuperResolution()
            null -> Unit
        }
    }

    /**
     * 关掉本次会话的超分 shader 后继续播放。策略回到初始监控态：如果去掉 shader 后仍然丢帧，
     * 说明瓶颈在解码器，后续还可以正常走“降清晰度”提示。
     */
    private fun confirmDisableSuperResolution() {
        vodBufferRecoveryPrompt = null
        videoPlayer?.disableSuperResolution()
        vodBufferRecoveryPolicy.reset()
        videoPlayer?.start()
        vodBufferRecoveryNotice = "已关闭本次播放的超分辨率（可在 MPV 设置中永久关闭）"
        addVodBufferRecoveryLog("用户关闭本次播放的超分辨率")
    }

    fun consumeVodBufferRecoveryNotice() {
        vodBufferRecoveryNotice = null
    }

    private fun confirmVodCdnSwitch(prompt: VodBufferRecoveryPrompt.SwitchCdn) {
        if (!vodBufferRecoveryPolicy.startCdnSwitch()) return
        vodBufferRecoveryPrompt = null
        val playbackSessionToken = vodPlaybackSessionToken
        val aid = currentAid
        val cid = currentCid
        val episodeId = currentEpid
        val currentSeasonId = seasonId.takeIf { it > 0 }
        val quality = currentQuality
        val codec = currentVideoCodec
        val audio = currentAudio
        val mediaMode = currentPlaybackMediaMode
        val resumePositionMs = videoPlayer?.currentPosition
            ?.takeIf { it > 0L }
            ?: prompt.resumePositionMs

        vodBufferRecoveryJob?.cancel()
        vodBufferRecoveryJob = viewModelScope.launch(Dispatchers.IO) {
            val originalService = prompt.fromService
            try {
                Prefs.cdnService = prompt.toService
                addLogs("切换全局播放 CDN：${originalService.displayName} -> ${prompt.toService.displayName}")

                val loaded = loadPlayUrl(
                    avid = aid,
                    cid = cid,
                    epid = episodeId,
                    seasonId = currentSeasonId,
                    preferApi = settings.apiType,
                    proxyArea = proxyArea,
                    initialSeekPositionMs = resumePositionMs,
                    targetQuality = quality,
                    targetVideoCodec = codec,
                    targetAudio = audio,
                    targetMediaMode = mediaMode,
                    playbackSessionToken = playbackSessionToken,
                )

                withContext(Dispatchers.Main) {
                    val stillCurrent = isVodPlaybackSessionActive(playbackSessionToken) &&
                        currentAid == aid && currentCid == cid
                    if (loaded && stillCurrent) {
                        vodBufferRecoveryPolicy.finishCdnSwitch(success = true)
                        videoPlayer?.start()
                        vodBufferRecoveryNotice = when (prompt.toService) {
                            CdnService.BaseUrl -> "已切换至主线路"
                            CdnService.BackupUrl -> "已切换至备选线路"
                            else -> "已更换播放 CDN"
                        }
                    } else if (stillCurrent) {
                        rollbackVodCdnSwitch(originalService)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.fWarn { "Switch vod CDN failed: ${error.message}" }
                withContext(Dispatchers.Main) {
                    if (isVodPlaybackSessionActive(playbackSessionToken)) {
                        rollbackVodCdnSwitch(originalService)
                    }
                }
            }
        }
    }

    private fun rollbackVodCdnSwitch(originalService: CdnService) {
        Prefs.cdnService = originalService
        vodBufferRecoveryPolicy.finishCdnSwitch(success = false)
        vodSourceTransitionPending = false
        loadState = RequestState.Success
        errorMessage = ""
        videoPlayer?.start()
        vodBufferRecoveryNotice = "更换 CDN 失败，已继续使用当前线路"
        addVodBufferRecoveryLog("更换 CDN 失败，已回滚到${originalService.displayName}")
    }

    private fun confirmVodResolutionDowngrade(
        prompt: VodBufferRecoveryPrompt.LowerResolution,
    ) {
        if (!vodBufferRecoveryPolicy.startResolutionDowngrade()) return
        vodBufferRecoveryPrompt = null
        val playbackSessionToken = vodPlaybackSessionToken
        val resumePositionMs = videoPlayer?.currentPosition
            ?.takeIf { it > 0L }
            ?: prompt.resumePositionMs

        vodBufferRecoveryJob?.cancel()
        vodBufferRecoveryJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                withContext(Dispatchers.Main) {
                    loadState = RequestState.Ready
                    errorMessage = ""
                }
                playQuality(
                    qn = prompt.toResolution,
                    initialSeekPositionMs = resumePositionMs,
                    playbackSessionToken = playbackSessionToken,
                )
                withContext(Dispatchers.Main) {
                    if (loadState == RequestState.Failed) {
                        throw IllegalStateException(errorMessage.ifBlank { "清晰度切换失败" })
                    }
                    vodBufferRecoveryPolicy.finishResolutionDowngrade()
                    if (isVodPlaybackSessionActive(playbackSessionToken)) {
                        loadState = RequestState.Success
                        videoPlayer?.start()
                        vodBufferRecoveryNotice = "已降低清晰度"
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.fWarn { "Downgrade vod resolution failed: ${error.message}" }
                withContext(Dispatchers.Main) {
                    currentQuality = prompt.fromResolution
                    vodBufferRecoveryPolicy.finishResolutionDowngrade()
                    vodSourceTransitionPending = false
                    loadState = RequestState.Success
                    errorMessage = ""
                    videoPlayer?.start()
                    vodBufferRecoveryNotice = "清晰度切换失败，已继续使用当前清晰度"
                }
            }
        }
    }

    private fun resetVodBufferRecovery() {
        vodBufferRecoveryJob?.cancel()
        vodBufferRecoveryJob = null
        vodBufferRecoveryPrompt = null
        vodBufferRecoveryNotice = null
        vodSourceTransitionPending = false
        vodBufferRecoveryPolicy.reset()
    }

    private fun addVodBufferRecoveryLog(text: String) {
        viewModelScope.launch(Dispatchers.Default) {
            addLogs(text)
        }
    }

    fun loadPlayUrl(
        avid: Long,
        cid: Long,
        epid: Int? = null,
        seasonId: Int? = null,
        continuePlayNext: Boolean = false,
        initialSeekPositionMs: Long? = null,
        preparedPlayData: PlayData? = null,
        forceStartPlayback: Boolean = false,
        preferOfflineCache: Boolean = false,
    ) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            startLoadPlayUrl(
                avid = avid,
                cid = cid,
                epid = epid,
                seasonId = seasonId,
                continuePlayNext = continuePlayNext,
                initialSeekPositionMs = initialSeekPositionMs,
                preparedPlayData = preparedPlayData,
                forceStartPlayback = forceStartPlayback,
                preferOfflineCache = preferOfflineCache
            )
        } else {
            viewModelScope.launch(Dispatchers.Main.immediate) {
                startLoadPlayUrl(
                    avid = avid,
                    cid = cid,
                    epid = epid,
                    seasonId = seasonId,
                    continuePlayNext = continuePlayNext,
                    initialSeekPositionMs = initialSeekPositionMs,
                    preparedPlayData = preparedPlayData,
                    forceStartPlayback = forceStartPlayback,
                    preferOfflineCache = preferOfflineCache
                )
            }
        }
    }

    private fun startLoadPlayUrl(
        avid: Long,
        cid: Long,
        epid: Int? = null,
        seasonId: Int? = null,
        continuePlayNext: Boolean = false,
        initialSeekPositionMs: Long? = null,
        preparedPlayData: PlayData? = null,
        forceStartPlayback: Boolean = false,
        preferOfflineCache: Boolean = false,
    ) {
        val manualPlaybackRequestedBeforeInitialLoad =
            vodPlaybackSessionToken == 0L && manualVodPlaybackRequested
        val playbackSessionToken = beginVodPlaybackSession()
        val historyPositionMs = lastPlayed
            .takeIf {
                !continuePlayNext &&
                    it > 0 &&
                    settings.playerDefaultStartPosition == PlayerDefaultStartPosition.History
            }
            ?.toLong()
        resolvedVodStartPositionMs = resolveVodPlaybackStartPositionMs(
            explicitPositionMs = initialSeekPositionMs,
            historyPositionMs = historyPositionMs,
            durationMs = 0L
        )
        hasResolvedVodStartPosition = true
        resolvedVodStartPositionSessionToken = playbackSessionToken
        val videoChanged = currentAid != avid || currentCid != cid
        if (videoChanged || preferOfflineCache) {
            resetVodBufferRecovery()
        }
        pendingVodPlaybackSource = null
        manualVodPlaybackRequested =
            settings.autoPlay || forceStartPlayback || manualPlaybackRequestedBeforeInitialLoad
        if (!settings.autoPlay) {
            runCatching {
                videoPlayer?.stop()
            }.onFailure {
                logger.fWarn { "Stop current vod playback before deferred manual play failed: ${it.message}" }
            }
            showBuffering = false
        }
        showInteractiveOptionDialog = false
        if (continuePlayNext) {
            lastPlayed = 0
        }
        if (videoChanged) {
            invalidatePreparedAutoPlayTarget()
            resetResolvedDanmakuMask(clearMasks = true)
            viewPoints = emptyList()
        }
        currentAid = avid
        currentCid = cid
        currentEpid = epid ?: 0
        availableVideoList
            .filterIsInstance<VideoListItemData>()
            .firstOrNull { it.cid == cid }
            ?.cover
            ?.takeIf { it.isNotBlank() }
            ?.let { cover = it }
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
            if (lastPlayEnabledSubtitle && settings.subtitleSmartDisplay) {
                logger.info { "Subtitle is enabled, next video will enable subtitle automatic" }
            }

            val useOfflinePlaybackSource = preferOfflineCache && offlineVideoCacheService.getCompletedEntry(avid, cid) != null
            if (!useOfflinePlaybackSource) {
                updateSubtitle()
                enableSmartSubtitleIfAvailable(
                    fallbackToFirstSubtitle = continuePlayNext && lastPlayEnabledSubtitle && upId <= 0L
                )
            }
            val playUrlLoaded = loadPlayUrl(
                avid = avid,
                cid = cid,
                epid = epid ?: 0,
                seasonId = seasonId,
                preferApi = settings.apiType,
                proxyArea = proxyArea,
                initialSeekPositionMs = initialSeekPositionMs,
                playbackSessionToken = playbackSessionToken,
                preparedPlayData = preparedPlayData,
                preferOfflineCache = preferOfflineCache,
            )
            if (playUrlLoaded && !useOfflinePlaybackSource) {
                launch {
                    updateViewPoints(
                        aid = avid,
                        cid = cid,
                        epid = epid,
                        seasonId = seasonId,
                        playbackSessionToken = playbackSessionToken
                    )
                }
            }
            if (isInteractivePlayback && (!interactiveOptionsFromQuestions || interactiveOptions.isEmpty())) {
                refreshInteractiveBranches(currentInteractiveEdgeId.takeIf { it > 0L })
            }
            if (playUrlLoaded) {
                val requestedDanmakuStartPositionMs = resolvedVodStartPositionMs.takeIf {
                    hasResolvedVodStartPosition &&
                        resolvedVodStartPositionSessionToken == playbackSessionToken
                } ?: resolveVodPlaybackStartPositionMs(
                    explicitPositionMs = initialSeekPositionMs,
                    historyPositionMs = lastPlayed
                        .takeIf {
                            it > 0 &&
                                settings.playerDefaultStartPosition == PlayerDefaultStartPosition.History
                        }
                        ?.toLong(),
                    durationMs = playData?.timeLength ?: 0L
                )
                val danmakuStartPositionMs = resolveVodPlaybackStartPositionMs(
                    explicitPositionMs = requestedDanmakuStartPositionMs,
                    historyPositionMs = null,
                    durationMs = playData?.timeLength ?: 0L
                )
                loadDanmaku(
                    cid = cid,
                    durationMs = playData?.timeLength ?: 0L,
                    initialPositionMsOverride = danmakuStartPositionMs
                )
            }
            if (playUrlLoaded && !useOfflinePlaybackSource) {
                updateDanmakuMask()

                // 加载 SponsorBlock 片段
                loadSponsorSegments(AvBvConverter.av2bv(avid), cid)

                updateVideoShot()
            }
        }
    }

    fun requestManualVodPlayback(): Boolean {
        if (isLive || settings.autoPlay) return false

        manualVodPlaybackRequested = true
        val playbackSource = pendingVodPlaybackSource ?: return true
        pendingVodPlaybackSource = null
        applyVodPlaybackSource(playbackSource, startWhenReady = true)
        return true
    }

    suspend fun cacheCurrentVideo(): Result<String> {
        if (isLive) {
            return Result.failure(IllegalStateException("直播暂不支持离线缓存"))
        }

        val target = withContext(Dispatchers.Main.immediate) {
            val data = playData ?: return@withContext null
            OfflineVideoCacheTarget(
                aid = currentAid,
                bvid = AvBvConverter.av2bv(currentAid),
                cid = currentCid,
                title = title,
                partTitle = partTitle,
                cover = cover,
                upName = upName,
                durationMs = data.timeLength,
                width = currentVideoWidth,
                height = currentVideoHeight,
                upFace = upFace,
                danmakuCount = danmaku
            )
        } ?: return Result.failure(IllegalStateException("视频地址尚未加载完成"))

        return cacheVideoTarget(target, settings.defaultOfflineCacheQuality)
    }

    suspend fun cacheVideoPage(
        page: VideoPage,
        preferredQuality: Resolution
    ): Result<String> {
        val target = withContext(Dispatchers.Main.immediate) {
            OfflineVideoCacheTarget(
                aid = currentAid,
                bvid = AvBvConverter.av2bv(currentAid),
                cid = page.cid,
                title = title,
                partTitle = page.title,
                cover = cover,
                upName = upName,
                durationMs = page.duration * 1000L,
                width = page.dimension.width,
                height = page.dimension.height,
                upFace = upFace,
                danmakuCount = danmaku
            )
        }
        return cacheVideoTarget(target, preferredQuality)
    }

    suspend fun cacheVideoTarget(
        target: OfflineVideoCacheTarget,
        preferredQuality: Resolution
    ): Result<String> {
        if (isLive) {
            return Result.failure(IllegalStateException("直播暂不支持离线缓存"))
        }

        if (target.aid <= 0L || target.cid <= 0L) {
            return Result.failure(IllegalStateException("视频信息不完整"))
        }

        val state = offlineVideoCacheService.stateOf(target.aid, target.cid)
        if (state.status == OfflineVideoCacheStatus.Completed) {
            return Result.success("已缓存，可离线播放")
        }
        if (state.isActive) {
            return Result.success("已在缓存队列中")
        }

        return offlineVideoCacheService.enqueue(
            buildOfflineCacheTaskRequest(target, preferredQuality)
        )
    }

    suspend fun getAvailableOfflineCacheQualities(
        target: OfflineVideoCacheTarget,
    ): Result<List<Resolution>> {
        if (isLive) {
            return Result.failure(IllegalStateException("直播暂不支持离线缓存"))
        }
        if (target.aid <= 0L || target.cid <= 0L) {
            return Result.failure(IllegalStateException("视频信息不完整"))
        }

        return runCatching {
            videoPlayRepository.getDownloadPlayData(
                aid = target.aid,
                bvid = target.bvid,
                cid = target.cid,
                qn = Resolution.R8K.code,
                tryLook1080P = settings.tryLook1080P,
            ).requireOfflineCacheStreams("WBI")
                .dashVideos
                .mapNotNull { video -> Resolution.entries.find { it.code == video.quality } }
                .distinct()
                .sortedByDescending { it.code }
                .ifEmpty { throw IllegalStateException("该视频没有可缓存画质") }
        }
    }

    fun cacheVideoTargets(
        targets: List<OfflineVideoCacheTarget>,
        preferredQuality: Resolution
    ): Result<String> {
        if (isLive) {
            return Result.failure(IllegalStateException("直播暂不支持离线缓存"))
        }
        val uniqueTargets = targets
            .filter { it.aid > 0L && it.cid > 0L }
            .distinctBy { "${it.aid}:${it.cid}" }
            .filter {
                val state = offlineVideoCacheService.stateOf(it.aid, it.cid)
                state.status != OfflineVideoCacheStatus.Completed && !state.isActive
            }
        if (uniqueTargets.isEmpty()) {
            return Result.failure(IllegalStateException("没有可添加的缓存任务"))
        }

        uniqueTargets.forEach { target ->
            offlineVideoCacheService.enqueue(
                buildOfflineCacheTaskRequest(target, preferredQuality)
            ).onFailure {
                logger.fException(it) {
                    "Add offline cache target failed: [aid=${target.aid}, cid=${target.cid}]"
                }
            }
        }
        return Result.success("已加入 ${uniqueTargets.size} 个缓存任务")
    }

    private fun buildOfflineCacheTaskRequest(
        target: OfflineVideoCacheTarget,
        preferredQuality: Resolution,
    ) = OfflineVideoCacheTaskRequest(
        target = target,
        preferredQuality = preferredQuality,
        tryLook1080P = settings.tryLook1080P,
        videoCodecPreferences = offlineCacheCodecCandidates(currentVideoCodec),
        preferredAudio = PlaybackPreferenceSelector.selectAudio(
            isCellular = isCellularPlaybackNetwork(),
            defaultAudio = settings.defaultAudio,
            cellularAudio = settings.defaultCellularAudio
        )
    )

    fun offlineCacheStateOf(aid: Long, cid: Long): OfflineVideoCacheTaskState =
        offlineVideoCacheService.stateOf(aid, cid)

    fun completedOfflineCacheEntry(aid: Long, cid: Long): OfflineVideoCacheEntry? =
        offlineVideoCacheService.getCompletedEntry(aid, cid)

    fun completedOfflineCacheEntries(aid: Long): List<OfflineVideoCacheEntry> =
        offlineVideoCacheService.getCompletedEntries(aid)

    fun prepareOfflinePlayback(aid: Long, cid: Long): Result<String> {
        val entries = offlineVideoCacheService.getAllCompletedEntries()
        val currentEntry = entries.firstOrNull { it.aid == aid && it.cid == cid }
            ?: return Result.failure(IllegalStateException("离线缓存文件不存在或不完整"))

        videoInfoRepository.replacePlaybackContext(
            videoList = entries.mapIndexed { index, entry ->
                VideoListUgcEpisode(
                    aid = entry.aid,
                    cid = entry.cid,
                    title = entry.title.ifBlank { entry.displayTitle },
                    partTitle = entry.displayTitle,
                    index = index,
                    cover = offlineVideoCacheService.getCachedCoverUri(entry),
                    duration = (entry.durationMs / 1000L)
                        .coerceAtMost(Int.MAX_VALUE.toLong())
                        .toInt(),
                    viewCount = 0L,
                    danmakuCount = entry.danmakuCount
                )
            },
            relatedVideos = emptyList(),
            interactivePlaybackContext = null
        )
        return playOfflineEntry(currentEntry, continuePlayNext = false)
    }

    fun playOfflinePlaylistItem(aid: Long, cid: Long): Result<String> {
        val entry = offlineVideoCacheService.getCompletedEntry(aid, cid)
            ?: return Result.failure(IllegalStateException("该视频的离线缓存不可用"))
        return playOfflineEntry(entry, continuePlayNext = true)
    }

    fun reloadCurrentOfflinePlayback(positionMs: Long): Result<String> {
        val entry = offlineVideoCacheService.getCompletedEntry(currentAid, currentCid)
            ?: return Result.failure(IllegalStateException("当前离线缓存不可用"))
        return playOfflineEntry(
            entry = entry,
            continuePlayNext = false,
            initialPositionMs = positionMs.coerceAtLeast(0L)
        )
    }

    private fun playOfflineEntry(
        entry: OfflineVideoCacheEntry,
        continuePlayNext: Boolean,
        initialPositionMs: Long? = null
    ): Result<String> {
        title = entry.title.ifBlank { entry.displayTitle }
        partTitle = entry.displayTitle
        cover = offlineVideoCacheService.getCachedCoverUri(entry).orEmpty()
        upName = entry.upName
        upFace = offlineVideoCacheService.getCachedUpFaceUri(entry).orEmpty()
        upId = 0L
        play = 0L
        danmaku = entry.danmakuCount
        like = 0
        coin = 0
        favorite = 0
        pubTime = ""
        fromSeason = false
        epid = 0
        seasonId = 0
        lastPlayed = 0
        isVerticalVideo = entry.height > entry.width && entry.width > 0
        currentPlaybackMediaMode = PlaybackMediaMode.Normal
        showRelatedVideos = false

        loadPlayUrl(
            avid = entry.aid,
            cid = entry.cid,
            continuePlayNext = continuePlayNext,
            initialSeekPositionMs = initialPositionMs,
            forceStartPlayback = true,
            preferOfflineCache = true
        )
        return Result.success("正在播放离线缓存")
    }

    fun pauseOfflineCache(): Result<String> =
        offlineVideoCacheService.pause(currentAid, currentCid)

    fun resumeOfflineCache(): Result<String> =
        offlineVideoCacheService.resume(currentAid, currentCid)

    suspend fun clearOfflineCacheTask(): Result<String> =
        offlineVideoCacheService.clearTask(currentAid, currentCid)

    fun playOfflineCache(): Result<String> {
        if (isLive) {
            return Result.failure(IllegalStateException("直播没有离线缓存"))
        }
        val aid = currentAid
        val cid = currentCid
        if (aid <= 0L || cid <= 0L) {
            return Result.failure(IllegalStateException("视频信息不完整"))
        }
        if (offlineVideoCacheService.getCompletedPlaybackSource(aid, cid) == null) {
            return Result.failure(IllegalStateException("当前分P尚未缓存完成"))
        }
        loadPlayUrl(
            avid = aid,
            cid = cid,
            epid = currentEpid.takeIf { it > 0 },
            seasonId = seasonId.takeIf { it > 0 },
            continuePlayNext = true,
            initialSeekPositionMs = videoPlayer?.currentPosition?.takeIf { it > 0L },
            forceStartPlayback = true,
            preferOfflineCache = true
        )
        return Result.success("正在播放离线缓存")
    }

    private fun applyVodPlaybackSource(
        playbackSource: PendingVodPlaybackSource,
        startWhenReady: Boolean = false,
    ) {
        if (!isVodPlaybackSessionActive(playbackSource.playbackSessionToken)) return

        val player = videoPlayer ?: return
        vodSourceTransitionPending = true
        logger.info { "Video url: ${playbackSource.videoUrl}" }
        logger.info { "Audio url: ${playbackSource.audioUrl}" }
        player.playUrl(playbackSource.videoUrl, playbackSource.audioUrl)

        if (playbackSource.startPositionMs > 0L) {
            logger.info { "Set initial seek position to ${playbackSource.startPositionMs}ms" }
            player.setInitialSeekPosition(playbackSource.startPositionMs)
        }

        player.prepare()
        if (startWhenReady) {
            player.start()
        }
        showBuffering = true
    }

    private suspend fun updateViewPoints(
        aid: Long,
        cid: Long,
        epid: Int?,
        seasonId: Int?,
        playbackSessionToken: Long,
    ) {
        val points = runCatching {
            val playerInfo = videoPlayRepository.getVideoPlayerWbiInfo(
                aid = aid,
                cid = cid,
                epid = epid,
                seasonId = seasonId
            )
            if (playerInfo.viewPoints.firstOrNull()?.type != 2) {
                emptyList()
            } else {
                playerInfo.viewPoints
                    .filter { point -> point.to > point.from && point.content.isNotBlank() }
                    .map { point ->
                        VideoPlayerViewPoint(
                            from = point.from,
                            to = point.to,
                            content = point.content,
                            imageUrl = point.imgUrl
                        )
                    }
            }
        }.onFailure {
            logger.fWarn { "Load video view points failed: ${it.message}" }
        }.getOrDefault(emptyList())

        withContext(Dispatchers.Main) {
            if (
                isVodPlaybackSessionActive(playbackSessionToken) &&
                currentAid == aid &&
                currentCid == cid
            ) {
                viewPoints = points
            }
        }
    }

    private suspend fun loadPlayUrl(
        avid: Long,
        cid: Long,
        epid: Int = 0,
        seasonId: Int? = null,
        preferApi: ApiType = settings.apiType,
        proxyArea: ProxyArea = ProxyArea.MainLand,
        initialSeekPositionMs: Long? = null,
        targetQuality: Resolution? = null,
        targetVideoCodec: VideoCodec? = null,
        targetAudio: Audio? = null,
        targetMediaMode: PlaybackMediaMode = currentPlaybackMediaMode,
        playbackSessionToken: Long = vodPlaybackSessionToken,
        preparedPlayData: PlayData? = null,
        preferOfflineCache: Boolean = false,
    ): Boolean {
        logger.fInfo { "Load play url: [av=$avid, cid=$cid, preferApi=$preferApi, proxyArea=$proxyArea, usePreparedPlayData=${preparedPlayData != null}, preferOfflineCache=$preferOfflineCache]" }
        withContext(Dispatchers.Main) {
            if (isVodPlaybackSessionActive(playbackSessionToken)) {
                loadState = RequestState.Ready
            }
        }
        logger.fInfo { "Set request state: ready" }
        logger.fInfo { "fromSeason: $fromSeason" }
        if (preferOfflineCache) {
            offlineVideoCacheService.getCompletedPlaybackSource(avid, cid)?.let { offlineSource ->
                logger.fInfo { "Use offline playback source: [av=$avid, cid=$cid]" }
                return loadOfflinePlaybackSource(
                    source = offlineSource,
                    initialSeekPositionMs = initialSeekPositionMs,
                    targetMediaMode = targetMediaMode,
                    playbackSessionToken = playbackSessionToken
                )
            }
        }
        withContext(Dispatchers.Main) {
            if (isVodPlaybackSessionActive(playbackSessionToken)) {
                currentPlaybackOffline = false
                videoPlayer?.setOfflinePlaybackMode(false)
            }
        }
        return try {
            val playData = when {
                preparedPlayData == null -> fetchPlayableVodPlayData(
                    avid = avid,
                    cid = cid,
                    epid = epid,
                    preferApi = preferApi,
                    proxyArea = proxyArea
                )

                preparedPlayData.needPay || preparedPlayData.hasPlayableVodStreams() -> preparedPlayData

                else -> {
                    logger.fWarn { "Prepared play data has empty streams, reload play url: [av=$avid, cid=$cid]" }
                    fetchPlayableVodPlayData(
                        avid = avid,
                        cid = cid,
                        epid = epid,
                        preferApi = preferApi,
                        proxyArea = proxyArea
                    )
                }
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

            // 内核无法按 HDR 输出（如 MPV 的 gpu 路径）时，HDR/杜比视界档位只会得到发灰或被压成 SDR 的画面，
            // 直接不提供这些档位，避免用户以为“开了 HDR”
            val player = videoPlayer
            if (player != null && !player.supportsHdrOutput) {
                val hdrResolutions = resolutionList.filter { it == Resolution.RHdr || it == Resolution.RDolby }
                if (hdrResolutions.isNotEmpty() && resolutionList.size > hdrResolutions.size) {
                    resolutionList.removeAll(hdrResolutions)
                    logger.fInfo { "Hide HDR resolutions for player without HDR output: $hdrResolutions" }
                    addLogs("当前播放内核无法输出 HDR，已隐藏 ${hdrResolutions.joinToString()} 档位")
                }
            }

            logger.fInfo { "Video available resolution: $resolutionList" }
            availableQuality.swapListWithMainContext(resolutionList)
            if (resolutionList.isEmpty()) {
                throw PlayDataUnavailableException("接口未返回可播放视频流")
            }

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
            val hasMuxedVideo = playData.hasMuxedVideo()
            if (audioList.isEmpty() && !hasMuxedVideo) {
                throw PlayDataUnavailableException("接口未返回可播放音频流")
            }

            ensureVodPlaybackSessionActive(playbackSessionToken)

            val isCellularNetwork = isCellularPlaybackNetwork()
            val preferredDefaultQuality = PlaybackPreferenceSelector.selectQuality(
                isCellular = isCellularNetwork,
                defaultQuality = settings.defaultQuality,
                cellularQuality = settings.defaultCellularQuality
            )

            // 确定使用哪个默认分辨率
            val portraitLimitedQuality = if (
                isVerticalVideo &&
                settings.portraitVideoFixMode == PortraitVideoFixMode.LimitResolution1080P &&
                preferredDefaultQuality >= Resolution.R4K
            ) {
                // 如果是竖屏视频且用户设置了竖屏视频限制最高使用1080P
                Resolution.R1080P60
            } else {
                // 否则使用普通设置
                preferredDefaultQuality
            }

            // 内核建议的上限（如 API 26 以下的 MPV 无法零拷贝硬解）：只约束自动选择，不覆盖用户在菜单里的手动选择
            val playerMaxCode = player?.preferredMaxResolutionCode
            val defaultQualityToUse = if (playerMaxCode != null && portraitLimitedQuality.code > playerMaxCode) {
                val capped = Resolution.fromCode(playerMaxCode) ?: portraitLimitedQuality
                logger.fInfo { "Cap default resolution $portraitLimitedQuality -> $capped for current player" }
                addLogs("当前播放内核在此设备上建议不超过 $capped，默认清晰度已下调")
                capped
            } else {
                portraitLimitedQuality
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

            val preferredAudio = targetAudio ?: PlaybackPreferenceSelector.selectAudio(
                isCellular = isCellularNetwork,
                defaultAudio = settings.defaultAudio,
                cellularAudio = settings.defaultCellularAudio
            )
            if (audioList.isNotEmpty()) {
                val selectedAudio = when {
                    audioList.contains(preferredAudio) -> preferredAudio
                    preferredAudio == Audio.ADolbyAtmos && audioList.contains(Audio.AHiRes) -> Audio.AHiRes
                    preferredAudio == Audio.AHiRes && audioList.contains(Audio.ADolbyAtmos) -> Audio.ADolbyAtmos
                    audioList.contains(Audio.A192K) -> Audio.A192K
                    audioList.contains(Audio.A132K) -> Audio.A132K
                    audioList.contains(Audio.A64K) -> Audio.A64K
                    else -> audioList.first()
                }
                withContext(Dispatchers.Main) {
                    if (isVodPlaybackSessionActive(playbackSessionToken)) {
                        currentAudio = selectedAudio
                    }
                }
            } else {
                logger.fInfo { "Use audio embedded in progressive video stream" }
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
        } catch (e: VVoucherException) {
            logger.fWarn { "Risk control v_voucher detected: ${e.vVoucher}" }
            val isCurrentPlayback = withContext(Dispatchers.Main.immediate) {
                isVodPlaybackSessionActive(playbackSessionToken)
            }
            if (!isCurrentPlayback) {
                logger.fDebug { "Ignore v_voucher from stale vod playback session" }
                return false
            }
            addLogs("触发风控，正在申请验证…")
            handleVVoucher(
                vVoucher = e.vVoucher,
                retryRequest = GeetestPlaybackRetryRequest(
                    avid = avid,
                    cid = cid,
                    epid = epid.takeIf { it > 0 },
                    seasonId = seasonId?.takeIf { it > 0 },
                    initialSeekPositionMs = initialSeekPositionMs,
                    preferApi = preferApi,
                    proxyArea = proxyArea,
                    fromSeason = fromSeason,
                    playbackSessionToken = playbackSessionToken,
                )
            )
            false
        } catch (e: PlayDataUnavailableException) {
            addLogs("加载视频地址失败：${e.localizedMessage}")
            withContext(Dispatchers.Main) {
                if (isVodPlaybackSessionActive(playbackSessionToken)) {
                    errorMessage = e.localizedMessage ?: "未获取到可播放的视频资源"
                    loadState = RequestState.Failed
                }
            }
            logger.fWarn { "Video play data unavailable: ${e.localizedMessage}" }
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

    private suspend fun handleVVoucher(
        vVoucher: String,
        retryRequest: GeetestPlaybackRetryRequest,
    ) {
        val registrationGeneration = withContext(Dispatchers.Main.immediate) {
            if (!isVodPlaybackSessionActive(retryRequest.playbackSessionToken)) {
                return@withContext null
            }
            ++geetestRegistrationGeneration
        } ?: return
        try {
            val (reservedVoucher, registration) = registerGeetestChallengeOnce(vVoucher)
            val applied = withContext(Dispatchers.Main.immediate) {
                if (
                    !isCurrentGeetestRegistration(
                        registrationGeneration = registrationGeneration,
                        currentRegistrationGeneration = geetestRegistrationGeneration,
                        playbackSessionToken = retryRequest.playbackSessionToken,
                        currentPlaybackSessionToken = vodPlaybackSessionToken,
                    )
                ) {
                    return@withContext false
                }
                pendingGeetestVerification = PendingGeetestVerification(
                    vVoucher = reservedVoucher,
                    token = registration.token,
                    challenge = registration.challenge.trim(),
                    retryRequest = retryRequest,
                )
                geetestValidationPending = null
                geetestGt = registration.gt
                geetestChallenge = registration.challenge
                showGeetestDialog = true
                true
            }
            if (applied) addLogs("请完成人机验证")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val isCurrent = withContext(Dispatchers.Main.immediate) {
                isCurrentGeetestRegistration(
                    registrationGeneration = registrationGeneration,
                    currentRegistrationGeneration = geetestRegistrationGeneration,
                    playbackSessionToken = retryRequest.playbackSessionToken,
                    currentPlaybackSessionToken = vodPlaybackSessionToken,
                )
            }
            if (!isCurrent) {
                logger.fDebug { "Ignore stale Gaia vgate register failure: ${e.message}" }
                return
            }
            addLogs("风控验证申请失败：${e.localizedMessage}")
            withContext(Dispatchers.Main.immediate) {
                errorMessage = "风控验证申请失败：${e.localizedMessage}"
                loadState = RequestState.Failed
            }
            logger.fException(e) { "gaiaVgateRegister failed" }
        }
    }

    private suspend fun registerGeetestChallengeOnce(
        candidate: String,
    ): Pair<String, RegisteredGeetestChallenge> =
        geetestVoucherRegisterMutex.withLock {
            val reservedVoucher = reserveFreshVVoucher(
                attemptedVVouchers = attemptedGeetestVVouchers,
                candidate = candidate,
            )
            reservedVoucher to registerGeetestChallenge(reservedVoucher)
        }

    private suspend fun registerGeetestChallenge(vVoucher: String): RegisteredGeetestChallenge {
        val registerResponse = withContext(Dispatchers.IO) {
            BiliHttpApi.gaiaVgateRegister(
                vVoucher = vVoucher,
                sessData = authRepository.sessionData,
                csrf = authRepository.biliJct
            ).getResponseData()
        }
        return RegisteredGeetestChallenge(
            token = registerResponse.token,
            gt = registerResponse.geetest.gt,
            challenge = registerResponse.geetest.challenge,
        ).also { registration ->
            if (
                registration.token.isBlank() ||
                registration.gt.isBlank() ||
                registration.challenge.isBlank()
            ) {
                error("gaia_vgate_register 返回数据不完整（可能无法通过 captcha 解除）")
            }
        }
    }

    /**
     * Gaia 的 v_voucher 只能注册一次。刷新验证状态时重新请求原播放接口，
     * 从新的风控响应中取得未使用的 voucher；若风控已经解除，则直接复用本次播放数据。
     */
    private suspend fun requestGeetestChallengeRefreshSource(
        pending: PendingGeetestVerification,
    ): GeetestChallengeRefreshSource {
        val retry = pending.retryRequest
        return try {
            val playData = withContext(Dispatchers.IO) {
                fetchPlayableVodPlayData(
                    avid = retry.avid,
                    cid = retry.cid,
                    epid = retry.epid ?: 0,
                    preferApi = retry.preferApi,
                    proxyArea = retry.proxyArea,
                    fromSeason = retry.fromSeason,
                )
            }
            GeetestChallengeRefreshSource.PlayableData(playData)
        } catch (e: VVoucherException) {
            GeetestChallengeRefreshSource.FreshVoucher(e.vVoucher)
        }
    }

    private suspend fun performGeetestChallengeRefresh(
        pending: PendingGeetestVerification,
    ): GeetestChallengeRefreshOutcome {
        repeat(GEETEST_REFRESH_SOURCE_MAX_ATTEMPTS) { attemptIndex ->
            when (val source = requestGeetestChallengeRefreshSource(pending)) {
                is GeetestChallengeRefreshSource.PlayableData -> {
                    return GeetestChallengeRefreshOutcome.PlayableData(source.value)
                }

                is GeetestChallengeRefreshSource.FreshVoucher -> {
                    try {
                        val (vVoucher, registration) =
                            registerGeetestChallengeOnce(source.value)
                        return GeetestChallengeRefreshOutcome.FreshRegistration(
                            vVoucher = vVoucher,
                            registration = registration,
                        )
                    } catch (e: VVoucherAlreadyAttemptedException) {
                        if (attemptIndex == GEETEST_REFRESH_SOURCE_MAX_ATTEMPTS - 1) throw e
                        logger.fDebug {
                            "Protected playback returned an already attempted v_voucher; retrying"
                        }
                        delay(GEETEST_REFRESH_RETRY_BASE_DELAY_MILLIS * (attemptIndex + 1))
                    }
                }
            }
        }
        error("无法获取未使用的 v_voucher")
    }

    /**
     * 本机验证与手机验证不能复用已初始化的 challenge。
     * 切换验证方式，或验证通过后重新取播放数据时，共用一次刷新请求：
     * 风控已解除就直接继续播放，否则取得新的 v_voucher 并原子替换验证状态。
     */
    suspend fun refreshGeetestChallenge(): Boolean {
        val flight = withContext(Dispatchers.Main.immediate) {
            val pending = pendingGeetestVerification ?: return@withContext null
            if (geetestValidationPending != null) return@withContext null
            if (!isVodPlaybackSessionActive(pending.retryRequest.playbackSessionToken)) {
                return@withContext null
            }
            val now = SystemClock.elapsedRealtime()
            val reusableFlight = geetestChallengeRefreshFlight?.takeIf { existing ->
                existing.pending === pending &&
                    !existing.deferred.isCancelled &&
                    (
                        existing.deferred.isActive ||
                            now - existing.startedAtMillis <= GEETEST_REFRESH_FLIGHT_TTL_MILLIS
                    )
            }
            if (reusableFlight != null) return@withContext reusableFlight

            val generation = ++geetestRegistrationGeneration
            val deferred = viewModelScope.async(start = CoroutineStart.LAZY) {
                try {
                    performGeetestChallengeRefresh(pending)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.fException(e) { "Geetest challenge refresh flight failed" }
                    throw e
                }
            }
            GeetestChallengeRefreshFlight(
                generation = generation,
                pending = pending,
                startedAtMillis = now,
                deferred = deferred,
            ).also { createdFlight ->
                geetestChallengeRefreshFlight = createdFlight
                deferred.invokeOnCompletion { cause ->
                    if (cause != null) {
                        viewModelScope.launch {
                            if (geetestChallengeRefreshFlight === createdFlight) {
                                geetestChallengeRefreshFlight = null
                            }
                        }
                    }
                }
                deferred.start()
            }
        } ?: return false

        return try {
            val outcome = flight.deferred.await()
            if (outcome is GeetestChallengeRefreshOutcome.PlayableData) {
                val resumed = withContext(Dispatchers.Main.immediate) {
                    if (
                        flight.generation != geetestRegistrationGeneration ||
                        pendingGeetestVerification !== flight.pending ||
                        geetestValidationPending != null ||
                        geetestChallengeRefreshFlight !== flight ||
                        !isVodPlaybackSessionActive(
                            flight.pending.retryRequest.playbackSessionToken
                        )
                    ) {
                        return@withContext false
                    }
                    geetestChallengeRefreshFlight = null
                    geetestRegistrationGeneration += 1
                    showGeetestDialog = false
                    pendingGeetestVerification = null
                    geetestValidationPending = null
                    errorMessage = ""
                    loadState = RequestState.Ready
                    true
                }
                if (resumed) {
                    addLogs("风控已解除，继续播放")
                    flight.pending.retryRequest.let { retry ->
                        loadPlayUrl(
                            avid = retry.avid,
                            cid = retry.cid,
                            epid = retry.epid,
                            seasonId = retry.seasonId,
                            initialSeekPositionMs = retry.initialSeekPositionMs,
                            preparedPlayData = outcome.value,
                            forceStartPlayback = true,
                        )
                    }
                }
                return resumed
            }

            val freshRegistration = outcome as GeetestChallengeRefreshOutcome.FreshRegistration
            val registration = freshRegistration.registration
            val applied = withContext(Dispatchers.Main.immediate) {
                if (
                    flight.generation != geetestRegistrationGeneration ||
                    pendingGeetestVerification !== flight.pending ||
                    geetestValidationPending != null ||
                    geetestChallengeRefreshFlight !== flight ||
                    !isVodPlaybackSessionActive(
                        flight.pending.retryRequest.playbackSessionToken
                    )
                ) {
                    return@withContext false
                }
                geetestChallengeRefreshFlight = null
                pendingGeetestVerification = flight.pending.copy(
                    vVoucher = freshRegistration.vVoucher,
                    token = registration.token,
                    challenge = registration.challenge.trim(),
                )
                geetestValidationPending = null
                geetestGt = registration.gt
                geetestChallenge = registration.challenge
                true
            }
            if (applied) {
                addLogs("已获取新的人机验证")
                logger.fInfo { "Refreshed Geetest challenge for the active playback session" }
            }
            applied
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val isCurrent = withContext(Dispatchers.Main.immediate) {
                if (geetestChallengeRefreshFlight === flight) {
                    geetestChallengeRefreshFlight = null
                }
                flight.generation == geetestRegistrationGeneration &&
                    pendingGeetestVerification === flight.pending &&
                    isVodPlaybackSessionActive(
                        flight.pending.retryRequest.playbackSessionToken
                    )
            }
            if (isCurrent) {
                addLogs("刷新人机验证失败：${e.localizedMessage}")
            }
            false
        }
    }

    fun onGeetestResult(
        challenge: String,
        validate: String,
        seccode: String,
        sourceChallenge: String? = null,
    ) {
        // TV WebView/HTTP 回调可能来自后台线程。先进入 ViewModel 主线程，
        // 再读取待验证状态，避免读不到 token 后静默返回。
        viewModelScope.launch {
            val pending = pendingGeetestVerification
            if (pending == null) {
                logger.fWarn { "Ignore Geetest result because no verification is pending" }
                return@launch
            }
            if (!isVodPlaybackSessionActive(pending.retryRequest.playbackSessionToken)) {
                logger.fWarn { "Ignore Geetest result from stale vod playback session" }
                return@launch
            }
            val resultChallenge = validatedGeetestResultChallengeOrNull(
                expectedSourceChallenge = pending.challenge,
                sourceChallenge = sourceChallenge,
                resultChallenge = challenge,
            )
            if (resultChallenge == null) {
                logger.fWarn { "Ignore invalid or stale Geetest result" }
                return@launch
            }
            if (geetestValidationPending != null) {
                logger.fDebug { "Ignore duplicated Geetest result while validation is in progress" }
                return@launch
            }
            geetestValidationPending = pending

            try {
                val validateResponse = withContext(Dispatchers.IO) {
                    BiliHttpApi.gaiaVgateValidate(
                        token = pending.token,
                        geetestChallenge = resultChallenge,
                        validate = validate,
                        seccode = seccode,
                        sessData = authRepository.sessionData,
                        csrf = authRepository.biliJct
                    ).getResponseData()
                }
                if (validateResponse.isValid != 1) {
                    error("验证未通过")
                }
                val griskId = validateResponse.griskId
                if (griskId.isBlank()) {
                    error("grisk_id 为空")
                }

                // 验证期间若用户已取消或已发起新验证，不再重试旧请求。
                if (
                    pendingGeetestVerification !== pending ||
                    !isVodPlaybackSessionActive(
                        pending.retryRequest.playbackSessionToken
                    )
                ) {
                    if (geetestValidationPending === pending) geetestValidationPending = null
                    logger.fWarn { "Skip stale Geetest validation result" }
                    return@launch
                }

                authRepository.gaiaVtoken = griskId
                geetestRegistrationGeneration += 1
                val reboundPending = rebindGeetestToNextVodPlaybackSession(pending)
                errorMessage = ""
                loadState = RequestState.Ready
                addLogs("风控验证通过")
                logger.fInfo {
                    "Gaia vgate validate success, refreshing protected play data"
                }
                val refreshed = refreshGeetestChallenge()
                if (
                    !refreshed &&
                    pendingGeetestVerification === reboundPending &&
                    isVodPlaybackSessionActive(
                        reboundPending.retryRequest.playbackSessionToken
                    )
                ) {
                    geetestRegistrationGeneration += 1
                    showGeetestDialog = false
                    pendingGeetestVerification = null
                    geetestValidationPending = null
                    errorMessage = "验证已通过，但刷新播放地址失败，请重试播放"
                    loadState = RequestState.Failed
                    addLogs(errorMessage)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (pendingGeetestVerification !== pending) {
                    if (geetestValidationPending === pending) geetestValidationPending = null
                    logger.fWarn { "Ignore stale Geetest validation failure: ${e.message}" }
                    return@launch
                }
                geetestRegistrationGeneration += 1
                geetestValidationPending = null
                addLogs("风控验证失败：${e.localizedMessage}")
                errorMessage = "风控验证失败：${e.localizedMessage}"
                loadState = RequestState.Failed
                showGeetestDialog = false
                pendingGeetestVerification = null
                logger.fException(e) { "gaiaVgateValidate failed" }
            }
        }
    }

    fun onGeetestCancelled() {
        viewModelScope.launch {
            // 成功后 SDK/WebView 可能还会补发关闭回调，不能把已开始的重试改回失败。
            if (pendingGeetestVerification == null) return@launch
            geetestRegistrationGeneration += 1
            geetestChallengeRefreshFlight?.deferred?.cancel()
            geetestChallengeRefreshFlight = null
            showGeetestDialog = false
            pendingGeetestVerification = null
            geetestValidationPending = null
            errorMessage = "验证已取消"
            loadState = RequestState.Failed
            addLogs("用户取消了风控验证")
        }
    }

    private suspend fun fetchPlayableVodPlayData(
        avid: Long,
        cid: Long,
        epid: Int,
        preferApi: ApiType,
        proxyArea: ProxyArea,
        fromSeason: Boolean = this.fromSeason,
    ): PlayData = resolvePlayableVodPlayData(
        preferredApi = preferApi,
        fetch = { api ->
            fetchVodPlayDataOnce(
                avid = avid,
                cid = cid,
                epid = epid,
                preferApi = api,
                proxyArea = proxyArea,
                fromSeason = fromSeason,
            )
        },
        onFailure = { api, failure ->
            logger.fWarn {
                "${api.playUrlSourceName()} play data failed: " +
                    "[av=$avid, cid=$cid, reason=${failure.localizedMessage}]"
            }
        },
        onEmpty = { api, playData ->
            logger.fWarn {
                "${api.playUrlSourceName()} play data has empty streams: " +
                    "[av=$avid, cid=$cid, video=${playData.dashVideos.size}, " +
                    "audio=${playData.playableAudioCount()}]"
            }
        },
        onFallback = { fromApi, fallbackApi ->
            addLogs("${fromApi.playUrlSourceName()}未返回可播放流，尝试${fallbackApi.playUrlSourceName()}")
        },
        onFallbackSuccess = { api ->
            logger.fInfo { "${api.playUrlSourceName()} fallback success: [av=$avid, cid=$cid]" }
        }
    )

    private suspend fun fetchVodPlayDataOnce(
        avid: Long,
        cid: Long,
        epid: Int,
        preferApi: ApiType,
        proxyArea: ProxyArea,
        fromSeason: Boolean = this.fromSeason,
    ): PlayData {
        return if (fromSeason) {
            videoPlayRepository.getPgcPlayData(
                aid = avid,
                cid = cid,
                epid = epid,
                preferCodec = settings.defaultVideoCodec.toBiliApiCodeType(),
                preferApiType = preferApi,
                enableProxy = proxyArea != ProxyArea.MainLand,
                proxyArea = when (proxyArea) {
                    ProxyArea.MainLand -> ""
                    ProxyArea.HongKong -> "hk"
                    ProxyArea.TaiWan -> "tw"
                },
                tryLook1080P = settings.tryLook1080P,
            )
        } else {
            videoPlayRepository.getPlayData(
                aid = avid,
                cid = cid,
                bvid = AvBvConverter.av2bv(avid),
                preferApiType = preferApi,
                tryLook1080P = settings.tryLook1080P,
            )
        }
    }

    private suspend fun loadOfflinePlaybackSource(
        source: OfflineVideoPlaybackSource,
        initialSeekPositionMs: Long?,
        targetMediaMode: PlaybackMediaMode,
        playbackSessionToken: Long,
    ): Boolean {
        ensureVodPlaybackSessionActive(playbackSessionToken)
        val entry = source.entry
        val playData = source.playData
        val resolution = Resolution.fromCode(entry.quality) ?: Resolution.R1080P
        val videoCodec = VideoCodec.fromCodecString(entry.videoCodec)
            ?: VideoCodec.fromCodecId(entry.videoCodecId)
        val audio = Audio.fromCode(entry.audioCodecId) ?: Audio.A64K

        withContext(Dispatchers.Main) {
            if (!isVodPlaybackSessionActive(playbackSessionToken)) return@withContext
            currentPlaybackOffline = true
            videoPlayer?.setOfflinePlaybackMode(true)
            needPay = false
            this@VideoPlayerV3ViewModel.playData = playData
            clipInfoList = emptyList()
            currentQuality = resolution
            currentVideoCodec = videoCodec
            currentAudio = audio
            currentVideoHeight = entry.height
            currentVideoWidth = entry.width
            availableQuality.swapList(listOf(resolution))
            availableVideoCodec.swapList(listOf(videoCodec))
            availableAudio.swapList(listOf(audio))
        }

        addLogs("使用离线缓存播放：${entry.qualityText.ifBlank { resolution.name }}")
        ensureVodPlaybackSessionActive(playbackSessionToken)
        playQuality(
            qn = resolution.code,
            codec = videoCodec,
            audio = audio,
            mediaMode = targetMediaMode,
            initialSeekPositionMs = initialSeekPositionMs,
            playbackSessionToken = playbackSessionToken
        )

        withContext(Dispatchers.Main) {
            if (isVodPlaybackSessionActive(playbackSessionToken)) {
                loadState = RequestState.Success
            }
        }
        return true
    }

    private fun offlineCacheCodecCandidates(currentCodec: VideoCodec): List<VideoCodec> =
        listOf(VideoCodec.AVC, settings.defaultVideoCodec, settings.secondVideoCodec, currentCodec).distinct()

    private fun PlayData.requireOfflineCacheStreams(source: String): PlayData {
        val audioCount = playableAudioCount()
        if (dashVideos.isEmpty() || audioCount == 0) {
            throw IllegalStateException(
                "$source 未返回可缓存音视频流：video=${dashVideos.size}, audio=$audioCount, qualities=${dashVideos.map { it.quality }.distinct()}"
            )
        }
        return this
    }

    private suspend fun updateAvailableCodec(preferredCodec: VideoCodec? = null) {
        if (settings.apiType == ApiType.App && playData!!.codec.isEmpty()) {
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
        val codecList = (
            supportedCodec[currentQuality.code].orEmpty() +
                playData!!.dashVideos
                    .filter { it.quality == currentQuality.code }
                    .mapNotNull { it.codecs }
            )
            .mapNotNull { VideoCodec.fromCodecString(it) }
            .distinct()

        val orderedCodecList = if (settings.useTvVideoCodecPriority) {
            PlaybackPreferenceSelector.orderAvailableVideoCodecs(
                availableCodecs = codecList,
                defaultCodec = settings.defaultVideoCodec,
                selectedQuality = currentQuality,
                h265CodecPriority = settings.h265CodecPriority,
            )
        } else {
            codecList
        }
        availableVideoCodec.swapListWithMainContext(orderedCodecList)
        logger.fInfo { "Video available codec: ${availableVideoCodec.toList()}" }

        val requestedCodec = preferredCodec ?: currentVideoCodec
        logger.fInfo {
            "Default codec: $requestedCodec, second codec: ${settings.secondVideoCodec}, " +
                "h265 priority: ${settings.h265CodecPriority.joinToString(">") { it.name }}"
        }
        val currentVideoCodec = if (settings.useTvVideoCodecPriority) {
            PlaybackPreferenceSelector.selectTvVideoCodec(
                requestedCodec = preferredCodec,
                currentCodec = currentVideoCodec,
                defaultCodec = settings.defaultVideoCodec,
                selectedQuality = currentQuality,
                availableCodecs = orderedCodecList,
                h265CodecPriority = settings.h265CodecPriority,
            )
        } else {
            PlaybackPreferenceSelector.selectVideoCodec(
                requestedCodec = preferredCodec,
                currentCodec = currentVideoCodec,
                defaultCodec = settings.defaultVideoCodec,
                secondCodec = settings.secondVideoCodec,
                availableCodecs = codecList
            )
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
            when (settings.apiType) {
                ApiType.Web -> it.quality == qn && codec.matchesCodecString(it.codecs)
                ApiType.App -> {
                    if (playData!!.codec.isEmpty()) it.quality == qn
                    else it.quality == qn && codec.matchesCodecString(it.codecs)
                }
            }
        }
        val selectedVideoItem = videoItem ?: playData!!.dashVideos.firstOrNull()
        val muxedVideo = selectedVideoItem?.takeIf { it.isMuxed }
        var videoUrl = if (audioOnlyMode) null else selectedVideoItem?.baseUrl
        if (!audioOnlyMode && videoUrl == null) {
            logger.fError { "Failed to get video URL" }
            errorMessage = "获取视频地址失败"
            loadState = RequestState.Failed
            return
        }
        val videoUrls = mutableListOf<String?>()
        if (!audioOnlyMode) {
            videoUrls.add(videoUrl)
            videoUrls.addAll(selectedVideoItem?.backUrl ?: emptyList())
        }

        val audioItem = playData!!.dashAudios.find { it.codecId == audio.code }
            ?: playData!!.dolby.takeIf { it?.codecId == audio.code }
            ?: playData!!.flac.takeIf { it?.codecId == audio.code }
            ?: playData!!.dashAudios.minByOrNull { it.codecId }
        var audioUrl = audioItem?.baseUrl
            ?: playData!!.dashAudios.firstOrNull()?.baseUrl
            ?: muxedVideo?.baseUrl.takeIf { audioOnlyMode }
        if (audioUrl == null && muxedVideo == null) {
            logger.fError { "Failed to get audio URL" }
            errorMessage = "获取音频地址失败"
            loadState = RequestState.Failed
            return
        }
        val audioUrls = mutableListOf<String?>()
        if (audioUrl != null) {
            audioUrls.add(audioUrl)
            audioUrls.addAll(
                audioItem?.backUrl
                    ?: muxedVideo?.backUrl.takeIf { audioOnlyMode }
                    ?: emptyList()
            )
        }

        if (videoUrls.isNotEmpty()) {
            logger.fInfo { "all video hosts: ${videoUrls.filterNotNull().map { it.toMediaLocationLog() }}" }
        }
        if (audioUrls.isNotEmpty()) {
            logger.fInfo { "all audio hosts: ${audioUrls.filterNotNull().map { it.toMediaLocationLog() }}" }
        } else {
            logger.fInfo { "Use audio embedded in video stream" }
        }

        var videoCdnSelection: VodCdnSelection? = null
        var audioCdnSelection: VodCdnSelection? = null

        //replace cdn
        if (Prefs.enableProxy && proxyArea != ProxyArea.MainLand) {
            if (videoUrl != null) {
                videoUrl = videoUrl.replaceUrlDomainWithAliCdn()
            }
            audioUrl = audioUrl?.replaceUrlDomainWithAliCdn()
        } else {
            // 如果未通过网络代理获得播放地址，才判断是否应该替换为官方 cdn
            if (videoUrls.isNotEmpty()) {
                videoCdnSelection = VodCdnUrlSelector.select(videoUrls, settings.cdnService)
                videoUrl = videoCdnSelection.url
            }
            if (audioUrls.isNotEmpty()) {
                audioCdnSelection = VodCdnUrlSelector.select(
                    urls = audioUrls,
                    cdnService = settings.cdnService,
                    isAudio = true,
                    disableAudioCdn = settings.disableAudioCdn
                )
                audioUrl = audioCdnSelection.url
            }
        }

        addLogs(
            "播放模式：${if (audioOnlyMode) "音频模式" else "正常模式"}，播放清晰度：${availableQuality.firstOrNull { it.code == qn }}, " +
                    "视频编码：${codec.getDisplayName(BVApp.context)}, " +
                    "音频编码：${if (muxedVideo != null) "内嵌音频" else (Audio.fromCode(audioItem?.codecId ?: 0))?.getDisplayName(BVApp.context) ?: "未知"}"
        )
        if (videoUrl != null) {
            addLogs("video host: ${videoUrl.toMediaLocationLog()}")
            videoCdnSelection?.let { addLogs("video cdn: ${it.reason}") }
        } else {
            addLogs("video host: audio only")
        }
        if (audioUrl != null) {
            addLogs("audio host: ${audioUrl.toMediaLocationLog()}")
        } else {
            addLogs("audio host: embedded in video")
        }
        audioCdnSelection?.let { addLogs("audio cdn: ${it.reason}") }

        logger.fInfo { "Select audio: $audioItem" }

        val playUrlExpiresAt = pickEarliestUrlExpiryEpochMs(videoUrl, audioUrl)

        withContext(Dispatchers.Main) {
            if (!isVodPlaybackSessionActive(playbackSessionToken)) return@withContext
            currentVideoHeight = selectedVideoItem?.height ?: 0
            currentVideoWidth = selectedVideoItem?.width ?: 0
            val historyPositionMs = lastPlayed
                .takeIf {
                    it > 0 &&
                        settings.playerDefaultStartPosition == PlayerDefaultStartPosition.History
                }
                ?.toLong()
            val startPositionMs = resolveVodPlaybackStartPositionMs(
                explicitPositionMs = initialSeekPositionMs,
                historyPositionMs = historyPositionMs,
                durationMs = playData?.timeLength ?: 0L
            )
            resolvedVodStartPositionMs = startPositionMs
            hasResolvedVodStartPosition = true
            resolvedVodStartPositionSessionToken = playbackSessionToken
            val playbackSource = PendingVodPlaybackSource(
                videoUrl = videoUrl,
                audioUrl = audioUrl,
                startPositionMs = startPositionMs,
                playbackSessionToken = playbackSessionToken,
            )
            if (!settings.autoPlay && !manualVodPlaybackRequested) {
                logger.info { "Defer vod playback prepare until manual play" }
                pendingVodPlaybackSource = playbackSource
                showBuffering = false
                return@withContext
            }

            pendingVodPlaybackSource = null
            applyVodPlaybackSource(
                playbackSource = playbackSource,
                startWhenReady = !settings.autoPlay && manualVodPlaybackRequested,
            )
        }

        if (playUrlExpiresAt > 0L) {
            scheduleVodPlayUrlAutoRefresh(playUrlExpiresAt, playbackSessionToken)
        } else {
            cancelVodPlayUrlAutoRefresh()
            logger.fWarn { "Skip vod URL auto refresh because no deadline/expires was parsed" }
        }
    }

    private fun String.toMediaLocationLog(): String =
        runCatching {
            val uri = URI(this)
            if (uri.scheme.equals("file", ignoreCase = true)) {
                "file://${uri.path?.substringAfterLast('/')?.ifBlank { uri.path } ?: "<local>"}"
            } else {
                "${uri.scheme}://${uri.authority}"
            }
        }.getOrDefault(this)

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

    private fun DanmakuLoadSession.loadedDanmakuSlices(segmentIndex: Int): List<DanmakuSlice> {
        return when (val cacheEntry = danmakuSegmentCacheBySegment[segmentIndex]) {
            is DanmakuSegmentCacheEntry.Loaded -> cacheEntry.value
            else -> emptyList()
        }
    }

    suspend fun loadDanmaku(
        cid: Long,
        durationMs: Long = 0,
        initialPositionMsOverride: Long? = null,
        reuseSegmentCache: Boolean = false
    ) {
        danmakuLoadJob?.cancelAndJoin()
        danmakuCatchUpJob?.cancelAndJoin()
        val loadToken = ++danmakuLoadToken
        val loadSession = prepareDanmakuLoadSession(
            cid = cid,
            loadToken = loadToken,
            reuseSegmentCache = reuseSegmentCache
        )
        danmakuLoadSession = loadSession
        runCatching {
            withContext(Dispatchers.Main) {
                danmakuData.clear()
                danmakuPlayer?.clearData()
                lastDanmakuCatchUpPositionMs = -1L
                lastDanmakuCatchUpSegment = -1
            }

            val requestedPositionMs = initialPositionMsOverride ?: withContext(Dispatchers.Main) {
                videoPlayer?.currentPosition?.takeIf { it > 0L }
            } ?: calculateInitialDanmakuPositionMs()
            val initialLoadPlan = buildInitialDanmakuLoadPlan(
                requestedPositionMs = requestedPositionMs,
                durationMs = durationMs,
                initialSegmentPrefetch = INITIAL_DANMAKU_SEGMENT_PREFETCH
            )
            val initialPositionMs = initialLoadPlan.startPositionMs
            val startSegment = initialLoadPlan.startSegment
            val segmentOrder = initialLoadPlan.segmentOrder
            if (segmentOrder.isEmpty()) {
                addDanmakuLog("未找到可加载的弹幕分段")
                return@runCatching
            }

            var initialEmittedDanmaku = 0
            ensureDanmakuLoadActive(loadToken)
            val startSegmentSlices = loadDanmakuSegmentSlices(
                cid = cid,
                segmentIndex = startSegment,
                loadToken = loadToken,
                loadSession = loadSession,
                forceRetryTransient = true,
                finalizeMergeForImmediateDisplay = true
            )
            if (startSegmentSlices.isNotEmpty()) {
                val initialSliceIndex = findDanmakuSliceIndexForPosition(
                    slices = startSegmentSlices,
                    positionMs = initialPositionMs
                )
                if (initialSliceIndex < startSegmentSlices.size) {
                    val initialEndExclusive = findDanmakuSliceEndExclusiveForCoverage(
                        slices = startSegmentSlices,
                        startSliceIndex = initialSliceIndex,
                        positionMs = initialPositionMs,
                        minSliceCount = INITIAL_DANMAKU_SLICE_PREFETCH
                    )
                    initialEmittedDanmaku += emitDanmakuSlices(
                        segmentIndex = startSegment,
                        slices = startSegmentSlices,
                        loadSession = loadSession,
                        startSliceIndex = initialSliceIndex,
                        endExclusive = initialEndExclusive,
                        loadToken = loadToken
                    )
                }
            }
            if (initialEmittedDanmaku > 0) {
                syncDanmakuPlayerAfterInitialData(initialPositionMs, loadToken)
            }
            loadSession.initialSegmentReady.set(true)

            pruneDanmakuSegmentCache(loadSession, startSegment)
            val cachedSegments = if (startSegmentSlices.isNotEmpty()) 1 else 0
            addDanmakuLog("已缓存 $cachedSegments 个弹幕分段，优先投喂 ${initialEmittedDanmaku} 条")

            val backgroundPrefetchSegments = initialLoadPlan.backgroundPrefetchSegments
            if (
                backgroundPrefetchSegments.isNotEmpty() ||
                loadSession.preloadPolicy.fullVideoPreloadEnabled
            ) {
                danmakuLoadJob = viewModelScope.launch(Dispatchers.Default) {
                    runCatching {
                        var backgroundEmittedDanmaku = 0
                        backgroundPrefetchSegments.forEach { segmentIndex ->
                            val slices = loadDanmakuSegmentSlices(
                                cid = cid,
                                segmentIndex = segmentIndex,
                                loadToken = loadToken,
                                loadSession = loadSession
                            )
                            logger.info {
                                "Background prefetched danmaku segment=$segmentIndex, slices=${slices.size}"
                            }
                            if (initialEmittedDanmaku == 0 && slices.isNotEmpty()) {
                                val fallbackEmittedDanmaku = emitDanmakuSlices(
                                    segmentIndex = segmentIndex,
                                    slices = slices,
                                    loadSession = loadSession,
                                    startSliceIndex = 0,
                                    endExclusive = INITIAL_DANMAKU_SLICE_PREFETCH,
                                    loadToken = loadToken
                                )
                                backgroundEmittedDanmaku += fallbackEmittedDanmaku
                                if (fallbackEmittedDanmaku > 0) {
                                    syncDanmakuPlayerAfterInitialData(initialPositionMs, loadToken)
                                }
                            }
                        }

                        if (loadSession.preloadPolicy.fullVideoPreloadEnabled) {
                            segmentOrder.forEach { segmentIndex ->
                                ensureDanmakuLoadActive(loadToken)
                                if (!hasEnoughMemoryForFullDanmakuPreload(loadSession.preloadPolicy)) {
                                    logger.info {
                                        "Stop full danmaku preload because memory is tight, segmentIndex=$segmentIndex, emitted=$backgroundEmittedDanmaku"
                                    }
                                    return@runCatching
                                }
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
                                    logger.info {
                                        "Background emitted danmaku segment=$segmentIndex, emitted=$emittedCount, retained=${danmakuData.size}"
                                    }
                                }
                                pruneDanmakuSegmentCache(loadSession, startSegment)
                            }
                            backgroundEmittedDanmaku += flushPendingMergedDanmaku(loadSession, loadToken)
                            logger.info { "Full danmaku preload finished, emitted=$backgroundEmittedDanmaku" }
                        }
                    }.onFailure {
                        when (it) {
                            is OutOfMemoryError -> handleDanmakuOutOfMemory(loadSession, loadToken, "后台补齐全片弹幕")
                            is kotlinx.coroutines.CancellationException -> Unit
                            else -> {
                                addDanmakuLog("后台补齐弹幕失败：${it.localizedMessage}")
                                logger.fWarn { "Background danmaku loading failed: ${it.stackTraceToString()}" }
                            }
                        }
                    }
                }
            } else {
                logger.info {
                    "No adjacent danmaku segment to prefetch; full preload disabled by memory policy: ${loadSession.preloadPolicy}"
                }
            }
        }.onFailure {
            when (it) {
                is OutOfMemoryError -> handleDanmakuOutOfMemory(loadSession, loadToken, "加载弹幕")
                is kotlinx.coroutines.CancellationException -> Unit
                else -> {
                    addDanmakuLog("加载弹幕失败：${it.localizedMessage}")
                    logger.fWarn { "Load danmaku filed: ${it.stackTraceToString()}" }
                }
            }
        }.onSuccess {
            addDanmakuLog("已启动弹幕切片加载，当前 ${danmakuData.size} 条")
            logger.info {
                "Load danmaku slices started, size=${danmakuData.size}, policy=${loadSession.preloadPolicy}"
            }
        }
    }

    private fun resolveCurrentDanmakuPreloadPolicy(): DanmakuPreloadPolicy {
        val activityManager = BVApp.context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        return resolveDanmakuPreloadPolicy(
            totalMemoryBytes = memoryInfo.totalMem,
            availableMemoryBytes = memoryInfo.availMem,
            isLowRamDevice = activityManager?.isLowRamDevice == true,
            maxHeapBytes = Runtime.getRuntime().maxMemory()
        )
    }

    private fun hasEnoughMemoryForFullDanmakuPreload(policy: DanmakuPreloadPolicy): Boolean {
        val runtime = Runtime.getRuntime()
        val usedHeap = runtime.totalMemory() - runtime.freeMemory()
        val freeHeap = runtime.maxMemory() - usedHeap
        if (freeHeap < policy.minFreeHeapBytesForFullPreload) return false

        val activityManager = BVApp.context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return true
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return !memoryInfo.lowMemory && memoryInfo.availMem >= policy.minAvailableMemoryBytesForFullPreload
    }

    private fun handleDanmakuOutOfMemory(
        loadSession: DanmakuLoadSession?,
        loadToken: Long,
        reason: String
    ) {
        if (loadToken != danmakuLoadToken) return
        danmakuLoadToken++
        loadSession?.loadedDanmakuIds?.clear()
        loadSession?.danmakuSegmentCacheBySegment?.clear()
        loadSession?.nextDanmakuSliceIndexBySegment?.clear()
        loadSession?.vodDanmakuMergeState?.clear()
        viewModelScope.launch(Dispatchers.Main) {
            danmakuData.clear()
            danmakuPlayer?.clearData()
        }
        logger.warn { "Danmaku loading stopped after OOM: $reason" }
    }

    private fun prepareDanmakuLoadSession(
        cid: Long,
        loadToken: Long,
        reuseSegmentCache: Boolean
    ): DanmakuLoadSession {
        val preloadPolicy = resolveCurrentDanmakuPreloadPolicy()
        val existingSession = danmakuLoadSession?.takeIf {
            reuseSegmentCache &&
                it.aid == currentAid &&
                it.cid == cid &&
                it.filterLevel == currentDanmakuFilterLevel &&
                it.mergeEnabled == currentDanmakuMergeEnabled
        }

        return if (existingSession != null) {
            existingSession.token = loadToken
            existingSession.preloadPolicy = preloadPolicy
            existingSession.initialSegmentReady.set(false)
            existingSession.rebuildLoadedDanmakuIdsFromCachedSlices()
            existingSession.nextDanmakuSliceIndexBySegment.clear()
            existingSession
        } else {
            DanmakuLoadSession(
                token = loadToken,
                aid = currentAid,
                cid = cid,
                filterLevel = currentDanmakuFilterLevel,
                mergeEnabled = currentDanmakuMergeEnabled,
                preloadPolicy = preloadPolicy
            )
        }
    }

    private fun DanmakuLoadSession.rebuildLoadedDanmakuIdsFromCachedSlices() {
        loadedDanmakuIds.clear()
        danmakuSegmentCacheBySegment.values.forEach { cacheEntry ->
            if (cacheEntry is DanmakuSegmentCacheEntry.Loaded) {
                cacheEntry.value.forEach { slice ->
                    slice.items.forEach { item ->
                        loadedDanmakuIds.add(item.danmakuId)
                    }
                }
            }
        }
        trimLoadedDanmakuIds(this)
    }

    fun reloadDanmakuAfterSeek(positionMs: Long, shouldPlay: Boolean) {
        if (isLive || currentCid <= 0L) return

        val durationMs = playData?.timeLength
            ?.takeIf { it > 0L }
            ?: videoPlayer?.duration
            ?: 0L

        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                addDanmakuLog("跳转后重载弹幕")
                val reusedExistingPlayer = withContext(Dispatchers.Main) {
                    danmakuPlayer?.let {
                        it.pause()
                        it.clearData()
                        it.seekTo(positionMs)
                        true
                    } ?: false
                }
                if (!reusedExistingPlayer) {
                    ensureDanmakuPlayer()
                }
                loadDanmaku(
                    cid = currentCid,
                    durationMs = durationMs,
                    initialPositionMsOverride = positionMs,
                    reuseSegmentCache = true
                )
                withContext(Dispatchers.Main) {
                    danmakuPlayer?.seekTo(positionMs)
                    if (shouldPlay || videoPlayer?.isPlaying == true) {
                        danmakuPlayer?.start()
                    } else {
                        danmakuPlayer?.pause()
                    }
                }
            }.onFailure {
                if (it !is kotlinx.coroutines.CancellationException) {
                    logger.fWarn { "Reload danmaku after seek failed: ${it.stackTraceToString()}" }
                    addDanmakuLog("跳转后重载弹幕失败：${it.localizedMessage}")
                }
            }
        }
    }

    fun resyncDanmakuAfterPlayerBound(positionMs: Long, shouldPlay: Boolean) {
        if (isLive) return

        viewModelScope.launch(Dispatchers.Main.immediate) {
            val player = danmakuPlayer ?: return@launch
            danmakuPlayerDataMutex.withLock {
                val retainedItems = danmakuData.toList()
                if (retainedItems.isNotEmpty()) {
                    player.clearData()
                    retainedItems.chunked(DANMAKU_BATCH_SIZE).forEach { chunk ->
                        player.updateData(chunk)
                        yield()
                    }
                }

                val safePosition = if (retainedItems.isEmpty()) {
                    0L
                } else {
                    positionMs.coerceAtLeast(0L)
                }
                player.seekTo(safePosition)
                if (shouldPlay) {
                    player.start()
                } else {
                    player.pause()
                }
                logger.info {
                    "Resynced danmaku after player bound, retained=${retainedItems.size}, positionMs=$safePosition, shouldPlay=$shouldPlay"
                }
            }
        }
    }

    fun ensureDanmakuCoverage(positionMs: Long) {
        if (isLive || currentCid <= 0L || positionMs < 0L) return

        val durationMs = playData?.timeLength
            ?.takeIf { it > 0L }
            ?: videoPlayer?.duration
            ?.takeIf { it > 0L }
            ?: 0L
        val normalizedPosition = resolveVodPlaybackStartPositionMs(
            explicitPositionMs = positionMs,
            historyPositionMs = null,
            durationMs = durationMs
        )
        val currentSegment = calculateDanmakuSegment(normalizedPosition)
        val loadToken = danmakuLoadToken
        val loadSession = danmakuLoadSession?.takeIf { it.token == loadToken }
        if (loadSession == null || !loadSession.initialSegmentReady.get()) return
        val shouldSkip = currentSegment == lastDanmakuCatchUpSegment &&
            lastDanmakuCatchUpPositionMs >= 0L &&
            normalizedPosition - lastDanmakuCatchUpPositionMs < DANMAKU_COVERAGE_RECHECK_INTERVAL_MS
        if (shouldSkip) return

        val previousCatchUpPositionMs = lastDanmakuCatchUpPositionMs
        val catchUpJobActive = danmakuCatchUpJob?.isActive == true

        if (catchUpJobActive) {
            logger.info {
                "Danmaku coverage catch-up skipped because previous job is active positionMs=$normalizedPosition, currentSegment=$currentSegment, lastPosition=$previousCatchUpPositionMs"
            }
            return
        }

        lastDanmakuCatchUpPositionMs = normalizedPosition
        lastDanmakuCatchUpSegment = currentSegment

        val maxSegments = calculateDanmakuMaxSegments(durationMs, normalizedPosition)

        logger.info {
            "Danmaku coverage catch-up start positionMs=$normalizedPosition, currentSegment=$currentSegment, lastPosition=$previousCatchUpPositionMs, jobActive=$catchUpJobActive"
        }

        danmakuCatchUpJob = viewModelScope.launch(Dispatchers.Default) {
            val catchUpStartMs = System.currentTimeMillis()
            runCatching {
                var totalEmitted = 0
                val targetSegments = buildList {
                    add(currentSegment.coerceIn(1, maxSegments.coerceAtLeast(1)))
                    val nextSegment = currentSegment + 1
                    if (nextSegment <= maxSegments) add(nextSegment)
                }
                targetSegments.forEachIndexed { index, segmentIndex ->
                    ensureDanmakuLoadActive(loadToken)
                    val anchorPosition = if (index == 0) normalizedPosition else segmentStartPositionMs(segmentIndex)
                    val segmentStartMs = System.currentTimeMillis()
                    val emittedCount = emitDanmakuSlicesAroundPosition(
                        cid = currentCid,
                        segmentIndex = segmentIndex,
                        positionMs = anchorPosition,
                        loadToken = loadToken,
                        loadSession = loadSession,
                        forceRetryTransient = index == 0,
                        finalizeMergeForImmediateDisplay = index == 0
                    )
                    totalEmitted += emittedCount
                    logger.info {
                        "Danmaku coverage segment done segmentIndex=$segmentIndex, anchorPosition=$anchorPosition, emittedCount=$emittedCount, costMs=${System.currentTimeMillis() - segmentStartMs}"
                    }
                }
                pruneDanmakuSegmentCache(loadSession, currentSegment)
                logger.info {
                    "Danmaku coverage catch-up done totalEmitted=$totalEmitted, costMs=${System.currentTimeMillis() - catchUpStartMs}"
                }
            }.onFailure {
                when (it) {
                    is OutOfMemoryError -> handleDanmakuOutOfMemory(loadSession, loadToken, "按需补齐弹幕")
                    is CancellationException -> Unit
                    else -> logger.fDebug {
                        "Danmaku catch-up skipped: ${it.message}, costMs=${System.currentTimeMillis() - catchUpStartMs}"
                    }
                }
            }
        }
    }

    private fun calculateInitialDanmakuPositionMs(): Long {
        return if (
            lastPlayed > 0 &&
            settings.playerDefaultStartPosition == PlayerDefaultStartPosition.History
        ) {
            lastPlayed.toLong()
        } else {
            0L
        }
    }

    private fun segmentStartPositionMs(segmentIndex: Int): Long {
        return ((segmentIndex - 1).coerceAtLeast(0)) * DANMAKU_SEGMENT_DURATION_MS
    }

    private suspend fun loadDanmakuSegmentSlices(
        cid: Long,
        segmentIndex: Int,
        loadToken: Long,
        loadSession: DanmakuLoadSession,
        forceRetryTransient: Boolean = false,
        finalizeMergeForImmediateDisplay: Boolean = false
    ): List<DanmakuSlice> = loadSession.segmentLoadMutex.withLock {
        val nowMs = System.currentTimeMillis()
        val cacheEntry = loadSession.danmakuSegmentCacheBySegment[segmentIndex]
        val shouldFetchCacheEntry = cacheEntry.shouldFetchDanmakuSegment(nowMs) ||
            (forceRetryTransient && cacheEntry is DanmakuSegmentCacheEntry.TransientFailed)
        if (!shouldFetchCacheEntry) {
            logDanmakuSegmentCache(
                cid = cid,
                segmentIndex = segmentIndex,
                cacheEntry = cacheEntry,
                attempts = cacheEntry?.cacheAttempts ?: 0
            )
            return when (cacheEntry) {
                is DanmakuSegmentCacheEntry.Loaded -> cacheEntry.value
                else -> emptyList()
            }
        }

        return try {
            loadDanmakuSegmentSlicesInternal(
                cid = cid,
                segmentIndex = segmentIndex,
                loadToken = loadToken,
                loadSession = loadSession,
                finalizeMergeForImmediateDisplay = finalizeMergeForImmediateDisplay
            )
        } catch (error: OutOfMemoryError) {
            handleDanmakuOutOfMemory(loadSession, loadToken, "加载第 $segmentIndex 段弹幕")
            throw CancellationException("Danmaku load stopped after OOM")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val failedEntry = nextFailedDanmakuSegmentCacheEntry(
                previous = cacheEntry,
                nowMs = System.currentTimeMillis(),
                source = "web",
                reason = error.javaClass.simpleName.ifBlank { "Exception" }
            )
            loadSession.danmakuSegmentCacheBySegment[segmentIndex] = failedEntry
            loadSession.nextDanmakuSliceIndexBySegment.remove(segmentIndex)
            logDanmakuSegmentCache(
                cid = cid,
                segmentIndex = segmentIndex,
                cacheEntry = failedEntry,
                attempts = failedEntry.cacheAttempts
            )
            logger.warn { "Load danmaku segment failed segmentIndex=$segmentIndex: ${error.stackTraceToString()}" }
            emptyList()
        }
    }

    private suspend fun loadDanmakuSegmentSlicesInternal(
        cid: Long,
        segmentIndex: Int,
        loadToken: Long,
        loadSession: DanmakuLoadSession,
        finalizeMergeForImmediateDisplay: Boolean
    ): List<DanmakuSlice> {
        ensureDanmakuLoadActive(loadToken)
        val fetchResult = fetchDanmakuSegmentWithRecovery(
            cid = cid,
            avid = currentAid,
            segmentIndex = segmentIndex,
            loadToken = loadToken
        )
        val segmentData = fetchResult.data
        ensureDanmakuLoadActive(loadToken)
        if (segmentData.isEmpty()) {
            val emptyEntry = nextEmptyDanmakuSegmentCacheEntry(
                previous = loadSession.danmakuSegmentCacheBySegment[segmentIndex],
                nowMs = System.currentTimeMillis(),
                source = fetchResult.source,
                rawCount = fetchResult.rawCount,
                reason = "empty_reply"
            )
            loadSession.danmakuSegmentCacheBySegment[segmentIndex] = emptyEntry
            loadSession.nextDanmakuSliceIndexBySegment.remove(segmentIndex)
            logDanmakuSegmentCache(
                cid = cid,
                segmentIndex = segmentIndex,
                cacheEntry = emptyEntry,
                attempts = fetchResult.attempts
            )
            return emptyList()
        }

        var deduplicatedCount = 0
        var filteredCount = 0
        val newDanmaku = ArrayList<DanmakuData>(segmentData.size)
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
            logger.info { "Deduplicated $deduplicatedCount danmaku in segment $segmentIndex" }
        }
        if (filteredCount > 0) {
            logger.info { "Filtered $filteredCount danmaku in segment $segmentIndex with level < $currentDanmakuFilterLevel" }
        }
        trimLoadedDanmakuIds(loadSession)
        if (newDanmaku.isEmpty()) {
            val filteredEntry = DanmakuSegmentCacheEntry.Loaded(
                value = emptyList<DanmakuSlice>(),
                updatedAtMs = System.currentTimeMillis(),
                source = fetchResult.source,
                rawCount = fetchResult.rawCount
            )
            loadSession.danmakuSegmentCacheBySegment[segmentIndex] = filteredEntry
            loadSession.nextDanmakuSliceIndexBySegment[segmentIndex] = 0
            logDanmakuSegmentCache(
                cid = cid,
                segmentIndex = segmentIndex,
                cacheEntry = filteredEntry,
                attempts = fetchResult.attempts,
                reason = "filtered_or_deduplicated_empty"
            )
            return emptyList()
        }

        ensureDanmakuLoadActive(loadToken)
        val convertedItems = if (currentDanmakuMergeEnabled) {
            val mergeStartNs = System.nanoTime()
            val mergeResult = if (finalizeMergeForImmediateDisplay) {
                VodDanmakuMerger.processSegmentForImmediateDisplay(
                    segmentDanmaku = newDanmaku,
                    segmentIndex = segmentIndex,
                    segmentDurationMs = DANMAKU_SEGMENT_DURATION_MS,
                    state = loadSession.vodDanmakuMergeState
                )
            } else {
                VodDanmakuMerger.processSegment(
                    segmentDanmaku = newDanmaku,
                    segmentIndex = segmentIndex,
                    segmentDurationMs = DANMAKU_SEGMENT_DURATION_MS,
                    state = loadSession.vodDanmakuMergeState
                )
            }
            val mergeCostMs = (System.nanoTime() - mergeStartNs) / 1_000_000L
            logDanmakuMergeResult(segmentIndex, mergeResult, mergeCostMs)
            convertMergedDanmakuItems(mergeResult.emittedDanmaku)
        } else {
            convertDanmakuItems(newDanmaku)
        }

        val slices = buildDanmakuSlices(segmentIndex, convertedItems)
        val loadedEntry = DanmakuSegmentCacheEntry.Loaded(
            value = slices,
            updatedAtMs = System.currentTimeMillis(),
            source = fetchResult.source,
            rawCount = fetchResult.rawCount
        )
        loadSession.danmakuSegmentCacheBySegment[segmentIndex] = loadedEntry
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            if (!loadSession.nextDanmakuSliceIndexBySegment.containsKey(segmentIndex)) {
                loadSession.nextDanmakuSliceIndexBySegment[segmentIndex] = 0
            }
        } else {
            loadSession.nextDanmakuSliceIndexBySegment.putIfAbsent(segmentIndex, 0)
        }
        logDanmakuSegmentCache(
            cid = cid,
            segmentIndex = segmentIndex,
            cacheEntry = loadedEntry,
            attempts = fetchResult.attempts
        )
        if (slices.isNotEmpty()) {
            logger.info {
                "Prepared ${slices.size} danmaku slices for segment $segmentIndex, total=${convertedItems.size}, first=${slices.first().startPositionMs}, last=${slices.last().endPositionMs}"
            }
        }
        return slices
    }

    private val DanmakuSegmentCacheEntry<*>.cacheAttempts: Int
        get() = when (this) {
            is DanmakuSegmentCacheEntry.ConfirmedEmpty -> attempts
            is DanmakuSegmentCacheEntry.TransientFailed -> attempts
            is DanmakuSegmentCacheEntry.Loaded -> 0
        }

    private suspend fun fetchDanmakuSegmentWithRecovery(
        cid: Long,
        avid: Long,
        segmentIndex: Int,
        loadToken: Long
    ): DanmakuSegmentFetchResult {
        offlineVideoCacheService.getCachedDanmakuSegment(
            aid = avid,
            cid = cid,
            segmentIndex = segmentIndex
        )?.let { cachedData ->
            logDanmakuSegmentFetch(
                cid = cid,
                segmentIndex = segmentIndex,
                source = "offline_cache",
                attempt = 1,
                rawCount = cachedData.size
            )
            return DanmakuSegmentFetchResult(
                data = cachedData,
                source = "offline_cache",
                attempts = 1,
                rawCount = cachedData.size
            )
        }

        if (currentPlaybackOffline) {
            logDanmakuSegmentFetch(
                cid = cid,
                segmentIndex = segmentIndex,
                source = "offline_cache_missing",
                attempt = 1,
                rawCount = 0
            )
            return DanmakuSegmentFetchResult(
                data = emptyList(),
                source = "offline_cache_missing",
                attempts = 1,
                rawCount = 0
            )
        }

        var webAttempts = 0
        repeat(DANMAKU_WEB_EMPTY_IMMEDIATE_RETRIES + 1) { attemptIndex ->
            val attempt = attemptIndex + 1
            ensureDanmakuLoadActive(loadToken)
            webAttempts = attempt
            val webData = BiliHttpApi.getDanmakuSeg(
                cid = cid,
                avid = avid,
                segmentIndex = segmentIndex,
                sessData = Prefs.sessData
            )
            logDanmakuSegmentFetch(
                cid = cid,
                segmentIndex = segmentIndex,
                source = "web",
                attempt = attempt,
                rawCount = webData.size
            )
            if (webData.isNotEmpty()) {
                return DanmakuSegmentFetchResult(
                    data = webData,
                    source = "web",
                    attempts = attempt,
                    rawCount = webData.size
                )
            }
            if (attempt <= DANMAKU_WEB_EMPTY_IMMEDIATE_RETRIES) {
                delay(DANMAKU_WEB_EMPTY_RETRY_DELAY_MS)
            }
        }

        ensureDanmakuLoadActive(loadToken)
        val appData = runCatching {
            videoPlayRepository.getAppDanmakuSegment(
                aid = avid,
                cid = cid,
                segmentIndex = segmentIndex
            )
        }.onFailure { error ->
            if (error is CancellationException) throw error
            logger.warn {
                "Danmaku app fallback failed aid=$avid cid=$cid segmentIndex=$segmentIndex: ${error.stackTraceToString()}"
            }
        }.getOrDefault(emptyList())
        logDanmakuSegmentFetch(
            cid = cid,
            segmentIndex = segmentIndex,
            source = "app_grpc_fallback",
            attempt = webAttempts + 1,
            rawCount = appData.size
        )
        if (appData.isNotEmpty()) {
            return DanmakuSegmentFetchResult(
                data = appData,
                source = "app_grpc_fallback",
                attempts = webAttempts + 1,
                rawCount = appData.size
            )
        }

        ensureDanmakuLoadActive(loadToken)
        val xmlData = runCatching {
            loadXmlDanmakuSegment(
                cid = cid,
                segmentIndex = segmentIndex
            )
        }.onFailure { error ->
            if (error is CancellationException) throw error
            logger.warn {
                "Danmaku xml fallback failed aid=$avid cid=$cid segmentIndex=$segmentIndex: ${error.stackTraceToString()}"
            }
        }.getOrDefault(emptyList())
        logDanmakuSegmentFetch(
            cid = cid,
            segmentIndex = segmentIndex,
            source = "xml_fallback",
            attempt = webAttempts + 2,
            rawCount = xmlData.size
        )
        return DanmakuSegmentFetchResult(
            data = xmlData,
            source = if (xmlData.isNotEmpty()) "xml_fallback" else "web_app_xml_empty",
            attempts = webAttempts + 2,
            rawCount = xmlData.size
        )
    }

    @Suppress("DEPRECATION")
    private suspend fun loadXmlDanmakuSegment(
        cid: Long,
        segmentIndex: Int
    ): List<DanmakuData> {
        val segmentStartMs = segmentStartPositionMs(segmentIndex)
        val segmentEndMs = segmentStartMs + DANMAKU_SEGMENT_DURATION_MS
        return BiliHttpApi.getDanmakuXml(
            cid = cid,
            sessData = Prefs.sessData
        ).data.filter { danmaku ->
            val positionMs = (danmaku.time * 1000).toLong()
            positionMs in segmentStartMs until segmentEndMs
        }
    }

    private fun logDanmakuSegmentFetch(
        cid: Long,
        segmentIndex: Int,
        source: String,
        attempt: Int,
        rawCount: Int
    ) {
        logger.info {
            "Danmaku segment fetch aid=$currentAid cid=$cid segmentIndex=$segmentIndex source=$source attempt=$attempt rawCount=$rawCount cacheState=Fetching positionMs=${segmentStartPositionMs(segmentIndex)}"
        }
    }

    private fun logDanmakuSegmentCache(
        cid: Long,
        segmentIndex: Int,
        cacheEntry: DanmakuSegmentCacheEntry<*>?,
        attempts: Int,
        reason: String? = null
    ) {
        if (cacheEntry == null) return
        val cacheReason = when (cacheEntry) {
            is DanmakuSegmentCacheEntry.ConfirmedEmpty -> cacheEntry.reason
            is DanmakuSegmentCacheEntry.TransientFailed -> cacheEntry.reason
            is DanmakuSegmentCacheEntry.Loaded -> reason.orEmpty()
        }.ifBlank { reason.orEmpty() }
        logger.info {
            "Danmaku segment cache aid=$currentAid cid=$cid segmentIndex=$segmentIndex source=${cacheEntry.source} attempt=$attempts rawCount=${cacheEntry.rawCount} cacheState=${cacheEntry.cacheState} positionMs=${segmentStartPositionMs(segmentIndex)} reason=$cacheReason"
        }
    }

    fun updateDanmakuMergeEnabled(enabled: Boolean) {
        val supportedEnabled = enabled && danmakuSmartFilterSupported
        if (currentDanmakuMergeEnabled == supportedEnabled) return

        val wasEnabled = currentDanmakuMergeEnabled
        currentDanmakuMergeEnabled = supportedEnabled

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

        val firstAvailableSlice = slices.indexOfFirst { it.endPositionMs >= positionMs }
        return if (firstAvailableSlice >= 0) firstAvailableSlice else slices.size
    }

    private fun findDanmakuSliceEndExclusiveForCoverage(
        slices: List<DanmakuSlice>,
        startSliceIndex: Int,
        positionMs: Long,
        minSliceCount: Int
    ): Int {
        if (slices.isEmpty()) return 0

        val normalizedStart = startSliceIndex.coerceIn(0, slices.size)
        if (normalizedStart >= slices.size) return slices.size

        val minEndExclusive = (normalizedStart + minSliceCount)
            .coerceAtMost(slices.size)
        val maxEndExclusive = (normalizedStart + MAX_DANMAKU_SLICE_PREFETCH)
            .coerceAtMost(slices.size)
        val targetEndPositionMs = positionMs + DANMAKU_COVERAGE_LOOKAHEAD_MS

        var endExclusive = minEndExclusive
        while (
            endExclusive < maxEndExclusive &&
            slices[endExclusive - 1].endPositionMs < targetEndPositionMs
        ) {
            endExclusive++
        }

        return endExclusive
    }

    private suspend fun emitDanmakuSlicesAroundPosition(
        cid: Long,
        segmentIndex: Int,
        positionMs: Long,
        loadToken: Long,
        loadSession: DanmakuLoadSession,
        forceRetryTransient: Boolean = false,
        finalizeMergeForImmediateDisplay: Boolean = false
    ): Int {
        val slices = loadDanmakuSegmentSlices(
            cid = cid,
            segmentIndex = segmentIndex,
            loadToken = loadToken,
            loadSession = loadSession,
            forceRetryTransient = forceRetryTransient,
            finalizeMergeForImmediateDisplay = finalizeMergeForImmediateDisplay
        )
        if (slices.isEmpty()) return 0

        val targetSliceIndex = findDanmakuSliceIndexForPosition(slices, positionMs)
        if (targetSliceIndex >= slices.size) return 0

        val nextSliceIndex = loadSession.nextDanmakuSliceIndexBySegment[segmentIndex] ?: 0
        val startSliceIndex = maxOf(nextSliceIndex, targetSliceIndex)
        val endExclusive = findDanmakuSliceEndExclusiveForCoverage(
            slices = slices,
            startSliceIndex = targetSliceIndex,
            positionMs = positionMs,
            minSliceCount = CATCH_UP_DANMAKU_SLICE_PREFETCH
        )
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
    ): Int = loadSession.sliceEmitMutex.withLock {
        if (slices.isEmpty()) {
            loadSession.nextDanmakuSliceIndexBySegment[segmentIndex] = 0
            return@withLock 0
        }

        val requestedStart = startSliceIndex ?: 0
        val nextSliceIndex = loadSession.nextDanmakuSliceIndexBySegment[segmentIndex] ?: 0
        val normalizedStart = maxOf(requestedStart, nextSliceIndex).coerceIn(0, slices.size)
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

        emittedCount
    }

    private suspend fun emitDanmakuItems(
        items: List<DanmakuItemData>,
        loadToken: Long? = null
    ) {
        if (items.isEmpty()) return

        items.chunked(DANMAKU_BATCH_SIZE).forEach { chunk ->
            loadToken?.let { ensureDanmakuLoadActive(it) }
            danmakuPlayerDataMutex.withLock {
                withContext(Dispatchers.Main.immediate) {
                    loadToken?.let { ensureDanmakuLoadActive(it) }
                    danmakuData.addAll(chunk)
                    danmakuLoadSession?.let { trimRetainedDanmakuData(it.preloadPolicy) }
                    danmakuPlayer?.updateData(chunk)
                }
            }
            delay(8)
        }
    }

    private suspend fun syncDanmakuPlayerAfterInitialData(
        fallbackPositionMs: Long,
        loadToken: Long
    ) {
        ensureDanmakuLoadActive(loadToken)
        withContext(Dispatchers.Main.immediate) {
            ensureDanmakuLoadActive(loadToken)
            val playbackPositionMs = videoPlayer?.currentPosition
                ?.takeIf { it > 0L }
                ?: fallbackPositionMs
            danmakuPlayer?.seekTo(playbackPositionMs)
            if (videoPlayer?.isPlaying == true) {
                danmakuPlayer?.start()
            }
            logger.info {
                "Synced danmaku after initial data, positionMs=$playbackPositionMs, fallbackPositionMs=$fallbackPositionMs"
            }
        }
    }

    private fun pruneDanmakuSegmentCache(
        loadSession: DanmakuLoadSession,
        anchorSegment: Int
    ) {
        val policy = loadSession.preloadPolicy
        val firstKeptSegment = anchorSegment - policy.cacheRadius
        val lastKeptSegment = anchorSegment + policy.cacheRadius
        val iterator = loadSession.danmakuSegmentCacheBySegment.keys.iterator()
        while (iterator.hasNext()) {
            val segmentIndex = iterator.next()
            if (segmentIndex !in firstKeptSegment..lastKeptSegment) {
                iterator.remove()
                loadSession.nextDanmakuSliceIndexBySegment.remove(segmentIndex)
            }
        }

        while (loadSession.danmakuSegmentCacheBySegment.size > policy.maxCachedSegments) {
            val farthestSegment = loadSession.danmakuSegmentCacheBySegment.keys.maxByOrNull {
                abs(it - anchorSegment)
            } ?: break
            loadSession.danmakuSegmentCacheBySegment.remove(farthestSegment)
            loadSession.nextDanmakuSliceIndexBySegment.remove(farthestSegment)
        }
        trimLoadedDanmakuIds(loadSession)
    }

    private fun trimLoadedDanmakuIds(loadSession: DanmakuLoadSession) {
        val maxLoadedDanmakuIds = loadSession.preloadPolicy.maxLoadedDanmakuIds
        if (loadSession.loadedDanmakuIds.size <= maxLoadedDanmakuIds) return

        val iterator = loadSession.loadedDanmakuIds.iterator()
        while (loadSession.loadedDanmakuIds.size > maxLoadedDanmakuIds && iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }

    private fun trimRetainedDanmakuData(policy: DanmakuPreloadPolicy) {
        val overflow = danmakuData.size - policy.maxRetainedDanmakuItems
        if (overflow > 0) {
            danmakuData.subList(0, overflow).clear()
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
        logger.info { "Flushed ${convertedItems.size} pending merged danmaku" }
        return convertedItems.size
    }

    private fun clearDanmakuSliceState() {
        danmakuLoadSession?.danmakuSegmentCacheBySegment?.clear()
        danmakuLoadSession?.nextDanmakuSliceIndexBySegment?.clear()
    }

    private fun clearVodDanmakuMergeState() {
        danmakuLoadSession?.vodDanmakuMergeState?.clear()
    }

    private fun logDanmakuMergeResult(
        segmentIndex: Int,
        mergeResult: DanmakuSegmentMergeResult,
        costMs: Long
    ) {
        if (mergeResult.mergedDuplicateCount <= 0) return
        logger.info {
            "Merged ${mergeResult.mergedDuplicateCount} duplicate danmaku in segment $segmentIndex, " +
                "emitted=${mergeResult.emittedDanmaku.size}, costMs=$costMs"
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

        SponsorBlockHttpApi.updateBaseUrl(settings.sponsorBlockApiServer)

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
                preferApiType = settings.apiType
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

    fun saveSubtitleSmartDisplayPreferenceIfNeeded() {
        if (!settings.subtitleSmartDisplay || isLive || upId <= 0L || currentSubtitleId == -1L) {
            return
        }

        val subtitle = availableSubtitle.firstOrNull {
            it.id == currentSubtitleId && it.id != -1L
        } ?: return

        Prefs.setSubtitleLanguagePreference(
            upId = upId,
            lang = subtitle.lang,
            langDoc = subtitle.langDoc
        )
        logger.info { "Save subtitle language preference: upId=$upId, lang=${subtitle.lang}, langDoc=${subtitle.langDoc}" }
    }

    private fun enableSmartSubtitleIfAvailable(fallbackToFirstSubtitle: Boolean) {
        if (!settings.subtitleSmartDisplay || isLive) return

        val preferredSubtitle = Prefs.getSubtitleLanguagePreference(upId)
            ?.let { preference ->
                availableSubtitle
                    .filter { it.id != -1L }
                    .firstOrNull { it.matchesLanguagePreference(preference) }
            }

        if (preferredSubtitle != null) {
            logger.info {
                "Load smart subtitle: upId=$upId, lang=${preferredSubtitle.lang}, langDoc=${preferredSubtitle.langDoc}"
            }
            loadSubtitle(preferredSubtitle.id)
        } else if (fallbackToFirstSubtitle) {
            enableFirstSubtitle()
        }
    }

    private suspend fun addDanmakuLog(text: String) {
        addLogs(text, reportToCrashlytics = false)
    }

    private suspend fun addLogs(
        text: String,
        reportToCrashlytics: Boolean = true
    ) {
        if (reportToCrashlytics) {
            logger.fInfo { text }
        } else {
            logger.info { text }
        }
        if (!settings.playerShowDebugInfo) {
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

    fun enqueueUploadHistory(time: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            uploadHistory(time)
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
                    preferApiType = settings.apiType
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
                    preferApiType = settings.apiType
                )
            }
        }.onSuccess {
            logger.info { "Send heartbeat success" }
        }.onFailure {
            if (it is CancellationException) {
                logger.debug { "Send heartbeat cancelled: ${it.message}" }
            } else {
                logger.warn { "Send heartbeat failed: ${it.stackTraceToString()}" }
            }
        }
    }

    private fun ensureLiveRoomEntryReported(roomId: Int) {
        if (!isLive || roomId <= 0) return
        if (settings.liveIncognitoMode) {
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

    private fun applyPreparedAutoPlayTransitionContext(transitionContext: PreparedAutoPlayTransitionContext) {
        if (transitionContext.replacePlaybackContext) {
            videoInfoRepository.replacePlaybackContext(
                videoList = transitionContext.availableVideoList,
                relatedVideos = transitionContext.relatedVideos,
                interactivePlaybackContext = transitionContext.interactivePlaybackContext,
            )
        }
        title = transitionContext.title
        partTitle = transitionContext.partTitle
        cover = transitionContext.cover
        fromSeason = transitionContext.fromSeason
        subType = transitionContext.subType
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
        if (candidate is AutoPlayCandidate.PlaylistItem) {
            return withContext(Dispatchers.Main.immediate) {
                createPreparedPlaylistItemTransitionContext(candidate.item)
            }
        }

        val detail = when (candidate) {
            is AutoPlayCandidate.CrossVideoPart -> videoDetailRepository.getVideoDetail(
                aid = candidate.item.aid,
                preferApiType = settings.apiType,
                withUserActions = false,
            )

            is AutoPlayCandidate.RelatedVideo -> videoDetailRepository.getVideoDetail(
                aid = candidate.video.avid,
                preferApiType = settings.apiType,
                withUserActions = false,
            )
            is AutoPlayCandidate.PlaylistItem -> return null
        }

        if (detail.redirectToEp || detail.isInteractive || detail.pages.isEmpty()) return null

        val targetCid = when (candidate) {
            is AutoPlayCandidate.CrossVideoPart -> candidate.item.cid ?: return null
            is AutoPlayCandidate.RelatedVideo -> detail.pages.first().cid
            is AutoPlayCandidate.PlaylistItem -> return null
        }
        val preferredPartTitle = when (candidate) {
            is AutoPlayCandidate.CrossVideoPart -> candidate.item.partTitle.takeIf { it.isNotBlank() }
            is AutoPlayCandidate.RelatedVideo -> null
            is AutoPlayCandidate.PlaylistItem -> return null
        }

        return detail.toPreparedAutoPlayTransitionContext(
            targetCid = targetCid,
            preferredPartTitle = preferredPartTitle,
        )
    }

    private fun createPreparedPlaylistItemTransitionContext(
        item: VideoListItemData,
    ): PreparedAutoPlayTransitionContext? {
        val cid = item.cid ?: return null
        return PreparedAutoPlayTransitionContext(
            aid = item.aid,
            cid = cid,
            epid = item.epid,
            seasonId = item.seasonId,
            title = item.title,
            partTitle = item.partTitle,
            cover = item.cover ?: cover,
            isVerticalVideo = isVerticalVideo,
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
            availableVideoList = availableVideoList.toList(),
            relatedVideos = relatedVideos.toList(),
            interactivePlaybackContext = videoInfoRepository.interactivePlaybackContext,
            replacePlaybackContext = false,
            fromSeason = fromSeason,
            subType = subType,
        )
    }

    private suspend fun fetchPreparedAutoPlayPlayData(
        transitionContext: PreparedAutoPlayTransitionContext,
    ): PlayData = fetchPlayableVodPlayData(
        avid = transitionContext.aid,
        cid = transitionContext.cid,
        epid = transitionContext.epid ?: 0,
        preferApi = settings.apiType,
        proxyArea = proxyArea,
        fromSeason = transitionContext.fromSeason,
    )

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
        val playbackSessionToken = advanceVodPlaybackSession()
        if (
            pendingGeetestVerification != null ||
            geetestValidationPending != null ||
            geetestChallengeRefreshFlight != null ||
            showGeetestDialog
        ) {
            geetestRegistrationGeneration += 1
            geetestChallengeRefreshFlight?.deferred?.cancel()
            geetestChallengeRefreshFlight = null
            pendingGeetestVerification = null
            geetestValidationPending = null
            showGeetestDialog = false
        }
        return playbackSessionToken
    }

    private fun rebindGeetestToNextVodPlaybackSession(
        pending: PendingGeetestVerification,
    ): PendingGeetestVerification {
        val playbackSessionToken = advanceVodPlaybackSession()
        geetestChallengeRefreshFlight?.deferred?.cancel()
        geetestChallengeRefreshFlight = null
        return pending.copy(
            retryRequest = pending.retryRequest.copy(
                playbackSessionToken = playbackSessionToken
            )
        ).also { rebound ->
            pendingGeetestVerification = rebound
            geetestValidationPending = null
        }
    }

    private fun advanceVodPlaybackSession(): Long {
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
            seasonId = seasonId.takeIf { it > 0 },
            preferApi = settings.apiType,
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
        if (!danmakuMaskSupported) return

        // 直播模式不获取蒙版数据
        if (isLive) return

        if (!currentDanmakuMask) return
        if (hasResolvedCurrentDanmakuMask()) return

        val targetAid = currentAid
        val targetCid = currentCid

        runCatching {
            val masks = loadDanmakuMasks(targetAid, targetCid)

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

    private suspend fun loadDanmakuMasks(
        aid: Long,
        cid: Long
    ): List<DanmakuMaskSegment> {
        val apiCandidates = buildList {
            add(settings.apiType)
            if (DeviceUtil.isTvDevice()) {
                add(if (settings.apiType == ApiType.App) ApiType.Web else ApiType.App)
            }
        }.distinct()

        var lastFailure: Throwable? = null
        apiCandidates.forEachIndexed { index, apiType ->
            val masks = runCatching {
                videoPlayRepository.getDanmakuMask(
                    aid = aid,
                    cid = cid,
                    preferApiType = apiType
                )
            }.onFailure { error ->
                lastFailure = error
                logger.fWarn { "Load danmaku mask failed with apiType=$apiType: ${error.localizedMessage}" }
            }.getOrNull() ?: emptyList()

            if (masks.isNotEmpty()) {
                if (index > 0) {
                    logger.fInfo { "Load danmaku mask fallback hit with apiType=$apiType, size=${masks.size}" }
                }
                return masks
            }

            if (index < apiCandidates.lastIndex) {
                logger.fInfo { "Danmaku mask empty with apiType=$apiType, fallback to next candidate" }
            }
        }

        lastFailure?.let { throw it }
        return emptyList()
    }

    private suspend fun updateVideoShot() {
        withContext(Dispatchers.Main) { videoShot = null }
        runCatching {
            val videoShot = videoPlayRepository.getVideoShot(
                aid = currentAid,
                cid = currentCid,
                preferApiType = settings.apiType
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
     * @param qn 请求的画质编号，默认使用当前网络对应的直播清晰度
     */
    fun loadLiveStreamWithQuality(roomId: Int, qn: Int = defaultLiveQnForCurrentNetwork()) {
        resetVodBufferRecovery()
        cancelVodPlayUrlAutoRefresh()
        viewPoints = emptyList()
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
            loadLiveRoomInfo(roomId)

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
                currentLiveLineIndex = playInfo.currentLineIndex
                availableLiveLines.clear()
                availableLiveLines.addAll(playInfo.availableLines)

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

    suspend fun loadLiveRoomInfo(roomId: Int = liveRoomId) {
        if (roomId <= 0) return
        runCatching {
            val data = liveRepository.getLiveRoomInfoH5(roomId)
            withContext(Dispatchers.Main) {
                data.roomInfo?.let { room ->
                    liveAnchorId = room.uid
                    if (room.title.isNotBlank()) title = room.title
                    liveCover = room.cover
                    liveBackground = room.appBackground
                }
                data.anchorInfo?.baseInfo?.let { anchor ->
                    if (anchor.uname.isNotBlank()) upName = anchor.uname
                    if (anchor.face.isNotBlank()) upFace = anchor.face
                }
                liveWatchedShow = data.watchedShow?.textLarge.orEmpty()
            }
        }.onFailure {
            logger.fWarn { "Load live room info failed: ${it.message}" }
        }
    }

    suspend fun sendLiveDanmaku(message: String): Result<String> {
        val text = message.trim()
        if (text.isBlank()) return Result.failure(IllegalArgumentException("弹幕内容不能为空"))
        if (liveRoomId <= 0) return Result.failure(IllegalStateException("直播间不存在"))
        if (sendingLiveDanmaku) return Result.failure(IllegalStateException("正在发送弹幕"))
        return runCatching {
            withContext(Dispatchers.Main) { sendingLiveDanmaku = true }
            try {
                liveRepository.sendLiveMsg(liveRoomId, text)
                "发送成功"
            } finally {
                withContext(Dispatchers.Main) { sendingLiveDanmaku = false }
            }
        }
    }

    suspend fun sendVideoDanmaku(
        message: String,
        mode: Int = 1,
        fontSize: Int = 25,
        color: Int = 0xFFFFFF
    ): Result<String> {
        val text = message.trim()
        if (text.isBlank()) return Result.failure(IllegalArgumentException("弹幕内容不能为空"))

        val snapshot = try {
            withContext(Dispatchers.Main.immediate) {
                check(!isLive) { "当前不是点播视频" }
                check(currentAid > 0L && currentCid > 0L) { "视频不存在" }
                check(!sendingVideoDanmaku) { "正在发送弹幕" }

                sendingVideoDanmaku = true
                VideoDanmakuSendSnapshot(
                    aid = currentAid,
                    cid = currentCid,
                    progress = (videoPlayer?.currentPosition ?: 0L)
                        .coerceIn(0L, Int.MAX_VALUE.toLong())
                        .toInt(),
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            return Result.failure(error)
        }

        return try {
            val dmid = videoPlayRepository.sendDanmaku(
                cid = snapshot.cid,
                bvid = AvBvConverter.av2bv(snapshot.aid),
                message = text,
                progress = snapshot.progress,
                mode = mode,
                fontSize = fontSize,
                color = color
            )
            val item = DanmakuItemData(
                danmakuId = dmid ?: System.currentTimeMillis(),
                position = snapshot.progress.toLong(),
                content = text,
                mode = when (mode) {
                    4 -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                    5 -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                    else -> DanmakuItemData.DANMAKU_MODE_ROLLING
                },
                textSize = fontSize,
                textColor = Color(color).toArgb(),
                danmakuStyle = DanmakuItemData.DANMAKU_STYLE_SELF_SEND
            )
            withContext(Dispatchers.Main.immediate) {
                danmakuData.add(item)
                danmakuPlayer?.updateData(listOf(item))
            }
            Result.success("发送成功")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        } finally {
            withContext(NonCancellable + Dispatchers.Main.immediate) {
                sendingVideoDanmaku = false
            }
        }
    }

    suspend fun getDlnaMediaSource(): Result<DlnaMediaSource> {
        val snapshot = try {
            withContext(Dispatchers.Main.immediate) {
                check(!isLive) { "直播暂不支持投屏" }
                check(!currentPlaybackOffline) { "离线缓存无法投屏" }
                check(currentAid > 0L && currentCid > 0L) { "视频信息无效，无法投屏" }

                DlnaSourceSnapshot(
                    aid = currentAid,
                    cid = currentCid,
                    epid = currentEpid.takeIf { it > 0 },
                    qn = currentQuality.code,
                    title = title,
                    partTitle = partTitle,
                    positionMs = videoPlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L,
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            return Result.failure(error)
        }

        return try {
            val resource = videoPlayRepository.getDlnaPlayResource(
                aid = snapshot.aid,
                cid = snapshot.cid,
                epid = snapshot.epid,
                qn = snapshot.qn,
            )
            Result.success(
                DlnaMediaSource(
                    url = resource.url,
                    mimeType = resource.mimeType,
                    title = snapshot.title,
                    partTitle = snapshot.partTitle,
                    positionMs = snapshot.positionMs,
                )
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    fun loadLiveEmoticons(forceReload: Boolean = false) {
        val roomId = liveRoomId
        if (roomId <= 0) return
        if (Prefs.sessData.isBlank()) {
            liveEmoticonError = "账号未登录，无法获取直播表情"
            return
        }
        if (liveEmoticonRoomId != roomId) {
            liveEmoticonRoomId = roomId
            liveEmotePackages.clear()
            liveEmoticonError = null
        }
        if (loadingLiveEmoticons) return
        if (!forceReload && liveEmotePackages.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                loadingLiveEmoticons = true
                liveEmoticonError = null
            }
            runCatching {
                liveRepository.getLiveEmoticons(roomId)
            }.onSuccess { packages ->
                withContext(Dispatchers.Main) {
                    liveEmotePackages.swapList(packages.filter { it.emoticons.isNotEmpty() })
                }
            }.onFailure { e ->
                logger.fWarn { "Load live emoticons failed: ${e.message}" }
                withContext(Dispatchers.Main) {
                    liveEmoticonError = e.localizedMessage ?: "获取表情失败"
                }
            }
            withContext(Dispatchers.Main) {
                loadingLiveEmoticons = false
            }
        }
    }

    suspend fun sendLiveEmoticon(emoticonUnique: String): Result<String> {
        val unique = emoticonUnique.trim()
        if (unique.isBlank()) return Result.failure(IllegalArgumentException("表情不存在"))
        if (liveRoomId <= 0) return Result.failure(IllegalStateException("直播间不存在"))
        if (sendingLiveDanmaku) return Result.failure(IllegalStateException("正在发送弹幕"))
        return runCatching {
            withContext(Dispatchers.Main) { sendingLiveDanmaku = true }
            try {
                liveRepository.sendLiveEmoticon(liveRoomId, unique)
                "发送成功"
            } finally {
                withContext(Dispatchers.Main) { sendingLiveDanmaku = false }
            }
        }
    }

    suspend fun likeLiveRoom(clickTime: Int = 1): Result<String> {
        if (liveRoomId <= 0) return Result.failure(IllegalStateException("直播间不存在"))
        if (likingLiveRoom) return Result.failure(IllegalStateException("正在点赞"))
        return runCatching {
            withContext(Dispatchers.Main) { likingLiveRoom = true }
            try {
                liveRepository.likeLiveRoom(
                    roomId = liveRoomId,
                    clickTime = clickTime.coerceAtLeast(1),
                    anchorId = liveAnchorId.takeIf { it > 0 }
                )
                "点赞成功"
            } finally {
                withContext(Dispatchers.Main) {
                    likingLiveRoom = false
                    liveLikeClickCount = 0
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
     * 切换直播线路
     * @param lineIndex 目标线路序号，对应当前播放信息中的 url_info 下标
     */
    fun changeLiveLine(lineIndex: Int) {
        logger.fInfo { "Change live line to: $lineIndex" }
        preferredLiveLineIndex = lineIndex
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
                currentLiveLineIndex = playInfo.currentLineIndex
                availableLiveLines.clear()
                availableLiveLines.addAll(playInfo.availableLines)
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
                currentLiveLineIndex = playInfo.currentLineIndex
                availableLiveLines.clear()
                availableLiveLines.addAll(playInfo.availableLines)
            }

            // 预置新地址而不打断当前流：playUrl() 在所有内核上都是纯设置操作（见 AbstractVideoPlayer），
            // 当前连接继续播放，直到出错/重试时 prepare() 才会使用新地址
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
        val liveCdnHost = settings.liveCdnUrl.takeIf { it.isNotBlank() }
        val initialPlayInfo = LiveStreamUrlFetcher.fetchLiveStreamUrl(
            roomId = roomId,
            qn = preferredQn,
            preferredCodec = codec,
            liveCdnHost = liveCdnHost,
            preferredLineIndex = preferredLiveLineIndex
        )
            ?: return null
        val resolvedQn = LiveQualityPreference.resolveRequestedQn(preferredQn, initialPlayInfo.acceptQn)

        if (resolvedQn == initialPlayInfo.currentQn) {
            return initialPlayInfo
        }

        logger.fInfo {
            "Preferred live quality $preferredQn unavailable or downgraded, retry with resolved qn=$resolvedQn"
        }
        return LiveStreamUrlFetcher.fetchLiveStreamUrl(
            roomId = roomId,
            qn = resolvedQn,
            preferredCodec = codec,
            liveCdnHost = liveCdnHost,
            preferredLineIndex = preferredLiveLineIndex
        ) ?: initialPlayInfo
    }

    /**
     * 加载直播流
     */
    fun loadLiveStream(streamUrl: String) {
        cancelVodPlayUrlAutoRefresh()
        viewPoints = emptyList()
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
        val shouldClearMessages = currentLiveDanmakuRoomId != roomId
        currentLiveDanmakuRoomId = roomId
        stopLiveDanmaku()
        if (shouldClearMessages) {
            viewModelScope.launch(Dispatchers.Main) {
                liveDanmakuMessages.clear()
                nextLiveDanmakuMessageId = 0L
            }
        }
        
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
                withContext(Dispatchers.Main) {
                    liveTime = playInfo.data?.liveTime?.takeIf { it > 0 }
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
                    parseEmoji = settings.showLiveDanmakuEmoji,
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

    fun resumeLiveDanmakuIfNeeded() {
        if (!isLive || liveRoomId <= 0 || liveWebSocket != null) return
        startLiveDanmaku(liveRoomId)
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
            liveDanmakuMessages.add(
                LiveDanmakuMessage(
                    id = nextLiveDanmakuMessageId++,
                    username = event.username,
                    content = event.content,
                    medalName = event.medalName,
                    medalLevel = event.medalLevel,
                    userLevel = event.userLevel,
                    color = 0xFF000000.toInt() or event.color,
                    fontSize = event.fontSize,
                    timestampMs = System.currentTimeMillis(),
                    emojiMap = event.emojiMap
                )
            )
            while (liveDanmakuMessages.size > LIVE_DANMAKU_MESSAGE_LIMIT) {
                liveDanmakuMessages.removeAt(0)
            }

            if (showDanmaku) {
                liveDanmakuPlayer?.let { emitLiveDanmaku(it, danmakuItem) }
            }
        }
    }

    private fun emitLiveDanmaku(player: LiveDanmakuPlayer, data: DanmakuItemData) {
        if (liveDanmakuObjectPoolUsable) {
            try {
                player.emit(data)
                return
            } catch (error: NullPointerException) {
                // A corrupted AkDanmaku pool leaves a null entry below a positive pool size.
                // Once observed, the data overload will fail on every subsequent acquisition.
                liveDanmakuObjectPoolUsable = false
                logger.fWarn {
                    "Live danmaku object pool is corrupted; switching to direct items: " +
                        error.stackTraceToString()
                }
            } catch (error: Exception) {
                logger.fWarn { "Emit live danmaku failed: ${error.stackTraceToString()}" }
                return
            }
        }

        try {
            player.emit(DanmakuItem(data, null))
        } catch (error: Exception) {
            logger.fWarn { "Emit live danmaku without pooling failed: ${error.stackTraceToString()}" }
        }
    }
}
