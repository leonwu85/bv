package dev.aaa1115910.bv.mobile.screen.settings.details

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.mobile.component.preferences.items.radioPreference
import dev.aaa1115910.bv.mobile.component.preferences.items.switchPreference
import dev.aaa1115910.bv.mobile.component.preferences.preferenceGroups
import dev.aaa1115910.bv.mobile.settings.MobilePrefKeys
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme
import dev.aaa1115910.bv.player.entity.DanmakuSpeedMode
import dev.aaa1115910.bv.player.entity.PlayMode
import dev.aaa1115910.bv.player.entity.PlayerDefaultStartPosition
import dev.aaa1115910.bv.player.entity.PortraitVideoFixMode

@Composable
fun PlayContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 18.dp)
    ) {
        preferenceGroups(
            "画面" to {
                radioPreference(
                    title = "竖屏视频修复",
                    prefReq = MobilePrefKeys.portraitVideoFixModeRequest,
                    values = PortraitVideoFixMode.entries.associate { it.value to it.displayName(context) }
                )
            },
            "播放" to {
                radioPreference(
                    title = "默认倍速",
                    prefReq = MobilePrefKeys.currentPlaySpeedRequest,
                    values = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
                        .associateWith { "${it}x" }
                )
                radioPreference(
                    title = "连播模式",
                    prefReq = MobilePrefKeys.defaultPlayModeRequest,
                    values = PlayMode.entries.associate { it.ordinal to it.getDisplayName(context) }
                )
                radioPreference(
                    title = "默认起播位置",
                    prefReq = MobilePrefKeys.playerDefaultStartPositionRequest,
                    values = PlayerDefaultStartPosition.entries.associate { it.value to it.displayName(context) }
                )
                switchPreference(
                    title = "自动播放",
                    prefReq = MobilePrefKeys.autoPlayRequest,
                    onCheckedChange = { true }
                )
                switchPreference(
                    title = "循环播放",
                    prefReq = MobilePrefKeys.isLoopRequest,
                    onCheckedChange = { true }
                )
                switchPreference(
                    title = "隐藏观看记录",
                    prefReq = MobilePrefKeys.incognitoModeRequest,
                    onCheckedChange = { true }
                )
            },
            "弹幕" to {
                switchPreference(
                    title = "默认显示弹幕",
                    prefReq = MobilePrefKeys.defaultDanmakuEnabledRequest,
                    onCheckedChange = { true }
                )
                radioPreference(
                    title = "弹幕大小",
                    prefReq = MobilePrefKeys.defaultMobileDanmakuScaleRequest,
                    values = mapOf(
                        0.6f to "小",
                        0.8f to "默认",
                        1f to "大",
                        1.2f to "特大"
                    )
                )
                radioPreference(
                    title = "弹幕透明度",
                    prefReq = MobilePrefKeys.defaultDanmakuOpacityRequest,
                    values = mapOf(
                        0.4f to "40%",
                        0.6f to "60%",
                        0.8f to "80%",
                        1f to "100%"
                    )
                )
                radioPreference(
                    title = "弹幕区域",
                    prefReq = MobilePrefKeys.defaultDanmakuAreaRequest,
                    values = mapOf(
                        0.25f to "1/4 屏",
                        0.5f to "半屏",
                        0.75f to "3/4 屏",
                        1f to "全屏"
                    )
                )
                switchPreference(
                    title = "智能过滤",
                    summary = "合并重复和相似弹幕",
                    prefReq = MobilePrefKeys.defaultDanmakuMergeEnabledRequest,
                    onCheckedChange = { true }
                )
                radioPreference(
                    title = "弹幕过滤等级",
                    prefReq = MobilePrefKeys.defaultDanmakuFilterLevelRequest,
                    values = (1..10).associateWith { it.toString() }
                )
                radioPreference(
                    title = "默认弹幕速度模式",
                    prefReq = MobilePrefKeys.defaultDanmakuSpeedModeRequest,
                    values = DanmakuSpeedMode.entries.associate { it.ordinal to it.getDisplayName(context) }
                )
                radioPreference(
                    title = "默认自定义弹幕速度",
                    prefReq = MobilePrefKeys.defaultDanmakuPresentationSpeedRequest,
                    values = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
                        .associateWith { "${it}x" }
                )
            },
            "直播" to {
                switchPreference(
                    title = "直播弹幕表情",
                    prefReq = MobilePrefKeys.showLiveDanmakuEmojiRequest,
                    onCheckedChange = { true }
                )
                radioPreference(
                    title = "直播弹幕过滤等级",
                    prefReq = MobilePrefKeys.defaultLiveDanmakuFilterLevelRequest,
                    values = (0..60).associateWith { it.toString() }
                )
                switchPreference(
                    title = "直播隐藏观看记录",
                    prefReq = MobilePrefKeys.liveIncognitoModeRequest,
                    onCheckedChange = { true }
                )
            }
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlayContentPreview() {
    BVMobileTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            PlayContent(
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
