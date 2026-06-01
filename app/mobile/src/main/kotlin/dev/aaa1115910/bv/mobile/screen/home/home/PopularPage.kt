package dev.aaa1115910.bv.mobile.screen.home.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.aaa1115910.bv.R
import dev.aaa1115910.biliapi.entity.ugc.UgcItem
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.entity.carddata.VideoCardFeedOption
import dev.aaa1115910.bv.mobile.component.videocard.UpIcon
import dev.aaa1115910.bv.mobile.component.videocard.VideoCardMoreMenu
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.OnBottomReached
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.removeHtmlTags
import dev.aaa1115910.bv.util.resizedImageUrl
import io.github.oshai.kotlinlogging.KotlinLogging

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PopularPage(
    state: LazyGridState,
    windowSize: WindowWidthSizeClass,
    videos: List<UgcItem>,
    onClickVideo: (aid: Long) -> Unit,
    loading: Boolean,
    refreshing: Boolean,
    enabled: Boolean = true,
    onRefresh: () -> Unit,
    loadMore: () -> Unit
) {
    val logger = KotlinLogging.logger { }
    val pullRefreshState = rememberPullRefreshState(refreshing, { onRefresh() })
    val isCompact = windowSize == WindowWidthSizeClass.Compact

    state.OnBottomReached(
        loading = loading,
        enabled = enabled
    ) {
        logger.fInfo { "on reached popular page bottom" }
        loadMore()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(state = pullRefreshState)
    ) {
        LazyVerticalGrid(
            state = state,
            columns = GridCells.Fixed(if (isCompact) 1 else 3),
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp),
            contentPadding = PaddingValues(
                horizontal = if (isCompact) 8.dp else 12.dp,
                vertical = if (isCompact) 8.dp else 12.dp
            )
        ) {
            items(videos, key = { it.aid }) { video ->
                PopularVideoListCard(
                    data = video.toPopularVideoCardData(),
                    compact = isCompact,
                    onClick = { onClickVideo(video.aid) }
                )
            }
        }
        PullRefreshIndicator(refreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun PopularVideoListCard(
    modifier: Modifier = Modifier,
    data: VideoCardData,
    compact: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 112.dp else 104.dp)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(16 / 9f)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = data.cover.resizedImageUrl(ImageSize.SmallVideoCardCover),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
                if (data.timeString.isNotBlank()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp),
                        color = Color.Black.copy(alpha = 0.58f),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            text = data.timeString,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = data.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    VideoCardMoreMenu(data = data)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    UpIcon()
                    Text(
                        text = data.upName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (data.playString.isNotBlank()) {
                        PopularMetaText(
                            icon = R.drawable.ic_play_count,
                            text = data.playString
                        )
                    }
                    if (data.danmakuString.isNotBlank()) {
                        PopularMetaText(
                            icon = R.drawable.ic_danmaku_count,
                            text = data.danmakuString
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PopularMetaText(
    icon: Int,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

private fun UgcItem.toPopularVideoCardData(): VideoCardData {
    return VideoCardData(
        avid = aid,
        bvid = bvid,
        title = title.removeHtmlTags(),
        cover = cover,
        play = play,
        danmaku = danmaku,
        upName = author,
        upId = authorId,
        upFace = authorFace,
        time = duration * 1000L,
        isInteractive = isInteractive,
        feedGoto = feedGoto,
        feedParam = feedParam,
        dislikeReasons = dislikeReasons.map { VideoCardFeedOption(it.id, it.name, it.toast) },
        feedbacks = feedbacks.map { VideoCardFeedOption(it.id, it.name, it.toast) }
    )
}
