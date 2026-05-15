package dev.aaa1115910.bv.mobile.screen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.origeek.imageViewer.previewer.ImagePreviewer
import com.origeek.imageViewer.previewer.ImagePreviewerState
import com.origeek.imageViewer.previewer.VerticalDragType
import com.origeek.imageViewer.previewer.rememberPreviewerState
import dev.aaa1115910.biliapi.entity.FavoriteFolderMetadata
import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.biliapi.entity.reply.Comment
import dev.aaa1115910.biliapi.entity.reply.CommentSort
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.mobile.activities.VideoPlayerActivity
import dev.aaa1115910.bv.mobile.component.player.VideoPlayerPages
import dev.aaa1115910.bv.mobile.component.reply.CommentItem
import dev.aaa1115910.bv.mobile.component.reply.ReplySheetScaffold
import dev.aaa1115910.bv.mobile.component.videocard.RelatedVideoItem
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme
import dev.aaa1115910.bv.mobile.util.saveImageToGallery
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerDanmakuMasksData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerHistoryData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerLoadStateData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerLogsData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerPaymentData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerSeekThumbData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerVideoInfoData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerVideoShotData
import dev.aaa1115910.bv.player.entity.VideoListPart
import dev.aaa1115910.bv.player.entity.VideoListPgcEpisode
import dev.aaa1115910.bv.player.entity.VideoListItemData
import dev.aaa1115910.bv.player.entity.VideoListUgcEpisode
import dev.aaa1115910.bv.player.entity.VideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.VideoPlayerDanmakuMasksData
import dev.aaa1115910.bv.player.entity.VideoPlayerHistoryData
import dev.aaa1115910.bv.player.entity.VideoPlayerLoadStateData
import dev.aaa1115910.bv.player.entity.VideoPlayerLogsData
import dev.aaa1115910.bv.player.entity.VideoPlayerPaymentData
import dev.aaa1115910.bv.player.entity.VideoPlayerSeekThumbData
import dev.aaa1115910.bv.player.entity.VideoPlayerVideoInfoData
import dev.aaa1115910.bv.player.entity.VideoPlayerVideoShotData
import dev.aaa1115910.bv.player.mobile.BvPlayer
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.settings.PlayerSettingsProvider
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.formatPubTimeString
import dev.aaa1115910.bv.util.ifElse
import dev.aaa1115910.bv.util.swapList
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.player.entity.LiveCodec
import dev.aaa1115910.bv.viewmodel.CommentViewModel
import dev.aaa1115910.bv.viewmodel.LiveDanmakuMessage
import dev.aaa1115910.bv.viewmodel.SeasonViewModel
import dev.aaa1115910.bv.viewmodel.VideoPlayerV3ViewModel
import dev.aaa1115910.bv.viewmodel.video.VideoDetailViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

private data class ReplyDraftTarget(
    val title: String,
    val placeholder: String,
    val root: Long? = null,
    val parent: Long? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    playerViewModel: VideoPlayerV3ViewModel = koinViewModel(),
    commentVideModel: CommentViewModel = koinViewModel(),
    seasonVideModel: SeasonViewModel = koinViewModel(),
    videoDetailViewModel: VideoDetailViewModel = koinViewModel(),
    windowSizeClass: WindowSizeClass
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger("VideoPlayerScreen")
    val playerSettings = PlayerSettingsProvider.current

    var isVideoFullscreen by rememberSaveable { mutableStateOf(false) }
    val forcePortrait =
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact || windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact

    val pictures = remember { mutableStateListOf<Picture>() }
    val previewerState = rememberPreviewerState(
        verticalDragType = VerticalDragType.UpAndDown,
        pageCount = { pictures.size },
        getKey = { pictures[it].key }
    )
    val replySheetState = rememberBottomSheetScaffoldState()
    var replyDraftTarget by remember { mutableStateOf<ReplyDraftTarget?>(null) }
    var liveDanmakuDraft by remember { mutableStateOf("") }
    var showLiveDanmakuDialog by remember { mutableStateOf(false) }
    var savingPreviewImage by remember { mutableStateOf(false) }
    var savingCoverImage by remember { mutableStateOf(false) }

    val setPreviewerPictures: (List<Picture>, () -> Unit) -> Unit =
        { newPictures, afterSetPictures ->
            pictures.clear()
            pictures.addAll(newPictures)
            afterSetPictures()
        }
    val launchVideoAction: (suspend () -> Result<String>) -> Unit = { action ->
        scope.launch(Dispatchers.IO) {
            val result = action()
            withContext(Dispatchers.Main) {
                result
                    .onSuccess { it.toast(context) }
                    .onFailure { (it.localizedMessage ?: "操作失败").toast(context) }
            }
        }
    }
    val openVideoReplyInput = {
        replyDraftTarget = ReplyDraftTarget(
            title = "评论视频",
            placeholder = "发一条友善的评论"
        )
    }
    val openCommentReplyInput: (Comment, Long) -> Unit = { comment, root ->
        replyDraftTarget = ReplyDraftTarget(
            title = "回复 ${comment.member.name}",
            placeholder = "回复 @${comment.member.name}",
            root = root,
            parent = comment.rpid
        )
    }
    val shareVideo: () -> Unit = share@{
        val detail = videoDetailViewModel.videoDetail ?: return@share
        val url = detail.bvid
            .takeIf { it.isNotBlank() }
            ?.let { "https://www.bilibili.com/video/$it" }
            ?: "https://www.bilibili.com/video/av${detail.aid}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "${detail.title} $url")
        }
        context.startActivity(Intent.createChooser(intent, "分享视频"))
    }
    val saveCover: () -> Unit = saveCover@{
        val coverUrl = videoDetailViewModel.videoDetail?.cover?.takeIf { it.isNotBlank() }
        if (coverUrl == null) {
            "封面不存在".toast(context)
            return@saveCover
        }
        if (savingCoverImage) return@saveCover
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                savingCoverImage = true
            }
            runCatching {
                saveImageToGallery(context, coverUrl)
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    "封面已保存到相册".toast(context)
                }
            }.onFailure {
                logger.warn(it) { "Save cover image failed" }
                withContext(Dispatchers.Main) {
                    "保存失败：${it.localizedMessage ?: "未知错误"}".toast(context)
                }
            }
            withContext(Dispatchers.Main) {
                savingCoverImage = false
            }
        }
    }
    val liveRoomUrl: () -> String = {
        "https://live.bilibili.com/${playerViewModel.liveRoomId}"
    }
    val copyLiveRoomUrl: () -> Unit = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("直播间链接", liveRoomUrl()))
        "已复制直播间链接".toast(context)
    }
    val shareLiveRoom: () -> Unit = {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "${playerViewModel.title.ifBlank { "直播间" }} ${liveRoomUrl()}"
            )
        }
        context.startActivity(Intent.createChooser(intent, "分享直播间"))
    }
    val openLiveInBrowser: () -> Unit = {
        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(liveRoomUrl())))
    }
    val sendLiveDanmaku: () -> Unit = {
        val message = liveDanmakuDraft
        scope.launch(Dispatchers.IO) {
            val result = playerViewModel.sendLiveDanmaku(message)
            withContext(Dispatchers.Main) {
                result
                    .onSuccess {
                        it.toast(context)
                        liveDanmakuDraft = ""
                        showLiveDanmakuDialog = false
                    }
                    .onFailure { (it.localizedMessage ?: "发送失败").toast(context) }
            }
        }
    }
    val likeLiveRoom: () -> Unit = {
        playerViewModel.liveLikeClickCount += 1
        val clickTime = playerViewModel.liveLikeClickCount
        scope.launch(Dispatchers.IO) {
            val result = playerViewModel.likeLiveRoom(clickTime)
            withContext(Dispatchers.Main) {
                result
                    .onSuccess { it.toast(context) }
                    .onFailure { (it.localizedMessage ?: "点赞失败").toast(context) }
            }
        }
    }

    SideEffect {
        val window = (context as Activity).window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        // 设置系统栏可见性
        if (isVideoFullscreen) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }

        // 设置系统栏外观
        if (windowSizeClass.widthSizeClass != WindowWidthSizeClass.Expanded) {
            insetsController.isAppearanceLightStatusBars = false
        }

        // 设置屏幕方向
        if (forcePortrait) {
            if (isVideoFullscreen) {
                context.requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                //在模拟器设为手机尺寸时，横屏时会莫名其妙抛出异常，貌似与折叠屏特性有关，因此手机上强制竖屏
                //java.lang.IllegalArgumentException: Bounding rectangle must start at the top or left window edge for folding features
                @SuppressLint("SourceLockedOrientationActivity")
                context.requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }
        } else {
            context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    BackHandler(previewerState.canClose || previewerState.animating) {
        if (previewerState.canClose) scope.launch {
            previewerState.closeTransform()
        }
    }

    BackHandler(isVideoFullscreen) {
        isVideoFullscreen = false
    }

    Scaffold(
        containerColor = if (windowSizeClass.widthSizeClass != WindowWidthSizeClass.Expanded) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .ifElse(
                    !isVideoFullscreen,
                    Modifier.padding(top = innerPadding.calculateTopPadding())
                )
            //.padding(top = innerPadding.calculateTopPadding())
        ) {
            val leftPartWidth by animateFloatAsState(
                targetValue = if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded && !isVideoFullscreen) 0.6f else 1f,
                label = "VideoPlayerLeftPartWidth"
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth(leftPartWidth)
            ) {
                if (playerViewModel.videoPlayer != null) {
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
                            availableResolutions = if (playerViewModel.isLive) {
                                playerViewModel.availableLiveQualities
                                    .mapNotNull { Resolution.fromCode(it.first) }
                                    .distinctBy { it.code }
                            } else {
                                playerViewModel.availableQuality
                            },
                            availableVideoCodec = playerViewModel.availableVideoCodec,
                            availableAudio = playerViewModel.availableAudio,
                            availableSubtitleTracks = playerViewModel.availableSubtitle,
                            availableVideoList = playerViewModel.availableVideoList,
                            currentVideoCid = playerViewModel.currentCid,
                            currentResolution = if (playerViewModel.isLive) {
                                Resolution.fromCode(playerViewModel.currentLiveQn)
                                    ?: playerViewModel.availableQuality.firstOrNull()
                                    ?: Resolution.R1080P
                            } else {
                                playerViewModel.currentQuality
                            },
                            currentVideoCodec = playerViewModel.currentVideoCodec,
                            currentVideoAspectRatio = playerViewModel.currentVideoAspectRatio,
                            currentVideoSpeed = playerViewModel.currentPlaySpeed,
                            currentAudio = playerViewModel.currentAudio,
                            currentDanmakuEnabled = playerViewModel.currentDanmakuEnabled,
                            currentDanmakuEnabledList = playerViewModel.currentDanmakuTypes,
                            currentDanmakuScale = playerViewModel.currentDanmakuScale,
                            currentDanmakuOpacity = playerViewModel.currentDanmakuOpacity,
                            currentDanmakuArea = playerViewModel.currentDanmakuArea,
                            currentDanmakuMask = playerViewModel.currentDanmakuMask,
                            currentSubtitleId = playerViewModel.currentSubtitleId,
                            currentSubtitleData = playerViewModel.currentSubtitleData,
                            currentSubtitleFontSize = playerViewModel.currentSubtitleFontSize,
                            currentSubtitleBackgroundOpacity = playerViewModel.currentSubtitleBackgroundOpacity,
                            currentSubtitleBottomPadding = playerViewModel.currentSubtitleBottomPadding,
                            currentPlayMode = playerViewModel.currentPlayMode,
                            incognitoMode = playerSettings.incognitoMode,
                            defaultStartPosition = playerSettings.playerDefaultStartPosition.toPlayerType(),
                            isLive = playerViewModel.isLive,
                            availableLiveQualities = playerViewModel.availableLiveQualities,
                            currentLiveQn = playerViewModel.currentLiveQn,
                            currentLiveQualityDescription = playerViewModel.currentLiveQualityDescription,
                            currentLiveCodec = playerViewModel.currentLiveCodec
                        ),
                        LocalVideoPlayerDanmakuMasksData provides VideoPlayerDanmakuMasksData(
                            danmakuMasks = playerViewModel.danmakuMasks,
                        ),
                        LocalVideoPlayerVideoShotData provides VideoPlayerVideoShotData(
                            videoShot = playerViewModel.videoShot,
                        ),
                    ) {
                        BvPlayer(
                            modifier = if (isVideoFullscreen) Modifier
                                .fillMaxSize()
                                .zIndex(1f)
                            else Modifier
                                .ifElse(
                                    { windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded },
                                    Modifier
                                        .padding(12.dp, 0.dp, 12.dp, 12.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                )
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f),
                            isFullScreen = isVideoFullscreen,
                            videoPlayer = playerViewModel.videoPlayer!!,
                            danmakuPlayer = playerViewModel.danmakuPlayer,
                            onClearBackToHistoryData = { playerViewModel.lastPlayed = 0 },
                            onReloadDanmakuAfterSeek = playerViewModel::reloadDanmakuAfterSeek,
                            onEnterFullScreen = {
                                isVideoFullscreen = true
                            },
                            onExitFullScreen = {
                                isVideoFullscreen = false
                            },
                            onBack = { (context as Activity).finish() },
                            onChangeResolution = { code, afterChange ->
                                scope.launch(Dispatchers.IO) {
                                    if (playerViewModel.isLive) {
                                        playerViewModel.changeLiveQuality(code.code)
                                    } else {
                                        playerViewModel.currentQuality = code
                                        playerViewModel.playQuality(code)
                                    }
                                    afterChange()
                                }
                            },
                            onChangeVideoCodec = { codec, afterChange ->
                                scope.launch(Dispatchers.IO) {
                                    playerViewModel.currentVideoCodec = codec
                                    playerViewModel.playQuality(codec = codec)
                                    afterChange()
                                }
                            },
                            onChangeAudio = { audio, afterChange ->
                                scope.launch(Dispatchers.IO) {
                                    playerViewModel.currentAudio = audio
                                    playerViewModel.playQuality(audio = audio)
                                    afterChange()
                                }
                            },
                            onChangeSpeed = { speed ->
                                playerViewModel.currentPlaySpeed = speed
                            },
                            onToggleDanmaku = { enabled ->
                                playerViewModel.currentDanmakuEnabled = enabled
                                if (playerViewModel.isLive) playerViewModel.showDanmaku = enabled
                                playerSettings.defaultDanmakuEnabledMutable = enabled
                            },
                            onEnabledDanmakuTypesChange = { types ->
                                playerViewModel.currentDanmakuTypes.swapList(types)
                            },
                            onDanmakuOpacityChange = { opacity ->
                                playerViewModel.currentDanmakuOpacity = opacity
                                playerSettings.defaultDanmakuOpacityMutable = opacity
                            },
                            onDanmakuScaleChange = { scale ->
                                playerViewModel.currentDanmakuScale = scale
                                playerSettings.defaultMobileDanmakuScaleMutable = scale
                            },
                            onDanmakuAreaChange = { area ->
                                playerViewModel.currentDanmakuArea = area
                                playerSettings.defaultDanmakuAreaMutable = area
                            },
                            onPlayModeChange = { playMode ->
                                playerViewModel.currentPlayMode = playMode
                                playerSettings.defaultPlayModeMutable = playMode
                            },
                            onLoadNextVideo = playerViewModel::playNextVideo,
                            onSendHeartbeat = playerViewModel::uploadHistory,
                            onLoadNewVideo = { videoListItem ->
                                logger.fInfo { "on load new video: $videoListItem" }
                                var aid = 0L
                                var cid = 0L
                                var epid: Int? = null
                                var seasonId: Int? = null

                                when (videoListItem) {
                                    is VideoListItemData -> {
                                        val targetCid = videoListItem.cid ?: return@BvPlayer
                                        aid = videoListItem.aid
                                        cid = targetCid
                                        epid = videoListItem.epid
                                        seasonId = videoListItem.seasonId
                                    }
                                }
                                playerViewModel.loadPlayUrl(
                                    avid = aid,
                                    cid = cid,
                                    epid = epid,
                                    seasonId = seasonId,
                                    continuePlayNext = true,
                                    initialSeekPositionMs = (videoListItem as? dev.aaa1115910.bv.player.entity.VideoListInteractiveNode)?.startPos?.times(1000L)
                                )
                                (videoListItem as? dev.aaa1115910.bv.player.entity.VideoListInteractiveNode)?.let {
                                    playerViewModel.selectInteractiveNode(it.nodeId)
                                    playerViewModel.refreshInteractiveBranches(it.edgeId)
                                }
                            },
                            danmakuOpacity = playerViewModel.currentDanmakuOpacity,
                            isLive = playerViewModel.isLive,
                            onLiveDanmakuPlayerReady = { player ->
                                playerViewModel.setLivePlayer(player)
                            }
                        )
                    }
                }
                if (playerViewModel.isLive) {
                    if (windowSizeClass.widthSizeClass != WindowWidthSizeClass.Expanded) {
                        LiveRoomPanel(
                            modifier = Modifier.fillMaxSize(),
                            playerViewModel = playerViewModel,
                            onSendDanmakuClick = { showLiveDanmakuDialog = true },
                            onLikeClick = likeLiveRoom,
                            onRefresh = { playerViewModel.loadLiveStreamWithQuality(playerViewModel.liveRoomId, playerViewModel.currentLiveQn) },
                            onQualitySelected = playerViewModel::changeLiveQuality,
                            onCodecSelected = playerViewModel::changeLiveCodec,
                            onCopyLink = copyLiveRoomUrl,
                            onShare = shareLiveRoom,
                            onOpenBrowser = openLiveInBrowser,
                            showHeader = true,
                            backgroundColor = Color.Black
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp)
                        ) {
                            item {
                                LivePlayerInfo(
                                    modifier = Modifier.padding(12.dp),
                                    upAvatar = playerViewModel.upFace,
                                    upName = playerViewModel.upName,
                                    title = playerViewModel.title,
                                    roomId = playerViewModel.liveRoomId,
                                    popularityText = playerViewModel.livePopularityText,
                                    onlineCountText = playerViewModel.liveOnlineCount,
                                    qualityText = playerViewModel.currentLiveQualityDescription,
                                    watchedText = playerViewModel.liveWatchedShow,
                                    liveTime = playerViewModel.liveTime,
                                    cover = playerViewModel.liveCover,
                                    backgroundColor = MaterialTheme.colorScheme.surfaceContainer
                                )
                            }
                            item {
                                LiveRoomInputBar(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp)
                                        .padding(bottom = 12.dp),
                                    playerViewModel = playerViewModel,
                                    onSendDanmakuClick = { showLiveDanmakuDialog = true },
                                    onLikeClick = likeLiveRoom,
                                    onQualitySelected = playerViewModel::changeLiveQuality,
                                    onCodecSelected = playerViewModel::changeLiveCodec,
                                    onRefresh = { playerViewModel.loadLiveStreamWithQuality(playerViewModel.liveRoomId, playerViewModel.currentLiveQn) },
                                    onCopyLink = copyLiveRoomUrl,
                                    onShare = shareLiveRoom,
                                    onOpenBrowser = openLiveInBrowser
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.navigationBarsPadding())
                            }
                        }
                    }
                } else {
                    val titles = listOf("简介", "评论")
                    val pagerState = rememberPagerState(
                        initialPage = 0,
                        initialPageOffsetFraction = 0f,
                        pageCount = { 2 }
                    )
                    if (windowSizeClass.widthSizeClass != WindowWidthSizeClass.Expanded) {
                    // 小屏幕下的视频详情推荐/评论
                    ReplySheetScaffold(
                        aid = commentVideModel.commentId,
                        rpid = commentVideModel.rpid,
                        repliesCount = commentVideModel.rpCount,
                        sheetState = replySheetState,
                        previewerState = previewerState,
                        onShowPreviewer = setPreviewerPictures,
                        onReplyComment = openCommentReplyInput
                    ) {
                        Column {
                            PrimaryTabRow(
                                selectedTabIndex = pagerState.currentPage
                            ) {
                                titles.forEachIndexed { index, title ->
                                    Tab(
                                        selected = pagerState.currentPage == index,
                                        onClick = { scope.launch { pagerState.scrollToPage(index) } },
                                        text = {
                                            Text(
                                                text = title,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    )
                                }
                            }
                            HorizontalPager(
                                state = pagerState
                            ) { page ->
                                when (page) {
                                    0 -> {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            item {
                                                VideoPlayerInfo(
                                                    modifier = Modifier.padding(12.dp),
                                                    upAvatar = videoDetailViewModel.videoDetail?.author?.face
                                                        ?: "",
                                                    upName = videoDetailViewModel.videoDetail?.author?.name
                                                        ?: "",
                                                    upFollowerCount = videoDetailViewModel.upOwnerStats?.followerCount,
                                                    upArchiveCount = videoDetailViewModel.upOwnerStats?.archiveCount,
                                                    title = videoDetailViewModel.videoDetail?.title
                                                        ?: "",
                                                    description = videoDetailViewModel.videoDetail?.description
                                                        ?: "",
                                                    playCount = videoDetailViewModel.videoDetail?.stat?.view
                                                        ?: 0,
                                                    danmakuCount = videoDetailViewModel.videoDetail?.stat?.danmaku
                                                        ?: 0,
                                                    likeCount = videoDetailViewModel.videoDetail?.stat?.like
                                                        ?: 0,
                                                    coinCount = videoDetailViewModel.videoDetail?.stat?.coin
                                                        ?: 0,
                                                    favoriteCount = videoDetailViewModel.videoDetail?.stat?.favorite
                                                        ?: 0,
                                                    shareCount = videoDetailViewModel.videoDetail?.stat?.share
                                                        ?: 0,
                                                    date = videoDetailViewModel.videoDetail?.publishDate
                                                        ?.formatPubTimeString(context) ?: "",
                                                    avid = videoDetailViewModel.videoDetail?.aid
                                                        ?: 0,
                                                    liked = videoDetailViewModel.videoDetail?.userActions?.like == true,
                                                    disliked = videoDetailViewModel.videoDetail?.userActions?.dislike == true,
                                                    coined = videoDetailViewModel.videoDetail?.userActions?.coin == true,
                                                    favorited = videoDetailViewModel.videoDetail?.userActions?.favorite == true,
                                                    inToView = videoDetailViewModel.inToView,
                                                    favoriteFolders = videoDetailViewModel.favoriteFolders,
                                                    favoriteFolderIds = videoDetailViewModel.favoriteFolderIds,
                                                    userActionUpdating = videoDetailViewModel.userActionUpdating,
                                                    savingCover = savingCoverImage,
                                                    onToggleLike = { launchVideoAction { videoDetailViewModel.toggleLike() } },
                                                    onTripleLike = { launchVideoAction { videoDetailViewModel.tripleLike() } },
                                                    onToggleDislike = { launchVideoAction { videoDetailViewModel.toggleDislike() } },
                                                    onAddCoin = { launchVideoAction { videoDetailViewModel.addCoin() } },
                                                    onAddToDefaultFavoriteFolder = {
                                                        launchVideoAction { videoDetailViewModel.addToDefaultFavoriteFolder() }
                                                    },
                                                    onUpdateFavoriteFolders = { folderIds ->
                                                        launchVideoAction { videoDetailViewModel.updateFavoriteFolders(folderIds) }
                                                    },
                                                    onToggleToView = { launchVideoAction { videoDetailViewModel.toggleToView() } },
                                                    onShare = shareVideo,
                                                    onSaveCover = saveCover
                                                )
                                            }
                                            item {
                                                VideoPlayerPages(
                                                    currentCid = playerViewModel.currentCid,
                                                    interactiveNodes = videoDetailViewModel.videoDetail?.interactiveNodes
                                                        ?: emptyList(),
                                                    pages = videoDetailViewModel.videoDetail?.pages
                                                        ?: emptyList(),
                                                    ugcSeason = videoDetailViewModel.videoDetail?.ugcSeason,
                                                    pgcSections = seasonVideModel.seasonData?.sections
                                                        ?: emptyList(),
                                                    onClickInteractiveNode = { node ->
                                                        playerViewModel.selectInteractiveNode(node.nodeId)
                                                        playerViewModel.loadPlayUrl(
                                                            avid = videoDetailViewModel.videoDetail!!.aid,
                                                            cid = node.cid,
                                                            continuePlayNext = true,
                                                            initialSeekPositionMs = node.startPos?.times(1000L)
                                                        )
                                                        playerViewModel.refreshInteractiveBranches(node.edgeId)
                                                    },
                                                    onClickPage = { videoPage ->
                                                        playerViewModel.loadPlayUrl(
                                                            avid = videoDetailViewModel.videoDetail!!.aid,
                                                            cid = videoPage.cid,
                                                            continuePlayNext = true
                                                        )
                                                    },
                                                    onClickEpisode = { sectionIndex, episode ->
                                                        videoDetailViewModel.updateUgcSeasonSectionVideoList(
                                                            sectionIndex
                                                        )
                                                        playerViewModel.loadPlayUrl(
                                                            avid = episode.aid,
                                                            cid = episode.cid,
                                                            epid = episode.epid,
                                                            continuePlayNext = true
                                                        )
                                                    }
                                                )
                                            }
                                            items(
                                                items = videoDetailViewModel.videoDetail?.relatedVideos
                                                    ?: emptyList()
                                            ) { relatedVideo ->
                                                RelatedVideoItem(
                                                    relatedVideo = relatedVideo,
                                                    onClick = {
                                                        VideoPlayerActivity.actionStart(
                                                            context = context,
                                                            aid = relatedVideo.aid,
                                                            fromSeason = relatedVideo.jumpToSeason
                                                        )
                                                    }
                                                )
                                            }
                                            item {
                                                Spacer(modifier = Modifier.navigationBarsPadding())
                                            }
                                        }
                                    }

                                    1 -> {
                                        VideoComments(
                                            previewerState = previewerState,
                                            comments = commentVideModel.comments,
                                            commentSort = commentVideModel.commentSort,
                                            refreshingComments = commentVideModel.refreshingComments,
                                            onLoadMoreComments = {
                                                scope.launch(Dispatchers.IO) { commentVideModel.loadMoreComment() }
                                            },
                                            onRefreshComments = {
                                                scope.launch(Dispatchers.IO) { commentVideModel.refreshComments() }
                                            },
                                            onSwitchCommentSort = {
                                                scope.launch(Dispatchers.IO) {
                                                    commentVideModel.switchCommentSort(it)
                                                }
                                            },
                                            onShowPreviewer = setPreviewerPictures,
                                            onReplyVideo = openVideoReplyInput,
                                            onReplyComment = { comment ->
                                                openCommentReplyInput(comment, comment.rpid)
                                            },
                                            onShowReplies = { rpId, repliesCount ->
                                                //logger.info { "show reply sheet: rpid=$replyId" }
                                                commentVideModel.rpid = rpId
                                                commentVideModel.rpCount = repliesCount
                                                scope.launch { replySheetState.bottomSheetState.expand() }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // 大屏幕下视频下方的视频详情和推荐视频
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                    ) {
                        item {
                            VideoPlayerInfo(
                                modifier = Modifier.padding(12.dp),
                                upAvatar = videoDetailViewModel.videoDetail?.author?.face
                                    ?: "",
                                upName = videoDetailViewModel.videoDetail?.author?.name ?: "",
                                upFollowerCount = videoDetailViewModel.upOwnerStats?.followerCount,
                                upArchiveCount = videoDetailViewModel.upOwnerStats?.archiveCount,
                                title = videoDetailViewModel.videoDetail?.title ?: "",
                                description = videoDetailViewModel.videoDetail?.description
                                    ?: "",
                                playCount = videoDetailViewModel.videoDetail?.stat?.view ?: 0,
                                danmakuCount = videoDetailViewModel.videoDetail?.stat?.danmaku
                                    ?: 0,
                                likeCount = videoDetailViewModel.videoDetail?.stat?.like ?: 0,
                                coinCount = videoDetailViewModel.videoDetail?.stat?.coin ?: 0,
                                favoriteCount = videoDetailViewModel.videoDetail?.stat?.favorite ?: 0,
                                shareCount = videoDetailViewModel.videoDetail?.stat?.share ?: 0,
                                date = videoDetailViewModel.videoDetail?.publishDate
                                    ?.formatPubTimeString(context) ?: "",
                                avid = videoDetailViewModel.videoDetail?.aid ?: 0,
                                liked = videoDetailViewModel.videoDetail?.userActions?.like == true,
                                disliked = videoDetailViewModel.videoDetail?.userActions?.dislike == true,
                                coined = videoDetailViewModel.videoDetail?.userActions?.coin == true,
                                favorited = videoDetailViewModel.videoDetail?.userActions?.favorite == true,
                                inToView = videoDetailViewModel.inToView,
                                favoriteFolders = videoDetailViewModel.favoriteFolders,
                                favoriteFolderIds = videoDetailViewModel.favoriteFolderIds,
                                userActionUpdating = videoDetailViewModel.userActionUpdating,
                                savingCover = savingCoverImage,
                                onToggleLike = { launchVideoAction { videoDetailViewModel.toggleLike() } },
                                onTripleLike = { launchVideoAction { videoDetailViewModel.tripleLike() } },
                                onToggleDislike = { launchVideoAction { videoDetailViewModel.toggleDislike() } },
                                onAddCoin = { launchVideoAction { videoDetailViewModel.addCoin() } },
                                onAddToDefaultFavoriteFolder = {
                                    launchVideoAction { videoDetailViewModel.addToDefaultFavoriteFolder() }
                                },
                                onUpdateFavoriteFolders = { folderIds ->
                                    launchVideoAction { videoDetailViewModel.updateFavoriteFolders(folderIds) }
                                },
                                onToggleToView = { launchVideoAction { videoDetailViewModel.toggleToView() } },
                                onShare = shareVideo,
                                onSaveCover = saveCover,
                                backgroundColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        }
                        item {
                            VideoPlayerPages(
                                modifier = Modifier
                                    .padding(vertical = 12.dp)
                                    .clip(MaterialTheme.shapes.medium),
                                currentCid = playerViewModel.currentCid,
                                interactiveNodes = videoDetailViewModel.videoDetail?.interactiveNodes
                                    ?: emptyList(),
                                pages = videoDetailViewModel.videoDetail?.pages ?: emptyList(),
                                ugcSeason = videoDetailViewModel.videoDetail?.ugcSeason,
                                pgcSections = seasonVideModel.seasonData?.sections ?: emptyList(),
                                onClickInteractiveNode = { node ->
                                    playerViewModel.selectInteractiveNode(node.nodeId)
                                    playerViewModel.loadPlayUrl(
                                        avid = videoDetailViewModel.videoDetail!!.aid,
                                        cid = node.cid,
                                        continuePlayNext = true,
                                        initialSeekPositionMs = node.startPos?.times(1000L)
                                    )
                                    playerViewModel.refreshInteractiveBranches(node.edgeId)
                                },
                                onClickPage = { videoPage ->
                                    playerViewModel.loadPlayUrl(
                                        avid = videoDetailViewModel.videoDetail!!.aid,
                                        cid = videoPage.cid,
                                        continuePlayNext = true
                                    )
                                },
                                onClickEpisode = { sectionIndex, episode ->
                                    videoDetailViewModel.updateUgcSeasonSectionVideoList(
                                        sectionIndex
                                    )
                                    playerViewModel.loadPlayUrl(
                                        avid = episode.aid,
                                        cid = episode.cid,
                                        epid = episode.epid,
                                        continuePlayNext = true
                                    )
                                }
                            )
                        }
                        itemsIndexed(
                            items = videoDetailViewModel.videoDetail?.relatedVideos
                                ?: emptyList()
                        ) { index, relatedVideo ->
                            RelatedVideoItem(
                                modifier = Modifier
                                    .ifElse(
                                        { index == 0 },
                                        Modifier.clip(
                                            MaterialTheme.shapes.medium.copy(
                                                bottomStart = CornerSize(0.dp),
                                                bottomEnd = CornerSize(0.dp)
                                            )
                                        )
                                    )
                                    .ifElse(
                                        {
                                            index == (videoDetailViewModel.videoDetail?.relatedVideos?.size
                                                ?: 0) - 1
                                        },
                                        Modifier.clip(
                                            MaterialTheme.shapes.medium.copy(
                                                topStart = CornerSize(0.dp),
                                                topEnd = CornerSize(0.dp)
                                            )
                                        )
                                    ),
                                relatedVideo = relatedVideo,
                                onClick = {
                                    VideoPlayerActivity.actionStart(
                                        context = context,
                                        aid = relatedVideo.aid,
                                        fromSeason = relatedVideo.jumpToSeason
                                    )
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.navigationBarsPadding())
                        }
                    }
                    }
                }
            }
            if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) {
                if (playerViewModel.isLive) {
                    LiveRoomPanel(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .fillMaxHeight(),
                        playerViewModel = playerViewModel,
                        onSendDanmakuClick = { showLiveDanmakuDialog = true },
                        onLikeClick = likeLiveRoom,
                        onRefresh = { playerViewModel.loadLiveStreamWithQuality(playerViewModel.liveRoomId, playerViewModel.currentLiveQn) },
                        onQualitySelected = playerViewModel::changeLiveQuality,
                        onCodecSelected = playerViewModel::changeLiveCodec,
                        onCopyLink = copyLiveRoomUrl,
                        onShare = shareLiveRoom,
                        onOpenBrowser = openLiveInBrowser,
                        showHeader = false,
                        backgroundColor = MaterialTheme.colorScheme.surface
                    )
                } else {
                    // 大屏幕下的右侧评论
                    Box(
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        ReplySheetScaffold(
                            modifier = Modifier,
                            aid = commentVideModel.commentId,
                            rpid = commentVideModel.rpid,
                            repliesCount = commentVideModel.rpCount,
                            sheetState = replySheetState,
                            previewerState = previewerState,
                            onShowPreviewer = setPreviewerPictures,
                            onReplyComment = openCommentReplyInput
                        ) {
                            VideoComments(
                                modifier = Modifier.fillMaxWidth(),
                                previewerState = previewerState,
                                comments = commentVideModel.comments,
                                commentSort = commentVideModel.commentSort,
                                refreshingComments = commentVideModel.refreshingComments,
                                onLoadMoreComments = {
                                    scope.launch(Dispatchers.IO) { commentVideModel.loadMoreComment() }
                                },
                                onRefreshComments = {
                                    scope.launch(Dispatchers.IO) { commentVideModel.refreshComments() }
                                },
                                onSwitchCommentSort = {
                                    scope.launch(Dispatchers.IO) {
                                        commentVideModel.switchCommentSort(it)
                                    }
                                },
                                onShowPreviewer = setPreviewerPictures,
                                onReplyVideo = openVideoReplyInput,
                                onReplyComment = { comment ->
                                    openCommentReplyInput(comment, comment.rpid)
                                },
                                onShowReplies = { rpId, repliesCount ->
                                    //logger.info { "show reply sheet: rpid=$replyId" }
                                    commentVideModel.rpid = rpId
                                    commentVideModel.rpCount = repliesCount
                                    scope.launch { replySheetState.bottomSheetState.expand() }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    ImagePreviewer(
        modifier = Modifier
            .fillMaxSize(),
        state = previewerState,
        imageLoader = { index ->
            val imageRequest = ImageRequest.Builder(LocalContext.current)
                .data(pictures[index].url)
                .size(coil.size.Size.ORIGINAL)
                .build()
            // 获取图片的初始大小
            rememberAsyncImagePainter(imageRequest)
            //rememberAsyncImagePainter(pictures[index].url)
        },
        previewerLayer = {
            foreground = { page ->
                ImagePreviewerActions(
                    saving = savingPreviewImage,
                    onClose = {
                        if (previewerState.canClose) {
                            scope.launch {
                                previewerState.closeTransform()
                            }
                        }
                    },
                    onSave = {
                        val picture = pictures.getOrNull(page)
                        if (picture == null) {
                            "图片不存在".toast(context)
                            return@ImagePreviewerActions
                        }
                        if (savingPreviewImage) return@ImagePreviewerActions
                        scope.launch(Dispatchers.IO) {
                            withContext(Dispatchers.Main) {
                                savingPreviewImage = true
                            }
                            runCatching {
                                saveImageToGallery(context, picture.url)
                            }.onSuccess {
                                withContext(Dispatchers.Main) {
                                    "图片已保存到相册".toast(context)
                                }
                            }.onFailure {
                                logger.warn(it) { "Save preview image failed" }
                                withContext(Dispatchers.Main) {
                                    "保存失败：${it.localizedMessage ?: "未知错误"}".toast(context)
                                }
                            }
                            withContext(Dispatchers.Main) {
                                savingPreviewImage = false
                            }
                        }
                    }
                )
            }
        }
    )

    replyDraftTarget?.let { target ->
        ReplyInputDialog(
            title = target.title,
            placeholder = target.placeholder,
            sending = commentVideModel.sendingComment,
            onDismiss = { replyDraftTarget = null },
            onSend = { message ->
                scope.launch(Dispatchers.IO) {
                    val result = commentVideModel.sendComment(
                        message = message,
                        root = target.root,
                        parent = target.parent
                    )
                    if (result.isSuccess) {
                        withContext(Dispatchers.Main) {
                            replyDraftTarget = null
                        }
                    }
                }
            }
        )
    }

    if (showLiveDanmakuDialog) {
        LiveSendDanmakuDialog(
            value = liveDanmakuDraft,
            sending = playerViewModel.sendingLiveDanmaku,
            onValueChange = { liveDanmakuDraft = it },
            onDismiss = { showLiveDanmakuDialog = false },
            onSend = sendLiveDanmaku
        )
    }
}

@Composable
private fun ImagePreviewerActions(
    saving: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PreviewerActionButton(
            icon = Icons.Filled.Close,
            contentDescription = "关闭预览",
            onClick = onClose
        )
        PreviewerActionButton(
            icon = Icons.Rounded.Download,
            contentDescription = "保存图片",
            enabled = !saving,
            loading = saving,
            onClick = onSave
        )
    }
}

@Composable
private fun PreviewerActionButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.46f)
    ) {
        IconButton(
            enabled = enabled,
            onClick = onClick
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun VideoPlayerInfo(
    modifier: Modifier = Modifier,
    upAvatar: String,
    upName: String,
    upFollowerCount: Int?,
    upArchiveCount: Int?,
    title: String,
    description: String,
    playCount: Long,
    danmakuCount: Int,
    likeCount: Int = 0,
    coinCount: Int = 0,
    favoriteCount: Int = 0,
    shareCount: Int = 0,
    date: String,
    avid: Long,
    liked: Boolean = false,
    disliked: Boolean = false,
    coined: Boolean = false,
    favorited: Boolean = false,
    inToView: Boolean = false,
    favoriteFolders: List<FavoriteFolderMetadata> = emptyList(),
    favoriteFolderIds: List<Long> = emptyList(),
    userActionUpdating: Boolean = false,
    savingCover: Boolean = false,
    onToggleLike: () -> Unit = {},
    onTripleLike: () -> Unit = {},
    onToggleDislike: () -> Unit = {},
    onAddCoin: () -> Unit = {},
    onAddToDefaultFavoriteFolder: () -> Unit = {},
    onUpdateFavoriteFolders: (List<Long>) -> Unit = {},
    onToggleToView: () -> Unit = {},
    onShare: () -> Unit = {},
    onSaveCover: () -> Unit = {},
    backgroundColor: Color = MaterialTheme.colorScheme.surface
) {
    val summaryTextStyle = MaterialTheme.typography.bodySmall.copy(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.height(64.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .padding(0.dp, 8.dp, 8.dp, 8.dp)
                ) {
                    AsyncImage(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Gray),
                        model = upAvatar,
                        contentDescription = null
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = upName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = formatUpStatsText(upFollowerCount, upArchiveCount),
                        style = summaryTextStyle,
                        fontSize = 10.sp
                    )
                }
            }

            Button(onClick = { /*TODO*/ }) {
                Text(text = "Follow")
            }
        }
        Text(
            text = title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium
        )
        ProvideTextStyle(summaryTextStyle) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        modifier = Modifier,
                        painter = painterResource(id = R.drawable.ic_play_count),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(text = "$playCount")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        modifier = Modifier,
                        painter = painterResource(id = R.drawable.ic_danmaku_count),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(text = "$danmakuCount")
                }
                Text(text = date)
                Text(text = "av$avid")
            }
            VideoActionGrid(
                liked = liked,
                disliked = disliked,
                coined = coined,
                favorited = favorited,
                likeCount = likeCount,
                coinCount = coinCount,
                favoriteCount = favoriteCount,
                shareCount = shareCount,
                inToView = inToView,
                favoriteFolders = favoriteFolders,
                favoriteFolderIds = favoriteFolderIds,
                enabled = !userActionUpdating,
                savingCover = savingCover,
                onToggleLike = onToggleLike,
                onTripleLike = onTripleLike,
                onToggleDislike = onToggleDislike,
                onAddCoin = onAddCoin,
                onAddToDefaultFavoriteFolder = onAddToDefaultFavoriteFolder,
                onUpdateFavoriteFolders = onUpdateFavoriteFolders,
                onToggleToView = onToggleToView,
                onShare = onShare,
                onSaveCover = onSaveCover
            )
            Text(text = description)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoActionGrid(
    liked: Boolean,
    disliked: Boolean,
    coined: Boolean,
    favorited: Boolean,
    likeCount: Int,
    coinCount: Int,
    favoriteCount: Int,
    shareCount: Int,
    inToView: Boolean,
    favoriteFolders: List<FavoriteFolderMetadata>,
    favoriteFolderIds: List<Long>,
    enabled: Boolean,
    savingCover: Boolean,
    onToggleLike: () -> Unit,
    onTripleLike: () -> Unit,
    onToggleDislike: () -> Unit,
    onAddCoin: () -> Unit,
    onAddToDefaultFavoriteFolder: () -> Unit,
    onUpdateFavoriteFolders: (List<Long>) -> Unit,
    onToggleToView: () -> Unit,
    onShare: () -> Unit,
    onSaveCover: () -> Unit
) {
    var showFavoriteDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        VideoActionItem(
            modifier = Modifier.weight(1f),
            selected = liked,
            enabled = enabled,
            icon = Icons.Outlined.ThumbUp,
            selectedIcon = Icons.Rounded.ThumbUp,
            label = formatStatCount(likeCount),
            onClick = onToggleLike,
            onLongClick = onTripleLike
        )
        VideoActionItem(
            modifier = Modifier.weight(1f),
            selected = disliked,
            enabled = enabled,
            icon = Icons.Outlined.ThumbDown,
            selectedIcon = Icons.Rounded.ThumbDown,
            label = "点踩",
            onClick = onToggleDislike
        )
        VideoActionItem(
            modifier = Modifier.weight(1f),
            selected = coined,
            enabled = enabled,
            icon = Icons.Outlined.Paid,
            selectedIcon = Icons.Rounded.Paid,
            label = formatStatCount(coinCount),
            onClick = onAddCoin
        )
        VideoActionItem(
            modifier = Modifier.weight(1f),
            selected = favorited,
            enabled = enabled,
            icon = Icons.Outlined.Star,
            selectedIcon = Icons.Rounded.Star,
            label = formatStatCount(favoriteCount),
            onClick = {
                if (favorited || favoriteFolders.size > 1) {
                    showFavoriteDialog = true
                } else {
                    onAddToDefaultFavoriteFolder()
                }
            }
        )
        VideoActionItem(
            modifier = Modifier.weight(1f),
            selected = inToView,
            enabled = enabled,
            icon = Icons.Rounded.WatchLater,
            selectedIcon = Icons.Rounded.WatchLater,
            label = "再看",
            onClick = onToggleToView
        )
        VideoActionItem(
            modifier = Modifier.weight(1f),
            selected = false,
            enabled = true,
            icon = Icons.Rounded.Share,
            selectedIcon = Icons.Rounded.Share,
            label = formatStatCount(shareCount),
            onClick = onShare
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            VideoActionItem(
                modifier = Modifier.fillMaxWidth(),
                selected = false,
                enabled = true,
                icon = Icons.Rounded.MoreVert,
                selectedIcon = Icons.Rounded.MoreVert,
                label = "更多",
                onClick = { showMoreMenu = true }
            )
            DropdownMenu(
                expanded = showMoreMenu,
                onDismissRequest = { showMoreMenu = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(text = if (savingCover) "保存中..." else "保存封面")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null
                        )
                    },
                    enabled = !savingCover,
                    onClick = {
                        showMoreMenu = false
                        onSaveCover()
                    }
                )
            }
        }
    }

    FavoriteFolderDialog(
        show = showFavoriteDialog,
        folders = favoriteFolders,
        selectedFolderIds = favoriteFolderIds,
        enabled = enabled,
        onDismiss = { showFavoriteDialog = false },
        onConfirm = {
            showFavoriteDialog = false
            onUpdateFavoriteFolders(it)
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoActionItem(
    modifier: Modifier = Modifier,
    selected: Boolean,
    enabled: Boolean,
    icon: ImageVector,
    selectedIcon: ImageVector,
    label: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val selectedColor = Color(0xfffb7299)
    val contentColor = when {
        selected -> selectedColor
        enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }

    Column(
        modifier = modifier
            .height(52.dp)
            .clip(MaterialTheme.shapes.small)
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            imageVector = if (selected) selectedIcon else icon,
            contentDescription = null,
            tint = contentColor
        )
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FavoriteFolderDialog(
    show: Boolean,
    folders: List<FavoriteFolderMetadata>,
    selectedFolderIds: List<Long>,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (List<Long>) -> Unit
) {
    if (!show) return

    val selectedIds = remember { mutableStateListOf<Long>() }
    LaunchedEffect(show, selectedFolderIds) {
        selectedIds.clear()
        selectedIds.addAll(selectedFolderIds)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "添加到收藏夹") },
        text = {
            if (folders.isEmpty()) {
                Text(
                    text = if (Prefs.isLogin) "正在获取收藏夹，稍后再试" else "账号未登录",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    folders.forEach { folder ->
                        val selected = selectedIds.contains(folder.id)
                        FilterChip(
                            selected = selected,
                            enabled = enabled,
                            onClick = {
                                if (selected) {
                                    selectedIds.remove(folder.id)
                                } else {
                                    selectedIds.add(folder.id)
                                }
                            },
                            label = {
                                Text(
                                    text = folder.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = enabled && folders.isNotEmpty(),
                onClick = { onConfirm(selectedIds.toList()) }
            ) {
                Text(text = "确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        }
    )
}

@Composable
private fun LiveRoomPanel(
    modifier: Modifier = Modifier,
    playerViewModel: VideoPlayerV3ViewModel,
    onSendDanmakuClick: () -> Unit,
    onLikeClick: () -> Unit,
    onRefresh: () -> Unit,
    onQualitySelected: (Int) -> Unit,
    onCodecSelected: (LiveCodec) -> Unit,
    onCopyLink: () -> Unit,
    onShare: () -> Unit,
    onOpenBrowser: () -> Unit,
    showHeader: Boolean,
    backgroundColor: Color
) {
    Box(
        modifier = modifier.background(backgroundColor)
    ) {
        val liveBackgroundModel: Any = playerViewModel.liveBackground.takeIf { it.isNotBlank() }
            ?: R.drawable.live_default_bg
        AsyncImage(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            model = liveBackgroundModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.6f
        )
        Column(modifier = Modifier.fillMaxSize()) {
            if (showHeader) {
                LivePlayerInfo(
                    modifier = Modifier.padding(12.dp),
                    upAvatar = playerViewModel.upFace,
                    upName = playerViewModel.upName,
                    title = playerViewModel.title,
                    roomId = playerViewModel.liveRoomId,
                    popularityText = playerViewModel.livePopularityText,
                    onlineCountText = playerViewModel.liveOnlineCount,
                    qualityText = playerViewModel.currentLiveQualityDescription,
                    watchedText = playerViewModel.liveWatchedShow,
                    liveTime = playerViewModel.liveTime,
                    cover = playerViewModel.liveCover,
                    backgroundColor = Color.Transparent
                )
            }
            LiveDanmakuList(
                modifier = Modifier.weight(1f),
                messages = playerViewModel.liveDanmakuMessages
            )
            LiveRoomInputBar(
                modifier = Modifier.navigationBarsPadding(),
                playerViewModel = playerViewModel,
                onSendDanmakuClick = onSendDanmakuClick,
                onLikeClick = onLikeClick,
                onQualitySelected = onQualitySelected,
                onCodecSelected = onCodecSelected,
                onRefresh = onRefresh,
                onCopyLink = onCopyLink,
                onShare = onShare,
                onOpenBrowser = onOpenBrowser
            )
        }
    }
}

@Composable
private fun LiveRoomInputBar(
    modifier: Modifier = Modifier,
    playerViewModel: VideoPlayerV3ViewModel,
    onSendDanmakuClick: () -> Unit,
    onLikeClick: () -> Unit,
    onQualitySelected: (Int) -> Unit,
    onCodecSelected: (LiveCodec) -> Unit,
    onRefresh: () -> Unit,
    onCopyLink: () -> Unit,
    onShare: () -> Unit,
    onOpenBrowser: () -> Unit
) {
    var showQualityMenu by remember { mutableStateOf(false) }
    var showCodecMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0x1AFFFFFF),
        shape = MaterialTheme.shapes.large.copy(
            bottomStart = CornerSize(0.dp),
            bottomEnd = CornerSize(0.dp)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(onClick = {
                playerViewModel.currentDanmakuEnabled = !playerViewModel.currentDanmakuEnabled
                playerViewModel.showDanmaku = playerViewModel.currentDanmakuEnabled
            }) {
                Icon(
                    imageVector = if (playerViewModel.currentDanmakuEnabled) {
                        Icons.AutoMirrored.Filled.Comment
                    } else {
                        Icons.AutoMirrored.Filled.Comment
                    },
                    contentDescription = "切换弹幕",
                    tint = Color(0xFFEEEEEE)
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(Color(0x22FFFFFF))
                    .clickable(onClick = onSendDanmakuClick)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "发送弹幕",
                    color = Color(0xFFEEEEEE),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Box {
                TextButton(onClick = { showQualityMenu = true }) {
                    Text(
                        text = playerViewModel.currentLiveQualityDescription.ifBlank { "画质" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFFEEEEEE)
                    )
                }
                DropdownMenu(
                    expanded = showQualityMenu,
                    onDismissRequest = { showQualityMenu = false }
                ) {
                    playerViewModel.availableLiveQualities.forEach { (qn, desc) ->
                        DropdownMenuItem(
                            text = { Text(desc) },
                            onClick = {
                                showQualityMenu = false
                                onQualitySelected(qn)
                            }
                        )
                    }
                }
            }
            IconButton(onClick = onLikeClick) {
                Box {
                    Icon(
                        imageVector = Icons.Rounded.ThumbUp,
                        contentDescription = "点赞",
                        tint = Color(0xFFEEEEEE)
                    )
                    if (playerViewModel.liveLikeClickCount > 0) {
                        Text(
                            modifier = Modifier.align(Alignment.TopEnd),
                            text = "x${playerViewModel.liveLikeClickCount}",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Box {
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "更多",
                        tint = Color(0xFFEEEEEE)
                    )
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                        text = { Text("刷新") },
                        onClick = {
                            showMoreMenu = false
                            onRefresh()
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                        text = { Text("分享直播间") },
                        onClick = {
                            showMoreMenu = false
                            onShare()
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                        text = { Text("复制链接") },
                        onClick = {
                            showMoreMenu = false
                            onCopyLink()
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Rounded.OpenInBrowser, contentDescription = null) },
                        text = { Text("浏览器打开") },
                        onClick = {
                            showMoreMenu = false
                            onOpenBrowser()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("编码：${playerViewModel.currentLiveCodec.getDisplayName(LocalContext.current)}") },
                        onClick = {
                            showMoreMenu = false
                            showCodecMenu = true
                        }
                    )
                }
                DropdownMenu(
                    expanded = showCodecMenu,
                    onDismissRequest = { showCodecMenu = false }
                ) {
                    LiveCodec.entries.forEach { codec ->
                        DropdownMenuItem(
                            text = { Text(codec.getDisplayName(LocalContext.current)) },
                            onClick = {
                                showCodecMenu = false
                                onCodecSelected(codec)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LivePlayerInfo(
    modifier: Modifier = Modifier,
    upAvatar: String,
    upName: String,
    title: String,
    roomId: Int,
    popularityText: String,
    onlineCountText: String,
    qualityText: String,
    watchedText: String = "",
    liveTime: Int? = null,
    cover: String = "",
    backgroundColor: Color = MaterialTheme.colorScheme.surface
) {
    val summaryTextStyle = MaterialTheme.typography.bodySmall.copy(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )
    val stats = listOfNotNull(
        watchedText.takeIf { it.isNotBlank() },
        popularityText.takeIf { it.isNotBlank() },
        onlineCountText.takeIf { it.isNotBlank() },
        liveTime?.let { formatLiveDuration(it) },
        qualityText.takeIf { it.isNotBlank() },
        roomId.takeIf { it > 0 }?.let { "房间 $it" }
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                model = upAvatar,
                contentDescription = null
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = upName.ifBlank { "未知主播" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "正在直播",
                    style = summaryTextStyle,
                    fontSize = 10.sp
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (cover.isNotBlank()) {
                AsyncImage(
                    modifier = Modifier
                        .width(96.dp)
                        .aspectRatio(16f / 9f)
                        .clip(MaterialTheme.shapes.small)
                        .background(Color.Gray),
                    model = cover,
                    contentDescription = null
                )
            }
            Text(
                modifier = Modifier.weight(1f),
                text = title.ifBlank { "直播间" },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
        }
        ProvideTextStyle(summaryTextStyle) {
            Text(
                text = stats.joinToString("  ·  "),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LiveDanmakuList(
    modifier: Modifier = Modifier,
    messages: List<LiveDanmakuMessage>
) {
    val listState = rememberLazyListState()
    var followLatest by remember { mutableStateOf(true) }
    var autoScrolling by remember { mutableStateOf(false) }
    val latestMessageId = messages.lastOrNull()?.id
    val isAtLatest by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItemsCount = listState.layoutInfo.totalItemsCount
            totalItemsCount == 0 || (lastVisibleItem?.index ?: 0) >= totalItemsCount - 1
        }
    }

    LaunchedEffect(listState.isScrollInProgress, isAtLatest, autoScrolling) {
        if (isAtLatest) {
            followLatest = true
        } else if (listState.isScrollInProgress && !autoScrolling) {
            followLatest = false
        }
    }

    LaunchedEffect(latestMessageId, followLatest) {
        if (followLatest && latestMessageId != null) {
            autoScrolling = true
            try {
                listState.scrollToItem(messages.size)
            } finally {
                autoScrolling = false
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
        state = listState,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "直播弹幕",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
        if (messages.isEmpty()) {
            item {
                Text(
                    text = "暂无弹幕",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.68f)
                )
            }
        }
        items(
            items = messages,
            key = { it.id }
        ) { message ->
            LiveDanmakuItem(message = message)
        }
    }
}

@Composable
private fun LiveDanmakuItem(
    modifier: Modifier = Modifier,
    message: LiveDanmakuMessage
) {
    Column(
        modifier = modifier
            .fillMaxWidth(0.96f)
            .clip(MaterialTheme.shapes.small)
            .background(Color.Black.copy(alpha = 0.38f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            message.medalName?.takeIf { it.isNotBlank() }?.let { medalName ->
                Text(
                    text = if (message.medalLevel != null) "$medalName ${message.medalLevel}" else medalName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.inversePrimary
            )
            }
            Text(
                text = message.username.ifBlank { "匿名用户" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.72f)
            )
        }
        Text(
            text = message.content,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}

@Composable
private fun LiveSendDanmakuDialog(
    value: String,
    sending: Boolean,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "发送弹幕") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                minLines = 1,
                maxLines = 3,
                singleLine = false,
                placeholder = { Text("输入弹幕内容") }
            )
        },
        confirmButton = {
            TextButton(
                enabled = value.isNotBlank() && !sending,
                onClick = onSend
            ) {
                Text(text = if (sending) "发送中" else "发送")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        }
    )
}

private fun formatLiveDuration(liveTime: Int): String {
    val seconds = (System.currentTimeMillis() / 1000 - liveTime).coerceAtLeast(0)
    val hours = seconds / 3600
    val minutes = seconds % 3600 / 60
    return when {
        hours > 0 -> "开播${hours}小时${minutes}分"
        minutes > 0 -> "开播${minutes}分"
        else -> "刚刚开播"
    }
}

private fun formatStatCount(value: Int): String {
    return when {
        value >= 100_000_000 -> "${value / 100_000_000}亿"
        value >= 10_000 -> String.format("%.1f万", value / 10_000.0)
        value > 0 -> value.toString()
        else -> "-"
    }
}

private fun formatUpStatsText(
    followerCount: Int?,
    archiveCount: Int?
): String {
    fun formatUpStat(value: Int): String {
        return when {
            value >= 100_000_000 -> "${value / 100_000_000}亿"
            value >= 10_000 -> String.format("%.1f万", value / 10_000.0)
            else -> value.toString()
        }
    }

    val followers = followerCount?.let { formatUpStat(it) } ?: "-"
    val archives = archiveCount?.let { formatUpStat(it) } ?: "-"
    return "${followers}粉丝 · ${archives}视频"
}

@Composable
private fun ReplyInputDialog(
    title: String,
    placeholder: String,
    sending: Boolean,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {
            if (!sending) onDismiss()
        },
        title = { Text(text = title) },
        text = {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = text,
                onValueChange = { text = it },
                enabled = !sending,
                minLines = 3,
                maxLines = 6,
                placeholder = { Text(text = placeholder) }
            )
        },
        confirmButton = {
            Button(
                enabled = !sending && text.isNotBlank(),
                onClick = { onSend(text) }
            ) {
                if (sending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null
                    )
                }
                Text(
                    modifier = Modifier.padding(start = 6.dp),
                    text = "发送"
                )
            }
        },
        dismissButton = {
            TextButton(
                enabled = !sending,
                onClick = onDismiss
            ) {
                Text(text = "取消")
            }
        }
    )
}

@Composable
private fun ReplyEntryBar(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        onClick = onClick,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.AutoMirrored.Filled.Comment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun VideoComments(
    modifier: Modifier = Modifier,
    previewerState: ImagePreviewerState,
    comments: List<Comment>,
    commentSort: CommentSort,
    refreshingComments: Boolean,
    onLoadMoreComments: () -> Unit,
    onRefreshComments: () -> Unit,
    onSwitchCommentSort: (CommentSort) -> Unit,
    onShowPreviewer: (newPictures: List<Picture>, afterSetPictures: () -> Unit) -> Unit,
    onReplyVideo: () -> Unit,
    onReplyComment: (Comment) -> Unit,
    onShowReplies: (rpId: Long, repliesCount: Int) -> Unit
) {
    val listState = rememberLazyListState()
    val pullRefreshState = rememberPullRefreshState(refreshingComments, { onRefreshComments() })

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                ?: return@derivedStateOf true

            lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 10
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMoreComments()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pullRefresh(state = pullRefreshState)
        ) {
            LazyColumn(
                state = listState
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (commentSort) {
                                CommentSort.Hot -> "热门评论"
                                CommentSort.Time -> "最新评论"
                                else -> ""
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(onClick = {
                            onSwitchCommentSort(
                                when (commentSort) {
                                    CommentSort.Hot -> CommentSort.Time
                                    CommentSort.Time -> CommentSort.Hot
                                    else -> CommentSort.Hot
                                }
                            )
                        }) {
                            Text(
                                text = when (commentSort) {
                                    CommentSort.Hot -> "按热度"
                                    CommentSort.Time -> "按时间"
                                    else -> ""
                                }
                            )
                        }
                    }
                }

                itemsIndexed(items = comments) { _, comment ->
                    Box {
                        CommentItem(
                            comment = comment,
                            previewerState = previewerState,
                            onShowPreviewer = onShowPreviewer,
                            onReply = { onReplyComment(comment) },
                            onShowReply = { rpId ->
                                onShowReplies(rpId, comment.repliesCount)
                            }
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            PullRefreshIndicator(
                refreshingComments,
                pullRefreshState,
                Modifier.align(Alignment.TopCenter)
            )
        }
        ReplyEntryBar(
            text = if (Prefs.isLogin) "发一条友善的评论" else "登录后发表评论",
            enabled = Prefs.isLogin,
            onClick = onReplyVideo
        )
    }
}

@Preview
@Composable
private fun VideoPlayerInfoPreview() {
    BVMobileTheme {
        Surface {
            VideoPlayerInfo(
                modifier = Modifier.padding(24.dp),
                upAvatar = "https://i0.hdslb.com/bfs/article/b6b843d84b84a3ba5526b09ebf538cd4b4c8c3f3.jpg@450w_450h_progressive.webp",
                upName = "bishi",
                upFollowerCount = 1400000000,
                upArchiveCount = 233,
                title = "This is the video title... repeat, this is the video title.",
                description = "descriptions....descriptions....descriptions....descriptions....descriptions....descriptions....descriptions....descriptions....descriptions....",
                playCount = 2434,
                danmakuCount = 14,
                date = "2023-5-22 23:17",
                avid = 170001,
            )
        }
    }
}
