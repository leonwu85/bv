package dev.aaa1115910.bv.tv.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import dev.aaa1115910.biliapi.entity.Picture
import kotlinx.coroutines.delay
import kotlin.math.max

@Composable
fun CommentImagePreviewDialog(
    show: Boolean,
    pictures: List<Picture>,
    initialIndex: Int = 0,
    onDismissRequest: () -> Unit
) {
    if (!show || pictures.isEmpty()) return

    val context = LocalContext.current
    var currentIndex by remember(show, pictures, initialIndex) {
        mutableIntStateOf(initialIndex.coerceIn(0, pictures.lastIndex))
    }
    var scale by remember(show, pictures, initialIndex) { mutableFloatStateOf(1f) }
    var rotation by remember(show, pictures, initialIndex) { mutableFloatStateOf(0f) }
    var offsetX by remember(show, pictures, initialIndex) { mutableFloatStateOf(0f) }
    var offsetY by remember(show, pictures, initialIndex) { mutableFloatStateOf(0f) }
    var previewAreaSize by remember { mutableStateOf(IntSize.Zero) }
    val imageFocusRequester = remember { FocusRequester() }
    val previewImageRequest = remember(context, pictures, currentIndex) {
        ImageRequest.Builder(context)
            .data(pictures[currentIndex].url)
            .size(Size.ORIGINAL)
            .allowHardware(false)
            .build()
    }

    fun maxTranslation(): Float {
        val baseSize = max(previewAreaSize.width, previewAreaSize.height).toFloat()
        return ((scale - 1f) * baseSize / 2f).coerceAtLeast(0f)
    }

    fun updateOffset(deltaX: Float = 0f, deltaY: Float = 0f) {
        val maxTranslation = maxTranslation()
        offsetX = (offsetX + deltaX).coerceIn(-maxTranslation, maxTranslation)
        offsetY = (offsetY + deltaY).coerceIn(-maxTranslation, maxTranslation)
    }

    LaunchedEffect(currentIndex) {
        scale = 1f
        rotation = 0f
        offsetX = 0f
        offsetY = 0f
    }

    LaunchedEffect(scale) {
        if (scale <= 1f) {
            offsetX = 0f
            offsetY = 0f
        } else {
            val maxTranslation = maxTranslation()
            offsetX = offsetX.coerceIn(-maxTranslation, maxTranslation)
            offsetY = offsetY.coerceIn(-maxTranslation, maxTranslation)
        }
    }

    BackHandler(enabled = show) {
        onDismissRequest()
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.94f),
            colors = SurfaceDefaults.colors(
                containerColor = Color.Black.copy(alpha = 0.96f)
            ),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "图片预览 ${currentIndex + 1}/${pictures.size}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Text(
                    text = if (pictures.size > 1) {
                        "图片区左右键切换，下键进入工具栏，返回键关闭"
                    } else {
                        "下键进入工具栏，返回键关闭"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .onSizeChanged { previewAreaSize = it }
                        .focusRequester(imageFocusRequester)
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.DirectionLeft -> {
                                    if (pictures.size > 1) {
                                        currentIndex = (currentIndex - 1).coerceAtLeast(0)
                                        true
                                    } else {
                                        false
                                    }
                                }

                                Key.DirectionRight -> {
                                    if (pictures.size > 1) {
                                        currentIndex = (currentIndex + 1).coerceAtMost(pictures.lastIndex)
                                        true
                                    } else {
                                        false
                                    }
                                }

                                Key.Back, Key.Escape -> {
                                    onDismissRequest()
                                    true
                                }

                                else -> false
                            }
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                rotationZ = rotation
                                translationX = offsetX
                                translationY = offsetY
                            },
                        model = previewImageRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Fit
                    )
                }
                Text(
                    text = "缩放 ${String.format("%.2f", scale)}x  旋转 ${rotation.toInt()}°  偏移(${offsetX.toInt()}, ${offsetY.toInt()})",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            scale = (scale + 0.25f).coerceAtMost(3f)
                        }
                    ) {
                        Text(text = "放大")
                    }
                    OutlinedButton(
                        onClick = {
                            scale = (scale - 0.25f).coerceAtLeast(1f)
                        }
                    ) {
                        Text(text = "缩小")
                    }
                    Button(
                        onClick = {
                            rotation = (rotation + 90f) % 360f
                        }
                    ) {
                        Text(text = "旋转")
                    }
                    Button(
                        onClick = {
                            updateOffset(deltaY = -80f)
                        },
                        enabled = scale > 1f
                    ) {
                        Text(text = "上移")
                    }
                    Button(
                        onClick = {
                            updateOffset(deltaY = 80f)
                        },
                        enabled = scale > 1f
                    ) {
                        Text(text = "下移")
                    }
                    Button(
                        onClick = {
                            updateOffset(deltaX = -80f)
                        },
                        enabled = scale > 1f
                    ) {
                        Text(text = "左移")
                    }
                    Button(
                        onClick = {
                            updateOffset(deltaX = 80f)
                        },
                        enabled = scale > 1f
                    ) {
                        Text(text = "右移")
                    }
                    OutlinedButton(
                        onClick = {
                            scale = 1f
                            rotation = 0f
                            offsetX = 0f
                            offsetY = 0f
                        }
                    ) {
                        Text(text = "重置")
                    }
                }
            }
        }
    }

    LaunchedEffect(show) {
        if (show) {
            delay(120)
            imageFocusRequester.requestFocus()
            delay(80)
            imageFocusRequester.requestFocus()
        }
    }
}