package dev.aaa1115910.bv.tv.screens.settings.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import dev.aaa1115910.bv.entity.LiveQualityPreference
import dev.aaa1115910.bv.player.entity.LiveCodec
import dev.aaa1115910.bv.tv.component.settings.SettingListItemWithDialog
import dev.aaa1115910.bv.tv.component.settings.SettingSwitchListItem
import dev.aaa1115910.bv.tv.screens.settings.SettingsMenuNavItem
import dev.aaa1115910.bv.util.Prefs

@Composable
fun LiveStreamingSetting(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var defaultLiveQuality by remember { mutableStateOf(LiveQualityPreference.fromQn(Prefs.defaultLiveQn)) }
    var defaultLiveCodec by remember { mutableStateOf(Prefs.defaultLiveCodec) }
    var showLiveInSidebar by remember { mutableStateOf(Prefs.showLiveInSidebar) }
    var showLiveDanmakuEmoji by remember { mutableStateOf(Prefs.showLiveDanmakuEmoji) }
    var showLivePopularity by remember { mutableStateOf(Prefs.showLivePopularity) }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = SettingsMenuNavItem.Live.getDisplayName(context),
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SettingSwitchListItem(
                        title = stringResource(R.string.settings_live_enable),
                        supportText = stringResource(R.string.settings_live_enable_desc),
                        checked = showLiveInSidebar,
                        onCheckedChange = {
                            showLiveInSidebar = it
                            Prefs.showLiveInSidebar = it
                        }
                    )
                }
                item {
                    SettingListItemWithDialog(
                        title = stringResource(R.string.settings_live_default_quality),
                        supportText = stringResource(R.string.settings_live_default_quality_desc),
                        options = LiveQualityPreference.entries,
                        getDisplayName = { item, ctx -> item.getDisplayName(ctx) },
                        value = defaultLiveQuality,
                        onValueChange = {
                            defaultLiveQuality = it
                            Prefs.defaultLiveQn = it.qn
                        }
                    )
                }
                item {
                    SettingListItemWithDialog(
                        title = stringResource(R.string.settings_live_default_codec),
                        supportText = stringResource(R.string.settings_live_default_codec_desc),
                        options = LiveCodec.entries,
                        getDisplayName = { item, ctx -> item.getDisplayName(ctx) },
                        value = defaultLiveCodec,
                        onValueChange = {
                            defaultLiveCodec = it
                            Prefs.defaultLiveCodec = it
                        }
                    )
                }
                item {
                    SettingSwitchListItem(
                        title = stringResource(R.string.settings_live_popularity),
                        supportText = stringResource(R.string.settings_live_popularity_desc),
                        checked = showLivePopularity,
                        onCheckedChange = {
                            showLivePopularity = it
                            Prefs.showLivePopularity = it
                        }
                    )
                }
                item {
                    SettingSwitchListItem(
                        title = stringResource(R.string.settings_live_danmaku_emoji),
                        supportText = stringResource(R.string.settings_live_danmaku_emoji_desc),
                        checked = showLiveDanmakuEmoji,
                        onCheckedChange = {
                            showLiveDanmakuEmoji = it
                            Prefs.showLiveDanmakuEmoji = it
                        }
                    )
                }
            }
        }
    }
}
