package dev.aaa1115910.bv.tv.component.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.live.LiveRoomItem
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.resizedImageUrl

private val LiveCoverGradient = Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
)

@Composable
fun LiveRoomCard(
    modifier: Modifier = Modifier,
    data: LiveRoomItem,
    onClick: () -> Unit = {},
    onFocus: () -> Unit = {},
    loadImages: Boolean = true,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .liveCardFocus(onFocus),
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
            pressedContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        scale = ClickableSurfaceDefaults.scale(scale = 1f, focusedScale = 1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp)
        ) {
            // 封面
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                LiveCardImage(
                    url = data.cover.resizedImageUrl(ImageSize.SmallVideoCardCover),
                    loadImages = loadImages,
                    modifier = Modifier.fillMaxSize(),
                )

                // 底部渐变遮罩
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(LiveCoverGradient)
                )

                // 直播中标识
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .background(
                            color = Color(0xFFFF6699).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = "直播中",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }

                // 在线人数
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.BottomStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(id = R.drawable.ic_play_count),
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatViewCount(
                            data.watchedShow?.num ?: (data.online / 10)
                        ),
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 直播间标题
            Text(
                text = data.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 主播信息
            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 主播头像
                LiveCardImage(
                    url = data.face.resizedImageUrl(ImageSize.Icon),
                    loadImages = loadImages,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = data.uname,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

/**
 * 格式化观看人数
 */
private fun formatViewCount(count: Int): String {
    return when {
        count >= 100_000_000 -> String.format(Locale.US, "%.1f亿", count / 100_000_000.0)
        count >= 10_000 -> String.format(Locale.US, "%.1f万", count / 10_000.0)
        else -> count.toString()
    }
}
