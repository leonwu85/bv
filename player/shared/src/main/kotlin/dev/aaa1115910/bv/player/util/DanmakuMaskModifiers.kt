package dev.aaa1115910.bv.player.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.toIntSize
import com.caverock.androidsvg.SVG
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMaskFrame
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMobMaskFrame
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuWebMaskFrame

fun Modifier.bitmapMask(
    bitmap: Bitmap,
    videoAspectRatio: Float = 0f
): Modifier = composed {
    drawWithContent {
        if (bitmap.isRecycled) {
            drawContent()
            return@drawWithContent
        }

        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(Offset.Zero, size), Paint())
            drawContent()

            // 计算视频居中显示时的实际区域
            val (dstOffset, dstSize) = if (videoAspectRatio > 0f) {
                val containerAspectRatio = size.width / size.height
                if (videoAspectRatio > containerAspectRatio) {
                    // 视频更宽，上下有黑边 (letterbox)
                    val videoHeight = size.width / videoAspectRatio
                    val offsetY = (size.height - videoHeight) / 2
                    androidx.compose.ui.unit.IntOffset(0, offsetY.toInt()) to
                            androidx.compose.ui.unit.IntSize(size.width.toInt(), videoHeight.toInt())
                } else {
                    // 视频更高，左右有黑边 (pillarbox)
                    val videoWidth = size.height * videoAspectRatio
                    val offsetX = (size.width - videoWidth) / 2
                    androidx.compose.ui.unit.IntOffset(offsetX.toInt(), 0) to
                            androidx.compose.ui.unit.IntSize(videoWidth.toInt(), size.height.toInt())
                }
            } else {
                // 无有效的视频宽高比，使用全尺寸
                androidx.compose.ui.unit.IntOffset.Zero to size.toIntSize()
            }

            if (!bitmap.isRecycled) {
                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstOffset = dstOffset,
                    dstSize = dstSize,
                    blendMode = BlendMode.DstIn
                )
            }
            canvas.restore()
        }
    }
}

fun Modifier.danmakuWebMask(
    frame: DanmakuWebMaskFrame,
    videoAspectRatio: Float = 0f
): Modifier = composed {
    val svgObj = runCatching {
        SVG.getFromString(frame.svg)
    }.getOrNull() ?: return@composed this

    val svgWidth = svgObj.documentWidth.toInt()
    val svgHeight = svgObj.documentHeight.toInt()

    val bitmap = Bitmap.createBitmap(svgWidth, svgHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    svgObj.renderToCanvas(canvas)

    bitmapMask(bitmap, videoAspectRatio)
}

fun Modifier.danmakuMobMask(
    frame: DanmakuMobMaskFrame,
    videoAspectRatio: Float = 0f
): Modifier = composed {
    bitmapMask(renderMobMaskToBitmap(frame), videoAspectRatio)
}

fun Modifier.danmakuMask(
    frame: DanmakuMaskFrame?,
    videoAspectRatio: Float = 0f
): Modifier = composed {
    if (frame == null) return@composed this

    when (frame) {
        is DanmakuWebMaskFrame -> danmakuWebMask(frame, videoAspectRatio)
        is DanmakuMobMaskFrame -> danmakuMobMask(frame, videoAspectRatio)
    }
}

/**
 * 预渲染蒙版帧到 Bitmap
 * 可在后台线程调用，避免阻塞主线程
 */
fun renderMaskFrameToBitmap(frame: DanmakuMaskFrame): Bitmap {
    return when (frame) {
        is DanmakuWebMaskFrame -> renderWebMaskToBitmap(frame)
        is DanmakuMobMaskFrame -> renderMobMaskToBitmap(frame)
    }
}

private fun renderWebMaskToBitmap(frame: DanmakuWebMaskFrame): Bitmap {
    val svgObj = runCatching {
        SVG.getFromString(frame.svg)
    }.getOrNull() ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    val svgWidth = svgObj.documentWidth.toInt()
    val svgHeight = svgObj.documentHeight.toInt()

    val bitmap = Bitmap.createBitmap(svgWidth, svgHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    svgObj.renderToCanvas(canvas)

    return bitmap
}

private fun renderMobMaskToBitmap(frame: DanmakuMobMaskFrame): Bitmap {
    val bitmap = Bitmap.createBitmap(frame.width, frame.height, Bitmap.Config.ARGB_8888)
    val totalPixels = frame.width * frame.height

    for (pixelIndex in 0 until totalPixels) {
        val byteIndex = pixelIndex / 8
        if (byteIndex >= frame.image.size) break

        val bitOffset = 7 - (pixelIndex % 8)
        val bit = (frame.image[byteIndex].toInt() ushr bitOffset) and 0x01
        val x = pixelIndex % frame.width
        val y = pixelIndex / frame.width
        bitmap.setPixel(x, y, if (bit == 0) Color.BLACK else Color.TRANSPARENT)
    }
    return bitmap
}

/**
 * 使用预渲染的 Bitmap 应用蒙版
 */
fun Modifier.danmakuMaskBitmap(
    bitmap: Bitmap?,
    videoAspectRatio: Float = 0f
): Modifier = composed {
    if (bitmap == null || bitmap.isRecycled) return@composed this
    bitmapMask(bitmap, videoAspectRatio)
}