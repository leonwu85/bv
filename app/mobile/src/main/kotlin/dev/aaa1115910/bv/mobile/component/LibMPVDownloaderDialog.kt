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
import dev.aaa1115910.bv.network.MpvLibsApi
import dev.aaa1115910.bv.player.impl.mpv.MpvLibsInstaller
import dev.aaa1115910.bv.util.AppRestarter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun LibMPVDownloaderDialog(
    modifier: Modifier = Modifier,
    show: Boolean = true,
    onDismissRequest: () -> Unit = {},
    onDownloadComplete: () -> Unit = {},
    onDownloadFailed: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var processing by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("等待操作中...") }
    // 组件已装好，但本进程已加载了更旧的 C++ 运行库（来自其他组件），必须重启进程才能用 MPV
    var restartRequired by remember { mutableStateOf(false) }

    val startInstall: () -> Unit = {
        processing = true

        scope.launch(Dispatchers.IO) {
            runCatching {
                val targetAbi = MpvLibsInstaller.getTargetAbi()
                val apkFile = File(context.cacheDir, "mpv-android-$targetAbi.apk")
                val mpvLibsDir = MpvLibsInstaller.getLibsDir(context)

                val tag = MpvLibsApi.pinnedReleaseTag
                text = "正在下载官方 MPV 组件 $tag ($targetAbi)..."
                val version = MpvLibsApi.downloadPinnedApk(targetAbi, apkFile) { received, total ->
                    val percent = if (total != null && total > 0L) received * 100 / total else 0
                    text = "正在下载官方 MPV 组件 $tag ($targetAbi)... ($percent%)"
                }

                text = "正在校验签名并安装..."
                MpvLibsInstaller.installFromApk(context, apkFile, mpvLibsDir, targetAbi, version)

                apkFile.delete()

                val restartReason = MpvLibsInstaller.restartRequiredReason(context)
                withContext(Dispatchers.Main) {
                    if (restartReason != null) {
                        text = "MPV 组件已安装。\n\n$restartReason"
                        restartRequired = true
                    } else {
                        onDownloadComplete()
                    }
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    onDownloadFailed(error.message ?: "未知错误")
                }
                error.printStackTrace()
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
            title = { Text(text = "MPV 下载器") },
            text = { Text(text = text) },
            confirmButton = {
                if (restartRequired) {
                    Button(
                        onClick = {
                            // 先让调用方把播放器内核切到 MPV 并持久化，再重启进程
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
