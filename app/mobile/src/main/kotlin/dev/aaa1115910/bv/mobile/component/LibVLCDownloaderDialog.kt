package dev.aaa1115910.bv.mobile.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.aaa1115910.bv.network.VlcLibsApi
import dev.aaa1115910.bv.player.impl.vlc.VlcNativeLibs
import dev.aaa1115910.bv.util.AppRestarter
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.VlcLibsInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 手机端的 LibVLC 组件下载弹窗，流程与 TV 端一致。
 *
 * @param version 要下载的 `libvlc-all` 版本，必须在 [VlcNativeLibs.supportedVersions] 内
 *   （VLC 3 稳定版或 VLC 4 预览版，Java 层同时兼容两者）
 */
@Composable
fun LibVLCDownloaderDialog(
    modifier: Modifier = Modifier,
    show: Boolean = true,
    version: String = VlcNativeLibs.defaultVersion,
    onDismissRequest: () -> Unit = {},
    onDownloadComplete: () -> Unit = {},
    onDownloadFailed: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var processing by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("等待操作中...") }
    // 组件已装好，但本进程已加载了更旧的 C++ 运行库（来自 MPV 组件）或另一版本的 LibVLC，必须重启进程
    var restartRequired by remember { mutableStateOf(false) }

    val startInstall: () -> Unit = {
        processing = true

        scope.launch(Dispatchers.IO) {
            runCatching {
                val aarFile = File(context.cacheDir, "libvlc-all-$version.aar")
                val vlcLibsDir = VlcNativeLibs.libsDir(context)
                val targetAbi = VlcLibsInstaller.getTargetAbi()

                text = "正在下载 v$version ($targetAbi)..."
                VlcLibsApi.downloadAar(version, aarFile) { received, total ->
                    val percent = if (total > 0) (received * 100 / total) else 0
                    text = "正在下载 v$version ($targetAbi)... ($percent%)"
                }

                text = "正在校验并安装..."
                VlcLibsInstaller.installFromAar(aarFile, vlcLibsDir, targetAbi, version)
                aarFile.delete()
                Prefs.vlcLibsVersion = version

                val restartReason = VlcNativeLibs.restartRequiredReason(context)
                    ?: VlcNativeLibs.loadedVersion
                        ?.takeIf { it != version }
                        ?.let { loaded -> "当前进程已加载 LibVLC $loaded，请完全退出并重新打开应用后再使用 $version。" }
                withContext(Dispatchers.Main) {
                    if (restartReason != null) {
                        text = "LibVLC 组件已安装。\n\n$restartReason"
                        restartRequired = true
                    } else {
                        onDownloadComplete()
                    }
                }
            }.onFailure { e ->
                withContext(Dispatchers.Main) {
                    onDownloadFailed(e.message ?: "未知错误")
                }
                e.printStackTrace()
            }

            processing = false
        }
    }

    if (show) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = {
                if (restartRequired) onDownloadComplete() else if (!processing) onDismissRequest()
            },
            title = { Text(text = "LibVLC 下载器 (v$version)") },
            text = { Text(text = text) },
            confirmButton = {
                if (restartRequired) {
                    Button(
                        onClick = {
                            // 先让调用方把内核选择持久化，再重启进程
                            onDownloadComplete()
                            AppRestarter.restart(context)
                        }
                    ) {
                        Text(text = "重启应用")
                    }
                } else {
                    Button(
                        onClick = { startInstall() },
                        enabled = !processing
                    ) {
                        Text(text = "下载")
                    }
                }
            },
            dismissButton = {
                if (restartRequired) {
                    TextButton(onClick = { onDownloadComplete() }) {
                        Text(text = "稍后")
                    }
                } else {
                    TextButton(
                        onClick = { onDismissRequest() },
                        enabled = !processing
                    ) {
                        Text(text = "取消")
                    }
                }
            }
        )
    }
}
