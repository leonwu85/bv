package dev.aaa1115910.bv.mobile.screen

import android.app.Activity
import android.content.Context
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.origeek.imageViewer.previewer.ImagePreviewer
import com.origeek.imageViewer.previewer.ImagePreviewerState
import com.origeek.imageViewer.previewer.VerticalDragType
import com.origeek.imageViewer.previewer.rememberPreviewerState
import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.biliapi.entity.reply.Comment
import dev.aaa1115910.biliapi.entity.reply.CommentSort
import dev.aaa1115910.biliapi.entity.user.DynamicItem
import dev.aaa1115910.bv.mobile.component.ImagePreviewerActions
import dev.aaa1115910.bv.mobile.component.home.dynamic.DynamicContent
import dev.aaa1115910.bv.mobile.component.home.dynamic.DynamicHeader
import dev.aaa1115910.bv.mobile.component.reply.Comments
import dev.aaa1115910.bv.mobile.component.reply.Replies
import dev.aaa1115910.bv.mobile.component.videocard.shareText
import dev.aaa1115910.bv.mobile.util.saveImageToGallery
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.swapList
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.CommentViewModel
import dev.aaa1115910.bv.viewmodel.DynamicDetailViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import kotlin.math.min

private data class DynamicReplyDraftTarget(
    val title: String,
    val placeholder: String,
    val root: Long? = null,
    val parent: Long? = null
)

@OptIn(
    ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3WindowSizeClassApi::class
)
@Composable
fun DynamicDetailScreen(
    modifier: Modifier = Modifier,
    dynamicDetailViewModel: DynamicDetailViewModel = koinViewModel(),
    commentViewModel: CommentViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger("DynamicDetailScreen")
    val windowSizeClass = calculateWindowSizeClass(context as Activity)
    val pictures = remember { mutableStateListOf<Picture>() }
    var savingPreviewImage by remember { mutableStateOf(false) }
    val dynamicDetailState = rememberDynamicDetailState(
        dynamicDetailViewModel = dynamicDetailViewModel,
        commentViewModel = commentViewModel,
        imagePreviewerState = rememberPreviewerState(
            verticalDragType = VerticalDragType.UpAndDown,
            pageCount = { pictures.size },
            getKey = { pictures[it].key }
        )
    )

    val onShowPreviewer: (newPictures: List<Picture>, afterSetPictures: () -> Unit) -> Unit =
        { newPictures, afterSetPictures ->
            pictures.swapList(newPictures)
            afterSetPictures()
        }

    if (windowSizeClass.widthSizeClass != WindowWidthSizeClass.Expanded) {
        DynamicDetailMobileContent(
            modifier = modifier,
            dynamicDetailState = dynamicDetailState,
            onShowPreviewer = onShowPreviewer,
        )
    } else {
        DynamicDetailScreenPadContent(
            modifier = modifier,
            dynamicDetailState = dynamicDetailState,
            onShowPreviewer = onShowPreviewer,
        )
    }

    ImagePreviewer(
        modifier = Modifier
            .fillMaxSize(),
        state = dynamicDetailState.imagePreviewerState,
        imageLoader = { index ->
            val imageRequest = ImageRequest.Builder(context)
                .data(pictures[index].url)
                .size(Size.ORIGINAL)
                .build()
            rememberAsyncImagePainter(imageRequest)
        },
        previewerLayer = {
            foreground = { page ->
                ImagePreviewerActions(
                    saving = savingPreviewImage,
                    onClose = {
                        if (dynamicDetailState.imagePreviewerState.canClose) {
                            scope.launch {
                                dynamicDetailState.imagePreviewerState.closeTransform()
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
                                logger.warn(it) { "Save dynamic detail preview image failed" }
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
}

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun DynamicDetailMobileContent(
    modifier: Modifier = Modifier,
    dynamicDetailState: DynamicDetailState,
    onShowPreviewer: (newPictures: List<Picture>, afterSetPictures: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val logger = KotlinLogging.logger { }
    val screenHeight = with(density) { context.resources.displayMetrics.heightPixels.toDp() }

    var showMask by remember { mutableStateOf(false) }
    var showReplies by remember { mutableStateOf(false) }
    var replyDraftTarget by remember { mutableStateOf<DynamicReplyDraftTarget?>(null) }

    val openDynamicReplyInput = {
        replyDraftTarget = DynamicReplyDraftTarget(
            title = "评论动态",
            placeholder = "发一条友善的评论"
        )
    }
    val openRootReplyInput: (Comment) -> Unit = { comment ->
        replyDraftTarget = DynamicReplyDraftTarget(
            title = "回复 ${comment.member.name}",
            placeholder = "回复 @${comment.member.name}",
            root = comment.rpid,
            parent = comment.rpid
        )
    }
    val openChildReplyInput: (Comment, Long) -> Unit = { comment, root ->
        replyDraftTarget = DynamicReplyDraftTarget(
            title = "回复 ${comment.member.name}",
            placeholder = "回复 @${comment.member.name}",
            root = root,
            parent = comment.rpid
        )
    }

    val onRepliesCloseAnimationFinish: (Dp) -> Unit = { finishDp ->
        logger.fInfo { "onRepliesCloseAnimationFinish: $finishDp" }
        if (finishDp == screenHeight) {
            showReplies = false
            showMask = false
        }
    }

    var maskAlphaTarget by remember { mutableFloatStateOf(0.5f) }
    val maskAlpha by animateFloatAsState(
        targetValue = maskAlphaTarget,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "replies scrim mask alpha"
    )
    var repliesOffsetYTarget by remember { mutableStateOf(0.dp) }
    val repliesOffsetY by animateDpAsState(
        targetValue = repliesOffsetYTarget,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "replies offset y",
        finishedListener = onRepliesCloseAnimationFinish
    )
    var repliesScaleTarget by remember { mutableFloatStateOf(1f) }
    val repliesScale by animateFloatAsState(
        targetValue = repliesScaleTarget,
        label = "replies scale"
    )
    val repliesRoundCorner by animateDpAsState(
        targetValue = if (repliesScaleTarget == 1f) 0.dp else 28.dp,
        label = "replies round corner"
    )

    val onCloseReplies: () -> Unit = {
        maskAlphaTarget = 0f
        repliesOffsetYTarget = screenHeight
    }

    LaunchedEffect(showMask) {
        maskAlphaTarget = if (showMask) 0.5f else 0f
    }

    LaunchedEffect(showReplies) {
        repliesOffsetYTarget = if (showReplies) 0.dp else screenHeight
        if (showReplies) repliesScaleTarget = 1f
        showMask = showReplies
    }

    PredictiveBackHandler(showMask) { progress: Flow<BackEventCompat> ->
        runCatching {
            progress.collect { backEvent ->
                maskAlphaTarget = (1 - backEvent.progress * 0.8f) * 0.5f
                repliesOffsetYTarget = (backEvent.progress * 200).dp
                repliesScaleTarget = 1 - min(0.6f, backEvent.progress) * 0.2f
            }
            onCloseReplies()
        }.onFailure {
            maskAlphaTarget = 0.5f
            repliesOffsetYTarget = 0.dp
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("动态详情") },
                    navigationIcon = {
                        IconButton(onClick = dynamicDetailState.onExitActivity) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    }
                )
            },
            bottomBar = {
                dynamicDetailState.dynamicItem?.let {
                    DynamicDetailBottomBar(
                        liked = dynamicDetailState.isLiked,
                        likeCount = dynamicDetailState.likeCount,
                        likeEnabled = !dynamicDetailState.isLiking,
                        commentEnabled = Prefs.isLogin,
                        onReply = openDynamicReplyInput,
                        onShare = dynamicDetailState::shareDynamic,
                        onLike = dynamicDetailState::toggleLike
                    )
                }
            }
        ) { innerPadding ->
            if (dynamicDetailState.dynamicItem != null) {
                CommentPart(
                    modifier = Modifier.padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding()
                    ),
                    previewerState = dynamicDetailState.imagePreviewerState,
                    comments = dynamicDetailState.comments,
                    commentSort = dynamicDetailState.commentSort,
                    isLoading = dynamicDetailState.isLoadingComments,
                    isRefreshing = dynamicDetailState.isRefreshingComments,
                    onLoadMoreComments = dynamicDetailState::loadMoreComments,
                    onRefreshComments = dynamicDetailState::refreshComments,
                    onSwitchCommentSort = dynamicDetailState::switchCommentSort,
                    onShowPreviewer = onShowPreviewer,
                    onReplyComment = openRootReplyInput,
                    onShowReplies = { comment ->
                        dynamicDetailState.updateCurrentComment(comment)
                        dynamicDetailState.refreshReplies()
                        showReplies = true
                    },
                    header = {
                        DynamicPart(
                            modifier = Modifier,
                            dynamicItem = dynamicDetailState.dynamicItem,
                            previewerState = dynamicDetailState.imagePreviewerState,
                            articleParagraphs = dynamicDetailState.articleParagraphs,
                            onShowPreviewer = onShowPreviewer
                        )
                    }
                )
            } else {
                LoadingIndicator()
            }
        }

        // Dark mask
        if (showMask)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = maskAlpha))
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = { }
                    )
            ) {}

        // replies
        if (showReplies) {
            ReplyPart(
                modifier = Modifier
                    .offset(y = repliesOffsetY)
                    .scale(repliesScale)
                    .clip(
                        RoundedCornerShape(
                            topStart = repliesRoundCorner,
                            topEnd = repliesRoundCorner
                        )
                    ),
                comment = dynamicDetailState.replyComment,
                sort = dynamicDetailState.replySort,
                replies = dynamicDetailState.replies,
                previewerState = dynamicDetailState.imagePreviewerState,
                repliesCount = dynamicDetailState.replyComment?.repliesCount ?: 0,
                isLoading = dynamicDetailState.isLoadingReplies,
                isRefreshing = dynamicDetailState.isRefreshingReplies,
                onShowPreviewer = onShowPreviewer,
                onCloseReplies = onCloseReplies,
                onSwitchSort = dynamicDetailState::switchReplySort,
                onRefreshReplies = dynamicDetailState::refreshReplies,
                onLoadMoreReplies = dynamicDetailState::loadMoreReplies,
                onReplyComment = openChildReplyInput,
            )
        }
    }

    replyDraftTarget?.let { target ->
        DynamicReplyInputDialog(
            title = target.title,
            placeholder = target.placeholder,
            sending = dynamicDetailState.sendingComment,
            onDismiss = { replyDraftTarget = null },
            onSend = { message ->
                dynamicDetailState.sendComment(
                    message = message,
                    root = target.root,
                    parent = target.parent,
                    onSuccess = { replyDraftTarget = null }
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun DynamicDetailScreenPadContent(
    modifier: Modifier = Modifier,
    dynamicDetailState: DynamicDetailState,
    onShowPreviewer: (newPictures: List<Picture>, afterSetPictures: () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val screenWidth = with(density) { context.resources.displayMetrics.widthPixels.toDp() }

    var showReplies by remember { mutableStateOf(false) }
    var replyDraftTarget by remember { mutableStateOf<DynamicReplyDraftTarget?>(null) }
    val openDynamicReplyInput = {
        replyDraftTarget = DynamicReplyDraftTarget(
            title = "评论动态",
            placeholder = "发一条友善的评论"
        )
    }
    val openRootReplyInput: (Comment) -> Unit = { comment ->
        replyDraftTarget = DynamicReplyDraftTarget(
            title = "回复 ${comment.member.name}",
            placeholder = "回复 @${comment.member.name}",
            root = comment.rpid,
            parent = comment.rpid
        )
    }
    val openChildReplyInput: (Comment, Long) -> Unit = { comment, root ->
        replyDraftTarget = DynamicReplyDraftTarget(
            title = "回复 ${comment.member.name}",
            placeholder = "回复 @${comment.member.name}",
            root = root,
            parent = comment.rpid
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("动态详情") },
                navigationIcon = {
                    IconButton(onClick = dynamicDetailState.onExitActivity) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        },
        bottomBar = {
            dynamicDetailState.dynamicItem?.let {
                DynamicDetailBottomBar(
                    liked = dynamicDetailState.isLiked,
                    likeCount = dynamicDetailState.likeCount,
                    likeEnabled = !dynamicDetailState.isLiking,
                    commentEnabled = Prefs.isLogin,
                    onReply = openDynamicReplyInput,
                    onShare = dynamicDetailState::shareDynamic,
                    onLike = dynamicDetailState::toggleLike
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding()
            )
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center
            ) {
                DynamicPart(
                    modifier = Modifier
                        .width(screenWidth / 3 - 10.dp)
                        .verticalScroll(rememberScrollState()),
                    dynamicItem = dynamicDetailState.dynamicItem,
                    previewerState = dynamicDetailState.imagePreviewerState,
                    articleParagraphs = dynamicDetailState.articleParagraphs,
                    onShowPreviewer = onShowPreviewer
                )
                AnimatedVisibility(
                    visible = dynamicDetailState.dynamicItem != null,
                    enter = expandHorizontally(),
                    exit = shrinkHorizontally()
                ) {
                    CommentPart(
                        modifier = Modifier.width(screenWidth / 3 - 10.dp),
                        previewerState = dynamicDetailState.imagePreviewerState,
                        comments = dynamicDetailState.comments,
                        commentSort = dynamicDetailState.commentSort,
                        isLoading = dynamicDetailState.isLoadingComments,
                        isRefreshing = dynamicDetailState.isRefreshingComments,
                        onLoadMoreComments = dynamicDetailState::loadMoreComments,
                        onRefreshComments = dynamicDetailState::refreshComments,
                        onSwitchCommentSort = dynamicDetailState::switchCommentSort,
                        onShowPreviewer = onShowPreviewer,
                        onReplyComment = openRootReplyInput,
                        onShowReplies = { comment ->
                            dynamicDetailState.updateCurrentComment(comment)
                            dynamicDetailState.refreshReplies()
                            showReplies = true
                        }
                    )
                }
                AnimatedVisibility(
                    visible = showReplies,
                    enter = expandHorizontally(),
                    exit = shrinkHorizontally()
                ) {
                    ReplyPart(
                        modifier = Modifier.width(screenWidth / 3 - 10.dp),
                        comment = dynamicDetailState.replyComment,
                        sort = dynamicDetailState.replySort,
                        replies = dynamicDetailState.replies,
                        previewerState = dynamicDetailState.imagePreviewerState,
                        repliesCount = dynamicDetailState.replyComment?.repliesCount ?: 0,
                        isLoading = dynamicDetailState.isLoadingReplies,
                        isRefreshing = dynamicDetailState.isRefreshingReplies,
                        enableTopPadding = false,
                        onShowPreviewer = onShowPreviewer,
                        onCloseReplies = { showReplies = false },
                        onSwitchSort = dynamicDetailState::switchReplySort,
                        onRefreshReplies = dynamicDetailState::refreshReplies,
                        onLoadMoreReplies = dynamicDetailState::loadMoreReplies,
                        onReplyComment = openChildReplyInput,
                    )
                }
            }
        }
    }

    replyDraftTarget?.let { target ->
        DynamicReplyInputDialog(
            title = target.title,
            placeholder = target.placeholder,
            sending = dynamicDetailState.sendingComment,
            onDismiss = { replyDraftTarget = null },
            onSend = { message ->
                dynamicDetailState.sendComment(
                    message = message,
                    root = target.root,
                    parent = target.parent,
                    onSuccess = { replyDraftTarget = null }
                )
            }
        )
    }
}

@Composable
private fun DynamicDetailBottomBar(
    liked: Boolean,
    likeCount: Int,
    likeEnabled: Boolean,
    commentEnabled: Boolean,
    onReply: () -> Unit,
    onShare: () -> Unit,
    onLike: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DynamicReplyEntryBar(
                enabled = commentEnabled,
                onClick = onReply
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                DynamicDetailActionButton(
                    icon = Icons.Rounded.Share,
                    text = "分享",
                    onClick = onShare
                )
                DynamicDetailActionButton(
                    icon = if (liked) Icons.Rounded.ThumbUp else Icons.Outlined.ThumbUp,
                    text = formatDynamicStatCount(likeCount),
                    selected = liked,
                    enabled = likeEnabled,
                    onClick = onLike
                )
            }
        }
    }
}

@Composable
private fun DynamicReplyEntryBar(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        onClick = onClick,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.AutoMirrored.Filled.Comment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (enabled) "发一条友善的评论" else "登录后发表评论",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DynamicDetailActionButton(
    icon: ImageVector,
    text: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    TextButton(
        enabled = enabled,
        onClick = onClick
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = icon,
            contentDescription = null,
            tint = color
        )
        Text(
            modifier = Modifier.padding(start = 6.dp),
            text = text,
            color = color
        )
    }
}

@Composable
private fun DynamicReplyInputDialog(
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

private fun formatDynamicStatCount(value: Int): String {
    return when {
        value >= 100_000_000 -> "${value / 100_000_000}亿"
        value >= 10_000 -> String.format("%.1f万", value / 10_000.0)
        value > 0 -> value.toString()
        else -> "点赞"
    }
}

data class DynamicDetailState(
    val context: Context,
    val scope: CoroutineScope,
    val dynamicDetailViewModel: DynamicDetailViewModel,
    val commentViewModel: CommentViewModel,
    val imagePreviewerState: ImagePreviewerState
) {
    val dynamicItem get() = dynamicDetailViewModel.dynamicItem
    val articleParagraphs get() = dynamicDetailViewModel.articleParagraphs
    val comments get() = commentViewModel.comments
    val replies get() = commentViewModel.replies
    var replyComment by mutableStateOf<Comment?>(null)

    val isLiked get() = dynamicDetailViewModel.isLiked
    val likeCount get() = dynamicDetailViewModel.likeCount
    val isLiking get() = dynamicDetailViewModel.isLiking
    val sendingComment get() = commentViewModel.sendingComment
    val commentSort get() = commentViewModel.commentSort
    val replySort get() = commentViewModel.replySort
    val isRefreshingComments get() = commentViewModel.refreshingComments
    val isRefreshingReplies get() = commentViewModel.refreshingReplies
    val isLoadingComments get() = commentViewModel.updatingComments
    val isLoadingReplies get() = commentViewModel.updatingReplies
    val hasMoreComments get() = commentViewModel.hasMoreComments
    val hasMoreReplies get() = commentViewModel.hasMoreReplies

    fun loadMoreComments() {
        scope.launch(Dispatchers.IO) {
            commentViewModel.loadMoreComment()
        }
    }

    fun loadMoreReplies() {
        scope.launch(Dispatchers.IO) {
            commentViewModel.loadMoreReplies()
        }
    }

    fun updateCurrentComment(comment: Comment) {
        replyComment = comment
        commentViewModel.commentId = comment.oid
        commentViewModel.commentType = comment.type
        commentViewModel.rpid = comment.rpid
    }

    fun switchCommentSort(newSort: CommentSort) {
        scope.launch(Dispatchers.IO) {
            commentViewModel.switchCommentSort(newSort)
        }
    }

    fun switchReplySort(newSort: CommentSort) {
        scope.launch(Dispatchers.IO) {
            commentViewModel.switchReplySort(newSort)
        }
    }

    fun refreshComments() {
        scope.launch(Dispatchers.IO) {
            commentViewModel.refreshComments()
        }
    }

    fun refreshReplies() {
        scope.launch(Dispatchers.IO) {
            commentViewModel.refreshReplies()
        }
    }

    fun toggleLike() {
        dynamicDetailViewModel.toggleLike()
    }

    fun shareDynamic() {
        val item = dynamicItem ?: return
        val link = item.id
            ?.takeIf { it.isNotBlank() }
            ?.let { "https://t.bilibili.com/$it" }
            ?: item.jumpUrl
        if (link.isNullOrBlank()) {
            "当前动态没有可分享链接".toast(context)
        } else {
            shareText(context, link, "分享动态")
        }
    }

    fun sendComment(
        message: String,
        root: Long?,
        parent: Long?,
        onSuccess: () -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            val result = commentViewModel.sendComment(
                message = message,
                root = root,
                parent = parent
            )
            if (result.isSuccess) {
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            }
        }
    }

    val onExitActivity: () -> Unit = { (context as Activity).finish() }
}

@Composable
fun rememberDynamicDetailState(
    dynamicDetailViewModel: DynamicDetailViewModel,
    commentViewModel: CommentViewModel,
    imagePreviewerState: ImagePreviewerState
): DynamicDetailState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    BackHandler(imagePreviewerState.canClose || imagePreviewerState.animating) {
        if (imagePreviewerState.canClose) scope.launch {
            imagePreviewerState.closeTransform()
        }
    }

    LaunchedEffect(Unit) {
        val intent = (context as Activity).intent
        val dynamicId = intent.getStringExtra("dynamicId")
        dynamicId?.let { dynamicDetailViewModel.dynamicId = dynamicId } ?: context.finish()

        scope.launch(Dispatchers.IO) {
            dynamicDetailViewModel.loadDynamic()
            dynamicDetailViewModel.loadArticleContent()
            if (dynamicDetailViewModel.dynamicItem?.commentId != null && dynamicDetailViewModel.dynamicItem?.commentType != null) {
                commentViewModel.commentId = dynamicDetailViewModel.dynamicItem!!.commentId
                commentViewModel.commentType = dynamicDetailViewModel.dynamicItem!!.commentType
                //commentViewModel.loadMoreComment()
            }
        }
    }

    return remember(
        dynamicDetailViewModel,
        commentViewModel,
        imagePreviewerState
    ) {
        DynamicDetailState(
            context = context,
            scope = scope,
            dynamicDetailViewModel = dynamicDetailViewModel,
            commentViewModel = commentViewModel,
            imagePreviewerState = imagePreviewerState
        )
    }
}

@Composable
private fun DynamicPart(
    modifier: Modifier = Modifier,
    dynamicItem: DynamicItem?,
    previewerState: ImagePreviewerState,
    articleParagraphs: List<dev.aaa1115910.biliapi.entity.user.ArticleParagraph> = emptyList(),
    onShowPreviewer: (newPictures: List<Picture>, afterSetPictures: () -> Unit) -> Unit
) {
    Column(
        modifier = modifier
    ) {
        if (dynamicItem != null) {
            DynamicHeader(
                modifier = Modifier
                    .padding(12.dp),
                author = dynamicItem.author,
                dynamicItem = dynamicItem
            )
        }
        if (dynamicItem != null) {
            DynamicContent(
                modifier = Modifier
                    .padding(
                        bottom = WindowInsets.navigationBars.asPaddingValues()
                            .calculateBottomPadding()
                    ),
                dynamicItem = dynamicItem,
                previewerState = previewerState,
                articleParagraphs = articleParagraphs,
                onShowPreviewer = onShowPreviewer,
                onClick = { }
            )
        }
    }
}

@Composable
private fun CommentPart(
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    previewerState: ImagePreviewerState,
    comments: List<Comment>,
    commentSort: CommentSort,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onLoadMoreComments: () -> Unit,
    onRefreshComments: () -> Unit,
    onSwitchCommentSort: (CommentSort) -> Unit,
    onShowPreviewer: (newPictures: List<Picture>, afterSetPictures: () -> Unit) -> Unit,
    onReplyComment: (Comment) -> Unit,
    onShowReplies: (comment: Comment) -> Unit
) {
    Comments(
        modifier = modifier,
        header = header,
        previewerState = previewerState,
        comments = comments,
        commentSort = commentSort,
        isLoading = isLoading,
        isRefreshing = isRefreshing,
        onLoadMoreComments = onLoadMoreComments,
        onRefreshComments = onRefreshComments,
        onSwitchCommentSort = onSwitchCommentSort,
        onShowPreviewer = onShowPreviewer,
        onReplyComment = onReplyComment,
        onShowReplies = onShowReplies,
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ReplyPart(
    modifier: Modifier = Modifier,
    previewerState: ImagePreviewerState,
    comment: Comment?,
    sort: CommentSort,
    replies: List<Comment>,
    repliesCount: Int,
    isLoading: Boolean,
    isRefreshing: Boolean,
    enableTopPadding: Boolean = true,
    onSwitchSort: (CommentSort) -> Unit,
    onShowPreviewer: (List<Picture>, () -> Unit) -> Unit,
    onCloseReplies: () -> Unit,
    onRefreshReplies: () -> Unit,
    onLoadMoreReplies: () -> Unit,
    onReplyComment: (Comment, Long) -> Unit
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier,
                title = { Text("Replies") },
                navigationIcon = {
                    IconButton(onClick = onCloseReplies) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                windowInsets = if (enableTopPadding) TopAppBarDefaults.windowInsets
                else WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
            )
        }
    ) { innerPadding ->
        Replies(
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
            previewerState = previewerState,
            rootComment = comment,
            replySort = sort,
            replies = replies,
            repliesCount = repliesCount,
            isLoading = isLoading,
            isRefreshing = isRefreshing,
            onSwitchReplySort = onSwitchSort,
            onShowPreviewer = onShowPreviewer,
            onReplyComment = onReplyComment,
            onLoadMoreReplies = onLoadMoreReplies,
            onRefreshReplies = onRefreshReplies,
        )
    }
}
