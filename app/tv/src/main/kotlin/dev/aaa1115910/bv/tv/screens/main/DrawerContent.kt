package dev.aaa1115910.bv.tv.screens.main

import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.tv.util.drawerNavItemsFlow
import dev.aaa1115910.bv.tv.util.LocalTvUiPerformanceProfile
import dev.aaa1115910.bv.tv.util.parseDrawerItemsOrder
import dev.aaa1115910.bv.tv.util.TvUiPerformanceProfile
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.ifElse
import dev.aaa1115910.bv.util.isDpadDown
import dev.aaa1115910.bv.util.isDpadRight
import dev.aaa1115910.bv.util.isDpadUp
import dev.aaa1115910.bv.util.isKeyDown
import dev.aaa1115910.bv.util.isKeyUp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class DrawerNavigationState(initialItem: DrawerItem) {
    var focusedItem: DrawerItem = initialItem
    var hasFocus: Boolean = false
    var isDirectionKeyHeld: Boolean = false
    var isRepeatingDirection: Boolean = false
    var lastFocusChangedAtMillis: Long = 0L
    var commitJob: Job? = null
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
    performanceProfile: TvUiPerformanceProfile = LocalTvUiPerformanceProfile.current,
    onDrawerItemChanged: (DrawerItem) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onFocusToContent: (DrawerItem) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val drawerAnimationMillis = performanceProfile.drawerAnimationMillis
    val hasUserAvatar = isLogin && avatar.isNotBlank()
    val userDisplayName = if (isLogin) username.ifBlank { "用户" } else DrawerItem.User.displayName

    val navigationState = remember { DrawerNavigationState(currentDrawerItem) }
    var isNavigationFocused by remember { mutableStateOf(false) }
    val isContentFocused = !isNavigationFocused

    val menuItems by drawerNavItemsFlow.collectAsState(
        initial = remember { parseDrawerItemsOrder(Prefs.drawerItemsOrder) }
    )

    fun cancelPendingDrawerCommit() {
        navigationState.commitJob?.cancel()
        navigationState.commitJob = null
    }

    fun commitDrawerItem(item: DrawerItem) {
        if (!item.hasContentPanel || item == currentDrawerItem) return
        onDrawerItemChanged(item)
    }

    fun remainingDrawerAnimationMillis(): Long {
        val elapsed = SystemClock.uptimeMillis() - navigationState.lastFocusChangedAtMillis
        return (drawerAnimationMillis - elapsed).coerceAtLeast(0L)
    }

    fun scheduleDrawerCommit(
        item: DrawerItem,
        afterCommit: (() -> Unit)? = null,
    ) {
        cancelPendingDrawerCommit()
        if (!item.hasContentPanel) return

        val commitDelay = if (navigationState.isRepeatingDirection) {
            performanceProfile.drawerRepeatSettleMillis
        } else {
            remainingDrawerAnimationMillis()
        }

        navigationState.commitJob = scope.launch {
            delay(commitDelay)
            if (!navigationState.hasFocus || navigationState.focusedItem != item) return@launch
            commitDrawerItem(item)
            afterCommit?.invoke()
            navigationState.commitJob = null
        }
    }

    fun onDrawerItemFocused(item: DrawerItem) {
        navigationState.focusedItem = item
        navigationState.lastFocusChangedAtMillis = SystemClock.uptimeMillis()
        if (navigationState.hasFocus) {
            when {
                // 首次 KeyDown 时无法预知用户会不会长按，因此等待 KeyUp 再提交。
                navigationState.isDirectionKeyHeld &&
                        !navigationState.isRepeatingDirection -> cancelPendingDrawerCommit()

                // 长按期间使用尾触发兜底；后续 repeat/focus 会持续取消旧任务。
                else -> scheduleDrawerCommit(item)
            }
        }
    }

    fun focusToContent() {
        val targetItem = navigationState.focusedItem
        navigationState.isRepeatingDirection = false
        scheduleDrawerCommit(targetItem) {
            onFocusToContent(targetItem)
        }
    }

    LaunchedEffect(currentDrawerItem, isNavigationFocused) {
        if (!isNavigationFocused) {
            navigationState.focusedItem = currentDrawerItem
        }
    }

    LaunchedEffect(menuItems, currentDrawerItem, isNavigationFocused) {
        val fallbackItem = menuItems.firstOrNull() ?: DrawerItem.defaultConfigurableItem
        if (isContentFocused) {
            navigationState.focusedItem = currentDrawerItem
            return@LaunchedEffect
        }

        if (navigationState.focusedItem.isConfigurable && navigationState.focusedItem !in menuItems) {
            navigationState.focusedItem = fallbackItem
            focusRequesters[fallbackItem]?.requestFocus()
            scheduleDrawerCommit(fallbackItem)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cancelPendingDrawerCommit()
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
                if (keyEvent.isDpadUp() || keyEvent.isDpadDown()) {
                    when {
                        keyEvent.isKeyDown() -> {
                            navigationState.isDirectionKeyHeld = true
                            navigationState.isRepeatingDirection =
                                keyEvent.nativeKeyEvent.repeatCount > 0
                            cancelPendingDrawerCommit()
                        }

                        keyEvent.isKeyUp() -> {
                            navigationState.isDirectionKeyHeld = false
                            navigationState.isRepeatingDirection = false
                            scheduleDrawerCommit(navigationState.focusedItem)
                        }
                    }
                }
                if (keyEvent.isDpadRight()) {
                    if (keyEvent.isKeyDown()) {
                        focusToContent()
                        return@onPreviewKeyEvent true
                    }
                }
                false
            }
            .onFocusChanged {
                navigationState.hasFocus = it.hasFocus
                isNavigationFocused = it.hasFocus
                if (it.hasFocus) {
                    scheduleDrawerCommit(navigationState.focusedItem)
                } else {
                    cancelPendingDrawerCommit()
                }
            },
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        DrawerUserNavigationItem(
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
                },
            hasUserAvatar = hasUserAvatar,
            avatar = avatar,
            userDisplayName = userDisplayName,
            isNavigationFocused = isNavigationFocused,
            selectedWhenContentFocused = currentDrawerItem == DrawerItem.User,
            animationMillis = drawerAnimationMillis,
            onFocused = { onDrawerItemFocused(DrawerItem.User) },
            onClick = { onDrawerItemFocused(DrawerItem.User) }
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
                DrawerNavigationItem(
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
                        },
                    item = item,
                    isNavigationFocused = isNavigationFocused,
                    selectedWhenContentFocused = currentDrawerItem == item,
                    indicatorColorWhenContentFocused = MaterialTheme.colorScheme.surfaceVariant,
                    animationMillis = drawerAnimationMillis,
                    onFocused = { onDrawerItemFocused(item) },
                    onClick = { onDrawerItemFocused(item) }
                )
            }
        }
        DrawerNavigationItem(
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
                },
            item = DrawerItem.Settings,
            isNavigationFocused = isNavigationFocused,
            selectedWhenContentFocused = currentDrawerItem == DrawerItem.Settings,
            indicatorColorWhenContentFocused = MaterialTheme.colorScheme.surfaceVariant,
            animationMillis = drawerAnimationMillis,
            onFocused = { onDrawerItemFocused(DrawerItem.Settings) },
            onClick = {
                onOpenSettings()
                onDrawerItemFocused(DrawerItem.Settings)
            }
        )
    }
}

@Composable
private fun DrawerUserNavigationItem(
    modifier: Modifier = Modifier,
    hasUserAvatar: Boolean,
    avatar: String,
    userDisplayName: String,
    isNavigationFocused: Boolean,
    selectedWhenContentFocused: Boolean,
    animationMillis: Int,
    onFocused: () -> Unit,
    onClick: () -> Unit
) {
    var hasFocus by remember { mutableStateOf(false) }
    val focusedInNavigation = isNavigationFocused && hasFocus

    NavigationRailItem(
        modifier = modifier.onFocusChanged {
            hasFocus = it.hasFocus
            if (it.hasFocus) onFocused()
        },
        onClick = onClick,
        selected = if (isNavigationFocused) hasFocus else selectedWhenContentFocused,
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
                            focusedInNavigation,
                            Modifier
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.inverseSurface,
                                    shape = CircleShape
                                )
                        )
                        .padding(3.dp)
                        .clip(CircleShape),
                    model = avatar,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds
                )
            } else {
                val userIconTint by animateColorAsState(
                    targetValue = if (focusedInNavigation) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.inverseSurface
                    },
                    animationSpec = tween(animationMillis),
                    label = "user icon tint"
                )
                Icon(
                    modifier = Modifier
                        .size(46.dp)
                        .ifElse(
                            focusedInNavigation,
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
                    tint = userIconTint
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
}

@Composable
private fun DrawerNavigationItem(
    modifier: Modifier = Modifier,
    item: DrawerItem,
    isNavigationFocused: Boolean,
    selectedWhenContentFocused: Boolean,
    indicatorColorWhenContentFocused: Color,
    animationMillis: Int,
    onFocused: () -> Unit,
    onClick: () -> Unit
) {
    var hasFocus by remember { mutableStateOf(false) }
    val focusedInNavigation = isNavigationFocused && hasFocus
    val iconTint by animateColorAsState(
        targetValue = if (focusedInNavigation) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.inverseSurface
        },
        animationSpec = tween(animationMillis),
        label = "drawer icon tint"
    )

    NavigationRailItem(
        modifier = modifier.onFocusChanged {
            hasFocus = it.hasFocus
            if (it.hasFocus) onFocused()
        },
        onClick = onClick,
        selected = if (isNavigationFocused) hasFocus else selectedWhenContentFocused,
        colors = NavigationRailItemDefaults.colors(
            indicatorColor = if (isNavigationFocused) {
                MaterialTheme.colorScheme.inverseSurface
            } else {
                indicatorColorWhenContentFocused
            }
        ),
        icon = {
            Icon(
                imageVector = item.displayIcon,
                contentDescription = null,
                tint = iconTint
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
