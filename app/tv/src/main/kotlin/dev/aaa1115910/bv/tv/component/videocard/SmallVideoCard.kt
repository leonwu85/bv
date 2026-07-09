package dev.aaa1115910.bv.tv.component.videocard

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.tv.component.UpIcon
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.resizedImageUrl

private val InteractiveBadgeColor = Color(0xFFFFD54F)
private val ChargingBadgeColor = Color(0xFF00FFFF)

@Composable
fun SmallVideoCard(
    modifier: Modifier = Modifier,
    data: VideoCardData,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onFocus: () -> Unit = {},
    onFocusStateChanged: (Boolean) -> Unit = {},
    overlay: @Composable BoxScope.(hasFocus: Boolean) -> Unit = {},
    initialFocus: Boolean = false
) {
    var hasFocus by remember { mutableStateOf(initialFocus) }
    val shape = MaterialTheme.shapes.medium
    val focusBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    val focusFillColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
    val density = LocalDensity.current
    val borderWidthPx = with(density) { 2.dp.toPx() }
    val cornerRadiusPx = with(density) {
        // medium shape 通常为圆角；取近似值绘制焦点框，避免焦点切换时改 Modifier 触发 relayout
        12.dp.toPx()
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged {
                hasFocus = it.isFocused
                onFocusStateChanged(hasFocus)
                if (hasFocus) onFocus()
            }
            // 用 draw 画焦点框，避免 border Modifier 增删导致 measure/layout
            .drawWithContent {
                drawContent()
                if (hasFocus) {
                    drawRoundRect(
                        color = focusBorderColor,
                        cornerRadius = CornerRadius(cornerRadiusPx),
                        style = Stroke(width = borderWidthPx)
                    )
                }
            },
        onClick = onClick,
        onLongClick = onLongClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = if (hasFocus) focusFillColor else Color.Transparent,
            pressedContainerColor = focusFillColor
        ),
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        scale = ClickableSurfaceDefaults.scale(scale = 1f, focusedScale = 1f)
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 3.dp, start = 3.dp, end = 3.dp),
            ) {
                CardCover(
                    modifier = Modifier.clip(shape),
                    cover = data.cover,
                    play = data.playString,
                    danmaku = data.danmakuString,
                    time = data.timeString,
                    badges = data.coverBadges
                )
                Spacer(modifier = Modifier.height(8.dp))
                CardInfo(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(79.dp)
                        .padding(horizontal = 1.dp),
                    title = data.title,
                    upName = data.upName,
                    pubTime = data.pubTime
                )
            }
            overlay(hasFocus)
        }
    }
}

@Composable
private fun CoverBottomInfo(
    modifier: Modifier = Modifier,
    play: String,
    danmaku: String
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (play.isNotBlank()) {
            Icon(
                painter = painterResource(id = R.drawable.ic_play_count),
                contentDescription = null,
                tint = Color.White
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = play,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }

        if (danmaku.isNotBlank()) {
            if (play.isNotBlank()) Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_danmaku_count),
                contentDescription = null,
                tint = Color.White
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = danmaku,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }
    }
}

@Composable
fun CardCover(
    modifier: Modifier = Modifier,
    cover: String,
    play: String,
    danmaku: String,
    time: String,
    badges: List<String> = emptyList()
) {
    val context = LocalContext.current
    // TV 网格列宽通常 >160dp，去掉 BoxWithConstraints 避免每张卡多一轮 measure
    val showInfo = play.isNotBlank() || danmaku.isNotBlank()
    val imageModel = remember(cover, context) {
        ImageRequest.Builder(context)
            .data(cover.resizedImageUrl(ImageSize.SmallVideoCardCover))
            // 与远端 640x400 裁剪尺寸对齐，降低解码/绑图成本
            .size(640, 400)
            .crossfade(false)
            .build()
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            model = imageModel,
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
        // 右上角信息区域：徽章和时长
        Column(
            modifier = Modifier
                .padding(5.dp)
                .align(Alignment.TopEnd),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            badges.forEach { badge ->
                if (badge == "互动视频") {
                    Row(
                        modifier = Modifier
                            .background(
                                color = Color.Black.copy(0.5f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(vertical = 1.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayCircle,
                            contentDescription = null,
                            tint = InteractiveBadgeColor
                        )
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.bodySmall,
                            color = InteractiveBadgeColor,
                            maxLines = 1
                        )
                    }
                } else {
                    Text(
                        modifier = Modifier
                            .background(
                                color = Color.Black.copy(0.5f),
                                shape = MaterialTheme.shapes.extraSmall
                            )
                            .padding(vertical = 1.dp, horizontal = 2.dp),
                        text = badge,
                        style = MaterialTheme.typography.bodySmall,
                        color = ChargingBadgeColor,
                        maxLines = 1
                    )
                }
            }
            // 时长显示
            if (time.isNotEmpty()) {
                Text(
                    modifier = Modifier
                        .background(
                            color = Color.Black.copy(0.5f),
                            shape = MaterialTheme.shapes.extraSmall
                        )
                        .padding(vertical = 1.dp, horizontal = 4.dp),
                    text = time,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
        if (showInfo) {
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

            CoverBottomInfo(
                play = play,
                danmaku = danmaku
            )
        }
    }
}

@Composable
private fun CardInfo(
    modifier: Modifier = Modifier,
    title: String,
    upName: String,
    pubTime: String?
) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier,
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (upName.isNotEmpty()) {
                UpIcon()
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 2.dp),
                    text = upName,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            pubTime?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Visible
                )
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SmallVideoCardWithoutFocusPreview() {
    val data = VideoCardData(
        avid = 0,
        title = "震惊！太震惊了！真的是太震惊了！我的天呐！真TMD震惊！",
        cover = "http://i2.hdslb.com/bfs/archive/af17fc07b8f735e822563cc45b7b5607a491dfff.jpg",
        upName = "震惊！太震惊了！真的是太震惊了！我的天呐！真TMD震惊！",
        play = 2333,
        danmaku = 666,
        time = 2333 * 1000,
        pubTime = "3小时前",
        isChargingArc = true
    )
    BVTheme {
        Surface(
            modifier = Modifier.width(300.dp)
        ) {
            SmallVideoCard(
                modifier = Modifier.padding(20.dp),
                data = data,
                initialFocus = false
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SmallVideoCardWithFocusPreview() {
    val data = VideoCardData(
        avid = 0,
        title = "震惊！太震惊了！真的是太震惊了！我的天呐！真TMD震惊！",
        cover = "http://i2.hdslb.com/bfs/archive/af17fc07b8f735e822563cc45b7b5607a491dfff.jpg",
        upName = "bishi",
        play = 2333,
        danmaku = 666,
        time = 2333 * 1000,
        pubTime = "3小时前"
    )
    BVTheme {
        Surface(
            modifier = Modifier.width(300.dp)
        ) {
            SmallVideoCard(
                modifier = Modifier.padding(20.dp),
                data = data,
                initialFocus = true
            )
        }
    }
}

@Preview(device = "id:tv_1080p")
@Preview(device = "id:tv_1080p", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SmallVideoCardsPreview() {
    val data = VideoCardData(
        avid = 0,
        title = "震惊！太震惊了！真的是太震惊了！我的天呐！真TMD震惊！",
        //cover = "http://i2.hdslb.com/bfs/archive/af17fc07b8f735e822563cc45b7b5607a491dfff.jpg",
        cover = "",
        upName = "bishi",
        play = 2333,
        danmaku = 666,
        time = 2333 * 1000,
        pubTime = "3小时前"
    )
    BVTheme {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
