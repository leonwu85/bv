package dev.aaa1115910.bv.mobile.screen.home

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.mobile.activities.FavoriteActivity
import dev.aaa1115910.bv.mobile.activities.FollowingSeasonActivity
import dev.aaa1115910.bv.mobile.activities.FollowingUserActivity
import dev.aaa1115910.bv.mobile.activities.HistoryActivity
import dev.aaa1115910.bv.mobile.activities.LoginActivity
import dev.aaa1115910.bv.mobile.activities.SettingsActivity
import dev.aaa1115910.bv.mobile.activities.ToViewActivity
import dev.aaa1115910.bv.mobile.component.home.UserDialogContent
import dev.aaa1115910.bv.viewmodel.UserSwitchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun MineScreen(
    modifier: Modifier = Modifier,
    windowSize: WindowWidthSizeClass,
    userSwitchViewModel: UserSwitchViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            userSwitchViewModel.updateUserDbList()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = if (windowSize == WindowWidthSizeClass.Compact) 0.dp else 24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            UserDialogContent(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .clip(if (windowSize == WindowWidthSizeClass.Compact) MaterialTheme.shapes.extraSmall else MaterialTheme.shapes.extraLarge),
                showCloseButton = false,
                currentUser = userSwitchViewModel.currentUser.takeIf { it.id != -1 },
                userList = userSwitchViewModel.userDbList,
                onClose = {},
                onSwitchUser = { user ->
                    scope.launch(Dispatchers.IO) {
                        userSwitchViewModel.switchUser(user)
                    }
                },
                onAddUser = { context.startActivity(Intent(context, LoginActivity::class.java)) },
                onDeleteUser = { user ->
                    scope.launch(Dispatchers.IO) {
                        userSwitchViewModel.deleteUser(user)
                    }
                },
                onOpenFollowingUser = {
                    context.startActivity(Intent(context, FollowingUserActivity::class.java))
                },
                onOpenHistory = {
                    context.startActivity(Intent(context, HistoryActivity::class.java))
                },
                onOpenFavorite = {
                    context.startActivity(Intent(context, FavoriteActivity::class.java))
                },
                onOpenFollowingPgc = {
                    context.startActivity(Intent(context, FollowingSeasonActivity::class.java))
                },
                onOpenToView = {
                    context.startActivity(Intent(context, ToViewActivity::class.java))
                },
                onOpenSettings = {
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                }
            )
        }
    }
}
