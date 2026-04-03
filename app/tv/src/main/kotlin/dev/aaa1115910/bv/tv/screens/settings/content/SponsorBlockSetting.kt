package dev.aaa1115910.bv.tv.screens.settings.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.http.ServerStatus
import dev.aaa1115910.biliapi.http.SponsorBlockHttpApi
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.player.entity.SponsorBlockSkipMode
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.tv.component.settings.SettingListItem
import dev.aaa1115910.bv.tv.component.settings.SettingListItemWithDialog
import dev.aaa1115910.bv.tv.component.settings.SettingSwitchListItem
import dev.aaa1115910.bv.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SponsorBlockSetting(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var enableSponsorBlock by remember { mutableStateOf(Prefs.enableSponsorBlock) }
    var skipMode by remember { mutableStateOf(Prefs.sponsorBlockSkipMode) }
    var apiServer by remember { mutableStateOf(Prefs.sponsorBlockApiServer) }
    var serverStatus by remember { mutableStateOf<ServerStatus>(ServerStatus.Checking) }
    var showApiServerEditDialog by remember { mutableStateOf(false) }

    suspend fun refreshServerStatus() {
        serverStatus = ServerStatus.Checking
        serverStatus = withContext(Dispatchers.IO) {
            SponsorBlockHttpApi.updateBaseUrl(apiServer)
            SponsorBlockHttpApi.checkServerStatus()
        }
    }

    LaunchedEffect(Unit) {
        refreshServerStatus()
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "广告助手",
            style = MaterialTheme.typography.displaySmall
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "由小电视空降助手（BilibiliSponsorBlock）提供支持",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "官方网站: https://www.bsbsb.top/",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                SettingSwitchListItem(
                    title = "启用广告助手（实验性功能）",
                    supportText = "自动识别视频中的广告片段（实验性）",
                    checked = enableSponsorBlock,
                    onCheckedChange = {
                        enableSponsorBlock = it
                        Prefs.enableSponsorBlock = it
                    }
                )
            }
            item {
                SettingListItemWithDialog(
                    title = stringResource(R.string.settings_sponsor_block_skip_mode_title),
                    supportText = stringResource(R.string.settings_sponsor_block_skip_mode_text),
                    options = SponsorBlockSkipMode.entries,
                    getDisplayName = { item, ctx ->
                        when (item) {
                            SponsorBlockSkipMode.Manual -> ctx.getString(R.string.settings_sponsor_block_skip_mode_manual)
                            SponsorBlockSkipMode.Auto -> ctx.getString(R.string.settings_sponsor_block_skip_mode_auto)
                        }
                    },
                    value = skipMode,
                    onValueChange = {
                        skipMode = it
                        Prefs.sponsorBlockSkipMode = it
                    }
                )
            }
            item {
                SettingListItem(
                    title = stringResource(R.string.settings_sponsor_block_api_server_title),
                    supportText = stringResource(R.string.settings_sponsor_block_api_server_text, apiServer),
                    onClick = { showApiServerEditDialog = true }
                )
            }
            item {
                ServerStatusListItem(
                    status = serverStatus,
                    onRefresh = { scope.launch { refreshServerStatus() } }
                )
            }
        }
    }

    ApiServerEditDialog(
        show = showApiServerEditDialog,
        onHideDialog = { showApiServerEditDialog = false },
        apiServer = apiServer,
        onApiServerChange = { newServer ->
            apiServer = newServer
            Prefs.sponsorBlockApiServer = newServer
            SponsorBlockHttpApi.updateBaseUrl(newServer)
            scope.launch { refreshServerStatus() }
        }
    )
}

@Composable
private fun ServerStatusListItem(
    modifier: Modifier = Modifier,
    status: ServerStatus,
    onRefresh: () -> Unit
) {
    val (statusText, statusColor) = when (status) {
        is ServerStatus.Connected -> stringResource(R.string.settings_sponsor_block_server_status_connected) to
                MaterialTheme.colorScheme.primary
        is ServerStatus.Error -> stringResource(R.string.settings_sponsor_block_server_status_error, status.message) to
                MaterialTheme.colorScheme.error
        ServerStatus.Checking -> stringResource(R.string.settings_sponsor_block_server_status_checking) to
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }

    ListItem(
        modifier = modifier,
        selected = false,
        onClick = onRefresh,
        headlineContent = {
            Text(text = stringResource(R.string.settings_sponsor_block_server_status_title))
        },
        supportingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (status is ServerStatus.Checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = statusColor
                    )
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
                )
            }
        }
    )
}

@Composable
private fun ApiServerEditDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    apiServer: String,
    onApiServerChange: (String) -> Unit
) {
    var serverString by remember(show) { mutableStateOf(apiServer) }

    if (show) {
        TvAlertDialog(
            modifier = modifier,
            title = { Text(text = stringResource(R.string.settings_sponsor_block_api_server_title)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = serverString,
                        onValueChange = { serverString = it },
                        singleLine = true,
                        maxLines = 1,
                        shape = MaterialTheme.shapes.medium,
                        placeholder = { Text(text = "bsbsb.top") }
                    )
                    Text(
                        text = stringResource(R.string.settings_sponsor_block_api_server_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
            onDismissRequest = onHideDialog,
            confirmButton = {
                Button(onClick = {
                    onApiServerChange(
                        serverString
                            .trim()
                            .replace("\n", "")
                            .trimEnd('/')
                            .ifBlank { "bsbsb.top" }
                    )
                    onHideDialog()
                }) {
                    Text(text = stringResource(id = R.string.common_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onHideDialog) {
                    Text(text = stringResource(id = R.string.common_cancel))
                }
            }
        )
    }
}
