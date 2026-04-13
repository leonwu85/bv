package dev.aaa1115910.bv.tv.screens.main

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
import androidx.tv.material3.Text
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.player.entity.PlayerLoadNextAction
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.tv.activities.settings.SettingsActivity
import dev.aaa1115910.bv.tv.component.settings.SettingListItem
import dev.aaa1115910.bv.tv.component.settings.SettingListItemWithDialog
import dev.aaa1115910.bv.tv.component.settings.SettingSwitchListItem
import dev.aaa1115910.bv.tv.screens.settings.SettingsMenuNavItem
import dev.aaa1115910.bv.tv.screens.settings.content.DrawerNavItemsEditDialog
import dev.aaa1115910.bv.tv.screens.settings.content.GridColumnsDialog
import dev.aaa1115910.bv.tv.screens.settings.content.ThemeTypeDialog
import dev.aaa1115910.bv.tv.screens.settings.content.UIDensityDialog
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
    val currentVersion = remember { "${BuildConfig.VERSION_NAME}.${BuildConfig.BUILD_TYPE}" }

    // UI setting states
    val density by Prefs.densityFlow.collectAsState(context.resources.displayMetrics.widthPixels / 960f)
    val themeType by Prefs.themeTypeFlow.collectAsState(Prefs.themeType)
    var gridColumns by remember { mutableStateOf(Prefs.gridColumns) }
    var defaultResolution by remember { mutableStateOf(Prefs.defaultQuality) }
    var showBottomProgressBar by remember { mutableStateOf(Prefs.playerShowBottomProgressBar) }
    var loadNextAction by remember { mutableStateOf(Prefs.playerLoadNextAction) }
    var enableSponsorBlock by remember { mutableStateOf(Prefs.enableSponsorBlock) }

    // Dialog states
    var showDensityDialog by remember { mutableStateOf(false) }
    var showThemeTypeDialog by remember { mutableStateOf(false) }
    var showGridColumnsDialog by remember { mutableStateOf(false) }
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
                modifier = Modifier.padding(start = 48.dp, top = 12.dp, bottom = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.title_activity_settings),
                    fontSize = 24.sp
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
                SettingListItemWithDialog(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.settings_item_resolution),
                    supportText = stringResource(R.string.settings_main_default_resolution_text),
                    options = Resolution.entries.reversed(),
                    getDisplayName = { item, ctx -> item.getDisplayName(ctx) },
                    value = defaultResolution,
                    onValueChange = {
                        defaultResolution = it
                        Prefs.defaultQuality = it
                    }
                )
            }
            item {
                SettingSwitchListItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.settings_player_show_bottom_progress_bar_title),
                    supportText = stringResource(R.string.settings_player_show_bottom_progress_bar_text),
                    checked = showBottomProgressBar,
                    onCheckedChange = {
                        showBottomProgressBar = it
                        Prefs.playerShowBottomProgressBar = it
                    }
                )
            }
            item {
                SettingListItemWithDialog(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.settings_player_load_next_action_title),
                    supportText = stringResource(R.string.settings_player_load_next_action_text),
                    options = PlayerLoadNextAction.entries,
                    getDisplayName = { item, ctx -> item.displayName(ctx) },
                    value = loadNextAction,
                    onValueChange = {
                        loadNextAction = it
                        Prefs.playerLoadNextAction = it
                    }
                )
            }
            item {
                SettingSwitchListItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.settings_main_enable_sponsor_block_title),
                    supportText = stringResource(R.string.settings_main_enable_sponsor_block_text),
                    checked = enableSponsorBlock,
                    onCheckedChange = {
                        enableSponsorBlock = it
                        Prefs.enableSponsorBlock = it
                    }
                )
            }
            item {
                SettingListItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.settings_item_about),
                    supportText = stringResource(R.string.settings_main_about_text),
                    valueText = currentVersion,
                    onClick = {
                        context.startActivity(
                            SettingsActivity.createIntent(
                                context = context,
                                initialMenu = SettingsMenuNavItem.About
                            )
                        )
                    }
                )
            }
            item {
                Button(
                    modifier = Modifier.padding(top = 16.dp),
                    onClick = {
                        context.startActivity(SettingsActivity.createIntent(context))
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
    DrawerNavItemsEditDialog(
        show = showDrawerNavItemsDialog,
        onHideDialog = { showDrawerNavItemsDialog = false },
        initialOrderString = Prefs.drawerItemsOrder
    )
}