package dev.aaa1115910.bv.tv.screens.settings.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import dev.aaa1115910.bv.entity.PlayerType
import androidx.tv.material3.Text
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.DanmakuSpeedMode
import dev.aaa1115910.bv.player.entity.PlayerBottomProgressBarColor
import dev.aaa1115910.bv.player.entity.PortraitVideoFixMode
import dev.aaa1115910.bv.player.entity.PlayerLoadNextAction
import dev.aaa1115910.bv.player.entity.PlayerDefaultStartPosition
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.tv.component.settings.SettingListItemWithDialog
import dev.aaa1115910.bv.tv.component.settings.SettingSwitchListItem
import dev.aaa1115910.bv.tv.component.settings.SettingNumberListItem
import dev.aaa1115910.bv.tv.component.settings.SettingH265CodecPriorityListItem
import dev.aaa1115910.bv.tv.component.LibMPVDownloaderDialog
import dev.aaa1115910.bv.tv.util.TvMpvOptions
import dev.aaa1115910.bv.tv.component.LibVLCDownloaderDialog
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.tv.screens.settings.SettingsMenuNavItem
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.VlcLibsInstaller
import dev.aaa1115910.bv.player.BuildConfig
import dev.aaa1115910.bv.player.impl.mpv.MpvLibsInstaller
import android.widget.Toast
import androidx.tv.material3.Button

@Composable
fun PlayerSetting(
    modifier: Modifier = Modifier,
    onPlayerTypeChanged: (PlayerType) -> Unit = {}
) {
    val context = LocalContext.current

    var selectedResolution by remember { mutableStateOf(Prefs.defaultQuality) }
    var selectedOfflineCacheResolution by remember {
        mutableStateOf(Prefs.defaultOfflineCacheQuality)
    }
    var selectedVideoCodec by remember { mutableStateOf(Prefs.defaultVideoCodec) }
    var selectedH265CodecPriority by remember { mutableStateOf(Prefs.h265CodecPriority) }
    var selectedAudio by remember { mutableStateOf(Prefs.defaultAudio) }
    var enableFfmpegAudioRenderer by remember { mutableStateOf(Prefs.enableFfmpegAudioRenderer) }
    var showUGCVideoInfo by remember { mutableStateOf(Prefs.showUGCVideoInfo) }
    var playerShowBottomProgressBar by remember { mutableStateOf(Prefs.playerShowBottomProgressBar) }
    var supportManualVideoRotation by remember { mutableStateOf(Prefs.supportManualVideoRotation) }
    var playerCommentSplitScreen by remember { mutableStateOf(Prefs.playerCommentSplitScreen) }
    var playerBottomProgressBarColor by remember { mutableStateOf(Prefs.playerBottomProgressBarColor) }
    var playerShowDebugInfo by remember { mutableStateOf(Prefs.playerShowDebugInfo) }
    var debugDanmakuMaskDownsample180p by remember { mutableStateOf(Prefs.debugDanmakuMaskDownsample180p) }
    var subtitleSmartDisplay by remember { mutableStateOf(Prefs.subtitleSmartDisplay) }
    var playerExitWhenAllIsPlayed by remember { mutableStateOf(Prefs.playerExitWhenAllIsPlayed) }
    var playerLoadNextAction by remember { mutableStateOf(Prefs.playerLoadNextAction) }
    var playerDefaultStartPosition by remember { mutableStateOf(Prefs.playerDefaultStartPosition) }
    var playerEnableStartPositionSwitch by remember { mutableStateOf(Prefs.playerEnableStartPositionSwitch) }
    var defaultPlaybackSpeed by remember { mutableDoubleStateOf(Prefs.defaultPlaySpeed.toDouble()) }
    var playerSeekForwardStep by remember { mutableDoubleStateOf(Prefs.playerSeekForwardStep.toDouble()) }
    var playerSeekBackwardStep by remember { mutableDoubleStateOf(Prefs.playerSeekBackwardStep.toDouble()) }
    var portraitVideoFixMode by remember { mutableStateOf(Prefs.portraitVideoFixMode) }
    var defaultDanmakuArea by remember { mutableDoubleStateOf(Prefs.defaultDanmakuArea.toDouble()) }
    var defaultDanmakuSpeedMode by remember { mutableStateOf(Prefs.defaultDanmakuSpeedMode) }
    var defaultDanmakuPresentationSpeed by remember {
        mutableDoubleStateOf(Prefs.defaultDanmakuPresentationSpeed.toDouble())
    }
    var skipPgcIntroOutro by remember { mutableStateOf(Prefs.skipPgcIntroOutro) }
    var selectedPlayerType by remember { mutableStateOf(Prefs.playerType) }
    var enableAsyncQueueing by remember { mutableStateOf(Prefs.enableAsyncQueueing) }
    var enableTunneling by remember { mutableStateOf(Prefs.enableTvTunneling) }
    var enableAudioPlaybackParams by remember { mutableStateOf(Prefs.enableAudioPlaybackParams) }
    var showVlcDownloadConfirmDialog by remember { mutableStateOf(false) }
    var showVlcDownloaderDialog by remember { mutableStateOf(false) }
    var showMpvDownloadConfirmDialog by remember { mutableStateOf(false) }
    var showMpvDownloaderDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = SettingsMenuNavItem.Player.getDisplayName(context),
            style = MaterialTheme.typography.displaySmall
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingListItemWithDialog(
                    title = stringResource(R.string.settings_item_resolution),
                    supportText = stringResource(R.string.settings_item_resolution),
                    options = Resolution.entries.reversed(),
                    getDisplayName = { item, ctx -> item.getDisplayName(ctx) },
                    value = selectedResolution,
                    onValueChange = {
                        Prefs.defaultQuality = it
                        selectedResolution = it
                    }
                )
            }
            item {
                SettingListItemWithDialog(
                    title = stringResource(R.string.settings_item_codec),
                    supportText = stringResource(R.string.settings_item_codec),
                    options = VideoCodec.entries.filter { it != VideoCodec.DVH1 && it != VideoCodec.HVC1 },
                    getDisplayName = { item, ctx -> item.getDisplayName(ctx) },
                    value = selectedVideoCodec,
                    onValueChange = {
                        Prefs.defaultVideoCodec = it
                        selectedVideoCodec = it
                    }
                )
            }
            if (selectedVideoCodec == VideoCodec.HEVC) {
                item {
                    SettingH265CodecPriorityListItem(
                        title = stringResource(R.string.settings_item_h265_codec_priority),
                        supportText = stringResource(R.string.settings_item_h265_codec_priority_text),
                        value = selectedH265CodecPriority,
                        onValueChange = { priority ->
                            Prefs.h265CodecPriority = priority
                            selectedH265CodecPriority = Prefs.h265CodecPriority
                        }
                    )
                }
            }
            item {
                SettingListItemWithDialog(
                    title = stringResource(R.string.settings_item_audio),
                    supportText = stringResource(R.string.settings_item_codec),
                    options = Audio.entries,
                    getDisplayName = { item, ctx -> item.getDisplayName(ctx) },
                    value = selectedAudio,
                    onValueChange = {
                        Prefs.defaultAudio = it
                        selectedAudio = it
                    }
                )
            }
            item {
                SettingListItemWithDialog(
                    title = "默认缓存画质",
                    supportText = "缓存时优先选择此画质；不可用时自动向下匹配",
                    options = Resolution.entries.reversed(),
                    getDisplayName = { item, ctx -> item.getDisplayName(ctx) },
                    value = selectedOfflineCacheResolution,
                    onValueChange = {
                        Prefs.defaultOfflineCacheQuality = it
                        selectedOfflineCacheResolution = it
                    }
                )
            }
            item {
                SettingListItemWithDialog(
                    title = stringResource(R.string.settings_item_player_type),
                    supportText = stringResource(R.string.settings_item_player_type),
                    options = PlayerType.entries,
                    getDisplayName = { item, _ -> item.name },
                    value = selectedPlayerType,
                    onValueChange = { newType ->
                        when (newType) {
                            PlayerType.VLC -> {
                                // 检查 VLC 库是否需要更新（未安装或版本不匹配）
                                if (VlcLibsInstaller.needsUpdate(context, BuildConfig.libVLCVersion)) {
                                    // 显示下载确认弹窗
                                    showVlcDownloadConfirmDialog = true
                                } else {
                                    selectedPlayerType = newType
                                    Prefs.playerType = newType
                                    onPlayerTypeChanged(newType)
                                }
                            }
                            PlayerType.MPV -> {
                                // 检查 MPV 官方组件是否已安装且与当前固定的 release 版本一致
                                if (MpvLibsInstaller.needsUpdate(context)) {
                                    showMpvDownloadConfirmDialog = true
                                } else {
                                    selectedPlayerType = newType
                                    Prefs.playerType = newType
                                    onPlayerTypeChanged(newType)
                                    if (!TvMpvOptions.supportsZeroCopyHwdec) {
                                        Toast.makeText(context, TvMpvOptions.LOW_API_MPV_HINT, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            else -> {
                                selectedPlayerType = newType
                                Prefs.playerType = newType
                                onPlayerTypeChanged(newType)
                            }
                        }
                    }
                )
            }
            item {
                SettingSwitchListItem(
                    title = stringResource(R.string.settings_other_ffmpeg_audio_renderer_title),
                    supportText = stringResource(R.string.settings_other_ffmpeg_audio_renderer_text),
                    checked = enableFfmpegAudioRenderer,
                    onCheckedChange = {
                        enableFfmpegAudioRenderer = it
                        Prefs.enableFfmpegAudioRenderer = it
                    }
                )
            }
            item {
                SettingSwitchListItem(
                    title = stringResource(R.string.settings_show_ugc_video_info_title),
                    supportText = stringResource(R.string.settings_show_ugc_video_info_text),
                    checked = showUGCVideoInfo,
                    onCheckedChange = {
                        showUGCVideoInfo = it
                        Prefs.showUGCVideoInfo = it
                    }
                )
            }
            item {
                SettingSwitchListItem(
                    title = stringResource(R.string.settings_player_show_bottom_progress_bar_title),
                    supportText = stringResource(R.string.settings_player_show_bottom_progress_bar_text),
                    checked = playerShowBottomProgressBar,
                    onCheckedChange = {
                        playerShowBottomProgressBar = it
                        Prefs.playerShowBottomProgressBar = it
                    }
                )
            }
            item {
                SettingSwitchListItem(
                    title = stringResource(R.string.settings_player_support_manual_video_rotation_title),
                    supportText = stringResource(R.string.settings_player_support_manual_video_rotation_text),
                    checked = supportManualVideoRotation,
                    onCheckedChange = {
                        supportManualVideoRotation = it
                        Prefs.supportManualVideoRotation = it
                    }
                )
            }
            item {
                SettingSwitchListItem(
                    title = stringResource(R.string.settings_player_comment_split_screen_title),
                    supportText = stringResource(R.string.settings_player_comment_split_screen_text),
                    checked = playerCommentSplitScreen,
                    onCheckedChange = {
                        playerCommentSplitScreen = it
                        Prefs.playerCommentSplitScreen = it
                    }
                )
            }
            if (playerShowBottomProgressBar) {
                item {
                    SettingListItemWithDialog(
                        title = stringResource(R.string.settings_player_bottom_progress_bar_color_title),
                        supportText = stringResource(R.string.settings_player_bottom_progress_bar_color_text),
                        options = PlayerBottomProgressBarColor.entries,
                        getDisplayName = { item, ctx -> item.displayName(ctx) },
                        value = playerBottomProgressBarColor,
                        onValueChange = {
                            playerBottomProgressBarColor = it
                            Prefs.playerBottomProgressBarColor = it
                        }
                    )
                }
            }
            item {
                SettingSwitchListItem(
                    title = stringResource(R.string.settings_player_subtitle_smart_display_title),
                    supportText = stringResource(R.string.settings_player_subtitle_smart_display_text),
                    checked = subtitleSmartDisplay,
                    onCheckedChange = {
                        subtitleSmartDisplay = it
                        Prefs.subtitleSmartDisplay = it
                    }
                )
            }
            item {
                SettingSwitchListItem(
                    title = stringResource(R.string.settings_player_show_debug_info_title),
                    supportText = stringResource(R.string.settings_player_show_debug_info_text),
                    checked = playerShowDebugInfo,
                    onCheckedChange = {
                        playerShowDebugInfo = it
                        Prefs.playerShowDebugInfo = it
                    }
                )
            }
            item {
                SettingSwitchListItem(
                    title = "弹幕蒙版 180p 短边降采样",
                    supportText = "降低普通模式 GL 弹幕蒙版上传尺寸，竖屏视频会按短边保留蒙版精度",
                    checked = debugDanmakuMaskDownsample180p,
                    onCheckedChange = {
                        debugDanmakuMaskDownsample180p = it
                        Prefs.debugDanmakuMaskDownsample180p = it
                    }
                )
            }
            item {
                SettingListItemWithDialog(
                    title = stringResource(R.string.settings_portrait_video_fix_mode_title),
                    supportText = stringResource(R.string.settings_portrait_video_fix_mode_text),
                    options = PortraitVideoFixMode.entries,
                    getDisplayName = { item, ctx -> item.displayName(ctx) },
                    value = portraitVideoFixMode,
                    onValueChange = {
                        portraitVideoFixMode = it
                        Prefs.portraitVideoFixMode = it
                    }
                )
            }
            item {
                SettingListItemWithDialog(
                    title = stringResource(R.string.settings_player_load_next_action_title),
                    supportText = stringResource(R.string.settings_player_load_next_action_text),
                    options = PlayerLoadNextAction.entries,
                    getDisplayName = { item, ctx -> item.displayName(ctx) },
                    value = playerLoadNextAction,
                    onValueChange = {
                        playerLoadNextAction = it
                        Prefs.playerLoadNextAction = it
                    }
                )
            }
            item {
                SettingListItemWithDialog(
                    title = stringResource(R.string.settings_player_default_start_position_title),
                    supportText = stringResource(R.string.settings_player_default_start_position_text),
                    options = PlayerDefaultStartPosition.entries,
                    getDisplayName = { item, ctx -> item.displayName(ctx) },
                    value = playerDefaultStartPosition,
                    onValueChange = {
                        playerDefaultStartPosition = it
                        Prefs.playerDefaultStartPosition = it
                    }
                )
            }
            item {
                SettingSwitchListItem(
                    title = stringResource(
                        if (playerDefaultStartPosition == PlayerDefaultStartPosition.History) {
                            R.string.settings_player_enable_start_position_switch_to_beginning_title
                        } else {
                            R.string.settings_player_enable_start_position_switch_to_history_title
                        }
                    ),
                    supportText = stringResource(R.string.settings_player_enable_start_position_switch_text),
                    checked = playerEnableStartPositionSwitch,
                    onCheckedChange = {
                        playerEnableStartPositionSwitch = it
                        Prefs.playerEnableStartPositionSwitch = it
                    }
                )
            }
            item {
                SettingSwitchListItem(
                    title = stringResource(R.string.settings_player_exit_when_all_is_played_title),
                    supportText = stringResource(R.string.settings_player_exit_when_all_is_played_text),
                    checked = playerExitWhenAllIsPlayed,
                    onCheckedChange = {
                        playerExitWhenAllIsPlayed = it
                        Prefs.playerExitWhenAllIsPlayed = it
                    }
                )
            }
            item {
                SettingNumberListItem(
                    title = stringResource(R.string.settings_player_default_playback_speed_title),
                    supportText = stringResource(R.string.settings_player_default_playback_speed_text),
                    value = defaultPlaybackSpeed,
                    minValue = 0.25,
                    maxValue = 2.5,
                    isInteger = false,
                    step = 0.25,
                    onValueChange = {
                        defaultPlaybackSpeed = it
                        Prefs.defaultPlaySpeed = it.toFloat()
                    }
                )
            }
            item {
                SettingNumberListItem(
                    title = stringResource(R.string.settings_player_seek_forward_step_title),
                    supportText = stringResource(R.string.settings_player_seek_forward_step_text),
                    value = playerSeekForwardStep,
                    minValue = 5.0,
                    maxValue = 30.0,
                    isInteger = true,
                    step = 1.0,
                    onValueChange = {
                        playerSeekForwardStep = it
                        Prefs.playerSeekForwardStep = it.toInt()
                    }
                )
            }
            item {
                SettingNumberListItem(
                    title = stringResource(R.string.settings_player_seek_backward_step_title),
                    supportText = stringResource(R.string.settings_player_seek_backward_step_text),
                    value = playerSeekBackwardStep,
                    minValue = 5.0,
                    maxValue = 30.0,
                    isInteger = true,
                    step = 1.0,
                    onValueChange = {
                        playerSeekBackwardStep = it
                        Prefs.playerSeekBackwardStep = it.toInt()
                    }
                )
            }
            item {
                SettingNumberListItem(
                    title = stringResource(R.string.settings_player_default_danmaku_area_title),
                    supportText = stringResource(R.string.settings_player_default_danmaku_area_text),
                    value = defaultDanmakuArea * 100,
                    minValue = 10.0,
                    maxValue = 100.0,
                    isInteger = true,
                    step = 10.0,
                    valueFormat = "%.0f%%",
                    onValueChange = {
                        defaultDanmakuArea = it / 100
                        Prefs.defaultDanmakuArea = (it / 100).toFloat()
                    }
                )
            }
            item {
                SettingListItemWithDialog(
                    title = "默认弹幕速度模式",
                    supportText = "点播弹幕展示速度策略",
                    options = DanmakuSpeedMode.entries,
                    getDisplayName = { item, ctx -> item.getDisplayName(ctx) },
                    value = defaultDanmakuSpeedMode,
                    onValueChange = {
                        defaultDanmakuSpeedMode = it
                        Prefs.defaultDanmakuSpeedMode = it
                    }
                )
            }
            item {
                SettingNumberListItem(
                    title = "默认自定义弹幕速度",
                    supportText = "仅在弹幕速度模式为自定义时生效",
                    value = defaultDanmakuPresentationSpeed,
                    minValue = 0.5,
                    maxValue = 2.0,
                    isInteger = false,
                    step = 0.05,
                    valueFormat = "%.2fx",
                    onValueChange = {
                        defaultDanmakuPresentationSpeed = it
                        Prefs.defaultDanmakuPresentationSpeed = it.toFloat()
                    }
                )
            }
            item {
                SettingSwitchListItem(
                    title = "跳过 PGC 片头片尾(实验性)",
                    supportText = "自动跳过 PGC 片头片尾",
                    checked = skipPgcIntroOutro,
                    onCheckedChange = {
                        skipPgcIntroOutro = it
                        Prefs.skipPgcIntroOutro = it
                    }
                )
            }
            // ExoPlayer/Media3 专用设置
            if (selectedPlayerType == PlayerType.Media3) {
                item {
                    SettingSwitchListItem(
                        title = "启用异步缓冲队列",
                        supportText = "减少丢帧和音频欠载，提升高帧率视频播放性能（Android 6.0-11 有效）",
                        checked = enableAsyncQueueing,
                        onCheckedChange = {
                            enableAsyncQueueing = it
                            Prefs.enableAsyncQueueing = it
                        }
                    )
                }
                item {
                    SettingSwitchListItem(
                        title = "启用隧道模式",
                        supportText = "使用硬件音频路径，可能提升播放性能但可能影响兼容性",
                        checked = enableTunneling,
                        onCheckedChange = {
                            enableTunneling = it
                            Prefs.enableTvTunneling = it
                        }
                    )
                }
                item {
                    SettingSwitchListItem(
                        title = "启用音频播放参数调整",
                        supportText = "允许调整音频播放速度和音效",
                        checked = enableAudioPlaybackParams,
                        onCheckedChange = {
                            enableAudioPlaybackParams = it
                            Prefs.enableAudioPlaybackParams = it
                        }
                    )
                }
            }
        }
    }

    // VLC 下载确认弹窗
    if (showVlcDownloadConfirmDialog) {
        TvAlertDialog(
            onDismissRequest = { showVlcDownloadConfirmDialog = false },
            title = { Text("需要下载 VLC 组件") },
            text = {
                Text("VLC 播放器需要下载额外的组件才能使用。\n\n" +
                     "下载大小：约 80 MB\n" +
                     "建议在 Wi-Fi 环境下下载")
            },
            confirmButton = {
                Button(onClick = {
                    showVlcDownloadConfirmDialog = false
                    showVlcDownloaderDialog = true
                }) {
                    Text("下载")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showVlcDownloadConfirmDialog = false
                }) {
                    Text("取消")
                }
            }
        )
    }

    // VLC 库下载弹窗
    if (showVlcDownloaderDialog) {
        LibVLCDownloaderDialog(
            show = true,
            onDismissRequest = {
                showVlcDownloaderDialog = false
            },
            onDownloadComplete = {
                showVlcDownloaderDialog = false
                selectedPlayerType = PlayerType.VLC
                Prefs.playerType = PlayerType.VLC
                onPlayerTypeChanged(PlayerType.VLC)
            },
            onDownloadFailed = { errorMessage ->
                showVlcDownloaderDialog = false
                Toast.makeText(context, "下载失败: $errorMessage", Toast.LENGTH_LONG).show()
            }
        )
    }

    // MPV 下载确认弹窗
    if (showMpvDownloadConfirmDialog) {
        TvAlertDialog(
            onDismissRequest = { showMpvDownloadConfirmDialog = false },
            title = { Text("需要下载 MPV 组件") },
            text = {
                Text(
                    "MPV 播放器需要下载官方 mpv-android 组件（${MpvLibsInstaller.expectedVersion}）才能使用。\n\n" +
                            "来源：mpv-android 官方 GitHub Release，安装前会校验官方签名\n" +
                            "连接失败时会自动尝试 GitHub 镜像\n" +
                            "建议在 Wi-Fi 环境下下载"
                )
            },
            confirmButton = {
                Button(onClick = {
                    showMpvDownloadConfirmDialog = false
                    showMpvDownloaderDialog = true
                }) {
                    Text("下载")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showMpvDownloadConfirmDialog = false
                }) {
                    Text("取消")
                }
            }
        )
    }

    // MPV 库下载弹窗
    if (showMpvDownloaderDialog) {
        LibMPVDownloaderDialog(
            show = true,
            onDismissRequest = {
                showMpvDownloaderDialog = false
            },
            onDownloadComplete = {
                showMpvDownloaderDialog = false
                selectedPlayerType = PlayerType.MPV
                Prefs.playerType = PlayerType.MPV
                onPlayerTypeChanged(PlayerType.MPV)
                val hint = if (TvMpvOptions.supportsZeroCopyHwdec) "" else "。${TvMpvOptions.LOW_API_MPV_HINT}"
                Toast.makeText(context, "MPV 组件下载完成$hint", Toast.LENGTH_LONG).show()
            },
            onDownloadFailed = { errorMessage ->
                showMpvDownloaderDialog = false
                Toast.makeText(context, "下载失败: $errorMessage", Toast.LENGTH_LONG).show()
            }
        )
    }

}
