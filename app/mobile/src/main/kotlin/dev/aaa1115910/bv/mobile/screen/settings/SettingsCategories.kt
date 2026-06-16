package dev.aaa1115910.bv.mobile.screen.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.mobile.component.preferences.items.textPreference
import dev.aaa1115910.bv.mobile.component.preferences.preferenceGroups
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsCategories(
    modifier: Modifier = Modifier,
    selectedSettings: MobileSettings?,
    onSelectedSettings: (MobileSettings) -> Unit,
    showMpvSettings: Boolean,
    showNavBack: Boolean,
    isLogin: Boolean,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val logoutTitle = stringResource(R.string.settings_logout_title)
    val logoutText = stringResource(R.string.settings_logout_text)

    Scaffold(
        modifier = modifier,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }.takeIf { showNavBack }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 18.dp)
        ) {
            preferenceGroups(
                null to {
                    listOf(
                        MobileSettings.Appearance,
                        MobileSettings.AudioVideo,
                        MobileSettings.Play,
                        MobileSettings.SponsorBlock,
                        MobileSettings.Advance,
                        MobileSettings.Debug
                    ).let { items ->
                        if (showMpvSettings) {
                            items.toMutableList().apply { add(3, MobileSettings.Mpv) }
                        } else {
                            items
                        }
                    }.forEach { item ->
                        textPreference(
                            title = item.title,
                            summary = item.summary,
                            onClick = { onSelectedSettings(item) },
                            selected = selectedSettings == item
                        )
                    }
                },
                null to {
                    textPreference(
                        title = MobileSettings.About.title,
                        summary = MobileSettings.About.summary,
                        onClick = { onSelectedSettings(MobileSettings.About) },
                        selected = selectedSettings == MobileSettings.About
                    )
                },
                null to {
                    if (isLogin) {
                        textPreference(
                            title = logoutTitle,
                            summary = logoutText,
                            onClick = onLogout
                        )
                    }
                }
            )
        }
    }
}

@Preview
@Composable
private fun SettingsCategoriesPreview() {
    BVMobileTheme {
        Surface {
            SettingsCategories(
                selectedSettings = MobileSettings.Appearance,
                onSelectedSettings = {},
                showMpvSettings = true,
                showNavBack = false,
                isLogin = true,
                onLogout = {},
                onBack = {},
            )
        }
    }
}
