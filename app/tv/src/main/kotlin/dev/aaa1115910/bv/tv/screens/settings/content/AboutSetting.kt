package dev.aaa1115910.bv.tv.screens.settings.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LinearProgressIndicator
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.network.GithubApi
import dev.aaa1115910.bv.network.entity.Release
import dev.aaa1115910.bv.tv.component.settings.ChangelogDialog
import dev.aaa1115910.bv.tv.component.settings.UpdateDialog
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.tv.screens.settings.SettingsMenuNavItem
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.requestFocus
import kotlinx.coroutines.launch
import kotlin.runCatching

/**
 * 更新检查状态
 */
enum class UpdateCheckState {
    Idle,           // 未检查
    Checking,       // 检查中
    HasUpdate,      // 有更新
    NoUpdate,       // 无更新
    CheckError,     // 检查失败
}

@Composable
fun AboutSetting(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var updateState by remember { mutableStateOf(UpdateCheckState.Idle) }
    var latestRelease by remember { mutableStateOf<Release?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    val buttonFocusRequester = remember { FocusRequester() }
    // 标记是否是用户手动触发的检查
    var isManualCheck by remember { mutableStateOf(false) }
    var showChangelogDialog by remember { mutableStateOf(false) }
    var showTestResultDialog by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val checkUpdate: (Boolean) -> Unit = { manual ->
        isManualCheck = manual
        updateState = UpdateCheckState.Checking
        errorMessage = ""
        scope.launch {
            runCatching {
                latestRelease = GithubApi.getLatestBuild()
                val asset = latestRelease!!.assets.firstOrNull { it.name.startsWith("BV") }
                if (asset == null) {
                    updateState = UpdateCheckState.CheckError
                    errorMessage = "未找到 BV APK 文件"
                    return@launch
                }
                val name = asset.name
                // 从文件名提取版本号: BV-x.y.z_buildType
                val parts = name.removePrefix("BV-").split("_")
                if (parts.size >= 2) {
                    val revision = parts[1].toIntOrNull()
                    if (revision != null && revision > BuildConfig.VERSION_CODE && !name.contains(BuildConfig.VERSION_NAME)) {
                        updateState = UpdateCheckState.HasUpdate
                    } else {
                        updateState = UpdateCheckState.NoUpdate
                    }
                } else {
                    // 无法解析版本号，默认认为有更新
                    updateState = UpdateCheckState.HasUpdate
                }
            }.onFailure {
                updateState = UpdateCheckState.CheckError
                errorMessage = it.message ?: "检查更新失败"
            }
        }
    }

    // 进入页面后延迟自动检查更新
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(500)
        checkUpdate(false)
    }

    // 状态变化时保持焦点在按钮区域（仅在用户手动触发检查且状态变为 HasUpdate 时）
    LaunchedEffect(updateState) {
        if (isManualCheck && updateState == UpdateCheckState.HasUpdate) {
            // HasUpdate 状态会切换到 Button，需要重新请求焦点
            kotlinx.coroutines.delay(100)
            buttonFocusRequester.requestFocus(scope)
        }
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = SettingsMenuNavItem.About.getDisplayName(context),
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.about_statement),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Red
                )
                Text(
                    text = stringResource(
                        R.string.settings_version_current_version,
                        "${BuildConfig.VERSION_NAME}.${BuildConfig.BUILD_TYPE}"
                    )
                )
                // 最新版本显示
                if (latestRelease != null && updateState != UpdateCheckState.Checking) {
                    Text(
                        text = stringResource(
                            R.string.settings_version_latest_version,
                            latestRelease!!.name
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 调试模式下的测试按钮
            if (BuildConfig.DEBUG) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            testResult = "测试中..."
                            showTestResultDialog = true
                            testResult = GithubApi.testProxyConnection()
                        }
                    }
                ) {
                    Text("测试代理连接")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 操作按钮区域
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 进度条（仅在检查时显示）
                if (updateState == UpdateCheckState.Checking) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator()
                        Text("检查更新中...")
                    }
                }

                // 状态消息
                when (updateState) {
                    UpdateCheckState.HasUpdate -> {
                        Text(
                            text = "有新版本可用",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF4CAF50)
                        )
                    }

                    UpdateCheckState.NoUpdate -> {
                        Text(
                            text = "已是最新版本",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    UpdateCheckState.CheckError -> {
                        Text(
                            text = "检查更新失败",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFF44336)
                        )
                        if (errorMessage.isNotEmpty()) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }

                    else -> {}
                }

                // 主按钮区域 - 始终存在，避免焦点丢失
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 主操作按钮
                    if (updateState == UpdateCheckState.HasUpdate) {
                        Button(
                            modifier = Modifier.focusRequester(buttonFocusRequester),
                            onClick = { showUpdateDialog = true }
                        ) {
                            Text("立即更新")
                        }
                    } else {
                        OutlinedButton(
                            modifier = Modifier.focusRequester(buttonFocusRequester),
                            onClick = {
                                if (updateState != UpdateCheckState.Checking) {
                                    checkUpdate(true)
                                }
                            }
                        ) {
                            Text(
                                when (updateState) {
                                    UpdateCheckState.Idle -> "检查更新"
                                    UpdateCheckState.Checking -> "检查中..."
                                    UpdateCheckState.NoUpdate -> "重新检查"
                                    UpdateCheckState.CheckError -> "重试"
                                    UpdateCheckState.HasUpdate -> "" // 不会到这里
                                }
                            )
                        }
                    }

                    // 查看更新内容按钮（仅在有版本信息时显示）
                    if (latestRelease != null && updateState != UpdateCheckState.Idle && updateState != UpdateCheckState.Checking) {
                        OutlinedButton(onClick = { showChangelogDialog = true }) {
                            Text("查看更新内容")
                        }
                    }
                }
            }
        }
    }

    // 更新对话框
    UpdateDialog(
        show = showUpdateDialog,
        onHideDialog = { showUpdateDialog = false }
    )

    // 更新内容对话框
    ChangelogDialog(
        show = showChangelogDialog,
        release = latestRelease,
        onHideDialog = { showChangelogDialog = false }
    )

    // 测试结果对话框（调试模式）
    if (BuildConfig.DEBUG && showTestResultDialog) {
        TvAlertDialog(
            title = { Text("代理连接测试结果") },
            text = {
                Text(
                    text = testResult,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            onDismissRequest = { showTestResultDialog = false },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = { showTestResultDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun AboutSettingPreview() {
    BVTheme {
        AboutSetting()
    }
}
