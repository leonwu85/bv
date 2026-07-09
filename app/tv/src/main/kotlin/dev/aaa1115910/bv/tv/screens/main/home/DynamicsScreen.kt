package dev.aaa1115910.bv.tv.screens.main.home

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.user.DynamicVideo
import dev.aaa1115910.bv.R as AppR
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.tv.R
import dev.aaa1115910.bv.tv.activities.user.FollowActivity
import dev.aaa1115910.bv.tv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.tv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.tv.component.ContentStatusCard
import dev.aaa1115910.bv.tv.component.LoadingTip
import dev.aaa1115910.bv.tv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.tv.util.ProvideListBringIntoViewSpec
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.viewmodel.home.DynamicViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun DynamicsScreen(
    modifier: Modifier = Modifier,
    lazyGridState: LazyGridState = rememberLazyGridState(),
    dynamicViewModel: DynamicViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusedIndexState = remember { mutableIntStateOf(-1) }
    // showTip 需要驱动 UI，用独立 state，避免读取 focusedIndex 导致整表重组
    var showTip by remember { mutableStateOf(false) }

    val onClickVideo: (DynamicVideo) -> Unit = remember(context) {
        { dynamic ->
            VideoInfoActivity.actionStart(
                context = context,
                aid = dynamic.aid,
                proxyArea = ProxyArea.checkProxyArea(dynamic.title)
            )
        }
    }
    val onLongClickVideo: (DynamicVideo) -> Unit = remember(context) {
        { dynamic ->
            UpInfoActivity.actionStart(
                context,
                mid = dynamic.authorId,
                name = dynamic.author,
                face = dynamic.authorFace
            )
        }
    }

    val videoList = dynamicViewModel.dynamicVideoList
    LaunchedEffect(videoList.size) {
        snapshotFlow { focusedIndexState.intValue }
            .map { index -> videoList.isNotEmpty() && index >= 0 && index + 12 > videoList.size }
            .distinctUntilChanged()
            .collect { shouldLoadMore ->
                if (shouldLoadMore) {
                    scope.launch(Dispatchers.IO) {
                        dynamicViewModel.loadMoreVideo()
                    }
                }
            }
    }

    if (dynamicViewModel.isLogin) {
        val padding = dimensionResource(R.dimen.grid_padding) / 2
        val spacedBy = dimensionResource(R.dimen.grid_spacedBy) / 2
        val gridColumns = remember { Prefs.gridColumns }
        if (showTip) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = (-20).dp, y = (-8).dp),
                text = stringResource(R.string.entry_follow_screen),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontSize = 12.sp,
                textAlign = TextAlign.End
            )
        }
        if (videoList.isEmpty()) {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (dynamicViewModel.loadingVideo) {
                    LoadingTip()
                } else {
                    ContentStatusCard(text = stringResource(AppR.string.no_data))
                }
            }
        } else {
            ProvideListBringIntoViewSpec {
                LazyVerticalGrid(
                    modifier = modifier
                        .fillMaxSize()
                        .onFocusChanged {
                            if (!it.isFocused) {
                                focusedIndexState.intValue = -1
                                showTip = false
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
                            data = remember(item.aid) {
                                VideoCardData(
                                    avid = item.aid,
                                    title = item.title,
                                    cover = item.cover,
                                    play = item.play,
                                    danmaku = item.danmaku,
                                    upName = item.author,
                                    time = item.duration * 1000L,
                                    pubTime = item.pubTime,
                                    isChargingArc = item.isChargingArc,
                                    badgeText = item.chargingArcBadge
                                )
                            },
                            onClick = { onClickVideo(item) },
                            onLongClick = { onLongClickVideo(item) },
                            onFocus = {
                                focusedIndexState.intValue = index
                                if (!showTip) showTip = true
                            }
                        )
                    }

                    if (dynamicViewModel.loadingVideo) {
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

                    if (!dynamicViewModel.videoHasMore) {
                        item(
                            key = "no_more",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = "footer"
                        ) {
                            Text(
                                text = "没有更多了捏",
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "请先登录")
        }
    }
}
