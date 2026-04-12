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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerSeekState
import dev.aaa1115910.bv.player.tv.theme.PlayerColors

@Composable
fun BottomSubtitle(
    modifier: Modifier = Modifier
) {
    val videoPlayerConfigData = LocalVideoPlayerConfigData.current
    val videoPlayerSeekState = LocalVideoPlayerSeekState.current
    val subtitleData = videoPlayerConfigData.currentSubtitleData
    val time = videoPlayerSeekState.position

    var currentText by remember { mutableStateOf("") }
    var isAI by remember { mutableStateOf(false) }

    val updateCurrentText: () -> Unit = {
        runCatching {
            val currentItem = subtitleData.find { it.isShowing(time) }
            currentText = currentItem?.content ?: ""
            isAI = currentItem?.isAI ?: false
        }
    }

    LaunchedEffect(time) {
        updateCurrentText()
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AnimatedVisibility(
            visible = currentText.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = videoPlayerConfigData.currentSubtitleBottomPadding)
        ) {
            Row {
                Text(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = videoPlayerConfigData.currentSubtitleBackgroundOpacity))
                        .padding(vertical = 6.dp, horizontal = 14.dp),
                    text = currentText,
                    fontSize = videoPlayerConfigData.currentSubtitleFontSize,
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
                        fontSize = (videoPlayerConfigData.currentSubtitleFontSize.value / 3).sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}