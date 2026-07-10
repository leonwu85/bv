package dev.aaa1115910.bv.tv.component.live

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.live.LiveHistoryItem
import dev.aaa1115910.biliapi.entity.ugc.toSmartDate
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.ifElse
import dev.aaa1115910.bv.util.resizedImageUrl
import dev.aaa1115910.bv.tv.util.deferredTvImageModel

@Composable
fun LiveHistoryCard(
    modifier: Modifier = Modifier,
    data: LiveHistoryItem,
    onClick: () -> Unit = {},
    onFocus: () -> Unit = {}
) {
    var hasFocus by remember { mutableStateOf(false) }
    val isLiving = data.liveStatus == 1
    val metadataText = buildList {
        if (data.areaName.isNotBlank()) add(data.areaName)
        if (data.viewAt > 0) add(data.viewAt.toSmartDate())
    }.joinToString(separator = " · ")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged {
                hasFocus = it.isFocused
                if (hasFocus) onFocus()
            }
            .ifElse(
                hasFocus,
                Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium
                )
            ),
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = if (hasFocus) {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
            } else {
                Color.Transparent
            },
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                AsyncImage(
                    model = deferredTvImageModel(
                        data.cover.resizedImageUrl(ImageSize.LargeCover)
                    ),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .background(
                            color = if (isLiving) {
                                Color(0xFFFF6699).copy(alpha = 0.7f)
                            } else {
                                Color.Black.copy(alpha = 0.65f)
                            },
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = if (isLiving) "直播中" else "未开播",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = data.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = deferredTvImageModel(data.face.resizedImageUrl(ImageSize.Icon)),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = data.uname,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }

            if (metadataText.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = metadataText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
