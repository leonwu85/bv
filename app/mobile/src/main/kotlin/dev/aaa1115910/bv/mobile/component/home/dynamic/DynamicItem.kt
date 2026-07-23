package dev.aaa1115910.bv.mobile.component.home.dynamic

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.origeek.imageViewer.previewer.ImagePreviewerState
import com.origeek.imageViewer.previewer.TransformImageView
import com.origeek.imageViewer.previewer.TransformItemState
import com.origeek.imageViewer.previewer.rememberPreviewerState
import com.origeek.imageViewer.previewer.rememberTransformItemState
import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.biliapi.entity.user.ArticleParagraph
import dev.aaa1115910.biliapi.entity.user.DynamicItem
import dev.aaa1115910.biliapi.entity.user.DynamicType
import dev.aaa1115910.biliapi.entity.user.RichTextNode
import dev.aaa1115910.biliapi.http.entity.dynamic.DynamicVoteInfo
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.component.DynamicRichText
import dev.aaa1115910.bv.mobile.component.user.UserAvatar
import dev.aaa1115910.bv.mobile.activities.TopicDynamicActivity
import dev.aaa1115910.bv.mobile.component.videocard.shareText
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.resizedImageUrl
import dev.aaa1115910.bv.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.util.UUID

@Composable
fun DynamicItem(
    modifier: Modifier = Modifier,
    dynamicItem: DynamicItem,
    previewerState: ImagePreviewerState = rememberPreviewerState(pageCount = { 0 }),
    onShowPreviewer: (newPictures: List<Picture>, afterSetPictures: () -> Unit) -> Unit = { _, _ -> },
    onTempBlockAuthor: ((DynamicItem.DynamicAuthorModule) -> Unit)? = null,
    onLike: (DynamicItem) -> Unit = {},
    onClick: (DynamicItem) -> Unit = {}
) {
    val context = LocalContext.current
    val paddingSize = 12.dp

    Surface(
        modifier = modifier,
        onClick = { onClick(dynamicItem) },
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(vertical = paddingSize),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DynamicHeader(
                modifier = Modifier.padding(horizontal = paddingSize),
                author = dynamicItem.author,
                dynamicItem = dynamicItem,
                onTempBlockAuthor = onTempBlockAuthor
            )
            DynamicContent(
                dynamicItem = dynamicItem,
                horizontalPadding = paddingSize,
                previewerState = previewerState,
                onShowPreviewer = onShowPreviewer,
                onClick = onClick
            )
            dynamicItem.vote?.let { vote ->
                DynamicVoteCard(
                    modifier = Modifier.padding(horizontal = paddingSize),
                    dynamicId = dynamicItem.id,
                    vote = vote
                )
            }
            dynamicItem.footer?.let { footer ->
                DynamicFooter(
                    modifier = Modifier.padding(horizontal = paddingSize),
                    footer = footer,
                    isLike = footer.isLiked,
                    onShare = {
                        dynamicItem.shareDynamicOrToast(context)
                    },
                    onShowComment = {
                        onClick(dynamicItem)
                    },
                    onLike = {
                        onLike(dynamicItem)
                    }
                )
            }
        }
    }
}

@Composable
fun DynamicContent(
    modifier: Modifier = Modifier,
    dynamicItem: DynamicItem,
    horizontalPadding: Dp = 12.dp,
    previewerState: ImagePreviewerState = rememberPreviewerState(pageCount = { 0 }),
    onShowPreviewer: (newPictures: List<Picture>, afterSetPictures: () -> Unit) -> Unit = { _, _ -> },
    articleParagraphs: List<ArticleParagraph> = emptyList(),
    onClick: (DynamicItem) -> Unit,
    onTopicClick: ((RichTextNode) -> Unit)? = null

) {
    val context = LocalContext.current
    val resolvedOnTopicClick = onTopicClick ?: { node: RichTextNode ->
        val topicId = node.rid?.toLongOrNull()
            ?: node.uri
                ?.let(Uri::parse)
                ?.getQueryParameter("topic_id")
                ?.toLongOrNull()
        if (topicId != null && topicId > 0L) {
            TopicDynamicActivity.actionStart(context, topicId, node.text.trim().trim('#'))
        }
    }
    val contentModifier = modifier.padding(horizontal = horizontalPadding)
    dynamicItem.blocked?.let { blocked ->
        DynamicBlocked(
            modifier = contentModifier,
            blocked = blocked
        )
        return
    }
    when (dynamicItem.type) {
        DynamicType.Av -> DynamicVideoContent(
            modifier = contentModifier,
            video = dynamicItem.video!!
        )

        DynamicType.Draw -> DynamicDraw(
            modifier = contentModifier,
            draw = dynamicItem.draw!!,
            previewerState = previewerState,
            onShowPreviewer = onShowPreviewer,
            onTopicClick = resolvedOnTopicClick
        )

        DynamicType.Forward -> DynamicForward(
            modifier = modifier,
            word = dynamicItem.word,
            dynamicItem = dynamicItem.orig!!,
            previewerState = previewerState,
            onShowPreviewer = onShowPreviewer,
            articleParagraphs = articleParagraphs,
            onClick = { onClick(dynamicItem.orig!!) },
            onTopicClick = resolvedOnTopicClick
        )

        DynamicType.LiveRcmd -> DynamicLiveRcmd(
            modifier = contentModifier,
            liveRcmd = dynamicItem.liveRcmd!!
        )

        DynamicType.UgcSeason -> {
            Text("${dynamicItem}")
        }

        DynamicType.Word -> DynamicWord(
            modifier = contentModifier,
            word = dynamicItem.word!!,
            onTopicClick = resolvedOnTopicClick
        )

        DynamicType.Pgc -> DynamicPgc(
            modifier = contentModifier,
            pgc = dynamicItem.pgc!!
        )

        DynamicType.Article -> DynamicArticle(
            modifier = contentModifier,
            article = dynamicItem.article!!,
            articleParagraphs = articleParagraphs,
            previewerState = previewerState,
            onShowPreviewer = onShowPreviewer
        )

        DynamicType.None -> DynamicNone(
            modifier = contentModifier,
            none = dynamicItem.none!!
        )

        else -> Text("${dynamicItem.type}")
    }
}

@Composable
fun DynamicVideoContent(
    modifier: Modifier = Modifier,
    video: DynamicItem.DynamicVideoModule
) {
    val badgeText = video.chargingArcBadge.ifBlank {
        if (video.isChargingArc) "充电专属" else ""
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (video.text.isNotBlank()) {
            Text(text = video.text)
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
        ) {
            Box(
                contentAlignment = Alignment.BottomCenter
            ) {
                AsyncImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.6f)
                        .clip(MaterialTheme.shapes.medium),
                    model = video.cover.resizedImageUrl(ImageSize.SmallVideoCardCover),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds
                )
                DynamicVideoCoverBadge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 10.dp),
                    text = badgeText,
                    isCharging = video.isChargingArc || badgeText == "充电专属"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.3f)
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp, 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (video.play.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    modifier = Modifier,
                                    painter = painterResource(id = R.drawable.ic_play_count),
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Text(
                                    text = video.play,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                        }
                        if (video.danmaku.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    modifier = Modifier,
                                    painter = painterResource(id = R.drawable.ic_danmaku_count),
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Text(
                                    text = video.danmaku,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Text(
                        text = video.duration,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }

        }
        Text(text = video.title)
    }
}

@Composable
private fun DynamicVideoCoverBadge(
    modifier: Modifier = Modifier,
    text: String,
    isCharging: Boolean
) {
    if (text.isBlank()) return

    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val containerColor = when {
        isCharging && isDark -> colorScheme.errorContainer
        isCharging -> colorScheme.error
        else -> colorScheme.primary
    }
    val contentColor = when {
        isCharging && isDark -> colorScheme.onErrorContainer
        isCharging -> colorScheme.onError
        else -> colorScheme.onPrimary
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            text = text,
            fontSize = 11.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DynamicHeader(
    modifier: Modifier = Modifier,
    author: DynamicItem.DynamicAuthorModule,
    dynamicItem: DynamicItem? = null,
    onTempBlockAuthor: ((DynamicItem.DynamicAuthorModule) -> Unit)? = null
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .padding(end = 30.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UserAvatar(
                avatar = author.avatar,
                size = 48.dp
            )
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Text(
                    text = author.author,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f, fill = false),
                        text = author.pubTime + " ${author.pubAction}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                        fontSize = 14.sp,
                        lineHeight = 14.sp
                    )
                    if (author.badgeText.isNotBlank()) {
                        Text(
                            text = author.badgeText,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 14.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        if (dynamicItem != null) DynamicMoreMenu(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(30.dp),
            dynamicItem = dynamicItem,
            onTempBlockAuthor = onTempBlockAuthor
        )
    }
}

@Composable
fun DynamicForwardHeader(
    modifier: Modifier = Modifier,
    author: DynamicItem.DynamicAuthorModule
) {
    Box(
        modifier = modifier
            .height(24.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                avatar = author.avatar,
                size = 20.dp
            )

            Text(
                text = author.author,
                maxLines = 1,
                fontSize = 14.sp,
                lineHeight = 14.sp
            )
            Text(
                text = author.pubTime + " ${author.pubAction}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                fontSize = 14.sp,
                lineHeight = 14.sp
            )
            if (author.badgeText.isNotBlank()) {
                Text(
                    text = author.badgeText,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 14.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun DynamicFooter(
    modifier: Modifier = Modifier,
    footer: DynamicItem.DynamicFooterModule,
    isLike: Boolean = false,
    onShare: (() -> Unit)? = null,
    onShowComment: (() -> Unit)? = null,
    onLike: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        DynamicFooterButton(
            icon = Icons.Default.Share,
            text = footer.share.formatDynamicStat("转发")
        ) { onShare?.invoke() }
        DynamicFooterButton(
            icon = Icons.AutoMirrored.Filled.Comment,
            text = footer.comment.formatDynamicStat("评论")
        ) { onShowComment?.invoke() }
        DynamicFooterButton(
            icon = if (isLike) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            text = footer.like.formatDynamicStat("点赞")
        ) { onLike?.invoke() }
    }
}

@Composable
fun DynamicFooterButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    TextButton(
        modifier = modifier,
        onClick = onClick
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = icon,
            contentDescription = null
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

private fun Int.formatDynamicStat(fallback: String): String =
    takeIf { it > 0 }?.toString() ?: fallback

@Composable
fun DynamicVoteCard(
    modifier: Modifier = Modifier,
    dynamicId: String?,
    vote: DynamicItem.DynamicVoteModule,
    userRepository: UserRepository = koinInject()
) {
    var showDialog by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { showDialog = true },
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VoteGlyph()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = vote.title.ifBlank { "投票" },
                    maxLines = 1,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${vote.joinNum.formatCompact()}人参与",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
            OutlinedButton(onClick = { showDialog = true }) {
                Text(text = "参与")
            }
        }
    }

    if (showDialog) {
        DynamicVoteDialog(
            vote = vote,
            dynamicId = dynamicId,
            userRepository = userRepository,
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun VoteGlyph() {
    Box(
        modifier = Modifier
            .width(70.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            listOf(0.45f, 0.78f, 0.58f).forEach { fraction ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(fraction)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun DynamicVoteDialog(
    vote: DynamicItem.DynamicVoteModule,
    dynamicId: String?,
    userRepository: UserRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var voteInfo by remember(vote.voteId) { mutableStateOf<DynamicVoteInfo?>(null) }
    var loading by remember(vote.voteId) { mutableStateOf(true) }
    var error by remember(vote.voteId) { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    val selectedVotes = remember(vote.voteId) { mutableStateListOf<Int>() }

    LaunchedEffect(vote.voteId) {
        loading = true
        error = null
        runCatching {
            withContext(Dispatchers.IO) {
                userRepository.getDynamicVoteInfo(vote.voteId)
            }
        }.onSuccess { data ->
            voteInfo = data.voteInfo.copy(
                myVotes = data.myVotes.ifEmpty { data.voteInfo.myVotes }
            )
        }.onFailure {
            error = it.localizedMessage ?: "投票加载失败"
        }
        loading = false
    }

    val info = voteInfo
    val myVotes = info?.myVotes.orEmpty()
    val nowSeconds = System.currentTimeMillis() / 1000
    val voteEnded = info?.endTime?.let { it > 0 && it <= nowSeconds } ?: false
    val canVote = info != null && Prefs.isLogin && !voteEnded && myVotes.isEmpty()
    val totalCount = (info?.options?.sumOf { it.cnt } ?: 0).coerceAtLeast(info?.joinNum ?: 0)
    val maxChoice = (info?.choiceCnt ?: 1).coerceAtLeast(1)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = info?.title?.ifBlank { vote.title } ?: vote.title.ifBlank { "投票" })
        },
        text = {
            when {
                loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp))
                        Text(text = "正在加载投票")
                    }
                }

                error != null -> Text(text = error.orEmpty())

                info != null -> {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (info.desc.isNotBlank()) {
                            Text(
                                text = info.desc,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = buildString {
                                append(info.joinNum.formatCompact())
                                append("人参与")
                                append(" · ")
                                append(if (voteEnded) "已结束" else "最多选${maxChoice}项")
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                        info.options.forEachIndexed { index, option ->
                            val voteIndex = option.optIdx.takeIf { it > 0 } ?: index + 1
                            val isSelected = voteIndex in selectedVotes
                            val isMyVote = voteIndex in myVotes
                            val progress = if (totalCount > 0) option.cnt / totalCount.toFloat() else 0f
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(enabled = canVote) {
                                        if (isSelected) {
                                            selectedVotes.remove(voteIndex)
                                        } else if (selectedVotes.size < maxChoice) {
                                            if (maxChoice == 1) selectedVotes.clear()
                                            selectedVotes.add(voteIndex)
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (canVote) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = {
                                                if (isSelected) {
                                                    selectedVotes.remove(voteIndex)
                                                } else if (selectedVotes.size < maxChoice) {
                                                    if (maxChoice == 1) selectedVotes.clear()
                                                    selectedVotes.add(voteIndex)
                                                }
                                            }
                                        )
                                    }
                                    Text(
                                        modifier = Modifier.weight(1f),
                                        text = option.optDesc,
                                        maxLines = 2
                                    )
                                    if (!canVote) {
                                        Text(
                                            text = "${option.cnt.formatCompact()}票",
                                            color = if (isMyVote) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                if (!canVote) {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth(),
                                        progress = { progress.coerceIn(0f, 1f) }
                                    )
                                }
                            }
                        }
                        if (!Prefs.isLogin) {
                            Text(
                                text = "登录后可参与投票",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (canVote) {
                Button(
                    enabled = selectedVotes.isNotEmpty() && !submitting,
                    onClick = {
                        submitting = true
                        scope.launch(Dispatchers.IO) {
                            runCatching {
                                userRepository.doDynamicVote(
                                    voteId = vote.voteId,
                                    votes = selectedVotes.toList(),
                                    dynamicId = dynamicId
                                ).voteInfo
                            }.onSuccess { result ->
                                withContext(Dispatchers.Main) {
                                    voteInfo = result.copy(myVotes = selectedVotes.toList())
                                    selectedVotes.clear()
                                    "投票成功".toast(context)
                                }
                            }.onFailure {
                                withContext(Dispatchers.Main) {
                                    (it.localizedMessage ?: "投票失败").toast(context)
                                }
                            }
                            withContext(Dispatchers.Main) {
                                submitting = false
                            }
                        }
                    }
                ) {
                    Text(text = if (submitting) "投票中" else "投票")
                }
            } else {
                TextButton(
                    onClick = {
                        val url = vote.url ?: "https://t.bilibili.com/vote/h5/index?vote_id=${vote.voteId}#/result"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                ) {
                    Text(text = "浏览器打开")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "关闭")
            }
        }
    )
}

@Composable
fun DynamicDraw(
    modifier: Modifier = Modifier,
    draw: DynamicItem.DynamicDrawModule,
    previewerState: ImagePreviewerState,
    onShowPreviewer: (newPictures: List<Picture>, afterSetPictures: () -> Unit) -> Unit,
    onTopicClick: (RichTextNode) -> Unit = {}
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (draw.title != null) {
            Text(
                text = draw.title!!,
                fontWeight = FontWeight.Bold
            )
        }
        if (draw.text.isNotBlank() || draw.richTextNodes.isNotEmpty()) {
            DynamicRichText(
                richTextNodes = draw.richTextNodes,
                fallbackText = draw.text,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 16.sp,
                onNodeClick = onTopicClick
            )
        }
        DynamicPictures(
            pictures = draw.images,
            previewerState = previewerState,
            onShowPreviewer = onShowPreviewer
        )
    }
}


@Composable
fun DynamicPictures(
    modifier: Modifier = Modifier,
    pictures: List<Picture>,
    previewerState: ImagePreviewerState,
    onShowPreviewer: (newPictures: List<Picture>, afterSetPictures: () -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()

    val onClickPicture: (index: Int, itemState: TransformItemState) -> Unit = { index, itemState ->
        onShowPreviewer(pictures) {
            scope.launch {
                previewerState.openTransform(
                    index = index,
                    itemState = itemState,
                )
            }
        }
    }

    if (pictures.isEmpty()) return

    if (pictures.size == 1) {
        val picture = pictures.first()
        val itemState = rememberTransformItemState()
        val width = picture.width.takeIf { it > 0 } ?: 1
        val height = picture.height.takeIf { it > 0 } ?: 1
        val ratio = (width.toFloat() / height.toFloat()).coerceIn(0.7f, 2.2f)
        val widthFraction = when {
            ratio < 0.85f -> 0.62f
            ratio < 1.1f -> 0.72f
            else -> 1f
        }
        Card(
            modifier = modifier
                .fillMaxWidth(widthFraction)
                .aspectRatio(ratio),
            shape = MaterialTheme.shapes.medium,
            onClick = { onClickPicture(0, itemState) }
        ) {
            TransformImageView(
                painter = rememberAsyncImagePainter(picture.url),
                key = picture.key,
                itemState = itemState,
                previewerState = previewerState,
            )
        }
        return
    }

    val columns = when (pictures.size) {
        2 -> 2
        4 -> 2
        else -> 3
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        pictures.chunked(columns).forEachIndexed { rowIndex, rowPictures ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rowPictures.forEachIndexed { columnIndex, picture ->
                    val index = rowIndex * columns + columnIndex
                    val itemState = rememberTransformItemState()
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        shape = dynamicPictureShape(
                            columns = columns,
                            count = pictures.size,
                            index = index
                        ),
                        onClick = { onClickPicture(index, itemState) }
                    ) {
                        TransformImageView(
                            painter = rememberAsyncImagePainter(picture.url),
                            key = picture.key,
                            itemState = itemState,
                            previewerState = previewerState,
                        )
                    }
                }
                repeat(columns - rowPictures.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun DynamicWord(
    modifier: Modifier = Modifier,
    word: DynamicItem.DynamicWordModule,
    onTopicClick: (RichTextNode) -> Unit = {}
) {
    DynamicRichText(
        modifier = modifier,
        richTextNodes = word.richTextNodes,
        fallbackText = word.text,
        style = MaterialTheme.typography.bodyLarge,
        fontSize = 16.sp,
        onNodeClick = onTopicClick
    )
}

@Composable
fun DynamicForward(
    modifier: Modifier = Modifier,
    word: DynamicItem.DynamicWordModule?,
    dynamicItem: DynamicItem,
    previewerState: ImagePreviewerState,
    horizontalPadding: Dp = 12.dp,
    onShowPreviewer: (newPictures: List<Picture>, afterSetPictures: () -> Unit) -> Unit,
    articleParagraphs: List<ArticleParagraph> = emptyList(),
    onClick: () -> Unit,
    onTopicClick: (RichTextNode) -> Unit = {}
) {
    Column(
        modifier = modifier,
    ) {
        if (word != null) {
            DynamicRichText(
                modifier = Modifier.padding(horizontal = horizontalPadding),
                richTextNodes = word.richTextNodes,
                fallbackText = word.text,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 16.sp,
                onNodeClick = onTopicClick
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            onClick = onClick
        ) {
            Box(
                modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 6.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (dynamicItem.author.mid != -1L) {
                        DynamicForwardHeader(
                            author = dynamicItem.author
                        )
                    }

                    DynamicContent(
                        modifier = Modifier.fillMaxWidth(),
                        dynamicItem = dynamicItem,
                        horizontalPadding = 0.dp,
                        previewerState = previewerState,
                        onShowPreviewer = onShowPreviewer,
                        articleParagraphs = articleParagraphs,
                        onClick = {},
                        onTopicClick = onTopicClick
                    )
                    dynamicItem.vote?.let { vote ->
                        DynamicVoteCard(
                            modifier = Modifier.fillMaxWidth(),
                            dynamicId = dynamicItem.id,
                            vote = vote
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DynamicLiveRcmd(
    modifier: Modifier = Modifier,
    liveRcmd: DynamicItem.DynamicLiveRcmdModule
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
        ) {
            Box(
                contentAlignment = Alignment.BottomCenter
            ) {
                AsyncImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.6f)
                        .clip(MaterialTheme.shapes.medium),
                    model = liveRcmd.cover.resizedImageUrl(ImageSize.SmallVideoCardCover),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.3f)
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp, 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(
                        text = "${liveRcmd.roomId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }
        Text(text = liveRcmd.title)
    }
}

@Composable
fun DynamicPgc(
    modifier: Modifier = Modifier,
    pgc: DynamicItem.DynamicPgcModule
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
        ) {
            AsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.6f)
                    .clip(MaterialTheme.shapes.medium),
                model = pgc.cover.resizedImageUrl(ImageSize.SmallVideoCardCover),
                contentDescription = null,
                contentScale = ContentScale.FillBounds
            )
        }
        Text(text = pgc.title)
    }
}

@Composable
fun DynamicArticle(
    modifier: Modifier = Modifier,
    article: DynamicItem.DynamicArticleModule,
    articleParagraphs: List<ArticleParagraph> = emptyList(),
    previewerState: ImagePreviewerState = rememberPreviewerState(pageCount = { 0 }),
    onShowPreviewer: (newPictures: List<Picture>, afterSetPictures: () -> Unit) -> Unit = { _, _ -> },
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = article.title,
            fontWeight = FontWeight.Bold
        )
        if (articleParagraphs.isEmpty()) {
            Text(text = article.text)
        } else {
            articleParagraphs.forEach { paragraph ->
                when (paragraph) {
                    is ArticleParagraph.TextParagraph -> {
                        Text(
                            text = paragraph.nodes.joinToString("") { it.text },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    is ArticleParagraph.PicturesParagraph -> {
                        DynamicPictures(
                            pictures = paragraph.pictures.map { picture ->
                                Picture(
                                    url = picture.url,
                                    width = picture.width,
                                    height = picture.height,
                                    key = picture.url.ifBlank { UUID.randomUUID().toString() }
                                )
                            },
                            previewerState = previewerState,
                            onShowPreviewer = onShowPreviewer
                        )
                    }

                    is ArticleParagraph.LineParagraph -> {
                        paragraph.picture?.let { picture ->
                            DynamicPictures(
                                pictures = listOf(
                                    Picture(
                                        url = picture.url,
                                        width = picture.width,
                                        height = picture.height,
                                        key = picture.url.ifBlank { UUID.randomUUID().toString() }
                                    )
                                ),
                                previewerState = previewerState,
                                onShowPreviewer = onShowPreviewer
                            )
                        }
                    }
                }
            }
        }
        if (articleParagraphs.isEmpty() && article.coverPictures.isNotEmpty()) {
            DynamicPictures(
                pictures = article.coverPictures,
                previewerState = previewerState,
                onShowPreviewer = onShowPreviewer
            )
        } else if (articleParagraphs.isEmpty() && article.covers.isNotEmpty()) {
            AsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium),
                model = article.covers.first(),
                contentDescription = null,
                contentScale = ContentScale.FillWidth
            )
        }
    }
}

@Composable
fun DynamicBlocked(
    modifier: Modifier = Modifier,
    blocked: DynamicItem.DynamicBlockedModule
) {
    val isDark = isSystemInDarkTheme()

    if (blocked.blockedType == 1) {
        BoxWithConstraints(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            val cardSize = if (maxWidth <= 255.dp) {
                maxWidth
            } else {
                (maxWidth * 0.8f).coerceAtMost(400.dp)
            }
            DynamicBlockedContainer(
                modifier = Modifier.size(cardSize),
                blocked = blocked,
                isDark = isDark,
                squareLayout = true
            )
        }
    } else {
        DynamicBlockedContainer(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 82.dp),
            blocked = blocked,
            isDark = isDark,
            squareLayout = false
        )
    }
}

@Composable
private fun DynamicBlockedContainer(
    modifier: Modifier,
    blocked: DynamicItem.DynamicBlockedModule,
    isDark: Boolean,
    squareLayout: Boolean
) {
    val context = LocalContext.current
    val shape = MaterialTheme.shapes.medium
    val bgUrl = blocked.bgImage?.url(isDark).orEmpty()
    val iconUrl = blocked.icon?.url(isDark).orEmpty()

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        if (bgUrl.isNotBlank()) {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = bgUrl,
                contentDescription = null,
                contentScale = ContentScale.FillBounds
            )
        }

        if (squareLayout) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                DynamicBlockedIcon(iconUrl = iconUrl, size = 42.dp)
                if (blocked.hintMessage.isNotBlank()) {
                    Text(
                        modifier = Modifier.padding(top = 5.dp),
                        text = blocked.hintMessage,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
                DynamicBlockedButton(
                    modifier = Modifier.padding(top = 8.dp),
                    button = blocked.button,
                    onClick = { blocked.button?.jumpUrl?.let { openExternalUrl(context, it) } }
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DynamicBlockedIcon(iconUrl = iconUrl, size = 42.dp)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (blocked.title.isNotBlank()) {
                        Text(
                            text = blocked.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (blocked.hintMessage.isNotBlank()) {
                        Text(
                            text = blocked.hintMessage,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
                DynamicBlockedButton(
                    button = blocked.button,
                    onClick = { blocked.button?.jumpUrl?.let { openExternalUrl(context, it) } }
                )
            }
        }
    }
}

@Composable
private fun DynamicBlockedIcon(
    iconUrl: String,
    size: Dp
) {
    if (iconUrl.isNotBlank()) {
        AsyncImage(
            modifier = Modifier.size(size),
            model = iconUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun DynamicBlockedButton(
    modifier: Modifier = Modifier,
    button: DynamicItem.DynamicBlockedModule.BlockedButton?,
    onClick: () -> Unit
) {
    if (button == null) return
    Button(
        modifier = modifier,
        onClick = onClick
    ) {
        if (button.icon.isNotBlank()) {
            AsyncImage(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(16.dp),
                model = button.icon,
                contentDescription = null,
                contentScale = ContentScale.Fit
            )
        }
        Text(text = button.text)
    }
}

private fun dynamicPictureShape(
    columns: Int,
    count: Int,
    index: Int
): RoundedCornerShape {
    val corner = CornerSize(8.dp)
    val row = index / columns
    val column = index % columns
    val rows = (count + columns - 1) / columns
    val isTop = row == 0
    val isBottom = row == rows - 1
    val isStart = column == 0
    val isEnd = column == columns - 1 || index == count - 1

    return RoundedCornerShape(
        topStart = if (isTop && isStart) corner else CornerSize(0.dp),
        topEnd = if (isTop && isEnd) corner else CornerSize(0.dp),
        bottomStart = if (isBottom && isStart) corner else CornerSize(0.dp),
        bottomEnd = if (isBottom && isEnd) corner else CornerSize(0.dp)
    )
}

private fun DynamicItem.shareDynamicOrToast(context: android.content.Context) {
    val link = id
        ?.takeIf { it.isNotBlank() }
        ?.let { "https://t.bilibili.com/$it" }
        ?: jumpUrl
    if (link.isNullOrBlank()) {
        "当前动态没有可分享链接".toast(context)
    } else {
        shareText(context, link, "分享动态")
    }
}

private fun openExternalUrl(context: android.content.Context, url: String) {
    val normalizedUrl = when {
        url.startsWith("//") -> "https:$url"
        else -> url
    }
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl)))
    }.onFailure {
        "无法打开链接".toast(context)
    }
}

private fun Int.formatCompact(): String = toLong().formatCompact()

private fun Long.formatCompact(): String = when {
    this >= 100_000_000L -> String.format("%.1f亿", this / 100_000_000f).trimTrailingZero()
    this >= 10_000L -> String.format("%.1f万", this / 10_000f).trimTrailingZero()
    else -> toString()
}

private fun String.trimTrailingZero(): String =
    replace(".0亿", "亿").replace(".0万", "万")

@Composable
fun DynamicNone(
    modifier: Modifier = Modifier,
    none: DynamicItem.DynamicNoneModule
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = none.text)
    }
}

@Preview
@Composable
private fun DynamicHeaderPreview() {
    BVMobileTheme {
        Surface {
            DynamicHeader(
                author = emptyDynamicVideoData.author
            )
        }
    }
}

@Preview
@Composable
private fun DynamicForwardHeaderPreview() {
    BVMobileTheme {
        Surface {
            DynamicForwardHeader(
                author = emptyDynamicVideoData.author
            )
        }
    }
}

@Preview
@Composable
private fun DynamicFooterPreview() {
    BVMobileTheme {
        Surface {
            DynamicFooter(
                footer = exampleFooterData
            )
        }
    }
}

private val exampleAuthorData = DynamicItem.DynamicAuthorModule(
    author = "author",
    avatar = "",
    mid = 0,
    pubTime = "54 分钟前 投稿了视频",
    pubAction = ""
)

private val exampleFooterData = DynamicItem.DynamicFooterModule(
    like = 2,
    comment = 61,
    share = 8,
)

private val exampleVideoData = DynamicItem.DynamicVideoModule(
    aid = 0,
    title = "title",
    cover = "",
    duration = "23:45",
    play = "xx play",
    danmaku = "xx dm",
    seasonId = 0,
    cid = 0,
    text = "desc"
)

private val emptyDynamicData = DynamicItem(
    type = DynamicType.Av,
    author = exampleAuthorData,
    footer = exampleFooterData
)

private val emptyDynamicVideoData = DynamicItem(
    type = DynamicType.Av,
    author = exampleAuthorData,
    video = exampleVideoData,
    footer = exampleFooterData
)

private val emptyDynamicDrawData = DynamicItem(
    type = DynamicType.Draw,
    author = exampleAuthorData,
    draw = DynamicItem.DynamicDrawModule(
        title = "title",
        text = "draw",
        images = emptyList()
    ),
    footer = exampleFooterData
)

private val emptyDynamicWordData = DynamicItem.DynamicWordModule(
    text = "this is word module"
)

private val exampleDynamicForwardData = DynamicItem(
    type = DynamicType.Forward,
    author = exampleAuthorData,
    orig = emptyDynamicVideoData,
    word = emptyDynamicWordData,
    footer = exampleFooterData
)

private val exampleDynamicForwardNoneData = DynamicItem(
    type = DynamicType.Forward,
    author = exampleAuthorData,
    orig = DynamicItem(
        type = DynamicType.None,
        author = DynamicItem.DynamicAuthorModule("", "", -1, "", ""),
        none = DynamicItem.DynamicNoneModule("unknown dynamic")
    ),
    word = emptyDynamicWordData,
    footer = exampleFooterData
)

private val exampleDynamicLiveRcmdData = DynamicItem(
    type = DynamicType.LiveRcmd,
    author = exampleAuthorData,
    liveRcmd = DynamicItem.DynamicLiveRcmdModule(
        cover = "",
        title = "title",
        roomId = 3
    ),
    footer = exampleFooterData
)

private val exampleDynamicPgcData = DynamicItem(
    type = DynamicType.Pgc,
    author = exampleAuthorData,
    pgc = DynamicItem.DynamicPgcModule(
        cover = "",
        title = "title",
        seasonId = 3,
        epid = 3,
        aid = 0,
        cid = 0
    ),
    footer = exampleFooterData
)

private val exampleDynamicArticleData = DynamicItem(
    type = DynamicType.Article,
    author = exampleAuthorData,
    article = DynamicItem.DynamicArticleModule(
        title = "title",
        text = "article content",
        covers = listOf(""),
        id = 0,
        url = "",
        label = ""
    ),
    footer = exampleFooterData
)

@Preview
@Composable
private fun DynamicVideoItemPreview() {
    BVMobileTheme {
        Surface {
            DynamicItem(
                modifier = Modifier.padding(vertical = 8.dp),
                dynamicItem = emptyDynamicVideoData
            )
        }
    }
}

private class DynamicDrawItemProvider : PreviewParameterProvider<DynamicItem> {
    override val values = List(5) { index ->
        emptyDynamicData.copy(
            type = DynamicType.Draw,
            draw = DynamicItem.DynamicDrawModule(
                title = "title",
                text = "this is $index picture draw",
                images = Array(index) { Picture("", 0, 0, "${UUID.randomUUID()}") }.toList()
            )
        )
    }.asSequence()
}

@Preview
@Composable
private fun DynamicDrawItemPreview(@PreviewParameter(DynamicDrawItemProvider::class) dynamicItem: DynamicItem) {
    BVMobileTheme {
        Surface {
            DynamicItem(
                modifier = Modifier.padding(vertical = 8.dp),
                dynamicItem = dynamicItem
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DynamicForwardItemPreview() {
    BVMobileTheme {
        Surface {
            DynamicItem(
                modifier = Modifier.padding(vertical = 8.dp),
                dynamicItem = exampleDynamicForwardData
            )
        }
    }
}

@Preview
@Composable
private fun DynamicForwardItemNonePreview() {
    BVMobileTheme {
        Surface {
            DynamicItem(
                modifier = Modifier.padding(vertical = 8.dp),
                dynamicItem = exampleDynamicForwardNoneData
            )
        }
    }
}

@Preview
@Composable
private fun DynamicLiveRcmdItemPreview() {
    BVMobileTheme {
        Surface {
            DynamicItem(
                modifier = Modifier.padding(vertical = 8.dp),
                dynamicItem = exampleDynamicLiveRcmdData
            )
        }
    }
}

@Preview
@Composable
private fun DynamicItemListPreview() {
    BVMobileTheme {
        Surface {
            LazyColumn(
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                items(3) {
                    DynamicItem(
                        modifier = Modifier.padding(bottom = 8.dp),
                        dynamicItem = emptyDynamicVideoData
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun DynamicPgcItemPreview() {
    BVMobileTheme {
        Surface {
            DynamicItem(
                modifier = Modifier.padding(vertical = 8.dp),
                dynamicItem = exampleDynamicPgcData
            )
        }
    }
}

@Preview
@Composable
private fun DynamicArticleItemPreview() {
    BVMobileTheme {
        Surface {
            DynamicItem(
                modifier = Modifier.padding(vertical = 8.dp),
                dynamicItem = exampleDynamicArticleData
            )
        }
    }
}
