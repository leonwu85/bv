package dev.aaa1115910.bv.mobile.screen.settings.details

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.schnettler.datastore.manager.DataStoreManager
import dev.aaa1115910.biliapi.http.ServerStatus
import dev.aaa1115910.biliapi.http.SponsorBlockHttpApi
import dev.aaa1115910.bv.dataStore
import dev.aaa1115910.bv.mobile.component.preferences.items.editTextPreference
import dev.aaa1115910.bv.mobile.component.preferences.items.radioPreference
import dev.aaa1115910.bv.mobile.component.preferences.items.switchPreference
import dev.aaa1115910.bv.mobile.component.preferences.items.textPreference
import dev.aaa1115910.bv.mobile.component.preferences.preferenceGroups
import dev.aaa1115910.bv.mobile.settings.MobilePrefKeys
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme
import dev.aaa1115910.bv.player.entity.SponsorBlockSkipMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.aaa1115910.bv.mobile.R as MobileR

@Composable
fun SponsorBlockContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val dataStoreManager = remember(context) { DataStoreManager(context.dataStore) }
    val sponsorBlockApiServer by dataStoreManager.getPreferenceState(MobilePrefKeys.sponsorBlockApiServerRequest)
    var sponsorBlockServerStatus by remember { mutableStateOf<ServerStatus>(ServerStatus.Checking) }

    suspend fun refreshSponsorBlockServerStatus(apiServer: String = sponsorBlockApiServer) {
        sponsorBlockServerStatus = ServerStatus.Checking
        sponsorBlockServerStatus = withContext(Dispatchers.IO) {
            SponsorBlockHttpApi.updateBaseUrl(apiServer)
            SponsorBlockHttpApi.checkServerStatus()
        }
    }

    LaunchedEffect(sponsorBlockApiServer) {
        refreshSponsorBlockServerStatus(sponsorBlockApiServer)
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 18.dp)
    ) {
        preferenceGroups(
            "广告助手" to {
                switchPreference(
                    title = "启用广告助手",
                    summary = "由小电视空降助手提供支持，开启后默认自动跳过广告片段",
                    prefReq = MobilePrefKeys.enableSponsorBlockRequest,
                    onCheckedChange = { true }
                )
                radioPreference(
                    title = "跳过方式",
                    prefReq = MobilePrefKeys.sponsorBlockSkipModeRequest,
                    values = SponsorBlockSkipMode.entries.associate {
                        it.value to when (it) {
                            SponsorBlockSkipMode.Manual -> "手动跳过"
                            SponsorBlockSkipMode.Auto -> "自动跳过"
                        }
                    }
                )
                editTextPreference(
                    title = "API 服务器地址",
                    prefReq = MobilePrefKeys.sponsorBlockApiServerRequest,
                    summary = { "当前服务器: $it" },
                    transformValue = { it.trim().ifBlank { "bsbsb.top" } }
                )
                val (statusText, canRefresh) = when (val status = sponsorBlockServerStatus) {
                    is ServerStatus.Connected -> "连接正常" to true
                    is ServerStatus.Error -> "连接失败: ${status.message}" to true
                    ServerStatus.Checking -> "检测中..." to false
                }
                textPreference(
                    title = "服务器状态",
                    summary = statusText,
                    enabled = canRefresh,
                    onClick = { scope.launch { refreshSponsorBlockServerStatus(sponsorBlockApiServer) } }
                )
            }
        )
        item(key = "sponsor_block_credit") {
            SponsorBlockCredit(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 28.dp)
                    .clickable { uriHandler.openUri("https://www.bsbsb.top") }
            )
        }
    }
}

@Composable
private fun SponsorBlockCredit(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier.size(72.dp),
            painter = painterResource(id = MobileR.drawable.ic_sponsor_block_logo),
            contentDescription = "小电视空降助手"
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "由小电视空降助手提供支持",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "https://www.bsbsb.top",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SponsorBlockContentPreview() {
    BVMobileTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            SponsorBlockContent(
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
