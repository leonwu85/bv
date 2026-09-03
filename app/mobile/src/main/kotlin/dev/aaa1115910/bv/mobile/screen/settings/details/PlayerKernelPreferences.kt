package dev.aaa1115910.bv.mobile.screen.settings.details

import android.content.Context
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.aaa1115910.bv.entity.PlayerType
import dev.aaa1115910.bv.mobile.component.LibMPVDownloaderDialog
import dev.aaa1115910.bv.mobile.component.LibVLCDownloaderDialog
import dev.aaa1115910.bv.mobile.component.preferences.PreferenceGroupScope
import dev.aaa1115910.bv.mobile.component.preferences.items.radioPreference
import dev.aaa1115910.bv.mobile.settings.MobilePrefKeys
import dev.aaa1115910.bv.mobile.settings.MobilePrefs
import dev.aaa1115910.bv.mobile.util.MobileMpvOptions
import dev.aaa1115910.bv.player.impl.mpv.MpvLibsInstaller
import dev.aaa1115910.bv.player.impl.vlc.VlcNativeLibs
import dev.aaa1115910.bv.util.VlcLibsInstaller

/**
 * 播放器内核选择及其组件下载流程（MPV 组件、LibVLC 3/4 组件），供「音视频」「高级」两页共用，
 * 行为与 TV 端 `PlayerSetting` 一致：选 MPV/VLC 时组件缺失或版本不符先下载，下载完成再切换内核；
 * 切换 LibVLC 版本只替换组件，不改变内核选择。
 */
class PlayerKernelState internal constructor(
    private val onPlayerTypeChanged: (PlayerType) -> Unit,
) {
    var showMpvDownloadConfirmDialog by mutableStateOf(false)
    var showMpvDownloaderDialog by mutableStateOf(false)
    var showVlcDownloadConfirmDialog by mutableStateOf(false)
    var showVlcDownloaderDialog by mutableStateOf(false)

    /** 本次下载由「LibVLC 版本」切换触发：完成后不改内核选择 */
    var vlcDownloadForVersionSwitch by mutableStateOf(false)
        private set

    /** 下载弹窗要取的 libvlc-all 版本 */
    var vlcDownloadVersion by mutableStateOf(MobilePrefs.vlcSelectedVersion)
        private set

    /** 内核单选的 onValueChange：返回是否立即持久化选择 */
    fun onPlayerTypeSelected(context: Context, newType: PlayerType): Boolean {
        return when (newType) {
            PlayerType.VLC -> {
                val version = MobilePrefs.vlcSelectedVersion
                if (VlcLibsInstaller.needsUpdate(context, version)) {
                    vlcDownloadForVersionSwitch = false
                    vlcDownloadVersion = version
                    showVlcDownloadConfirmDialog = true
                    false
                } else {
                    onPlayerTypeChanged(newType)
                    true
                }
            }

            PlayerType.MPV -> {
                if (MpvLibsInstaller.needsUpdate(context)) {
                    showMpvDownloadConfirmDialog = true
                    false
                } else {
                    onPlayerTypeChanged(newType)
                    if (!MobileMpvOptions.supportsZeroCopyHwdec) {
                        Toast.makeText(context, MobileMpvOptions.LOW_API_MPV_HINT, Toast.LENGTH_LONG).show()
                    }
                    true
                }
            }

            PlayerType.Media3 -> {
                onPlayerTypeChanged(newType)
                true
            }
        }
    }

    /** 「LibVLC 版本」单选的 onValueChange：选择总是保存，组件不符时再提示下载 */
    fun onVlcVersionSelected(context: Context, version: String): Boolean {
        if (VlcLibsInstaller.needsUpdate(context, version)) {
            vlcDownloadForVersionSwitch = true
            vlcDownloadVersion = version
            showVlcDownloadConfirmDialog = true
        }
        return true
    }

    internal fun onMpvDownloaded(context: Context) {
        showMpvDownloaderDialog = false
        MobilePrefs.playerType = PlayerType.MPV
        onPlayerTypeChanged(PlayerType.MPV)
        val hint = if (MobileMpvOptions.supportsZeroCopyHwdec) "MPV 组件下载完成" else MobileMpvOptions.LOW_API_MPV_HINT
        Toast.makeText(context, hint, Toast.LENGTH_LONG).show()
    }

    internal fun onVlcDownloaded(context: Context) {
        showVlcDownloaderDialog = false
        if (vlcDownloadForVersionSwitch) {
            Toast.makeText(context, "LibVLC $vlcDownloadVersion 组件下载完成", Toast.LENGTH_SHORT).show()
        } else {
            MobilePrefs.playerType = PlayerType.VLC
            onPlayerTypeChanged(PlayerType.VLC)
            Toast.makeText(context, "VLC 组件下载完成", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun rememberPlayerKernelState(onPlayerTypeChanged: (PlayerType) -> Unit = {}): PlayerKernelState {
    return remember { PlayerKernelState(onPlayerTypeChanged) }
}

/**
 * 「播放器内核」单选；[includeVlcOptions] 为 true 时追加「LibVLC 版本」「VLC 视频输出」两项。
 */
fun PreferenceGroupScope.playerKernelPreferences(
    context: Context,
    state: PlayerKernelState,
    includeVlcOptions: Boolean,
) {
    radioPreference(
        title = "播放器内核",
        prefReq = MobilePrefKeys.playerTypeRequest,
        values = PlayerType.entries.associate { it.ordinal to it.name },
        onValueChange = { ordinal -> state.onPlayerTypeSelected(context, PlayerType.entries[ordinal]) }
    )
    if (!includeVlcOptions) return
    radioPreference(
        title = "LibVLC 版本",
        prefReq = MobilePrefKeys.vlcSelectedVersionRequest,
        values = VlcNativeLibs.supportedVersions.associateWith { VlcNativeLibs.describeVersion(it) },
        onValueChange = { version -> state.onVlcVersionSelected(context, version) }
    )
    radioPreference(
        title = "VLC 视频输出（重启应用后生效）",
        prefReq = MobilePrefKeys.vlcVideoOutputRequest,
        values = MobileMpvOptions.vlcVideoOutputOptions
    )
}

/** 组件下载相关的所有弹窗，放在页面的 LazyColumn 之外 */
@Composable
fun PlayerKernelDialogs(state: PlayerKernelState) {
    val context = LocalContext.current

    if (state.showMpvDownloadConfirmDialog) {
        AlertDialog(
            onDismissRequest = { state.showMpvDownloadConfirmDialog = false },
            title = { Text("需要下载 MPV 组件") },
            text = {
                Text(
                    "MPV 播放器需要下载官方 mpv-android 组件才能使用。\n\n" +
                        "来源：mpv-android 官方 GitHub Release\n" +
                        "连接失败时会自动尝试 GitHub 镜像\n" +
                        "建议在 Wi-Fi 环境下下载"
                )
            },
            confirmButton = {
                Button(onClick = {
                    state.showMpvDownloadConfirmDialog = false
                    state.showMpvDownloaderDialog = true
                }) {
                    Text("下载")
                }
            },
            dismissButton = {
                TextButton(onClick = { state.showMpvDownloadConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (state.showMpvDownloaderDialog) {
        LibMPVDownloaderDialog(
            show = true,
            onDismissRequest = { state.showMpvDownloaderDialog = false },
            onDownloadComplete = { state.onMpvDownloaded(context) },
            onDownloadFailed = { errorMessage ->
                state.showMpvDownloaderDialog = false
                Toast.makeText(context, "下载失败: $errorMessage", Toast.LENGTH_LONG).show()
            }
        )
    }

    if (state.showVlcDownloadConfirmDialog) {
        val version = state.vlcDownloadVersion
        AlertDialog(
            onDismissRequest = { state.showVlcDownloadConfirmDialog = false },
            title = { Text("需要下载 VLC 组件") },
            text = {
                Text(
                    "VLC 播放器需要下载 libvlc-all ${VlcNativeLibs.describeVersion(version)} 组件才能使用。\n\n" +
                        "来源：Maven Central（连接失败时自动尝试镜像），安装前校验 SHA-256\n" +
                        "下载大小：约 ${if (version == VlcNativeLibs.vlc4Version) "105" else "90"} MB\n" +
                        "建议在 Wi-Fi 环境下下载"
                )
            },
            confirmButton = {
                Button(onClick = {
                    state.showVlcDownloadConfirmDialog = false
                    state.showVlcDownloaderDialog = true
                }) {
                    Text("下载")
                }
            },
            dismissButton = {
                TextButton(onClick = { state.showVlcDownloadConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (state.showVlcDownloaderDialog) {
        LibVLCDownloaderDialog(
            show = true,
            version = state.vlcDownloadVersion,
            onDismissRequest = { state.showVlcDownloaderDialog = false },
            onDownloadComplete = { state.onVlcDownloaded(context) },
            onDownloadFailed = { errorMessage ->
                state.showVlcDownloaderDialog = false
                Toast.makeText(context, "下载失败: $errorMessage", Toast.LENGTH_LONG).show()
            }
        )
    }
}
