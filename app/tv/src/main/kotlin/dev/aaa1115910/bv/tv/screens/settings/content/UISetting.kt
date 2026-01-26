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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.rememberCoroutineScope
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
import dev.aaa1115910.bv.entity.ThemeType
import dev.aaa1115910.bv.player.entity.PlayerLongPressAction
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.tv.component.settings.SettingListItem
import dev.aaa1115910.bv.tv.component.settings.SettingSwitchListItem
import dev.aaa1115910.bv.tv.component.HomeTopNavItem
import dev.aaa1115910.bv.tv.screens.settings.SettingsMenuNavItem
import dev.aaa1115910.bv.tv.util.NavItemConfig
import dev.aaa1115910.bv.tv.util.moveNavItemToFirstAndUnhide
import dev.aaa1115910.bv.tv.util.parseNavItemsOrderToConfig
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.requestFocus
import kotlin.math.roundToInt

@Composable
fun UISetting(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showDensityDialog by remember { mutableStateOf(false) }
    var showThemeTypeDialog by remember { mutableStateOf(false) }
    var showDefaultHomeTabDialog by remember { mutableStateOf(false) }
    var showGridColumnsDialog by remember { mutableStateOf(false) }
    var showHomeNavItemsDialog by remember { mutableStateOf(false) }
    var showLongPressActionDialog by remember { mutableStateOf(false) }
    var showOnlineViewerCountDialog by remember { mutableStateOf(false) }
    val density by Prefs.densityFlow.collectAsState(context.resources.displayMetrics.widthPixels / 960f)
    val themeType by Prefs.themeTypeFlow.collectAsState(Prefs.themeType)
    val showOnlineViewerCount by Prefs.showOnlineViewerCountFlow.collectAsState(Prefs.showOnlineViewerCount)
    var defaultHomeTab by remember { mutableStateOf(HomeTopNavItem.entries.getOrElse(Prefs.defaultHomeTab) { HomeTopNavItem.Recommend }) }
    var gridColumns by remember { mutableStateOf(Prefs.gridColumns) }

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
                        title = stringResource(R.string.settings_ui_theme_type_title),
                        supportText = stringResource(R.string.settings_ui_theme_type_text),
                        valueText = themeType.getDisplayName(context),
                        onClick = { showThemeTypeDialog = true }
                    )
                }
                item {
                    SettingListItem(
                        title = stringResource(R.string.settings_ui_default_home_tab_title),
                        supportText = stringResource(R.string.settings_ui_default_home_tab_text),
                        valueText = defaultHomeTab.getDisplayName(context),
                        onClick = { showDefaultHomeTabDialog = true }
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

    DefaultHomeTabDialog(
        show = showDefaultHomeTabDialog,
        onHideDialog = { showDefaultHomeTabDialog = false },
        defaultHomeTab = defaultHomeTab,
        onDefaultHomeTabChange = {
            defaultHomeTab = it
            Prefs.defaultHomeTab = it.ordinal

            // 更新排序配置：将新的默认标签移到第一位，并取消隐藏
            val currentOrder = Prefs.homeNavItemsOrder
            val updatedOrder = moveNavItemToFirstAndUnhide(currentOrder, it.ordinal)
            Prefs.homeNavItemsOrder = updatedOrder
        }
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
}

@Composable
private fun UIDensityDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    density: Float,
    onDensityChange: (Float) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val defaultDensity by remember { mutableFloatStateOf(context.resources.displayMetrics.widthPixels / 960f) }

    LaunchedEffect(show) {
        if (show) focusRequester.requestFocus(scope)
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
fun DefaultHomeTabDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    defaultHomeTab: HomeTopNavItem,
    onDefaultHomeTabChange: (HomeTopNavItem) -> Unit
) {
    if (show) {
        TvAlertDialog(
            modifier = modifier,
            onDismissRequest = { onHideDialog() },
            title = { Text(text = stringResource(R.string.settings_ui_default_home_tab_title)) },
            text = {
                Column {
                    HomeTopNavItem.entries.forEach {
                        ListItem(
                            selected = defaultHomeTab == it,
                            onClick = { onDefaultHomeTabChange(it) },
                            headlineContent = {
                                Text(text = it.getDisplayName(LocalContext.current))
                            },
                            trailingContent = {
                                RadioButton(
                                    selected = defaultHomeTab == it,
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
private fun HomeNavItemsEditDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    initialOrderString: String
) {
    if (!show) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    // 解析初始配置（按显示顺序）
    val initialConfigs = remember(initialOrderString) {
        parseNavItemsOrderToConfig(initialOrderString)
    }

    // 当前配置状态
    var navConfigs by remember { mutableStateOf(initialConfigs) }

    // 当前选中的索引
    var selectedIndex by remember { mutableIntStateOf(0) }

    // 为每个列表项创建 FocusRequester
    val focusRequesters = remember(navConfigs.size) {
        List(navConfigs.size) { FocusRequester() }
    }

    LaunchedEffect(show) {
        if (show) {
            focusRequester.requestFocus(scope)
            // 延迟请求焦点到第一个列表项
            focusRequesters.firstOrNull()?.requestFocus()
        }
    }

    TvAlertDialog(
        modifier = modifier,
        onDismissRequest = {
            // 关闭时自动保存
            saveNavConfigs(navConfigs)
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
                                    saveNavConfigs(navConfigs)
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
                                    val defaultHomeTabOrdinal = Prefs.defaultHomeTab
                                    if (config.ordinal != defaultHomeTabOrdinal) {
                                        navConfigs = navConfigs.toMutableList().apply {
                                            this[selectedIndex] = config.copy(hidden = !config.hidden)
                                        }
                                    }
                                    true
                                }
                                else -> false
                            }
                        }
                        false
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
                        val isDefaultHomeTab = config.ordinal == Prefs.defaultHomeTab

                        NavItemEditRow(
                            navItem = navItem,
                            hidden = config.hidden,
                            selected = isSelected,
                            isDefaultHomeTab = isDefaultHomeTab,
                            onFocus = { selectedIndex = index },
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
private fun NavItemEditRow(
    navItem: HomeTopNavItem,
    hidden: Boolean,
    selected: Boolean,
    isDefaultHomeTab: Boolean,
    onFocus: () -> Unit,
    focusRequester: FocusRequester
) {
    val context = LocalContext.current

    ListItem(
        selected = selected,
        onClick = { /* 点击由父组件处理 */ },
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

/**
 * 保存导航项配置到 Prefs
 * 默认标签强制不隐藏
 */
private fun saveNavConfigs(navConfigs: List<NavItemConfig>) {
    val defaultTabOrdinal = Prefs.defaultHomeTab
    val finalOrderString = navConfigs.joinToString(",") { config ->
        val shouldHide = if (config.ordinal == defaultTabOrdinal) {
            false  // 默认标签强制不隐藏
        } else {
            config.hidden
        }
        if (shouldHide) "-${config.ordinal}" else "${config.ordinal}"
    }
    Prefs.homeNavItemsOrder = finalOrderString
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
