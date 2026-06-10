package dev.aaa1115910.bv.tv.screens.settings.content

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.repository.UserRepository
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.tv.component.settings.SettingListItem
import dev.aaa1115910.bv.tv.component.settings.PlayerShortcutKeyBindingsDialog
import dev.aaa1115910.bv.tv.component.settings.SettingSwitchListItem
import dev.aaa1115910.bv.tv.activities.settings.LogsActivity
import dev.aaa1115910.bv.tv.screens.settings.SettingsMenuNavItem
import dev.aaa1115910.bv.util.FirebaseUtil
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@Composable
fun OtherSetting(
    modifier: Modifier = Modifier,
    userRepository: UserRepository = koinInject()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showFps by remember { mutableStateOf(Prefs.showFps) }
    var showPlayerShortcutDialog by remember { mutableStateOf(false) }
    var playerShortcutKeyBindings by remember { mutableStateOf(Prefs.playerShortcutKeyBindings) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var logoutInProgress by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = SettingsMenuNavItem.Other.getDisplayName(context),
            style = MaterialTheme.typography.displaySmall
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingSwitchListItem(
                    title = stringResource(R.string.settings_other_firebase_title),
                    supportText = stringResource(R.string.settings_other_firebase_text),
                    checked = Prefs.enableFirebaseCollection,
                    onCheckedChange = {
                        Prefs.enableFirebaseCollection = it
                        FirebaseUtil.setCrashlyticsCollectionEnabled(it)
                    }
                )
            }
            item {
                SettingSwitchListItem(
                    title = stringResource(R.string.settings_other_fps_title),
                    supportText = stringResource(R.string.settings_other_fps_text),
                    checked = showFps,
                    onCheckedChange = {
                        showFps = it
                        Prefs.showFps = it
                    }
                )
            }
            item {
                SettingListItem(
                    title = stringResource(R.string.settings_player_shortcut_section_title),
                    supportText = stringResource(R.string.settings_player_shortcut_entry_text),
                    onClick = {
                        showPlayerShortcutDialog = true
                    }
                )
            }
            item {
                SettingListItem(
                    title = stringResource(R.string.settings_create_logs_title),
                    supportText = stringResource(R.string.settings_create_logs_text),
                    onClick = {
                        context.startActivity(Intent(context, LogsActivity::class.java))
                    }
                )
            }
            if (userRepository.isLogin) {
                item {
                    SettingListItem(
                        title = stringResource(R.string.settings_logout_title),
                        supportText = stringResource(R.string.settings_logout_text),
                        onClick = {
                            showLogoutDialog = true
                        }
                    )
                }
            }
            if (BuildConfig.DEBUG) {
                item {
                    SettingListItem(
                        title = stringResource(R.string.settings_crash_test_title),
                        supportText = stringResource(R.string.settings_crash_test_text),
                        onClick = {
                            throw Exception("Boom!")
                        }
                    )
                }
            }
        }
    }

    PlayerShortcutKeyBindingsDialog(
        show = showPlayerShortcutDialog,
        keyBindings = playerShortcutKeyBindings,
        onKeyCodeChange = { action, keyCode ->
            Prefs.setPlayerShortcutKeyCode(action, keyCode)
            playerShortcutKeyBindings = Prefs.playerShortcutKeyBindings
        },
        onDismissRequest = { showPlayerShortcutDialog = false }
    )

    if (showLogoutDialog) {
        TvAlertDialog(
            onDismissRequest = {
                if (!logoutInProgress) showLogoutDialog = false
            },
            title = { Text(text = stringResource(R.string.settings_logout_dialog_title)) },
            text = { Text(text = stringResource(R.string.settings_logout_dialog_text)) },
            confirmButton = {
                Button(
                    enabled = !logoutInProgress,
                    onClick = {
                        scope.launch {
                            logoutInProgress = true
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    userRepository.logoutFromServer()
                                }
                            }.onSuccess {
                                showLogoutDialog = false
                                context.getString(R.string.settings_logout_success).toast(context)
                            }.onFailure {
                                context.getString(
                                    R.string.settings_logout_failed,
                                    it.localizedMessage ?: it.message ?: "未知错误"
                                ).toast(context)
                            }
                            logoutInProgress = false
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.settings_logout_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(
                    enabled = !logoutInProgress,
                    onClick = { showLogoutDialog = false }
                ) {
                    Text(text = stringResource(R.string.settings_logout_cancel))
                }
            }
        )
    }
}
