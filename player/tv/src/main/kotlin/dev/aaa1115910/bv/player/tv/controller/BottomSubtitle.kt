package dev.aaa1115910.bv.player.tv.controller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerSeekState
import dev.aaa1115910.bv.player.tv.theme.PlayerColors

@Composable
private fun SubtitleBubble(
    text: String,
    isAI: Boolean,
    fontSize: TextUnit,
    backgroundOpacity: Float,
    modifier: Modifier = Modifier,
    onHeightChanged: (Int) -> Unit = {}
) {
    Row(
        modifier = modifier.onSizeChanged { onHeightChanged(it.height) }
    ) {
        Text(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = backgroundOpacity))
                .padding(vertical = 6.dp, horizontal = 14.dp),
            text = text,
            fontSize = fontSize,
            textAlign = TextAlign.Center,
            color = PlayerColors.textPrimary
        )
        if (isAI) {
            Text(
                modifier = Modifier
                    .padding(start = 4.dp, top = 2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PlayerColors.accentPink.copy(alpha = 0.7f))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                text = "AI",
                fontSize = (fontSize.value / 3).sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun BottomSubtitle(
    modifier: Modifier = Modifier
) {
    val videoPlayerConfigData = LocalVideoPlayerConfigData.current
    val videoPlayerSeekState = LocalVideoPlayerSeekState.current
    val time = videoPlayerSeekState.position
    val density = LocalDensity.current
    val currentSubtitleItem = videoPlayerConfigData.currentSubtitleData.find { it.isShowing(time) }
    val currentSecondarySubtitleItem =
        videoPlayerConfigData.currentSecondarySubtitleData.find { it.isShowing(time) }
    val secondarySubtitleEnabled = videoPlayerConfigData.currentSecondarySubtitleId != -1L

    var secondarySubtitleHeightPx by remember { mutableIntStateOf(0) }
    val measuredSecondaryHeight = with(density) { secondarySubtitleHeightPx.toDp() }
    val estimatedSecondaryHeight = if (secondarySubtitleEnabled) {
        with(density) { videoPlayerConfigData.currentSecondarySubtitleFontSize.toDp() } + 24.dp
    } else {
        0.dp
    }
    val secondaryOccupiedHeight = if (measuredSecondaryHeight > estimatedSecondaryHeight) {
        measuredSecondaryHeight
    } else {
        estimatedSecondaryHeight
    }
    val primaryBottomPadding: Dp = if (secondarySubtitleEnabled) {
        videoPlayerConfigData.currentSecondarySubtitleBottomPadding +
                secondaryOccupiedHeight +
                videoPlayerConfigData.currentSubtitleBottomPadding +
                8.dp
    } else {
        videoPlayerConfigData.currentSubtitleBottomPadding
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AnimatedVisibility(
            visible = currentSubtitleItem?.content?.isNotEmpty() == true,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = primaryBottomPadding)
        ) {
            SubtitleBubble(
                text = currentSubtitleItem?.content.orEmpty(),
                isAI = currentSubtitleItem?.isAI == true,
                fontSize = videoPlayerConfigData.currentSubtitleFontSize,
                backgroundOpacity = videoPlayerConfigData.currentSubtitleBackgroundOpacity,
                onHeightChanged = {}
            )
        }

        AnimatedVisibility(
            visible = currentSecondarySubtitleItem?.content?.isNotEmpty() == true,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = videoPlayerConfigData.currentSecondarySubtitleBottomPadding)
        ) {
            SubtitleBubble(
                text = currentSecondarySubtitleItem?.content.orEmpty(),
                isAI = currentSecondarySubtitleItem?.isAI == true,
                fontSize = videoPlayerConfigData.currentSecondarySubtitleFontSize,
                backgroundOpacity = videoPlayerConfigData.currentSecondarySubtitleBackgroundOpacity,
                onHeightChanged = { secondarySubtitleHeightPx = it }
            )
        }
    }
}