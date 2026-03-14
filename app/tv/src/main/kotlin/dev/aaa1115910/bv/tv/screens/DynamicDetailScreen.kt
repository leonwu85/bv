package dev.aaa1115910.bv.tv.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.LocalContentColor
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.biliapi.entity.user.ArticleParagraph
import dev.aaa1115910.biliapi.entity.user.DynamicItem
import dev.aaa1115910.biliapi.entity.user.DynamicType
import dev.aaa1115910.bv.component.DynamicRichText
import dev.aaa1115910.bv.tv.activities.video.SeasonInfoActivity
import dev.aaa1115910.bv.tv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.tv.component.ArticleContent
import dev.aaa1115910.bv.tv.component.CommentPanel
import dev.aaa1115910.bv.util.fInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import dev.aaa1115910.bv.viewmodel.DynamicDetailViewModel

private val logger = KotlinLogging.logger {}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicDetailScreen(
    modifier: Modifier = Modifier,
    dynamicId: String,
    dynamicDetailViewModel: DynamicDetailViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var dynamicItem by remember { mutableStateOf<DynamicItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showCommentPanel by remember { mutableStateOf(false) }

    LaunchedEffect(dynamicId) {
        logger.fInfo { "Loading dynamic detail for id: $dynamicId" }
        dynamicDetailViewModel.dynamicId = dynamicId
        dynamicDetailViewModel.loadDynamic()
        dynamicItem = dynamicDetailViewModel.dynamicItem
        isLoading = false

        // 如果是专栏类型，加载完整内容
        if (dynamicItem?.type == DynamicType.Article) {
            dynamicDetailViewModel.loadArticleContent()
        }
    }

    val contentFocusRequester = remember { FocusRequester() }
    val commentButtonFocusRequester = remember { FocusRequester() }
    val likeButtonFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // 初始焦点请求到内容区域
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            contentFocusRequester.requestFocus()
        }
    }

    // 评论浮层关闭后，焦点返回到评论按钮
    LaunchedEffect(showCommentPanel) {
        if (!showCommentPanel && !isLoading && dynamicItem != null) {
            delay(100) // 等待动画完成
            commentButtonFocusRequester.requestFocus()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    if (dynamicItem != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AsyncImage(
                                model = dynamicItem!!.author.avatar,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Column {
                                Text(
                                    text = dynamicItem!!.author.author,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = dynamicItem!!.author.pubTime + " ${dynamicItem!!.author.pubAction}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        Text("动态详情")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("加载中...", style = MaterialTheme.typography.bodyLarge)
            }
        } else if (dynamicItem == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("动态不存在或已删除", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 内容区域
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(contentFocusRequester)
                        .focusable()
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.key) {
                                    Key.DirectionUp -> {
                                        // 如果还能向上滚动，则滚动并消费事件
                                        if (listState.canScrollBackward) {
                                            scope.launch {
                                                listState.animateScrollBy(-200f)
                                            }
                                            return@onKeyEvent true
                                        }
                                    }
                                    Key.DirectionDown -> {
                                        // 如果还能向下滚动，则滚动并消费事件
                                        if (listState.canScrollForward) {
                                            scope.launch {
                                                listState.animateScrollBy(200f)
                                            }
                                            return@onKeyEvent true
                                        }
                                    }
                                }
                            }
                            return@onKeyEvent false
                        },
                    state = listState,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 48.dp,
                        vertical = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 动态内容
                    item {
                        DynamicContentSection(
                            dynamicItem = dynamicItem!!,
                            articleParagraphs = dynamicDetailViewModel.articleParagraphs,
                            onClickVideo = { aid ->
                                VideoInfoActivity.actionStart(
                                    context = context,
                                    aid = aid
                                )
                            },
                            onClickPgc = { epid, seasonId ->
                                SeasonInfoActivity.actionStart(
                                    context = context,
                                    epId = epid,
                                    seasonId = seasonId
                                )
                            }
                        )
                    }

                    // 互动数据
                    item {
                        dynamicItem!!.footer?.let { footer ->
                            DynamicFooterInfo(footer = footer)
                        }
                    }

                    // 占位，确保内容可以滚动到底部
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // 底部按钮区域
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                        .padding(horizontal = 48.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 点赞按钮
                    Button(
                        modifier = Modifier
                            .focusRequester(likeButtonFocusRequester),
                        onClick = {
                            dynamicDetailViewModel.toggleLike()
                        },
                        enabled = !dynamicDetailViewModel.isLiking,
                        colors = ButtonDefaults.colors(
                            containerColor = if (dynamicDetailViewModel.isLiked)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedContainerColor = if (dynamicDetailViewModel.isLiked)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.inverseSurface,
                            focusedContentColor = Color.Black
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (dynamicDetailViewModel.isLiked)
                                    Icons.Default.Favorite
                                else
                                    Icons.Default.FavoriteBorder,
                                contentDescription = "点赞",
                                modifier = Modifier.size(20.dp),
                                tint = LocalContentColor.current
                            )
                            Text(
                                text = if (dynamicDetailViewModel.isLiked) "已赞" else "点赞",
                                fontSize = 14.sp,
                                color = LocalContentColor.current
                            )
                            Text(
                                text = dynamicDetailViewModel.likeCount.toString(),
                                fontSize = 14.sp,
                                color = LocalContentColor.current
                            )
                        }
                    }

                    // 评论按钮
                    Button(
                        modifier = Modifier
                            .focusRequester(commentButtonFocusRequester),
                        onClick = {
                            showCommentPanel = true
                        },
                        colors = ButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
                            focusedContentColor = Color.Black
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ModeComment,
                                contentDescription = "评论",
                                modifier = Modifier.size(20.dp),
                                tint = LocalContentColor.current
                            )
                            Text(
                                text = "评论",
                                fontSize = 14.sp,
                                color = LocalContentColor.current
                            )
                            dynamicItem!!.footer?.comment?.let { count ->
                                Text(
                                    text = count.toString(),
                                    fontSize = 14.sp,
                                    color = LocalContentColor.current
                                )
                            }
                        }
                    }

                    // 跳转原动态视频按钮（仅转发类型的动态且有原视频时显示）
                    if (dynamicItem!!.type == DynamicType.Forward &&
                        dynamicItem!!.orig?.type == DynamicType.Av &&
                        dynamicItem!!.orig?.video != null) {
                        Button(
                            onClick = {
                                dynamicItem!!.orig?.video?.let { video ->
                                    VideoInfoActivity.actionStart(
                                        context = context,
                                        aid = video.aid
                                    )
                                }
                            },
                            colors = ButtonDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
                                focusedContentColor = Color.Black
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "跳转原视频",
                                    modifier = Modifier.size(20.dp),
                                    tint = LocalContentColor.current
                                )
                                Text(
                                    text = "跳转原视频",
                                    fontSize = 14.sp,
                                    color = LocalContentColor.current
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 评论浮层
    dynamicItem?.let { item ->
        CommentPanel(
            show = showCommentPanel,
            oid = item.commentId,
            type = item.commentType,
            onHide = { showCommentPanel = false }
        )
    }
}

@Composable
private fun DynamicContentSection(
    dynamicItem: DynamicItem,
    articleParagraphs: List<ArticleParagraph> = emptyList(),
    onClickVideo: (Long) -> Unit,
    onClickPgc: (Int, Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (dynamicItem.type) {
            DynamicType.Av -> {
                dynamicItem.video?.let { video ->
                    // 描述文字
                    if (video.text.isNotBlank()) {
                        Text(
                            text = video.text,
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 16.sp
                        )
                    }
                    // 视频封面
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        AsyncImage(
                            model = video.cover,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // 播放按钮提示
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(60.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.6f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▶", color = Color.White, fontSize = 24.sp)
                        }
                        // 时长
                        Text(
                            text = video.duration,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.7f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 14.sp
                        )
                    }
                    // 视频标题
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 18.sp
                    )
                }
            }
            DynamicType.Pgc -> {
                dynamicItem.pgc?.let { pgc ->
                    Text(
                        text = pgc.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 18.sp
                    )
                    // 番剧封面
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
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
                                    Color(0xFFFB7299),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("番剧", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
            DynamicType.Draw -> {
                dynamicItem.draw?.let { draw ->
                    val drawTitle = draw.title
                    if (!drawTitle.isNullOrBlank()) {
                        Text(
                            text = drawTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 18.sp
                        )
                    }
                    if (draw.text.isNotBlank()) {
                        DynamicRichText(
                            richTextNodes = draw.richTextNodes,
                            fallbackText = draw.text,
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 16.sp
                        )
                    }
                    // 图片网格
                    if (draw.images.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            draw.images.forEach { image ->
                                AsyncImage(
                                    model = image.url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.FillWidth
                                )
                            }
                        }
                    }
                }
            }
            DynamicType.Word -> {
                dynamicItem.word?.let { word ->
                    DynamicRichText(
                        richTextNodes = word.richTextNodes,
                        fallbackText = word.text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 16.sp
                    )
                }
            }
            DynamicType.Article -> {
                dynamicItem.article?.let { article ->
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 18.sp
                    )

                    // 如果有完整段落内容，显示完整内容
                    val paragraphs = articleParagraphs
                    if (paragraphs.isNotEmpty()) {
                        ArticleContent(
                            paragraphs = paragraphs
                        )
                    } else {
                        // 回退到摘要显示
                        if (article.covers.isNotEmpty()) {
                            AsyncImage(
                                model = article.covers.first(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        if (article.text.isNotBlank()) {
                            Text(
                                text = article.text,
                                style = MaterialTheme.typography.bodyLarge,
                                fontSize = 16.sp
                            )
                        }
                    }

                    // 专栏标签
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = article.label.ifBlank { "专栏" },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            DynamicType.Forward -> {
                dynamicItem.word?.let { word ->
                    if (word.text.isNotBlank()) {
                        DynamicRichText(
                            richTextNodes = word.richTextNodes,
                            fallbackText = word.text,
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 16.sp
                        )
                    }
                }
                // 原动态
                dynamicItem.orig?.let { orig ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (orig.author.mid > 0) {
                                    AsyncImage(
                                        model = orig.author.avatar,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Text(
                                        text = orig.author.author,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            when (orig.type) {
                                DynamicType.Av -> orig.video?.let {
                                    Text(it.title, style = MaterialTheme.typography.bodyMedium)
                                }
                                DynamicType.Word -> orig.word?.let {
                                    Text(it.text, style = MaterialTheme.typography.bodyMedium)
                                }
                                else -> Text("原动态", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            else -> {
                Text(
                    text = "暂不支持的动态类型",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun DynamicFooterInfo(footer: DynamicItem.DynamicFooterModule) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("👍 ${footer.like}", fontSize = 14.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("💬 ${footer.comment}", fontSize = 14.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("↗️ ${footer.share}", fontSize = 14.sp)
        }
    }
}
