package dev.aaa1115910.bv.player.tv

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import dev.aaa1115910.bv.player.seekbar.SeekBarThumb
import dev.aaa1115910.bv.player.seekbar.SeekMoveState
import dev.aaa1115910.bv.player.tv.theme.PlayerColors
import dev.aaa1115910.bv.util.formatHourMinSec
import kotlin.math.max

@Composable
fun VideoSeekBar(
    modifier: Modifier = Modifier,
    duration: Long,
    position: Long,
    bufferedPercentage: Int,
    idleIcon: String = "",
    movingIcon: String = "",
    moveState: SeekMoveState = SeekMoveState.Idle,
    showPosition: Boolean = false,
    isFocused: Boolean = false,
) {
    VideoSeekBar(
        modifier = modifier,
        duration = duration,
        position = position,
        bufferedPercentage = bufferedPercentage,
        useDefaultThumb = idleIcon.isBlank(),
        showPosition = showPosition,
        thumb = { thumbModifier ->
            SeekBarThumb(
                modifier = thumbModifier,
                state = moveState,
                idleJsonUrl = idleIcon,
                movingJsonUrl = movingIcon
            )
        },
        isFocused = isFocused
    )
}

@Composable
private fun VideoSeekBar(
    modifier: Modifier = Modifier,
    duration: Long,
    position: Long,
    bufferedPercentage: Int,
    colors: SliderColors = SliderDefaults.colors(),
    useDefaultThumb: Boolean = false,
    showPosition: Boolean = false,
    thumb: (@Composable (Modifier) -> Unit)? = null,
    isFocused: Boolean = false,
) {
    val barHeight by animateDpAsState(
        targetValue = if (isFocused) 12.dp else 6.dp,
        animationSpec = tween(200),
        label = "seekbar height"
    )

    BoxWithConstraints(
        modifier = modifier
    ) {
        val width = this.maxWidth

        ConstraintLayout(
            modifier = Modifier.fillMaxWidth()
        ) {
            val (positionText, seek, thumbIcon) = createRefs()

            // 自定义渐变进度条
            Canvas(
                modifier = Modifier
                    .constrainAs(seek) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom, 8.dp)
                    }
                    .border(
                        width = if (isFocused) 1.5.dp else 0.dp,
                        color = if (isFocused) PlayerColors.seekBarFocusBorder else Color.Transparent,
                        shape = RoundedCornerShape(barHeight / 2)
                    )
                    .padding(horizontal = 6.dp, vertical = 1.dp)
                    .fillMaxWidth()
                    .height(barHeight)
            ) {
                val cornerRadius = CornerRadius(size.height / 2, size.height / 2)
                val progressFraction = if (duration > 0) (position.toFloat() / duration) else 0f
                val bufferedFraction = max(progressFraction, bufferedPercentage / 100f)

                // 未播放轨道
                drawRoundRect(
                    color = PlayerColors.seekBarTrack,
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = cornerRadius
                )

                // 已缓冲轨道
                drawRoundRect(
                    color = PlayerColors.seekBarBuffered,
                    topLeft = Offset.Zero,
                    size = Size(size.width * bufferedFraction, size.height),
                    cornerRadius = cornerRadius
                )

                // 已播放轨道 (渐变)
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = PlayerColors.progressGradientColors
                    ),
                    topLeft = Offset.Zero,
                    size = Size(size.width * progressFraction, size.height),
                    cornerRadius = cornerRadius
                )

                // 焦点时显示白色圆形 Thumb
                if (isFocused && duration > 0) {
                    val thumbRadius = 8f
                    val thumbCenterX = (size.width * progressFraction).coerceIn(thumbRadius, size.width - thumbRadius)
                    drawCircle(
                        color = PlayerColors.seekBarThumb,
                        radius = thumbRadius,
                        center = Offset(thumbCenterX, center.y)
                    )
                }
            }

            thumb?.invoke(
                Modifier
                    .constrainAs(thumbIcon) {
                        start.linkTo(
                            parent.start,
                            (width - 48.dp) * (position / max(duration.toFloat(), 1f))
                        )
                        bottom.linkTo(seek.bottom)
                        top.linkTo(seek.top)
                    }
            )
            if (showPosition) {
                Text(
                    text = position.formatHourMinSec(),
                    modifier = Modifier.constrainAs(positionText) {
                        start.linkTo(thumbIcon.start)
                        end.linkTo(thumbIcon.end)
                        bottom.linkTo(thumbIcon.top)
                    }
                )
            }
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun SeekWithThumbPreview(@PreviewParameter(ProgressProvider::class) data: Triple<Long, Long, Int>) {
    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        Surface {
            VideoSeekBar(
                duration = data.first,
                position = data.second,
                bufferedPercentage = data.third,
                showPosition = true,
                thumb = { modifier ->
                    SeekBarThumb(
                        modifier = modifier,
                        state = SeekMoveState.Idle,
                        idleJsonUrl = "https://i0.hdslb.com/bfs/garb/item/df917f079cd8175cc851cd1e19a197d810a1c6b7.json",
                        movingJsonUrl = "https://i0.hdslb.com/bfs/garb/item/b61bb387a4c895ef165798102ef322c631a9e4e1.json"
                    )
                },
                isFocused = true
            )
        }

    }
}

private class ProgressProvider : PreviewParameterProvider<Triple<Long, Long, Int>> {
    override val values = sequenceOf(
        Triple(1234_000L, 0L, 3),
        Triple(1234_000L, 234_000L, 24),
        Triple(1234_000L, 555_000L, 57),
        Triple(1234_000L, 1234_000L, 100)
    )
}
