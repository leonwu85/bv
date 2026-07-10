package dev.aaa1115910.bv.tv.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import dev.aaa1115910.bv.tv.util.LocalTvImageLoadingAllowed
import dev.aaa1115910.bv.tv.util.tvImageMemoryPolicy
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.resizedImageUrl

enum class TvDynamicImageUseCase(
    val fallbackWidth: Int,
    val fallbackHeight: Int
) {
    ListPreview(fallbackWidth = 640, fallbackHeight = 320),
    DetailInline(fallbackWidth = 1280, fallbackHeight = 720)
}

@Composable
fun TvSafeDynamicImage(
    url: String,
    sourceWidth: Int,
    sourceHeight: Int,
    modifier: Modifier = Modifier,
    useCase: TvDynamicImageUseCase,
    imageSize: ImageSize? = null,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    shape: Shape = RoundedCornerShape(8.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    emptyText: String? = null
) {
    val context = LocalContext.current
    val imageLoadingAllowed = LocalTvImageLoadingAllowed.current
    val imageMemoryPolicy = remember(context) { context.tvImageMemoryPolicy() }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val isLongImage = sourceWidth > 0 && sourceHeight > sourceWidth * 3
    val targetImageSize = imageSize ?: when (useCase) {
        TvDynamicImageUseCase.ListPreview -> ImageSize.DynamicPreview
        TvDynamicImageUseCase.DetailInline ->
            if (isLongImage) imageMemoryPolicy.detailLongImageSize else imageMemoryPolicy.detailImageSize
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .onSizeChanged {
                if (containerSize != it) containerSize = it
            },
        contentAlignment = Alignment.Center
    ) {
        if (url.isBlank()) {
            emptyText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f)
                )
            }
        } else if (imageLoadingAllowed && containerSize != IntSize.Zero) {
            val requestWidth = containerSize.width
            val requestHeight = containerSize.height
            val imageRequest = remember(
                context,
                imageMemoryPolicy,
                url,
                targetImageSize,
                requestWidth,
                requestHeight
            ) {
                val (targetWidth, targetHeight) = imageMemoryPolicy.containerRequestSize(
                    viewportWidth = requestWidth,
                    viewportHeight = requestHeight
                )
                ImageRequest.Builder(context)
                    .data(url.resizedImageUrl(targetImageSize))
                    .size(targetWidth, targetHeight)
                    .crossfade(false)
                    .precision(Precision.INEXACT)
                    .allowHardware(useCase == TvDynamicImageUseCase.ListPreview)
                    .build()
            }

            AsyncImage(
                model = imageRequest,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                alignment = alignment,
                contentScale = contentScale
            )
        }
    }
}
