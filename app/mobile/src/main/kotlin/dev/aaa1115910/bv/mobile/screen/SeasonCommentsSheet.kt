package dev.aaa1115910.bv.mobile.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.origeek.imageViewer.previewer.ImagePreviewer
import com.origeek.imageViewer.previewer.VerticalDragType
import com.origeek.imageViewer.previewer.rememberPreviewerState
import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.biliapi.entity.video.season.Episode
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.mobile.component.ImagePreviewerActions
import dev.aaa1115910.bv.mobile.component.reply.Comments
import dev.aaa1115910.bv.mobile.component.reply.ReplySheetScaffold
import dev.aaa1115910.bv.mobile.util.saveImageToGallery
import dev.aaa1115910.bv.util.swapList
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.CommentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val CommentAccent = Color(0xFFFF6699)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SeasonCommentsSheet(
    seasonTitle: String,
    episodes: List<Episode>,
    initialEpisodeId: Int?,
    commentViewModel: CommentViewModel,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val availableEpisodes = remember(episodes) {
        episodes.filter { it.aid > 0L }
    }
    if (availableEpisodes.isEmpty()) return
    val initialEpisode = remember(availableEpisodes, initialEpisodeId) {
        availableEpisodes.findEpisode(initialEpisodeId) ?: availableEpisodes.first()
    }
    var selectedEpisodeId by rememberSaveable(availableEpisodes, initialEpisodeId) {
        mutableStateOf(initialEpisode.id)
    }
    val selectedEpisode = availableEpisodes.findEpisode(selectedEpisodeId) ?: initialEpisode
    val modalSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val replyBottomSheetState = rememberBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        enabledValues = setOf(
            SheetValue.Hidden,
            SheetValue.PartiallyExpanded,
            SheetValue.Expanded
        )
    )
    val replySheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = replyBottomSheetState
    )

    val pictures = remember { mutableStateListOf<Picture>() }
    val previewerState = rememberPreviewerState(
        verticalDragType = VerticalDragType.UpAndDown,
        pageCount = { pictures.size },
        getKey = { pictures[it].key }
    )
    var savingPreviewImage by remember { mutableStateOf(false) }
    val onShowPreviewer: (List<Picture>, () -> Unit) -> Unit =
        { newPictures, afterSetPictures ->
            pictures.swapList(newPictures)
            afterSetPictures()
        }

    LaunchedEffect(selectedEpisode.aid) {
        commentViewModel.setCommentTarget(
            commentId = selectedEpisode.aid,
            commentType = 1
        )
        withContext(Dispatchers.IO) {
            commentViewModel.loadMoreComment()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = modalSheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = seasonTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${stringResource(R.string.season_info_comments)} · ${
                        buildEpisodeTitle(selectedEpisode)
                    }",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 10.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = availableEpisodes,
                    key = { "comment-episode:${it.id}:${it.aid}" }
                ) { episode ->
                    CommentEpisodeChip(
                        text = resumeEpisodeLabel(
                            progressIndex = null,
                            episodeTitle = episode.title
                        ),
                        selected = episode.sameEpisodeAs(selectedEpisode),
                        onClick = { selectedEpisodeId = episode.id }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                key(selectedEpisode.aid) {
                    ReplySheetScaffold(
                        modifier = Modifier.fillMaxSize(),
                        aid = selectedEpisode.aid,
                        rpid = commentViewModel.rpid,
                        repliesCount = commentViewModel.rpCount,
                        sheetState = replySheetState,
                        previewerState = previewerState,
                        onShowPreviewer = onShowPreviewer,
                        onReplyComment = { _, _ ->
                            context.getString(R.string.season_info_comment_reply_hint)
                                .toast(context)
                        }
                    ) {
                        Comments(
                            modifier = Modifier.fillMaxSize(),
                            previewerState = previewerState,
                            comments = commentViewModel.comments,
                            vote = commentViewModel.commentVote,
                            commentSort = commentViewModel.commentSort,
                            isLoading = commentViewModel.updatingComments,
                            isRefreshing = commentViewModel.refreshingComments,
                            onLoadMoreComments = {
                                scope.launch(Dispatchers.IO) {
                                    commentViewModel.loadMoreComment()
                                }
                            },
                            onRefreshComments = {
                                scope.launch(Dispatchers.IO) {
                                    commentViewModel.refreshComments()
                                }
                            },
                            onSwitchCommentSort = { sort ->
                                scope.launch(Dispatchers.IO) {
                                    commentViewModel.switchCommentSort(sort)
                                }
                            },
                            onShowPreviewer = onShowPreviewer,
                            onReplyComment = {
                                context.getString(R.string.season_info_comment_reply_hint)
                                    .toast(context)
                            },
                            onShowReplies = { comment ->
                                commentViewModel.rpid = comment.rpid
                                commentViewModel.rpCount = comment.repliesCount
                                scope.launch {
                                    replySheetState.bottomSheetState.expand()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    BackHandler(previewerState.canClose || previewerState.animating) {
        if (previewerState.canClose) {
            scope.launch { previewerState.closeTransform() }
        }
    }

    ImagePreviewer(
        modifier = Modifier.fillMaxSize(),
        state = previewerState,
        imageLoader = { index ->
            val request = ImageRequest.Builder(context)
                .data(pictures[index].url)
                .size(Size.ORIGINAL)
                .build()
            rememberAsyncImagePainter(request)
        },
        previewerLayer = {
            foreground = { page ->
                ImagePreviewerActions(
                    saving = savingPreviewImage,
                    onClose = {
                        if (previewerState.canClose) {
                            scope.launch { previewerState.closeTransform() }
                        }
                    },
                    onSave = {
                        val picture = pictures.getOrNull(page) ?: return@ImagePreviewerActions
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

@Composable
private fun CommentEpisodeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = if (selected) {
            CommentAccent.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        contentColor = if (selected) CommentAccent else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) CommentAccent else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1
        )
    }
}
