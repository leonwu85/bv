package dev.aaa1115910.bv.mobile.screen.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.aaa1115910.bv.mobile.screen.settings.details.AboutContent
import dev.aaa1115910.bv.mobile.screen.settings.details.AdvanceContent
import dev.aaa1115910.bv.mobile.screen.settings.details.AudioVideoContent
import dev.aaa1115910.bv.mobile.screen.settings.details.AppearanceContent
import dev.aaa1115910.bv.mobile.screen.settings.details.DebugContent
import dev.aaa1115910.bv.mobile.screen.settings.details.MpvContent
import dev.aaa1115910.bv.mobile.screen.settings.details.PlayContent
import dev.aaa1115910.bv.mobile.screen.settings.details.SponsorBlockContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDetails(
    modifier: Modifier = Modifier,
    selectedSettings: MobileSettings?,
    showNavBack: Boolean,
    onPlayerTypeChanged: (dev.aaa1115910.bv.entity.PlayerType) -> Unit = {},
    onBack: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(text = selectedSettings?.title ?: "NaN")
                },
                navigationIcon = {
                    if (showNavBack) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding))
        val contentModifier = Modifier.padding(top = innerPadding.calculateTopPadding())
        when (selectedSettings) {
            null, MobileSettings.Appearance -> AppearanceContent(modifier = contentModifier)
            MobileSettings.AudioVideo -> AudioVideoContent(
                modifier = contentModifier,
                onPlayerTypeChanged = onPlayerTypeChanged
            )
            MobileSettings.Play -> PlayContent(modifier = contentModifier)
            MobileSettings.Mpv -> MpvContent(modifier = contentModifier)
            MobileSettings.SponsorBlock -> SponsorBlockContent(modifier = contentModifier)
            MobileSettings.About -> AboutContent(modifier = contentModifier)
            MobileSettings.Debug -> DebugContent(modifier = contentModifier)
            MobileSettings.Advance -> AdvanceContent(modifier = contentModifier)
        }
    }
}
