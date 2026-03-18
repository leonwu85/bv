package dev.aaa1115910.bv.player.tv.controller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.sponsorblock.SponsorCategory
import dev.aaa1115910.biliapi.entity.sponsorblock.SponsorSegment
import kotlinx.coroutines.delay

/**
 * SponsorBlock 跳过提示组件
 */
@Composable
fun SponsorBlockTip(
    modifier: Modifier = Modifier,
    show: Boolean,
    segment: SponsorSegment?,
    onSkip: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    var remainingTime by remember { mutableIntStateOf(3) }

    // 3秒倒计时
    LaunchedEffect(show) {
        if (show) {
            remainingTime = 3
            repeat(3) {
                delay(1000)
                remainingTime--
            }
            onDismiss?.invoke()
        }
    }

    // 预留文字字段
    val categoryDisplayName = when (segment?.categoryEnum) {
        SponsorCategory.SPONSOR -> "恰饭广告"
        SponsorCategory.INTRO -> "片头"
        SponsorCategory.OUTRO -> "片尾"
        SponsorCategory.INTERACTION -> "互动提醒"
        SponsorCategory.SELF_PROMO -> "自我推广"
        SponsorCategory.PREVIEW -> "预告/回顾"
        SponsorCategory.MUSIC_OFF_TOPIC -> "非音乐部分"
        SponsorCategory.FILLER -> "填充内容"
        else -> "片段"
    }

    AnimatedVisibility(
        visible = show,
        enter = expandHorizontally(),
        exit = shrinkHorizontally()
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 64.dp),
                colors = SurfaceDefaults.colors(
                    containerColor = Color.Black.copy(alpha = 0.4f)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    modifier = Modifier.padding(12.dp),
                    text = "${categoryDisplayName}来袭，按确认键可跳过 (${remainingTime}s)",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
    }
}
