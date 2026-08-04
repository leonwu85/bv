package dev.aaa1115910.bv.mobile.screen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.hardware.SensorManager
import android.net.Uri
import android.provider.OpenableColumns
import android.view.OrientationEventListener
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.material.icons.rounded.PictureInPictureAlt
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.geetest.sdk.GT3ConfigBean
import com.geetest.sdk.GT3ErrorBean
import com.geetest.sdk.GT3GeetestUtils
import com.geetest.sdk.GT3Listener
import dev.aaa1115910.bv.viewmodel.login.GeetestResult
import kotlinx.serialization.json.Json
import org.json.JSONObject
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import dev.aaa1115910.biliapi.entity.user.DynamicEmoteDraft
import dev.aaa1115910.biliapi.entity.user.DynamicImageDraft
import dev.aaa1115910.biliapi.entity.user.DynamicMentionDraft
import dev.aaa1115910.biliapi.entity.FavoriteFolderMetadata
import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.biliapi.entity.reply.Comment
import dev.aaa1115910.biliapi.entity.reply.CommentSort
import dev.aaa1115910.biliapi.entity.reply.CommentVote
import dev.aaa1115910.biliapi.entity.video.VideoDetail
import dev.aaa1115910.biliapi.entity.video.VideoPage
import dev.aaa1115910.biliapi.entity.video.season.Episode
import dev.aaa1115910.biliapi.http.entity.live.LiveEmotePackage
import dev.aaa1115910.biliapi.http.entity.live.LiveEmoticon
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.mobile.activities.OfflineCacheActivity
import dev.aaa1115910.bv.mobile.activities.UserSpaceActivity
import dev.aaa1115910.bv.mobile.activities.VideoPlayerActivity
import dev.aaa1115910.bv.mobile.component.emote.EmoteInputToken
import dev.aaa1115910.bv.mobile.component.emote.EmoteTextSelection
import dev.aaa1115910.bv.mobile.component.emote.EmotePanel
import dev.aaa1115910.bv.mobile.component.emote.EmoteTextEditor
import dev.aaa1115910.bv.mobile.component.emote.emoteDisplayName
import dev.aaa1115910.bv.mobile.component.player.VideoPlayerPages
import dev.aaa1115910.bv.mobile.component.reply.CommentItem
import dev.aaa1115910.bv.mobile.component.reply.CommentVoteCard
import dev.aaa1115910.bv.mobile.component.reply.ReplySheetScaffold
import dev.aaa1115910.bv.mobile.dlna.DlnaCastDialog
import dev.aaa1115910.bv.mobile.settings.MobilePrefs
import dev.aaa1115910.bv.mobile.component.videocard.RelatedVideoItem
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme
import dev.aaa1115910.bv.mobile.R as MobileR
import dev.aaa1115910.bv.mobile.util.saveImageToGallery
import dev.aaa1115910.bv.offline.OfflineVideoCacheStatus
import dev.aaa1115910.bv.offline.OfflineVideoCacheTaskState
import dev.aaa1115910.bv.offline.OfflineVideoCacheTarget
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
import dev.aaa1115910.bv.util.formatHourMinSec
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class ReplyDraftTarget(
    val title: String,
    val placeholder: String,
    val root: Long? = null,
    val parent: Long? = null
)

private data class ReplyLocalImage(
    val uri: Uri,
    val fileName: String
)

private data class ReplyMentionToken(
    val marker: String,
    val name: String,
    val uid: Long,
    val preferredStart: Int
)

private data class ReplySendDraft(
    val message: String,
    val images: List<ReplyLocalImage>,
    val atNameToMid: Map<String, Long>,
    val syncToDynamic: Boolean
)

private enum class ReplyInputPanel {
    Emoji,
    Mention,
    More
}

private val DanmakuActionIconFontFamily = FontFamily(Font(MobileR.font.danmaku_action_icon))
private const val DanmakuOffIcon = "\uE802"
private const val DanmakuOnIcon = "\uE803"
private const val AUTO_ROTATE_SETTLE_MILLIS = 350L
private const val AUTO_ROTATE_HYSTERESIS_DEGREES = 10
private const val MIN_AUTO_ROTATE_REARM_ANGLE = 10

internal fun landscapeTiltDegrees(orientationDegrees: Int): Int? {
    if (orientationDegrees !in 0 until 360) return null
    val halfTurnDegrees = orientationDegrees % 180
    return minOf(halfTurnDegrees, 180 - halfTurnDegrees)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    playerViewModel: VideoPlayerV3ViewModel,
    commentVideModel: CommentViewModel,
    seasonVideModel: SeasonViewModel,
    videoDetailViewModel: VideoDetailViewModel,
    windowSizeClass: WindowSizeClass,
    isInPictureInPictureMode: Boolean = false,
    pictureInPictureSupported: Boolean = false,
    onEnterPictureInPicture: () -> Unit = {},
    onPlayPgcEpisode: (Episode) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger("VideoPlayerScreen")
    val playerSettings = PlayerSettingsProvider.current
    val useDarkSystemBarIcons = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val useLightSystemBarIcons = playerViewModel.isLive || !useDarkSystemBarIcons

    var isVideoFullscreen by rememberSaveable { mutableStateOf(false) }
    var autoRotateSuppressed by remember { mutableStateOf(false) }
    val forcePortrait =
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact || windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact

    val initialAutoRotateVideo = remember { MobilePrefs.autoRotateVideo }
    val initialAutoRotateVideoAngle = remember { MobilePrefs.autoRotateVideoAngle }
    val autoRotateVideo by MobilePrefs.autoRotateVideoFlow.collectAsState(
        initial = initialAutoRotateVideo
    )
    val autoRotateVideoAngle by MobilePrefs.autoRotateVideoAngleFlow.collectAsState(
        initial = initialAutoRotateVideoAngle
    )
    val currentAutoRotateSuppressed by rememberUpdatedState(autoRotateSuppressed)
    val updateVideoFullscreen by rememberUpdatedState<(Boolean) -> Unit> { fullscreen ->
        isVideoFullscreen = fullscreen
    }
    val updateAutoRotateSuppressed by rememberUpdatedState<(Boolean) -> Unit> { suppressed ->
        autoRotateSuppressed = suppressed
    }

    DisposableEffect(
        context,
        isVideoFullscreen,
        isInPictureInPictureMode,
        autoRotateVideo,
        autoRotateVideoAngle
    ) {
        // 小窗阶段即开始监听，不依赖视频详情或播放地址是否已经加载完成。
        if (isVideoFullscreen || isInPictureInPictureMode || !autoRotateVideo) {
            return@DisposableEffect onDispose {}
        }

        val enterThreshold = autoRotateVideoAngle.toDouble()
        val rearmThreshold = (autoRotateVideoAngle - AUTO_ROTATE_HYSTERESIS_DEGREES)
            .coerceAtLeast(MIN_AUTO_ROTATE_REARM_ANGLE)
            .toDouble()
        var pendingEnterFullscreenJob: Job? = null

        fun cancelPendingEnterFullscreen() {
            pendingEnterFullscreenJob?.cancel()
            pendingEnterFullscreenJob = null
        }

        val orientationListener = object : OrientationEventListener(
            context,
            SensorManager.SENSOR_DELAY_UI
        ) {
            override fun onOrientationChanged(orientation: Int) {
                val tiltDegrees = landscapeTiltDegrees(orientation)?.toDouble()
                if (tiltDegrees == null) {
                    cancelPendingEnterFullscreen()
                    return
                }
                if (currentAutoRotateSuppressed) {
                    if (tiltDegrees <= rearmThreshold) {
                        updateAutoRotateSuppressed(false)
                    }
                    cancelPendingEnterFullscreen()
                    return
                }

                if (tiltDegrees < enterThreshold) {
                    cancelPendingEnterFullscreen()
                    return
                }

                if (pendingEnterFullscreenJob?.isActive == true) {
                    return
                }

                pendingEnterFullscreenJob = scope.launch {
                    delay(AUTO_ROTATE_SETTLE_MILLIS)
                    pendingEnterFullscreenJob = null
                    logger.info {
                        "Auto enter fullscreen: orientation=$orientation, " +
                            "tilt=$tiltDegrees, threshold=$enterThreshold"
                    }
                    updateVideoFullscreen(true)
                }
            }
        }

        if (!orientationListener.canDetectOrientation()) {
            return@DisposableEffect onDispose {}
        }

        orientationListener.enable()
        onDispose {
            cancelPendingEnterFullscreen()
            orientationListener.disable()
        }
    }

    // 风控 Geetest（官方 Sensebot SDK）
    var gt3GeetestUtils: GT3GeetestUtils? by remember { mutableStateOf(null) }
    val gt3ConfigBean by remember { mutableStateOf(GT3ConfigBean()) }

    DisposableEffect(Unit) {
        gt3GeetestUtils = GT3GeetestUtils(context)
        gt3ConfigBean.apply {
            pattern = 1
            isCanceledOnTouchOutside = false
            lang = null
            timeout = 10000
            webviewTimeout = 10000
            corners = 24
            listener = object : GT3Listener() {
                override fun onReceiveCaptchaCode(p0: Int) {}
                override fun onStatistics(p0: String?) {}
                override fun onSuccess(p0: String?) {}
                override fun onButtonClick() {}

                override fun onClosed(p0: Int) {
                    playerViewModel.onGeetestCancelled()
                }

                override fun onFailed(p0: GT3ErrorBean?) {
                    playerViewModel.onGeetestCancelled()
                }

                override fun onDialogResult(result: String) {
                    runCatching {
                        val geetestResult = Json.decodeFromString<GeetestResult>(result)
                        gt3GeetestUtils?.showSuccessDialog()
                        playerViewModel.onGeetestResult(
                            challenge = geetestResult.geetestChallenge,
                            validate = geetestResult.geetestValidate,
                            seccode = geetestResult.geetestSeccode
                        )
                    }.onFailure {
                        gt3GeetestUtils?.showFailedDialog()
                        playerViewModel.onGeetestCancelled()
                    }
                }
            }
        }
        gt3GeetestUtils!!.init(gt3ConfigBean)

        onDispose {
            gt3GeetestUtils?.destory()
        }
    }

    LaunchedEffect(playerViewModel.showGeetestDialog) {
        if (playerViewModel.showGeetestDialog) {
            gt3GeetestUtils?.startCustomFlow()
            gt3ConfigBean.api1Json = JSONObject().apply {
                put("success", 1)
                put("gt", playerViewModel.geetestGt)
                put("challenge", playerViewModel.geetestChallenge)
            }
            gt3GeetestUtils?.getGeetest()
        }
    }

    val pictures = remember { mutableStateListOf<Picture>() }
    val previewerState = rememberPreviewerState(
        verticalDragType = VerticalDragType.UpAndDown,
        pageCount = { pictures.size },
        getKey = { pictures[it].key }
    )
    val replyBottomSheetState = rememberBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        enabledValues = setOf(
            SheetValue.PartiallyExpanded,
            SheetValue.Expanded,
        ),
    )
    val replySheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = replyBottomSheetState
    )
    var replyDraftTarget by remember { mutableStateOf<ReplyDraftTarget?>(null) }
    var liveDanmakuDraft by remember { mutableStateOf("") }
    var liveDanmakuSelection by remember { mutableStateOf(EmoteTextSelection.Zero) }
    val liveDanmakuEmoteTokens = remember { mutableStateListOf<EmoteInputToken>() }
    var showLiveDanmakuDialog by remember { mutableStateOf(false) }
    var videoDanmakuDraft by rememberSaveable { mutableStateOf("") }
    var videoDanmakuMode by rememberSaveable { mutableIntStateOf(1) }
    var videoDanmakuFontSize by rememberSaveable { mutableIntStateOf(25) }
    var videoDanmakuColor by rememberSaveable { mutableIntStateOf(0xFFFFFF) }
    var showVideoDanmakuSheet by remember { mutableStateOf(false) }
    var savingPreviewImage by remember { mutableStateOf(false) }
    var savingCoverImage by remember { mutableStateOf(false) }
    var showOfflineCacheDialog by remember { mutableStateOf(false) }
    var showDlnaDialog by remember { mutableStateOf(false) }
    var offlineCacheDialogLoading by remember { mutableStateOf(false) }
    val offlineCacheState = playerViewModel.offlineCacheState

    LaunchedEffect(isInPictureInPictureMode) {
        if (isInPictureInPictureMode) {
            showOfflineCacheDialog = false
            showDlnaDialog = false
            showVideoDanmakuSheet = false
            showLiveDanmakuDialog = false
            replyDraftTarget = null
            if (previewerState.canClose) {
                previewerState.closeTransform()
            }
        }
    }

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
    val launchOfflineCache: (OfflineVideoCacheTarget, Resolution) -> Unit = launchOfflineCache@{ target, quality ->
        if (offlineCacheDialogLoading) return@launchOfflineCache
        offlineCacheDialogLoading = true
        scope.launch(Dispatchers.IO) {
            val result = playerViewModel.cacheVideoTarget(target, quality)
            withContext(Dispatchers.Main) {
                offlineCacheDialogLoading = false
                result
                    .onSuccess { it.toast(context) }
                    .onFailure { (it.localizedMessage ?: "操作失败").toast(context) }
            }
        }
    }
    val launchOfflineCacheAll: (List<OfflineVideoCacheTarget>, Resolution) -> Unit = { targets, quality ->
        val result = playerViewModel.cacheVideoTargets(targets, quality)
        result
            .onSuccess {
                showOfflineCacheDialog = false
                it.toast(context)
            }
            .onFailure { (it.localizedMessage ?: "操作失败").toast(context) }
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
    val sendVideoDanmaku: () -> Unit = {
        val message = videoDanmakuDraft
        scope.launch(Dispatchers.IO) {
            val result = playerViewModel.sendVideoDanmaku(
                message = message,
                mode = videoDanmakuMode,
                fontSize = videoDanmakuFontSize,
                color = videoDanmakuColor
            )
            withContext(Dispatchers.Main) {
                result
                    .onSuccess {
                        it.toast(context)
                        videoDanmakuDraft = ""
                        showVideoDanmakuSheet = false
                    }
                    .onFailure { (it.localizedMessage ?: "发送失败").toast(context) }
            }
        }
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
                        liveDanmakuSelection = EmoteTextSelection.Zero
                        liveDanmakuEmoteTokens.clear()
                        showLiveDanmakuDialog = false
                    }
                    .onFailure { (it.localizedMessage ?: "发送失败").toast(context) }
            }
        }
    }
    val sendLiveEmoticon: (LiveEmoticon) -> Unit = { emoticon ->
        scope.launch(Dispatchers.IO) {
            val result = playerViewModel.sendLiveEmoticon(emoticon.emoticonUnique)
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
        if (isVideoFullscreen || isInPictureInPictureMode) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }

        // 设置系统栏外观
        if (!isVideoFullscreen && !isInPictureInPictureMode) {
            insetsController.isAppearanceLightStatusBars = !useLightSystemBarIcons
            insetsController.isAppearanceLightNavigationBars = !useLightSystemBarIcons
        }

        // 设置屏幕方向
        if (isInPictureInPictureMode) {
            context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else if (forcePortrait) {
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

    BackHandler(isVideoFullscreen && !isInPictureInPictureMode) {
        autoRotateSuppressed = true
        isVideoFullscreen = false
    }

    OfflineCacheBottomSheet(
        show = showOfflineCacheDialog,
        videoDetail = videoDetailViewModel.videoDetail,
        currentCid = playerViewModel.currentCid,
        currentQuality = playerViewModel.currentQuality,
        loading = offlineCacheDialogLoading,
        cacheStateOf = playerViewModel::offlineCacheStateOf,
        onDismiss = {
            if (!offlineCacheDialogLoading) showOfflineCacheDialog = false
        },
        onCacheOne = launchOfflineCache,
        onCacheAll = launchOfflineCacheAll,
        onOpenCachePage = {
            showOfflineCacheDialog = false
            context.startActivity(Intent(context, OfflineCacheActivity::class.java))
        }
    )

    if (showDlnaDialog) {
        DlnaCastDialog(
            sourceProvider = playerViewModel::getDlnaMediaSource,
            onCastStarted = { device, _ ->
                playerViewModel.videoPlayer?.pause()
                "已开始投屏到 ${device.name}".toast(context)
            },
            onDismiss = { showDlnaDialog = false }
        )
    }

    Scaffold(
        containerColor = if (isVideoFullscreen || isInPictureInPictureMode) Color.Black else Color.Transparent
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (playerViewModel.isLive && !isVideoFullscreen && !isInPictureInPictureMode) {
                LiveRoomScreenBackground(backgroundUrl = playerViewModel.liveBackground)
            }
            Row(
                modifier = Modifier
                    .ifElse(
                        !isVideoFullscreen && !isInPictureInPictureMode,
                        Modifier.padding(top = innerPadding.calculateTopPadding())
                    )
                //.padding(top = innerPadding.calculateTopPadding())
            ) {
            val leftPartWidth by animateFloatAsState(
                targetValue = if (
                    windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded &&
                    !isVideoFullscreen &&
                    !isInPictureInPictureMode
                ) 0.6f else 1f,
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
                            cover = playerViewModel.cover,
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
                            autoPlay = playerSettings.autoPlay,
                            currentAudio = playerViewModel.currentAudio,
                            currentDanmakuEnabled = playerViewModel.currentDanmakuEnabled,
                            currentDanmakuEnabledList = playerViewModel.currentDanmakuTypes,
                            currentDanmakuScale = playerViewModel.currentDanmakuScale,
                            currentDanmakuOpacity = playerViewModel.currentDanmakuOpacity,
                            currentDanmakuArea = playerViewModel.currentDanmakuArea,
                            currentDanmakuMask = playerViewModel.currentDanmakuMask,
                            currentDanmakuSpeedMode = playerViewModel.currentDanmakuSpeedMode,
                            currentDanmakuPresentationSpeed = playerViewModel.currentDanmakuPresentationSpeed,
                            currentSubtitleId = playerViewModel.currentSubtitleId,
                            currentSubtitleData = playerViewModel.currentSubtitleData,
                            currentSubtitleFontSize = playerViewModel.currentSubtitleFontSize,
                            currentSubtitleBackgroundOpacity = playerViewModel.currentSubtitleBackgroundOpacity,
                            currentSubtitleBottomPadding = playerViewModel.currentSubtitleBottomPadding,
                            currentPlayMode = playerViewModel.currentPlayMode,
                            incognitoMode = playerSettings.incognitoMode,
                            defaultStartPosition = playerSettings.playerDefaultStartPosition.toPlayerType(),
                            viewPoints = playerViewModel.viewPoints,
                            isLive = playerViewModel.isLive,
                            availableLiveQualities = playerViewModel.availableLiveQualities,
                            currentLiveQn = playerViewModel.currentLiveQn,
                            currentLiveQualityDescription = playerViewModel.currentLiveQualityDescription,
                            currentLiveCodec = playerViewModel.currentLiveCodec,
                            availableLiveLines = playerViewModel.availableLiveLines.toList(),
                            currentLiveLineIndex = playerViewModel.currentLiveLineIndex
                        ),
                        LocalVideoPlayerDanmakuMasksData provides VideoPlayerDanmakuMasksData(
                            danmakuMasks = playerViewModel.danmakuMasks,
                        ),
                        LocalVideoPlayerVideoShotData provides VideoPlayerVideoShotData(
                            videoShot = playerViewModel.videoShot,
                        ),
                    ) {
                        BvPlayer(
                            modifier = if (isVideoFullscreen || isInPictureInPictureMode) Modifier
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
                            isFullScreen = isVideoFullscreen || isInPictureInPictureMode,
                            controlsEnabled = !isInPictureInPictureMode,
                            videoPlayer = playerViewModel.videoPlayer!!,
                            danmakuPlayer = playerViewModel.danmakuPlayer,
                            onClearBackToHistoryData = { playerViewModel.lastPlayed = 0 },
                            onReloadDanmakuAfterSeek = playerViewModel::reloadDanmakuAfterSeek,
                            onRequestManualPlayback = playerViewModel::requestManualVodPlayback,
                            onEnterFullScreen = {
                                autoRotateSuppressed = false
                                isVideoFullscreen = true
                            },
                            onExitFullScreen = {
                                autoRotateSuppressed = true
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
                            onChangeLiveQuality = { qn ->
                                playerViewModel.changeLiveQuality(qn)
                            },
                            onChangeLiveCodec = { codec ->
                                playerViewModel.changeLiveCodec(codec)
                            },
                            onChangeLiveLine = { lineIndex ->
                                playerViewModel.changeLiveLine(lineIndex)
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
                            onDanmakuSpeedModeChange = { mode ->
                                playerViewModel.currentDanmakuSpeedMode = mode
                                playerSettings.defaultDanmakuSpeedModeMutable = mode
                            },
                            onDanmakuPresentationSpeedChange = { speed ->
                                playerViewModel.currentDanmakuPresentationSpeed = speed
                                playerSettings.defaultDanmakuPresentationSpeedMutable = speed
                            },
                            onDanmakuMergeChange = { enabled ->
                                playerSettings.defaultDanmakuMergeEnabledMutable = enabled
                                playerViewModel.updateDanmakuMergeEnabled(enabled)
                            },
                            onDanmakuFilterLevelChange = { filterLevel ->
                                if (playerViewModel.isLive) {
                                    val sanitizedLevel = filterLevel.coerceIn(0, 60)
                                    playerViewModel.currentLiveDanmakuFilterLevel = sanitizedLevel
                                    playerSettings.defaultLiveDanmakuFilterLevelMutable = sanitizedLevel
                                } else {
                                    val sanitizedLevel = filterLevel.coerceIn(1, 10)
                                    playerViewModel.currentDanmakuFilterLevel = sanitizedLevel
                                    playerSettings.defaultDanmakuFilterLevelMutable = sanitizedLevel
                                }
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
                            },
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
                            }
                        )
                    }
                }
                if (!isInPictureInPictureMode && playerViewModel.isLive) {
                    if (windowSizeClass.widthSizeClass != WindowWidthSizeClass.Expanded) {
                        LiveRoomPanel(
                            modifier = Modifier.fillMaxSize(),
                            playerViewModel = playerViewModel,
                            showDanmakuInput = showLiveDanmakuDialog,
                            danmakuInputValue = liveDanmakuDraft,
                            danmakuInputSelection = liveDanmakuSelection,
                            danmakuInputEmoteTokens = liveDanmakuEmoteTokens,
                            sendingDanmaku = playerViewModel.sendingLiveDanmaku,
                            emotePackages = playerViewModel.liveEmotePackages,
                            loadingEmoticons = playerViewModel.loadingLiveEmoticons,
                            emoticonError = playerViewModel.liveEmoticonError,
                            onDanmakuInputValueChange = { value, selection ->
                                liveDanmakuDraft = value
                                liveDanmakuSelection = selection
                            },
                            onLoadEmoticons = playerViewModel::loadLiveEmoticons,
                            onTextEmoticonClick = { emoticon ->
                                val insertion = emoticon.messageText
                                val start = liveDanmakuSelection.start.coerceIn(0, liveDanmakuDraft.length)
                                val end = liveDanmakuSelection.end.coerceIn(0, liveDanmakuDraft.length)
                                val rangeStart = minOf(start, end)
                                val rangeEnd = maxOf(start, end)
                                liveDanmakuDraft = buildString {
                                    append(liveDanmakuDraft.substring(0, rangeStart))
                                    append(insertion)
                                    append(liveDanmakuDraft.substring(rangeEnd))
                                }
                                liveDanmakuSelection = EmoteTextSelection.collapsed(rangeStart + insertion.length)
                                liveDanmakuEmoteTokens.add(
                                    EmoteInputToken(
                                        marker = insertion,
                                        preferredStart = rangeStart,
                                        emoteUrl = emoticon.url,
                                        emoteName = emoticon.displayName
                                    )
                                )
                            },
                            onLiveEmoticonClick = sendLiveEmoticon,
                            onDismissDanmakuInput = { showLiveDanmakuDialog = false },
                            onSendDanmaku = sendLiveDanmaku,
                            onSendDanmakuClick = { showLiveDanmakuDialog = true },
                            onLikeClick = likeLiveRoom,
                            onRefresh = { playerViewModel.loadLiveStreamWithQuality(playerViewModel.liveRoomId, playerViewModel.currentLiveQn) },
                            onQualitySelected = playerViewModel::changeLiveQuality,
                            onCodecSelected = playerViewModel::changeLiveCodec,
                            onLineSelected = playerViewModel::changeLiveLine,
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
                                    onLineSelected = playerViewModel::changeLiveLine,
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
                } else if (!isInPictureInPictureMode) {
                    val commentTabTitle = videoDetailViewModel.videoDetail?.stat?.reply
                        ?.let { "评论 ${it.coerceAtLeast(0)}" }
                        ?: "评论"
                    val titles = listOf("简介", commentTabTitle)
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
                            VideoDetailTabsWithDanmakuControls(
                                titles = titles,
                                selectedTabIndex = pagerState.currentPage,
                                danmakuEnabled = playerViewModel.currentDanmakuEnabled,
                                onTabClick = { index ->
                                    scope.launch { pagerState.scrollToPage(index) }
                                },
                                onSendDanmakuClick = { showVideoDanmakuSheet = true },
                                onToggleDanmaku = {
                                    val enabled = !playerViewModel.currentDanmakuEnabled
                                    playerViewModel.currentDanmakuEnabled = enabled
                                    playerSettings.defaultDanmakuEnabledMutable = enabled
                                }
                            )
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
                                                        ?: playerViewModel.upFace,
                                                    upName = videoDetailViewModel.videoDetail?.author?.name
                                                        ?: playerViewModel.upName,
                                                    upMid = videoDetailViewModel.videoDetail?.author?.mid
                                                        ?.takeIf { it > 0L }
                                                        ?: playerViewModel.upId,
                                                    upFollowerCount = videoDetailViewModel.upOwnerStats?.followerCount,
                                                    upArchiveCount = videoDetailViewModel.upOwnerStats?.archiveCount,
                                                    upFollowing = videoDetailViewModel.upFollowing,
                                                    upRelationLoading = videoDetailViewModel.upRelationLoading,
                                                    title = videoDetailViewModel.videoDetail?.title
                                                        ?: playerViewModel.title,
                                                    description = videoDetailViewModel.videoDetail?.description
                                                        ?: "",
                                                    playCount = videoDetailViewModel.videoDetail?.stat?.view
                                                        ?: playerViewModel.play,
                                                    danmakuCount = videoDetailViewModel.videoDetail?.stat?.danmaku
                                                        ?: playerViewModel.danmaku,
                                                    likeCount = videoDetailViewModel.videoDetail?.stat?.like
                                                        ?: 0,
                                                    coinCount = videoDetailViewModel.videoDetail?.stat?.coin
                                                        ?: 0,
                                                    favoriteCount = videoDetailViewModel.videoDetail?.stat?.favorite
                                                        ?: 0,
                                                    shareCount = videoDetailViewModel.videoDetail?.stat?.share
                                                        ?: 0,
                                                    date = videoDetailViewModel.videoDetail?.publishDate
                                                        ?.takeIf { it.time > 0L }
                                                        ?.formatPubTimeString(context)
                                                        ?: playerViewModel.pubTime,
                                                    avid = videoDetailViewModel.videoDetail?.aid
                                                        ?: playerViewModel.currentAid,
                                                    liked = videoDetailViewModel.videoDetail?.userActions?.like == true,
                                                    disliked = videoDetailViewModel.videoDetail?.userActions?.dislike == true,
                                                    coined = videoDetailViewModel.videoDetail?.userActions?.coin == true,
                                                    favorited = videoDetailViewModel.videoDetail?.userActions?.favorite == true,
                                                    inToView = videoDetailViewModel.inToView,
                                                    favoriteFolders = videoDetailViewModel.favoriteFolders,
                                                    favoriteFolderIds = videoDetailViewModel.favoriteFolderIds,
                                                    userActionUpdating = videoDetailViewModel.userActionUpdating,
                                                    savingCover = savingCoverImage,
                                                    offlineCacheState = offlineCacheState,
                                                    offlinePlayback = playerViewModel.currentPlaybackOffline,
                                                    dlnaAvailable = !playerViewModel.currentPlaybackOffline,
                                                    pictureInPictureSupported = pictureInPictureSupported,
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
                                                    onCast = { showDlnaDialog = true },
                                                    onEnterPictureInPicture = onEnterPictureInPicture,
                                                    onSaveCover = saveCover,
                                                    onCacheVideo = { showOfflineCacheDialog = true },
                                                    onPauseCache = { launchVideoAction { playerViewModel.pauseOfflineCache() } },
                                                    onResumeCache = { launchVideoAction { playerViewModel.resumeOfflineCache() } },
                                                    onClearCacheTask = { launchVideoAction { playerViewModel.clearOfflineCacheTask() } },
                                                    onPlayCache = { launchVideoAction { playerViewModel.playOfflineCache() } },
                                                    onToggleUpFollow = {
                                                        launchVideoAction { videoDetailViewModel.toggleUpFollow() }
                                                    },
                                                    onUpClick = { mid, name ->
                                                        UserSpaceActivity.actionStart(context, mid, name)
                                                    }
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
                                                    pgcEpisodes = if (playerViewModel.fromSeason) {
                                                        seasonVideModel.seasonData?.episodes.orEmpty()
                                                    } else {
                                                        emptyList()
                                                    },
                                                    pgcSections = if (playerViewModel.fromSeason) {
                                                        seasonVideModel.seasonData?.sections.orEmpty()
                                                    } else {
                                                        emptyList()
                                                    },
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
                                                    onClickPgcEpisode = onPlayPgcEpisode,
                                                    onClickEpisode = { sectionIndex, episode ->
                                                        videoDetailViewModel.updateUgcSeasonSectionVideoList(
                                                            sectionIndex
                                                        )
                                                        playerViewModel.loadPlayUrl(
                                                            avid = episode.aid,
                                                            cid = episode.pages.firstOrNull()?.cid ?: episode.cid,
                                                            epid = episode.epid,
                                                            continuePlayNext = true
                                                        )
                                                    },
                                                    onClickEpisodePage = { sectionIndex, episode, page ->
                                                        videoDetailViewModel.updateUgcSeasonSectionVideoList(
                                                            sectionIndex
                                                        )
                                                        playerViewModel.loadPlayUrl(
                                                            avid = episode.aid,
                                                            cid = page.cid,
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
                                                            relatedVideo = relatedVideo
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
                                            vote = commentVideModel.commentVote,
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
                            VideoDetailDanmakuActionRow(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                danmakuEnabled = playerViewModel.currentDanmakuEnabled,
                                onSendDanmakuClick = { showVideoDanmakuSheet = true },
                                onToggleDanmaku = {
                                    val enabled = !playerViewModel.currentDanmakuEnabled
                                    playerViewModel.currentDanmakuEnabled = enabled
                                    playerSettings.defaultDanmakuEnabledMutable = enabled
                                }
                            )
                        }
                        item {
                            VideoPlayerInfo(
                                modifier = Modifier.padding(12.dp),
                                upAvatar = videoDetailViewModel.videoDetail?.author?.face
                                    ?: playerViewModel.upFace,
                                upName = videoDetailViewModel.videoDetail?.author?.name ?: playerViewModel.upName,
                                upMid = videoDetailViewModel.videoDetail?.author?.mid
                                    ?.takeIf { it > 0L }
                                    ?: playerViewModel.upId,
                                upFollowerCount = videoDetailViewModel.upOwnerStats?.followerCount,
                                upArchiveCount = videoDetailViewModel.upOwnerStats?.archiveCount,
                                upFollowing = videoDetailViewModel.upFollowing,
                                upRelationLoading = videoDetailViewModel.upRelationLoading,
                                title = videoDetailViewModel.videoDetail?.title ?: playerViewModel.title,
                                description = videoDetailViewModel.videoDetail?.description
                                    ?: "",
                                playCount = videoDetailViewModel.videoDetail?.stat?.view ?: playerViewModel.play,
                                danmakuCount = videoDetailViewModel.videoDetail?.stat?.danmaku
                                    ?: playerViewModel.danmaku,
                                likeCount = videoDetailViewModel.videoDetail?.stat?.like ?: 0,
                                coinCount = videoDetailViewModel.videoDetail?.stat?.coin ?: 0,
                                favoriteCount = videoDetailViewModel.videoDetail?.stat?.favorite ?: 0,
                                shareCount = videoDetailViewModel.videoDetail?.stat?.share ?: 0,
                                date = videoDetailViewModel.videoDetail?.publishDate
                                    ?.takeIf { it.time > 0L }
                                    ?.formatPubTimeString(context)
                                    ?: playerViewModel.pubTime,
                                avid = videoDetailViewModel.videoDetail?.aid ?: playerViewModel.currentAid,
                                liked = videoDetailViewModel.videoDetail?.userActions?.like == true,
                                disliked = videoDetailViewModel.videoDetail?.userActions?.dislike == true,
                                coined = videoDetailViewModel.videoDetail?.userActions?.coin == true,
                                favorited = videoDetailViewModel.videoDetail?.userActions?.favorite == true,
                                inToView = videoDetailViewModel.inToView,
                                favoriteFolders = videoDetailViewModel.favoriteFolders,
                                favoriteFolderIds = videoDetailViewModel.favoriteFolderIds,
                                userActionUpdating = videoDetailViewModel.userActionUpdating,
                                savingCover = savingCoverImage,
                                offlineCacheState = offlineCacheState,
                                offlinePlayback = playerViewModel.currentPlaybackOffline,
                                dlnaAvailable = !playerViewModel.currentPlaybackOffline,
                                pictureInPictureSupported = pictureInPictureSupported,
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
                                onCast = { showDlnaDialog = true },
                                onEnterPictureInPicture = onEnterPictureInPicture,
                                onSaveCover = saveCover,
                                onCacheVideo = { showOfflineCacheDialog = true },
                                onPauseCache = { launchVideoAction { playerViewModel.pauseOfflineCache() } },
                                onResumeCache = { launchVideoAction { playerViewModel.resumeOfflineCache() } },
                                onClearCacheTask = { launchVideoAction { playerViewModel.clearOfflineCacheTask() } },
                                onPlayCache = { launchVideoAction { playerViewModel.playOfflineCache() } },
                                onToggleUpFollow = {
                                    launchVideoAction { videoDetailViewModel.toggleUpFollow() }
                                },
                                onUpClick = { mid, name ->
                                    UserSpaceActivity.actionStart(context, mid, name)
                                },
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
                                pgcEpisodes = if (playerViewModel.fromSeason) {
                                    seasonVideModel.seasonData?.episodes.orEmpty()
                                } else {
                                    emptyList()
                                },
                                pgcSections = if (playerViewModel.fromSeason) {
                                    seasonVideModel.seasonData?.sections.orEmpty()
                                } else {
                                    emptyList()
                                },
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
                                onClickPgcEpisode = onPlayPgcEpisode,
                                onClickEpisode = { sectionIndex, episode ->
                                    videoDetailViewModel.updateUgcSeasonSectionVideoList(
                                        sectionIndex
                                    )
                                    playerViewModel.loadPlayUrl(
                                        avid = episode.aid,
                                        cid = episode.pages.firstOrNull()?.cid ?: episode.cid,
                                        epid = episode.epid,
                                        continuePlayNext = true
                                    )
                                },
                                onClickEpisodePage = { sectionIndex, episode, page ->
                                    videoDetailViewModel.updateUgcSeasonSectionVideoList(
                                        sectionIndex
                                    )
                                    playerViewModel.loadPlayUrl(
                                        avid = episode.aid,
                                        cid = page.cid,
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
                                        relatedVideo = relatedVideo
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
            if (
                windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded &&
                !isInPictureInPictureMode
            ) {
                if (playerViewModel.isLive) {
                    LiveRoomPanel(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .fillMaxHeight(),
                        playerViewModel = playerViewModel,
                        showDanmakuInput = showLiveDanmakuDialog,
                        danmakuInputValue = liveDanmakuDraft,
                        danmakuInputSelection = liveDanmakuSelection,
                        danmakuInputEmoteTokens = liveDanmakuEmoteTokens,
                        sendingDanmaku = playerViewModel.sendingLiveDanmaku,
                        emotePackages = playerViewModel.liveEmotePackages,
                        loadingEmoticons = playerViewModel.loadingLiveEmoticons,
                        emoticonError = playerViewModel.liveEmoticonError,
                        onDanmakuInputValueChange = { value, selection ->
                            liveDanmakuDraft = value
                            liveDanmakuSelection = selection
                        },
                        onLoadEmoticons = playerViewModel::loadLiveEmoticons,
                        onTextEmoticonClick = { emoticon ->
                            val insertion = emoticon.messageText
                            val start = liveDanmakuSelection.start.coerceIn(0, liveDanmakuDraft.length)
                            val end = liveDanmakuSelection.end.coerceIn(0, liveDanmakuDraft.length)
                            val rangeStart = minOf(start, end)
                            val rangeEnd = maxOf(start, end)
                            liveDanmakuDraft = buildString {
                                append(liveDanmakuDraft.substring(0, rangeStart))
                                append(insertion)
                                append(liveDanmakuDraft.substring(rangeEnd))
                            }
                            liveDanmakuSelection = EmoteTextSelection.collapsed(rangeStart + insertion.length)
                            liveDanmakuEmoteTokens.add(
                                EmoteInputToken(
                                    marker = insertion,
                                    preferredStart = rangeStart,
                                    emoteUrl = emoticon.url,
                                    emoteName = emoticon.displayName
                                )
                            )
                        },
                        onLiveEmoticonClick = sendLiveEmoticon,
                        onDismissDanmakuInput = { showLiveDanmakuDialog = false },
                        onSendDanmaku = sendLiveDanmaku,
                        onSendDanmakuClick = { showLiveDanmakuDialog = true },
                        onLikeClick = likeLiveRoom,
                        onRefresh = { playerViewModel.loadLiveStreamWithQuality(playerViewModel.liveRoomId, playerViewModel.currentLiveQn) },
                        onQualitySelected = playerViewModel::changeLiveQuality,
                        onCodecSelected = playerViewModel::changeLiveCodec,
                        onLineSelected = playerViewModel::changeLiveLine,
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
                                vote = commentVideModel.commentVote,
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
        ReplyInputSheet(
            commentViewModel = commentVideModel,
            playerViewModel = playerViewModel,
            title = target.title,
            placeholder = target.placeholder,
            sending = commentVideModel.sendingComment,
            canUploadImages = target.root == null || target.root == 0L,
            onDismiss = { replyDraftTarget = null },
            onSend = { draft ->
                scope.launch(Dispatchers.IO) {
                    val result = runCatching {
                        val uploadedImages = draft.images.map { image ->
                            val bytes = context.replyUriBytes(image.uri)
                                ?: error("无法读取图片：${image.fileName}")
                            commentVideModel.uploadCommentImage(
                                fileName = image.fileName,
                                bytes = bytes
                            ).getOrThrow()
                        }
                        commentVideModel.sendComment(
                            message = draft.message,
                            root = target.root,
                            parent = target.parent,
                            pictures = uploadedImages,
                            atNameToMid = draft.atNameToMid,
                            syncToDynamic = draft.syncToDynamic
                        ).getOrThrow()
                    }
                    if (result.isSuccess) {
                        withContext(Dispatchers.Main) {
                            replyDraftTarget = null
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            (result.exceptionOrNull()?.localizedMessage ?: "发送失败").toast(context)
                        }
                    }
                }
            }
        )
    }

    if (showVideoDanmakuSheet) {
        VideoSendDanmakuSheet(
            value = videoDanmakuDraft,
            sending = playerViewModel.sendingVideoDanmaku,
            mode = videoDanmakuMode,
            fontSize = videoDanmakuFontSize,
            color = videoDanmakuColor,
            onValueChange = { videoDanmakuDraft = it },
            onModeChange = { videoDanmakuMode = it },
            onFontSizeChange = { videoDanmakuFontSize = it },
            onColorChange = { videoDanmakuColor = it },
            onDismiss = { showVideoDanmakuSheet = false },
            onSend = sendVideoDanmaku
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
    upMid: Long,
    upFollowerCount: Int?,
    upArchiveCount: Int?,
    upFollowing: Boolean? = null,
    upRelationLoading: Boolean = false,
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
    offlineCacheState: OfflineVideoCacheTaskState = OfflineVideoCacheTaskState(aid = 0L, cid = 0L),
    offlinePlayback: Boolean = false,
    dlnaAvailable: Boolean = false,
    pictureInPictureSupported: Boolean = false,
    onToggleLike: () -> Unit = {},
    onTripleLike: () -> Unit = {},
    onToggleDislike: () -> Unit = {},
    onAddCoin: () -> Unit = {},
    onAddToDefaultFavoriteFolder: () -> Unit = {},
    onUpdateFavoriteFolders: (List<Long>) -> Unit = {},
    onToggleToView: () -> Unit = {},
    onShare: () -> Unit = {},
    onCast: () -> Unit = {},
    onEnterPictureInPicture: () -> Unit = {},
    onSaveCover: () -> Unit = {},
    onCacheVideo: () -> Unit = {},
    onPauseCache: () -> Unit = {},
    onResumeCache: () -> Unit = {},
    onClearCacheTask: () -> Unit = {},
    onPlayCache: () -> Unit = {},
    onToggleUpFollow: () -> Unit = {},
    onUpClick: (Long, String) -> Unit = { _, _ -> },
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
                modifier = Modifier
                    .height(64.dp)
                    .clickable(enabled = upMid > 0L) {
                        onUpClick(upMid, upName)
                    },
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

            if (upMid > 0L && upMid != Prefs.uid) {
                Button(
                    enabled = !upRelationLoading,
                    onClick = onToggleUpFollow
                ) {
                    if (upRelationLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(text = if (upFollowing == true) "已关注" else "关注")
                    }
                }
            }
        }
        LongPressCopyText(
            text = title,
            label = "标题",
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
                    Text(text = formatStatCount(playCount))
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
                    Text(text = formatStatCount(danmakuCount))
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
                offlineCacheState = offlineCacheState,
                offlinePlayback = offlinePlayback,
                dlnaAvailable = dlnaAvailable,
                pictureInPictureSupported = pictureInPictureSupported,
                onToggleLike = onToggleLike,
                onTripleLike = onTripleLike,
                onToggleDislike = onToggleDislike,
                onAddCoin = onAddCoin,
                onAddToDefaultFavoriteFolder = onAddToDefaultFavoriteFolder,
                onUpdateFavoriteFolders = onUpdateFavoriteFolders,
                onToggleToView = onToggleToView,
                onShare = onShare,
                onCast = onCast,
                onEnterPictureInPicture = onEnterPictureInPicture,
                onSaveCover = onSaveCover,
                onCacheVideo = onCacheVideo,
                onPauseCache = onPauseCache,
                onResumeCache = onResumeCache,
                onClearCacheTask = onClearCacheTask,
                onPlayCache = onPlayCache
            )
            LongPressCopyText(
                text = description,
                label = "简介"
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LongPressCopyText(
    text: String,
    label: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    style: TextStyle? = null
) {
    val context = LocalContext.current
    val textStyle = style ?: LocalTextStyle.current
    val interactionSource = remember { MutableInteractionSource() }
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Text(
            modifier = Modifier.combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {},
                onLongClick = {
                    if (text.isNotBlank()) expanded = true
                }
            ),
            text = text,
            maxLines = maxLines,
            overflow = overflow,
            style = textStyle
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(text = "复制") },
                leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                onClick = {
                    expanded = false
                    copyText(context, label, text)
                    "已复制$label".toast(context)
                }
            )
        }
    }
}

private fun copyText(
    context: Context,
    label: String,
    text: String
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
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
    offlineCacheState: OfflineVideoCacheTaskState,
    offlinePlayback: Boolean,
    dlnaAvailable: Boolean,
    pictureInPictureSupported: Boolean,
    onToggleLike: () -> Unit,
    onTripleLike: () -> Unit,
    onToggleDislike: () -> Unit,
    onAddCoin: () -> Unit,
    onAddToDefaultFavoriteFolder: () -> Unit,
    onUpdateFavoriteFolders: (List<Long>) -> Unit,
    onToggleToView: () -> Unit,
    onShare: () -> Unit,
    onCast: () -> Unit,
    onEnterPictureInPicture: () -> Unit,
    onSaveCover: () -> Unit,
    onCacheVideo: () -> Unit,
    onPauseCache: () -> Unit,
    onResumeCache: () -> Unit,
    onClearCacheTask: () -> Unit,
    onPlayCache: () -> Unit
) {
    var showFavoriteDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val cachePercent = (offlineCacheState.progress * 100).toInt().coerceIn(0, 100)
    val cacheMenuText = when (offlineCacheState.status) {
        OfflineVideoCacheStatus.Completed -> if (offlinePlayback) "正在播放缓存" else "播放缓存"
        OfflineVideoCacheStatus.Paused -> "继续缓存"
        OfflineVideoCacheStatus.Failed -> "重新选择缓存"
        OfflineVideoCacheStatus.Queued -> "暂停缓存"
        OfflineVideoCacheStatus.Fetching -> "暂停缓存"
        OfflineVideoCacheStatus.DownloadingVideo,
        OfflineVideoCacheStatus.DownloadingAudio,
        OfflineVideoCacheStatus.DownloadingDanmaku -> "暂停缓存 $cachePercent%"
        OfflineVideoCacheStatus.Idle -> "选择缓存"
    }
    val hasClearableCacheTask = offlineCacheState.status != OfflineVideoCacheStatus.Idle &&
        offlineCacheState.status != OfflineVideoCacheStatus.Completed

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
                if (dlnaAvailable) {
                    DropdownMenuItem(
                        text = { Text(text = "投屏") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Cast,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            showMoreMenu = false
                            onCast()
                        }
                    )
                }
                if (pictureInPictureSupported) {
                    DropdownMenuItem(
                        text = { Text(text = "画中画") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.PictureInPictureAlt,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            showMoreMenu = false
                            onEnterPictureInPicture()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(text = cacheMenuText) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null
                        )
                    },
                    enabled = !offlinePlayback || offlineCacheState.status != OfflineVideoCacheStatus.Completed,
                    onClick = {
                        showMoreMenu = false
                        when (offlineCacheState.status) {
                            OfflineVideoCacheStatus.Completed -> onPlayCache()
                            OfflineVideoCacheStatus.Paused -> onResumeCache()
                            OfflineVideoCacheStatus.Queued,
                            OfflineVideoCacheStatus.Fetching,
                            OfflineVideoCacheStatus.DownloadingVideo,
                            OfflineVideoCacheStatus.DownloadingAudio,
                            OfflineVideoCacheStatus.DownloadingDanmaku -> onPauseCache()
                            OfflineVideoCacheStatus.Failed,
                            OfflineVideoCacheStatus.Idle -> onCacheVideo()
                        }
                    }
                )
                if (hasClearableCacheTask) {
                    DropdownMenuItem(
                        text = { Text(text = "清除缓存任务") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            showMoreMenu = false
                            onClearCacheTask()
                        }
                    )
                }
                DropdownMenuItem(
                    text = {
                        Text(text = if (savingCover) "保存中..." else "保存封面")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Image,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfflineCacheBottomSheet(
    show: Boolean,
    videoDetail: VideoDetail?,
    currentCid: Long,
    currentQuality: Resolution,
    loading: Boolean,
    cacheStateOf: (Long, Long) -> OfflineVideoCacheTaskState,
    onDismiss: () -> Unit,
    onCacheOne: (OfflineVideoCacheTarget, Resolution) -> Unit,
    onCacheAll: (List<OfflineVideoCacheTarget>, Resolution) -> Unit,
    onOpenCachePage: () -> Unit
) {
    if (!show) return

    var selectedQuality by remember(show, currentQuality) { mutableStateOf(currentQuality) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var showCacheAllConfirm by remember { mutableStateOf(false) }
    val qualityOptions = remember { Resolution.entries.sortedByDescending { it.code } }
    val context = LocalContext.current
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val items = remember(videoDetail, currentCid) {
        buildOfflineCacheSheetItems(videoDetail, currentCid)
    }
    val bottomBarColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val cacheableTargets = items
        .filter { item ->
            val state = cacheStateOf(item.target.aid, item.target.cid)
            state.status != OfflineVideoCacheStatus.Completed && !state.isActive
        }
        .map { it.target }

    if (showCacheAllConfirm) {
        AlertDialog(
            onDismissRequest = { showCacheAllConfirm = false },
            title = { Text(text = "确定缓存全部？") },
            text = {
                Text(
                    text = if (cacheableTargets.isEmpty()) {
                        "当前列表没有可添加的缓存任务"
                    } else {
                        "将以 ${selectedQuality.getDisplayName(context)} 作为最高画质添加 ${cacheableTargets.size} 个缓存任务。"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = cacheableTargets.isNotEmpty(),
                    onClick = {
                        showCacheAllConfirm = false
                        onCacheAll(cacheableTargets, selectedQuality)
                    }
                ) {
                    Text(text = "缓存全部")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCacheAllConfirm = false }) {
                    Text(text = "取消")
                }
            }
        )
    }

    ModalBottomSheet(
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = {
            if (!loading) onDismiss()
        },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = "离线缓存",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "最高画质",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box {
                        TextButton(
                            enabled = !loading,
                            onClick = { showQualityMenu = true }
                        ) {
                            Text(text = selectedQuality.getDisplayName(context))
                        }
                        DropdownMenu(
                            expanded = showQualityMenu,
                            onDismissRequest = { showQualityMenu = false }
                        ) {
                            qualityOptions.forEach { quality ->
                                val selected = quality == selectedQuality

                                DropdownMenuItem(
                                    modifier = Modifier.background(
                                        if (selected) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            Color.Transparent
                                        }
                                    ),
                                    text = {
                                        Text(
                                            text = quality.getDisplayName(context),
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    },
                                    trailingIcon = if (selected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    onClick = {
                                        selectedQuality = quality
                                        showQualityMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "视频列表尚未加载完成",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = items,
                        key = { "${it.target.aid}:${it.target.cid}" }
                    ) { item ->
                        OfflineCacheSheetItemRow(
                            item = item,
                            state = cacheStateOf(item.target.aid, item.target.cid),
                            loading = loading,
                            onCache = { onCacheOne(item.target, selectedQuality) }
                        )
                    }
                }
            }

            HorizontalDivider()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bottomBarColor)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        enabled = !loading && cacheableTargets.isNotEmpty(),
                        onClick = { showCacheAllConfirm = true }
                    ) {
                        Text(text = "缓存全部")
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(22.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    TextButton(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        onClick = onOpenCachePage
                    ) {
                        Text(text = "查看缓存")
                    }
                }
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                )
            }
        }
    }
}

@Composable
private fun OfflineCacheSheetItemRow(
    item: OfflineCacheSheetItem,
    state: OfflineVideoCacheTaskState,
    loading: Boolean,
    onCache: () -> Unit
) {
    val enabled = !loading &&
        state.status != OfflineVideoCacheStatus.Completed &&
        !state.isActive
    val statusText = state.toSheetStatusText()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (item.isCurrent) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
                }
            )
            .clickable(enabled = enabled, onClick = onCache)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AsyncImage(
            modifier = Modifier
                .width(96.dp)
                .height(54.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            model = item.cover,
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (item.isCurrent) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = item.subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (statusText != null) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = when (state.status) {
                    OfflineVideoCacheStatus.Completed -> MaterialTheme.colorScheme.primary
                    OfflineVideoCacheStatus.Failed -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Download,
                contentDescription = "缓存",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class OfflineCacheSheetItem(
    val target: OfflineVideoCacheTarget,
    val title: String,
    val subtitle: String,
    val cover: String,
    val isCurrent: Boolean
)

private fun buildOfflineCacheSheetItems(
    videoDetail: VideoDetail?,
    currentCid: Long
): List<OfflineCacheSheetItem> {
    if (videoDetail == null) return emptyList()

    val ugcItems = videoDetail.ugcSeason?.sections
        ?.flatMap { section ->
            section.episodes.flatMap { episode ->
                val pages = episode.pages
                if (pages.size > 1) {
                    pages.map { page ->
                        val title = "${episode.title} - P${page.index} ${page.title}"
                        val target = OfflineVideoCacheTarget(
                            aid = episode.aid,
                            bvid = episode.bvid,
                            cid = page.cid,
                            title = episode.title,
                            partTitle = "${episode.title} - ${page.title}",
                            cover = episode.cover.ifBlank { videoDetail.cover },
                            upName = videoDetail.author.name,
                            durationMs = page.duration * 1000L,
                            width = page.dimension.width,
                            height = page.dimension.height
                        )
                        OfflineCacheSheetItem(
                            target = target,
                            title = title,
                            subtitle = buildOfflineCacheItemSubtitle(
                                sectionTitle = section.title,
                                durationMs = target.durationMs,
                                isCurrent = page.cid == currentCid
                            ),
                            cover = target.cover,
                            isCurrent = page.cid == currentCid
                        )
                    }
                } else {
                    val page = pages.firstOrNull()
                    val cid = page?.cid ?: episode.cid
                    val target = OfflineVideoCacheTarget(
                        aid = episode.aid,
                        bvid = episode.bvid,
                        cid = cid,
                        title = episode.title,
                        partTitle = "",
                        cover = episode.cover.ifBlank { videoDetail.cover },
                        upName = videoDetail.author.name,
                        durationMs = ((page?.duration ?: episode.duration) * 1000L),
                        width = page?.dimension?.width ?: episode.dimension?.width ?: 0,
                        height = page?.dimension?.height ?: episode.dimension?.height ?: 0
                    )
                    listOf(
                        OfflineCacheSheetItem(
                            target = target,
                            title = episode.title,
                            subtitle = buildOfflineCacheItemSubtitle(
                                sectionTitle = section.title,
                                durationMs = target.durationMs,
                                isCurrent = cid == currentCid
                            ),
                            cover = target.cover,
                            isCurrent = cid == currentCid
                        )
                    )
                }
            }
        }
        .orEmpty()

    if (ugcItems.isNotEmpty()) return ugcItems

    return videoDetail.pages.map { page ->
        val target = OfflineVideoCacheTarget(
            aid = videoDetail.aid,
            bvid = videoDetail.bvid,
            cid = page.cid,
            title = videoDetail.title,
            partTitle = page.title,
            cover = videoDetail.cover,
            upName = videoDetail.author.name,
            durationMs = page.duration * 1000L,
            width = page.dimension.width,
            height = page.dimension.height
        )
        OfflineCacheSheetItem(
            target = target,
            title = "P${page.index} ${page.title}",
            subtitle = buildOfflineCacheItemSubtitle(
                sectionTitle = "",
                durationMs = target.durationMs,
                isCurrent = page.cid == currentCid
            ),
            cover = target.cover,
            isCurrent = page.cid == currentCid
        )
    }
}

private fun buildOfflineCacheItemSubtitle(
    sectionTitle: String,
    durationMs: Long,
    isCurrent: Boolean
): String = listOfNotNull(
    sectionTitle.takeIf { it.isNotBlank() },
    durationMs.takeIf { it > 0L }?.formatHourMinSec(),
    "正在播放".takeIf { isCurrent }
).joinToString(" · ")

private fun OfflineVideoCacheTaskState.toSheetStatusText(): String? = when (status) {
    OfflineVideoCacheStatus.Completed -> "已缓存"
    OfflineVideoCacheStatus.Queued -> "等待中"
    OfflineVideoCacheStatus.Fetching -> "准备中"
    OfflineVideoCacheStatus.DownloadingVideo,
    OfflineVideoCacheStatus.DownloadingAudio,
    OfflineVideoCacheStatus.DownloadingDanmaku -> "${(progress * 100).toInt().coerceIn(0, 100)}%"
    OfflineVideoCacheStatus.Paused -> "已暂停"
    OfflineVideoCacheStatus.Failed -> "失败"
    OfflineVideoCacheStatus.Idle -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoDetailTabsWithDanmakuControls(
    titles: List<String>,
    selectedTabIndex: Int,
    danmakuEnabled: Boolean,
    onTabClick: (Int) -> Unit,
    onSendDanmakuClick: () -> Unit,
    onToggleDanmaku: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            PrimaryTabRow(
                modifier = Modifier.fillMaxSize(),
                selectedTabIndex = selectedTabIndex
            ) {
                titles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { onTabClick(index) },
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
        }
        VideoDanmakuActionButtons(
            danmakuEnabled = danmakuEnabled,
            onSendDanmakuClick = onSendDanmakuClick,
            onToggleDanmaku = onToggleDanmaku
        )
    }
}

@Composable
private fun VideoDetailDanmakuActionRow(
    modifier: Modifier = Modifier,
    danmakuEnabled: Boolean,
    onSendDanmakuClick: () -> Unit,
    onToggleDanmaku: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        VideoDanmakuActionButtons(
            danmakuEnabled = danmakuEnabled,
            onSendDanmakuClick = onSendDanmakuClick,
            onToggleDanmaku = onToggleDanmaku
        )
    }
}

@Composable
private fun VideoDanmakuActionButtons(
    danmakuEnabled: Boolean,
    onSendDanmakuClick: () -> Unit,
    onToggleDanmaku: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(26.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onSendDanmakuClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "发弹幕",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
    IconButton(
        modifier = Modifier.size(38.dp),
        onClick = onToggleDanmaku
    ) {
        val contentDescription = if (danmakuEnabled) "关闭弹幕" else "开启弹幕"
        Text(
            modifier = Modifier.clearAndSetSemantics {
                this.contentDescription = contentDescription
            },
            text = if (danmakuEnabled) DanmakuOnIcon else DanmakuOffIcon,
            fontFamily = DanmakuActionIconFontFamily,
            fontSize = 22.sp,
            lineHeight = 22.sp,
            color = if (danmakuEnabled) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.outline
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoSendDanmakuSheet(
    value: String,
    sending: Boolean,
    mode: Int,
    fontSize: Int,
    color: Int,
    onValueChange: (String) -> Unit,
    onModeChange: (Int) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onColorChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissSheet = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        onDismiss()
    }
    val colorOptions = remember {
        listOf(
            0xFFFFFF,
            0xFE0302,
            0xFF7204,
            0xFFFF00,
            0xA0EE00,
            0x00CD00,
            0x89D5FF,
            0x4266BE,
            0xCC0273,
            0x9B9B9B
        )
    }

    ModalBottomSheet(
        onDismissRequest = dismissSheet,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = "发弹幕",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(
                    enabled = !sending,
                    onClick = dismissSheet
                ) {
                    Text(text = "取消")
                }
                Button(
                    enabled = value.isNotBlank() && !sending,
                    onClick = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        onSend()
                    }
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
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = value,
                enabled = !sending,
                onValueChange = { input ->
                    onValueChange(input.take(100))
                },
                placeholder = { Text(text = "输入弹幕内容") },
                minLines = 1,
                maxLines = 3,
                singleLine = false
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "样式",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                VideoDanmakuOptionChip(
                    selected = mode == 1,
                    text = "滚动",
                    onClick = { onModeChange(1) }
                )
                VideoDanmakuOptionChip(
                    selected = mode == 5,
                    text = "顶部",
                    onClick = { onModeChange(5) }
                )
                VideoDanmakuOptionChip(
                    selected = mode == 4,
                    text = "底部",
                    onClick = { onModeChange(4) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "字号",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                VideoDanmakuOptionChip(
                    selected = fontSize == 18,
                    text = "小",
                    onClick = { onFontSizeChange(18) }
                )
                VideoDanmakuOptionChip(
                    selected = fontSize == 25,
                    text = "标准",
                    onClick = { onFontSizeChange(25) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.padding(end = 12.dp),
                    text = "颜色",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(colorOptions) { item ->
                        VideoDanmakuColorSwatch(
                            color = item,
                            selected = color == item,
                            onClick = { onColorChange(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoDanmakuOptionChip(
    selected: Boolean,
    text: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = text) }
    )
}

@Composable
private fun VideoDanmakuColorSwatch(
    color: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF000000.toInt() or color),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {}
}

@Composable
private fun LiveRoomScreenBackground(
    backgroundUrl: String
) {
    val liveBackgroundModel: Any = backgroundUrl.takeIf { it.isNotBlank() }
        ?: R.drawable.live_default_bg
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = liveBackgroundModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.62f
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
        )
    }
}

@Composable
private fun LiveRoomPanel(
    modifier: Modifier = Modifier,
    playerViewModel: VideoPlayerV3ViewModel,
    showDanmakuInput: Boolean,
    danmakuInputValue: String,
    danmakuInputSelection: EmoteTextSelection,
    danmakuInputEmoteTokens: List<EmoteInputToken>,
    sendingDanmaku: Boolean,
    emotePackages: List<LiveEmotePackage>,
    loadingEmoticons: Boolean,
    emoticonError: String?,
    onDanmakuInputValueChange: (String, EmoteTextSelection) -> Unit,
    onLoadEmoticons: (Boolean) -> Unit,
    onTextEmoticonClick: (LiveEmoticon) -> Unit,
    onLiveEmoticonClick: (LiveEmoticon) -> Unit,
    onDismissDanmakuInput: () -> Unit,
    onSendDanmaku: () -> Unit,
    onSendDanmakuClick: () -> Unit,
    onLikeClick: () -> Unit,
    onRefresh: () -> Unit,
    onQualitySelected: (Int) -> Unit,
    onCodecSelected: (LiveCodec) -> Unit,
    onLineSelected: (Int) -> Unit,
    onCopyLink: () -> Unit,
    onShare: () -> Unit,
    onOpenBrowser: () -> Unit,
    showHeader: Boolean,
    backgroundColor: Color
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissDanmakuInput = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        onDismissDanmakuInput()
    }

    Box(
        modifier = modifier.background(backgroundColor)
    ) {
        BackHandler(enabled = showDanmakuInput, onBack = dismissDanmakuInput)
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
                    compact = true,
                    backgroundColor = Color.Transparent
                )
            }
            LiveDanmakuList(
                modifier = Modifier.weight(1f),
                messages = playerViewModel.liveDanmakuMessages,
                shouldKeepLatestVisible = showDanmakuInput
            )
            if (showDanmakuInput) {
                LiveSendDanmakuPanel(
                    value = danmakuInputValue,
                    selection = danmakuInputSelection,
                    emoteTokens = danmakuInputEmoteTokens,
                    sending = sendingDanmaku,
                    emotePackages = emotePackages,
                    loadingEmoticons = loadingEmoticons,
                    emoticonError = emoticonError,
                    onValueChange = onDanmakuInputValueChange,
                    onLoadEmoticons = onLoadEmoticons,
                    onTextEmoticonClick = onTextEmoticonClick,
                    onLiveEmoticonClick = onLiveEmoticonClick,
                    onDismiss = dismissDanmakuInput,
                    onSend = onSendDanmaku
                )
            } else {
                LiveRoomInputBar(
                    modifier = Modifier.navigationBarsPadding(),
                    playerViewModel = playerViewModel,
                    onSendDanmakuClick = onSendDanmakuClick,
                    onLikeClick = onLikeClick,
                    onQualitySelected = onQualitySelected,
                    onCodecSelected = onCodecSelected,
                    onLineSelected = onLineSelected,
                    onRefresh = onRefresh,
                    onCopyLink = onCopyLink,
                    onShare = onShare,
                    onOpenBrowser = onOpenBrowser
                )
            }
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
    onLineSelected: (Int) -> Unit,
    onRefresh: () -> Unit,
    onCopyLink: () -> Unit,
    onShare: () -> Unit,
    onOpenBrowser: () -> Unit
) {
    var showQualityMenu by remember { mutableStateOf(false) }
    var showCodecMenu by remember { mutableStateOf(false) }
    var showLineMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val currentLiveLine = playerViewModel.availableLiveLines
        .firstOrNull { it.index == playerViewModel.currentLiveLineIndex }
    val currentLiveLineButtonText = currentLiveLine
        ?.let { "线路 ${it.index + 1}" }
        ?: "线路"

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
                    if (playerViewModel.availableLiveLines.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text("线路：$currentLiveLineButtonText") },
                            onClick = {
                                showMoreMenu = false
                                showLineMenu = true
                            }
                        )
                    }
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
                DropdownMenu(
                    expanded = showLineMenu,
                    onDismissRequest = { showLineMenu = false }
                ) {
                    playerViewModel.availableLiveLines.forEach { line ->
                        DropdownMenuItem(
                            text = { Text(line.displayName) },
                            onClick = {
                                showLineMenu = false
                                onLineSelected(line.index)
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
    compact: Boolean = false,
    backgroundColor: Color = MaterialTheme.colorScheme.surface
) {
    val summaryTextStyle = MaterialTheme.typography.bodySmall.copy(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )
    val stats = listOfNotNull(
        popularityText.takeIf { it.isNotBlank() },
        onlineCountText.takeIf { it.isNotBlank() },
        roomId.takeIf { it > 0 }?.let { "房间 $it" }
    )
    val statsText = stats.joinToString("  ·  ")

    if (compact) {
        val compactSummaryTextStyle = MaterialTheme.typography.bodySmall.copy(
            color = Color.White.copy(alpha = 0.72f)
        )
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(backgroundColor),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                modifier = Modifier
                    .padding(end = 10.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                model = upAvatar,
                contentDescription = null
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = upName.ifBlank { "未知主播" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    Text(
                        text = "正在直播",
                        style = compactSummaryTextStyle,
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = title.ifBlank { "直播间" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.92f)
                )
                if (statsText.isNotBlank()) {
                    ProvideTextStyle(compactSummaryTextStyle) {
                        Text(
                            text = statsText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        return
    }

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
                text = statsText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LiveDanmakuList(
    modifier: Modifier = Modifier,
    messages: List<LiveDanmakuMessage>,
    shouldKeepLatestVisible: Boolean = false
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

    LaunchedEffect(shouldKeepLatestVisible, messages.size) {
        if (shouldKeepLatestVisible && messages.isNotEmpty()) {
            followLatest = true
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
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
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
            .padding(6.dp),
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
        LiveDanmakuRichText(
            content = message.content,
            emojiMap = message.emojiMap,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LiveDanmakuRichText(
    content: String,
    emojiMap: Map<String, String>,
    modifier: Modifier = Modifier
) {
    if (emojiMap.isEmpty()) {
        Text(
            modifier = modifier,
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
        return
    }

    val emojiEntries = emojiMap
        .filter { (key, url) -> key.isNotBlank() && url.isNotBlank() }
        .toList()
        .sortedByDescending { (key, _) -> key.length }

    if (emojiEntries.isEmpty()) {
        Text(
            modifier = modifier,
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
        return
    }

    val inlineContent = emojiEntries.associate { (key, url) ->
        key to InlineTextContent(
            Placeholder(
                width = 28.sp,
                height = 28.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            )
        ) {
            AsyncImage(
                model = url,
                contentDescription = key,
                contentScale = ContentScale.Fit
            )
        }
    }
    val annotatedContent = buildAnnotatedString {
        var index = 0
        while (index < content.length) {
            val match = emojiEntries.firstOrNull { (key, _) ->
                content.regionMatches(index, key, 0, key.length)
            }
            if (match != null) {
                appendInlineContent(match.first, match.first)
                index += match.first.length
            } else {
                append(content[index])
                index += 1
            }
        }
        if (content.isBlank() && emojiEntries.size == 1) {
            appendInlineContent(emojiEntries.first().first, emojiEntries.first().first)
        }
    }

    Text(
        modifier = modifier,
        text = annotatedContent,
        inlineContent = inlineContent,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White
    )
}

@Composable
private fun LiveSendDanmakuPanel(
    value: String,
    selection: EmoteTextSelection,
    emoteTokens: List<EmoteInputToken>,
    sending: Boolean,
    emotePackages: List<LiveEmotePackage>,
    loadingEmoticons: Boolean,
    emoticonError: String?,
    onValueChange: (String, EmoteTextSelection) -> Unit,
    onLoadEmoticons: (Boolean) -> Unit,
    onTextEmoticonClick: (LiveEmoticon) -> Unit,
    onLiveEmoticonClick: (LiveEmoticon) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit
) {
    var showEmoticons by remember { mutableStateOf(false) }

    LaunchedEffect(showEmoticons) {
        if (showEmoticons) onLoadEmoticons(false)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp)
            .imePadding()
            .navigationBarsPadding(),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 15.dp, top = 12.dp, end = 15.dp, bottom = 10.dp)
            ) {
                EmoteTextEditor(
                    modifier = Modifier.fillMaxWidth(),
                    value = value,
                    selection = selection,
                    emoteTokens = emoteTokens,
                    placeholder = "输入弹幕内容",
                    label = null,
                    enabled = !sending,
                    minLines = 1,
                    maxLines = 2,
                    shape = RoundedCornerShape(0.dp),
                    border = null,
                    containerColor = Color.Transparent,
                    contentPadding = PaddingValues(0.dp),
                    onValueChange = onValueChange
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showEmoticons = !showEmoticons }) {
                        Icon(
                            imageVector = if (showEmoticons) Icons.Default.Keyboard else Icons.Default.EmojiEmotions,
                            contentDescription = if (showEmoticons) "收起表情" else "选择表情"
                        )
                    }
                    TextButton(
                        enabled = !sending,
                        onClick = onDismiss
                    ) {
                        Text(text = "取消")
                    }
                }
                FilledTonalButton(
                    enabled = value.isNotBlank() && !sending,
                    onClick = onSend,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    if (sending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = "发送")
                    }
                }
            }
            if (showEmoticons) {
                LiveEmoticonPanel(
                    packages = emotePackages,
                    loading = loadingEmoticons,
                    error = emoticonError,
                    sending = sending,
                    onReload = { onLoadEmoticons(true) },
                    onTextEmoticonClick = onTextEmoticonClick,
                    onLiveEmoticonClick = onLiveEmoticonClick
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveEmoticonPanel(
    packages: List<LiveEmotePackage>,
    loading: Boolean,
    error: String?,
    sending: Boolean,
    onReload: () -> Unit,
    onTextEmoticonClick: (LiveEmoticon) -> Unit,
    onLiveEmoticonClick: (LiveEmoticon) -> Unit
) {
    var selectedPackageIndex by remember { mutableStateOf(0) }
    val selectedPackage = packages.getOrNull(selectedPackageIndex)

    LaunchedEffect(packages.size) {
        if (selectedPackageIndex >= packages.size) {
            selectedPackageIndex = 0
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp, max = 300.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        tonalElevation = 1.dp
    ) {
        when {
            loading && packages.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                }
            }
            error != null && packages.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(onClick = onReload) {
                        Text(text = error.ifBlank { "获取表情失败，点击重试" })
                    }
                }
            }
            selectedPackage == null -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(onClick = onReload) {
                        Text(text = "暂无可用表情")
                    }
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LazyVerticalGrid(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        columns = GridCells.Adaptive(48.dp),
                        contentPadding = PaddingValues(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedPackage.emoticons) { emoticon ->
                            LiveEmoticonItem(
                                emoticon = emoticon,
                                enabled = !sending,
                                onClick = {
                                    if (selectedPackage.pkgType == 3) {
                                        onTextEmoticonClick(emoticon)
                                    } else {
                                        onLiveEmoticonClick(emoticon)
                                    }
                                }
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    PrimaryScrollableTabRow(
                        selectedTabIndex = selectedPackageIndex,
                        edgePadding = 0.dp,
                        minTabWidth = 48.dp,
                        divider = {},
                        containerColor = Color.Transparent
                    ) {
                        packages.forEachIndexed { index, item ->
                            Tab(
                                selected = index == selectedPackageIndex,
                                onClick = { selectedPackageIndex = index },
                                icon = {
                                    AsyncImage(
                                        modifier = Modifier.size(24.dp),
                                        model = item.currentCover,
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveEmoticonItem(
    emoticon: LiveEmoticon,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(5.dp),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = emoticon.url,
            contentDescription = emoticon.displayName,
            contentScale = ContentScale.Fit
        )
    }
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

private fun formatStatCount(value: Long): String {
    return when {
        value >= 100_000_000L -> "${value / 100_000_000L}亿"
        value >= 10_000L -> String.format("%.1f万", value / 10_000.0)
        value > 0L -> value.toString()
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

private fun Context.replyUriFileName(uri: Uri): String {
    return runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
    }.getOrNull()?.takeIf { it.isNotBlank() }
        ?: "reply_${System.currentTimeMillis()}.jpg"
}

private fun Context.replyUriBytes(uri: Uri): ByteArray? =
    contentResolver.openInputStream(uri)?.use { it.readBytes() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReplyInputSheet(
    commentViewModel: CommentViewModel,
    playerViewModel: VideoPlayerV3ViewModel,
    title: String,
    placeholder: String,
    sending: Boolean,
    canUploadImages: Boolean,
    onDismiss: () -> Unit,
    onSend: (ReplySendDraft) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    var text by remember { mutableStateOf("") }
    var selection by remember { mutableStateOf(EmoteTextSelection.Zero) }
    var activePanel by remember { mutableStateOf<ReplyInputPanel?>(null) }
    var mentionKeyword by remember { mutableStateOf("") }
    var loadingMentions by remember { mutableStateOf(false) }
    var syncToDynamic by remember { mutableStateOf(false) }
    val mentionSuggestions = remember { mutableStateListOf<DynamicMentionDraft>() }
    val selectedImages = remember { mutableStateListOf<ReplyLocalImage>() }
    val emoteTokens = remember { mutableStateListOf<EmoteInputToken>() }
    val mentionTokens = remember { mutableStateListOf<ReplyMentionToken>() }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val remaining = 9 - selectedImages.size
        if (remaining <= 0) {
            "最多选择 9 张图片".toast(context)
            return@rememberLauncherForActivityResult
        }
        selectedImages.addAll(
            uris.take(remaining).map { uri ->
                ReplyLocalImage(
                    uri = uri,
                    fileName = context.replyUriFileName(uri)
                )
            }
        )
        if (uris.size > remaining) "已达到 9 张图片上限".toast(context)
    }

    fun insertText(
        marker: String,
        emote: DynamicEmoteDraft? = null,
        mention: DynamicMentionDraft? = null
    ) {
        val start = selection.start.coerceIn(0, text.length)
        val end = selection.end.coerceIn(0, text.length)
        val replaceStart = minOf(start, end)
        val replaceEnd = maxOf(start, end)
        text = text.replaceRange(replaceStart, replaceEnd, marker)
        selection = EmoteTextSelection.collapsed(replaceStart + marker.length)
        if (emote != null && emote.url.isNotBlank()) {
            emoteTokens.add(
                EmoteInputToken(
                    marker = marker,
                    preferredStart = replaceStart,
                    emoteUrl = emote.url,
                    emoteName = emote.emoteDisplayName
                )
            )
        }
        if (mention != null) {
            mentionTokens.add(
                ReplyMentionToken(
                    marker = marker,
                    name = mention.name,
                    uid = mention.uid.toLongOrNull() ?: 0L,
                    preferredStart = replaceStart
                )
            )
        }
    }

    LaunchedEffect(activePanel) {
        if (activePanel == ReplyInputPanel.Emoji) {
            commentViewModel.loadEmotePackages()
        }
    }

    LaunchedEffect(activePanel, mentionKeyword) {
        if (activePanel != ReplyInputPanel.Mention) return@LaunchedEffect
        if (mentionKeyword.isNotBlank()) delay(250)
        loadingMentions = true
        val result = withContext(Dispatchers.IO) {
            commentViewModel.searchMention(mentionKeyword)
        }
        mentionSuggestions.clear()
        mentionSuggestions.addAll(result.getOrDefault(emptyList()))
        loadingMentions = false
    }

    val canSend = text.isNotBlank() || selectedImages.isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = {
            if (!sending) onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(
                    enabled = !sending,
                    onClick = onDismiss
                ) {
                    Text(text = "取消")
                }
                Button(
                    enabled = !sending && canSend,
                    onClick = {
                        onSend(
                            ReplySendDraft(
                                message = text,
                                images = selectedImages.toList(),
                                atNameToMid = mentionTokens
                                    .filter { token ->
                                        token.uid > 0L && text.contains(token.marker)
                                    }
                                    .associate { it.name to it.uid },
                                syncToDynamic = syncToDynamic
                            )
                        )
                    }
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
            }

            EmoteTextEditor(
                modifier = Modifier.fillMaxWidth(),
                value = text,
                selection = selection,
                emoteTokens = emoteTokens,
                placeholder = placeholder,
                label = "回复内容",
                enabled = !sending,
                minLines = 4,
                maxLines = 8,
                onValueChange = { value, newSelection ->
                    text = value
                    selection = newSelection
                }
            )

            if (selectedImages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(selectedImages) { image ->
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            AsyncImage(
                                modifier = Modifier.fillMaxSize(),
                                model = image.uri,
                                contentDescription = image.fileName,
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(28.dp)
                                    .background(
                                        color = Color.Black.copy(alpha = 0.45f),
                                        shape = CircleShape
                                    ),
                                onClick = { selectedImages.remove(image) }
                            ) {
                                Icon(
                                    modifier = Modifier.size(16.dp),
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "删除图片",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReplyToolButton(
                    selected = activePanel == ReplyInputPanel.Emoji,
                    icon = if (activePanel == ReplyInputPanel.Emoji) Icons.Default.Keyboard else Icons.Default.EmojiEmotions,
                    contentDescription = "表情",
                    onClick = {
                        activePanel = if (activePanel == ReplyInputPanel.Emoji) null else ReplyInputPanel.Emoji
                    }
                )
                ReplyToolButton(
                    enabled = canUploadImages,
                    selected = selectedImages.isNotEmpty(),
                    icon = if (canUploadImages) Icons.Default.Image else Icons.Default.ImageNotSupported,
                    contentDescription = "图片",
                    onClick = {
                        if (canUploadImages) {
                            imagePicker.launch("image/*")
                        } else {
                            "当前评论区不支持发送图片".toast(context)
                        }
                    }
                )
                ReplyToolButton(
                    selected = activePanel == ReplyInputPanel.Mention,
                    icon = Icons.Default.AlternateEmail,
                    contentDescription = "@",
                    onClick = {
                        activePanel = if (activePanel == ReplyInputPanel.Mention) null else ReplyInputPanel.Mention
                    }
                )
                ReplyToolButton(
                    selected = activePanel == ReplyInputPanel.More,
                    icon = if (activePanel == ReplyInputPanel.More) Icons.Default.Keyboard else Icons.Default.AddCircleOutline,
                    contentDescription = "更多",
                    onClick = {
                        activePanel = if (activePanel == ReplyInputPanel.More) null else ReplyInputPanel.More
                    }
                )
                Spacer(modifier = Modifier.weight(1f))
                FilledTonalButton(
                    onClick = { syncToDynamic = !syncToDynamic },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Checkbox(
                        modifier = Modifier.size(22.dp),
                        checked = syncToDynamic,
                        onCheckedChange = { syncToDynamic = it }
                    )
                    Text(text = "转动态")
                }
            }

            when (activePanel) {
                ReplyInputPanel.Emoji -> EmotePanel(
                    packages = commentViewModel.emotePackages,
                    loading = commentViewModel.loadingEmotes,
                    onSelect = { emoji ->
                        insertText(
                            marker = emoji.text,
                            emote = emoji
                        )
                    }
                )

                ReplyInputPanel.Mention -> ReplyMentionPanel(
                    keyword = mentionKeyword,
                    onKeywordChange = { mentionKeyword = it },
                    mentions = mentionSuggestions,
                    loading = loadingMentions,
                    onSelect = { mention ->
                        insertText(
                            marker = "@${mention.name} ",
                            mention = mention
                        )
                    }
                )

                ReplyInputPanel.More -> ReplyMorePanel(
                    canInsertProgress = playerViewModel.videoPlayer?.currentPosition != null,
                    onInsertProgress = {
                        val position = playerViewModel.videoPlayer?.currentPosition ?: 0L
                        insertText(" ${position.formatHourMinSec()} ")
                    }
                )

                null -> Unit
            }
        }
    }
}

@Composable
private fun ReplyToolButton(
    selected: Boolean,
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        enabled = enabled,
        onClick = onClick
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                selected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun ReplyMentionPanel(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    mentions: List<DynamicMentionDraft>,
    loading: Boolean,
    onSelect: (DynamicMentionDraft) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = keyword,
                onValueChange = onKeywordChange,
                label = { Text(text = "搜索用户") },
                singleLine = true
            )
            if (loading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            }
            mentions.take(8).forEach { item ->
                InputChip(
                    selected = false,
                    onClick = { onSelect(item) },
                    label = {
                        Text(
                            text = item.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ReplyMorePanel(
    canInsertProgress: Boolean,
    onInsertProgress: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReplyMoreItem(
                title = "视频进度",
                icon = Icons.Rounded.WatchLater,
                enabled = canInsertProgress,
                onClick = onInsertProgress
            )
            ReplyMoreItem(
                title = "插入内容",
                icon = Icons.AutoMirrored.Filled.Comment,
                enabled = false,
                onClick = {}
            )
        }
    }
}

@Composable
private fun ReplyMoreItem(
    title: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                }
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
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
    vote: CommentVote? = null,
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
                vote?.let { commentVote ->
                    item {
                        CommentVoteCard(
                            vote = commentVote,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
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
                upMid = 2L,
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
