package dev.aaa1115910.bv.mobile.dlna

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.aaa1115910.bv.viewmodel.DlnaMediaSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun DlnaCastDialog(
    sourceProvider: suspend () -> Result<DlnaMediaSource>,
    onCastStarted: (device: DlnaDevice, source: DlnaMediaSource) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val manager = remember(context) { DlnaManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val latestSourceProvider by rememberUpdatedState(sourceProvider)
    val latestOnCastStarted by rememberUpdatedState(onCastStarted)
    val latestOnDismiss by rememberUpdatedState(onDismiss)
    val localNetworkPermissionRequired = Build.VERSION.SDK_INT >= 37
    var localNetworkPermissionGranted by remember {
        mutableStateOf(
            !localNetworkPermissionRequired ||
                ContextCompat.checkSelfPermission(
                    context,
                    ACCESS_LOCAL_NETWORK_PERMISSION,
                ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var sourceRequest by remember { mutableIntStateOf(0) }
    var scanRequest by remember { mutableIntStateOf(0) }
    var source by remember { mutableStateOf<DlnaMediaSource?>(null) }
    var devices by remember { mutableStateOf<List<DlnaDevice>>(emptyList()) }
    var loadingSource by remember { mutableStateOf(true) }
    var scanning by remember { mutableStateOf(true) }
    var sourceError by remember { mutableStateOf<String?>(null) }
    var discoveryError by remember { mutableStateOf<String?>(null) }
    var castError by remember { mutableStateOf<String?>(null) }
    var castingDeviceId by remember { mutableStateOf<String?>(null) }
    val localNetworkPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        localNetworkPermissionGranted = granted
        if (!granted) {
            discoveryError = "需要本地网络权限才能搜索并连接投屏设备"
        }
    }

    LaunchedEffect(localNetworkPermissionRequired) {
        if (localNetworkPermissionRequired && !localNetworkPermissionGranted) {
            localNetworkPermissionLauncher.launch(ACCESS_LOCAL_NETWORK_PERMISSION)
        }
    }

    LaunchedEffect(sourceRequest) {
        loadingSource = true
        sourceError = null
        try {
            source = latestSourceProvider().getOrThrow()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            source = null
            sourceError = error.localizedMessage ?: "无法获取投屏地址"
        } finally {
            loadingSource = false
        }
    }

    LaunchedEffect(scanRequest, localNetworkPermissionGranted) {
        if (!localNetworkPermissionGranted) {
            scanning = false
            devices = emptyList()
            discoveryError = "需要本地网络权限才能搜索并连接投屏设备"
            return@LaunchedEffect
        }
        scanning = true
        discoveryError = null
        devices = emptyList()
        try {
            devices = manager.discoverDevices()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            discoveryError = error.localizedMessage ?: "搜索投屏设备失败"
        } finally {
            scanning = false
        }
    }

    AlertDialog(
        onDismissRequest = latestOnDismiss,
        title = { Text(text = "投屏到设备") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    loadingSource -> ProgressMessage(text = "正在准备投屏地址…")
                    source != null -> Text(
                        text = "视频：${source?.displayTitle.orEmpty()}",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (scanning) {
                    ProgressMessage(text = "正在搜索同一局域网内的设备…")
                }

                if (!scanning && devices.isEmpty() && discoveryError == null) {
                    Text(
                        text = "未发现可投屏设备。请确认手机与电视处于同一 Wi-Fi，并已开启电视的投屏功能。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                devices.forEachIndexed { index, device ->
                    val casting = castingDeviceId == device.id
                    val enabled =
                        source != null && !loadingSource && castingDeviceId == null
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = enabled) {
                                val selectedSource = source ?: return@clickable
                                castError = null
                                castingDeviceId = device.id
                                coroutineScope.launch {
                                    try {
                                        manager.cast(device, selectedSource)
                                        latestOnCastStarted(device, selectedSource)
                                        latestOnDismiss()
                                    } catch (error: CancellationException) {
                                        throw error
                                    } catch (error: Throwable) {
                                        castError =
                                            error.localizedMessage ?: "连接投屏设备失败"
                                    } finally {
                                        castingDeviceId = null
                                    }
                                }
                            },
                        supportingContent = { Text(text = "DLNA / UPnP") },
                        trailingContent = {
                            if (casting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                        },
                    ) {
                        Text(
                            text = device.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (index != devices.lastIndex) {
                        HorizontalDivider()
                    }
                }

                listOfNotNull(sourceError, discoveryError, castError)
                    .distinct()
                    .forEach { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !scanning && castingDeviceId == null,
                onClick = {
                    castError = null
                    if (localNetworkPermissionGranted) {
                        sourceRequest += 1
                        scanRequest += 1
                    } else {
                        localNetworkPermissionLauncher.launch(
                            ACCESS_LOCAL_NETWORK_PERMISSION
                        )
                    }
                },
            ) {
                Text(text = "刷新")
            }
        },
        dismissButton = {
            TextButton(onClick = latestOnDismiss) {
                Text(text = "取消")
            }
        },
    )
}

@Composable
private fun ProgressMessage(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private const val ACCESS_LOCAL_NETWORK_PERMISSION =
    "android.permission.ACCESS_LOCAL_NETWORK"
