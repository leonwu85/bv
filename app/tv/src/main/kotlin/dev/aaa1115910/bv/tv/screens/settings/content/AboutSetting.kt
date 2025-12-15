package dev.aaa1115910.bv.tv.screens.settings.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.tv.screens.settings.SettingsMenuNavItem
import dev.aaa1115910.bv.ui.theme.BVTheme

@Composable
fun AboutSetting(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
    ) {
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
            }
        }
        /*
        Text(
            modifier = Modifier.align(Alignment.BottomCenter),
            text = "https://github.com/fantasytyx/bv",
        )
        */
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun AboutSettingPreview() {
    BVTheme {
        AboutSetting()
    }
}