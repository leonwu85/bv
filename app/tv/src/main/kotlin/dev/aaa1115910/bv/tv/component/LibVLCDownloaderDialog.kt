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
import dev.aaa1115910.bv.network.VlcLibsApi
import dev.aaa1115910.bv.player.BuildConfig
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.VlcLibsInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun LibVLCDownloaderDialog(
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
    val version = BuildConfig.libVLCVersion

    val startInstall: () -> Unit = {
        processing = true

        scope.launch(Dispatchers.IO) {
            runCatching {
                val aarFile = File(context.cacheDir, "libvlc-all-$version.aar")
                val vlcLibsDir = File(context.filesDir, "vlc_libs")
                val targetAbi = VlcLibsInstaller.getTargetAbi()

                // 1. 下载 AAR（自动尝试多个镜像源）
                text = "正在下载 v$version ($targetAbi)..."
                VlcLibsApi.downloadAar(version, aarFile) { received, total ->
                    val percent = if (total > 0) (received * 100 / total) else 0
                    text = "正在下载 v$version ($targetAbi)... ($percent%)"
                }

                // 2. 解压并安装 so 文件
                text = "正在安装..."
                VlcLibsInstaller.installFromAar(aarFile, vlcLibsDir, targetAbi)

                // 3. 删除临时 AAR 文件
                aarFile.delete()

                // 4. 保存已安装的版本号
                Prefs.vlcLibsVersion = version

                // 4. 通知完成
                withContext(Dispatchers.Main) {
                    onDownloadComplete()
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
        TvAlertDialog(
            modifier = modifier,
            title = { Text(text = "LibVLC 下载器 (v$version)") },
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
