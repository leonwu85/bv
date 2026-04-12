package dev.aaa1115910.bv.tv.screens.main

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.ThemeType
import dev.aaa1115910.bv.tv.activities.settings.SettingsActivity
import dev.aaa1115910.bv.tv.component.settings.SettingListItem
import dev.aaa1115910.bv.tv.component.settings.SettingSwitchListItem
import dev.aaa1115910.bv.tv.screens.settings.content.GridColumnsDialog
import dev.aaa1115910.bv.tv.screens.settings.content.ThemeTypeDialog
import dev.aaa1115910.bv.tv.screens.settings.content.UIDensityDialog
import dev.aaa1115910.bv.tv.screens.settings.content.HomeNavItemsEditDialog
import dev.aaa1115910.bv.tv.screens.settings.content.UgcNavItemsEditDialog
import dev.aaa1115910.bv.tv.screens.settings.content.DrawerNavItemsEditDialog
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.isDpadLeft
import dev.aaa1115910.bv.util.isKeyDown

@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    navFocusRequester: FocusRequester,
    onRequestDrawerFocus: () -> Unit = {},
) {
    val context = LocalContext.current
    var contentHasFocus by remember { mutableStateOf(false) }

    // UI setting states
    val density by Prefs.densityFlow.collectAsState(context.resources.displayMetrics.widthPixels / 960f)
    val themeType by Prefs.themeTypeFlow.collectAsState(Prefs.themeType)
    val enableMainUiAnimation by Prefs.enableMainUiAnimationFlow.collectAsState(Prefs.enableMainUiAnimation)
    var gridColumns by remember { mutableStateOf(Prefs.gridColumns) }

    // Dialog states
    var showDensityDialog by remember { mutableStateOf(false) }
    var showThemeTypeDialog by remember { mutableStateOf(false) }
    var showGridColumnsDialog by remember { mutableStateOf(false) }
    var showHomeNavItemsDialog by remember { mutableStateOf(false) }
    var showUgcNavItemsDialog by remember { mutableStateOf(false) }
    var showDrawerNavItemsDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = contentHasFocus) {
        onRequestDrawerFocus()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .onFocusChanged { contentHasFocus = it.hasFocus }
            .onKeyEvent {
                if (contentHasFocus && it.isKeyDown() && it.isDpadLeft()) {
                    onRequestDrawerFocus()
                    true
                } else {
                    false
                }
            },
        topBar = {
            Box(
                modifier = Modifier.padding(start = 48.dp, top = 24.dp, bottom = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.title_activity_settings),
                    fontSize = 48.sp
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 48.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(navFocusRequester),
                    title = stringResource(R.string.settings_ui_theme_type_title),
                    supportText = stringResource(R.string.settings_ui_theme_type_text),
                    valueText = themeType.getDisplayName(context),
                    onClick = { showThemeTypeDialog = true }
                )
            }
            item {
                SettingListItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.settings_ui_density_title),
                    supportText = stringResource(R.string.settings_ui_density_text),
                    valueText = density.toString(),
                    onClick = { showDensityDialog = true }
                )
            }
            item {
                SettingListItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.settings_ui_grid_columns_title),
                    supportText = stringResource(R.string.settings_ui_grid_columns_text),
                    valueText = if (gridColumns == 4) stringResource(R.string.settings_ui_grid_columns_4) else stringResource(R.string.settings_ui_grid_columns_5),
                    onClick = { showGridColumnsDialog = true }
                )
            }
            item {
                SettingListItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.settings_ui_drawer_nav_items_title),
                    supportText = stringResource(R.string.settings_ui_drawer_nav_items_text),
                    onClick = { showDrawerNavItemsDialog = true }
                )
            }
            item {
                SettingListItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.settings_ui_home_nav_items_title),
                    supportText = stringResource(R.string.settings_ui_home_nav_items_text),
                    onClick = { showHomeNavItemsDialog = true }
                )
            }
            item {
                SettingListItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.settings_ui_ugc_nav_items_title),
                    supportText = stringResource(R.string.settings_ui_ugc_nav_items_text),
                    onClick = { showUgcNavItemsDialog = true }
                )
            }
            item {
                SettingSwitchListItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.settings_ui_main_animation_title),
                    supportText = stringResource(R.string.settings_ui_main_animation_text),
                    checked = enableMainUiAnimation,
                    onCheckedChange = { Prefs.enableMainUiAnimation = it }
                )
            }
            item {
                Button(
                    modifier = Modifier.padding(top = 16.dp),
                    onClick = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }
                ) {
                    Text(text = stringResource(R.string.main_settings_open_button))
                }
            }
        }
    }

    // Dialogs
    UIDensityDialog(
        show = showDensityDialog,
        onHideDialog = { showDensityDialog = false },
        density = density,
        onDensityChange = { Prefs.density = it }
    )
    ThemeTypeDialog(
        show = showThemeTypeDialog,
        onHideDialog = { showThemeTypeDialog = false },
        themeType = themeType,
        onThemeTypeChange = { Prefs.themeType = it }
    )
    GridColumnsDialog(
        show = showGridColumnsDialog,
        onHideDialog = { showGridColumnsDialog = false },
        gridColumns = gridColumns,
        onGridColumnsChange = {
            gridColumns = it
            Prefs.gridColumns = it
        }
    )
    HomeNavItemsEditDialog(
        show = showHomeNavItemsDialog,
        onHideDialog = { showHomeNavItemsDialog = false },
        initialOrderString = Prefs.homeNavItemsOrder
    )
    UgcNavItemsEditDialog(
        show = showUgcNavItemsDialog,
        onHideDialog = { showUgcNavItemsDialog = false },
        initialOrderString = Prefs.ugcNavItemsOrder
    )
    DrawerNavItemsEditDialog(
        show = showDrawerNavItemsDialog,
        onHideDialog = { showDrawerNavItemsDialog = false },
        initialOrderString = Prefs.drawerItemsOrder
    )
}