package dev.aaa1115910.bv.tv.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.tv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.tv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.util.onBackPressed
import dev.aaa1115910.bv.util.requestFocus
import kotlinx.coroutines.delay

@Composable
fun RelatedVideosPanel(
    show: Boolean,
    videos: List<VideoCardData>,
    onHide: () -> Unit,
    onOpenVideoInfo: (VideoCardData) -> Unit,
    onOpenSeasonInfo: (VideoCardData) -> Unit
) {
    val context = LocalContext.current
    val firstCardFocusRequester = remember { FocusRequester() }

    BackHandler(enabled = show) {
        onHide()
    }

    LaunchedEffect(show) {
        if (show && videos.isNotEmpty()) {
            delay(100)
            firstCardFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterEnd
    ) {
        if (show) {
            // scrim 遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = null,
                        indication = null
                    ) { onHide() }
            )
        }

        AnimatedVisibility(
            visible = show,
            enter = expandHorizontally(expandFrom = Alignment.End),
            exit = shrinkHorizontally(shrinkTowards = Alignment.End)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.525f)
                    .clickable(enabled = true, onClick = {}) // 阻止点击穿透
                    .onBackPressed { onHide() },
                colors = SurfaceDefaults.colors(
                    containerColor = Color.Black.copy(alpha = 0.95f)
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // 标题行
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.video_info_related_video_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }

                    if (videos.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.video_info_related_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            modifier = Modifier.fillMaxWidth(),
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(
                                items = videos,
                                key = { _, video -> video.avid }
                            ) { index, videoData ->
                                SmallVideoCard(
                                    modifier = Modifier
                                        .then(
                                            if (index == 0) Modifier.focusRequester(
                                                firstCardFocusRequester
                                            ) else Modifier
                                        ),
                                    data = videoData,
                                    onClick = {
                                        if (videoData.jumpToSeason) {
                                            onOpenSeasonInfo(videoData)
                                        } else {
                                            onOpenVideoInfo(videoData)
                                        }
                                    },
                                    onLongClick = {
                                        if (videoData.upId > 0) {
                                            UpInfoActivity.actionStart(
                                                context,
                                                mid = videoData.upId,
                                                name = videoData.upName,
                                                face = videoData.upFace
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
