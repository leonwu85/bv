package dev.aaa1115910.bv.player.tv.controller

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerPaymentData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerStateData
import dev.aaa1115910.bv.player.tv.theme.PlayerColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import qrcode.QRCode
import qrcode.color.DefaultColorFunction
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@Composable
fun PlayStateTips(
    modifier: Modifier = Modifier,
    canShowPause: Boolean = true
) {
    val videoPlayerStateData = LocalVideoPlayerStateData.current
    val videoPlayerPaymentData = LocalVideoPlayerPaymentData.current

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AnimatedVisibility(
            visible = !videoPlayerStateData.isPlaying && !videoPlayerStateData.isBuffering && !videoPlayerStateData.isError && canShowPause,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                PauseIcon(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                )
            }
        }
        if (videoPlayerStateData.isBuffering && !videoPlayerStateData.isError) {
            BufferingTip(
                modifier = Modifier
                    .align(Alignment.Center),
                speed = ""
            )
        }
        if (videoPlayerStateData.isError) {
            PlayErrorTip(
                modifier = Modifier.align(Alignment.Center),
                exception = videoPlayerStateData.exception
            )
        }
        if (videoPlayerPaymentData.needPay) {
            PaidRequireTip(
                modifier = Modifier.align(Alignment.Center),
                epid = videoPlayerPaymentData.epid
            )
        }
    }
}

@Composable
fun PauseIcon(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        colors = SurfaceDefaults.colors(
            containerColor = PlayerColors.tipBackground
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            modifier = Modifier
                .padding(12.dp, 8.dp)
                .size(48.dp),
            imageVector = Icons.Rounded.Pause,
            contentDescription = null,
            tint = PlayerColors.textPrimary
        )
    }
}

@Composable
fun BufferingTip(
    modifier: Modifier = Modifier,
    speed: String
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = PlayerColors.textPrimary,
            strokeWidth = 3.dp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "缓冲中...$speed",
            color = PlayerColors.textPrimary,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun PlayErrorTip(
    modifier: Modifier = Modifier,
    exception: Exception?
) {
    Surface(
        modifier = modifier,
        colors = SurfaceDefaults.colors(
            containerColor = PlayerColors.tipBackground
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp, 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "播放器正在抽风",
                style = MaterialTheme.typography.headlineSmall,
                color = PlayerColors.textPrimary
            )
            Text(
                text = " _(:з」∠)_",
                color = PlayerColors.textSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "错误信息：${exception?.message ?: "未知错误"}",
                color = PlayerColors.textTertiary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "按任意键重试",
                color = PlayerColors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun PaidRequireTip(
    modifier: Modifier = Modifier,
    epid: Int,
) {
    val scope = rememberCoroutineScope()
    var qrImage by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val output = ByteArrayOutputStream()
            val url = "https://b23.tv/ep$epid"
            QRCode(
                data = url,
                colorFn = DefaultColorFunction(
                    foreground = android.graphics.Color.WHITE,
                    background = android.graphics.Color.TRANSPARENT
                )
            )
                .render()
                .writeImage(output)
            val input = ByteArrayInputStream(output.toByteArray())
            val newQrImage = BitmapFactory.decodeStream(input).asImageBitmap()
            withContext(Dispatchers.Main) {
                qrImage = newQrImage
            }
        }
    }
    Surface(
        modifier = modifier,
        colors = SurfaceDefaults.colors(
            containerColor = PlayerColors.tipBackground
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp, 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "请先购买影片",
                style = MaterialTheme.typography.headlineSmall,
                color = PlayerColors.textPrimary
            )
            Text(
                text = "(・∀・)つ㊿",
                color = PlayerColors.textSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            AnimatedVisibility(visible = qrImage != null) {
                Image(
                    modifier = Modifier
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                    bitmap = qrImage!!,
                    contentDescription = "EP$epid QR Code"
                )
            }
        }
    }
}

@Preview
@Composable
private fun PauseIconPreview() {
    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        Box(modifier = Modifier.padding(10.dp)) {
            PauseIcon()
        }
    }
}

@Preview
@Composable
private fun BufferingTipPreview() {
    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        BufferingTip(
            modifier = Modifier.padding(10.dp),
            speed = ""
        )
    }
}

@Preview
@Composable
private fun PlayErrorTipPreview() {
    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        PlayErrorTip(exception = Exception("This is a test exception."))
    }
}

@Preview
@Composable
private fun PaidRequireTipPreview() {
    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        PaidRequireTip(epid = 752900)
    }
}