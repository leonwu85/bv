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

            // 蒙版坐标属于视频图像，只能映射到居中显示的实际图像区域。
            val contentBounds = VideoContentGeometry.fitCenter(
                containerWidth = size.width,
                containerHeight = size.height,
                videoAspectRatio = videoAspectRatio,
            )
            val dstOffset = androidx.compose.ui.unit.IntOffset(
                x = contentBounds.left.toInt(),
                y = contentBounds.top.toInt(),
            )
            val dstSize = androidx.compose.ui.unit.IntSize(
                width = contentBounds.width.toInt(),
                height = contentBounds.height.toInt(),
            ).takeIf { it.width > 0 && it.height > 0 } ?: size.toIntSize()

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
    bitmapMask(renderMobMaskToBitmap(frame, reusableBitmap = null), videoAspectRatio)
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
    return renderMaskFrameToBitmap(frame, reusableBitmap = null)
}

fun renderMaskFrameToBitmap(
    frame: DanmakuMaskFrame,
    reusableBitmap: Bitmap?
): Bitmap {
    return when (frame) {
        is DanmakuWebMaskFrame -> renderWebMaskToBitmap(frame, reusableBitmap)
        is DanmakuMobMaskFrame -> renderMobMaskToBitmap(frame, reusableBitmap)
    }
}

private fun renderWebMaskToBitmap(
    frame: DanmakuWebMaskFrame,
    reusableBitmap: Bitmap?
): Bitmap {
    val svgObj = runCatching {
        SVG.getFromString(frame.svg)
    }.getOrNull() ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    val svgWidth = svgObj.documentWidth.toInt()
    val svgHeight = svgObj.documentHeight.toInt()

    val bitmap = reusableBitmap?.takeIf {
        !it.isRecycled &&
                it.width == svgWidth &&
                it.height == svgHeight
    } ?: Bitmap.createBitmap(svgWidth, svgHeight, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Color.TRANSPARENT)
    val canvas = Canvas(bitmap)
    svgObj.renderToCanvas(canvas)

    return bitmap
}

private fun renderMobMaskToBitmap(
    frame: DanmakuMobMaskFrame,
    reusableBitmap: Bitmap?
): Bitmap {
    val bitmap = reusableBitmap?.takeIf {
        !it.isRecycled &&
                it.width == frame.width &&
                it.height == frame.height
    } ?: Bitmap.createBitmap(frame.width, frame.height, Bitmap.Config.ARGB_8888)
    val totalPixels = frame.width * frame.height
    val pixels = IntArray(totalPixels) { Color.BLACK }

    var pixelIndex = 0
    for (byteValue in frame.image) {
        if (pixelIndex >= totalPixels) break

        val bits = byteValue.toInt() and 0xFF
        val limit = minOf(8, totalPixels - pixelIndex)
        if (bits != 0) {
            for (bitIndex in 0 until limit) {
                if (((bits ushr (7 - bitIndex)) and 0x01) != 0) {
                    pixels[pixelIndex + bitIndex] = Color.TRANSPARENT
                }
            }
        }
        pixelIndex += limit
    }

    bitmap.setPixels(pixels, 0, frame.width, 0, 0, frame.width, frame.height)
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
