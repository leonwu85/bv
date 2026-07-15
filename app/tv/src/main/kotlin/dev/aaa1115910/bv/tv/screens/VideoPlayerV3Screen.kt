package dev.aaa1115910.bv.tv.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.tv.activities.video.SeasonInfoActivity
import dev.aaa1115910.bv.tv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.player.autoplay.AutoPlayCandidate
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerDanmakuMasksData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerHistoryData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerLoadStateData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerLogsData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerPaymentData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerSeekThumbData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerVideoInfoData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerVideoShotData
import dev.aaa1115910.bv.player.entity.PortraitVideoFixMode
import dev.aaa1115910.bv.player.entity.PlayerBottomControlPanelButtonIds
import dev.aaa1115910.bv.player.entity.PlayerLoadNextAction
import dev.aaa1115910.bv.player.entity.PlayerLongPressAction
import dev.aaa1115910.bv.player.entity.PlaybackMediaMode
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.SponsorBlockSkipMode
import dev.aaa1115910.bv.player.entity.VideoListInteractiveNode
import dev.aaa1115910.bv.player.entity.VideoListItemData
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.player.entity.VideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.VideoPlayerDanmakuMasksData
import dev.aaa1115910.bv.player.entity.VideoPlayerHistoryData
import dev.aaa1115910.bv.player.entity.VideoPlayerLoadStateData
import dev.aaa1115910.bv.player.entity.VideoPlayerLogsData
import dev.aaa1115910.bv.player.entity.VideoPlayerPaymentData
import dev.aaa1115910.bv.player.entity.VideoPlayerSeekThumbData
import dev.aaa1115910.bv.player.entity.VideoPlayerVideoInfoData
import dev.aaa1115910.bv.player.entity.VideoPlayerVideoShotData
import dev.aaa1115910.bv.player.tv.BvPlayer
import dev.aaa1115910.bv.player.tv.controller.LiveViewerCountTip
import dev.aaa1115910.bv.player.tv.controller.OnlineViewerCountTip
import dev.aaa1115910.bv.player.tv.controller.SkipTip
import dev.aaa1115910.bv.player.tv.controller.TripleLikeTip
import dev.aaa1115910.bv.tv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.tv.component.GeetestTvVerifyDialog
import dev.aaa1115910.bv.tv.component.InteractiveOptionDialog
import dev.aaa1115910.bv.tv.component.buttons.CoinButton
import dev.aaa1115910.bv.tv.component.CommentPanel
import dev.aaa1115910.bv.tv.component.buttons.FavoriteButton
import dev.aaa1115910.bv.tv.component.buttons.LikeButton
import dev.aaa1115910.bv.tv.manager.FollowStateManager
import dev.aaa1115910.bv.tv.manager.PlayedAidsCache
import dev.aaa1115910.bv.tv.manager.VideoUserActionManager
import dev.aaa1115910.bv.tv.manager.VideoUserActionManager.getStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.aaa1115910.bv.tv.component.videocard.VideosRow
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.swapList
import dev.aaa1115910.bv.viewmodel.VideoPlayerV3ViewModel
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.repositories.LiveRepository
import dev.aaa1115910.biliapi.repositories.UserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin

private const val COMMENT_SPLIT_PLAYER_RESIZE_ANIMATION_MS = 260

@Composable
fun VideoPlayerV3Screen(
    modifier: Modifier = Modifier,
    playerViewModel: VideoPlayerV3ViewModel = koinViewModel(),
    userRepository: UserRepository = getKoin().get(),
    liveRepository: LiveRepository = getKoin().get(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger { }

    // subscribe shared action state by aid
    val currentAid = playerViewModel.currentAid
    val sharedActionFlow = remember(currentAid) { getStateFlow(currentAid, Prefs.uid) }
    val sharedActionState by sharedActionFlow.collectAsState()
    val followStateMap by FollowStateManager.followStateMap.collectAsState()

    LaunchedEffect(followStateMap, playerViewModel.upId) {
        val currentUpId = playerViewModel.upId
        if (currentUpId > 0) {
            val cachedState = FollowStateManager.getFollowState(currentUpId)
            if (cachedState != null) {
                if (playerViewModel.isFollowingUp != cachedState) {
                    playerViewModel.isFollowingUp = cachedState
                }
            } else {
                // 缓存中无记录（直播等场景），从 API 查询
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        userRepository.checkIsFollowing(currentUpId, Prefs.apiType)
                    }.getOrNull()
                }
                if (result != null) {
                    FollowStateManager.updateFollowState(currentUpId, result)
                    playerViewModel.isFollowingUp = result
                }
            }
        }
    }

    // Automatic-exit countdown state
    var autoActionCountdownJob by remember { mutableStateOf<Job?>(null) }
    var autoActionTipVisible by remember { mutableStateOf(false) }
    var autoActionTipText by remember { mutableStateOf("") }

    // 评论面板状态
    var showCommentPanel by remember { mutableStateOf(false) }

    // 一键三连 Tip 状态
    var showTripleLikeTip by remember { mutableStateOf(false) }
    var tripleLikeTipMessage by remember { mutableStateOf("") }

    // 简介弹窗状态
    var showDescriptionDialog by remember { mutableStateOf(false) }
    var videoDescription by remember(currentAid) { mutableStateOf("") }
    var descriptionLoaded by remember(currentAid) { mutableStateOf(false) }

    var selectedAutoPlayRelatedVideo by remember(playerViewModel.currentCid) {
        mutableStateOf<VideoCardData?>(null)
    }

    // 在线观看人数状态
    var onlineViewerCount by remember { mutableStateOf("") }
    var showOnlineViewerCountTip by remember { mutableStateOf(false) }
    var showLiveControlPanel by remember { mutableStateOf(false) }

    // 焦点管理
    val relatedVideosFocusRequester = remember { FocusRequester() }

    // 当显示相关视频时，自动将焦点转移到VideosRow的第一个卡片
    LaunchedEffect(playerViewModel.showRelatedVideos) {
        if (playerViewModel.showRelatedVideos) {
            delay(300)
            kotlin.runCatching {
                relatedVideosFocusRequester.requestFocus()
            }
        }
    }

    LaunchedEffect(playerViewModel.isLive) {
        if (!playerViewModel.isLive) {
            showLiveControlPanel = false
        }
    }

    // 获取在线观看人数
    LaunchedEffect(playerViewModel.currentCid, playerViewModel.currentAid) {
        if (playerViewModel.currentCid > 0 && playerViewModel.currentAid > 0 && Prefs.showOnlineViewerCount > 0) {
            withContext(Dispatchers.IO) {
                try {
                    val response = BiliHttpApi.getVideoOnlineTotal(
                        cid = playerViewModel.currentCid,
                        aid = playerViewModel.currentAid
                    )
                    if (response.code == 0) {
                        onlineViewerCount = response.data?.total ?: ""
                        showOnlineViewerCountTip = true

                        // 如果设置为 30 秒后隐藏，则自动隐藏
                        if (Prefs.showOnlineViewerCount == 1) {
                            delay(30_000)
                            showOnlineViewerCountTip = false
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to get online viewer count" }
                }
            }
        }
    }

    // 如果设置为始终显示，每 5 分钟刷新一次数据
    LaunchedEffect(showOnlineViewerCountTip, Prefs.showOnlineViewerCount) {
        if (showOnlineViewerCountTip && Prefs.showOnlineViewerCount == 2) {
            while (true) {
                delay(300_000)  // 5 分钟
                if (playerViewModel.currentCid > 0 && playerViewModel.currentAid > 0) {
                    withContext(Dispatchers.IO) {
                        try {
                            val response = BiliHttpApi.getVideoOnlineTotal(
                                cid = playerViewModel.currentCid,
                                aid = playerViewModel.currentAid
                            )
                            if (response.code == 0) {
                                onlineViewerCount = response.data?.total ?: ""
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            logger.warn(e) { "Failed to refresh online viewer count" }
                        }
                    }
                }
            }
        }
    }

    val exitPlayer = {
        playerViewModel.saveSubtitleSmartDisplayPreferenceIfNeeded()
        playerViewModel.dismissInteractiveOptionDialog()
        Prefs.currentPlaySpeed = Prefs.defaultPlaySpeed
        PlayedAidsCache.clear()
        (context as Activity).finish()
    }

    fun findNextEpisode(): VideoListItemData? {
        val currentIndex = playerViewModel.availableVideoList.indexOfFirst {
            when (it) {
                is VideoListItemData -> it.cid == playerViewModel.currentCid
                else -> false
            }
        }
        if (currentIndex < 0 || currentIndex + 1 >= playerViewModel.availableVideoList.size) return null

        return playerViewModel.availableVideoList
            .drop(currentIndex + 1)
            .firstOrNull { it is VideoListItemData } as? VideoListItemData
    }

    fun findNextRelatedVideo(): VideoCardData? {
        val candidates = playerViewModel.relatedVideos
            .filter { related ->
                !related.isChargingArc &&
                    related.avid != playerViewModel.currentAid &&
                    !PlayedAidsCache.hasPlayed(related.avid)
            }
            .take(10)

        val selectedCandidate = selectedAutoPlayRelatedVideo?.takeIf { it in candidates }
        if (selectedCandidate != null) return selectedCandidate

        val nextCandidate = candidates.randomOrNull()
        if (selectedAutoPlayRelatedVideo != nextCandidate) {
            selectedAutoPlayRelatedVideo = nextCandidate
        }
        return nextCandidate
    }

    fun resolveNextAutoPlayVideo(immediate: Boolean): Any? {
        val nextEp = findNextEpisode()
        if (immediate) return nextEp

        return when (Prefs.playerLoadNextAction) {
            PlayerLoadNextAction.PlayRecommend -> findNextRelatedVideo()
            PlayerLoadNextAction.PlayNextPart -> nextEp
            PlayerLoadNextAction.PlayNextPartOrRecommend -> nextEp ?: findNextRelatedVideo()
            PlayerLoadNextAction.DoNothing -> null
        }
    }

    fun buildAutoPlayCandidate(nextVideo: Any?): AutoPlayCandidate? {
        return when (nextVideo) {
            is VideoListItemData -> {
                when {
                    playerViewModel.fromSeason || playerViewModel.currentAid == nextVideo.aid ->
                        AutoPlayCandidate.PlaylistItem(nextVideo)
                    nextVideo.seasonId == null -> AutoPlayCandidate.CrossVideoPart(nextVideo)
                    else -> null
                }
            }

            is VideoCardData -> {
                if (!nextVideo.jumpToSeason) {
                    AutoPlayCandidate.RelatedVideo(nextVideo)
                } else {
                    null
                }
            }

            else -> null
        }
    }

    // 处理back键，当推荐视频有焦点时隐藏推荐视频并将焦点返回到播放器
    BackHandler(enabled = playerViewModel.showRelatedVideos) {
        playerViewModel.showRelatedVideos = false
    }

    BackHandler(enabled = playerViewModel.showInteractiveOptionDialog) {
        exitPlayer()
    }

    val commentSplitScreenEnabled = Prefs.playerCommentSplitScreen && playerViewModel.currentAid > 0
    val useCommentSplitScreen = commentSplitScreenEnabled && showCommentPanel
    var splitPlayerSnapped by remember { mutableStateOf(false) }
    var splitCommentPanelVisible by remember { mutableStateOf(false) }

    LaunchedEffect(useCommentSplitScreen) {
        if (useCommentSplitScreen) {
            splitPlayerSnapped = false
            splitCommentPanelVisible = false
            delay(COMMENT_SPLIT_PLAYER_RESIZE_ANIMATION_MS.toLong())
            splitPlayerSnapped = true
            splitCommentPanelVisible = true
        } else {
            splitPlayerSnapped = false
            splitCommentPanelVisible = false
        }
    }

    LaunchedEffect(
        playerViewModel.currentCid,
        playerViewModel.isInteractivePlayback,
        playerViewModel.showRelatedVideos,
        playerViewModel.availableVideoList.size,
        playerViewModel.relatedVideos.size,
        Prefs.playerLoadNextAction,
    ) {
        if (playerViewModel.isInteractivePlayback || playerViewModel.showRelatedVideos) {
            playerViewModel.prepareAutoPlayTarget(null)
            return@LaunchedEffect
        }

        val directCandidate = buildAutoPlayCandidate(resolveNextAutoPlayVideo(immediate = false))
        playerViewModel.prepareAutoPlayTarget(directCandidate)
    }

    CompositionLocalProvider(
        LocalVideoPlayerSeekThumbData provides VideoPlayerSeekThumbData(
            idleIcon = playerViewModel.playerIconIdle,
            movingIcon = playerViewModel.playerIconMoving
        ),
        LocalVideoPlayerVideoInfoData provides VideoPlayerVideoInfoData(
            width = playerViewModel.currentVideoWidth,
            height = playerViewModel.currentVideoHeight,
            codec = playerViewModel.currentVideoCodec.name,
            title = playerViewModel.title,
            partTitle = playerViewModel.partTitle,
            play = playerViewModel.play,
            danmaku = playerViewModel.danmaku,
            like = playerViewModel.like,
            coin = playerViewModel.coin,
            favorite = playerViewModel.favorite,
            upName = playerViewModel.upName,
            upAvatar = playerViewModel.upFace,
            pubTime = playerViewModel.pubTime,
            fromSeason = playerViewModel.fromSeason,
            isFollowingUp = playerViewModel.isFollowingUp,
            isVerticalVideo = playerViewModel.isVerticalVideo
        ),
        LocalVideoPlayerLogsData provides VideoPlayerLogsData(
            logs = playerViewModel.logs
        ),
        LocalVideoPlayerHistoryData provides VideoPlayerHistoryData(
            lastPlayed = playerViewModel.lastPlayed,
        ),
        LocalVideoPlayerPaymentData provides VideoPlayerPaymentData(
            needPay = playerViewModel.needPay,
            epid = playerViewModel.epid,
        ),
        LocalVideoPlayerLoadStateData provides VideoPlayerLoadStateData(
            loadState = playerViewModel.loadState,
            errorMessage = playerViewModel.errorMessage,
        ),
        LocalVideoPlayerConfigData provides VideoPlayerConfigData(
            availableResolutions = playerViewModel.availableQuality,
            availableVideoCodec = playerViewModel.availableVideoCodec,
            availableAudio = playerViewModel.availableAudio,
            availableSubtitleTracks = playerViewModel.availableSubtitle,
            availableVideoList = playerViewModel.availableVideoList,
            currentVideoCid = playerViewModel.currentCid,
            currentResolution = playerViewModel.currentQuality,
            currentVideoCodec = playerViewModel.currentVideoCodec,
            currentVideoAspectRatio = playerViewModel.currentVideoAspectRatio,
            currentVideoRotation = playerViewModel.currentVideoRotation,
            supportManualVideoRotation = Prefs.supportManualVideoRotation,
            currentVideoSpeed = playerViewModel.currentPlaySpeed,
            currentPlaybackMediaMode = playerViewModel.currentPlaybackMediaMode,
            currentAudio = playerViewModel.currentAudio,
            currentDanmakuEnabled = playerViewModel.currentDanmakuEnabled,
            currentDanmakuEnabledList = playerViewModel.currentDanmakuTypes,
            currentDanmakuScale = playerViewModel.currentDanmakuScale,
            currentDanmakuOpacity = playerViewModel.currentDanmakuOpacity,
            currentDanmakuArea = playerViewModel.currentDanmakuArea,
            currentDanmakuMask = playerViewModel.currentDanmakuMask,
            currentDanmakuFilterLevel = playerViewModel.currentDanmakuFilterLevel,
            currentDanmakuMergeEnabled = playerViewModel.currentDanmakuMergeEnabled,
            currentDanmakuSpeedMode = playerViewModel.currentDanmakuSpeedMode,
            currentDanmakuPresentationSpeed = playerViewModel.currentDanmakuPresentationSpeed,
            currentLiveDanmakuFilterLevel = playerViewModel.currentLiveDanmakuFilterLevel,
            currentSubtitleId = playerViewModel.currentSubtitleId,
            currentSubtitleData = playerViewModel.currentSubtitleData,
            currentSubtitleFontSize = playerViewModel.currentSubtitleFontSize,
            currentSubtitleBackgroundOpacity = playerViewModel.currentSubtitleBackgroundOpacity,
            currentSubtitleBottomPadding = playerViewModel.currentSubtitleBottomPadding,
            currentSecondarySubtitleId = playerViewModel.currentSecondarySubtitleId,
            currentSecondarySubtitleData = playerViewModel.currentSecondarySubtitleData,
            currentSecondarySubtitleFontSize = playerViewModel.currentSecondarySubtitleFontSize,
            currentSecondarySubtitleBackgroundOpacity = playerViewModel.currentSecondarySubtitleBackgroundOpacity,
            currentSecondarySubtitleBottomPadding = playerViewModel.currentSecondarySubtitleBottomPadding,
            currentPlayMode = playerViewModel.currentPlayMode,
            incognitoMode = Prefs.incognitoMode,
            isLoop = playerViewModel.isLoop,
            showDanmaku = playerViewModel.showDanmaku,
            showRelatedVideos = playerViewModel.showRelatedVideos,
            showNextVideoBtn = Prefs.playerLoadNextAction != PlayerLoadNextAction.DoNothing,
            defaultStartPosition = Prefs.playerDefaultStartPosition.toPlayerType(),
            enableStartPositionSwitch = Prefs.playerEnableStartPositionSwitch,
            debugDanmakuMaskDownsample180p = Prefs.debugDanmakuMaskDownsample180p,
            clipInfoList = playerViewModel.clipInfoList,
            skipPgcIntroOutro = Prefs.skipPgcIntroOutro,
            isLive = playerViewModel.isLive,
            availableLiveQualities = playerViewModel.availableLiveQualities.toList(),
            currentLiveQn = playerViewModel.currentLiveQn,
            currentLiveQualityDescription = playerViewModel.currentLiveQualityDescription,
            currentLiveCodec = playerViewModel.currentLiveCodec,
            availableLiveLines = playerViewModel.availableLiveLines.toList(),
            currentLiveLineIndex = playerViewModel.currentLiveLineIndex,
            bottomControlPanelConfig = Prefs.playerBottomControlPanelConfig
        ),
        LocalVideoPlayerDanmakuMasksData provides VideoPlayerDanmakuMasksData(
            danmakuMasks = playerViewModel.danmakuMasks,
        ),
        LocalVideoPlayerVideoShotData provides VideoPlayerVideoShotData(
            videoShot = playerViewModel.videoShot,
        ),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyUp && autoActionCountdownJob != null) {
                        // Any key can cancel automatic exit.
                        logger.debug { "Key ${keyEvent.key}: cancel automatic exit" }
                        autoActionCountdownJob?.cancel()
                        autoActionCountdownJob = null
                        autoActionTipVisible = false
                        return@onPreviewKeyEvent keyEvent.key == Key.Back
                    }
                    false
                }
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val splitCommentPanelWidth = maxWidth * 0.3f
                val splitMaskWidth by animateDpAsState(
                    targetValue = if (useCommentSplitScreen) splitCommentPanelWidth else 0.dp,
                    animationSpec = tween(durationMillis = COMMENT_SPLIT_PLAYER_RESIZE_ANIMATION_MS),
                    label = "CommentSplitMaskWidth"
                )
                Box(
                    modifier = Modifier
                        .width(
                            if (useCommentSplitScreen && splitPlayerSnapped) {
                                maxWidth - splitCommentPanelWidth
                            } else {
                                maxWidth
                            }
                        )
                        .fillMaxHeight()
                        .align(Alignment.CenterStart)
                ) {
                    BvPlayer(
                        modifier = Modifier.fillMaxSize(),
                        videoPlayer = playerViewModel.videoPlayer!!,
                        danmakuPlayer = playerViewModel.danmakuPlayer,
                        danmakuOpacity = playerViewModel.currentDanmakuOpacity,
                        playerSeekForwardStep = Prefs.playerSeekForwardStep,
                        playerSeekBackwardStep = Prefs.playerSeekBackwardStep,
                        showBottomProgressBar = Prefs.playerShowBottomProgressBar,
                        bottomProgressBarColor = Prefs.playerBottomProgressBarColor.toComposeColor(),
                        bottomControlPanelConfig = Prefs.playerBottomControlPanelConfig,
                        useTextureViewFixPortraitVideo =
                            Prefs.portraitVideoFixMode == PortraitVideoFixMode.UseTextureView &&
                                playerViewModel.isVerticalVideo &&
                                playerViewModel.currentQuality >= Resolution.R4K &&
                                playerViewModel.currentQuality !in setOf(
                                    Resolution.RHdr,
                                    Resolution.RDolby,
                                ),
                        showRelatedButton = !playerViewModel.fromSeason &&
                            playerViewModel.seasonId == 0 &&
                            playerViewModel.epid == 0,
                onToggleRelatedVideos = { state ->
                    playerViewModel.showRelatedVideos = if (playerViewModel.relatedVideos.isNotEmpty()) state else false
                },
                autoOpenPlayListOnVideoEnd = false,
                onSendHeartbeat = playerViewModel::uploadHistory,
                onClearBackToHistoryData = { playerViewModel.lastPlayed = 0 },
                onReloadDanmakuAfterSeek = playerViewModel::reloadDanmakuAfterSeek,
                onDanmakuPlayerBound = playerViewModel::resyncDanmakuAfterPlayerBound,
                onEnsureDanmakuCoverage = playerViewModel::ensureDanmakuCoverage,
                onNearEnd = {
                    if (playerViewModel.showRelatedVideos || playerViewModel.isInteractivePlayback) return@BvPlayer
                    val directCandidate = buildAutoPlayCandidate(resolveNextAutoPlayVideo(immediate = false))
                    playerViewModel.prefetchPreparedAutoPlayTarget(directCandidate)
                },
                onLoadPrevVideo = {
                    val currentIndex = playerViewModel.availableVideoList.indexOfFirst {
                        when (it) {
                            is VideoListItemData -> it.cid == playerViewModel.currentCid
                            else -> false
                        }
                    }
                    val prevEp =
                        if (currentIndex > 0) {
                            playerViewModel.availableVideoList
                                .take(currentIndex)
                                .lastOrNull { it is VideoListItemData } as? VideoListItemData
                        } else null

                    if (prevEp != null) {
                        playerViewModel.saveSubtitleSmartDisplayPreferenceIfNeeded()
                        playerViewModel.title = prevEp.title
                        playerViewModel.partTitle = prevEp.partTitle
                        if (prevEp.seasonId == null && playerViewModel.currentAid != prevEp.aid) {
                            VideoInfoActivity.actionStart(
                                context = context,
                                aid = prevEp.aid,
                                cid = prevEp.cid,
                                fromPlayer = true,
                                audioOnlyMode = playerViewModel.currentPlaybackMediaMode == PlaybackMediaMode.AudioOnly
                            )
                        } else {
                            playerViewModel.loadPlayUrl(
                                avid = prevEp.aid,
                                cid = prevEp.cid!!,
                                epid = prevEp.epid,
                                seasonId = prevEp.seasonId,
                                continuePlayNext = true
                            )
                        }
                    }
                },
                onLoadNextVideo = { immediate ->
                    if (playerViewModel.showRelatedVideos) {
                        logger.info { "Related videos is shown, skip auto action" }
                        return@BvPlayer
                    }

                    if (playerViewModel.isInteractivePlayback) {
                        playerViewModel.requestInteractiveOptionDialog()
                        return@BvPlayer
                    }

                    playerViewModel.saveSubtitleSmartDisplayPreferenceIfNeeded()
                    // 标记当前稿件已播放
                    PlayedAidsCache.markPlayed(playerViewModel.currentAid)
                    val nextVideo = resolveNextAutoPlayVideo(immediate)
                    if (nextVideo != null) {
                        scope.launch {
                            try {
                                when (nextVideo) {
                                    is VideoListItemData -> {
                                        PlayedAidsCache.markPlayed(nextVideo.aid)
                                        val directCandidate = buildAutoPlayCandidate(nextVideo)
                                        if (directCandidate != null && playerViewModel.consumePreparedAutoPlayTarget(directCandidate)) {
                                            return@launch
                                        }
                                        playerViewModel.title = nextVideo.title
                                        playerViewModel.partTitle = nextVideo.partTitle
                                        if (nextVideo.seasonId == null && playerViewModel.currentAid != nextVideo.aid) {
                                            VideoInfoActivity.actionStart(
                                                context = context,
                                                aid = nextVideo.aid,
                                                cid = nextVideo.cid,
                                                fromPlayer = true,
                                                audioOnlyMode = playerViewModel.currentPlaybackMediaMode == PlaybackMediaMode.AudioOnly
                                            )
                                        } else {
                                            playerViewModel.loadPlayUrl(
                                                avid = nextVideo.aid,
                                                cid = nextVideo.cid!!,
                                                epid = nextVideo.epid,
                                                seasonId = nextVideo.seasonId,
                                                continuePlayNext = true,
                                                forceStartPlayback = true
                                            )
                                        }
                                    }

                                    is VideoCardData -> {
                                        // 推荐视频卡片：跳转到视频详情（再进入播放器）
                                        PlayedAidsCache.markPlayed(nextVideo.avid)
                                        val directCandidate = buildAutoPlayCandidate(nextVideo)
                                        if (directCandidate != null && playerViewModel.consumePreparedAutoPlayTarget(directCandidate)) {
                                            return@launch
                                        }
                                        if (nextVideo.jumpToSeason) {
                                            SeasonInfoActivity.actionStart(
                                                context = context,
                                                epId = nextVideo.epId!!,
                                                proxyArea = ProxyArea.checkProxyArea(nextVideo.title)
                                            )
                                        } else {
                                            VideoInfoActivity.actionStart(
                                                context = context,
                                                aid = nextVideo.avid,
                                                fromPlayer = true,
                                                audioOnlyMode = playerViewModel.currentPlaybackMediaMode == PlaybackMediaMode.AudioOnly
                                            )
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                logger.warn(e) { "Start next video failed" }
                            }
                        }
                    } else if (Prefs.playerExitWhenAllIsPlayed) {
                        // 没有下一个：退出
                        autoActionCountdownJob = scope.launch {
                            try {
                                autoActionTipText = "播放结束，即将退出"
                                autoActionTipVisible = true
                                delay(1600)
                                autoActionTipVisible = false
                                if (autoActionCountdownJob != null) {
                                    autoActionCountdownJob = null
                                    Prefs.currentPlaySpeed = Prefs.defaultPlaySpeed
                                    playerViewModel.saveSubtitleSmartDisplayPreferenceIfNeeded()
                                    // 自动退出时也清空缓存
                                    PlayedAidsCache.clear()
                                    (context as Activity).finish()
                                }
                            } catch (_: Exception) {
                                autoActionTipVisible = false
                                autoActionCountdownJob = null
                            }
                        }
                    }
                    // 什么都不做
                },
                onExit = {
                    playerViewModel.saveSubtitleSmartDisplayPreferenceIfNeeded()
                    Prefs.currentPlaySpeed = Prefs.defaultPlaySpeed
                    // 退出时清空播放缓存
                    PlayedAidsCache.clear()
                    (context as Activity).finish()
                },
                onLoadNewVideo = { videoListItem ->
                    when (videoListItem) {
                        is VideoListItemData -> {
                            playerViewModel.saveSubtitleSmartDisplayPreferenceIfNeeded()
                            // 手动选择新视频时也标记播放
                            PlayedAidsCache.markPlayed(videoListItem.aid)
                            if (videoListItem is VideoListInteractiveNode) {
                                playerViewModel.playInteractiveOption(videoListItem)
                            } else if (videoListItem.seasonId == null && playerViewModel.currentAid != videoListItem.aid) {
                                playerViewModel.title = videoListItem.title
                                playerViewModel.partTitle = videoListItem.partTitle
                                VideoInfoActivity.actionStart(
                                    context = context,
                                    aid = videoListItem.aid,
                                    cid = videoListItem.cid,
                                    fromPlayer = true
                                )
                            } else {
                                playerViewModel.title = videoListItem.title
                                playerViewModel.partTitle = videoListItem.partTitle
                                playerViewModel.loadPlayUrl(
                                    avid = videoListItem.aid,
                                    cid = videoListItem.cid!!,
                                    epid = videoListItem.epid,
                                    seasonId = videoListItem.seasonId,
                                    continuePlayNext = true
                                )
                            }
                        }
                    }
                },
                onRefreshVideo = {
                    if (playerViewModel.isLive) {
                        // 直播模式：重新获取直播流 URL
                        logger.info { "Reload live stream for room ${playerViewModel.liveRoomId}" }
                        playerViewModel.loadLiveStreamWithQuality(
                            playerViewModel.liveRoomId,
                            playerViewModel.currentLiveQn
                        )
                    } else {
                        val time = playerViewModel.videoPlayer?.currentPosition ?: 0
                        logger.info { "Reload video and back to time: ${time.formatHourMinSec()}" }
                        scope.launch {
                            playerViewModel.playQuality()
                            delay(300)
                            playerViewModel.videoPlayer?.seekTo(time)
                            playerViewModel.danmakuPlayer?.pause()
                            playerViewModel.videoPlayer?.start()
                        }
                    }
                },
                onLiveRetry = {
                    playerViewModel.retryLiveStream()
                },
                commentPanelVisible = showCommentPanel,
                hideControllerOnCommentPanelOpen = commentSplitScreenEnabled,
                onShowComment = {
                    showCommentPanel = playerViewModel.currentAid > 0 && !showCommentPanel
                },
                onShowDescription = {
                    if (!descriptionLoaded) {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val response = BiliHttpApi.getVideoDetail(
                                    av = playerViewModel.currentAid,
                                    sessData = Prefs.accessToken
                                )
                                val desc = response.getResponseData().view.desc
                                withContext(Dispatchers.Main) {
                                    videoDescription = desc
                                    descriptionLoaded = true
                                    showDescriptionDialog = true
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    videoDescription = "加载失败"
                                    showDescriptionDialog = true
                                }
                            }
                        }
                    } else {
                        showDescriptionDialog = true
                    }
                },
                onResolutionChange = { resolutionCode, afterChange ->
                    scope.launch(Dispatchers.Default) {
                        playerViewModel.playQuality(resolutionCode)
                        afterChange()
                        playerViewModel.currentQuality = resolutionCode
                    }
                },
                onCodecChange = { videoCodec, afterChange ->
                    playerViewModel.currentVideoCodec = videoCodec
                    scope.launch(Dispatchers.Default) {
                        playerViewModel.playQuality(
                            playerViewModel.currentQuality,
                            playerViewModel.currentVideoCodec
                        )
                        afterChange()
                    }
                },
                onAspectRatioChange = { aspectRatio ->
                    playerViewModel.currentVideoAspectRatio = aspectRatio
                },
                onRotationChange = { rotation ->
                    playerViewModel.currentVideoRotation = rotation
                },
                onPlaySpeedChange = { speed ->
                    Prefs.currentPlaySpeed = speed
                    playerViewModel.currentPlaySpeed = speed
                },
                onAudioChange = { audio, afterChange ->
                    playerViewModel.currentAudio = audio
                    scope.launch(Dispatchers.Default) {
                        playerViewModel.playQuality(audio = audio)
                        afterChange()
                    }
                },
                onPlaybackMediaModeChange = { mediaMode, currentPosition, afterChange ->
                    playerViewModel.currentPlaybackMediaMode = mediaMode
                    scope.launch(Dispatchers.Default) {
                        playerViewModel.playQuality(
                            mediaMode = mediaMode,
                            initialSeekPositionMs = currentPosition
                        )
                        afterChange()
                    }
                },
                onLiveQualityChange = { qn ->
                    playerViewModel.changeLiveQuality(qn)
                },
                onLiveCodecChange = { codec ->
                    println("VideoPlayerV3Screen: onLiveCodecChange called with codec=$codec")
                    playerViewModel.changeLiveCodec(codec)
                },
                onLiveLineChange = { lineIndex ->
                    playerViewModel.changeLiveLine(lineIndex)
                },
                onDanmakuSwitchChange = { enabledDanmakuTypes ->
                    Prefs.defaultDanmakuTypes = enabledDanmakuTypes
                    playerViewModel.currentDanmakuTypes.swapList(enabledDanmakuTypes)
                },
                onDanmakuSizeChange = { scale ->
                    Prefs.defaultTvDanmakuScale = scale
                    playerViewModel.currentDanmakuScale = scale
                },
                onDanmakuOpacityChange = { opacity ->
                    Prefs.defaultDanmakuOpacity = opacity
                    playerViewModel.currentDanmakuOpacity = opacity
                },
                onDanmakuAreaChange = { area ->
                    Prefs.defaultDanmakuArea = area
                    playerViewModel.currentDanmakuArea = area
                },
                onDanmakuSpeedModeChange = { mode ->
                    Prefs.defaultDanmakuSpeedMode = mode
                    playerViewModel.currentDanmakuSpeedMode = mode
                },
                onDanmakuPresentationSpeedChange = { speed ->
                    Prefs.defaultDanmakuPresentationSpeed = speed
                    playerViewModel.currentDanmakuPresentationSpeed = speed
                },
                onDanmakuMaskChange = { mask ->
                    Prefs.defaultDanmakuMask = mask
                    playerViewModel.currentDanmakuMask = mask
                },
                onDanmakuMergeChange = { enabled ->
                    Prefs.defaultDanmakuMergeEnabled = enabled
                    playerViewModel.updateDanmakuMergeEnabled(enabled)
                },
                onDanmakuFilterLevelChange = { filterLevel ->
                    if (playerViewModel.isLive) {
                        Prefs.defaultLiveDanmakuFilterLevel = filterLevel
                        playerViewModel.currentLiveDanmakuFilterLevel = filterLevel
                    } else {
                        Prefs.defaultDanmakuFilterLevel = filterLevel
                        playerViewModel.currentDanmakuFilterLevel = filterLevel
                    }
                },
                onSubtitleChange = { subtitle ->
                    playerViewModel.loadSubtitle(subtitle.id)
                },
                onSubtitleSizeChange = { size ->
                    Prefs.defaultSubtitleFontSize = size
                    playerViewModel.currentSubtitleFontSize = size
                },
                onSubtitleBackgroundOpacityChange = { opacity ->
                    Prefs.defaultSubtitleBackgroundOpacity = opacity
                    playerViewModel.currentSubtitleBackgroundOpacity = opacity
                },
                onSubtitleBottomPadding = { padding ->
                    Prefs.defaultSubtitleBottomPadding = padding
                    playerViewModel.currentSubtitleBottomPadding = padding
                },
                onSecondarySubtitleChange = { subtitle ->
                    playerViewModel.loadSecondarySubtitle(subtitle.id)
                },
                onSecondarySubtitleSizeChange = { size ->
                    Prefs.defaultSecondarySubtitleFontSize = size
                    playerViewModel.currentSecondarySubtitleFontSize = size
                },
                onSecondarySubtitleBackgroundOpacityChange = { opacity ->
                    Prefs.defaultSecondarySubtitleBackgroundOpacity = opacity
                    playerViewModel.currentSecondarySubtitleBackgroundOpacity = opacity
                },
                onSecondarySubtitleBottomPadding = { padding ->
                    Prefs.defaultSecondarySubtitleBottomPadding = padding
                    playerViewModel.currentSecondarySubtitleBottomPadding = padding
                },
                onPlayModeChange = { playMode ->
                    Prefs.defaultPlayMode = playMode
                    playerViewModel.currentPlayMode = playMode
                },
                onTripleLike = {
                    scope.launch {
                        val success = VideoUserActionManager.tripleLike(playerViewModel.currentAid)
                        tripleLikeTipMessage = if (success) "一键三连成功" else "一键三连失败"
                        showTripleLikeTip = true
                        delay(2000)
                        showTripleLikeTip = false
                    }
                },
                shortcutKeyBindings = Prefs.playerShortcutKeyBindings,
                useTripleLikeOnLongPress = Prefs.playerLongPressAction == PlayerLongPressAction.TripleLike,
                onToggleFollow = {
                    val upId = playerViewModel.upId
                    if (upId > 0) {
                        scope.launch(Dispatchers.IO) {
                            val isCurrentlyFollowing = playerViewModel.isFollowingUp
                            runCatching {
                                if (isCurrentlyFollowing) {
                                    userRepository.unfollowUser(
                                        mid = upId,
                                        preferApiType = Prefs.apiType
                                    )
                                } else {
                                    userRepository.followUser(
                                        mid = upId,
                                        preferApiType = Prefs.apiType
                                    )
                                }
                            }.onSuccess { success ->
                                if (success) {
                                    val newState = !isCurrentlyFollowing
                                    FollowStateManager.updateFollowState(upId, newState)
                                    withContext(Dispatchers.Main) {
                                        playerViewModel.isFollowingUp = newState
                                    }
                                }
                            }
                        }
                    }
                },
                liveIncognitoMode = Prefs.liveIncognitoMode,
                onReportLiveHistory = {
                    val roomId = playerViewModel.liveRoomId
                    if (roomId > 0) {
                        scope.launch(Dispatchers.IO) {
                            runCatching {
                                liveRepository.reportRoomEntryAction(roomId)
                            }.onSuccess {
                                withContext(Dispatchers.Main) {
                                    "已上报历史".toast(context)
                                }
                            }.onFailure {
                                withContext(Dispatchers.Main) {
                                    "上报历史失败".toast(context)
                                }
                            }
                        }
                    }
                },
                isLive = playerViewModel.isLive,
                onLiveDanmakuPlayerReady = { player ->
                    playerViewModel.setLivePlayer(player)
                },
                onOpenUpSpace = {
                    UpInfoActivity.actionStart(
                        context,
                        mid = playerViewModel.upId,
                        name = playerViewModel.upName,
                        face = playerViewModel.upFace
                    )
                },
                onShowDanmakuChange = {
                    Prefs.showDanmaku = it
                    playerViewModel.showDanmaku = it
                },
                onLoopPlayModeChange = {
                    Prefs.isLoop = it
                    playerViewModel.isLoop = it
                },
                onInfoVisibilityChanged = { visible ->
                    showLiveControlPanel = playerViewModel.isLive && visible
                },

                // SponsorBlock 相关参数
                enableSponsorBlock = playerViewModel.enableSponsorBlock,
                sponsorBlockSkipMode = playerViewModel.sponsorBlockSkipMode,
                sponsorSegments = playerViewModel.sponsorSegments,
                showSponsorBlockTip = playerViewModel.showSponsorBlockTip,
                currentSponsorSegment = playerViewModel.currentSponsorSegment,
                onShowSponsorBlockTip = { segment ->
                    playerViewModel.currentSponsorSegment = segment
                    playerViewModel.showSponsorBlockTip = true
                },
                onSkipSponsorSegment = { segment ->
                    playerViewModel.skipSponsorSegment(segment)
                },
                onDismissSponsorBlockTip = {
                    playerViewModel.dismissSponsorBlockTip()
                },

                userActionButtonIds = if (Prefs.isLogin && !playerViewModel.fromSeason) {
                    setOf(
                        PlayerBottomControlPanelButtonIds.Like,
                        PlayerBottomControlPanelButtonIds.Favorite,
                        PlayerBottomControlPanelButtonIds.Coin
                    )
                } else {
                    emptySet()
                },
                userActionButtonContent = { buttonId, modifier, contentPadding, onPauseAutoHide ->
                    if (Prefs.isLogin && !playerViewModel.fromSeason) {
                        when (buttonId) {
                            PlayerBottomControlPanelButtonIds.Like -> {
                                LikeButton(
                                    modifier = modifier,
                                    contentPadding = contentPadding,
                                    colors = ButtonDefaults.colors(
                                        containerColor = Color.Transparent,
                                        focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        focusedContentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = ButtonDefaults.border(
                                        border = Border(
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = Color.Transparent
                                            )
                                        ),
                                        focusedBorder = Border(
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = Color.White.copy(alpha = 0.45f)
                                            )
                                        )
                                    ),
                                    isLike = sharedActionState.liked,
                                    onToggleLike = {
                                        val aid = playerViewModel.currentAid
                                        scope.launch {
                                            val flow = getStateFlow(aid, Prefs.uid)
                                            val current = flow.value
                                            if (current.liked) {
                                                val success = VideoUserActionManager.delLike(aid, Prefs.uid)
                                                if (!success) {
                                                    "点赞失败".toast(context)
                                                }
                                            } else {
                                                val success = VideoUserActionManager.addLike(aid, Prefs.uid)
                                                if (!success) {
                                                    "取消点赞失败".toast(context)
                                                }
                                            }
                                        }
                                    }
                                )
                            }

                            PlayerBottomControlPanelButtonIds.Favorite -> {
                                FavoriteButton(
                                    modifier = modifier,
                                    contentPadding = contentPadding,
                                    colors = ButtonDefaults.colors(
                                        containerColor = Color.Transparent,
                                        focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        focusedContentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = ButtonDefaults.border(
                                        border = Border(
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = Color.Transparent
                                            )
                                        ),
                                        focusedBorder = Border(
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = Color.White.copy(alpha = 0.45f)
                                            )
                                        )
                                    ),
                                    dialogContainerColor = Color.Black.copy(alpha = 0.5f),
                                    isFavorite = sharedActionState.favorited,
                                    userFavoriteFolders = sharedActionState.favoriteFolders,
                                    favoriteFolderIds = sharedActionState.favoriteFolderIds,
                                    onAddToDefaultFavoriteFolder = {
                                        scope.launch {
                                            val success = VideoUserActionManager.addToDefaultFavoriteFolder(playerViewModel.currentAid, Prefs.uid)
                                            if (!success) {
                                                "收藏操作失败".toast(context)
                                            }
                                        }
                                    },
                                    onUpdateFavoriteFolders = {
                                        scope.launch {
                                            val success = VideoUserActionManager.updateVideoFavoriteFolders(playerViewModel.currentAid, it, Prefs.uid)
                                            if (!success) {
                                                "收藏操作失败".toast(context)
                                            }
                                        }
                                    },
                                    onDialogVisibilityChanged = onPauseAutoHide
                                )
                            }

                            PlayerBottomControlPanelButtonIds.Coin -> {
                                CoinButton(
                                    modifier = modifier,
                                    contentPadding = contentPadding,
                                    colors = ButtonDefaults.colors(
                                        containerColor = Color.Transparent,
                                        focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        focusedContentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = ButtonDefaults.border(
                                        border = Border(
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = Color.Transparent
                                            )
                                        ),
                                        focusedBorder = Border(
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = Color.White.copy(alpha = 0.45f)
                                            )
                                        )
                                    ),
                                    isCoin = sharedActionState.coin,
                                    onAddCoin = {
                                        scope.launch {
                                            val success = VideoUserActionManager.addCoin(playerViewModel.currentAid, Prefs.uid)
                                            withContext(Dispatchers.Main) {
                                                if (!success) {
                                                    "投币失败".toast(context)
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )

            // 显示跳过提示
            if (autoActionTipVisible) {
                SkipTip(
                    show = true,
                    text = autoActionTipText,
                    align = Alignment.BottomEnd
                )
            }

            InteractiveOptionDialog(
                show = playerViewModel.showInteractiveOptionDialog,
                options = playerViewModel.interactiveOptions,
                onSelectOption = { option ->
                    playerViewModel.saveSubtitleSmartDisplayPreferenceIfNeeded()
                    PlayedAidsCache.markPlayed(option.aid)
                    playerViewModel.playInteractiveOption(option)
                },
                onExit = exitPlayer
            )

            // 推荐视频
            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
                visible = playerViewModel.showRelatedVideos,
                enter = expandVertically(),
                exit = shrinkVertically(),
                label = "RelatedVideosForPlayer"
            ) {
                VideosRow(
                    header = stringResource(R.string.video_info_related_video_title),
                    videos = playerViewModel.relatedVideos,
                    showMore = {},
                    focusRequester = relatedVideosFocusRequester,
                    onOpenSeasonInfo = { videoData ->
                        playerViewModel.saveSubtitleSmartDisplayPreferenceIfNeeded()
                        SeasonInfoActivity.actionStart(
                            context = context,
                            epId = videoData.epId!!,
                            proxyArea = ProxyArea.checkProxyArea(videoData.title)
                        )
                    },
                    onOpenVideoInfo = { videoData ->
                        playerViewModel.saveSubtitleSmartDisplayPreferenceIfNeeded()
                        VideoInfoActivity.actionStart(
                            context = context,
                            aid = videoData.avid,
                            fromPlayer = true
                        )
                    }
                )
            }

            // 评论面板
            if (!useCommentSplitScreen && playerViewModel.currentAid > 0) {
                CommentPanel(
                    show = showCommentPanel,
                    oid = playerViewModel.currentAid,
                    onHide = { showCommentPanel = false }
                )
            }

            // 在线观看人数 Tip
            OnlineViewerCountTip(
                show = showOnlineViewerCountTip,
                count = onlineViewerCount
            )

            // 直播人气 Tip（左下角常驻）
            LiveViewerCountTip(
                show = playerViewModel.isLive && Prefs.showLivePopularity && !showLiveControlPanel,
                popularityText = playerViewModel.livePopularityText,
                onlineCount = playerViewModel.liveOnlineCount
            )

            // 一键三连 Tip
            TripleLikeTip(
                show = showTripleLikeTip,
                message = tripleLikeTipMessage
            )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(splitMaskWidth)
                        .fillMaxHeight()
                        .background(Color.Black.copy(alpha = 0.95f))
                )

                if (useCommentSplitScreen) {
                    CommentPanel(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(splitCommentPanelWidth)
                            .fillMaxHeight(),
                        show = splitCommentPanelVisible,
                        oid = playerViewModel.currentAid,
                        onHide = { showCommentPanel = false },
                        embedded = true
                    )
                }
            }
        }

        // 简介弹窗
        VideoDescriptionDialog(
            show = showDescriptionDialog,
            onHideDialog = { showDescriptionDialog = false },
            description = videoDescription
        )

        // 风控 Geetest：本机十字光标 / 手机扫码代验证
        if (playerViewModel.showGeetestDialog) {
            GeetestTvVerifyDialog(
                gt = playerViewModel.geetestGt,
                challenge = playerViewModel.geetestChallenge,
                onResult = { result ->
                    playerViewModel.onGeetestResult(
                        challenge = result.challenge,
                        validate = result.validate,
                        seccode = result.seccode,
                    )
                },
                onDismiss = {
                    playerViewModel.onGeetestCancelled()
                },
                onRefreshChallenge = playerViewModel::refreshGeetestChallenge,
            )
        }
    }
}
