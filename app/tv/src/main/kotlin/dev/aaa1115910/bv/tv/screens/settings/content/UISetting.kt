package dev.aaa1115910.bv.tv.screens.settings.content

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Text
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.DynamicPageStyle
import dev.aaa1115910.bv.entity.DynamicTabType
import dev.aaa1115910.bv.entity.ThemeType
import dev.aaa1115910.bv.player.entity.PlayerLongPressAction
import dev.aaa1115910.bv.tv.activities.settings.PlayerBottomControlPanelCustomizeActivity
import dev.aaa1115910.bv.tv.component.UgcTopNavItem
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.tv.screens.main.DrawerItem
import dev.aaa1115910.bv.tv.component.settings.SettingListItem
import dev.aaa1115910.bv.tv.component.settings.SettingSwitchListItem
import dev.aaa1115910.bv.tv.component.HomeTopNavItem
import dev.aaa1115910.bv.tv.screens.settings.SettingsMenuNavItem
import dev.aaa1115910.bv.tv.util.NavItemConfig
import dev.aaa1115910.bv.tv.util.parseDrawerItemsOrderToConfig
import dev.aaa1115910.bv.tv.util.parseNavItemsOrderToConfig
import dev.aaa1115910.bv.tv.util.parseUgcNavItemsOrderToConfig
import dev.aaa1115910.bv.tv.render.TvUiRenderMode
import dev.aaa1115910.bv.tv.render.TvUiRenderSettings
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.requestFocusWithRetry
import dev.aaa1115910.bv.util.toast
import kotlin.math.roundToInt

@Composable
fun UISetting(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showDensityDialog by remember { mutableStateOf(false) }
    var showThemeTypeDialog by remember { mutableStateOf(false) }
    var showGridColumnsDialog by remember { mutableStateOf(false) }
    var showHomeNavItemsDialog by remember { mutableStateOf(false) }
    var showUgcNavItemsDialog by remember { mutableStateOf(false) }
    var showDrawerNavItemsDialog by remember { mutableStateOf(false) }
    var showLongPressActionDialog by remember { mutableStateOf(false) }
    var showOnlineViewerCountDialog by remember { mutableStateOf(false) }
    var showDynamicPageStyleDialog by remember { mutableStateOf(false) }
    var showDynamicDefaultTabDialog by remember { mutableStateOf(false) }
    var showTvUiRenderModeDialog by remember { mutableStateOf(false) }
    val density by Prefs.densityFlow.collectAsState(context.resources.displayMetrics.widthPixels / 960f)
    val themeType by Prefs.themeTypeFlow.collectAsState(Prefs.themeType)
    val enableMainUiAnimation by Prefs.enableMainUiAnimationFlow.collectAsState(Prefs.enableMainUiAnimation)
    val collapseVideoInfoRelatedVideos by Prefs.collapseVideoInfoRelatedVideosFlow.collectAsState(
        Prefs.collapseVideoInfoRelatedVideos
    )
    val showDetailPageBackgroundImage by Prefs.showDetailPageBackgroundImageFlow.collectAsState(
        Prefs.showDetailPageBackgroundImage
    )
    val showOnlineViewerCount by Prefs.showOnlineViewerCountFlow.collectAsState(Prefs.showOnlineViewerCount)
    var gridColumns by remember { mutableStateOf(Prefs.gridColumns) }
    var dynamicPageStyle by remember { mutableStateOf(Prefs.dynamicPageStyle) }
    var dynamicDefaultTab by remember { mutableStateOf(Prefs.dynamicDefaultTab) }
    var tvUiRenderMode by remember { mutableStateOf(TvUiRenderSettings.getMode(context)) }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = SettingsMenuNavItem.UI.getDisplayName(context),
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SettingListItem(
                        title = stringResource(R.string.settings_ui_density_title),
                        supportText = stringResource(R.string.settings_ui_density_text),
                        valueText = density.toString(),
                        onClick = { showDensityDialog = true }
                    )
                }
                item {
                    SettingListItem(
                        title = "4K 界面渲染",
                        supportText = "仅 TV 端生效；保持 4K 视频输出，界面可以 1080p 渲染后放大",
                        valueText = tvUiRenderMode.displayName,
                        onClick = { showTvUiRenderModeDialog = true },
                    )
                }
                item {
                    SettingListItem(
                        title = stringResource(R.string.settings_ui_theme_type_title),
                        supportText = stringResource(R.string.settings_ui_theme_type_text),
                        valueText = themeType.getDisplayName(context),
                        onClick = { showThemeTypeDialog = true }
                    )
                }
                item {
                    SettingListItem(
                        title = stringResource(R.string.settings_ui_grid_columns_title),
                        supportText = stringResource(R.string.settings_ui_grid_columns_text),
                        valueText = if (gridColumns == 4) stringResource(R.string.settings_ui_grid_columns_4) else stringResource(R.string.settings_ui_grid_columns_5),
                        onClick = { showGridColumnsDialog = true }
                    )
                }
                item {
                    SettingListItem(
                        title = stringResource(R.string.settings_ui_home_nav_items_title),
                        supportText = stringResource(R.string.settings_ui_home_nav_items_text),
                        onClick = { showHomeNavItemsDialog = true }
                    )
                }
                item {
                    SettingListItem(
                        title = stringResource(R.string.settings_ui_ugc_nav_items_title),
                        supportText = stringResource(R.string.settings_ui_ugc_nav_items_text),
                        onClick = { showUgcNavItemsDialog = true }
                    )
                }
                item {
                    SettingListItem(
                        title = stringResource(R.string.settings_ui_drawer_nav_items_title),
                        supportText = stringResource(R.string.settings_ui_drawer_nav_items_text),
                        onClick = { showDrawerNavItemsDialog = true }
                    )
                }
                item {
                    SettingSwitchListItem(
                        title = stringResource(R.string.settings_ui_main_animation_title),
                        supportText = stringResource(R.string.settings_ui_main_animation_text),
                        checked = enableMainUiAnimation,
                        onCheckedChange = { Prefs.enableMainUiAnimation = it }
                    )
                }
                item {
                    SettingSwitchListItem(
                        title = stringResource(R.string.settings_ui_collapse_video_info_related_title),
                        supportText = stringResource(R.string.settings_ui_collapse_video_info_related_text),
                        checked = collapseVideoInfoRelatedVideos,
                        onCheckedChange = { Prefs.collapseVideoInfoRelatedVideos = it }
                    )
                }
                item {
                    SettingSwitchListItem(
                        title = stringResource(R.string.settings_ui_detail_background_title),
                        supportText = stringResource(R.string.settings_ui_detail_background_text),
                        checked = showDetailPageBackgroundImage,
                        onCheckedChange = { Prefs.showDetailPageBackgroundImage = it }
                    )
                }
                item {
                    val currentLongPressAction by Prefs.playerLongPressActionFlow.collectAsState(
                        Prefs.playerLongPressAction
                    )
                    SettingListItem(
                        title = stringResource(R.string.settings_ui_long_press_title),
                        supportText = stringResource(R.string.settings_ui_long_press_text),
                        valueText = currentLongPressAction.getDisplayName(LocalContext.current),
                        onClick = { showLongPressActionDialog = true }
                    )
                }
                item {
                    SettingListItem(
                        title = stringResource(R.string.settings_ui_bottom_control_panel_title),
                        supportText = stringResource(R.string.settings_ui_bottom_control_panel_text),
                        onClick = { PlayerBottomControlPanelCustomizeActivity.actionStart(context) }
                    )
                }
                item {
                    SettingListItem(
                        title = "视频在线观看人数",
                        supportText = "设置播放器在线人数显示方式",
                        valueText = when (showOnlineViewerCount) {
                            0 -> "不显示"
                            1 -> "30 秒后隐藏"
                            2 -> "始终显示"
                            else -> "30 秒后隐藏"
                        },
                        onClick = { showOnlineViewerCountDialog = true }
                    )
                }
                item {
                    SettingListItem(
                        title = "动态页面样式",
                        supportText = "切换动态页面的显示样式",
                        valueText = dynamicPageStyle.getDisplayName(context),
                        onClick = { showDynamicPageStyleDialog = true }
                    )
                }
                if (dynamicPageStyle == DynamicPageStyle.New) {
                    item {
                        SettingListItem(
                            title = "新动态页默认页面",
                            supportText = "设置新动态页打开时的默认页面",
                            valueText = dynamicDefaultTab.getDisplayName(context),
                            onClick = { showDynamicDefaultTabDialog = true }
                        )
                    }
                }
            }
        }
    }

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

    LongPressActionDialog(
        show = showLongPressActionDialog,
        onHideDialog = { showLongPressActionDialog = false },
        longPressAction = Prefs.playerLongPressAction,
        onLongPressActionChange = { Prefs.playerLongPressAction = it }
    )

    OnlineViewerCountDialog(
        show = showOnlineViewerCountDialog,
        onHideDialog = { showOnlineViewerCountDialog = false },
        showOnlineViewerCount = showOnlineViewerCount,
        onShowOnlineViewerCountChange = { Prefs.showOnlineViewerCount = it }
    )

    DynamicPageStyleDialog(
        show = showDynamicPageStyleDialog,
        onHideDialog = { showDynamicPageStyleDialog = false },
        dynamicPageStyle = dynamicPageStyle,
        onDynamicPageStyleChange = {
            dynamicPageStyle = it
            Prefs.dynamicPageStyle = it
        }
    )

    DynamicDefaultTabDialog(
        show = showDynamicDefaultTabDialog,
        onHideDialog = { showDynamicDefaultTabDialog = false },
        dynamicDefaultTab = dynamicDefaultTab,
        onDynamicDefaultTabChange = {
            dynamicDefaultTab = it
            Prefs.dynamicDefaultTab = it
        }
    )

    TvUiRenderModeDialog(
        show = showTvUiRenderModeDialog,
        currentMode = tvUiRenderMode,
        onHideDialog = { showTvUiRenderModeDialog = false },
        onModeSelected = { mode ->
            tvUiRenderMode = mode
            TvUiRenderSettings.setMode(context, mode)
            showTvUiRenderModeDialog = false
        },
    )
}

@Composable
private fun TvUiRenderModeDialog(
    show: Boolean,
    currentMode: TvUiRenderMode,
    onHideDialog: () -> Unit,
    onModeSelected: (TvUiRenderMode) -> Unit,
) {
    if (!show) return
    TvAlertDialog(
        onDismissRequest = onHideDialog,
        title = { Text("4K 界面渲染") },
        text = {
            Column {
                Text("设置修改后，重新进入界面或重启 APP 生效。")
                TvUiRenderMode.entries.forEach { mode ->
                    ListItem(
                        selected = mode == currentMode,
                        onClick = { onModeSelected(mode) },
                        headlineContent = { Text(mode.displayName) },
                        trailingContent = {
                            RadioButton(
                                selected = mode == currentMode,
                                onClick = null,
                            )
                        },
                    )
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
fun UIDensityDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    density: Float,
    onDensityChange: (Float) -> Unit
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val defaultDensity by remember { mutableFloatStateOf(context.resources.displayMetrics.widthPixels / 960f) }

    LaunchedEffect(show) {
        if (show) focusRequester.requestFocusWithRetry()
    }

    // 这里得采用固定的 Density，否则会导致更改 Density 时，对话框反复重新加载
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = defaultDensity,
            fontScale = LocalDensity.current.fontScale
        )
    ) {
        if (show) {
            TvAlertDialog(
                modifier = modifier,
                onDismissRequest = { onHideDialog() },
                title = { Text(text = stringResource(R.string.settings_ui_density_title)) },
                text = {
                    Column(
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .focusable()
                            .fillMaxWidth()
                            .onPreviewKeyEvent {
                                if (it.key == Key.DirectionUp || it.key == Key.DirectionDown) {
                                    if (it.type == KeyEventType.KeyDown) {
                                        var newDensity = if (it.key == Key.DirectionUp)
                                            density + 0.1f else density - 0.1f
                                        newDensity = (newDensity * 10).roundToInt() / 10f
                                        if (newDensity < 0.5f) newDensity = 0.5f
                                        if (newDensity > 5f) newDensity = 5f
                                        onDensityChange(newDensity)
                                    }
                                }
                                false
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Rounded.ArrowDropUp, contentDescription = null)
                        Text(text = "$density")
                        Icon(imageVector = Icons.Rounded.ArrowDropDown, contentDescription = null)
                    }
                },
                confirmButton = {}
            )
        }
    }
}

@Composable
fun ThemeTypeDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    themeType: ThemeType,
    onThemeTypeChange: (ThemeType) -> Unit
) {
    if (show) {
        TvAlertDialog(
            modifier = modifier,
            onDismissRequest = { onHideDialog() },
            title = { Text(text = stringResource(R.string.settings_ui_theme_type_title)) },
            text = {
                Column {
                    ThemeType.entries.forEach {
                        ListItem(
                            selected = themeType == it,
                            onClick = { onThemeTypeChange(it) },
                            headlineContent = {
                                Text(text = it.getDisplayName(LocalContext.current))
                            },
                            trailingContent = {
                                RadioButton(
                                    selected = themeType == it,
                                    onClick = null
                                )
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Preview
@Composable
fun UIDensityDialogPreview() {
    val show by remember { mutableStateOf(true) }
    var density by remember { mutableFloatStateOf(1.0f) }

    BVTheme {
        UIDensityDialog(
            show = show,
            onHideDialog = {},
            density = density,
            onDensityChange = { density = it }
        )
    }
}

@Preview
@Composable
private fun ThemeTypeDialogPreview() {
    val show by remember { mutableStateOf(true) }
    val themeType by remember { mutableStateOf(ThemeType.Auto) }

    BVTheme {
        ThemeTypeDialog(
            show = show,
            onHideDialog = {},
            themeType = themeType,
            onThemeTypeChange = {}
        )
    }
}

@Composable
fun GridColumnsDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    gridColumns: Int,
    onGridColumnsChange: (Int) -> Unit
) {
    val gridColumnOptions = listOf(4, 5)
    if (show) {
        TvAlertDialog(
            modifier = modifier,
            onDismissRequest = { onHideDialog() },
            title = { Text(text = stringResource(R.string.settings_ui_grid_columns_title)) },
            text = {
                Column {
                    gridColumnOptions.forEach { columns ->
                        ListItem(
                            selected = gridColumns == columns,
                            onClick = { onGridColumnsChange(columns) },
                            headlineContent = {
                                Text(
                                    text = if (columns == 4)
                                        stringResource(R.string.settings_ui_grid_columns_4)
                                    else
                                        stringResource(R.string.settings_ui_grid_columns_5)
                                )
                            },
                            trailingContent = {
                                RadioButton(
                                    selected = gridColumns == columns,
                                    onClick = null
                                )
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun HomeNavItemsEditDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    initialOrderString: String
) {
    if (!show) return

    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    // 解析初始配置（按显示顺序）
    val initialConfigs = remember(initialOrderString) {
        parseNavItemsOrderToConfig(initialOrderString)
    }

    // 当前配置状态
    var navConfigs by remember { mutableStateOf(initialConfigs) }
    var defaultHomeTabOrdinal by remember { mutableIntStateOf(Prefs.defaultHomeTab) }

    // 当前选中的索引
    var selectedIndex by remember { mutableIntStateOf(0) }

    // 为每个列表项创建 FocusRequester
    val focusRequesters = remember(navConfigs.size) {
        List(navConfigs.size) { FocusRequester() }
    }

    LaunchedEffect(show) {
        if (show) {
            focusRequester.requestFocusWithRetry()
            // 延迟请求焦点到第一个列表项
            focusRequesters.firstOrNull()?.requestFocus()
        }
    }

    TvAlertDialog(
        modifier = modifier,
        onDismissRequest = {
            // 关闭时自动保存
            saveNavConfigs(navConfigs, defaultHomeTabOrdinal)
            onHideDialog()
        },
        title = { Text(text = stringResource(R.string.settings_ui_home_nav_items_title)) },
        text = {
            Column(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown) {
                            when (it.key) {
                                Key.Back -> {
                                    // 返回键：关闭弹框并保存
                                    saveNavConfigs(navConfigs, defaultHomeTabOrdinal)
                                    onHideDialog()
                                    true
                                }
                                Key.DirectionLeft -> {
                                    // 向左移动：与上一个元素交换位置
                                    // 但第一个元素（默认标签）不能向左移动
                                    if (selectedIndex > 0) {
                                        navConfigs = navConfigs.toMutableList().apply {
                                            val temp = this[selectedIndex]
                                            this[selectedIndex] = this[selectedIndex - 1]
                                            this[selectedIndex - 1] = temp
                                        }
                                        selectedIndex--
                                    }
                                    true
                                }
                                Key.DirectionRight -> {
                                    // 向右移动：与下一个元素交换位置
                                    if (selectedIndex < navConfigs.size - 1) {
                                        navConfigs = navConfigs.toMutableList().apply {
                                            val temp = this[selectedIndex]
                                            this[selectedIndex] = this[selectedIndex + 1]
                                            this[selectedIndex + 1] = temp
                                        }
                                        selectedIndex++
                                    }
                                    true
                                }
                                Key.DirectionUp -> {
                                    // 向上选择
                                    if (selectedIndex > 0) selectedIndex--
                                    true
                                }
                                Key.DirectionDown -> {
                                    // 向下选择
                                    if (selectedIndex < navConfigs.size - 1) selectedIndex++
                                    true
                                }
                                Key.Enter, Key.DirectionCenter -> {
                                    // 确认键：切换隐藏状态（除了默认首页标签）
                                    val config = navConfigs[selectedIndex]
                                    if (config.ordinal != defaultHomeTabOrdinal) {
                                        navConfigs = navConfigs.toMutableList().apply {
                                            this[selectedIndex] = config.copy(hidden = !config.hidden)
                                        }
                                    }
                                    true
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    }
            ) {
                // 提示文字
                Text(
                    text = stringResource(R.string.settings_ui_home_nav_items_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                navConfigs.forEachIndexed { index, config ->
                    val navItem = HomeTopNavItem.entries.getOrNull(config.ordinal)
                    if (navItem != null) {
                        val isSelected = index == selectedIndex
                        val isDefaultHomeTab = config.ordinal == defaultHomeTabOrdinal

                        NavItemEditRow(
                            navItem = navItem,
                            hidden = config.hidden,
                            selected = isSelected,
                            isDefaultHomeTab = isDefaultHomeTab,
                            onFocus = { selectedIndex = index },
                            onLongClick = {
                                defaultHomeTabOrdinal = config.ordinal
                                navConfigs = navConfigs.toMutableList().apply {
                                    this[index] = config.copy(hidden = false)
                                }
                                Prefs.defaultHomeTab = config.ordinal
                            },
                            focusRequester = focusRequesters[index]
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun DrawerNavItemsEditDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    initialOrderString: String
) {
    if (!show) return

    val focusRequester = remember { FocusRequester() }

    val initialConfigs = remember(initialOrderString) {
        parseDrawerItemsOrderToConfig(initialOrderString)
    }

    var navConfigs by remember { mutableStateOf(initialConfigs) }
    var defaultDrawerItemOrdinal by remember { mutableIntStateOf(Prefs.defaultDrawerTab) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    val focusRequesters = remember(navConfigs.size) {
        List(navConfigs.size) { FocusRequester() }
    }

    LaunchedEffect(show) {
        if (show) {
            focusRequester.requestFocusWithRetry()
            focusRequesters.firstOrNull()?.requestFocus()
        }
    }

    TvAlertDialog(
        modifier = modifier,
        onDismissRequest = {
            saveDrawerNavConfigs(navConfigs, defaultDrawerItemOrdinal)
            onHideDialog()
        },
        title = { Text(text = stringResource(R.string.settings_ui_drawer_nav_items_title)) },
        text = {
            Column(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown) {
                            when (it.key) {
                                Key.Back -> {
                                    saveDrawerNavConfigs(navConfigs, defaultDrawerItemOrdinal)
                                    onHideDialog()
                                    true
                                }
                                Key.DirectionLeft -> {
                                    if (selectedIndex > 0) {
                                        navConfigs = navConfigs.toMutableList().apply {
                                            val temp = this[selectedIndex]
                                            this[selectedIndex] = this[selectedIndex - 1]
                                            this[selectedIndex - 1] = temp
                                        }
                                        selectedIndex--
                                    }
                                    true
                                }
                                Key.DirectionRight -> {
                                    if (selectedIndex < navConfigs.size - 1) {
                                        navConfigs = navConfigs.toMutableList().apply {
                                            val temp = this[selectedIndex]
                                            this[selectedIndex] = this[selectedIndex + 1]
                                            this[selectedIndex + 1] = temp
                                        }
                                        selectedIndex++
                                    }
                                    true
                                }
                                Key.DirectionUp -> {
                                    if (selectedIndex > 0) selectedIndex--
                                    true
                                }
                                Key.DirectionDown -> {
                                    if (selectedIndex < navConfigs.size - 1) selectedIndex++
                                    true
                                }
                                Key.Enter, Key.DirectionCenter -> {
                                    val config = navConfigs[selectedIndex]
                                    if (config.ordinal != defaultDrawerItemOrdinal) {
                                        navConfigs = navConfigs.toMutableList().apply {
                                            this[selectedIndex] = config.copy(hidden = !config.hidden)
                                        }
                                    }
                                    true
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    }
            ) {
                Text(
                    text = stringResource(R.string.settings_ui_drawer_nav_items_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                navConfigs.forEachIndexed { index, config ->
                    val drawerItem = DrawerItem.entries.getOrNull(config.ordinal)?.takeIf { it.isConfigurable }
                    if (drawerItem != null) {
                        val isSelected = index == selectedIndex
                        DrawerItemEditRow(
                            drawerItem = drawerItem,
                            hidden = config.hidden,
                            selected = isSelected,
                            isDefaultDrawerItem = config.ordinal == defaultDrawerItemOrdinal,
                            onFocus = { selectedIndex = index },
                            onLongClick = {
                                defaultDrawerItemOrdinal = config.ordinal
                                navConfigs = navConfigs.toMutableList().apply {
                                    this[index] = config.copy(hidden = false)
                                }
                                Prefs.defaultDrawerTab = config.ordinal
                            },
                            focusRequester = focusRequesters[index]
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun UgcNavItemsEditDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    initialOrderString: String
) {
    if (!show) return

    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    val initialConfigs = remember(initialOrderString) {
        parseUgcNavItemsOrderToConfig(initialOrderString)
    }

    var navConfigs by remember { mutableStateOf(initialConfigs) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val focusRequesters = remember(navConfigs.size) {
        List(navConfigs.size) { FocusRequester() }
    }

    LaunchedEffect(show) {
        if (show) {
            focusRequester.requestFocusWithRetry()
            focusRequesters.firstOrNull()?.requestFocus()
        }
    }

    LaunchedEffect(selectedIndex, navConfigs.size) {
        if (navConfigs.isNotEmpty()) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    TvAlertDialog(
        modifier = modifier,
        onDismissRequest = {
            saveUgcNavConfigs(navConfigs)
            onHideDialog()
        },
        title = { Text(text = stringResource(R.string.settings_ui_ugc_nav_items_title)) },
        text = {
            Column(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown) {
                            when (it.key) {
                                Key.Back -> {
                                    saveUgcNavConfigs(navConfigs)
                                    onHideDialog()
                                    true
                                }
                                Key.DirectionLeft -> {
                                    if (selectedIndex > 0) {
                                        navConfigs = navConfigs.toMutableList().apply {
                                            val temp = this[selectedIndex]
                                            this[selectedIndex] = this[selectedIndex - 1]
                                            this[selectedIndex - 1] = temp
                                        }
                                        selectedIndex--
                                    }
                                    true
                                }
                                Key.DirectionRight -> {
                                    if (selectedIndex < navConfigs.size - 1) {
                                        navConfigs = navConfigs.toMutableList().apply {
                                            val temp = this[selectedIndex]
                                            this[selectedIndex] = this[selectedIndex + 1]
                                            this[selectedIndex + 1] = temp
                                        }
                                        selectedIndex++
                                    }
                                    true
                                }
                                Key.DirectionUp -> {
                                    if (selectedIndex > 0) selectedIndex--
                                    true
                                }
                                Key.DirectionDown -> {
                                    if (selectedIndex < navConfigs.size - 1) selectedIndex++
                                    true
                                }
                                Key.Enter, Key.DirectionCenter -> {
                                    val config = navConfigs[selectedIndex]
                                    val visibleCount = navConfigs.count { !it.hidden }
                                    if (!config.hidden && visibleCount <= 1) {
                                        context.getString(R.string.settings_ui_ugc_nav_items_keep_one).toast(context)
                                    } else {
                                        navConfigs = navConfigs.toMutableList().apply {
                                            this[selectedIndex] = config.copy(hidden = !config.hidden)
                                        }
                                    }
                                    true
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    }
            ) {
                Text(
                    text = stringResource(R.string.settings_ui_ugc_nav_items_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(
                        items = navConfigs,
                        key = { _, config -> config.ordinal }
                    ) { index, config ->
                        val navItem = UgcTopNavItem.entries.getOrNull(config.ordinal)
                        if (navItem != null) {
                            UgcNavItemEditRow(
                                navItem = navItem,
                                hidden = config.hidden,
                                selected = index == selectedIndex,
                                onFocus = { selectedIndex = index },
                                focusRequester = focusRequesters[index],
                                context = context
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun NavItemEditRow(
    navItem: HomeTopNavItem,
    hidden: Boolean,
    selected: Boolean,
    isDefaultHomeTab: Boolean,
    onFocus: () -> Unit,
    onLongClick: () -> Unit,
    focusRequester: FocusRequester
) {
    val context = LocalContext.current

    ListItem(
        selected = selected,
        onClick = { },
        onLongClick = onLongClick,
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.hasFocus) onFocus() },
        colors = androidx.tv.material3.ListItemDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            focusedSelectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            pressedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        headlineContent = {
            Text(
                text = navItem.getDisplayName(context),
                color = if (hidden)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurface
            )
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 默认首页标签标记
                if (isDefaultHomeTab) {
                    Text(
                        text = stringResource(R.string.settings_ui_home_nav_default_tag),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // 隐藏状态标记
                if (hidden) {
                    Text(
                        text = stringResource(R.string.settings_ui_home_nav_hidden),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = stringResource(R.string.settings_ui_home_nav_visible),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

@Composable
private fun UgcNavItemEditRow(
    navItem: UgcTopNavItem,
    hidden: Boolean,
    selected: Boolean,
    onFocus: () -> Unit,
    focusRequester: FocusRequester,
    context: android.content.Context
) {
    ListItem(
        selected = selected,
        onClick = { },
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.hasFocus) onFocus() },
        colors = androidx.tv.material3.ListItemDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            focusedSelectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            pressedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        headlineContent = {
            Text(
                text = navItem.getDisplayName(context),
                color = if (hidden) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        },
        trailingContent = {
            Text(
                text = if (hidden) stringResource(R.string.settings_ui_home_nav_hidden) else stringResource(R.string.settings_ui_home_nav_visible),
                style = MaterialTheme.typography.bodySmall,
                color = if (hidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun DrawerItemEditRow(
    drawerItem: DrawerItem,
    hidden: Boolean,
    selected: Boolean,
    isDefaultDrawerItem: Boolean,
    onFocus: () -> Unit,
    onLongClick: () -> Unit,
    focusRequester: FocusRequester
) {
    ListItem(
        selected = selected,
        onClick = { },
        onLongClick = onLongClick,
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.hasFocus) onFocus() },
        colors = androidx.tv.material3.ListItemDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            focusedSelectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            pressedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        headlineContent = {
            Text(
                text = drawerItem.displayName,
                color = if (hidden) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isDefaultDrawerItem) {
                    Text(
                        text = stringResource(R.string.settings_ui_home_nav_default_tag),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = if (hidden) stringResource(R.string.settings_ui_home_nav_hidden) else stringResource(R.string.settings_ui_home_nav_visible),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

/**
 * 保存导航项配置到 Prefs
 * 默认标签强制不隐藏
 */
private fun saveNavConfigs(navConfigs: List<NavItemConfig>, defaultHomeTabOrdinal: Int) {
    val effectiveDefaultOrdinal = HomeTopNavItem.entries
        .getOrNull(defaultHomeTabOrdinal)
        ?.ordinal
        ?: HomeTopNavItem.Recommend.ordinal

    val normalizedConfigs = navConfigs.map { config ->
        if (config.ordinal == effectiveDefaultOrdinal) {
            config.copy(hidden = false)
        } else {
            config
        }
    }.let { configs ->
        if (configs.any { !it.hidden }) {
            configs
        } else {
            configs.map { config ->
                if (config.ordinal == effectiveDefaultOrdinal) {
                    config.copy(hidden = false)
                } else {
                    config
                }
            }
        }
    }

    Prefs.homeNavItemsOrder = normalizedConfigs.joinToString(",") { config ->
        if (config.hidden) "-${config.ordinal}" else "${config.ordinal}"
    }
    Prefs.defaultHomeTab = effectiveDefaultOrdinal
}

/**
 * 保存 UGC 导航项配置到 Prefs
 * 至少保留一个分区可见，兜底显示第一个分区
 */
private fun saveUgcNavConfigs(navConfigs: List<NavItemConfig>) {
    val fallbackOrdinal = UgcTopNavItem.Douga.ordinal
    val normalizedConfigs = navConfigs.let { configs ->
        if (configs.any { !it.hidden }) {
            configs
        } else {
            configs.map { config ->
                if (config.ordinal == fallbackOrdinal) {
                    config.copy(hidden = false)
                } else {
                    config
                }
            }
        }
    }

    Prefs.ugcNavItemsOrder = normalizedConfigs.joinToString(",") { config ->
        if (config.hidden) "-${config.ordinal}" else "${config.ordinal}"
    }
}

/**
 * 保存左侧业务导航项配置到 Prefs
 * 至少保留一个业务项可见，兜底显示首页
 */
private fun saveDrawerNavConfigs(navConfigs: List<NavItemConfig>, defaultDrawerItemOrdinal: Int) {
    val effectiveDefaultOrdinal = DrawerItem.entries
        .getOrNull(defaultDrawerItemOrdinal)
        ?.takeIf { it.isConfigurable }
        ?.ordinal
        ?: DrawerItem.defaultConfigurableItem.ordinal

    val normalizedConfigs = navConfigs.map { config ->
        if (config.ordinal == effectiveDefaultOrdinal) {
            config.copy(hidden = false)
        } else {
            config
        }
    }.let { configs ->
        if (configs.any { !it.hidden }) {
            configs
        } else {
            configs.map { config ->
                if (config.ordinal == effectiveDefaultOrdinal) {
                    config.copy(hidden = false)
                } else {
                    config
                }
            }
        }
    }

    Prefs.drawerItemsOrder = normalizedConfigs.joinToString(",") { config ->
        if (config.hidden) "-${config.ordinal}" else "${config.ordinal}"
    }
    Prefs.defaultDrawerTab = effectiveDefaultOrdinal
}

@Composable
fun LongPressActionDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    longPressAction: PlayerLongPressAction,
    onLongPressActionChange: (PlayerLongPressAction) -> Unit
) {
    var selectedAction by remember(show) { mutableStateOf(longPressAction) }

    if (show) {
        TvAlertDialog(
            modifier = modifier,
            onDismissRequest = { onHideDialog() },
            title = { Text(text = stringResource(R.string.settings_ui_long_press_title)) },
            text = {
                Column {
                    PlayerLongPressAction.entries.forEach {
                        ListItem(
                            selected = selectedAction == it,
                            onClick = {
                                selectedAction = it
                                onLongPressActionChange(it)
                            },
                            headlineContent = {
                                Text(text = it.getDisplayName(LocalContext.current))
                            },
                            trailingContent = {
                                RadioButton(
                                    selected = selectedAction == it,
                                    onClick = null
                                )
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun OnlineViewerCountDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    showOnlineViewerCount: Int,
    onShowOnlineViewerCountChange: (Int) -> Unit
) {
    if (show) {
        TvAlertDialog(
            modifier = modifier,
            onDismissRequest = { onHideDialog() },
            title = { Text(text = "视频在线观看人数") },
            text = {
                Column {
                    val options = listOf(
                        "不显示" to 0,
                        "30 秒后隐藏" to 1,
                        "始终显示" to 2
                    )
                    options.forEach { (text, value) ->
                        ListItem(
                            selected = showOnlineViewerCount == value,
                            onClick = { onShowOnlineViewerCountChange(value) },
                            headlineContent = { Text(text = text) },
                            trailingContent = {
                                RadioButton(
                                    selected = showOnlineViewerCount == value,
                                    onClick = null
                                )
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun DynamicPageStyleDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    dynamicPageStyle: DynamicPageStyle,
    onDynamicPageStyleChange: (DynamicPageStyle) -> Unit
) {
    if (show) {
        TvAlertDialog(
            modifier = modifier,
            onDismissRequest = { onHideDialog() },
            title = { Text(text = "动态页面样式") },
            text = {
                Column {
                    DynamicPageStyle.entries.forEach {
                        ListItem(
                            selected = dynamicPageStyle == it,
                            onClick = { onDynamicPageStyleChange(it) },
                            headlineContent = {
                                Text(text = it.getDisplayName(LocalContext.current))
                            },
                            trailingContent = {
                                RadioButton(
                                    selected = dynamicPageStyle == it,
                                    onClick = null
                                )
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun DynamicDefaultTabDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    dynamicDefaultTab: DynamicTabType,
    onDynamicDefaultTabChange: (DynamicTabType) -> Unit
) {
    val options = listOf(DynamicTabType.All, DynamicTabType.Video)
    if (show) {
        TvAlertDialog(
            modifier = modifier,
            onDismissRequest = { onHideDialog() },
            title = { Text(text = "新动态页默认页面") },
            text = {
                Column {
                    options.forEach {
                        ListItem(
                            selected = dynamicDefaultTab == it,
                            onClick = { onDynamicDefaultTabChange(it) },
                            headlineContent = {
                                Text(text = it.getDisplayName(LocalContext.current))
                            },
                            trailingContent = {
                                RadioButton(
                                    selected = dynamicDefaultTab == it,
                                    onClick = null
                                )
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}
