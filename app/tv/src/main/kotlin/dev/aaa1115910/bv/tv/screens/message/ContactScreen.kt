package dev.aaa1115910.bv.tv.screens.message

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.aaa1115910.bv.tv.util.requireTvActivity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.user.FollowedUser
import dev.aaa1115910.bv.tv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.viewmodel.message.ContactViewModel
import org.koin.compose.koinInject
import dev.aaa1115910.biliapi.repositories.UserRepository as RemoteUserRepository
import dev.aaa1115910.bv.repository.UserRepository as AccountRepository

@Composable
fun ContactScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = requireTvActivity()
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
    val isFollowingTab = selectedTab == 0
    val loading = if (isFollowingTab) viewModel.loadingFollowing else viewModel.loadingFans
    val users = if (isFollowingTab) viewModel.followedUsers else viewModel.fanUsers
    val error = if (isFollowingTab) viewModel.followingError else viewModel.fanError
    val emptyText = if (isFollowingTab) "暂无关注" else "暂无粉丝"
    val refresh = if (isFollowingTab) viewModel::refreshFollowing else viewModel::refreshFans

    BackHandler {
        activity.finish()
    }

    androidx.compose.material3.Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TvMessageTopBar(title = "通讯录") {
                Button(onClick = { selectedTab = 0 }) {
                    Text(text = "我的关注")
                }
                Button(onClick = { selectedTab = 1 }) {
                    Text(text = "我的粉丝")
                }
                Button(onClick = refresh) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text(text = "刷新")
                }
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        when {
            users.isEmpty() && loading -> TvMessageCenterContent(
                modifier = Modifier.padding(innerPadding),
                text = "正在加载",
                icon = Icons.Rounded.Groups,
                loading = true
            )

            users.isEmpty() && error != null -> TvMessageCenterContent(
                modifier = Modifier.padding(innerPadding),
                text = error,
                icon = Icons.Rounded.Groups,
                action = "重试",
                onAction = refresh
            )

            users.isEmpty() -> TvMessageCenterContent(
                modifier = Modifier.padding(innerPadding),
                text = emptyText,
                icon = Icons.Rounded.Groups
            )

            else -> ContactList(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                users = users,
                onOpenUser = { user ->
                    UpInfoActivity.actionStart(
                        context = context,
                        mid = user.mid,
                        name = user.name,
                        face = user.avatar
                    )
                }
            )
        }
    }
}

@Composable
private fun ContactList(
    modifier: Modifier = Modifier,
    users: List<FollowedUser>,
    onOpenUser: (FollowedUser) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 32.dp, end = 32.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items = users, key = { it.mid }) { user ->
            ContactUserRow(
                user = user,
                onClick = { onOpenUser(user) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f))
        }
    }
}

@Composable
private fun ContactUserRow(
    user: FollowedUser,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        colors = tvMessageClickableSurfaceColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.large),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TvMessageAvatar(url = user.avatar)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = user.sign.ifBlank { "这个人还没有签名" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current.copy(alpha = 0.72f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
