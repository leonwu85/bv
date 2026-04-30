package dev.aaa1115910.bv.mobile.screen.home.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.entity.ugc.UgcItem
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.mobile.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.util.OnBottomReached
import dev.aaa1115910.bv.util.fInfo
import io.github.oshai.kotlinlogging.KotlinLogging

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RcmdPage(
    state: LazyGridState,
    windowSize: WindowWidthSizeClass,
    videos: List<UgcItem>,
    onClickVideo: (aid: Long) -> Unit,
    loading: Boolean,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    loadMore: () -> Unit
) {
    val logger = KotlinLogging.logger { }
    val pullRefreshState = rememberPullRefreshState(refreshing, { onRefresh() })
    val isCompact = windowSize == WindowWidthSizeClass.Compact

    state.OnBottomReached(
        loading = loading
    ) {
        logger.fInfo { "on reached rcmd page bottom" }
        loadMore()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(state = pullRefreshState)
    ) {
        LazyVerticalGrid(
            state = state,
            columns = GridCells.Fixed(if (isCompact) 2 else 5),
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp),
            contentPadding = PaddingValues(
                horizontal = if (isCompact) 8.dp else 12.dp,
                vertical = if (isCompact) 8.dp else 12.dp
            )
        ) {
            items(videos) { video ->
                SmallVideoCard(
                    data = VideoCardData(
                        avid = video.aid,
                        title = video.title,
                        cover = video.cover,
                        play = video.play,
                        danmaku = video.danmaku,
                        upName = video.author,
                        time = video.duration * 1000L,
                        isInteractive = video.isInteractive
                    ),
                    onClick = { onClickVideo(video.aid) }
                )
            }
        }
        PullRefreshIndicator(refreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
    }
}
