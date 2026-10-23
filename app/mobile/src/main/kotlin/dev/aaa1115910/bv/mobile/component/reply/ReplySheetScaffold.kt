package dev.aaa1115910.bv.mobile.component.reply

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.origeek.imageViewer.previewer.ImagePreviewerState
import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.biliapi.entity.reply.Comment
import dev.aaa1115910.biliapi.entity.reply.CommentReplyPage
import dev.aaa1115910.biliapi.entity.reply.CommentSort
import dev.aaa1115910.biliapi.repositories.VideoDetailRepository
import dev.aaa1115910.bv.mobile.settings.MobilePrefs
import dev.aaa1115910.bv.util.OnBottomReached
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.getKoin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplySheetScaffold(
    modifier: Modifier = Modifier,
    aid: Long,
    description: String = "",
    rpid: Long,
    repliesCount: Int,
    sheetState: BottomSheetScaffoldState,
    previewerState: ImagePreviewerState,
    onShowPreviewer: (newPictures: List<Picture>, afterSetPictures: () -> Unit) -> Unit,
    onReplyComment: (Comment, Long) -> Unit = { _, _ -> },
    videoDetailRepository: VideoDetailRepository = getKoin().get(),
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val logger = KotlinLogging.logger("ReplySheetScaffold")

    var nextPage by remember { mutableStateOf(CommentReplyPage()) }
    var comment by remember { mutableStateOf<Comment?>(null) }
    val replies = remember { mutableStateListOf<Comment>() }
    val visibleComments = dev.aaa1115910.bv.util.rememberFilteredComments(replies, description)
    var sort by remember { mutableStateOf(CommentSort.Time) }
    var hasNext by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }

    val loadMoreReply = {
        if (hasNext && !loading) {
            loading = true
            logger.info { "load more reply: [aid=$aid, rpid=$rpid, next=$nextPage]" }
            scope.launch(Dispatchers.IO) {
                runCatching {
                    val commentRepliesData = videoDetailRepository.getCommentReplies(
                        aid = aid,
                        commentId = rpid,
                        page = nextPage,
                        sort = sort,
                        preferApiType = MobilePrefs.apiType
                    )
                    hasNext = commentRepliesData.hasNext
                    nextPage = commentRepliesData.nextPage
                    if (comment == null) comment = commentRepliesData.rootComment
                    replies.addAll(commentRepliesData.replies)
                }.onFailure {
                    it.printStackTrace()
                    hasNext = false
                }
                loading = false
            }
        }
    }

    val clearData = {
        hasNext = true
        comment = null
        replies.clear()
        nextPage = CommentReplyPage()
    }

    val sheetExpanded by remember(sheetState) {
        derivedStateOf {
            sheetState.bottomSheetState.currentValue == SheetValue.Expanded
        }
    }

    if (sheetExpanded) {
        listState.OnBottomReached(loading = loading) {
            loadMoreReply()
        }
    }

    val switchCommentSort: (CommentSort) -> Unit = { newSort ->
        sort = newSort
        clearData()
        loadMoreReply()
    }

    LaunchedEffect(rpid) {
        clearData()
    }

    LaunchedEffect(sheetState.bottomSheetState.currentValue) {
        when (sheetState.bottomSheetState.currentValue) {
            SheetValue.Hidden, SheetValue.PartiallyExpanded -> clearData()
            SheetValue.Expanded -> {}//loadMoreReply()
        }
    }

    BackHandler(
        sheetState.bottomSheetState.currentValue == SheetValue.Expanded &&
                !(previewerState.canClose || previewerState.animating)
    ) {
        scope.launch { sheetState.bottomSheetState.hide() }
    }

    ReplySheetLayout(
        modifier = modifier,
        sheetState = sheetState,
        sheetContent = {
            LazyColumn(
                modifier = Modifier.fillMaxHeight(),
                state = listState
            ) {
                if (comment != null) {
                    item {
                        CommentItem(
                            comment = comment!!,
                            previewerState = previewerState,
                            onShowPreviewer = onShowPreviewer,
                            showReplies = false,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            onReply = { onReplyComment(it, it.rpid) }
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
                            text = "相关回复共 $repliesCount 条",
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(onClick = {
                            switchCommentSort(
                                when (sort) {
                                    CommentSort.Hot -> CommentSort.Time
                                    CommentSort.Time -> CommentSort.Hot
                                    else -> CommentSort.Hot
                                }
                            )
                        }) {
                            Text(
                                text = when (sort) {
                                    CommentSort.Hot -> "按热度"
                                    CommentSort.Time -> "按时间"
                                    else -> ""
                                }
                            )
                        }
                    }
                }

                items(items = visibleComments) { reply ->
                    CommentItem(
                        comment = reply,
                        previewerState = previewerState,
                        onShowPreviewer = onShowPreviewer,
                        showReplies = false,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        onReply = { onReplyComment(it, rpid) }
                    )
                }

                if (replies.size > visibleComments.size && hasNext && !loading) {
                    item {
                        TextButton(onClick = { loadMoreReply() }, modifier = Modifier.fillMaxWidth()) {
                            Text("部分回复已过滤 · 继续加载")
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }
    ) {
        content()
    }
}

/** Replies are either hidden or fully expanded; there is no persistent peek content. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberReplySheetScaffoldState(): BottomSheetScaffoldState =
    rememberBottomSheetScaffoldState(
        bottomSheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReplySheetLayout(
    sheetState: BottomSheetScaffoldState,
    modifier: Modifier = Modifier,
    sheetContent: @Composable ColumnScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    BottomSheetScaffold(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clip(
                MaterialTheme.shapes.extraLarge.copy(
                    bottomStart = CornerSize(0.dp),
                    bottomEnd = CornerSize(0.dp)
                )
            ),
        scaffoldState = sheetState,
        // A PartiallyExpanded state makes Material3's snackbar placement read requireOffset()
        // before anchors exist during layout changes. Hidden/Expanded avoids that path and
        // also gives an empty layout a valid Hidden anchor; a 1.dp peek cannot guarantee this.
        sheetPeekHeight = 0.dp,
        sheetContent = sheetContent,
    ) {
        content()
    }
}
