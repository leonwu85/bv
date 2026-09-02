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
import dev.aaa1115910.bv.player.impl.vlc.VlcNativeLibs
import dev.aaa1115910.bv.util.AppRestarter
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
    var restartRequired by remember { mutableStateOf(false) }
    val version = BuildConfig.libVLCVersion

    val startInstall: () -> Unit = {
        processing = true

        scope.launch(Dispatchers.IO) {
            runCatching {
                val aarFile = File(context.cacheDir, "libvlc-all-$version.aar")
                val vlcLibsDir = VlcNativeLibs.libsDir(context)
                val targetAbi = VlcLibsInstaller.getTargetAbi()

                // 1. 下载 AAR（自动尝试多个镜像源）
                text = "正在下载 v$version ($targetAbi)..."
                VlcLibsApi.downloadAar(version, aarFile) { received, total ->
                    val percent = if (total > 0) (received * 100 / total) else 0
                    text = "正在下载 v$version ($targetAbi)... ($percent%)"
                }

                // 2. 校验 SHA-256，解压并原子替换安装 so 文件
                text = "正在校验并安装..."
                VlcLibsInstaller.installFromAar(aarFile, vlcLibsDir, targetAbi, version)

                // 3. 删除临时 AAR 文件
                aarFile.delete()

                // 4. 保存已安装的版本号
                Prefs.vlcLibsVersion = version

                // 5. 通知完成；若本进程已加载了更旧的 C++ 运行库（来自 MPV 组件），需重启进程才能用 VLC
                val restartReason = VlcNativeLibs.restartRequiredReason(context)
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
        TvAlertDialog(
            modifier = modifier,
            title = { Text(text = "LibVLC 下载器 (v$version)") },
            text = { Text(text = text) },
            onDismissRequest = {
                if (restartRequired) onDownloadComplete() else if (!processing) onDismissRequest()
            },
            confirmButton = {
                if (restartRequired) {
                    Button(
                        onClick = {
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
                    OutlinedButton(onClick = { onDownloadComplete() }) {
                        Text(text = "稍后")
                    }
                } else {
                    OutlinedButton(
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
