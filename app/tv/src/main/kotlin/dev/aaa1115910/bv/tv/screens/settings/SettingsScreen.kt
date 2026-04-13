package dev.aaa1115910.bv.tv.screens.settings

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.tv.screens.settings.content.AboutSetting
import dev.aaa1115910.bv.tv.screens.settings.content.ApiSetting
import dev.aaa1115910.bv.tv.screens.settings.content.DanmakuFilterSetting
import dev.aaa1115910.bv.tv.screens.settings.content.InfoSetting
import dev.aaa1115910.bv.tv.screens.settings.content.LiveStreamingSetting
import dev.aaa1115910.bv.tv.screens.settings.content.NetworkSetting
import dev.aaa1115910.bv.tv.screens.settings.content.OtherSetting
import dev.aaa1115910.bv.tv.screens.settings.content.PlayerSetting
import dev.aaa1115910.bv.tv.screens.settings.content.SponsorBlockSetting
import dev.aaa1115910.bv.tv.screens.settings.content.StorageSetting
import dev.aaa1115910.bv.tv.screens.settings.content.UISetting
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.requestFocus

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    var currentMenu by remember { mutableStateOf(SettingsMenuNavItem.Player) }
    var focusInNav by remember { mutableStateOf(false) }
    var focusInContent by remember { mutableStateOf(false) }
    val navFocusRequester = remember { FocusRequester() }

    BackHandler(enabled = focusInContent) {
        focusInNav = true
    }

    Scaffold(
        modifier = modifier,
    ) { innerPadding ->
        Row(
            modifier = Modifier.padding(innerPadding)
        ) {
            // Left navigation panel
            Column(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight()
            ) {
                // Title
                Text(
                    modifier = Modifier.padding(
                        start = 48.dp,
                        top = 24.dp,
                        bottom = 4.dp,
                        end = 48.dp
                    ),
                    text = stringResource(R.string.title_activity_settings),
                    style = MaterialTheme.typography.displaySmall
                )
                // Subtitle: current category
                AnimatedContent(
                    targetState = currentMenu,
                    transitionSpec = {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                    },
                    label = "settings subtitle"
                ) { menu ->
                    Text(
                        modifier = Modifier.padding(
                            start = 48.dp,
                            bottom = 12.dp,
                            end = 48.dp
                        ),
                        text = menu.getDisplayName(LocalContext.current),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SettingsNav(
                    modifier = Modifier
                        .onFocusChanged { focusInNav = it.hasFocus },
                    currentMenu = currentMenu,
                    onMenuChanged = { currentMenu = it },
                    isFocusing = focusInNav,
                    focusRequester = navFocusRequester
                )
            }
            // Right content panel
            SettingContent(
                modifier = Modifier
                    .weight(5f)
                    .fillMaxSize()
                    .padding(top = 48.dp),
                onBackNav = { focusInNav = true },
                onContentFocusChanged = { focusInContent = it },
                currentMenu = currentMenu
            )
        }
    }
}

@Composable
fun SettingsNav(
    modifier: Modifier = Modifier,
    currentMenu: SettingsMenuNavItem,
    onMenuChanged: (SettingsMenuNavItem) -> Unit,
    isFocusing: Boolean,
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(isFocusing) {
        if (isFocusing) focusRequester.requestFocus(scope)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus(scope)
    }

    LazyColumn(
        modifier = modifier
            .focusGroup()
            .focusRestorer(focusRequester),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (item in SettingsMenuNavItem.entries) {
            val buttonModifier = if (currentMenu == item) Modifier
                .focusRequester(focusRequester)
                .fillMaxWidth()
            else Modifier.fillMaxWidth()
            item {
                SettingsMenuButton(
                    modifier = buttonModifier,
                    text = item.getDisplayName(context),
                    icon = item.icon,
                    selected = currentMenu == item,
                    onFocus = {
                        onMenuChanged(item)
                    }
                )
            }
        }
    }
}

enum class SettingsMenuNavItem(
    private val strRes: Int,
    val icon: ImageVector
) {
    Player(R.string.settings_item_player, Icons.Rounded.PlayCircle),
    UI(R.string.settings_item_ui, Icons.Rounded.Palette),
    Live(R.string.settings_item_live, Icons.Rounded.LiveTv),
    DanmakuFilter(R.string.settings_item_danmaku_filter, Icons.Rounded.FilterAlt),
    SponsorBlock(R.string.settings_item_sponsor_block, Icons.Rounded.Block),
    Api(R.string.settings_item_api, Icons.Rounded.Code),
    Other(R.string.settings_item_other, Icons.Rounded.MoreHoriz),
    Storage(R.string.settings_item_storage, Icons.Rounded.Storage),
    Network(R.string.settings_item_network, Icons.Rounded.Wifi),
    Info(R.string.settings_item_info, Icons.Rounded.Info),
    About(R.string.settings_item_about, Icons.Rounded.NewReleases);

    fun getDisplayName(context: Context) = context.getString(strRes)
}

@Composable
fun SettingContent(
    modifier: Modifier = Modifier,
    onBackNav: () -> Unit,
    onContentFocusChanged: (Boolean) -> Unit = {},
    currentMenu: SettingsMenuNavItem
) {
    Box(
        modifier = modifier
            .padding(24.dp)
    ) {
        SettingsDetail(
            modifier = Modifier.fillMaxSize(),
            onFocusBackMenuList = {
                onBackNav()
            },
            onContentFocusChanged = onContentFocusChanged
        ) {
            AnimatedContent(
                targetState = currentMenu,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInVertically { it / 8 } + fadeIn(tween(200)))
                            .togetherWith(slideOutVertically { -it / 8 } + fadeOut(tween(150)))
                    } else {
                        (slideInVertically { -it / 8 } + fadeIn(tween(200)))
                            .togetherWith(slideOutVertically { it / 8 } + fadeOut(tween(150)))
                    }
                },
                label = "settings content"
            ) { menu ->
                when (menu) {
                    SettingsMenuNavItem.Player -> PlayerSetting()
                    SettingsMenuNavItem.Info -> InfoSetting()
                    SettingsMenuNavItem.SponsorBlock -> SponsorBlockSetting()
                    SettingsMenuNavItem.About -> AboutSetting()
                    SettingsMenuNavItem.Other -> OtherSetting()
                    SettingsMenuNavItem.Network -> NetworkSetting()
                    SettingsMenuNavItem.UI -> UISetting()
                    SettingsMenuNavItem.Storage -> StorageSetting()
                    SettingsMenuNavItem.Api -> ApiSetting()
                    SettingsMenuNavItem.Live -> LiveStreamingSetting()
                    SettingsMenuNavItem.DanmakuFilter -> DanmakuFilterSetting()
                }
            }
        }
    }
}

@Composable
fun SettingsMenuButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector,
    onFocus: () -> Unit,
    onLoseFocus: () -> Unit = {},
    onClick: () -> Unit = {},
    selected: Boolean
) {
    var hasFocus by remember { mutableStateOf(false) }

    val contentColor = when {
        hasFocus -> MaterialTheme.colorScheme.inverseOnSurface
        selected -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }

    Surface(
        modifier = modifier
            .onFocusChanged {
                hasFocus = it.hasFocus
                if (it.hasFocus) onFocus() else onLoseFocus()
            }
            .clip(RoundedCornerShape(16.dp)),
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(16.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
            pressedContainerColor = MaterialTheme.colorScheme.inverseSurface
        ),
        border = ClickableSurfaceDefaults.border(
            border = if (selected) Border(
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) else Border.None,
            focusedBorder = Border.None
        ),
        scale = ClickableSurfaceDefaults.scale(
            scale = 1f,
            focusedScale = 1.02f
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = icon,
                contentDescription = null,
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor
            )
        }
    }
}

@Preview
@Composable
fun SettingsMenuButtonPreview() {
    BVTheme {
        Column(
            modifier = Modifier.size(300.dp, 200.dp).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsMenuButton(
                text = "播放器设置",
                icon = Icons.Rounded.PlayCircle,
                selected = true,
                onFocus = {}
            )
            SettingsMenuButton(
                text = "界面设置",
                icon = Icons.Rounded.Palette,
                selected = false,
                onFocus = {}
            )
        }
    }
}

@Composable
fun SettingsDetail(
    modifier: Modifier = Modifier,
    onFocusBackMenuList: () -> Unit,
    onContentFocusChanged: (Boolean) -> Unit = {},
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .focusGroup()
            .onFocusChanged {
                onContentFocusChanged(it.hasFocus)
            }
            .onPreviewKeyEvent {
                val result = it.key.nativeKeyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT
                if (result) onFocusBackMenuList()
                result
            }
    ) {
        content()
    }
}
