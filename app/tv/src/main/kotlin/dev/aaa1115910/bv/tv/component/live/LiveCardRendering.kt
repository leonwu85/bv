package dev.aaa1115910.bv.tv.component.live

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import dev.aaa1115910.bv.tv.util.deferredTvImageModel

/** Read focus in drawing so D-pad movement does not recompose or relayout the whole card. */
@Composable
internal fun Modifier.liveCardFocus(onFocus: () -> Unit): Modifier {
    val focused = remember { mutableStateOf(false) }
    val shape = MaterialTheme.shapes.medium
    val borderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    return onFocusChanged {
        focused.value = it.isFocused
        if (it.isFocused) onFocus()
    }.drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val stroke = Stroke(2.dp.toPx())
        onDrawWithContent {
            drawContent()
            if (focused.value) drawOutline(outline, borderColor, style = stroke)
        }
    }
}

@Composable
internal fun LiveCardImage(
    url: String,
    loadImages: Boolean,
    modifier: Modifier = Modifier,
) {
    // A null Coil model still builds a painter and its request machinery. Keep that work
    // out of the tab-switch frame, while preserving the card's measured image bounds.
    if (!loadImages) {
        Box(modifier)
        return
    }
    val context = LocalContext.current
    val request = remember(context, url) {
        ImageRequest.Builder(context)
            .data(url)
            .precision(Precision.INEXACT)
            .crossfade(false)
            .build()
    }
    AsyncImage(
        model = deferredTvImageModel(request),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop,
    )
}
