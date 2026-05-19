package dev.aaa1115910.bv.mobile.screen.home

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SupervisorAccount
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.FavoriteFolderMetadata
import dev.aaa1115910.biliapi.http.entity.user.MyInfoData
import dev.aaa1115910.biliapi.http.entity.user.UserNavStatData
import dev.aaa1115910.bv.entity.db.UserDB
import dev.aaa1115910.bv.mobile.activities.FavoriteActivity
import dev.aaa1115910.bv.mobile.activities.FollowingSeasonActivity
import dev.aaa1115910.bv.mobile.activities.FollowingUserActivity
import dev.aaa1115910.bv.mobile.activities.HistoryActivity
import dev.aaa1115910.bv.mobile.activities.LoginActivity
import dev.aaa1115910.bv.mobile.activities.SettingsActivity
import dev.aaa1115910.bv.mobile.activities.ToViewActivity
import dev.aaa1115910.bv.mobile.activities.UserSpaceActivity
import dev.aaa1115910.bv.viewmodel.UserSwitchViewModel
import dev.aaa1115910.bv.viewmodel.UserViewModel
import dev.aaa1115910.bv.viewmodel.user.FavoriteViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

@Composable
fun MineScreen(
    modifier: Modifier = Modifier,
    windowSize: WindowWidthSizeClass,
    userViewModel: UserViewModel = koinViewModel(),
    userSwitchViewModel: UserSwitchViewModel = koinViewModel(),
    favoriteViewModel: FavoriteViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentUser = userSwitchViewModel.currentUser.takeIf { it.id != -1 }
    val userInfo = userViewModel.responseData
    val statData = userViewModel.statData

    fun refreshMine() {
        scope.launch(Dispatchers.IO) {
            userSwitchViewModel.updateUserDbList()
        }
        userViewModel.updateUserInfo(forceUpdate = true)
        if (userViewModel.isLogin) {
            favoriteViewModel.updateFoldersInfo()
        }
    }

    fun openLogin() {
        context.startActivity(Intent(context, LoginActivity::class.java))
    }

    fun openSelfSpace() {
        val uid = userInfo?.mid ?: currentUser?.uid ?: 0L
        if (uid <= 0L) {
            openLogin()
        } else {
            UserSpaceActivity.actionStart(
                context = context,
                mid = uid,
                name = userInfo?.name ?: currentUser?.username.orEmpty()
            )
        }
    }

    fun switchUser(user: UserDB) {
        scope.launch {
            withContext(Dispatchers.IO) {
                userSwitchViewModel.switchUser(user)
                userSwitchViewModel.updateUserDbList()
            }
            favoriteViewModel.clearData()
            userViewModel.updateUserInfo(forceUpdate = true)
            if (userViewModel.isLogin) {
                favoriteViewModel.updateFoldersInfo()
            }
        }
    }

    fun deleteUser(user: UserDB) {
        scope.launch {
            withContext(Dispatchers.IO) {
                userSwitchViewModel.deleteUser(user)
            }
            favoriteViewModel.clearData()
            if (userViewModel.isLogin) {
                favoriteViewModel.updateFoldersInfo()
                userViewModel.updateUserInfo(forceUpdate = true)
            } else {
                userViewModel.clearUserInfo()
            }
        }
    }

    LaunchedEffect(userViewModel.isLogin) {
        refreshMine()
    }

    DisposableEffect(lifecycleOwner) {
        var leaveFromThisPage = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                leaveFromThisPage = true
            } else if (event == Lifecycle.Event.ON_RESUME) {
                if (leaveFromThisPage) refreshMine()
                leaveFromThisPage = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(
                    start = if (windowSize == WindowWidthSizeClass.Compact) 16.dp else 24.dp,
                    top = 10.dp,
                    end = if (windowSize == WindowWidthSizeClass.Compact) 16.dp else 24.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    MineHeaderActions(
                        onBack = onBack,
                        onRefresh = ::refreshMine,
                        onAddUser = ::openLogin,
                        onOpenSettings = {
                            context.startActivity(Intent(context, SettingsActivity::class.java))
                        }
                    )
                }

                item {
                    if (userViewModel.isLogin || currentUser != null) {
                        MineUserCard(
                            currentUser = currentUser,
                            userInfo = userInfo,
                            statData = statData,
                            userList = userSwitchViewModel.userDbList,
                            onOpenSelfSpace = ::openSelfSpace,
                            onSwitchUser = ::switchUser,
                            onAddUser = ::openLogin,
                            onDeleteUser = ::deleteUser,
                            onOpenFollowingUser = {
                                context.startActivity(Intent(context, FollowingUserActivity::class.java))
                            }
                        )
                    } else {
                        MineGuestCard(onLogin = ::openLogin)
                    }
                }

                item {
                    MineQuickActions(
                        enabled = userViewModel.isLogin,
                        onOpenHistory = {
                            context.startActivity(Intent(context, HistoryActivity::class.java))
                        },
                        onOpenFollowingSeason = {
                            context.startActivity(Intent(context, FollowingSeasonActivity::class.java))
                        },
                        onOpenToView = {
                            context.startActivity(Intent(context, ToViewActivity::class.java))
                        },
                        onOpenFavorite = {
                            context.startActivity(Intent(context, FavoriteActivity::class.java))
                        },
                        onOpenLogin = ::openLogin
                    )
                }

                item {
                    val uid = userInfo?.mid ?: currentUser?.uid ?: 0L
                    MineFavoritePreview(
                        isLogin = userViewModel.isLogin,
                        folders = favoriteViewModel.favoriteFolderMetadataList.filter { it.mid == uid },
                        loading = favoriteViewModel.updatingFolders,
                        onRefresh = { favoriteViewModel.updateFoldersInfo() },
                        onOpenFavorite = {
                            context.startActivity(Intent(context, FavoriteActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MineHeaderActions(
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onAddUser: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回"
            )
        }
        Text(
            text = "我的",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onRefresh) {
            Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "刷新")
        }
        IconButton(onClick = onAddUser) {
            Icon(imageVector = Icons.Outlined.PersonAdd, contentDescription = "添加账号")
        }
        IconButton(onClick = onOpenSettings) {
            Icon(imageVector = Icons.Rounded.Settings, contentDescription = "设置")
        }
    }
}

@Composable
private fun MineGuestCard(
    modifier: Modifier = Modifier,
    onLogin: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                modifier = Modifier.size(72.dp),
                imageVector = Icons.Rounded.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "点击登录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "登录后查看收藏、历史记录、关注和稍后再看",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onLogin) {
                Text(text = "登录")
            }
        }
    }
}

@Composable
private fun MineUserCard(
    modifier: Modifier = Modifier,
    currentUser: UserDB?,
    userInfo: MyInfoData?,
    statData: UserNavStatData?,
    userList: List<UserDB>,
    onOpenSelfSpace: () -> Unit,
    onSwitchUser: (UserDB) -> Unit,
    onAddUser: () -> Unit,
    onDeleteUser: (UserDB) -> Unit,
    onOpenFollowingUser: () -> Unit
) {
    var expandUserManager by remember { mutableStateOf(false) }
    val username = userInfo?.name ?: currentUser?.username ?: "加载中"
    val avatar = userInfo?.face ?: currentUser?.avatar.orEmpty()
    val uid = userInfo?.mid ?: currentUser?.uid ?: 0L
    val levelInfo = userInfo?.levelExp
    val currentExp = levelInfo?.currentExp ?: 0
    val currentMin = levelInfo?.currentMin ?: 0
    val nextExp = levelInfo?.nextExp ?: 0
    val progress = when {
        nextExp <= currentMin -> 1f
        else -> ((currentExp - currentMin).toFloat() / (nextExp - currentMin).toFloat()).coerceIn(0f, 1f)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .clickable(onClick = onOpenSelfSpace),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                UserAvatar(
                    modifier = Modifier.size(64.dp),
                    avatar = avatar
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = username,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        LevelBadge(level = userInfo?.level ?: levelInfo?.currentLevel ?: 0)
                    }
                    Text(
                        text = if (uid > 0) "UID $uid" else "UID -",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = "硬币 ${formatCoins(userInfo?.coins)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                IconButton(onClick = { expandUserManager = !expandUserManager }) {
                    Icon(
                        imageVector = if (expandUserManager) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "账号管理"
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (nextExp > 0) "$currentExp/$nextExp" else "已满级",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = { progress },
                    gapSize = 0.dp
                )
            }

            UserStatsRow(
                dynamicCount = statData?.dynamicCount,
                following = statData?.following ?: userInfo?.following,
                follower = statData?.follower ?: userInfo?.follower,
                onOpenDynamic = onOpenSelfSpace,
                onOpenFollowing = onOpenFollowingUser,
                onOpenFollower = onOpenSelfSpace
            )

            AnimatedVisibility(
                visible = expandUserManager,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                AccountManager(
                    currentUser = currentUser,
                    userList = userList,
                    onSwitchUser = {
                        expandUserManager = false
                        onSwitchUser(it)
                    },
                    onAddUser = onAddUser,
                    onDeleteUser = {
                        expandUserManager = false
                        onDeleteUser(it)
                    }
                )
            }
        }
    }
}

@Composable
private fun UserAvatar(
    modifier: Modifier = Modifier,
    avatar: String
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (avatar.isBlank()) {
            Icon(
                imageVector = Icons.Rounded.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = avatar,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun LevelBadge(level: Int) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            text = "Lv$level",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun UserStatsRow(
    dynamicCount: Int?,
    following: Int?,
    follower: Int?,
    onOpenDynamic: () -> Unit,
    onOpenFollowing: () -> Unit,
    onOpenFollower: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            modifier = Modifier.weight(1f),
            count = dynamicCount,
            label = "动态",
            onClick = onOpenDynamic
        )
        StatItem(
            modifier = Modifier.weight(1f),
            count = following,
            label = "关注",
            onClick = onOpenFollowing
        )
        StatItem(
            modifier = Modifier.weight(1f),
            count = follower,
            label = "粉丝",
            onClick = onOpenFollower
        )
    }
}

@Composable
private fun StatItem(
    modifier: Modifier = Modifier,
    count: Int?,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = formatCount(count),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun AccountManager(
    modifier: Modifier = Modifier,
    currentUser: UserDB?,
    userList: List<UserDB>,
    onSwitchUser: (UserDB) -> Unit,
    onAddUser: () -> Unit,
    onDeleteUser: (UserDB) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.large
            )
            .padding(vertical = 6.dp)
    ) {
        userList
            .filter { it != currentUser }
            .forEach { user ->
                AccountRow(
                    user = user,
                    trailingIcon = null,
                    label = "切换",
                    onClick = { onSwitchUser(user) }
                )
            }
        AccountActionRow(
            icon = Icons.Outlined.PersonAdd,
            text = "添加其他账号",
            onClick = onAddUser
        )
        if (currentUser != null) {
            AccountActionRow(
                icon = Icons.Outlined.PersonRemove,
                text = "移除此设备上的账号",
                onClick = { onDeleteUser(currentUser) }
            )
        }
    }
}

@Composable
private fun AccountRow(
    user: UserDB,
    trailingIcon: ImageVector?,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        UserAvatar(
            modifier = Modifier.size(36.dp),
            avatar = user.avatar
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.username,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = user.uid.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (trailingIcon != null) {
            Icon(imageVector = trailingIcon, contentDescription = null)
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AccountActionRow(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            imageVector = icon,
            contentDescription = null
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun MineQuickActions(
    enabled: Boolean,
    onOpenHistory: () -> Unit,
    onOpenFollowingSeason: () -> Unit,
    onOpenToView: () -> Unit,
    onOpenFavorite: () -> Unit,
    onOpenLogin: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.History,
                title = "观看记录",
                onClick = if (enabled) onOpenHistory else onOpenLogin
            )
            QuickActionItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.SupervisorAccount,
                title = "我的追番",
                onClick = if (enabled) onOpenFollowingSeason else onOpenLogin
            )
            QuickActionItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.WatchLater,
                title = "稍后再看",
                onClick = if (enabled) onOpenToView else onOpenLogin
            )
            QuickActionItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Favorite,
                title = "我的收藏",
                onClick = if (enabled) onOpenFavorite else onOpenLogin
            )
        }
    }
}

@Composable
private fun QuickActionItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MineFavoritePreview(
    isLogin: Boolean,
    folders: List<FavoriteFolderMetadata>,
    loading: Boolean,
    onRefresh: () -> Unit,
    onOpenFavorite: () -> Unit
) {
    if (!isLogin) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "我的收藏",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (folders.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = folders.size.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onRefresh) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "刷新收藏")
                    }
                }
                TextButton(onClick = onOpenFavorite) {
                    Text(text = "全部")
                }
            }

            if (folders.isEmpty()) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                    text = if (loading) "收藏夹加载中" else "暂无收藏夹",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(folders.take(12), key = { it.id }) { folder ->
                        FavoriteFolderCard(
                            folder = folder,
                            onClick = onOpenFavorite
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteFolderCard(
    folder: FavoriteFolderMetadata,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(132.dp)
            .height(148.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (folder.cover.isNullOrBlank()) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                } else {
                    AsyncImage(
                        modifier = Modifier.fillMaxSize(),
                        model = folder.cover,
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = folder.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${folder.mediaCount} 个内容",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

private fun formatCount(value: Int?): String {
    if (value == null || value < 0) return "-"
    return when {
        value >= 100_000_000 -> "${value / 100_000_000}亿"
        value >= 10_000 -> "${value / 10_000}万"
        else -> value.toString()
    }
}

private fun formatCoins(value: Float?): String {
    return value?.let {
        if (it % 1f == 0f) it.toInt().toString() else "%.1f".format(it)
    } ?: "-"
}
