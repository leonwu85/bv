package dev.aaa1115910.bv.tv.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.tv.activities.settings.SettingsActivity
import dev.aaa1115910.bv.tv.screens.main.DrawerContent
import dev.aaa1115910.bv.tv.screens.main.DrawerItem
import dev.aaa1115910.bv.tv.screens.main.HomeContent
import dev.aaa1115910.bv.tv.screens.main.LiveContent
import dev.aaa1115910.bv.tv.screens.main.PgcContent
import dev.aaa1115910.bv.tv.screens.main.SettingsContent
import dev.aaa1115910.bv.tv.screens.main.UgcContent
import dev.aaa1115910.bv.tv.screens.main.UserContent
import dev.aaa1115910.bv.tv.screens.search.SearchInputScreen
import dev.aaa1115910.bv.tv.util.drawerNavItemsFlow
import dev.aaa1115910.bv.tv.util.parseDrawerItemsOrder
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fException
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.UserViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    userViewModel: UserViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val logger = KotlinLogging.logger("MainScreen")
    val enableMainUiAnimation by Prefs.enableMainUiAnimationFlow.collectAsState(Prefs.enableMainUiAnimation)
    val drawerMenuItems by drawerNavItemsFlow.collectAsState(
        initial = remember { parseDrawerItemsOrder(Prefs.drawerItemsOrder) }
    )
    val preferredDefaultDrawerItem = remember(drawerMenuItems) {
        DrawerItem.entries
            .getOrNull(Prefs.defaultDrawerTab)
            ?.takeIf { it.isConfigurable && it in drawerMenuItems }
    }
    var lastPressBack: Long by remember { mutableLongStateOf(0L) }
    var selectedDrawerItem by remember {
        mutableStateOf(
            preferredDefaultDrawerItem
                ?: drawerMenuItems.firstOrNull()
                ?: DrawerItem.defaultConfigurableItem
        )
    }
    val drawerFocusRequesters = remember {
        DrawerItem.entries.associateWith { FocusRequester() }
    }

    var homeSelectedTabIndex by rememberSaveable { mutableIntStateOf(Prefs.defaultHomeTab) }
    var ugcSelectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var pgcSelectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    val mainFocusRequester = remember { FocusRequester() }
    val ugcFocusRequester = remember { FocusRequester() }
    val pgcFocusRequester = remember { FocusRequester() }
    val liveFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }
    val userFocusRequester = remember { FocusRequester() }
    val settingsFocusRequester = remember { FocusRequester() }

    fun requestDrawerFocus(item: DrawerItem) {
        drawerFocusRequesters[item]?.requestFocus()
    }

    fun requestContentFocus(item: DrawerItem) {
        when (item) {
            DrawerItem.User -> userFocusRequester.requestFocus()
            DrawerItem.Home -> mainFocusRequester.requestFocus()
            DrawerItem.UGC -> ugcFocusRequester.requestFocus()
            DrawerItem.PGC -> pgcFocusRequester.requestFocus()
            DrawerItem.Live -> liveFocusRequester.requestFocus()
            DrawerItem.Search -> searchFocusRequester.requestFocus()
            DrawerItem.Settings -> settingsFocusRequester.requestFocus()
        }
    }

    // 时间显示状态
    var currentTime by remember {
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        mutableStateOf(dateFormat.format(Date()))
    }

    // 定时更新时间
    LaunchedEffect(Unit) {
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            delay(60_000L - System.currentTimeMillis() % 60_000L)
            currentTime = dateFormat.format(Date())
        }
    }

    val handleBack = {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPressBack < 1500) {
            logger.fInfo { "Exiting Bv Video" }
            (context as Activity).finish()
        } else {
            lastPressBack = currentTime
            R.string.home_press_back_again_to_exit.toast(context)
        }
    }

    val onFocusToContent: (DrawerItem) -> Unit = { drawerItem ->
        requestContentFocus(drawerItem)
    }

    LaunchedEffect(drawerMenuItems) {
        val fallbackItem = preferredDefaultDrawerItem
            ?: drawerMenuItems.firstOrNull()
            ?: DrawerItem.defaultConfigurableItem
        if (selectedDrawerItem.isConfigurable && selectedDrawerItem !in drawerMenuItems) {
            selectedDrawerItem = fallbackItem
        }
    }

    LaunchedEffect(userViewModel.isLogin) {
        if (userViewModel.isLogin) {
            userViewModel.updateUserInfo()
        } else {
            userViewModel.clearUserInfo()
        }
    }

    LaunchedEffect(Unit) {
        runCatching {
            requestContentFocus(selectedDrawerItem)
        }.onFailure {
            logger.fException(it) { "request default focus requester failed" }
        }
    }

    BackHandler {
        handleBack()
    }

    Scaffold(modifier = modifier) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            val borderColor = MaterialTheme.colorScheme.surfaceContainerHigh
            val borderWidth = 1.dp
            // Left side - NavigationRail
            NavigationRail(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(71.dp)
                    .padding(end = borderWidth)
                    .drawBehind {
                        val borderWidthPx = borderWidth.toPx()
                        val x = size.width + borderWidthPx

                        drawLine(
                            color = borderColor,
                            start = Offset(x = x, y = 0f),
                            end = Offset(x = x, y = size.height),
                            strokeWidth = borderWidthPx
                        )
                    },
            ) {
                DrawerContent(
                    modifier = Modifier.fillMaxWidth(),
                    focusRequesters = drawerFocusRequesters,
                    currentDrawerItem = selectedDrawerItem,
                    isLogin = userViewModel.isLogin,
                    avatar = userViewModel.face,
                    username = userViewModel.username,
                    onDrawerItemChanged = { selectedDrawerItem = it },
                    onOpenSettings = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    },
                    onFocusToContent = onFocusToContent
                )
            }

            // Right side - NavHost content
            AnimatedContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 72.dp),
                targetState = selectedDrawerItem,
                label = "main animated content",
                transitionSpec = {
                    if (!enableMainUiAnimation) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        val coefficient = 20
                        if (targetState.ordinal < initialState.ordinal) {
                            slideInVertically { -it / coefficient } togetherWith
                                    fadeOut(animationSpec = tween(200)) + slideOutVertically { it / coefficient }
                        } else {
                            slideInVertically { it / coefficient } togetherWith
                                    fadeOut(animationSpec = tween(200)) + slideOutVertically { -it / coefficient }
                        }
                    }
                }
            ) { screen ->
                when (screen) {
                    DrawerItem.User -> UserContent(
                        navFocusRequester = userFocusRequester,
                        onRequestDrawerFocus = { requestDrawerFocus(DrawerItem.User) },
                        userViewModel = userViewModel
                    )
                    DrawerItem.Home -> HomeContent(
                        navFocusRequester = mainFocusRequester,
                        selectedTabOrdinal = homeSelectedTabIndex,
                        onSelectedTabChanged = { homeSelectedTabIndex = it },
                        onRequestDrawerFocus = { requestDrawerFocus(DrawerItem.Home) }
                    )
                    DrawerItem.UGC -> UgcContent(
                        navFocusRequester = ugcFocusRequester,
                        selectedTabOrdinal = ugcSelectedTabIndex,
                        onSelectedTabChanged = { ugcSelectedTabIndex = it },
                        onRequestDrawerFocus = { requestDrawerFocus(DrawerItem.UGC) }
                    )
                    DrawerItem.PGC -> PgcContent(
                        navFocusRequester = pgcFocusRequester,
                        selectedTabOrdinal = pgcSelectedTabIndex,
                        onSelectedTabChanged = { pgcSelectedTabIndex = it },
                        onRequestDrawerFocus = { requestDrawerFocus(DrawerItem.PGC) }
                    )
                    DrawerItem.Live -> LiveContent(
                        navFocusRequester = liveFocusRequester,
                        onRequestDrawerFocus = { requestDrawerFocus(DrawerItem.Live) }
                    )
                    DrawerItem.Search -> SearchInputScreen(
                        defaultFocusRequester = searchFocusRequester,
                        onRequestDrawerFocus = { requestDrawerFocus(DrawerItem.Search) }
                    )
                    DrawerItem.Settings -> SettingsContent(
                        navFocusRequester = settingsFocusRequester,
                        onRequestDrawerFocus = { requestDrawerFocus(DrawerItem.Settings) }
                    )
                }
            }

            // 右上角时间显示
            Text(
                text = currentTime,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 10.dp),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
