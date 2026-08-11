package dev.aaa1115910.bv.tv.screens.user

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.bv.repository.UserRepository
import dev.aaa1115910.bv.offline.OfflineVideoCacheEntry
import dev.aaa1115910.bv.offline.OfflineVideoCacheService
import dev.aaa1115910.bv.offline.OfflineVideoCacheStatus
import dev.aaa1115910.bv.offline.OfflineVideoCacheTaskState
import dev.aaa1115910.bv.tv.activities.message.InboxActivity
import dev.aaa1115910.bv.tv.activities.user.FavoriteActivity
import dev.aaa1115910.bv.tv.activities.user.FollowingSeasonActivity
import dev.aaa1115910.bv.tv.activities.user.HistoryActivity
import dev.aaa1115910.bv.tv.activities.user.LoginActivity
import dev.aaa1115910.bv.tv.activities.user.OfflineCacheActivity
import dev.aaa1115910.bv.tv.activities.user.ToViewActivity
import dev.aaa1115910.bv.tv.activities.user.UserSwitchActivity
import dev.aaa1115910.bv.tv.activities.video.OfflineVideoPlayerActivity
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.isDpadLeft
import dev.aaa1115910.bv.util.isKeyDown
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin

@Composable
fun UserInfoScreen(
    modifier: Modifier = Modifier,
    initialFocusRequester: FocusRequester? = null,
    autoRequestInitialFocus: Boolean = true,
    onRequestDrawerFocus: (() -> Unit)? = null,
    userViewModel: UserViewModel = koinViewModel(),
    userRepository: UserRepository = getKoin().get(),
    offlineCacheService: OfflineVideoCacheService = getKoin().get(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusRequester = initialFocusRequester ?: remember { FocusRequester() }
    val isLogin = userViewModel.isLogin
    var contentHasFocus by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var logoutInProgress by remember { mutableStateOf(false) }
    var incognitoModeEnabled by remember { mutableStateOf(Prefs.incognitoMode) }

    val activeTasks = offlineCacheService.taskStates.values
        .filter { it.status != OfflineVideoCacheStatus.Idle && it.status != OfflineVideoCacheStatus.Completed }
        .sortedByDescending { it.isActive }
    val completedEntries = offlineCacheService.entries.toList()
    val shelfTasks = activeTasks.take(2)
    val remainingSlots = (2 - shelfTasks.size).coerceAtLeast(0)
    val shelfEntries = completedEntries.take(remainingSlots)

    fun openLogin() {
        context.startActivity(Intent(context, LoginActivity::class.java))
    }

    fun requireLogin() {
        "登录后可使用此功能".toast(context)
    }

    fun updateData() {
        scope.launch { offlineCacheService.refreshEntries() }
        incognitoModeEnabled = Prefs.incognitoMode
        if (isLogin) userViewModel.updateUserInfo(forceUpdate = true)
    }

    BackHandler(enabled = contentHasFocus && onRequestDrawerFocus != null) {
        onRequestDrawerFocus?.invoke()
    }

    LaunchedEffect(isLogin) {
        updateData()
        if (autoRequestInitialFocus) focusRequester.requestFocus()
    }

    DisposableEffect(lifecycleOwner, isLogin) {
        var leftScreen = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> leftScreen = true
                Lifecycle.Event.ON_RESUME -> if (leftScreen) {
                    updateData()
                    leftScreen = false
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .onFocusChanged { contentHasFocus = it.hasFocus },
        contentPadding = PaddingValues(start = 26.dp, top = 22.dp, end = 32.dp, bottom = 34.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            val userInfo = userViewModel.responseData
            ProfileHeader(
                isLogin = isLogin,
                username = userViewModel.username,
                face = userViewModel.face,
                uid = userInfo?.mid ?: 0L,
                level = userInfo?.level ?: 0,
                isSeniorMember = userInfo?.isSeniorMember == 1,
                vipActive = userInfo?.vip?.status == 1,
                vipIconUrl = userInfo?.vip?.label?.imgLabelUriHansStatic
                    .orEmpty()
                    .ifBlank { userInfo?.vip?.label?.path.orEmpty() },
                coins = userInfo?.coins ?: 0f,
                following = userViewModel.statData?.following ?: userInfo?.following ?: 0,
                loginModifier = if (!isLogin) Modifier.focusRequester(focusRequester) else Modifier,
                onLogin = ::openLogin
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(text = "离线缓存", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "${activeTasks.size} 个任务 · ${completedEntries.size} 个已缓存",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        modifier = if (isLogin && shelfTasks.isEmpty() && shelfEntries.isEmpty()) {
                            Modifier.focusRequester(focusRequester)
                        } else Modifier,
                        onClick = { OfflineCacheActivity.actionStart(context) }
                    ) {
                        Text("管理全部")
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (shelfTasks.isEmpty() && shelfEntries.isEmpty()) {
                        EmptyOfflineCard(
                            modifier = Modifier
                                .weight(1f)
                                .returnToDrawerOnLeft(onRequestDrawerFocus),
                            onClick = { OfflineCacheActivity.actionStart(context) }
                        )
                        OfflineSummaryCard(
                            modifier = Modifier.weight(1f),
                            completedCount = 0,
                            activeCount = 0,
                            onClick = { OfflineCacheActivity.actionStart(context) }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        shelfTasks.forEachIndexed { index, task ->
                            OfflineShelfCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (index == 0) {
                                            Modifier.returnToDrawerOnLeft(onRequestDrawerFocus)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .then(
                                        if (isLogin && index == 0) Modifier.focusRequester(focusRequester)
                                        else Modifier
                                    ),
                                cover = task.cover,
                                title = task.partTitle.ifBlank { task.title },
                                subtitle = taskStatusText(task),
                                progress = task.progress.takeIf { task.totalBytes > 0L },
                                completed = false,
                                onClick = { OfflineCacheActivity.actionStart(context) }
                            )
                        }
                        shelfEntries.forEachIndexed { index, entry ->
                            OfflineShelfCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (shelfTasks.isEmpty() && index == 0) {
                                            Modifier.returnToDrawerOnLeft(onRequestDrawerFocus)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .then(
                                        if (isLogin && shelfTasks.isEmpty() && index == 0) {
                                            Modifier.focusRequester(focusRequester)
                                        } else Modifier
                                    ),
                                cover = offlineCacheService.getCachedCoverUri(entry).orEmpty(),
                                title = entry.displayTitle,
                                subtitle = "${entry.qualityText} · ${formatBytes(entry.totalBytes)}",
                                progress = null,
                                completed = true,
                                onClick = {
                                    OfflineVideoPlayerActivity.actionStart(
                                        context = context,
                                        aid = entry.aid,
                                        cid = entry.cid
                                    )
                                }
                            )
                        }
                        OfflineSummaryCard(
                            modifier = Modifier.weight(1f),
                            completedCount = completedEntries.size,
                            activeCount = activeTasks.size,
                            onClick = { OfflineCacheActivity.actionStart(context) }
                        )
                        repeat((2 - shelfTasks.size - shelfEntries.size).coerceAtLeast(0)) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item {
            MyContentSection(
                isLogin = isLogin,
                incognitoModeEnabled = incognitoModeEnabled,
                onRequestDrawerFocus = onRequestDrawerFocus,
                onRequireLogin = ::requireLogin,
                onHistory = { context.startActivity(Intent(context, HistoryActivity::class.java)) },
                onFavorite = { context.startActivity(Intent(context, FavoriteActivity::class.java)) },
                onFollowing = { context.startActivity(Intent(context, FollowingSeasonActivity::class.java)) },
                onLater = { context.startActivity(Intent(context, ToViewActivity::class.java)) },
                onInbox = { InboxActivity.actionStart(context) },
                onAccount = {
                    if (isLogin) context.startActivity(Intent(context, UserSwitchActivity::class.java))
                    else openLogin()
                },
                onToggleIncognito = {
                    val enabled = !Prefs.incognitoMode
                    Prefs.incognitoMode = enabled
                    incognitoModeEnabled = enabled
                    if (enabled) "已开启隐身模式".toast(context)
                    else "已关闭隐身模式".toast(context)
                },
                onLogout = { showLogoutDialog = true }
            )
        }
    }

    if (showLogoutDialog) {
        TvAlertDialog(
            onDismissRequest = { if (!logoutInProgress) showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("退出当前账号后，本机离线缓存仍会保留。") },
            confirmButton = {
                Button(
                    enabled = !logoutInProgress,
                    onClick = {
                        scope.launch {
                            logoutInProgress = true
                            runCatching {
                                withContext(Dispatchers.IO) { userRepository.logoutFromServer() }
                            }.onSuccess {
                                showLogoutDialog = false
                                "已退出登录".toast(context)
                            }.onFailure {
                                "退出失败：${it.localizedMessage ?: "未知错误"}".toast(context)
                            }
                            logoutInProgress = false
                        }
                    }
                ) { Text(if (logoutInProgress) "正在退出" else "确认退出") }
            },
            dismissButton = {
                OutlinedButton(
                    enabled = !logoutInProgress,
                    onClick = { showLogoutDialog = false }
                ) { Text("取消") }
            }
        )
    }
}

@Composable
private fun ProfileHeader(
    isLogin: Boolean,
    username: String,
    face: String,
    uid: Long,
    level: Int,
    isSeniorMember: Boolean,
    vipActive: Boolean,
    vipIconUrl: String,
    coins: Float,
    following: Int,
    loginModifier: Modifier,
    onLogin: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (isLogin && face.isNotBlank()) {
                AsyncImage(
                    model = face,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(
            modifier = Modifier.widthIn(max = 640.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isLogin) username.ifBlank { "已登录用户" } else "登录 BV",
                    modifier = Modifier.widthIn(max = 320.dp),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isLogin && vipActive && vipIconUrl.isNotBlank()) {
                    AsyncImage(
                        model = vipIconUrl,
                        contentDescription = "大会员",
                        modifier = Modifier
                            .height(20.dp)
                            .widthIn(max = 170.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                if (isLogin) {
                    AsyncImage(
                        model = officialLevelIconUrl(level, isSeniorMember),
                        contentDescription = "等级 $level",
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            if (isLogin) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "UID: $uid",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "硬币: %.1f".format(coins),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "已关注 $following",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "登录后同步收藏、追番、历史记录和多设备进度",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        if (!isLogin) {
            Button(modifier = loginModifier, onClick = onLogin) {
                Text("立即登录")
            }
        }
    }
}

private fun officialLevelIconUrl(level: Int, isSeniorMember: Boolean): String {
    val iconName = if (isSeniorMember && level >= 6) "level_h" else "level_${level.coerceIn(0, 6)}"
    return "https://i0.hdslb.com/bfs/seed/jinkela/short/webui/user-profile/img/$iconName.svg"
}

@Composable
private fun OfflineShelfCard(
    modifier: Modifier,
    cover: String,
    title: String,
    subtitle: String,
    progress: Float?,
    completed: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.height(104.dp),
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f),
            focusedContainerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.large),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(142.dp)
                    .height(86.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (cover.isNotBlank()) {
                    AsyncImage(
                        model = cover,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Rounded.Download, null, modifier = Modifier.size(32.dp))
                }
                Icon(
                    imageVector = if (completed) Icons.Rounded.PlayArrow else Icons.Rounded.Download,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp),
                    tint = Color.White
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = title.ifBlank { "离线视频" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                progress?.let {
                    LinearProgressIndicator(
                        progress = { it },
                        modifier = Modifier.fillMaxWidth(),
                        gapSize = 0.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyOfflineCard(modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(104.dp),
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            focusedContainerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.large)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Rounded.Download, null, modifier = Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("还没有离线视频", style = MaterialTheme.typography.titleMedium)
                Text("在视频详情或播放页选择缓存", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun OfflineSummaryCard(
    modifier: Modifier,
    completedCount: Int,
    activeCount: Int,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.height(104.dp),
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f),
            focusedContainerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.large)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text("全部缓存", style = MaterialTheme.typography.titleMedium)
                Text("$completedCount 已完成 · $activeCount 进行中", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MyContentSection(
    isLogin: Boolean,
    incognitoModeEnabled: Boolean,
    onRequestDrawerFocus: (() -> Unit)?,
    onRequireLogin: () -> Unit,
    onHistory: () -> Unit,
    onFavorite: () -> Unit,
    onFollowing: () -> Unit,
    onLater: () -> Unit,
    onInbox: () -> Unit,
    onAccount: () -> Unit,
    onToggleIncognito: () -> Unit,
    onLogout: () -> Unit,
) {
    val lockedClick = onRequireLogin
    val cards = buildList {
        add(MyContentItem("历史记录", Icons.Rounded.History, isLogin, if (isLogin) onHistory else lockedClick))
        add(MyContentItem("我的收藏", Icons.Rounded.FavoriteBorder, isLogin, if (isLogin) onFavorite else lockedClick))
        add(MyContentItem("正在追", Icons.Rounded.Subscriptions, isLogin, if (isLogin) onFollowing else lockedClick))
        add(MyContentItem("稍后再看", Icons.Rounded.WatchLater, isLogin, if (isLogin) onLater else lockedClick))
        add(MyContentItem("消息", Icons.Rounded.MailOutline, isLogin, if (isLogin) onInbox else lockedClick))
        add(
            MyContentItem(
                title = "隐身模式",
                icon = Icons.Rounded.VisibilityOff,
                available = true,
                onClick = onToggleIncognito,
                selected = incognitoModeEnabled
            )
        )
        add(MyContentItem(if (isLogin) "账号管理" else "登录账号", Icons.Rounded.ManageAccounts, true, onAccount))
        if (isLogin) add(MyContentItem("退出登录", Icons.Rounded.Logout, true, onLogout, danger = true))
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("我的内容", style = MaterialTheme.typography.titleLarge)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            cards.forEachIndexed { index, item ->
                MyContentCard(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (index == 0) {
                                Modifier.returnToDrawerOnLeft(onRequestDrawerFocus)
                            } else {
                                Modifier
                            }
                        ),
                    item = item
                )
            }
        }
    }
}

private data class MyContentItem(
    val title: String,
    val icon: ImageVector,
    val available: Boolean,
    val onClick: () -> Unit,
    val danger: Boolean = false,
    val selected: Boolean = false,
)

@Composable
private fun MyContentCard(modifier: Modifier, item: MyContentItem) {
    val baseColor = when {
        item.danger -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.62f)
        item.selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
        item.available -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
    }
    Surface(
        modifier = modifier
            .height(100.dp)
            .alpha(if (item.available) 1f else 0.58f),
        onClick = item.onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = baseColor,
            focusedContainerColor = if (item.danger) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.large)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (item.available) item.icon else Icons.Rounded.Lock,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = when {
                    item.danger -> MaterialTheme.colorScheme.error
                    item.available -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun Modifier.returnToDrawerOnLeft(
    onRequestDrawerFocus: (() -> Unit)?,
): Modifier = if (onRequestDrawerFocus == null) {
    this
} else {
    onKeyEvent { event ->
        if (event.isKeyDown() && event.isDpadLeft()) {
            onRequestDrawerFocus()
            true
        } else {
            false
        }
    }
}

private fun taskStatusText(task: OfflineVideoCacheTaskState): String = when (task.status) {
    OfflineVideoCacheStatus.Queued -> "等待缓存"
    OfflineVideoCacheStatus.Fetching -> "正在准备"
    OfflineVideoCacheStatus.DownloadingVideo,
    OfflineVideoCacheStatus.DownloadingAudio,
    OfflineVideoCacheStatus.DownloadingDanmaku -> "缓存中 ${(task.progress * 100).toInt()}%"
    OfflineVideoCacheStatus.Paused -> "已暂停"
    OfflineVideoCacheStatus.Failed -> "缓存失败"
    OfflineVideoCacheStatus.Completed -> "已缓存"
    OfflineVideoCacheStatus.Idle -> ""
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return if (value >= 10 || index == 0) "${value.toInt()} ${units[index]}"
    else "%.1f %s".format(value, units[index])
}
