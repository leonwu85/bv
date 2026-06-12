package dev.aaa1115910.bv.mobile.screen.message

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.user.FollowedUser
import dev.aaa1115910.bv.mobile.activities.UserSpaceActivity
import dev.aaa1115910.bv.viewmodel.message.ContactViewModel
import org.koin.compose.koinInject
import dev.aaa1115910.biliapi.repositories.UserRepository as RemoteUserRepository
import dev.aaa1115910.bv.repository.UserRepository as AccountRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val refreshState = rememberPullToRefreshState()
    val remoteUserRepository: RemoteUserRepository = koinInject()
    val accountRepository: AccountRepository = koinInject()
    val viewModel: ContactViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ContactViewModel(remoteUserRepository, accountRepository) as T
        }
    )
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("我的关注", "我的粉丝")
    val loading = if (selectedTab == 0) viewModel.loadingFollowing else viewModel.loadingFans

    BackHandler {
        (context as Activity).finish()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(text = "通讯录") },
                    navigationIcon = {
                        IconButton(onClick = { (context as Activity).finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(text = title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            state = refreshState,
            isRefreshing = loading,
            onRefresh = {
                if (selectedTab == 0) {
                    viewModel.refreshFollowing()
                } else {
                    viewModel.refreshFans()
                }
            }
        ) {
            if (selectedTab == 0) {
                ContactListContent(
                    users = viewModel.followedUsers,
                    loading = viewModel.loadingFollowing,
                    errorMessage = viewModel.followingError,
                    emptyText = "暂无关注",
                    onRetry = viewModel::refreshFollowing
                )
            } else {
                ContactListContent(
                    users = viewModel.fanUsers,
                    loading = viewModel.loadingFans,
                    errorMessage = viewModel.fanError,
                    emptyText = "暂无粉丝",
                    onRetry = viewModel::refreshFans
                )
            }
        }
    }
}

@Composable
private fun ContactListContent(
    users: List<FollowedUser>,
    loading: Boolean,
    errorMessage: String?,
    emptyText: String,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    when {
        users.isEmpty() && loading -> ContactCenterContent(text = "正在加载", loading = true)
        users.isEmpty() && errorMessage != null -> ContactCenterContent(
            text = errorMessage,
            action = "重试",
            onAction = onRetry
        )
        users.isEmpty() -> ContactCenterContent(text = emptyText)
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(items = users, key = { it.mid }) { user ->
                ContactUserItem(
                    user = user,
                    onClick = {
                        UserSpaceActivity.actionStart(
                            context = context,
                            mid = user.mid,
                            name = user.name
                        )
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )
            }
            item {
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun ContactUserItem(
    user: FollowedUser,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = user.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = user.sign,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (user.avatar.isBlank()) {
                    Icon(
                        imageVector = Icons.Rounded.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    AsyncImage(
                        modifier = Modifier.fillMaxSize(),
                        model = user.avatar,
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    )
}

@Composable
private fun ContactCenterContent(
    text: String,
    loading: Boolean = false,
    action: String? = null,
    onAction: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
        } else {
            Icon(
                modifier = Modifier.size(48.dp),
                imageVector = Icons.Rounded.Groups,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.size(14.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        if (action != null) {
            TextButton(onClick = onAction) {
                Text(text = action)
            }
        }
    }
}
