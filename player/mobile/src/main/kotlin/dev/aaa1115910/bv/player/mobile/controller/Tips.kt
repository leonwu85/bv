package dev.aaa1115910.bv.player.mobile.controller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import dev.aaa1115910.biliapi.entity.sponsorblock.SponsorSegment
import dev.aaa1115910.bv.player.shared.R
import dev.aaa1115910.bv.util.formatHourMinSec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SeekMoveTip(
    modifier: Modifier = Modifier,
    show: Boolean,
    startTime: Long,
    move: Long,
    totalTime: Long
) {
    if (show) {
        Box(
            modifier = modifier
                .fillMaxSize()
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center),
                color = Color.Black.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    modifier = Modifier.padding(12.dp),
                    text = "${
                        if (startTime + move > totalTime) {
                            totalTime.formatHourMinSec()
                        } else {
                            (startTime + move).formatHourMinSec()
                        }
                    }/${totalTime.formatHourMinSec()}",
                    color = Color.White
                )
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun SeekMoveTipPreview() {
    MaterialTheme {
        Surface {
            SeekMoveTip(
                show = true,
                startTime = 2345L,
                move = 20L,
                totalTime = 23456L
            )
        }
    }
}

@Composable
fun QuickDoubleSpeedPlaybackTip(
    modifier: Modifier = Modifier,
    show: Boolean
) {
    if (show) {
        Box(
            modifier = modifier
                .fillMaxSize()
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center),
                color = Color.Black.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.medium

            ) {
                Text(
                    modifier = Modifier.padding(12.dp),
                    text = "x2 倍速播放中",
                    color = Color.White
                )
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun QuickDoubleSpeedPlaybackTipPreview() {
    MaterialTheme {
        Surface {
            QuickDoubleSpeedPlaybackTip(show = true)
        }
    }
}

@Composable
fun BrightnessTip(
    modifier: Modifier = Modifier,
    show: Boolean,
    progress: Float,
) {
    val scope = rememberCoroutineScope()

    val displayProgress by animateFloatAsState(
        targetValue = progress,
        label = "BrightnessTipProgress"
    )
    val showValue by rememberUpdatedState(show)
    var showTip by remember { mutableStateOf(false) }

    LaunchedEffect(showValue) {
        if (!showValue) {
            scope.launch(Dispatchers.Default) {
                delay(500)
                if (!showValue) showTip = false
            }
        } else {
            showTip = true
        }
    }

    if (showTip) {
        Box(
            modifier = modifier
                .fillMaxSize()
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center),
                color = Color.Black.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.medium

            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when {
                            progress < 1 / 3f -> Icons.Rounded.BrightnessLow
                            progress < 2 / 3f -> Icons.Rounded.BrightnessMedium
                            else -> Icons.Rounded.BrightnessHigh
                        },
                        contentDescription = null,
                        tint = Color.White
                    )
                    LinearProgressIndicator(
                        modifier = Modifier.width(100.dp),
                        progress = { displayProgress },
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun BrightnessTipPreview() {
    MaterialTheme {
        Surface {
            BrightnessTip(show = true, progress = 0.3f)
        }
    }
}

@Composable
fun VolumeTip(
    modifier: Modifier = Modifier,
    show: Boolean,
    progress: Float,
) {
    val scope = rememberCoroutineScope()

    val displayProgress by animateFloatAsState(
        targetValue = progress,
        label = "VolumeTipProgress"
    )
    val showValue by rememberUpdatedState(show)
    var showTip by remember { mutableStateOf(false) }

    LaunchedEffect(showValue) {
        if (!showValue) {
            scope.launch(Dispatchers.Default) {
                delay(500)
                if (!showValue) showTip = false
            }
        } else {
            showTip = true
        }
    }

    if (showTip) {
        Box(
            modifier = modifier
                .fillMaxSize()
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center),
                color = Color.Black.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.medium

            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when {
                            progress == 0f -> Icons.Rounded.VolumeOff
                            progress < 0.5f -> Icons.Rounded.VolumeDown
                            else -> Icons.Rounded.VolumeUp
                        },
                        contentDescription = null,
                        tint = Color.White
                    )
                    LinearProgressIndicator(
                        modifier = Modifier.width(100.dp),
                        progress = { displayProgress },
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun VolumeTipPreview() {
    MaterialTheme {
        Surface {
            VolumeTip(show = true, progress = 0.3f)
        }
    }
}

@Composable
fun SponsorBlockTip(
    modifier: Modifier = Modifier,
    show: Boolean,
    isFullScreen: Boolean,
    segment: SponsorSegment?,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    var remainingTime by remember { mutableStateOf(10) }
    val density = LocalDensity.current

    LaunchedEffect(show, segment?.UUID) {
        if (!show) return@LaunchedEffect
        remainingTime = 10
        repeat(10) {
            delay(1000)
            remainingTime--
        }
        onDismiss()
    }

    val tipContent: @Composable () -> Unit = {
        AnimatedVisibility(
            visible = show,
            enter = fadeIn() + slideInVertically { if (isFullScreen) it / 2 else -it / 2 },
            exit = fadeOut() + slideOutVertically { if (isFullScreen) it / 2 else -it / 2 }
        ) {
            val containerColor = if (isFullScreen) {
                Color.Black.copy(alpha = 0.62f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            }
            val contentColor = if (isFullScreen) Color.White else MaterialTheme.colorScheme.onSurface
            val tipShape = if (isFullScreen) {
                MaterialTheme.shapes.medium
            } else {
                RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 0.dp,
                    bottomStart = 12.dp,
                    bottomEnd = 12.dp
                )
            }
            Surface(
                modifier = Modifier.widthIn(max = 420.dp),
                color = containerColor,
                shape = tipShape,
                shadowElevation = if (isFullScreen) 0.dp else 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "${segment?.categoryEnum?.displayName ?: "广告片段"}来袭 (${remainingTime}s)",
                        color = contentColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = onSkip) {
                        Text(
                            text = "跳过",
                            color = if (isFullScreen) Color.White else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    if (isFullScreen) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
            ) { tipContent() }
        }
    } else {
        Popup(
            alignment = Alignment.BottomCenter,
            offset = IntOffset(0, with(density) { 60.dp.roundToPx() })
        ) {
            tipContent()
        }
    }
}

@Composable
fun AutoSkipSponsorTip(
    modifier: Modifier = Modifier,
    show: Boolean,
    isFullScreen: Boolean,
    skippedSeconds: Int
) {
    val density = LocalDensity.current
    val tipContent: @Composable () -> Unit = {
        AnimatedVisibility(
            visible = show && skippedSeconds > 0,
            enter = fadeIn() + slideInVertically { if (isFullScreen) it / 2 else -it / 2 },
            exit = fadeOut() + slideOutVertically { if (isFullScreen) it / 2 else -it / 2 }
        ) {
            val containerColor = if (isFullScreen) {
                Color.Black.copy(alpha = 0.62f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            }
            val contentColor = if (isFullScreen) Color.White else MaterialTheme.colorScheme.onSurface
            val tipShape = if (isFullScreen) {
                MaterialTheme.shapes.medium
            } else {
                RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 0.dp,
                    bottomStart = 12.dp,
                    bottomEnd = 12.dp
                )
            }
            Surface(
                modifier = Modifier.widthIn(max = 420.dp),
                color = containerColor,
                shape = tipShape,
                shadowElevation = if (isFullScreen) 0.dp else 6.dp
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    text = stringResource(R.string.video_player_auto_skip_sponsor_tip, skippedSeconds),
                    color = contentColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    if (isFullScreen) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
            ) { tipContent() }
        }
    } else {
        Popup(
            alignment = Alignment.BottomCenter,
            offset = IntOffset(0, with(density) { 60.dp.roundToPx() })
        ) {
            tipContent()
        }
    }
}
