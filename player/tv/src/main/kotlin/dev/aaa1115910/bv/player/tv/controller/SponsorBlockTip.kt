package dev.aaa1115910.bv.player.tv.controller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.sponsorblock.SponsorCategory
import dev.aaa1115910.biliapi.entity.sponsorblock.SponsorSegment
import dev.aaa1115910.bv.player.tv.component.PlayerAnimations
import dev.aaa1115910.bv.player.tv.theme.PlayerColors
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
    var remainingTime by remember { mutableIntStateOf(10) }

    // 10秒倒计时
    LaunchedEffect(show) {
        if (show) {
            remainingTime = 10
            repeat(10) {
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
        enter = PlayerAnimations.tipEnter,
        exit = PlayerAnimations.tipExit
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp),
                colors = SurfaceDefaults.colors(
                    containerColor = PlayerColors.tipBackground
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    text = "${categoryDisplayName}来袭，按确认键可跳过 (${remainingTime}s)",
                    style = MaterialTheme.typography.titleMedium,
                    color = PlayerColors.textPrimary
                )
            }
        }
    }
}
