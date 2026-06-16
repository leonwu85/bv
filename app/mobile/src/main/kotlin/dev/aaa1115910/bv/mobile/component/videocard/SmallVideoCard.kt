package dev.aaa1115910.bv.mobile.component.videocard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.resizedImageUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallVideoCard(
    modifier: Modifier = Modifier,
    data: VideoCardData,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.62f))
    ) {
        Column {
            Box(
                modifier = Modifier.clip(MaterialTheme.shapes.medium),
                contentAlignment = Alignment.BottomCenter
            ) {
                AsyncImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.6f)
                        .clip(MaterialTheme.shapes.medium),
                    model = data.cover.resizedImageUrl(ImageSize.SmallVideoCardCover),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds
                )
                if (data.coverBadges.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        data.coverBadges.forEach { badge ->
                            SmallVideoCardBadge(
                                text = badge,
                                isCharging = badge.contains("充电")
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f)
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
                        if (data.playString != "") {
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
                                    text = data.playString,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                        }
                        if (data.danmakuString != "") {
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
                                    text = data.danmakuString,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Text(
                        text = data.timeString,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = data.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        minLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    VideoCardMoreMenu(data = data)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UpIcon()
                    Text(
                        text = data.upName,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallVideoCardBadge(
    text: String,
    isCharging: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.surface.luminance() < 0.5f
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
        shape = MaterialTheme.shapes.extraSmall,
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            text = text,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview
@Composable
fun SmallVideoCardPreview() {
    val data = VideoCardData(
        avid = 0,
        title = "震惊！太震惊了！真的是太震惊了！我的天呐！真TMD震惊！",
        cover = "http://i2.hdslb.com/bfs/archive/af17fc07b8f735e822563cc45b7b5607a491dfff.jpg",
        upName = "bishi",
        play = 2333,
        danmaku = 666,
        time = 2333 * 1000,
        isInteractive = true
    )
    BVMobileTheme {
        Surface {
            SmallVideoCard(
                data = data
            )
        }
    }
}

@Preview
@Composable
fun SmallVideoCardsPreview() {
    val data = VideoCardData(
        avid = 0,
        title = "震惊！太震惊了！真的是太震惊了！我的天呐！真TMD震惊！",
        cover = "http://i2.hdslb.com/bfs/archive/af17fc07b8f735e822563cc45b7b5607a491dfff.jpg",
        upName = "bishi",
        play = 2333,
        danmaku = 666,
        time = 2333 * 1000,
        isInteractive = true
    )
    BVMobileTheme {
        Surface {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2)
            ) {
                repeat(20) {
                    item {
                        SmallVideoCard(
                            data = data
                        )
                    }
                }
            }
        }
    }
}
