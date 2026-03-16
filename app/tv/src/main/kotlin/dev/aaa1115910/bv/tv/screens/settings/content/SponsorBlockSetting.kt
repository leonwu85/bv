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
import dev.aaa1115910.bv.player.entity.SponsorBlockSkipMode
import dev.aaa1115910.bv.tv.component.settings.SettingListItemWithDialog
import dev.aaa1115910.bv.tv.component.settings.SettingSwitchListItem
import dev.aaa1115910.bv.util.Prefs

@Composable
fun SponsorBlockSetting(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var enableSponsorBlock by remember { mutableStateOf(Prefs.enableSponsorBlock) }
    var skipMode by remember { mutableStateOf(Prefs.sponsorBlockSkipMode) }

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
        }
    }
}
