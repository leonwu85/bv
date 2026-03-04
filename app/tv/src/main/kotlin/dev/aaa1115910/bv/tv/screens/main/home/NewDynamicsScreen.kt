package dev.aaa1115910.bv.tv.screens.main.home

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.user.DynamicItem
import dev.aaa1115910.biliapi.entity.user.DynamicType
import dev.aaa1115910.biliapi.entity.user.DynamicVideo
import dev.aaa1115910.bv.entity.DynamicTabType
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.tv.R
import dev.aaa1115910.bv.tv.activities.user.FollowActivity
import dev.aaa1115910.bv.tv.activities.video.SeasonInfoActivity
import dev.aaa1115910.bv.tv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.tv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.tv.activities.dynamic.DynamicDetailActivity
import dev.aaa1115910.bv.tv.component.LoadingTip
import dev.aaa1115910.bv.tv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.tv.util.ProvideListBringIntoViewSpec
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.viewmodel.home.DynamicViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun NewDynamicsScreen(
    modifier: Modifier = Modifier,
    lazyGridState: LazyGridState = rememberLazyGridState(),
    initialSelectedTabIndex: Int = 0,
    onSelectedTabChanged: (Int) -> Unit = {},
    dynamicViewModel: DynamicViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val tabRowFocusRequester = remember { FocusRequester() }
    var selectedTabIndex by remember { mutableIntStateOf(initialSelectedTabIndex) }
    var currentFocusedIndex by remember { mutableIntStateOf(-1) }
    var contentHasFocus by remember { mutableStateOf(false) }

    // 当子 Tab 切换时通知父组件记住选择
    LaunchedEffect(selectedTabIndex) {
        onSelectedTabChanged(selectedTabIndex)
    }

    val selectedTabType = DynamicTabType.entries[selectedTabIndex]

    // 根据选中的 Tab 获取对应的数据列表
    val currentList: List<DynamicItem> = when (selectedTabType) {
        DynamicTabType.All -> dynamicViewModel.dynamicAllList
        DynamicTabType.Video -> emptyList() // 视频标签页单独处理
        DynamicTabType.Pgc -> dynamicViewModel.dynamicPgcList
        DynamicTabType.Article -> dynamicViewModel.dynamicArticleList
    }

    // 获取当前标签的加载状态
    val (isLoading, hasMore) = when (selectedTabType) {
        DynamicTabType.All -> dynamicViewModel.loadingAll to dynamicViewModel.allHasMore
        DynamicTabType.Video -> dynamicViewModel.loadingVideo to dynamicViewModel.videoHasMore
        DynamicTabType.Pgc -> dynamicViewModel.loadingPgc to dynamicViewModel.pgcHasMore
        DynamicTabType.Article -> dynamicViewModel.loadingArticle to dynamicViewModel.articleHasMore
    }

    val showTip = (selectedTabType == DynamicTabType.Video && dynamicViewModel.dynamicVideoList.isNotEmpty() || currentList.isNotEmpty()) && currentFocusedIndex >= 0

    // 视频点击处理器
    val onClickVideo: (DynamicVideo) -> Unit = { video ->
        VideoInfoActivity.actionStart(
            context = context,
            aid = video.aid,
            proxyArea = ProxyArea.checkProxyArea(video.title)
        )
    }

    val onLongClickVideo: (DynamicVideo) -> Unit = { video ->
        UpInfoActivity.actionStart(
            context,
            mid = video.authorId,
            name = video.author,
            face = video.authorFace
        )
    }

    val onClickDynamicItem: (DynamicItem) -> Unit = { dynamic ->
        when (dynamic.type) {
            DynamicType.Av -> {
                dynamic.video?.let { video ->
                    VideoInfoActivity.actionStart(
                        context = context,
                        aid = video.aid,
                        proxyArea = ProxyArea.checkProxyArea(dynamic.video?.title ?: "")
                    )
                }
            }
            DynamicType.Pgc -> {
                dynamic.pgc?.let { pgc ->
                    SeasonInfoActivity.actionStart(
                        context = context,
                        epId = pgc.epid,
                        seasonId = pgc.seasonId
                    )
                }
            }
            else -> {
                // 其他类型跳转到动态详情页
                dynamic.id?.let { dynamicId ->
                    DynamicDetailActivity.actionStart(context, dynamicId)
                }
            }
        }
    }

    val onLongClickDynamicItem: (DynamicItem) -> Unit = { dynamic ->
        UpInfoActivity.actionStart(
            context,
            mid = dynamic.author.mid,
            name = dynamic.author.author,
            face = dynamic.author.avatar
        )
    }

    // 获取当前列表大小（用于加载更多判断）
    val currentListSize = if (selectedTabType == DynamicTabType.Video) {
        dynamicViewModel.dynamicVideoList.size
    } else {
        currentList.size
    }

    // 加载更多
    LaunchedEffect(currentListSize, currentFocusedIndex, selectedTabType) {
        val needLoadMore = currentListSize > 0 && currentFocusedIndex + 12 > currentListSize
        if (needLoadMore && hasMore && !isLoading) {
            scope.launch(Dispatchers.IO) {
                dynamicViewModel.loadMoreByType(selectedTabType)
            }
        }
    }

    // 当 Tab 切换时加载数据
    LaunchedEffect(selectedTabType) {
        val isEmpty = if (selectedTabType == DynamicTabType.Video) {
            dynamicViewModel.dynamicVideoList.isEmpty()
        } else {
            currentList.isEmpty()
        }
        if (isEmpty && hasMore) {
            scope.launch(Dispatchers.IO) {
                dynamicViewModel.loadMoreByType(selectedTabType)
            }
        }
    }

    // 在内容区域按返回键时，先将焦点给到子 TabRow
    BackHandler(contentHasFocus) {
        tabRowFocusRequester.requestFocus()
    }

    if (dynamicViewModel.isLogin) {
        Column(modifier = modifier.fillMaxSize()) {
            // Tab Row
            DynamicTabRow(
                tabRowFocusRequester = tabRowFocusRequester,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { index ->
                    selectedTabIndex = index
                    currentFocusedIndex = -1
                }
            )

            // Content
            if (showTip) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(x = (-20).dp, y = (-8).dp)
                        .padding(top = 8.dp, end = 24.dp),
                    text = stringResource(R.string.entry_follow_screen),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.End
                )
            }

            // 根据选中的 Tab 显示不同的布局
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onFocusChanged { contentHasFocus = it.hasFocus }
            ) {
            when (selectedTabType) {
                DynamicTabType.Video -> {
                    // 视频页使用旧的 grid 布局和 SmallVideoCard
                    VideoDynamicContent(
                        videoList = dynamicViewModel.dynamicVideoList,
                        onClickVideo = onClickVideo,
                        onLongClickVideo = onLongClickVideo,
                        onFocus = { currentFocusedIndex = it },
                        isLoading = isLoading,
                        hasMore = hasMore
                    )
                }
                else -> {
                    // 其他页面使用瀑布流布局
                    StaggeredDynamicContent(
                        filteredList = currentList,
                        onClickDynamicItem = onClickDynamicItem,
                        onLongClickDynamicItem = onLongClickDynamicItem,
                        onFocus = { currentFocusedIndex = it },
                        isLoading = isLoading,
                        hasMore = hasMore
                    )
                }
            }
            }
        }
    } else {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "请先登录")
        }
    }
}

@Composable
private fun DynamicTabRow(
    modifier: Modifier = Modifier,
    tabRowFocusRequester: FocusRequester,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        delay(100)
        tabRowFocusRequester.requestFocus()
    }

    TabRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .focusRestorer(tabRowFocusRequester),
        selectedTabIndex = selectedTabIndex
    ) {
        DynamicTabType.entries.forEachIndexed { index, tab ->
            Tab(
                modifier = if (index == selectedTabIndex) Modifier.focusRequester(tabRowFocusRequester) else Modifier,
                selected = index == selectedTabIndex,
                onFocus = { onTabSelected(index) },
                onClick = { onTabSelected(index) }
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    text = tab.getDisplayName(context),
                    color = LocalContentColor.current,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

// 视频页内容 - 使用旧的 Grid 布局和 SmallVideoCard
@Composable
private fun VideoDynamicContent(
    videoList: List<DynamicVideo>,
    onClickVideo: (DynamicVideo) -> Unit,
    onLongClickVideo: (DynamicVideo) -> Unit,
    onFocus: (Int) -> Unit,
    isLoading: Boolean,
    hasMore: Boolean
) {
    val context = LocalContext.current
    val padding = dimensionResource(R.dimen.grid_padding) / 2
    val spacedBy = dimensionResource(R.dimen.grid_spacedBy) / 2
    val gridColumns = Prefs.gridColumns

    ProvideListBringIntoViewSpec {
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged {
                    if (!it.isFocused) {
                        onFocus(-1)
                    }
                }
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyUp && it.key == Key.Menu) {
                        context.startActivity(Intent(context, FollowActivity::class.java))
                        return@onPreviewKeyEvent true
                    }
                    false
                },
            columns = GridCells.Fixed(gridColumns),
            contentPadding = PaddingValues(padding),
            verticalArrangement = Arrangement.spacedBy(spacedBy),
            horizontalArrangement = Arrangement.spacedBy(spacedBy)
        ) {
            itemsIndexed(videoList) { index, video ->
                SmallVideoCard(
                    data = remember(video.aid) {
                        VideoCardData(
                            avid = video.aid,
                            title = video.title,
                            cover = video.cover,
                            playString = video.play.toString(),
                            danmakuString = video.danmaku.toString(),
                            upName = video.author,
                            timeString = video.duration.toString(),
                            pubTime = video.pubTime
                        )
                    },
                    onClick = { onClickVideo(video) },
                    onLongClick = { onLongClickVideo(video) },
                    onFocus = { onFocus(index) }
                )
            }

            if (isLoading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingTip()
                    }
                }
            }

            if (!hasMore && videoList.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        text = "没有更多了捏",
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// 其他页面内容 - 使用瀑布流布局
@Composable
private fun StaggeredDynamicContent(
    filteredList: List<DynamicItem>,
    onClickDynamicItem: (DynamicItem) -> Unit,
    onLongClickDynamicItem: (DynamicItem) -> Unit,
    onFocus: (Int) -> Unit,
    isLoading: Boolean,
    hasMore: Boolean
) {
    val context = LocalContext.current
    val staggeredGridState = rememberLazyStaggeredGridState()

    ProvideListBringIntoViewSpec {
        LazyVerticalStaggeredGrid(
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged {
                    if (!it.isFocused) {
                        onFocus(-1)
                    }
                }
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyUp && it.key == Key.Menu) {
                        context.startActivity(Intent(context, FollowActivity::class.java))
                        return@onPreviewKeyEvent true
                    }
                    false
                },
            columns = StaggeredGridCells.Fixed(2),
            state = staggeredGridState,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalItemSpacing = 16.dp,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(filteredList) { index, item ->
                StaggeredDynamicCard(
                    dynamicItem = item,
                    onClick = { onClickDynamicItem(item) },
                    onLongClick = { onLongClickDynamicItem(item) },
                    onFocus = { onFocus(index) }
                )
            }

            if (isLoading) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingTip()
                    }
                }
            }

            if (!hasMore && filteredList.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        text = "没有更多了捏",
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// 瀑布流卡片
@Composable
private fun StaggeredDynamicCard(
    modifier: Modifier = Modifier,
    dynamicItem: DynamicItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocus: () -> Unit
) {
    var isFocused by remember { mutableIntStateOf(0) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged {
                if (it.isFocused) {
                    isFocused = 1
                    onFocus()
                } else {
                    isFocused = 0
                }
            },
        onClick = onClick,
        onLongClick = onLongClick,
        shape = CardDefaults.shape(RoundedCornerShape(8.dp)),
        colors = CardDefaults.colors(
            containerColor = if (isFocused == 1) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        scale = CardDefaults.scale(focusedScale = 1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 作者信息头部
            DynamicCardHeader(author = dynamicItem.author)

            // 根据类型显示不同内容
            when (dynamicItem.type) {
                DynamicType.Av -> DynamicVideoContent(video = dynamicItem.video)
                DynamicType.Pgc -> DynamicPgcContent(pgc = dynamicItem.pgc)
                DynamicType.Draw -> DynamicDrawContent(draw = dynamicItem.draw)
                DynamicType.Word -> DynamicWordContent(word = dynamicItem.word)
                DynamicType.Article -> DynamicArticleContent(article = dynamicItem.article)
                DynamicType.Forward -> DynamicForwardContent(
                    word = dynamicItem.word,
                    orig = dynamicItem.orig
                )
                else -> DynamicUnknownContent(type = dynamicItem.type)
            }
        }
    }
}

@Composable
private fun DynamicCardHeader(
    author: DynamicItem.DynamicAuthorModule
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 头像
        AsyncImage(
            model = author.avatar,
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
            contentScale = ContentScale.Crop
        )
        Column {
            Text(
                text = author.author,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = author.pubTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DynamicVideoContent(video: DynamicItem.DynamicVideoModule?) {
    video ?: return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 封面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            AsyncImage(
                model = video.cover,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // 底部渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                        )
                    )
            )
            // 时长和播放信息
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = video.duration,
                    color = Color.White,
                    fontSize = 12.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (video.play.isNotBlank()) {
                        Text(
                            text = video.play,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                    if (video.danmaku.isNotBlank()) {
                        Text(
                            text = video.danmaku,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        // 标题
        Text(
            text = video.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // 描述文字
        if (video.text.isNotBlank()) {
            Text(
                text = video.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DynamicPgcContent(pgc: DynamicItem.DynamicPgcModule?) {
    pgc ?: return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 封面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            AsyncImage(
                model = pgc.cover,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // 番剧标签
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        Color(0xFFFB7299).copy(alpha = 0.9f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "番剧",
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
        }
        // 标题
        Text(
            text = pgc.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DynamicDrawContent(draw: DynamicItem.DynamicDrawModule?) {
    draw ?: return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 标题
        val title = draw.title
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // 图片 - 瀑布流中图片纵向排列
        if (draw.images.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                draw.images.take(3).forEach { image ->
                    AsyncImage(
                        model = image.url,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                if (draw.images.size > 3) {
                    Text(
                        text = "+${draw.images.size - 3} 张图片",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
        // 文字
        if (draw.text.isNotBlank()) {
            Text(
                text = draw.text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DynamicWordContent(word: DynamicItem.DynamicWordModule?) {
    word ?: return
    if (word.text.isNotBlank()) {
        Text(
            text = word.text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DynamicArticleContent(article: DynamicItem.DynamicArticleModule?) {
    article ?: return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 标题
        Text(
            text = article.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // 封面
        if (article.covers.isNotEmpty()) {
            AsyncImage(
                model = article.covers.first(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
        }
        // 摘要
        if (article.text.isNotBlank()) {
            Text(
                text = article.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        // 专栏标签
        Box(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = article.label.ifBlank { "专栏" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun DynamicForwardContent(
    word: DynamicItem.DynamicWordModule?,
    orig: DynamicItem?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 转发文字
        if (word != null && word.text.isNotBlank()) {
            Text(
                text = word.text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        // 原动态
        if (orig != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // 原作者
                    if (orig.author.mid > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AsyncImage(
                                model = orig.author.avatar,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                text = orig.author.author,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                    // 原内容
                    when (orig.type) {
                        DynamicType.Av -> orig.video?.let {
                            Text(
                                text = it.title,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DynamicType.Word -> orig.word?.let {
                            Text(
                                text = it.text,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DynamicType.Draw -> orig.draw?.let {
                            val drawTitle = it.title
                            Text(
                                text = it.text.ifBlank { drawTitle ?: "图文动态" },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DynamicType.Article -> orig.article?.let {
                            Text(
                                text = it.title,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        else -> Text(
                            text = "原动态",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicUnknownContent(type: DynamicType) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "暂不支持的动态类型: ${type.name}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}
