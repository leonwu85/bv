package dev.aaa1115910.bv.tv.screens.main.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import dev.aaa1115910.biliapi.entity.ugc.UgcItem
import dev.aaa1115910.bv.R as AppR
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.tv.R
import dev.aaa1115910.bv.tv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.tv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.tv.component.ContentStatusCard
import dev.aaa1115910.bv.tv.component.LoadingTip
import dev.aaa1115910.bv.tv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.tv.util.ProvideListBringIntoViewSpec
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.viewmodel.home.RecommendViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private const val VIDEO_GRID_PAGINATION_IDLE_MS = 120L

@Composable
fun RecommendScreen(
    modifier: Modifier = Modifier,
    lazyGridState: LazyGridState = rememberLazyGridState(),
    recommendViewModel: RecommendViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 不在 composition 中读取，避免每次 D-pad 移动整表重组
    val focusedIndexState = remember { mutableIntStateOf(0) }

    val onClickVideo: (UgcItem) -> Unit = remember(context) {
        { ugcItem -> VideoInfoActivity.actionStart(context, ugcItem.aid) }
    }
    val onLongClickVideo: (UgcItem) -> Unit = remember(context) {
        { ugcItem ->
            UpInfoActivity.actionStart(
                context,
                mid = ugcItem.authorId,
                name = ugcItem.author,
                face = ugcItem.authorFace
            )
        }
    }

    val videoList = recommendViewModel.recommendVideoList
    LaunchedEffect(videoList.size) {
        snapshotFlow {
            videoList.isNotEmpty() &&
                    focusedIndexState.intValue + 12 > videoList.size &&
                    !lazyGridState.isScrollInProgress
        }
            .distinctUntilChanged()
            .collectLatest { shouldLoadMore ->
                if (shouldLoadMore) {
                    delay(VIDEO_GRID_PAGINATION_IDLE_MS)
                    if (!lazyGridState.isScrollInProgress) {
                        scope.launch(Dispatchers.IO) {
                            recommendViewModel.loadMore()
                        }
                    }
                }
            }
    }

    val padding = dimensionResource(R.dimen.grid_padding) / 2
    val spacedBy = dimensionResource(R.dimen.grid_spacedBy) / 2
    val gridColumns = remember { Prefs.gridColumns }
    ProvideListBringIntoViewSpec(usePlatformDefault = true) {
        if (videoList.isEmpty()) {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (recommendViewModel.loading) {
                    LoadingTip()
                } else {
                    ContentStatusCard(text = stringResource(AppR.string.no_data))
                }
            }
        } else {
            LazyVerticalGrid(
                modifier = modifier.fillMaxSize(),
                columns = GridCells.Fixed(gridColumns),
                state = lazyGridState,
                contentPadding = PaddingValues(padding),
                verticalArrangement = Arrangement.spacedBy(spacedBy),
                horizontalArrangement = Arrangement.spacedBy(spacedBy)
            ) {
                itemsIndexed(
                    items = videoList,
                    key = { index, item -> "${item.aid}#$index" },
                    contentType = { _, _ -> "video_card" }
                ) { index, item ->
                    SmallVideoCard(
                        data = remember(item.aid, item.isChargingArc, item.chargingArcBadge) {
                            VideoCardData(
                                avid = item.aid,
                                title = item.title,
                                cover = item.cover,
                                play = with(item.play) { if (this == -1L) null else this },
                                danmaku = with(item.danmaku) { if (this == -1) null else this },
                                upName = item.author,
                                time = item.duration * 1000L,
                                pubTime = item.pubTime,
                                isInteractive = item.isInteractive,
                                isChargingArc = item.isChargingArc,
                                badgeText = item.chargingArcBadge
                            )
                        },
                        onClick = { onClickVideo(item) },
                        onLongClick = { onLongClickVideo(item) },
                        onFocus = { focusedIndexState.intValue = index }
                    )
                }

                if (recommendViewModel.loading) {
                    item(
                        key = "loading",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = "loading"
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingTip()
                        }
                    }
                }
            }
        }
    }
}
