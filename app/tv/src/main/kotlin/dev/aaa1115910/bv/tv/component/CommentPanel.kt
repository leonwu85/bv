package dev.aaa1115910.bv.tv.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.reply.Comment
import dev.aaa1115910.biliapi.entity.reply.CommentPage
import dev.aaa1115910.biliapi.entity.reply.CommentSort
import dev.aaa1115910.biliapi.repositories.CommentRepository
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.focusedBorder
import dev.aaa1115910.bv.util.isDpadDown
import dev.aaa1115910.bv.util.isDpadLeft
import dev.aaa1115910.bv.util.isKeyDown
import dev.aaa1115910.bv.util.onBackPressed
import dev.aaa1115910.bv.util.requestFocus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.getKoin

/**
 * 评论浮层组件
 *
 * @param show 是否显示浮层
 * @param oid 视频 aid
 * @param onHide 关闭浮层回调
 */
@Composable
fun CommentPanel(
    show: Boolean,
    oid: Long,
    onHide: () -> Unit
) {
    val commentRepository: CommentRepository = getKoin().get()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    val comments = remember { mutableStateListOf<Comment>() }
    var loading by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(CommentPage()) }
    var hasNext by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // 子评论浮窗状态
    var showSubCommentPanel by remember { mutableStateOf(false) }
    var selectedRootComment by remember { mutableStateOf<Comment?>(null) }
    var hasRequestedFocus by remember { mutableStateOf(false) }
    var wasSubCommentPanelShown by remember { mutableStateOf(false) }
    var selectedCommentIndex by remember { mutableStateOf(0) }
    var focusedCommentIndex by remember { mutableStateOf(0) }

    // 判断是否滚动到底部
    val isAtBottom by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == comments.size - 1
        }
    }

    // 加载评论
    val loadComments: (reset: Boolean) -> Unit = { reset ->
        scope.launch {
            if (loading) return@launch
            loading = true
            error = null

            try {
                val page = if (reset) CommentPage() else currentPage
                val data = commentRepository.getComments(
                    id = oid,
                    type = 1L, // 视频评论
                    sort = CommentSort.Hot,
                    page = page,
                    preferApiType = Prefs.apiType
                )

                if (reset) {
                    comments.clear()
                    comments.addAll(data.comments)
                } else {
                    comments.addAll(data.comments)
                }

                currentPage = data.nextPage
                hasNext = data.hasNext
            } catch (e: Exception) {
                error = e.message ?: "加载失败"
            } finally {
                loading = false
            }
        }
    }

    // 显示时加载评论
    LaunchedEffect(show, oid) {
        if (show && comments.isEmpty()) {
            loadComments(true)
        }
        if (!show) {
            hasRequestedFocus = false
            wasSubCommentPanelShown = false
        }
    }

    // 显示后请求焦点（初次显示时或子评论浮窗关闭后）
    LaunchedEffect(show, showSubCommentPanel, comments.isNotEmpty()) {
        if (show && !showSubCommentPanel && comments.isNotEmpty()) {
            // 子评论浮窗刚关闭，需要恢复焦点到之前点击的评论
            if (wasSubCommentPanelShown) {
                delay(300) // 等待动画完成
                listState.scrollToItem(selectedCommentIndex)
                delay(100)
                focusRequester.requestFocus(scope)
                wasSubCommentPanelShown = false
            }
            // 初次显示父评论浮窗，请求焦点
            else if (!hasRequestedFocus) {
                delay(300) // 等待动画和渲染完成
                focusRequester.requestFocus(scope)
                hasRequestedFocus = true
            }
        }
        // 记录子评论浮窗显示状态
        if (showSubCommentPanel) {
            wasSubCommentPanelShown = true
        }
    }

    // 懒加载：滚动到底部时加载更多
    LaunchedEffect(isAtBottom, hasNext, loading) {
        if (isAtBottom && hasNext && !loading && comments.isNotEmpty()) {
            loadComments(false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onHide),
        contentAlignment = Alignment.CenterEnd
    ) {
        AnimatedVisibility(
            visible = show && !showSubCommentPanel,
            enter = expandHorizontally(expandFrom = Alignment.End),
            exit = shrinkHorizontally(shrinkTowards = Alignment.End)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .widthIn(min = 300.dp, max = 400.dp)
                    .fillMaxWidth(0.3f)
                    .clickable(enabled = true, onClick = {}) // 阻止点击穿透
                    .onBackPressed { onHide() },
                colors = SurfaceDefaults.colors(
                    containerColor = Color.Black.copy(alpha = 0.95f)
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 标题栏
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "评论",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = "左键返回顶部",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                        Text(
                            text = if (comments.isNotEmpty()) "${comments.size} 条" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    // 评论列表
                    if (error != null) {
                        Text(
                            text = error ?: "加载失败",
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else if (comments.isEmpty() && !loading) {
                        Text(
                            text = "暂无评论",
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onPreviewKeyEvent { event ->
                                    when {
                                        // 左键返回顶部
                                        event.isKeyDown() && event.isDpadLeft() -> {
                                            scope.launch {
                                                listState.scrollToItem(0)
                                                // 滚动后重新请求焦点，使焦点移到第一条评论
                                                delay(100)
                                                focusRequester.requestFocus(scope)
                                            }
                                            true
                                        }
                                        // 下键：检查当前评论是否已完全可见
                                        event.isKeyDown() && event.isDpadDown() -> {
                                            val currentItemInfo = listState.layoutInfo.visibleItemsInfo
                                                .firstOrNull { it.index == focusedCommentIndex }

                                            if (currentItemInfo != null) {
                                                val viewportEnd = listState.layoutInfo.viewportEndOffset
                                                val itemBottom = currentItemInfo.offset + currentItemInfo.size
                                                val amountToScroll = itemBottom - viewportEnd

                                                // 如果评论底部不可见，精确滚动使其可见
                                                if (amountToScroll > 0) {
                                                    scope.launch {
                                                        listState.animateScrollBy(amountToScroll.toFloat())
                                                    }
                                                    true // 拦截事件
                                                } else {
                                                    false // 允许焦点转移
                                                }
                                            } else {
                                                false
                                            }
                                        }
                                        else -> false
                                    }
                                },
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(
                                items = comments,
                                key = { _, it -> it.rpid }
                            ) { index, comment ->
                                CommentItem(
                                    comment = comment,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { focusState ->
                                            if (focusState.hasFocus) {
                                                focusedCommentIndex = index
                                            }
                                        },
                                    onClick = {
                                        // 只有有子评论时才能点击打开子评论浮窗
                                        if (comment.repliesCount > 0) {
                                            selectedCommentIndex = index
                                            selectedRootComment = comment
                                            showSubCommentPanel = true
                                        }
                                    }
                                )
                            }

                            // 加载状态
                            if (loading) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        LoadingTip()
                                    }
                                }
                            }

                            // 没有更多了
                            if (!hasNext && comments.isNotEmpty()) {
                                item {
                                    Text(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        text = "没有更多评论了",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    // 底部提示
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    // 子评论浮窗
    if (selectedRootComment != null) {
        SubCommentPanel(
            show = showSubCommentPanel,
            oid = oid,
            rootId = selectedRootComment!!.rpid,
            rootComment = selectedRootComment!!,
            onHide = {
                showSubCommentPanel = false
                selectedRootComment = null
            }
        )
    }
}
