package dev.aaa1115910.bv.tv.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Scaffold
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import dev.aaa1115910.bv.tv.util.requireTvActivity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButtonDefaults
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.FavoriteFolderMetadata
import dev.aaa1115910.biliapi.entity.video.Dimension
import dev.aaa1115910.biliapi.entity.video.InteractiveNode
import dev.aaa1115910.biliapi.entity.video.Tag
import dev.aaa1115910.biliapi.entity.video.VideoDetail
import dev.aaa1115910.biliapi.entity.video.VideoPage
import dev.aaa1115910.biliapi.entity.video.season.Episode
import dev.aaa1115910.biliapi.http.BiliPlusHttpApi
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.component.QrImage
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.offline.OfflineCacheQualitySelector
import dev.aaa1115910.bv.offline.OfflineVideoCacheService
import dev.aaa1115910.bv.offline.OfflineVideoCacheStatus
import dev.aaa1115910.bv.offline.OfflineVideoCacheTarget
import dev.aaa1115910.bv.offline.OfflineVideoCacheTaskState
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoListItem
import dev.aaa1115910.bv.player.entity.VideoListPart
import dev.aaa1115910.bv.player.entity.VideoListUgcEpisode
import dev.aaa1115910.bv.player.entity.VideoListUgcEpisodeTitle
import dev.aaa1115910.bv.repository.VideoInfoRepository
import dev.aaa1115910.bv.tv.activities.search.SearchResultActivity
import dev.aaa1115910.bv.tv.activities.user.OfflineCacheActivity
import dev.aaa1115910.bv.tv.activities.video.SeasonInfoActivity
import dev.aaa1115910.bv.tv.activities.video.TagActivity
import dev.aaa1115910.bv.tv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.tv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.tv.component.CommentPanel
import dev.aaa1115910.bv.tv.component.LoadingTip
import dev.aaa1115910.bv.tv.component.RelatedVideosPanel
import dev.aaa1115910.bv.tv.component.screenBackgroundGradient
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.tv.component.TvOverlayDialog
import dev.aaa1115910.bv.tv.component.UpIcon
import dev.aaa1115910.bv.tv.component.buttons.VideoInfoButtons
import dev.aaa1115910.bv.tv.manager.VideoUserActionManager
import dev.aaa1115910.bv.tv.manager.VideoUserActionManager.getStateFlow
import dev.aaa1115910.bv.tv.component.videocard.VideosRow
import dev.aaa1115910.bv.tv.util.launchPlayerActivity
import dev.aaa1115910.bv.tv.manager.FollowStateManager
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.fWarn
import dev.aaa1115910.bv.util.focusedBorder
import dev.aaa1115910.bv.util.focusedScale
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.formatPubTimeString
import dev.aaa1115910.bv.util.ifElse
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.onBackPressed
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.util.requestFocusWithRetry
import dev.aaa1115910.bv.util.resizedImageUrl
import dev.aaa1115910.bv.util.scrollToItemIfAvailable
import dev.aaa1115910.bv.util.swapList
import dev.aaa1115910.bv.util.swapListWithMainContext
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.video.VideoDetailViewModel
import dev.aaa1115910.bv.viewmodel.video.VideoInfoState
import dev.aaa1115910.bv.viewmodel.VideoPlayerV3ViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin
import kotlin.math.ceil
import kotlin.math.max

private val InteractiveBadgeColor = Color(0xFFFFD54F)
private val ChargingBadgeColor = Color(0xFF00FFFF)
private val UgcPartBadgeColor = Color(0xFF8BD8FF)
private val NoScrollBringIntoViewSpec = object : BringIntoViewSpec {
    override fun calculateScrollDistance(
        offset: Float,
        size: Float,
        containerSize: Float,
    ): Float = 0f
}
private const val ChargingBadgeDefaultText = "充电专属"
private const val ChargingUrlPrefix = "https://www.bilibili.com/h5/upower/index?mid="
private const val SupportedClickableTagType = "old_channel"
private val DiscoverTagTitleRegex = Regex("^发现\\s*《(.+?)》$")

private fun extractDiscoverTagKeyword(tagName: String): String? {
    return DiscoverTagTitleRegex.matchEntire(tagName)?.groupValues?.getOrNull(1)
}

private fun formatVideoTagName(tagName: String): String {
    return extractDiscoverTagKeyword(tagName) ?: tagName
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun VideoInfoScreen(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    videoInfoRepository: VideoInfoRepository = getKoin().get(),
    videoDetailViewModel: VideoDetailViewModel = koinViewModel(),
    offlineCacheViewModel: VideoPlayerV3ViewModel = koinViewModel(),
    offlineCacheService: OfflineVideoCacheService = getKoin().get(),
    userRepository: UserRepository = getKoin().get(),
) {
    val context = LocalContext.current
    val activity = requireTvActivity()
    val scope = rememberCoroutineScope()
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val heroContentColor = MaterialTheme.colorScheme.onSurface
    val heroSecondaryContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    val intent = activity.intent
    val logger = KotlinLogging.logger { }
    val playButtonFocusRequester = remember { FocusRequester() }
    val lazyListState = rememberLazyListState()
    var suppressPlayButtonBringIntoView by remember { mutableStateOf(false) }
    val defaultBringIntoViewSpec = LocalBringIntoViewSpec.current
    val focusUndetachedBringIntoViewSpec = remember {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(
                offset: Float,
                size: Float,
                containerSize: Float
            ): Float = 0f
        }
    }

    var showFollowButton by remember { mutableStateOf(false) }
    var isFollowing by remember { mutableStateOf(false) }

    // 监听关注状态变化
    val followStateMap by FollowStateManager.followStateMap.collectAsState()

    // 当关注状态map变化时，更新当前用户的关注状态
    LaunchedEffect(followStateMap, videoDetailViewModel.videoDetail?.author?.mid) {
        videoDetailViewModel.videoDetail?.author?.mid?.let { mid ->
            FollowStateManager.getFollowState(mid)?.let { following ->
                isFollowing = following
            }
        }
    }

    // 添加用于管理简介对话框的状态
    var showDescriptionDialog by remember { mutableStateOf(false) }
    var showChargingQrDialog by remember { mutableStateOf(false) }
    val chargeButtonFocusRequester = remember { FocusRequester() }
    val cacheButtonFocusRequester = remember { FocusRequester() }

    // 添加用于管理评论浮层的状态
    var showCommentPanel by remember { mutableStateOf(false) }
    val commentButtonFocusRequester = remember { FocusRequester() }
    var shouldRestoreCommentButtonFocus by remember { mutableStateOf(false) }

    var showRelatedPanel by remember { mutableStateOf(false) }
    val relatedButtonFocusRequester = remember { FocusRequester() }
    var shouldRestoreRelatedButtonFocus by remember { mutableStateOf(false) }

    var lastPlayedCid by remember { mutableLongStateOf(0) }
    var lastPlayedTime by remember { mutableIntStateOf(0) }

    var tip by remember { mutableStateOf("Loading") }
    var showUGCVideoInfo by remember { mutableStateOf(Prefs.showUGCVideoInfo) }
    val collapseVideoInfoRelatedVideos by Prefs.collapseVideoInfoRelatedVideosFlow.collectAsState(
        Prefs.collapseVideoInfoRelatedVideos
    )
    val showDetailPageBackgroundImage by Prefs.showDetailPageBackgroundImageFlow.collectAsState(
        Prefs.showDetailPageBackgroundImage
    )
    val usePureBlackBackground = !showUGCVideoInfo
    var fromSeason by remember { mutableStateOf(false) }
    var fromPlayer by remember { mutableStateOf(false) }
    var audioOnlyMode by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var proxyArea by remember { mutableStateOf(ProxyArea.MainLand) }
    var intentAid by remember { mutableLongStateOf(0L) }
    var showOfflineCachePanel by remember { mutableStateOf(false) }
    var offlineCacheLoading by remember { mutableStateOf(false) }
    var shouldRestoreCacheButtonFocus by remember { mutableStateOf(false) }
    var intentCid by remember { mutableLongStateOf(0L) }

    val currentDetailTargetIsVertical by remember(
        videoDetailViewModel.videoDetail,
        lastPlayedCid,
        intentAid
    ) {
        derivedStateOf {
            videoDetailViewModel.videoDetail?.isCurrentDetailTargetVertical(
                lastPlayedCid = lastPlayedCid,
                intentAid = intentAid
            ) ?: false
        }
    }

    val currentOfflineCacheAid = intentAid.takeIf { it > 0L }
        ?: videoDetailViewModel.videoDetail?.aid
        ?: 0L
    val preferredOfflineCacheCid = intentCid.takeIf { it > 0L }
        ?: lastPlayedCid.takeIf { it > 0L }
        ?: 0L
    val offlineCacheItems = remember(
        videoDetailViewModel.videoDetail,
        currentOfflineCacheAid,
        preferredOfflineCacheCid
    ) {
        buildTvOfflineCacheItems(
            videoDetail = videoDetailViewModel.videoDetail,
            currentAid = currentOfflineCacheAid,
            preferredCid = preferredOfflineCacheCid
        )
    }
    val currentOfflineCacheItem = offlineCacheItems.firstOrNull { it.isCurrent }
        ?: offlineCacheItems.firstOrNull()
    val currentOfflineCacheState = currentOfflineCacheItem?.target?.let {
        offlineCacheService.stateOf(it.aid, it.cid)
    } ?: OfflineVideoCacheTaskState(aid = 0L, cid = 0L)

    val chargingQrContent = remember(videoDetailViewModel.videoDetail?.author?.mid) {
        videoDetailViewModel.videoDetail?.author?.mid
            ?.takeIf { it > 0L }
            ?.let { "$ChargingUrlPrefix$it" }
            .orEmpty()
    }
    val showChargeButton = videoDetailViewModel.videoDetail?.isChargingArc == true &&
            videoDetailViewModel.videoDetail?.isUpowerPlay == false &&
            chargingQrContent.isNotBlank()
    val hideChargingQrDialog = {
        showChargingQrDialog = false
        if (showChargeButton) {
            chargeButtonFocusRequester.requestFocus(scope)
        }
    }

    var favorited by remember { mutableStateOf(false) }
    // local copies for like / coin sync
    var liked by remember { mutableStateOf(false) }
    var isCoin by remember { mutableStateOf(false) }
    val favoriteFolderMetadataList = remember { mutableStateListOf<FavoriteFolderMetadata>() }
    val videoInFavoriteFolderIds = remember { mutableStateListOf<Long>() }

    // subscribe shared action state by aid
    val aid = videoDetailViewModel.videoDetail?.aid ?: 0L
    val sharedActionStateFlow = remember(aid) { getStateFlow(aid, Prefs.uid) }
    val sharedState by sharedActionStateFlow.collectAsState()

    // keep local copies in sync with shared state
    LaunchedEffect(sharedState) {
        // sync favorite/like/coin from shared manager
        favorited = sharedState.favorited
        liked = sharedState.liked
        isCoin = sharedState.coin
        videoInFavoriteFolderIds.swapList(sharedState.favoriteFolderIds)
        favoriteFolderMetadataList.swapList(sharedState.favoriteFolders)
    }

    val setHistory = {
        logger.info { "play history: ${videoDetailViewModel.videoDetail?.history}" }
        lastPlayedCid = videoDetailViewModel.videoDetail?.history?.lastPlayedCid ?: 0
        lastPlayedTime = videoDetailViewModel.videoDetail?.history?.progress ?: 0
    }

    val updateHistory = {
        scope.launch(Dispatchers.IO) {
            runCatching {
                videoDetailViewModel.loadDetailOnlyUpdateHistory(videoDetailViewModel.videoDetail!!.aid)
            }
            withContext(Dispatchers.Main) {
                setHistory()
            }
        }
    }


    val updateFollowingState: () -> Unit = {
        scope.launch(Dispatchers.IO) {
            val userMid = videoDetailViewModel.videoDetail?.author?.mid ?: -1

            // 先检查缓存中是否有关注状态
            val cachedState = FollowStateManager.getFollowState(userMid)
            if (cachedState != null) {
                withContext(Dispatchers.Main) {
                    showFollowButton = true
                    isFollowing = cachedState
                }
                return@launch
            }

            // 缓存中没有，调用API获取
            logger.fInfo { "Checking is following user $userMid" }
            val success = userRepository.checkIsFollowing(
                mid = userMid,
                preferApiType = Prefs.apiType
            )
            logger.fInfo { "Following user result: $success" }
            withContext(Dispatchers.Main) {
                showFollowButton = success != null
                if (success != null) {
                    isFollowing = success
                    // 更新到缓存中
                    FollowStateManager.updateFollowState(userMid, success)
                }
            }
        }
    }

    val addFollow: (afterModify: (success: Boolean) -> Unit) -> Unit = { afterModify ->
        scope.launch(Dispatchers.IO) {
            val userMid = videoDetailViewModel.videoDetail?.author?.mid ?: -1
            logger.fInfo { "Add follow to user $userMid" }
            val success = userRepository.followUser(
                mid = userMid,
                preferApiType = Prefs.apiType
            )
            logger.fInfo { "Add follow result: $success" }
            // 更新缓存状态
            if (success) {
                FollowStateManager.updateFollowState(userMid, true)
            }
            afterModify(success)
        }
    }

    val delFollow: (afterModify: (success: Boolean) -> Unit) -> Unit = { afterModify ->
        scope.launch(Dispatchers.IO) {
            val userMid = videoDetailViewModel.videoDetail?.author?.mid ?: -1
            logger.fInfo { "Del follow to user $userMid" }
            val success = userRepository.unfollowUser(
                mid = userMid,
                preferApiType = Prefs.apiType
            )
            logger.fInfo { "Del follow result: $success" }
            // 更新缓存状态
            if (success) {
                FollowStateManager.updateFollowState(userMid, false)
            }
            afterModify(success)
        }
    }

    val fetchFavoriteData: (Long) -> Unit = { avid ->
        scope.launch {
            VideoUserActionManager.fetchFavoriteData(avid, Prefs.uid)
        }
    }

    val updateVideoFavoriteData: (List<Long>) -> Unit = { folderIds ->
        scope.launch {
            val success =
                VideoUserActionManager.updateVideoFavoriteFolders(aid, folderIds, Prefs.uid)
            if (!success) {
                "收藏操作失败".toast(context)
            }
        }
    }

    val addVideoToDefaultFavoriteFolder: () -> Unit = {
        scope.launch {
            val success = VideoUserActionManager.addToDefaultFavoriteFolder(aid, Prefs.uid)
            if (!success) {
                "添加收藏失败".toast(context)
            }
        }
    }

    val updateVideoUserActionData = {
        // update shared state from loaded data
        val configAid = videoDetailViewModel.videoDetail?.aid ?: 0L
        VideoUserActionManager.updateFromLoadedData(
            configAid,
            liked = videoDetailViewModel.videoDetail?.userActions?.like ?: false,
            favorited = videoDetailViewModel.videoDetail?.userActions?.favorite ?: false,
            coin = videoDetailViewModel.videoDetail?.userActions?.coin ?: false
        )
    }

    val updateUgcSeasonSectionVideoList: (Int) -> Unit = { sectionIndex ->
        val partVideoList = mutableListOf<VideoListItem>()
        val sectionTitle =
            videoDetailViewModel.videoDetail!!.ugcSeason!!.sections[sectionIndex]?.title ?: ""
        videoDetailViewModel.videoDetail!!.ugcSeason!!.sections[sectionIndex].episodes.mapIndexed { epIndex, episode ->
            if (episode.pages.size == 1) {
                episode.pages.mapIndexed { pageInd, videoPage ->
                    partVideoList.add(
                        VideoListUgcEpisode(
                            aid = episode.aid,
                            cid = videoPage.cid,
                            title = if (sectionTitle == "正片") episode.title else sectionTitle,
                            partTitle = if (sectionTitle == "正片") "" else episode.title,
                            index = epIndex,
                            cover = episode.cover,
                            duration = episode.duration,
                            viewCount = episode.viewCount,
                            danmakuCount = episode.danmakuCount,
                        )
                    )
                }
            } else {
                partVideoList.add(
                    VideoListUgcEpisodeTitle(
                        title = episode.title,
                        index = epIndex,
                        cover = episode.cover,
                        duration = episode.duration,
                        viewCount = episode.viewCount,
                        danmakuCount = episode.danmakuCount,
                    )
                )
                episode.pages.mapIndexed { pageIndex, videoPage ->
                    partVideoList.add(
                        VideoListPart(
                            aid = episode.aid,
                            cid = videoPage.cid,
                            title = episode.title,
                            partTitle = videoPage.title,
                            index = pageIndex,
                            duration = videoPage.duration,
                        )
                    )
                }
            }
        }
        videoInfoRepository.videoList.clear()
        videoInfoRepository.videoList.addAll(partVideoList)
    }


    suspend fun addVideoLike(): Boolean {
        val configAid = videoDetailViewModel.videoDetail?.aid ?: 0L
        return VideoUserActionManager.addLike(configAid, Prefs.uid)
    }

    suspend fun delVideoLike(): Boolean {
        val configAid = videoDetailViewModel.videoDetail?.aid ?: 0L
        return VideoUserActionManager.delLike(configAid, Prefs.uid)
    }


    suspend fun addVideoCoin(): Boolean {
        val configAid = videoDetailViewModel.videoDetail?.aid ?: 0L
        return VideoUserActionManager.addCoin(configAid, Prefs.uid)
    }

    suspend fun requestPlayButtonFocusWithoutScrolling() {
        suppressPlayButtonBringIntoView = true
        try {
            withFrameNanos { }
            playButtonFocusRequester.requestFocusWithRetry()
            delay(80)
        } finally {
            suppressPlayButtonBringIntoView = false
        }
    }

    LaunchedEffect(Unit) {
        if (intent.hasExtra("aid")) {
            val aid = intent.getLongExtra("aid", 170001)
            intentAid = aid
            var cid = intent.getLongExtra("cid", 0)
            intentCid = cid
            fromSeason = intent.getBooleanExtra("fromSeason", false)
            fromPlayer = intent.getBooleanExtra("fromPlayer", false)
            audioOnlyMode = intent.getBooleanExtra("audioOnlyMode", false)
            proxyArea = ProxyArea.entries[intent.getIntExtra("proxy_area", 0)]
            //获取视频信息
            scope.launch(Dispatchers.IO) {
                if (proxyArea != ProxyArea.MainLand) {
                    runCatching {
                        val seasonId = BiliPlusHttpApi.getSeasonIdByAvid(aid)
                        logger.info { "Get season id from biliplus: $seasonId" }
                        seasonId?.let {
                            logger.fInfo { "Redirect to season $seasonId" }
                            SeasonInfoActivity.actionStart(
                                context = context,
                                seasonId = seasonId,
                                proxyArea = proxyArea
                            )
                            activity.finish()
                        }
                    }.onFailure {
                        logger.fWarn { "Redirect failed: ${it.stackTraceToString()}" }
                    }
                }

                runCatching {
                    videoDetailViewModel.loadDetail(aid, fromSeason)
                    withContext(Dispatchers.Main) {
                        updateVideoUserActionData()
                        setHistory()
                    }
                    if (Prefs.isLogin) fetchFavoriteData(aid)

                    videoInfoRepository.relatedVideos.clear()
                    if (!fromSeason) {
                        if (Prefs.isLogin) updateFollowingState()

                        videoInfoRepository.relatedVideos.addAll(
                            videoDetailViewModel.relatedVideos.subList(
                                0,
                                videoDetailViewModel.relatedVideos.size.takeIf { it < 16 } ?: 16))
                    }
                    // 从播放器推荐视频打开时 fromPlayer=true 并显示loading。300m后 fromPlayer改成false，此后从播放器返回详情页，正常显示详情内容
                    //如果是从剧集跳转过来的或设置不显示视频详情，就直接播放 P1
                    if (fromSeason || !showUGCVideoInfo || fromPlayer) {
                        val playPart = videoDetailViewModel.videoDetail!!.pages.first()
                        cid = cid.takeIf { it > 0L } ?: playPart.cid

                        if (videoDetailViewModel.videoDetail!!.ugcSeason !== null) {
                            val sectionIndex =
                                videoDetailViewModel.videoDetail!!.ugcSeason!!.sections
                                    .indexOfFirst { section -> section.episodes.any { it.cid == cid || it.pages.any { it.cid == cid } } }
                            updateUgcSeasonSectionVideoList(sectionIndex)
                        }

                        // 检查Activity是否已经finish，如果已关闭则不启动播放器
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            launchPlayerActivity(
                                context = context,
                                avid = videoDetailViewModel.videoDetail!!.aid,
                                cid = cid,
                                title = videoDetailViewModel.videoDetail!!.title,
                                partTitle = videoDetailViewModel.videoDetail!!.pages.find { it.cid == cid }!!.title,
                                played = if (cid == lastPlayedCid) lastPlayedTime * 1000 else 0,
                                fromSeason = fromSeason,
                                isVerticalVideo = videoDetailViewModel.videoDetail!!.pages.find { it.cid == cid }!!.dimension.isVertical,
                                playerIconIdle = videoDetailViewModel.videoDetail!!.playerIcon?.idle
                                    ?: "",
                                playerIconMoving = videoDetailViewModel.videoDetail!!.playerIcon?.moving
                                    ?: "",
                                play = videoDetailViewModel.videoDetail!!.stat.view,
                                danmaku = videoDetailViewModel.videoDetail!!.stat.danmaku,
                                like = videoDetailViewModel.videoDetail!!.stat.like,
                                coin = videoDetailViewModel.videoDetail!!.stat.coin,
                                favorite = videoDetailViewModel.videoDetail!!.stat.favorite,
                                upName = videoDetailViewModel.videoDetail!!.author.name,
                                upId = videoDetailViewModel.videoDetail!!.author.mid,
                                upFace = videoDetailViewModel.videoDetail!!.author.face,
                                pubTime = videoDetailViewModel.videoDetail!!.publishDate.formatPubTimeString(),
                                audioOnlyMode = audioOnlyMode
                            )
                        }
                        if (fromPlayer) {
                            // 清除一次性透传状态
                            audioOnlyMode = false
                            intent.removeExtra("audioOnlyMode")
                            // 清除标记
                            scope.launch {
                                delay(1200)
                                fromPlayer = false
                                intent.removeExtra("fromPlayer")
                                if (!showUGCVideoInfo) {
                                    activity.finish()
                                }
                            }
                        }
                        if (!fromPlayer) {
                            activity.finish()
                        }
                    }
                }.onFailure {
                    val errorMessage = it.localizedMessage
                    val isVideoNotFound = when (VideoDetailViewModel.DETAIL_API_TYPE) {
                        ApiType.Web -> errorMessage == "啥都木有"
                        ApiType.App -> errorMessage == "访问权限不足"
                    }

                    logger.fInfo { "Get video info failed: ${it.stackTraceToString()}" }
                    if (!isVideoNotFound || !Prefs.enableProxy) {
                        withContext(Dispatchers.Main) {
                            tip = it.localizedMessage ?: "未知错误"
                        }
                        return@onFailure
                    }
                    withContext(Dispatchers.Main) {
                        videoDetailViewModel.state = VideoInfoState.Loading
                    }

                    logger.fInfo { "Trying get video info through proxy server" }
                    runCatching {
                        val seasonId = BiliPlusHttpApi.getSeasonIdByAvid(aid)
                        logger.info { "Get season id from biliplus: $seasonId" }
                        seasonId?.let {
                            logger.fInfo { "Redirect to season $seasonId" }
                            SeasonInfoActivity.actionStart(
                                context = context,
                                seasonId = seasonId,
                                proxyArea = ProxyArea.HongKong
                            )
                            activity.finish()
                        } ?: let {
                            withContext(Dispatchers.Main) {
                                tip = "视频不存在"
                                videoDetailViewModel.state = VideoInfoState.Error
                            }
                        }
                    }.onFailure { e ->
                        logger.fWarn { "Redirect failed: ${e.stackTraceToString()}" }
                        withContext(Dispatchers.Main) {
                            tip = e.localizedMessage ?: "未知错误"
                            videoDetailViewModel.state = VideoInfoState.Error
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(videoDetailViewModel.videoDetail) {
        //如果是从剧集页跳转回来的，那就不需要再跳转到剧集页了
        if (fromSeason || !showUGCVideoInfo) return@LaunchedEffect

        videoDetailViewModel.videoDetail?.let {
            if (it.redirectToEp) {
                runCatching {
                    logger.fInfo { "Redirect to ep ${it.epid}" }
                    SeasonInfoActivity.actionStart(
                        context = context,
                        epId = it.epid,
                        proxyArea = proxyArea
                    )
                    activity.finish()
                }.onFailure {
                    logger.fWarn { "Redirect failed: ${it.stackTraceToString()}" }
                }
            } else {
                logger.fInfo { "No redirection required" }
            }
        }
    }

    // 确保页面显示时封面获得焦点
    LaunchedEffect(videoDetailViewModel.videoDetail, fromSeason, showUGCVideoInfo, fromPlayer) {
        if (videoDetailViewModel.videoDetail != null &&
            !videoDetailViewModel.videoDetail!!.redirectToEp &&
            !fromSeason &&
            showUGCVideoInfo &&
            !fromPlayer
        ) {
            // 延迟一小段时间确保UI完全渲染
            delay(300)
            requestPlayButtonFocusWithoutScrolling()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                paused = true
            } else if (event == Lifecycle.Event.ON_RESUME) {
                // 如果 pause==true 那可能是从播放页返回回来的，此时更新历史记录
                if (paused) updateHistory()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (videoDetailViewModel.videoDetail == null || videoDetailViewModel.videoDetail?.redirectToEp == true || fromSeason || !showUGCVideoInfo || fromPlayer) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (usePureBlackBackground) Color.Black else MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            if (tip == "Loading") {
                LoadingTip()
            } else {
                Text(
                    text = tip,
                    fontSize = 20.sp
                )
            }
        }
    } else {
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val heroHeight = screenHeight * 0.5f
        val topArgueTips = buildList {
            if (currentDetailTargetIsVertical) {
                add(context.getString(R.string.video_info_argue_tip_vertical_screen))
            }
            if (videoDetailViewModel.videoDetail?.isChargingArc == true) {
                val chargingArcStatus = when (videoDetailViewModel.videoDetail?.isUpowerPlay) {
                    true -> context.getString(R.string.video_info_argue_tip_charging_arc_charged)
                    false -> context.getString(R.string.video_info_argue_tip_charging_arc_not_charged)
                    null -> ""
                }
                add(
                    context.getString(
                        R.string.video_info_argue_tip_charging_arc,
                        videoDetailViewModel.videoDetail?.chargingArcBadge
                            .takeUnless { it.isNullOrBlank() }
                            ?: ChargingBadgeDefaultText,
                        chargingArcStatus
                    )
                )
            }
            if (videoDetailViewModel.videoDetail?.isInteractive == true) {
                add(context.getString(R.string.video_info_argue_tip_interactive))
            }
            videoDetailViewModel.videoDetail?.argueTip?.let { add(it) }
        }
        val topArgueTipsPadding = if (topArgueTips.isEmpty()) {
            0.dp
        } else {
            // 12.dp + (44.dp * topArgueTips.size) + (8.dp * (topArgueTips.size - 1))
            20.dp
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(if (isDarkTheme) Color.Black else MaterialTheme.colorScheme.surface)
        ) {
            if (showDetailPageBackgroundImage && isDarkTheme) {
                val bgLoaded = remember(videoDetailViewModel.videoDetail?.cover) { mutableStateOf(false) }
                val bgAnimatedAlpha by animateFloatAsState(
                    targetValue = if (bgLoaded.value) 1f else 0f,
                    animationSpec = tween(durationMillis = 600),
                    label = "hero bg alpha"
                )
                AsyncImage(
                    modifier = Modifier
                        .fillMaxSize(),
                    model = videoDetailViewModel.videoDetail?.cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = bgAnimatedAlpha,
                    onSuccess = { bgLoaded.value = true },
                    onError = { bgLoaded.value = false }
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.9f),
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Transparent
                                ),
                                startX = 0f,
                                endX = Float.MAX_VALUE * 0.5f
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Transparent,
                                    0.6f to Color.Transparent,
                                    0.82f to Color.Black.copy(alpha = 0.8f),
                                    1f to Color.Black
                                )
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isDarkTheme) Modifier.background(screenBackgroundGradient())
                            else Modifier.background(MaterialTheme.colorScheme.surface)
                        )
                )
            }

                CompositionLocalProvider(
                    LocalBringIntoViewSpec provides if (suppressPlayButtonBringIntoView) {
                        focusUndetachedBringIntoViewSpec
                    } else {
                        defaultBringIntoViewSpec
                    }
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.key) {
                                        Key.DirectionUp -> {
                                            scope.launch {
                                                lazyListState.animateScrollBy(-200f)
                                            }
                                        }

                                        Key.DirectionDown -> {
                                            scope.launch {
                                                lazyListState.animateScrollBy(200f)
                                            }
                                        }
                                    }
                                }
                                return@onKeyEvent false
                            },
                        state = lazyListState,
                        contentPadding = PaddingValues(top = topArgueTipsPadding, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Hero 信息区
                        item {
                            val videoDetail = videoDetailViewModel.videoDetail!!
                            var coverAspectRatio by remember(videoDetail.cover) {
                                mutableStateOf(16f / 9f)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(heroHeight)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(start = 48.dp, end = 48.dp, bottom = 8.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    // 左侧：信息区
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                    // UP 主信息行（头像 + 名称 + 关注）
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            onClick = {
                                                UpInfoActivity.actionStart(
                                                    context,
                                                    mid = videoDetail.author.mid,
                                                    name = videoDetail.author.name,
                                                    face = videoDetail.author.face
                                                )
                                            },
                                            shape = ClickableSurfaceDefaults.shape(
                                                shape = RoundedCornerShape(20.dp)
                                            ),
                                            colors = ClickableSurfaceDefaults.colors(
                                                containerColor = Color.Transparent,
                                                focusedContainerColor = heroContentColor.copy(alpha = 0.12f)
                                            ),
                                            border = ClickableSurfaceDefaults.border(
                                                focusedBorder = Border(
                                                    border = BorderStroke(2.dp, heroContentColor),
                                                    shape = RoundedCornerShape(20.dp)
                                                )
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(
                                                    start = 4.dp,
                                                    end = 12.dp,
                                                    top = 4.dp,
                                                    bottom = 4.dp
                                                ),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                AsyncImage(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape),
                                                    model = videoDetail.author.face,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop
                                                )
                                                Text(
                                                    text = videoDetail.author.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = heroSecondaryContentColor
                                                )
                                            }
                                        }
                                        if (Prefs.isLogin && showFollowButton) {
                                            IconButton(
                                                modifier = Modifier.size(36.dp),
                                                onClick = {
                                                    if (isFollowing) {
                                                        delFollow { success ->
                                                            scope.launch(Dispatchers.Main) {
                                                                if (success) "已取消关注".toast(context)
                                                                else "取消关注失败".toast(context)
                                                            }
                                                        }
                                                    } else {
                                                        addFollow { success ->
                                                            scope.launch(Dispatchers.Main) {
                                                                if (success) "关注成功".toast(context)
                                                                else "关注失败".toast(context)
                                                            }
                                                        }
                                                    }
                                                },
                                                colors = OutlinedButtonDefaults.colors(
                                                    containerColor = Color.White.copy(alpha = 0.15f)
                                                ),
                                                border = OutlinedButtonDefaults.border()
                                            ) {
                                                Icon(
                                                    modifier = Modifier.size(18.dp),
                                                    imageVector = if (isFollowing) Icons.Rounded.Done else Icons.Rounded.Add,
                                                    contentDescription = null,
                                                    tint = if (isFollowing) Color(0xfffb7299) else Color.Unspecified
                                                )
                                            }
                                        }
                                    }

                                    // 标题
                                    AutoResizeTitleText(
                                        text = videoDetail.title,
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = heroContentColor
                                    )

                                    // 元信息行
                                    Text(
                                        text = buildString {
                                            append(with(videoDetail.stat.view) { if (this >= 10000) "${this / 10000}万" else "$this" })
                                            append(" 播放")
                                            append("  ·  ")
                                            append(with(videoDetail.stat.danmaku) { if (this >= 10000) "${this / 10000}万" else "$this" })
                                            append(" 弹幕")
                                            append("  ·  ")
                                            append(videoDetail.publishDate.formatPubTimeString())
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = heroSecondaryContentColor
                                    )

                                    if (videoDetail.tags.isNotEmpty()) {
                                        VideoTagsRow(
                                            tags = videoDetail.tags,
                                            onClickTag = { tag ->
                                                if (tag.tagType != SupportedClickableTagType) {
                                                    "该标签类型(${tag.tagType})暂不支持跳转".toast(context)
                                                } else {
                                                    val searchKeyword = extractDiscoverTagKeyword(tag.name)
                                                    if (searchKeyword != null) {
                                                        SearchResultActivity.actionStart(
                                                            context = context,
                                                            keyword = searchKeyword,
                                                            enableProxy = false
                                                        )
                                                    } else {
                                                        TagActivity.actionStart(
                                                            context = context,
                                                            tagId = tag.id,
                                                            tagName = formatVideoTagName(tag.name)
                                                        )
                                                    }
                                                }
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(1.dp))

                                    // 按钮行
                                        VideoInfoButtons(
                                            playButtonFocusRequester = playButtonFocusRequester,
                                            chargeButtonFocusRequester = chargeButtonFocusRequester,
                                            cacheButtonFocusRequester = cacheButtonFocusRequester,
                                    commentButtonFocusRequester = commentButtonFocusRequester,
                                            relatedButtonFocusRequester = relatedButtonFocusRequester,
                                            lastPlayedTime = lastPlayedTime,
                                            isLogin = Prefs.isLogin,
                                            showChargeButton = showChargeButton,
                                    onPlay = {
                                                logger.fInfo { "Click play button" }
                                                var title = ""
                                                var partTitle = ""
                                                //set video list
                                                if (videoDetailViewModel.videoDetail?.ugcSeason != null) {
                                                    // 合集
                                                    if (videoDetailViewModel.videoDetail!!.ugcSeason!!.sections.size == 1) {
                                                        // 只有一个分组
                                                        updateUgcSeasonSectionVideoList(0)
                                                    } else {
                                                        // 多个组，找默认播放哪个组的
                                                        val cid =
                                                            videoDetailViewModel.videoDetail!!.pages.first().cid
                                                        val sectionIndex =
                                                            videoDetailViewModel.videoDetail!!.ugcSeason!!.sections
                                                                .indexOfFirst { section -> section.episodes.any { it.cid == cid || it.pages.any { it.cid == cid } } }
                                                        val section =
                                                            videoDetailViewModel.videoDetail!!.ugcSeason!!.sections.getOrNull(
                                                                sectionIndex
                                                            )
                                                        title =
                                                            if (section?.title == "正片") section.episodes.find { it.cid == cid }!!.title else section?.title
                                                                ?: ""
                                                        partTitle =
                                                            if (section?.title == "正片") "" else section?.episodes?.find { it.cid == cid }!!.title
                                                        updateUgcSeasonSectionVideoList(sectionIndex)
                                                    }
                                                } else {
                                                    // 分 p
                                                    val partVideoList =
                                                        videoDetailViewModel.videoDetail!!.pages.mapIndexed { index, videoPage ->
                                                            VideoListPart(
                                                                aid = videoDetailViewModel.videoDetail!!.aid,
                                                                cid = videoPage.cid,
                                                                title = videoDetailViewModel.videoDetail!!.title,
                                                                partTitle = if (videoDetailViewModel.videoDetail!!.pages.size == 1) "" else videoPage.title,
                                                                index = index,
                                                                duration = videoPage.duration,
                                                            )
                                                        }
                                                    videoInfoRepository.videoList.clear()
                                                    videoInfoRepository.videoList.addAll(partVideoList)
                                                }

                                                val lastPlayedPage =
                                                    videoDetailViewModel.videoDetail!!.pages.find { it.cid == lastPlayedCid }
                                                val playPage = lastPlayedPage
                                                    ?: videoDetailViewModel.videoDetail!!.pages.first()

                                                launchPlayerActivity(
                                                    context = context,
                                                    avid = videoDetailViewModel.videoDetail!!.aid,
                                                    cid = playPage.cid,
                                                    title = if (title.isNotEmpty()) title else videoDetailViewModel.videoDetail!!.title,
                                                    partTitle = if (partTitle.isNotEmpty()) partTitle else if (videoDetailViewModel.videoDetail!!.pages.size == 1) "" else playPage.title,
                                                    played = if (playPage.cid == lastPlayedCid) lastPlayedTime * 1000 else 0,
                                                    fromSeason = false,
                                                    isVerticalVideo = videoDetailViewModel.videoDetail!!.pages.first().dimension.isVertical,
                                                    playerIconIdle = videoDetailViewModel.videoDetail!!.playerIcon?.idle
                                                        ?: "",
                                                    playerIconMoving = videoDetailViewModel.videoDetail!!.playerIcon?.moving
                                                        ?: "",
                                                    play = videoDetailViewModel.videoDetail!!.stat.view,
                                                    danmaku = videoDetailViewModel.videoDetail!!.stat.danmaku,
                                                    like = videoDetailViewModel.videoDetail!!.stat.like,
                                                    coin = videoDetailViewModel.videoDetail!!.stat.coin,
                                                    favorite = videoDetailViewModel.videoDetail!!.stat.favorite,
                                                    upName = videoDetailViewModel.videoDetail!!.author.name,
                                                    upId = videoDetailViewModel.videoDetail!!.author.mid,
                                                    upFace = videoDetailViewModel.videoDetail!!.author.face,
                                                    pubTime = videoDetailViewModel.videoDetail!!.publishDate.formatPubTimeString(),
                                                    audioOnlyMode = audioOnlyMode
                                                )
                                            },
                                            onCharge = { showChargingQrDialog = true },
                                    isLike = liked,
                                            onAddLike = {
                                                scope.launch {
                                                    if (!liked) {
                                                        if (addVideoLike()) {
                                                            liked = true
                                                            "点赞成功".toast(context)
                                                        } else {
                                                            "点赞失败".toast(context)
                                                        }
                                                    }
                                                }
                                            },
                                            onDelLike = {
                                                scope.launch {
                                                    if (liked) {
                                                        if (delVideoLike()) {
                                                            liked = false
                                                            "已取消点赞".toast(context)
                                                        } else {
                                                            "取消点赞失败".toast(context)
                                                        }
                                                    }
                                                }
                                            },
                                            isCoin = isCoin,
                                            onAddCoin = {
                                                scope.launch {
                                                    if (!isCoin) {
                                                        if (addVideoCoin()) {
                                                            isCoin = true
                                                            "投币成功".toast(context)
                                                        } else {
                                                            "投币失败".toast(context)
                                                        }
                                                    }
                                                }
                                            },
                                            isFavorite = favorited,
                                            userFavoriteFolders = favoriteFolderMetadataList,
                                            favoriteFolderIds = videoInFavoriteFolderIds,
                                            onAddToDefaultFavoriteFolder = {
                                                addVideoToDefaultFavoriteFolder()
                                                favorited = true
                                                "已添加到默认收藏夹".toast(context)
                                            },
                                            onUpdateFavoriteFolders = {
                                                updateVideoFavoriteData(it)
                                                favorited = it.isNotEmpty()
                                                videoInFavoriteFolderIds.swapList(it)
                                                if (it.isNotEmpty()) {
                                                    "收藏成功".toast(context)
                                                } else {
                                                    "已取消收藏".toast(context)
                                                }
                                            },
                                            offlineCacheStatus = currentOfflineCacheState.status,
                                            offlineCacheProgress = currentOfflineCacheState.progress,
                                            onCache = { showOfflineCachePanel = true },
                                            hasDescription = videoDetail.description.isNotBlank(),
                                            onShowDescription = { showDescriptionDialog = true },
                                            onShowComment = { showCommentPanel = true },
                                            hasRelatedVideos = collapseVideoInfoRelatedVideos && videoDetailViewModel.relatedVideos.isNotEmpty(),
                                            onShowRelated = { showRelatedPanel = true }
                                        )
                                    }
                                    // 右侧：封面图
                                    Box(
                                        modifier = Modifier
                                            .width(240.dp)
                                            .animateContentSize()
                                            .aspectRatio(coverAspectRatio)
                                    ) {
                                        AsyncImage(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp)),
                                            model = videoDetail.cover,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            onSuccess = { state ->
                                                val drawable = state.result.drawable
                                                val intrinsicWidth = drawable.intrinsicWidth
                                                val intrinsicHeight = drawable.intrinsicHeight
                                                if (intrinsicWidth > 0 && intrinsicHeight > 0) {
                                                    coverAspectRatio = intrinsicWidth.toFloat() / intrinsicHeight.toFloat()
                                                }
                                            },
                                            onError = {
                                                coverAspectRatio = 16f / 9f
                                            }
                                        )

                                        VideoHeroBadges(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(10.dp),
                                            videoDetail = videoDetail
                                        )
                                    }
                                }
                            }
                        }

                        if (videoDetailViewModel.videoDetail?.ugcSeason == null) {
                            item {
                                VideoPartRow(
                                    pages = videoDetailViewModel.videoDetail?.pages ?: emptyList(),
                                    lastPlayedCid = lastPlayedCid,
                                    lastPlayedTime = lastPlayedTime,
                                    enablePartListDialog =
                                        (videoDetailViewModel.videoDetail?.pages?.size ?: 0) > 5,
                                    onClick = { cid ->
                                        logger.fInfo { "Click video part: [av:${videoDetailViewModel.videoDetail?.aid}, bv:${videoDetailViewModel.videoDetail?.bvid}, cid:$cid]" }
                                        val videoDetail = videoDetailViewModel.videoDetail
                                        if (videoDetail == null) {
                                            logger.fWarn { "Skip video part click because video detail is missing: cid=$cid" }
                                        } else {
                                            val selectedPage = videoDetail.pages.find { it.cid == cid }
                                            val fallbackPage = videoDetail.pages.firstOrNull()
                                            launchPlayerActivity(
                                                context = context,
                                                avid = videoDetail.aid,
                                                cid = cid,
                                                title = videoDetail.title,
                                                partTitle = selectedPage?.title.orEmpty(),
                                                played = if (cid == lastPlayedCid) lastPlayedTime * 1000 else 0,
                                                fromSeason = false,
                                                isVerticalVideo = selectedPage?.dimension?.isVertical
                                                    ?: fallbackPage?.dimension?.isVertical
                                                    ?: false,
                                                playerIconIdle = videoDetail.playerIcon?.idle ?: "",
                                                playerIconMoving = videoDetail.playerIcon?.moving ?: "",
                                                play = videoDetail.stat.view,
                                                danmaku = videoDetail.stat.danmaku,
                                                like = videoDetail.stat.like,
                                                coin = videoDetail.stat.coin,
                                                favorite = videoDetail.stat.favorite,
                                                upName = videoDetail.author.name,
                                                upId = videoDetail.author.mid,
                                                upFace = videoDetail.author.face,
                                                pubTime = videoDetail.publishDate.formatPubTimeString(),
                                                audioOnlyMode = audioOnlyMode
                                            )
                                        }
                                    }
                                )
                            }
                        } else {
                            itemsIndexed(items = videoDetailViewModel.videoDetail?.ugcSeason!!.sections) { index, section ->
                                VideoUgcSeasonRow(
                                    title = section.title,
                                    episodes = section.episodes,
                                    lastPlayedCid = lastPlayedCid,
                                    lastPlayedTime = lastPlayedTime,
                                    intentAid = intentAid,
                                    enableUgcListDialog = section.episodes.size > 5,
                                    onClickEp = { aid, cid ->
                                        logger.fInfo { "Click ugc season episode: [av:${videoDetailViewModel.videoDetail?.aid}, bv:${videoDetailViewModel.videoDetail?.bvid}, cid:$cid]" }
                                        updateUgcSeasonSectionVideoList(index)
                                        val videoDetail = videoDetailViewModel.videoDetail
                                        val sectionTitle =
                                            videoDetail?.ugcSeason?.sections?.getOrNull(index)?.title
                                        val episode = section.episodes.find { it.cid == cid }
                                        if (videoDetail == null || episode == null) {
                                            logger.fWarn {
                                                "Skip ugc season episode click because detail or episode is missing: cid=$cid, sectionIndex=$index"
                                            }
                                        } else {
                                            val firstPage = episode.pages.firstOrNull()
                                            val fallbackPage = videoDetail.pages.firstOrNull()
                                            launchPlayerActivity(
                                                context = context,
                                                avid = aid,
                                                cid = cid,
                                                title = if (sectionTitle == "正片") {
                                                    episode.title
                                                } else {
                                                    sectionTitle ?: videoDetail.ugcSeason?.title ?: ""
                                                },
                                                partTitle = if (sectionTitle == "正片") {
                                                    if (episode.pages.size > 1) firstPage?.title.orEmpty() else ""
                                                } else {
                                                    episode.title
                                                },
                                                played = if (cid == lastPlayedCid) lastPlayedTime * 1000 else 0,
                                                fromSeason = false,
                                                isVerticalVideo = fallbackPage?.dimension?.isVertical ?: false,
                                                playerIconIdle = videoDetail.playerIcon?.idle ?: "",
                                                playerIconMoving = videoDetail.playerIcon?.moving ?: "",
                                                play = videoDetail.stat.view,
                                                danmaku = videoDetail.stat.danmaku,
                                                like = videoDetail.stat.like,
                                                coin = videoDetail.stat.coin,
                                                favorite = videoDetail.stat.favorite,
                                                upName = videoDetail.author.name,
                                                upId = videoDetail.author.mid,
                                                upFace = videoDetail.author.face,
                                                pubTime = videoDetail.publishDate.formatPubTimeString(),
                                                audioOnlyMode = audioOnlyMode
                                            )
                                        }
                                    },
                                    onClickEpPart = { episode, cid ->
                                        logger.fInfo { "Click ugc season episode part: [av:${videoDetailViewModel.videoDetail?.aid}, bv:${videoDetailViewModel.videoDetail?.bvid}, cid:$cid]" }
                                        val videoDetail = videoDetailViewModel.videoDetail
                                        val sectionTitle =
                                            videoDetail?.ugcSeason?.sections?.getOrNull(index)?.title
                                        if (videoDetail == null) {
                                            logger.fWarn {
                                                "Skip ugc season episode part click because video detail is missing: cid=$cid, sectionIndex=$index"
                                            }
                                        } else {
                                            val episodePage = episode.pages.find { it.cid == cid }
                                            val selectedPage = videoDetail.pages.find { it.cid == cid }
                                            val fallbackPage = videoDetail.pages.firstOrNull()
                                            launchPlayerActivity(
                                                context = context,
                                                avid = episode.aid,
                                                cid = cid,
                                                title = if (!sectionTitle.isNullOrEmpty()) episode.title else videoDetail.title,
                                                partTitle = episodePage?.title.orEmpty(),
                                                played = if (cid == lastPlayedCid) lastPlayedTime * 1000 else 0,
                                                fromSeason = false,
                                                isVerticalVideo = selectedPage?.dimension?.isVertical
                                                    ?: fallbackPage?.dimension?.isVertical
                                                    ?: false,
                                                playerIconIdle = videoDetail.playerIcon?.idle ?: "",
                                                playerIconMoving = videoDetail.playerIcon?.moving ?: "",
                                                play = videoDetail.stat.view,
                                                danmaku = videoDetail.stat.danmaku,
                                                like = videoDetail.stat.like,
                                                coin = videoDetail.stat.coin,
                                                favorite = videoDetail.stat.favorite,
                                                upName = videoDetail.author.name,
                                                upId = videoDetail.author.mid,
                                                upFace = videoDetail.author.face,
                                                pubTime = videoDetail.publishDate.formatPubTimeString(),
                                                audioOnlyMode = audioOnlyMode
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        if (!collapseVideoInfoRelatedVideos && videoDetailViewModel.relatedVideos.isNotEmpty()) {
                            item {
                                VideosRow(
                                    modifier = Modifier.padding(bottom = 24.dp),
                                    header = stringResource(R.string.video_info_related_video_title),
                                    videos = videoDetailViewModel.relatedVideos,
                                    showMore = {},
                                    onOpenSeasonInfo = { videoData ->
                                        SeasonInfoActivity.actionStart(
                                            context = context,
                                            epId = videoData.epId!!,
                                            proxyArea = ProxyArea.checkProxyArea(videoData.title)
                                        )
                                    },
                                    onOpenVideoInfo = { videoData ->
                                        VideoInfoActivity.actionStart(context, videoData.avid)
                                    }
                                )
                            }
                        }
                    }
                }

            if (topArgueTips.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(top = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    topArgueTips.forEach { tipText ->
                        ArgueTip(text = tipText)
                    }
                }
            }
        }
    }

    TvOfflineCachePanel(
        show = showOfflineCachePanel,
        items = offlineCacheItems,
        loading = offlineCacheLoading,
        stateOf = { target -> offlineCacheService.stateOf(target.aid, target.cid) },
        onLoadQualities = { target ->
            offlineCacheViewModel.getAvailableOfflineCacheQualities(target)
        },
        onDismiss = { if (!offlineCacheLoading) showOfflineCachePanel = false },
        onCacheOne = { target, quality ->
            if (!offlineCacheLoading) {
                offlineCacheLoading = true
                scope.launch(Dispatchers.IO) {
                    val result = offlineCacheViewModel.cacheVideoTarget(target, quality)
                    withContext(Dispatchers.Main) {
                        offlineCacheLoading = false
                        result.fold(
                            onSuccess = { it.toast(context) },
                            onFailure = { (it.localizedMessage ?: "缓存失败").toast(context) }
                        )
                    }
                }
            }
        },
        onCacheAll = { targets, quality ->
            offlineCacheViewModel.cacheVideoTargets(targets, quality).fold(
                onSuccess = { it.toast(context) },
                onFailure = { (it.localizedMessage ?: "添加缓存任务失败").toast(context) }
            )
        },
        onPause = { target ->
            offlineCacheService.pause(target.aid, target.cid).fold(
                onSuccess = { it.toast(context) },
                onFailure = { (it.localizedMessage ?: "暂停失败").toast(context) }
            )
        },
        onResume = { target ->
            offlineCacheService.resume(target.aid, target.cid).fold(
                onSuccess = { it.toast(context) },
                onFailure = { (it.localizedMessage ?: "继续缓存失败").toast(context) }
            )
        },
        onClear = { target ->
            scope.launch {
                offlineCacheService.clearTask(target.aid, target.cid).fold(
                    onSuccess = { it.toast(context) },
                    onFailure = { (it.localizedMessage ?: "清除失败").toast(context) }
                )
            }
        },
        onOpenCachePage = {
            showOfflineCachePanel = false
            OfflineCacheActivity.actionStart(context)
        }
    )

    LaunchedEffect(showOfflineCachePanel) {
        if (showOfflineCachePanel) {
            shouldRestoreCacheButtonFocus = true
        } else if (shouldRestoreCacheButtonFocus) {
            cacheButtonFocusRequester.requestFocusWithRetry()
            shouldRestoreCacheButtonFocus = false
        }
    }

    if (showChargingQrDialog && chargingQrContent.isNotBlank()) {
        VideoChargingQrDialog(
            qrContent = chargingQrContent,
            onDismissRequest = hideChargingQrDialog
        )
    }

    VideoDescriptionDialog(
        show = showDescriptionDialog,
        onHideDialog = { showDescriptionDialog = false },
        description = videoDetailViewModel.videoDetail?.description ?: ""
    )

    // 计算评论面板的初始 episode id（用于 UGC 合集）
    val commentInitialEpisodeId = remember(lastPlayedCid, intentAid, videoDetailViewModel.videoDetail?.ugcSeason) {
        val sections = videoDetailViewModel.videoDetail?.ugcSeason?.sections ?: return@remember -1
        val allEpisodes = sections.flatMap { it.episodes }

        // 优先使用历史记录对应的 episode
        if (lastPlayedCid != 0L) {
            allEpisodes.find { ep ->
                ep.cid == lastPlayedCid || ep.pages.any { it.cid == lastPlayedCid }
            }?.id?.let { return@remember it }
        }

        // 没有历史记录时，使用与 intentAid 匹配的 episode
        if (intentAid != 0L) {
            allEpisodes.find { it.aid == intentAid }?.id?.let { return@remember it }
        }

        -1
    }

    CommentPanel(
        show = showCommentPanel,
        oid = videoDetailViewModel.videoDetail?.aid ?: 0L,
        onHide = { showCommentPanel = false },
        sections = videoDetailViewModel.videoDetail?.ugcSeason?.sections ?: emptyList(),
        initialEpisodeId = commentInitialEpisodeId
    )

    // 浮层关闭后，焦点返回评论按钮
    LaunchedEffect(showCommentPanel) {
        if (showCommentPanel) {
            shouldRestoreCommentButtonFocus = true
        } else if (shouldRestoreCommentButtonFocus) {
            commentButtonFocusRequester.requestFocusWithRetry()
            shouldRestoreCommentButtonFocus = false
        }
    }

    LaunchedEffect(collapseVideoInfoRelatedVideos) {
        if (!collapseVideoInfoRelatedVideos) {
            showRelatedPanel = false
        }
    }

    RelatedVideosPanel(
        show = collapseVideoInfoRelatedVideos && showRelatedPanel,
        videos = videoDetailViewModel.relatedVideos,
        onHide = { showRelatedPanel = false },
        onOpenVideoInfo = { videoData ->
            VideoInfoActivity.actionStart(context, videoData.avid)
        },
        onOpenSeasonInfo = { videoData ->
            SeasonInfoActivity.actionStart(
                context = context,
                epId = videoData.epId!!,
                proxyArea = ProxyArea.checkProxyArea(videoData.title)
            )
        }
    )

    // 推荐面板关闭后，焦点返回推荐按钮
    LaunchedEffect(showRelatedPanel) {
        if (showRelatedPanel) {
            shouldRestoreRelatedButtonFocus = true
        } else if (shouldRestoreRelatedButtonFocus) {
            relatedButtonFocusRequester.requestFocusWithRetry()
            shouldRestoreRelatedButtonFocus = false
        }
    }
}

private data class TvOfflineCacheItem(
    val target: OfflineVideoCacheTarget,
    val title: String,
    val subtitle: String,
    val cover: String,
    val isCurrent: Boolean,
)

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun TvOfflineCachePanel(
    show: Boolean,
    items: List<TvOfflineCacheItem>,
    loading: Boolean,
    stateOf: (OfflineVideoCacheTarget) -> OfflineVideoCacheTaskState,
    onLoadQualities: suspend (OfflineVideoCacheTarget) -> Result<List<Resolution>>,
    onDismiss: () -> Unit,
    onCacheOne: (OfflineVideoCacheTarget, Resolution) -> Unit,
    onCacheAll: (List<OfflineVideoCacheTarget>, Resolution) -> Unit,
    onPause: (OfflineVideoCacheTarget) -> Unit,
    onResume: (OfflineVideoCacheTarget) -> Unit,
    onClear: (OfflineVideoCacheTarget) -> Unit,
    onOpenCachePage: () -> Unit,
) {
    if (!show) return

    val context = LocalContext.current
    val preferredQuality = remember(show) { Prefs.defaultOfflineCacheQuality }
    var selectedQuality by remember(show) { mutableStateOf(preferredQuality) }
    var qualityOptions by remember(show) { mutableStateOf(emptyList<Resolution>()) }
    var qualityLoading by remember(show) { mutableStateOf(false) }
    var qualityLoadError by remember(show) { mutableStateOf<String?>(null) }
    var qualityReloadKey by remember(show) { mutableIntStateOf(0) }
    val qualityFocusRequester = remember { FocusRequester() }
    val qualityListState = rememberLazyListState()
    val cacheableTargets = items.map { it.target }.filter { target ->
        stateOf(target).status == OfflineVideoCacheStatus.Idle ||
            stateOf(target).status == OfflineVideoCacheStatus.Failed
    }
    val qualityTarget = items.firstOrNull { it.isCurrent }?.target ?: items.firstOrNull()?.target
    val currentItemIndex = remember(items) {
        items.indexOfFirst { it.isCurrent }.coerceAtLeast(0)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentItemIndex)
    val switchSelectedQuality: (Int) -> Unit = { direction ->
        if (qualityOptions.isNotEmpty()) {
            val selectedIndex = qualityOptions.indexOf(selectedQuality)
                .takeIf { it >= 0 }
                ?: 0
            selectedQuality = qualityOptions[
                (selectedIndex + direction).coerceIn(0, qualityOptions.lastIndex)
            ]
        }
    }

    LaunchedEffect(show, qualityTarget?.aid, qualityTarget?.cid, qualityReloadKey) {
        if (!show || qualityTarget == null) return@LaunchedEffect

        qualityLoading = true
        qualityLoadError = null
        qualityOptions = emptyList()
        withContext(Dispatchers.IO) { onLoadQualities(qualityTarget) }
            .fold(
                onSuccess = { availableQualities ->
                    qualityOptions = availableQualities
                    selectedQuality = OfflineCacheQualitySelector.select(
                        availableQualities = availableQualities,
                        preferredQuality = preferredQuality,
                    ) ?: preferredQuality
                },
                onFailure = { error ->
                    qualityLoadError = error.localizedMessage ?: "读取画质失败"
                }
            )
        qualityLoading = false
    }
    LaunchedEffect(show, qualityLoading, qualityOptions) {
        if (!show || qualityLoading) return@LaunchedEffect
        val selectedIndex = qualityOptions.indexOf(selectedQuality)
        withFrameNanos { }
        qualityFocusRequester.requestFocus()
        withFrameNanos { }
        if (selectedIndex >= 0) {
            qualityListState.scrollToItemIfAvailable(
                qualityWindowStartIndex(qualityOptions.size, selectedIndex)
            )
        }
    }
    LaunchedEffect(show, selectedQuality) {
        if (!show) return@LaunchedEffect
        val selectedIndex = qualityOptions.indexOf(selectedQuality)
        if (selectedIndex >= 0) {
            qualityListState.scrollToItemIfAvailable(
                qualityWindowStartIndex(qualityOptions.size, selectedIndex)
            )
        }
    }
    LaunchedEffect(show, currentItemIndex, items.size) {
        if (show && items.isNotEmpty()) {
            listState.scrollToItemIfAvailable(currentItemIndex)
        }
    }

    TvOverlayDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 18.dp, top = 34.dp, bottom = 34.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Surface(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = SurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("离线缓存", style = MaterialTheme.typography.headlineSmall)
                                Text(
                                    when {
                                        qualityLoading -> "正在读取当前分P画质"
                                        qualityOptions.isNotEmpty() -> {
                                            "当前分P ${qualityOptions.size} 档 · 默认优先 ${preferredQuality.getShortDisplayName(context)}"
                                        }
                                        else -> "选择分P和最高画质"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                modifier = if (qualityOptions.isEmpty() && !qualityLoading) {
                                    Modifier.focusRequester(qualityFocusRequester)
                                } else Modifier,
                                onClick = onOpenCachePage
                            ) { Text("查看缓存") }
                        }
                        when {
                            qualityLoading -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        "正在匹配可缓存画质…",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            qualityOptions.isNotEmpty() -> {
                                CompositionLocalProvider(
                                    LocalBringIntoViewSpec provides NoScrollBringIntoViewSpec
                                ) {
                                    LazyRow(
                                        modifier = Modifier
                                            .width(256.dp)
                                            .height(42.dp),
                                        state = qualityListState,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(
                                            items = qualityOptions,
                                            key = { it.code }
                                        ) { quality ->
                                            Button(
                                                modifier = Modifier
                                                    .width(80.dp)
                                                    .height(42.dp)
                                                    .onFocusChanged { focusState ->
                                                        if (focusState.isFocused) {
                                                            selectedQuality = quality
                                                        }
                                                    }
                                                    .then(
                                                        if (selectedQuality == quality) {
                                                            Modifier.focusRequester(qualityFocusRequester)
                                                        } else Modifier
                                                    ),
                                                onClick = { selectedQuality = quality },
                                                scale = ButtonDefaults.scale(
                                                    scale = 1f,
                                                    focusedScale = 1f,
                                                    pressedScale = 1f,
                                                ),
                                                colors = ButtonDefaults.colors(
                                                    containerColor = if (selectedQuality == quality) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = if (selectedQuality == quality) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                            ) {
                                                Text(
                                                    text = quality.getShortDisplayName(context),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        qualityLoadError ?: "暂无可缓存画质",
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    OutlinedButton(onClick = { qualityReloadKey++ }) {
                                        Text("重试")
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                    )

                    if (items.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("视频列表尚未加载完成", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .onPreviewKeyEvent { event ->
                                    if (event.key != Key.DirectionLeft && event.key != Key.DirectionRight) {
                                        return@onPreviewKeyEvent false
                                    }
                                    if (event.type == KeyEventType.KeyDown) {
                                        switchSelectedQuality(
                                            if (event.key == Key.DirectionLeft) -1 else 1
                                        )
                                    }
                                    true
                                },
                            state = listState,
                            contentPadding = PaddingValues(14.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            items(items, key = { "${it.target.aid}:${it.target.cid}" }) { item ->
                                val state = stateOf(item.target)
                                TvOfflineCacheRow(
                                    item = item,
                                    state = state,
                                    enabled = !loading,
                                    onClick = {
                                        when {
                                            state.isActive -> onPause(item.target)
                                            state.status == OfflineVideoCacheStatus.Paused -> onResume(item.target)
                                            state.status == OfflineVideoCacheStatus.Failed -> onClear(item.target)
                                            state.status == OfflineVideoCacheStatus.Completed -> onOpenCachePage()
                                            else -> onCacheOne(item.target, selectedQuality)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = onDismiss
                        ) { Text("取消") }
                        Button(
                            modifier = Modifier.weight(1f),
                            enabled = !loading && !qualityLoading &&
                                qualityOptions.isNotEmpty() && cacheableTargets.isNotEmpty(),
                            onClick = { onCacheAll(cacheableTargets, selectedQuality) }
                        ) {
                            Text(if (loading) "正在添加" else "缓存全部 ${cacheableTargets.size}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvOfflineCacheRow(
    item: TvOfflineCacheItem,
    state: OfflineVideoCacheTaskState,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp),
        onClick = { if (enabled) onClick() },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (item.isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
            focusedContainerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.large),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                modifier = Modifier
                    .width(88.dp)
                    .height(52.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                model = item.cover,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (state.totalBytes > 0L && state.status != OfflineVideoCacheStatus.Completed) {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth(),
                        gapSize = 0.dp
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    imageVector = when {
                        state.status == OfflineVideoCacheStatus.Completed -> Icons.Rounded.Check
                        state.status == OfflineVideoCacheStatus.Paused -> Icons.Rounded.PlayCircle
                        state.status == OfflineVideoCacheStatus.Failed -> Icons.Rounded.Refresh
                        state.isActive -> Icons.Rounded.Pause
                        else -> Icons.Rounded.Download
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = when {
                        state.status == OfflineVideoCacheStatus.Completed -> "已缓存"
                        state.status == OfflineVideoCacheStatus.Paused -> "继续"
                        state.status == OfflineVideoCacheStatus.Failed -> "清除"
                        state.isActive -> "${(state.progress * 100).toInt()}%"
                        else -> "缓存"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun qualityWindowStartIndex(
    itemCount: Int,
    selectedIndex: Int,
    visibleItemCount: Int = 3,
): Int {
    if (itemCount <= visibleItemCount) return 0
    return (selectedIndex - visibleItemCount / 2)
        .coerceIn(0, itemCount - visibleItemCount)
}

private fun buildTvOfflineCacheItems(
    videoDetail: VideoDetail?,
    currentAid: Long,
    preferredCid: Long,
): List<TvOfflineCacheItem> {
    if (videoDetail == null) return emptyList()

    val ugcItems = videoDetail.ugcSeason?.sections
        ?.flatMap { section ->
            section.episodes.flatMap { episode ->
                if (episode.pages.size > 1) {
                    episode.pages.map { page ->
                        val target = OfflineVideoCacheTarget(
                            aid = episode.aid,
                            bvid = episode.bvid,
                            cid = page.cid,
                            title = episode.title,
                            partTitle = "${episode.title} - ${page.title}",
                            cover = episode.cover.ifBlank { videoDetail.cover },
                            upName = videoDetail.author.name,
                            upFace = videoDetail.author.face,
                            danmakuCount = videoDetail.stat.danmaku,
                            durationMs = page.duration * 1000L,
                            width = page.dimension.width,
                            height = page.dimension.height
                        )
                        TvOfflineCacheItem(
                            target = target,
                            title = "${episode.title} · P${page.index} ${page.title}",
                            subtitle = listOfNotNull(
                                section.title.takeIf { it.isNotBlank() },
                                target.durationMs.formatHourMinSec()
                            ).joinToString(" · "),
                            cover = target.cover,
                            isCurrent = false
                        )
                    }
                } else {
                    val page = episode.pages.firstOrNull()
                    val targetCid = page?.cid ?: episode.cid
                    val target = OfflineVideoCacheTarget(
                        aid = episode.aid,
                        bvid = episode.bvid,
                        cid = targetCid,
                        title = episode.title,
                        partTitle = "",
                        cover = episode.cover.ifBlank { videoDetail.cover },
                        upName = videoDetail.author.name,
                        upFace = videoDetail.author.face,
                        danmakuCount = videoDetail.stat.danmaku,
                        durationMs = (page?.duration ?: episode.duration) * 1000L,
                        width = page?.dimension?.width ?: episode.dimension?.width ?: 0,
                        height = page?.dimension?.height ?: episode.dimension?.height ?: 0
                    )
                    listOf(
                        TvOfflineCacheItem(
                            target = target,
                            title = episode.title,
                            subtitle = listOfNotNull(
                                section.title.takeIf { it.isNotBlank() },
                                target.durationMs.formatHourMinSec()
                            ).joinToString(" · "),
                            cover = target.cover,
                            isCurrent = false
                        )
                    )
                }
            }
        }
        .orEmpty()
    if (ugcItems.isNotEmpty()) {
        return ugcItems.markCurrentCacheItem(currentAid, preferredCid)
    }

    return videoDetail.pages.map { page ->
        val target = OfflineVideoCacheTarget(
            aid = videoDetail.aid,
            bvid = videoDetail.bvid,
            cid = page.cid,
            title = videoDetail.title,
            partTitle = page.title,
            cover = videoDetail.cover,
            upName = videoDetail.author.name,
            upFace = videoDetail.author.face,
            danmakuCount = videoDetail.stat.danmaku,
            durationMs = page.duration * 1000L,
            width = page.dimension.width,
            height = page.dimension.height
        )
        TvOfflineCacheItem(
            target = target,
            title = if (videoDetail.pages.size > 1) "P${page.index} ${page.title}" else videoDetail.title,
            subtitle = listOfNotNull(
                target.durationMs.formatHourMinSec()
            ).joinToString(" · "),
            cover = target.cover,
            isCurrent = false
        )
    }.markCurrentCacheItem(currentAid, preferredCid)
}

private fun List<TvOfflineCacheItem>.markCurrentCacheItem(
    currentAid: Long,
    preferredCid: Long,
): List<TvOfflineCacheItem> {
    if (isEmpty()) return this

    val exactIndex = indexOfFirst {
        it.target.aid == currentAid && it.target.cid == preferredCid
    }
    val currentAidIndex = indexOfFirst { it.target.aid == currentAid }
    val preferredCidIndex = indexOfFirst { it.target.cid == preferredCid }
    val currentIndex = listOf(exactIndex, currentAidIndex, preferredCidIndex)
        .firstOrNull { it >= 0 }
        ?: 0

    return mapIndexed { index, item ->
        val isCurrent = index == currentIndex
        item.copy(
            isCurrent = isCurrent,
            subtitle = if (isCurrent) {
                listOf(item.subtitle, "当前播放")
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
            } else {
                item.subtitle
            }
        )
    }
}

private fun VideoDetail.isCurrentDetailTargetVertical(
    lastPlayedCid: Long,
    intentAid: Long,
): Boolean {
    val episodes = ugcSeason?.sections?.flatMap { it.episodes }.orEmpty()
    if (episodes.isNotEmpty()) {
        if (lastPlayedCid != 0L) {
            episodes.firstNotNullOfOrNull { episode ->
                episode.verticalStateForCid(lastPlayedCid)
            }?.let { return it }
        }

        if (intentAid != 0L) {
            episodes.firstOrNull { it.aid == intentAid }?.dimension?.isVertical?.let { return it }
        }

        return false
    }

    return when {
        lastPlayedCid != 0L -> {
            pages.firstOrNull { it.cid == lastPlayedCid }?.dimension?.isVertical
                ?: pages.firstOrNull()?.dimension?.isVertical
        }

        else -> pages.firstOrNull()?.dimension?.isVertical
    } == true
}

private fun Episode.verticalStateForCid(cid: Long): Boolean? {
    pages.firstOrNull { it.cid == cid }?.dimension?.isVertical?.let { return it }
    if (this.cid == cid) {
        return dimension?.isVertical
    }
    return null
}

@Composable
private fun VideoHeroBadges(
    videoDetail: VideoDetail,
    modifier: Modifier = Modifier,
) {
    if (!videoDetail.isInteractive && !videoDetail.isChargingArc) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (videoDetail.isInteractive) {
            Row(
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = MaterialTheme.shapes.extraSmall
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    imageVector = Icons.Rounded.PlayCircle,
                    contentDescription = null,
                    tint = InteractiveBadgeColor
                )
                Text(
                    text = "互动视频",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InteractiveBadgeColor
                )
            }
        }

        if (videoDetail.isChargingArc) {
            Text(
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = MaterialTheme.shapes.extraSmall
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                text = if (videoDetail.chargingArcBadge.isNotBlank()) {
                    "⚡${videoDetail.chargingArcBadge}"
                } else {
                    "⚡充电专属"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = ChargingBadgeColor
            )
        }
    }
}

@Composable
private fun VideoTagsRow(
    tags: List<Tag>,
    modifier: Modifier = Modifier,
    onClickTag: (Tag) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val currentOnClickTag by rememberUpdatedState(onClickTag)
    val tagContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    val tagFocusedContainerColor = MaterialTheme.colorScheme.onSurface

    LazyRow(
        modifier = modifier.focusRestorer(focusRequester),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(items = tags, key = { _, tag -> tag.id }) { index, tag ->
            var hasFocus by remember(tag.id) { mutableStateOf(false) }
            val displayTagName = remember(tag.name) { formatVideoTagName(tag.name) }

            Surface(
                modifier = Modifier
                    .ifElse(index == 0, Modifier.focusRequester(focusRequester))
                    .onFocusChanged { hasFocus = it.hasFocus },
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = tagContainerColor,
                    focusedContainerColor = tagFocusedContainerColor,
                    pressedContainerColor = tagFocusedContainerColor
                ),
                scale = ClickableSurfaceDefaults.scale(scale = 1f, focusedScale = 1.05f),
                shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(4.dp)),
                border = ClickableSurfaceDefaults.border(
                    border = Border.None,
                    focusedBorder = Border.None,
                    pressedBorder = Border.None
                ),
                onClick = { currentOnClickTag(tag) }
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    text = displayTagName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasFocus) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AutoResizeTitleText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineLarge,
    minFontSize: androidx.compose.ui.unit.TextUnit = 24.sp,
    maxLines: Int = 2,
    color: Color = Color.White,
) {
    var resizedStyle by remember(text, style) { mutableStateOf(style) }
    var readyToDraw by remember(text, style) { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            if (readyToDraw) {
                drawContent()
            }
        },
        style = resizedStyle,
        maxLines = maxLines,
        overflow = TextOverflow.Clip,
        color = color,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && resizedStyle.fontSize > minFontSize) {
                resizedStyle = resizedStyle.copy(fontSize = resizedStyle.fontSize * 0.95f)
            } else {
                readyToDraw = true
            }
        }
    )
}

@Composable
fun ArgueTip(
    modifier: Modifier = Modifier,
    text: String
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        colors = SurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.Yellow
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Yellow.copy(alpha = 0.22f),
                            Color.Yellow.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 6.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = Color.Yellow
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun VideoDescriptionDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    description: String
) {
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(show) {
        if (show) {
            focusRequester.requestFocus()
        }
    }

    if (show) {
        TvOverlayDialog(
            onDismissRequest = { onHideDialog() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = modifier
                    .fillMaxWidth(0.6f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                // 底层：深色玻璃背景
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                )
                // 中层：微带蓝紫色调
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0xFF1a1a2e).copy(alpha = 0.7f))
                )
                // 顶层：高光渐变
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.video_info_description_title),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 320.dp)
                            .focusable()
                            .focusRequester(focusRequester)
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.key) {
                                        Key.DirectionUp -> {
                                            scope.launch { state.animateScrollBy(-state.layoutInfo.viewportSize.height / 3f) }
                                            true
                                        }

                                        Key.DirectionDown -> {
                                            scope.launch { state.animateScrollBy(state.layoutInfo.viewportSize.height / 3f) }
                                            true
                                        }

                                        else -> false
                                    }
                                } else {
                                    false
                                }
                            },
                        state = state
                    ) {
                        item {
                            Text(
                                text = description,
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoChargingQrDialog(
    qrContent: String,
    onDismissRequest: () -> Unit
) {
    TvAlertDialog(
        modifier = Modifier.widthIn(min = 760.dp, max = 860.dp),
        text = {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QrImage(
                    modifier = Modifier.size(220.dp),
                    content = qrContent,
                    borderWidth = 20.dp,
                    showLoadingWhenContentChanged = false
                )
                Column(
                    modifier = Modifier.widthIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "手机扫码前往充电页面",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    Text(
                        text = "请使用哔哩哔哩手机客户端扫码",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text(text = "关闭")
            }
        },
        containerColor = Color(0xFF111111).copy(alpha = 0.96f),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

@Composable
private fun VideoPartButton(
    modifier: Modifier = Modifier,
    index: Int,
    title: String,
    duration: Int,
    played: Int = 0,
    isLastPlayed: Boolean = false,
    isCurrentIntent: Boolean = false,
    type: VideoPartType = VideoPartType.Part,
    onClick: () -> Unit
) {
    var hasFocus by remember { mutableStateOf(false) }
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val goldColor = Color(0xFFE39B17)
    val lastPlayedColor = Color(0xFFE39B17)
    val focusedColor = if (isDarkTheme) Color(0xFFF1CD8B) else MaterialTheme.colorScheme.primary
    val partContentColor = MaterialTheme.colorScheme.onSurface
    val partSecondaryContentColor = MaterialTheme.colorScheme.onSurfaceVariant

    val progressFraction = remember(played, duration) {
        if (duration <= 0) 0f
        else if (played < 0) 1f
        else (played.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }

    val durationText = remember(duration) {
        if (duration <= 0) ""
        else {
            val min = duration / 60
            val sec = duration % 60
            "%d:%02d".format(min, sec)
        }
    }

    val prefix = when (type) {
        VideoPartType.Episode -> "EP"
        VideoPartType.Part -> "P"
    }

    Surface(
        modifier = modifier.onFocusChanged { hasFocus = it.hasFocus },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            pressedContainerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        scale = ClickableSurfaceDefaults.scale(scale = 1f, focusedScale = 1.05f),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
        border = ClickableSurfaceDefaults.border(
            border = if (isCurrentIntent) Border(
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) else Border.None,
            focusedBorder = Border(
                border = BorderStroke(3.dp, if (isLastPlayed) lastPlayedColor else focusedColor),
                shape = RoundedCornerShape(12.dp)
            ),
            pressedBorder = Border(
                border = BorderStroke(3.dp, if (isLastPlayed) lastPlayedColor else focusedColor),
                shape = RoundedCornerShape(12.dp)
            )
        ),
        onClick = { onClick() }
    ) {
        Box(
            modifier = Modifier.width(200.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 编号标签
                    Text(
                        text = "$prefix$index",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isLastPlayed) goldColor
                        else if (hasFocus) partContentColor
                        else partSecondaryContentColor.copy(alpha = 0.72f),
                        maxLines = 1
                    )

                    // 标题 + 时长/状态
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isLastPlayed) partContentColor
                            else partContentColor.copy(alpha = 0.88f),
                            minLines = 2,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isLastPlayed) {
                                Icon(
                                    modifier = Modifier.size(12.dp),
                                    imageVector = Icons.Rounded.PlayCircle,
                                    contentDescription = null,
                                    tint = goldColor
                                )
                            }
                            if (durationText.isNotEmpty()) {
                                Text(
                                    text = durationText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = partSecondaryContentColor.copy(alpha = 0.72f)
                                )
                            }
                        }
                    }
                }

                // 底部进度条
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                        .background(partContentColor.copy(alpha = 0.12f))
                ) {
                    if (progressFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressFraction)
                                .background(Color(0xFF00E676))
                        )
                    }
                }
            }
        }
    }
}

private enum class VideoPartType {
    Episode, Part
}

@Composable
private fun VideoPartRowButton(
    modifier: Modifier = Modifier,
    buttonSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val iconSize = max(buttonSize.value * 0.75f, 16f).dp

    Surface(
        modifier = modifier.size(buttonSize),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            pressedContainerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            )
        ),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier
                    .size(iconSize)
                    .rotate(90f),
                imageVector = Icons.Rounded.ViewModule,
                contentDescription = null
            )
        }
    }
}

@Composable
fun VideoPartRow(
    modifier: Modifier = Modifier,
    pages: List<VideoPage>,
    lastPlayedCid: Long = 0,
    lastPlayedTime: Int = 0,
    enablePartListDialog: Boolean = false,
    nested: Boolean = false,
    subtitle: String = "",
    titleText: String? = null,
    dialogTitle: String = "分 P 列表",
    onClick: (cid: Long) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    var showPartListDialog by remember { mutableStateOf(false) }
    val partRowContentColor = MaterialTheme.colorScheme.onSurface
    val partRowSecondaryContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    val listState = rememberLazyListState()
    val initialFocusIndex = remember(lastPlayedCid, pages) {
        pages.indexOfFirst { it.cid == lastPlayedCid }
            .takeIf { it >= 0 }
            ?: 0
    }
    val titleFontSize by animateFloatAsState(
        targetValue = if (hasFocus) 21f else 14f,
        label = "title font size",
        animationSpec = tween(
            durationMillis = 120
        )
    )

    // 滚动到有历史记录的那一集
    LaunchedEffect(lastPlayedCid, pages) {
        if (lastPlayedCid != 0L && pages.isNotEmpty()) {
            val index = pages.indexOfFirst { it.cid == lastPlayedCid }
            if (index > 0) {
                listState.scrollToItemIfAvailable(index)
            }
        }
    }

    Column(
        modifier = modifier
            .ifElse(!nested, Modifier.padding(start = 26.dp))
            .ifElse(
                nested,
                Modifier
                    .padding(horizontal = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f))
                    .padding(top = 10.dp, bottom = 8.dp)
            )
            .onFocusChanged { hasFocus = it.hasFocus },
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                modifier = Modifier.weight(1f, fill = false),
                text = (titleText ?: stringResource(R.string.video_info_part_row_title))
                        + (" - $subtitle".takeIf { subtitle.isNotBlank() } ?: ""),
                fontSize = titleFontSize.sp,
                color = if (hasFocus) partRowContentColor else partRowSecondaryContentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (nested && pages.size > 1) {
                Text(
                    modifier = Modifier
                        .background(
                            color = UgcPartBadgeColor.copy(alpha = if (hasFocus) 0.26f else 0.14f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    text = "共${pages.size}P",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasFocus) UgcPartBadgeColor else partRowSecondaryContentColor,
                    maxLines = 1
                )
            }
            if (enablePartListDialog) {
                VideoPartRowButton(
                    buttonSize = max(titleFontSize * 1.5f, 21f).dp,
                    onClick = { showPartListDialog = true }
                )
            }
        }

        LazyRow(
            modifier = Modifier
                .padding(top = 4.dp)
                .focusRestorer(focusRequester),
            state = listState,
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(items = pages, key = { index, page -> "$index:${page.cid}" }) { index, page ->
                VideoPartButton(
                    modifier = Modifier
                        .ifElse(index == initialFocusIndex, Modifier.focusRequester(focusRequester)),
                    index = index + 1,
                    title = page.title,
                    played = if (page.cid == lastPlayedCid) lastPlayedTime else 0,
                    isLastPlayed = page.cid == lastPlayedCid,
                    duration = page.duration,
                    onClick = { onClick(page.cid) }
                )
            }
        }
    }

    VideoPartListDialog(
        show = showPartListDialog,
        onHideDialog = { showPartListDialog = false },
        pages = pages,
        lastPlayedCid = lastPlayedCid,
        lastPlayedTime = lastPlayedTime,
        title = dialogTitle,
        onClick = onClick
    )
}

@Composable
fun VideoUgcSeasonRow(
    modifier: Modifier = Modifier,
    title: String,
    episodes: List<Episode>,
    lastPlayedCid: Long = 0,
    lastPlayedTime: Int = 0,
    intentAid: Long = 0,
    enableUgcListDialog: Boolean = false,
    onClickEp: (avid: Long, cid: Long) -> Unit,
    onClickEpPart: (episode: Episode, cid: Long) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    var showUgcListDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val initialFocusIndex = remember(lastPlayedCid, intentAid, episodes) {
        when {
            lastPlayedCid != 0L -> {
                episodes.indexOfFirst {
                    it.cid == lastPlayedCid || it.pages.any { page -> page.cid == lastPlayedCid }
                }
            }

            intentAid != 0L -> episodes.indexOfFirst { it.aid == intentAid }
            else -> -1
        }.takeIf { it >= 0 } ?: 0
    }
    val titleColor = if (hasFocus) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val titleFontSize by animateFloatAsState(
        targetValue = if (hasFocus) 30f else 14f,
        label = "title font size",
        animationSpec = tween(
            durationMillis = 120
        )
    )
    var focusingEpisode by remember(episodes, initialFocusIndex) {
        mutableStateOf(episodes.getOrNull(initialFocusIndex))
    }

    // 滚动到有历史记录的那一集，如果没有历史记录则滚动到与 intentAid 相同的视频
    LaunchedEffect(lastPlayedCid, intentAid, episodes) {
        if (episodes.isEmpty()) return@LaunchedEffect

        val index = if (lastPlayedCid != 0L) {
            // 优先使用历史记录
            episodes.indexOfFirst { it.cid == lastPlayedCid || it.pages.any { page -> page.cid == lastPlayedCid } }
        } else if (intentAid != 0L) {
            // 没有历史记录时，滚动到与 intentAid 相同的视频
            episodes.indexOfFirst { it.aid == intentAid }
        } else {
            -1
        }

        if (index > 0) {
            listState.scrollToItemIfAvailable(index)
        }
    }

    Column(
        modifier = modifier
            .padding(bottom = 24.dp)
            .onFocusChanged { hasFocus = it.hasFocus },
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.padding(start = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontSize = titleFontSize.sp,
                color = titleColor
            )
            if (enableUgcListDialog) {
                VideoPartRowButton(
                    buttonSize = 32.dp,
                    onClick = { showUgcListDialog = true }
                )
            }
        }

        LazyRow(
            modifier = Modifier
                .padding(top = 15.dp)
                .focusRestorer(focusRequester),
            state = listState,
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(items = episodes) { index, episode ->
                val episodeTitle by remember { mutableStateOf(ugcEpisodeDisplayTitle(episode)) }
                val isLastPlayed = remember(episode, lastPlayedCid) {
                    episode.cid == lastPlayedCid || episode.pages.any { it.cid == lastPlayedCid }
                }
                val playedSeconds = remember(episode, lastPlayedCid, lastPlayedTime) {
                    calculateUgcEpisodePlayedSeconds(
                        episodeCid = episode.cid,
                        pages = episode.pages,
                        episodeDurationSeconds = episode.duration,
                        lastPlayedCid = lastPlayedCid,
                        currentPagePlayedSeconds = lastPlayedTime
                    )
                }
                UgcEpisodeButton(
                    modifier = Modifier
                        .ifElse(index == initialFocusIndex, Modifier.focusRequester(focusRequester))
                        .onFocusChanged { if (it.hasFocus) focusingEpisode = episode },
                    title = episodeTitle,
                    cover = episode.cover,
                    viewCount = episode.viewCount,
                    danmakuCount = episode.danmakuCount,
                    isInteractive = episode.isInteractive,
                    isChargingArc = episode.isChargingArc,
                    chargingArcBadge = episode.chargingArcBadge,
                    partCount = episode.pages.size,
                    played = playedSeconds,
                    isLastPlayed = isLastPlayed,
                    duration = episode.duration,
                    onClick = { onClickEp(episode.aid, episode.cid) }
                )
            }
        }

        AnimatedVisibility((focusingEpisode?.pages?.size ?: 0) > 1) {
            VideoPartRow(
                modifier = Modifier.padding(top = 8.dp),
                pages = focusingEpisode!!.pages,
                lastPlayedCid = lastPlayedCid,
                lastPlayedTime = lastPlayedTime,
                enablePartListDialog = (focusingEpisode?.pages?.size ?: 0) > 5,
                nested = true,
                titleText = "分 P",
                dialogTitle = "${focusingEpisode!!.title} - 分 P 列表",
                onClick = { onClickEpPart(focusingEpisode!!, it) },
                subtitle = focusingEpisode!!.title
            )
        }
    }

    VideoUgcListDialog(
        show = showUgcListDialog,
        onHideDialog = { showUgcListDialog = false },
        episodes = episodes,
        lastPlayedCid = lastPlayedCid,
        lastPlayedTime = lastPlayedTime,
        intentAid = intentAid,
        sectionTitle = title,
        title = "合集列表",
        onClick = onClickEp
    )
}

private fun ugcEpisodeDisplayTitle(episode: Episode): String {
    return episode.longTitle.ifBlank { episode.title }
}

/**
 * Converts the history position for one P into the position within the whole UGC episode.
 * Both page durations and history progress use seconds here.
 */
internal fun calculateUgcEpisodePlayedSeconds(
    episodeCid: Long,
    pages: List<VideoPage>,
    episodeDurationSeconds: Int,
    lastPlayedCid: Long,
    currentPagePlayedSeconds: Int
): Long {
    val currentPageIndex = pages.indexOfFirst { it.cid == lastPlayedCid }
    if (lastPlayedCid == 0L || (episodeCid != lastPlayedCid && currentPageIndex < 0)) return 0L

    val episodeDuration = episodeDurationSeconds.coerceAtLeast(0).toLong()
    if (currentPagePlayedSeconds < 0) return episodeDuration

    val playedBeforeCurrentPage = if (currentPageIndex > 0) {
        pages.take(currentPageIndex).sumOf { it.duration.coerceAtLeast(0).toLong() }
    } else {
        0L
    }
    val currentPageDuration = pages.getOrNull(currentPageIndex)
        ?.duration
        ?.coerceAtLeast(0)
        ?.toLong()
        ?: 0L
    val playedInCurrentPage = currentPagePlayedSeconds.coerceAtLeast(0).toLong().let { played ->
        if (currentPageDuration > 0L) played.coerceAtMost(currentPageDuration) else played
    }
    val cumulativePlayed = playedBeforeCurrentPage + playedInCurrentPage

    return if (episodeDuration > 0L) {
        cumulativePlayed.coerceAtMost(episodeDuration)
    } else {
        cumulativePlayed
    }
}

private fun formatUgcEpisodeCount(count: Long): String {
    return when {
        count >= 10000 -> "${count / 10000}万"
        else -> count.toString()
    }
}

@Composable
private fun UgcEpisodeButton(
    modifier: Modifier = Modifier,
    title: String,
    cover: String,
    viewCount: Long = 0,
    danmakuCount: Int = 0,
    isInteractive: Boolean = false,
    isChargingArc: Boolean = false,
    chargingArcBadge: String = "",
    partCount: Int = 1,
    duration: Int,
    played: Long = 0,
    isLastPlayed: Boolean = false,
    onClick: () -> Unit
) {
    val isPreview = LocalInspectionMode.current
    val lastPlayedColor = Color(0xFFE39B17)
    val focusedColor = Color(0xFFF1CD8B)

    val durationText = remember(duration) {
        if (duration <= 0) ""
        else {
            val min = duration / 60
            val sec = duration % 60
            "%d:%02d".format(min, sec)
        }
    }

    val progressFraction = remember(played, duration) {
        if (duration <= 0) 0f
        else if (played < 0) 1f
        else (played.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }
    val playText = remember(viewCount) {
        if (viewCount > 0) formatUgcEpisodeCount(viewCount) else ""
    }
    val danmakuText = remember(danmakuCount) {
        if (danmakuCount > 0) formatUgcEpisodeCount(danmakuCount.toLong()) else ""
    }

    Surface(
        modifier = modifier.width(220.dp),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            pressedContainerColor = Color.Transparent
        ),
        scale = ClickableSurfaceDefaults.scale(scale = 1f, focusedScale = 1.05f),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        border = ClickableSurfaceDefaults.border(
            border = Border.None,
            focusedBorder = Border(
                border = BorderStroke(2.dp, if (isLastPlayed) lastPlayedColor else focusedColor),
                shape = MaterialTheme.shapes.medium
            ),
            pressedBorder = Border.None
        ),
        onClick = onClick
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(if (isPreview) Color.Gray else Color.Black)
            ) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = cover.resizedImageUrl(ImageSize.TvEpisodeCover),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )

                VideoEpisodeBadges(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    isInteractive = isInteractive,
                    isChargingArc = isChargingArc,
                    chargingArcBadge = chargingArcBadge
                )

                if (partCount > 1) {
                    Text(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.62f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = UgcPartBadgeColor.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        text = "共${partCount}P",
                        style = MaterialTheme.typography.labelSmall,
                        color = UgcPartBadgeColor,
                        maxLines = 1
                    )
                }

                if (playText.isNotEmpty() || danmakuText.isNotEmpty() || durationText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.8f)
                                    )
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier
                                .background(
                                    Color.Black.copy(alpha = 0.7f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (playText.isNotEmpty()) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_play_count),
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = playText,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }

                            if (danmakuText.isNotEmpty()) {
                                if (playText.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_danmaku_count),
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = danmakuText,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        if (durationText.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color.Black.copy(alpha = 0.7f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = durationText,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                if (isLastPlayed) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier.size(36.dp),
                            imageVector = Icons.Rounded.PlayCircle,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                if (progressFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressFraction)
                                .background(lastPlayedColor)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun VideoEpisodeBadges(
    modifier: Modifier = Modifier,
    isInteractive: Boolean,
    isChargingArc: Boolean,
    chargingArcBadge: String,
) {
    if (!isInteractive && !isChargingArc) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.End
    ) {
        if (isInteractive) {
            Row(
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    modifier = Modifier.size(14.dp),
                    imageVector = Icons.Rounded.PlayCircle,
                    contentDescription = null,
                    tint = InteractiveBadgeColor
                )
                Text(
                    text = "互动视频",
                    style = MaterialTheme.typography.labelSmall,
                    color = InteractiveBadgeColor,
                    maxLines = 1
                )
            }
        }

        if (isChargingArc) {
            Text(
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                text = "⚡${chargingArcBadge.ifBlank { ChargingBadgeDefaultText }}",
                style = MaterialTheme.typography.labelSmall,
                color = ChargingBadgeColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun VideoPartListDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    title: String,
    pages: List<VideoPage>,
    lastPlayedCid: Long = 0,
    lastPlayedTime: Int = 0,
    onHideDialog: () -> Unit,
    onClick: (cid: Long) -> Unit
) {
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabCount by remember { mutableIntStateOf(ceil(pages.size / 20.0).toInt()) }
    val selectedVideoPart = remember { mutableStateListOf<VideoPage>() }

    val tabFocusRequester = remember { FocusRequester() }
    val tabRowFocusRequester = remember { FocusRequester() }
    val videoListFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyGridState()

    LaunchedEffect(selectedTabIndex) {
        val fromIndex = selectedTabIndex * 20
        var toIndex = (selectedTabIndex + 1) * 20
        if (toIndex >= pages.size) {
            toIndex = pages.size
        }
        selectedVideoPart.swapListWithMainContext(pages.subList(fromIndex, toIndex))
    }

    LaunchedEffect(show) {
        if (show && tabCount > 1) tabFocusRequester.requestFocusWithRetry()
        if (show && tabCount == 1) videoListFocusRequester.requestFocusWithRetry()
    }

    if (show) {
        TvAlertDialog(
            modifier = modifier,
            title = { Text(text = title) },
            onDismissRequest = { onHideDialog() },
            confirmButton = {},
            properties = DialogProperties(usePlatformDefaultWidth = false),
            text = {
                Column(
                    modifier = Modifier.size(660.dp, 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabRow(
                        modifier = Modifier
                            .onFocusChanged {
                                if (it.hasFocus) {
                                    scope.launch(Dispatchers.Main) {
                                        listState.scrollToItemIfAvailable(0)
                                    }
                                }
                            }
                            .focusRestorer()
                            .focusRequester(tabRowFocusRequester),
                        selectedTabIndex = selectedTabIndex,
                        separator = { Spacer(modifier = Modifier.width(12.dp)) },
                    ) {
                        for (i in 0 until tabCount) {
                            Tab(
                                modifier = if (i == 0) Modifier.focusRequester(
                                    tabFocusRequester
                                ) else Modifier,
                                selected = i == selectedTabIndex,
                                onFocus = { selectedTabIndex = i },
                            ) {
                                Text(
                                    text = "P${i * 20 + 1}-${(i + 1) * 20}",
                                    fontSize = 12.sp,
                                    color = LocalContentColor.current,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 6.dp
                                    )
                                )
                            }
                        }
                    }

                    LazyVerticalGrid(
                        modifier = Modifier
                            .onBackPressed {
                                if (tabCount > 1) tabRowFocusRequester.requestFocus() else onHideDialog()
                            },
                        state = listState,
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(
                            items = selectedVideoPart,
                            key = { index, video -> "$index:${video.cid}" }
                        ) { index, page ->
                            val buttonModifier =
                                if (index == 0) Modifier.focusRequester(videoListFocusRequester) else Modifier

                            VideoPartButton(
                                modifier = buttonModifier,
                                index = page.index,
                                title = page.title,
                                played = if (page.cid == lastPlayedCid) lastPlayedTime else 0,
                                isLastPlayed = page.cid == lastPlayedCid,
                                duration = page.duration,
                                onClick = { onClick(page.cid) }
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun VideoUgcListDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    title: String,
    episodes: List<Episode>,
    lastPlayedCid: Long = 0,
    lastPlayedTime: Int = 0,
    intentAid: Long = 0,
    sectionTitle: String,
    onHideDialog: () -> Unit,
    onClick: (avid: Long, cid: Long) -> Unit
) {
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabCount by remember { mutableIntStateOf(ceil(episodes.size / 20.0).toInt()) }
    val selectedVideoPart = remember { mutableStateListOf<Episode>() }

    val tabFocusRequester = remember { FocusRequester() }
    val tabRowFocusRequester = remember { FocusRequester() }
    val videoListFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyGridState()

    LaunchedEffect(selectedTabIndex) {
        val fromIndex = selectedTabIndex * 20
        var toIndex = (selectedTabIndex + 1) * 20
        if (toIndex >= episodes.size) {
            toIndex = episodes.size
        }
        selectedVideoPart.swapListWithMainContext(episodes.subList(fromIndex, toIndex))
    }

    LaunchedEffect(show) {
        if (show && tabCount > 1) tabFocusRequester.requestFocusWithRetry()
        if (show && tabCount == 1) videoListFocusRequester.requestFocusWithRetry()
    }

    if (show) {
        TvAlertDialog(
            modifier = modifier,
            title = { Text(text = title) },
            onDismissRequest = { onHideDialog() },
            confirmButton = {},
            properties = DialogProperties(usePlatformDefaultWidth = false),
            text = {
                Column(
                    modifier = Modifier.size(700.dp, 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabRow(
                        modifier = Modifier
                            .onFocusChanged {
                                if (it.hasFocus) {
                                    scope.launch(Dispatchers.Main) {
                                        listState.scrollToItemIfAvailable(0)
                                    }
                                }
                            }
                            .focusRestorer()
                            .focusRequester(tabRowFocusRequester),
                        selectedTabIndex = selectedTabIndex,
                        separator = { Spacer(modifier = Modifier.width(12.dp)) },
                    ) {
                        for (i in 0 until tabCount) {
                            Tab(
                                modifier = if (i == 0) Modifier.focusRequester(
                                    tabFocusRequester
                                ) else Modifier,
                                selected = i == selectedTabIndex,
                                onFocus = { selectedTabIndex = i },
                            ) {
                                Text(
                                    text = "EP${i * 20 + 1}-${(i + 1) * 20}",
                                    fontSize = 12.sp,
                                    color = LocalContentColor.current,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 6.dp
                                    )
                                )
                            }
                        }
                    }

                    LazyVerticalGrid(
                        modifier = Modifier
                            .onBackPressed {
                                if (tabCount > 1) tabRowFocusRequester.requestFocus() else onHideDialog()
                            },
                        state = listState,
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(
                            items = selectedVideoPart,
                            key = { index, video -> "$index:${video.cid}" }
                        ) { index, episode ->
                            val buttonModifier =
                                if (index == 0) Modifier.focusRequester(videoListFocusRequester) else Modifier
                            val absoluteIndex = selectedTabIndex * 20 + index + 1
                            val episodeTitle by remember { mutableStateOf(ugcEpisodeDisplayTitle(episode)) }
                            val isLastPlayed = remember(episode, lastPlayedCid) {
                                episode.cid == lastPlayedCid || episode.pages.any { it.cid == lastPlayedCid }
                            }
                            val playedSeconds = remember(episode, lastPlayedCid, lastPlayedTime) {
                                calculateUgcEpisodePlayedSeconds(
                                    episodeCid = episode.cid,
                                    pages = episode.pages,
                                    episodeDurationSeconds = episode.duration,
                                    lastPlayedCid = lastPlayedCid,
                                    currentPagePlayedSeconds = lastPlayedTime
                                )
                            }

                            UgcEpisodeButton(
                                modifier = buttonModifier.focusedScale(0.95f),
                                title = episodeTitle,
                                cover = episode.cover,
                                viewCount = episode.viewCount,
                                danmakuCount = episode.danmakuCount,
                                isInteractive = episode.isInteractive,
                                isChargingArc = episode.isChargingArc,
                                chargingArcBadge = episode.chargingArcBadge,
                                partCount = episode.pages.size,
                                played = playedSeconds,
                                isLastPlayed = isLastPlayed,
                                duration = episode.duration,
                                onClick = { onClick(episode.aid, episode.cid) }
                            )
                        }
                    }
                }
            }
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun VideoPartButtonShortTextPreview() {
    BVTheme {
        VideoPartButton(
            index = 2,
            title = "这是一段短文字",
            duration = 100,
            onClick = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun VideoPartButtonLongTextPreview() {
    BVTheme {
        VideoPartButton(
            index = 2,
            title = "这可能是我这辈子距离梅西最近的一次",
            played = 23333,
            duration = 100,
            onClick = {}
        )
    }
}


@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun VideoPartRowPreview() {
    val pages = remember { mutableStateListOf<VideoPage>() }
    for (i in 0..10) {
        pages.add(
            VideoPage(
                cid = 1000L + i,
                index = i,
                title = "这可能是我这辈子距离梅西最近的一次",
                duration = 10,
                dimension = Dimension(0, 0)
            )
        )
    }
    BVTheme {
        VideoPartRow(pages = pages, onClick = {})
    }
}
