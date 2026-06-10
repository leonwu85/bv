package dev.aaa1115910.bv.mobile.screen.settings

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneExpansionAnchor
import androidx.compose.material3.adaptive.layout.PaneExpansionState
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldScope
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.PlayerType
import dev.aaa1115910.bv.mobile.settings.MobilePrefs
import dev.aaa1115910.bv.repository.UserRepository
import dev.aaa1115910.bv.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SettingsScreen(
    userRepository: UserRepository = koinInject()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator()

    var selectedSettings by rememberSaveable { mutableStateOf<MobileSettings?>(null) }
    var playerType by remember { mutableStateOf(MobilePrefs.playerType) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var logoutInProgress by remember { mutableStateOf(false) }
    val onPlayerTypeChanged: (PlayerType) -> Unit = { nextPlayerType ->
        playerType = nextPlayerType
        if (nextPlayerType != PlayerType.MPV && selectedSettings == MobileSettings.Mpv) {
            selectedSettings = MobileSettings.AudioVideo
        }
    }
    val effectiveSelectedSettings = if (playerType != PlayerType.MPV && selectedSettings == MobileSettings.Mpv) {
        MobileSettings.AudioVideo
    } else {
        selectedSettings
    }
    val singlePart = listOf(WindowWidthSizeClass.COMPACT, WindowWidthSizeClass.MEDIUM)
        .contains(currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass)

    BackHandler(scaffoldNavigator.canNavigateBack()) {
        scope.launch { scaffoldNavigator.navigateBack() }
    }

    ListDetailPaneScaffold(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        directive = scaffoldNavigator.scaffoldDirective,
        value = scaffoldNavigator.scaffoldValue,
        listPane = {
            AnimatedPane(
                modifier = Modifier.preferredWidth(360.dp),
                enterTransition = fadeIn() + slideInHorizontally(),
                exitTransition = fadeOut() + slideOutHorizontally()
            ) {
                SettingsCategories(
                    selectedSettings = if (singlePart) null else effectiveSelectedSettings
                        ?: MobileSettings.Appearance,
                    onSelectedSettings = {
                        selectedSettings = it
                        scope.launch {
                            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                        }
                    },
                    showMpvSettings = playerType == PlayerType.MPV,
                    showNavBack = !scaffoldNavigator.canNavigateBack(),
                    isLogin = userRepository.isLogin,
                    onLogout = { showLogoutDialog = true },
                    onBack = { (context as Activity).finish() },
                )
            }
        },
        detailPane = {
            AnimatedPane(
                modifier = Modifier,
                enterTransition = fadeIn() + slideInHorizontally { it / 2 },
                exitTransition = fadeOut() + slideOutHorizontally { it / 2 }
            ) {
                SettingsDetails(
                    selectedSettings = effectiveSelectedSettings ?: MobileSettings.Appearance,
                    showNavBack = scaffoldNavigator.canNavigateBack(),
                    onPlayerTypeChanged = onPlayerTypeChanged,
                    onBack = { scope.launch { scaffoldNavigator.navigateBack() } }
                )
            }
        },
        paneExpansionDragHandle = { state -> PaneExpansionDragHandle(state) },
        paneExpansionState = rememberPaneExpansionState(
            keyProvider = scaffoldNavigator.scaffoldValue,
            anchors = PaneExpansionAnchors,
        )
    )

    if (showLogoutDialog) {
        AlertDialog(
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
                TextButton(
                    enabled = !logoutInProgress,
                    onClick = { showLogoutDialog = false }
                ) {
                    Text(text = stringResource(R.string.settings_logout_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ThreePaneScaffoldScope.PaneExpansionDragHandle(
    state: PaneExpansionState = rememberPaneExpansionState()
) {
    val interactionSource = remember { MutableInteractionSource() }
    VerticalDragHandle(
        modifier = Modifier
            .paneExpansionDraggable(
                state,
                LocalMinimumInteractiveComponentSize.current,
                interactionSource,
            ),
        interactionSource = interactionSource
    )
}

enum class MobileSettings(
    val title: String,
    val summary: String? = null
) {
    Appearance(title = "外观设置", summary = "主题模式、动态取色、主色"),
    AudioVideo(title = "音视频设置", summary = "画质音质、CDN、解码"),
    Play(title = "播放设置", summary = "播放行为、弹幕、直播"),
    Mpv(title = "MPV 设置", summary = "超分、输出、硬解与缓存参数"),
    SponsorBlock(title = "广告助手", summary = "广告片段识别、自动或手动跳过"),
    Advance(title = "更多设置", summary = "接口"),
    Debug(title = "调试", "播放器信息显示"),
    About(title = "关于", summary = "版本和项目说明");
}

private val PaneExpansionAnchors = listOf(
    PaneExpansionAnchor.Offset.fromStart(360.dp),
    PaneExpansionAnchor.Proportion(0.5f),
    PaneExpansionAnchor.Offset.fromEnd(360.dp),
)
