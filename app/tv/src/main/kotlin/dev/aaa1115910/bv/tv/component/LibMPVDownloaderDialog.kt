package dev.aaa1115910.bv.tv.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.tv.material3.Button
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.aaa1115910.bv.network.MpvLibsApi
import dev.aaa1115910.bv.player.impl.mpv.MpvLibsInstaller
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

    val startInstall: () -> Unit = {
        processing = true

        scope.launch(Dispatchers.IO) {
            runCatching {
                val targetAbi = MpvLibsInstaller.getTargetAbi()
                val apkFile = File(context.cacheDir, "mpv-android-$targetAbi.apk")
                val mpvLibsDir = MpvLibsInstaller.getLibsDir(context)

                text = "正在下载官方 MPV 组件 ($targetAbi)..."
                val version = MpvLibsApi.downloadLatestApk(targetAbi, apkFile) { received, total ->
                    val percent = if (total != null && total > 0L) received * 100 / total else 0
                    text = "正在下载官方 MPV 组件 ($targetAbi)... ($percent%)"
                }

                text = "正在安装..."
                MpvLibsInstaller.installFromApk(apkFile, mpvLibsDir, targetAbi, version)

                apkFile.delete()

                withContext(Dispatchers.Main) {
                    onDownloadComplete()
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
        TvAlertDialog(
            modifier = modifier,
            title = { Text(text = "MPV 下载器") },
            text = { Text(text = text) },
            onDismissRequest = { if (!processing) onDismissRequest() },
            confirmButton = {
                Button(
                    onClick = { startInstall() },
                    enabled = !processing
                ) {
                    Text(text = "下载")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { onDismissRequest() },
                    enabled = !processing
                ) {
                    Text(text = "取消")
                }
            }
        )
    }
}
