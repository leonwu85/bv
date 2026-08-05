package dev.aaa1115910.bv.tv.screens.settings.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.tv.component.settings.SettingNumberListItem
import dev.aaa1115910.bv.tv.component.settings.SettingSwitchListItem
import dev.aaa1115910.bv.tv.screens.settings.SettingsMenuNavItem
import dev.aaa1115910.bv.util.DanmakuSmartFilterPolicy
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.toast

@Composable
fun DanmakuFilterSetting(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val danmakuSmartFilterSupported = remember { DanmakuSmartFilterPolicy.isSupported() }
    var defaultDanmakuFilterLevel by remember { mutableStateOf(Prefs.defaultDanmakuFilterLevel) }
    var defaultDanmakuMergeEnabled by remember { mutableStateOf(Prefs.defaultDanmakuMergeEnabled) }
    var defaultLiveDanmakuFilterLevel by remember { mutableStateOf(Prefs.defaultLiveDanmakuFilterLevel) }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = SettingsMenuNavItem.DanmakuFilter.getDisplayName(context),
            style = MaterialTheme.typography.displaySmall
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingNumberListItem(
                    title = stringResource(R.string.settings_player_danmaku_filter_level_title),
                    supportText = stringResource(R.string.settings_player_danmaku_filter_level_text),
                    value = defaultDanmakuFilterLevel.toDouble(),
                    minValue = 1.0,
                    maxValue = 10.0,
                    isInteger = true,
                    step = 1.0,
                    onValueChange = {
                        defaultDanmakuFilterLevel = it.toInt()
                        Prefs.defaultDanmakuFilterLevel = it.toInt()
                    }
                )
            }
            item {
                SettingSwitchListItem(
                    title = stringResource(R.string.settings_player_danmaku_merge_title),
                    supportText = stringResource(R.string.settings_player_danmaku_merge_text),
                    checked = defaultDanmakuMergeEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !danmakuSmartFilterSupported) {
                            R.string.danmaku_smart_filter_unsupported_legacy_android.toast(context)
                        } else {
                            defaultDanmakuMergeEnabled = enabled
                            Prefs.defaultDanmakuMergeEnabled = enabled
                        }
                    }
                )
            }
            item {
                SettingNumberListItem(
                    title = stringResource(R.string.settings_live_danmaku_filter_level_title),
                    supportText = stringResource(R.string.settings_live_danmaku_filter_level_text),
                    value = defaultLiveDanmakuFilterLevel.toDouble(),
                    minValue = 0.0,
                    maxValue = 60.0,
                    isInteger = true,
                    step = 1.0,
                    onValueChange = {
                        defaultLiveDanmakuFilterLevel = it.toInt()
                        Prefs.defaultLiveDanmakuFilterLevel = it.toInt()
                    }
                )
            }
        }
    }
}
