package dev.aaa1115910.bv.tv.component.search

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import dev.aaa1115910.bv.tv.screens.search.SearchTheme

@Composable
fun SearchKeyword(
    modifier: Modifier = Modifier,
    index: Int? = null,
    keyword: String,
    leadingIcon: String,
    trailingIcon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var hasFocus by remember { mutableStateOf(false) }

    val backgroundColor by animateColorAsState(
        targetValue = if (hasFocus)
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else Color.Transparent,
        animationSpec = tween(150),
        label = "keyword bg"
    )
    val indicatorColor by animateColorAsState(
        targetValue = if (hasFocus) SearchTheme.accentPink else Color.Transparent,
        animationSpec = tween(150),
        label = "keyword indicator"
    )

    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(context)
            .data(data = leadingIcon)
            .size(Size.ORIGINAL)
            .build(),
        contentScale = ContentScale.FillHeight
    )
    val hasIcon = leadingIcon != "" && painter.state is AsyncImagePainter.State.Success

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { hasFocus = it.hasFocus },
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(SearchTheme.itemShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            pressedContainerColor = Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor, SearchTheme.itemShape)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 焦点指示器
            Box(
                modifier = Modifier
                    .width(SearchTheme.focusIndicatorWidth)
                    .height(16.dp)
                    .background(indicatorColor, SearchTheme.itemShape)
            )
            Spacer(modifier = Modifier.width(6.dp))

            // 序号
            if (index != null) {
                Text(
                    text = "$index",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (index <= 3) SearchTheme.accentPink
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            // 图标
            if (hasIcon) {
                Image(
                    modifier = Modifier.height(16.dp),
                    painter = painter,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            // 文字
            Text(
                modifier = Modifier.weight(1f),
                text = keyword,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 尾部图标
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(4.dp))
                trailingIcon()
            }
        }
    }
}