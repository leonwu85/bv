package dev.aaa1115910.bv.tv.screens.main

import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.tv.util.drawerNavItemsFlow
import dev.aaa1115910.bv.tv.util.parseDrawerItemsOrder
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.ifElse
import dev.aaa1115910.bv.util.isDpadDown
import dev.aaa1115910.bv.util.isDpadRight
import dev.aaa1115910.bv.util.isDpadUp
import dev.aaa1115910.bv.util.isKeyDown
import dev.aaa1115910.bv.util.onDelayFocusChanged
import kotlinx.coroutines.delay

private enum class DrawerFocusLayer {
    Navigation,
    Content,
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawerContent(
    modifier: Modifier = Modifier,
    focusRequesters: Map<DrawerItem, FocusRequester>,
    currentDrawerItem: DrawerItem = DrawerItem.Home,
    isLogin: Boolean = false,
    avatar: String = "",
    username: String = "",
    onDrawerItemChanged: (DrawerItem) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onFocusToContent: (DrawerItem) -> Unit = {}
) {
    val enableMainUiAnimation by Prefs.enableMainUiAnimationFlow.collectAsState(Prefs.enableMainUiAnimation)
    val drawerSelectionDelay = if (enableMainUiAnimation) 200L else 0L
    val hasUserAvatar = isLogin && avatar.isNotBlank()
    val userDisplayName = if (isLogin) username.ifBlank { "用户" } else DrawerItem.User.displayName

    var drawerFocusedItem by remember { mutableStateOf(currentDrawerItem) }
    var focusLayer by remember { mutableStateOf(DrawerFocusLayer.Content) }
    var canMoveFocusToContent by remember { mutableStateOf(true) }
    val isNavigationFocused = focusLayer == DrawerFocusLayer.Navigation
    val isContentFocused = focusLayer == DrawerFocusLayer.Content
    val visualSelectedItem = if (isContentFocused) currentDrawerItem else drawerFocusedItem

    LaunchedEffect(drawerFocusedItem, currentDrawerItem, focusLayer) {
        if (!isNavigationFocused || !drawerFocusedItem.hasContentPanel || drawerFocusedItem == currentDrawerItem) {
            canMoveFocusToContent = true
            return@LaunchedEffect
        }
        canMoveFocusToContent = false
        delay(drawerSelectionDelay)
        onDrawerItemChanged(drawerFocusedItem)
        // 别急着向右移动焦点，动画还没结束
        delay(drawerSelectionDelay)
        canMoveFocusToContent = true
    }

    val menuItems by drawerNavItemsFlow.collectAsState(
        initial = remember { parseDrawerItemsOrder(Prefs.drawerItemsOrder) }
    )

    LaunchedEffect(menuItems, currentDrawerItem, focusLayer) {
        val fallbackItem = menuItems.firstOrNull() ?: DrawerItem.defaultConfigurableItem
        if (isContentFocused) {
            drawerFocusedItem = currentDrawerItem
            return@LaunchedEffect
        }

        if (drawerFocusedItem.isConfigurable && drawerFocusedItem !in menuItems) {
            drawerFocusedItem = fallbackItem
        }
    }

    val userFocusRequester = focusRequesters.getValue(DrawerItem.User)
    val settingsFocusRequester = focusRequesters.getValue(DrawerItem.Settings)
    val currentDrawerFocusRequester = focusRequesters.getValue(currentDrawerItem)
    val firstMenuFocusRequester = menuItems.firstOrNull()?.let { focusRequesters.getValue(it) }
    val lastMenuFocusRequester = menuItems.lastOrNull()?.let { focusRequesters.getValue(it) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp)
            .focusProperties {
                onEnter = { currentDrawerFocusRequester.requestFocus() }
            }
            .focusGroup()
            .focusRestorer(currentDrawerFocusRequester)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.isDpadRight()) {
                    if (keyEvent.isKeyDown()) {
                        if (canMoveFocusToContent) onFocusToContent(drawerFocusedItem)
                        return@onPreviewKeyEvent true
                    }
                }
                false
            }
            .onDelayFocusChanged(delayTime = 0) {
                focusLayer = if (it.hasFocus) DrawerFocusLayer.Navigation else DrawerFocusLayer.Content
            },
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        NavigationRailItem(
            modifier = Modifier
                .focusRequester(userFocusRequester)
                .focusProperties {
                    down = firstMenuFocusRequester ?: settingsFocusRequester
                }
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.isKeyDown() && keyEvent.isDpadDown()) {
                        (firstMenuFocusRequester ?: settingsFocusRequester).requestFocus()
                        return@onPreviewKeyEvent true
                    }
                    false
                }
                .onFocusChanged {
                    if (it.hasFocus && isNavigationFocused) {
                        drawerFocusedItem = DrawerItem.User
                    }
                },
            onClick = {
                drawerFocusedItem = DrawerItem.User
            },
            selected = visualSelectedItem == DrawerItem.User,
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = Color.Transparent,
                indicatorColor = Color.Transparent
            ),
            icon = {
                if (hasUserAvatar) {
                    AsyncImage(
                        modifier = Modifier
                            .size(52.dp)
                            .ifElse(
                                isNavigationFocused && drawerFocusedItem == DrawerItem.User,
                                Modifier
                                    .border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.inverseSurface,
                                        shape = CircleShape
                                    )
                            )
                            .padding(3.dp)  // 边框和图片之间的1dp透明区域
                            .clip(CircleShape),
                        model = avatar,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds
                    )
                } else {
                    Icon(
                        modifier = Modifier
                            .size(46.dp)
                            .ifElse(
                                isNavigationFocused && drawerFocusedItem == DrawerItem.User,
                                Modifier
                                    .border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.inverseSurface,
                                        shape = CircleShape
                                    )
                            )
                            .clip(CircleShape),
                        imageVector = DrawerItem.User.displayIcon,
                        contentDescription = null,
                        tint = if (isNavigationFocused && drawerFocusedItem == DrawerItem.User) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.inverseSurface
                    )
                }
            },
            label = {
                Text(
                    modifier = Modifier.offset(y = (-3).dp),
                    text = userDisplayName,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        )

        LazyColumn(
            modifier = Modifier.focusGroup(),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
        ) {
            items(menuItems.size) { index ->
                val item = menuItems[index]
                val previousFocusRequester = if (index == 0) {
                    userFocusRequester
                } else {
                    focusRequesters.getValue(menuItems[index - 1])
                }
                val nextFocusRequester = if (index == menuItems.lastIndex) {
                    settingsFocusRequester
                } else {
                    focusRequesters.getValue(menuItems[index + 1])
                }
                NavigationRailItem(
                        modifier = Modifier
                            .focusRequester(focusRequesters.getValue(item))
                            .focusProperties {
                                up = previousFocusRequester
                                down = nextFocusRequester
                            }
                            .onPreviewKeyEvent { keyEvent ->
                                if (!keyEvent.isKeyDown()) {
                                    return@onPreviewKeyEvent false
                                }
                                if (index == 0 && keyEvent.isDpadUp()) {
                                    previousFocusRequester.requestFocus()
                                    return@onPreviewKeyEvent true
                                }
                                if (index == menuItems.lastIndex && keyEvent.isDpadDown()) {
                                    nextFocusRequester.requestFocus()
                                    return@onPreviewKeyEvent true
                                }
                                false
                            }
                            // 立即更新focusedItem以反映视觉状态
                            .onFocusChanged {
                                if (it.hasFocus && isNavigationFocused) {
                                    drawerFocusedItem = item
                                }
                            },
                        onClick = {
                            drawerFocusedItem = item
                        },
                        selected = visualSelectedItem == item,
                        colors = NavigationRailItemDefaults.colors(
                            indicatorColor = if (isContentFocused) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.inverseSurface
                        ),
                        icon = {
                            Icon(
                                imageVector = item.displayIcon,
                                contentDescription = null,
                                tint = if (isNavigationFocused && drawerFocusedItem == item) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.inverseSurface
                            )
                        },
                        label = {
                            Text(
                                modifier = Modifier.offset(y = (-3).dp),
                                text = item.displayName,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
            }
        }
        NavigationRailItem(
            modifier = Modifier
                .focusRequester(settingsFocusRequester)
                .focusProperties {
                    up = lastMenuFocusRequester ?: userFocusRequester
                }
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.isKeyDown() && keyEvent.isDpadUp()) {
                        (lastMenuFocusRequester ?: userFocusRequester).requestFocus()
                        return@onPreviewKeyEvent true
                    }
                    false
                }
                .onFocusChanged {
                    if (it.hasFocus && isNavigationFocused) {
                        drawerFocusedItem = DrawerItem.Settings
                    }
                },
            onClick = {
                onOpenSettings()
                drawerFocusedItem = DrawerItem.Settings
            },
            selected = visualSelectedItem == DrawerItem.Settings,
            colors = NavigationRailItemDefaults.colors(
                indicatorColor = if (isContentFocused) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.inverseSurface
            ),
            icon = {
                Icon(
                    imageVector = DrawerItem.Settings.displayIcon,
                    contentDescription = null,
                    tint = if (isNavigationFocused && drawerFocusedItem == DrawerItem.Settings) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.inverseSurface
                )
            },
            label = {
                Text(
                    modifier = Modifier.offset(y = (-3).dp),
                    text = DrawerItem.Settings.displayName,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        )
    }
}

enum class DrawerItem(
    val displayName: String,
    val displayIcon: ImageVector
) {
    User(displayName = "点击登录", displayIcon = Icons.Default.AccountCircle),
    Search(displayName = "搜索", displayIcon = Icons.Default.Search),
    Home(displayName = "首页", displayIcon = Icons.Default.Home),
    UGC(displayName = "UGC", displayIcon = Icons.Default.OndemandVideo),
    PGC(displayName = "PGC", displayIcon = Icons.Default.Movie),
    Live(displayName = "直播", displayIcon = Icons.Default.Videocam),
    Settings(displayName = "设置", displayIcon = Icons.Default.Settings), ;

    val isConfigurable: Boolean
        get() = this in configurableEntries

    val hasContentPanel: Boolean
        get() = this in contentEntries

    companion object {
        val configurableEntries = listOf(Search, Home, UGC, PGC, Live)
        val contentEntries = entries.toList()
        val defaultConfigurableItem = Home
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun DrawerContentPreview() {
    BVTheme {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(180.dp)
        ) {
            NavigationRail(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(72.dp),
                containerColor = MaterialTheme.colorScheme.inverseOnSurface
            ) {
                DrawerContent(
                    focusRequesters = remember {
                        DrawerItem.entries.associateWith { FocusRequester() }
                    }
                )
            }
        }
    }
}