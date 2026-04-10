package dev.aaa1115910.bv.tv.screens.user

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.http.entity.AuthFailureException
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.tv.activities.user.FollowActivity
import dev.aaa1115910.bv.tv.activities.user.FollowingSeasonActivity
import dev.aaa1115910.bv.tv.activities.user.HistoryActivity
import dev.aaa1115910.bv.tv.activities.user.UserSwitchActivity
import dev.aaa1115910.bv.tv.activities.user.FavoriteActivity
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.fWarn
import dev.aaa1115910.bv.util.isDpadLeft
import dev.aaa1115910.bv.util.isDpadRight
import dev.aaa1115910.bv.util.isKeyDown
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.UserViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
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
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val logger = KotlinLogging.logger { }
    val focusRequester = initialFocusRequester ?: remember { FocusRequester() }
    var contentHasFocus by remember { mutableStateOf(false) }
    var followingUpCount by remember { mutableIntStateOf(0) }
    var incognitoModeEnabled by remember { mutableStateOf(Prefs.incognitoMode) }

    val updateFollowingUpCount = {
        scope.launch(Dispatchers.IO) {
            runCatching {
                logger.fInfo { "Update following up count with user ${Prefs.uid}" }
                userRepository.getFollowingUpCount(
                    mid = Prefs.uid,
                    preferApiType = Prefs.apiType
                )
            }.onSuccess {
                followingUpCount = it
                logger.fInfo { "Following up count: $followingUpCount" }
            }.onFailure {
                logger.fWarn { "Load followed users count failed: ${it.stackTraceToString()}" }
                when (it) {
                    is AuthFailureException -> {
                        withContext(Dispatchers.Main) {
                            context.getString(R.string.exception_auth_failure).toast(context)
                        }
                        logger.fInfo { "User auth failure" }
                        if (!BuildConfig.DEBUG) userViewModel.logout()
                    }

                    else -> {}
                }
            }
        }
    }

    val updateData = {
        userViewModel.updateUserInfo(forceUpdate = true)
        incognitoModeEnabled = Prefs.incognitoMode
        updateFollowingUpCount()
    }

    BackHandler(enabled = contentHasFocus && onRequestDrawerFocus != null) {
        onRequestDrawerFocus?.invoke()
    }

    LaunchedEffect(Unit) {
        if (autoRequestInitialFocus) {
            focusRequester.requestFocus()
        }
        updateData()
    }

    DisposableEffect(lifecycleOwner) {
        var leaveFromThisPage = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                leaveFromThisPage = true
            } else if (event == Lifecycle.Event.ON_RESUME) {
                if (leaveFromThisPage) updateData()
                leaveFromThisPage = false
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = modifier
            .onFocusChanged { contentHasFocus = it.hasFocus }
            .onKeyEvent {
                if (contentHasFocus && onRequestDrawerFocus != null && it.isKeyDown() && it.isDpadLeft()) {
                    onRequestDrawerFocus()
                    true
                } else {
                    false
                }
            }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 48.dp, top = 24.dp, end = 48.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                UserInfo(
                    modifier = Modifier.fillMaxWidth(),
                    username = userViewModel.username,
                    face = userViewModel.face,
                    uid = userViewModel.responseData?.mid ?: 0,
                    level = userViewModel.responseData?.level ?: 0,
                    currentExp = userViewModel.responseData?.levelExp?.currentExp ?: 0,
                    nextLevelExp = with(userViewModel.responseData?.levelExp?.nextExp) {
                        if (this == null) {
                            1
                        } else if (this <= 0) {
                            userViewModel.responseData?.levelExp?.currentExp ?: 1
                        } else {
                            (userViewModel.responseData?.levelExp?.currentExp ?: 1)
                            +(userViewModel.responseData?.levelExp?.nextExp ?: 0)
                        }
                    },
                    showLabel = userViewModel.responseData?.vip?.avatarSubscript == 1,
                    labelUrl = userViewModel.responseData?.vip?.label?.imgLabelUriHansStatic ?: "",
                    coins = userViewModel.responseData?.coins ?: 0f,
                    followingUpCount = followingUpCount,
                )
            }
            item {
                UserQuickAccessSection(
                    modifier = Modifier.fillMaxWidth(),
                    initialFocusRequester = focusRequester,
                    followingUpCount = followingUpCount,
                    incognitoModeEnabled = incognitoModeEnabled,
                    onOpenHistory = {
                        context.startActivity(Intent(context, HistoryActivity::class.java))
                    },
                    onOpenFollowingSeason = {
                        context.startActivity(Intent(context, FollowingSeasonActivity::class.java))
                    },
                    onOpenFavorite = {
                        context.startActivity(Intent(context, FavoriteActivity::class.java))
                    },
                    onOpenFollowingUser = {
                        context.startActivity(Intent(context, FollowActivity::class.java))
                    },
                    onOpenUserSwitch = {
                        context.startActivity(Intent(context, UserSwitchActivity::class.java))
                    },
                    onToggleIncognito = {
                        incognitoModeEnabled = !incognitoModeEnabled
                        Prefs.incognitoMode = incognitoModeEnabled
                    }
                )
            }
        }
    }
}

@Composable
private fun UserInfo(
    modifier: Modifier = Modifier,
    face: String,
    username: String,
    uid: Long,
    level: Int,
    currentExp: Int,
    nextLevelExp: Int,
    showLabel: Boolean,
    labelUrl: String,
    followingUpCount: Int,
    coins: Float = 0f
) {
    val levelSlider by animateFloatAsState(
        targetValue = if (nextLevelExp <= 0) 0f else (currentExp.toFloat() / nextLevelExp.toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900),
        label = "user level progress"
    )
    val animateFollowingNumber by animateIntAsState(
        targetValue = followingUpCount,
        label = "animate following number"
    )

    Surface(
        modifier = modifier,
        colors = SurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
                        )
                    )
                )
                .padding(32.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(102.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                shape = CircleShape
                            )
                            .background(Color.White.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            modifier = Modifier
                                .size(104.dp)
                                .clip(CircleShape),
                            model = face,
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = username,
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 34.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (showLabel) {
                                AsyncImage(
                                    modifier = Modifier
                                        .height(24.dp)
                                        .widthIn(max = 112.dp),
                                    model = labelUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.FillHeight
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.user_info_uid, uid),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.user_info_coins, coins),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        UserMetricBadge(text = stringResource(R.string.user_info_level, level))
                        UserMetricBadge(
                            text = stringResource(R.string.user_homepage_follow),
                            value = animateFollowingNumber.toString()
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "$currentExp/$nextLevelExp",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        progress = { levelSlider },
                        gapSize = 0.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun UserMetricBadge(
    text: String,
    value: String? = null,
) {
    Column(
        modifier = Modifier
            .background(
                color = Color.White.copy(alpha = 0.16f),
                shape = MaterialTheme.shapes.large
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun UserQuickAccessSection(
    modifier: Modifier = Modifier,
    initialFocusRequester: FocusRequester,
    followingUpCount: Int,
    incognitoModeEnabled: Boolean,
    onOpenHistory: () -> Unit,
    onOpenFollowingSeason: () -> Unit,
    onOpenFavorite: () -> Unit,
    onOpenFollowingUser: () -> Unit,
    onOpenUserSwitch: () -> Unit,
    onToggleIncognito: () -> Unit,
) {
    val focusRequesters = remember(initialFocusRequester) {
        listOf(
            initialFocusRequester,
            FocusRequester(),
            FocusRequester(),
            FocusRequester(),
            FocusRequester(),
            FocusRequester()
        )
    }

    Column(
        modifier = modifier.focusGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            UserActionCard(
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequesters[0])
                    .onPreviewKeyEvent {
                        if (!it.isKeyDown()) return@onPreviewKeyEvent false
                        when {
                            it.isDpadLeft() -> false
                            it.isDpadRight() -> {
                                focusRequesters[1].requestFocus()
                                true
                            }
                            it.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP -> true
                            it.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                focusRequesters[3].requestFocus()
                                true
                            }
                            else -> false
                        }
                    },
                title = stringResource(R.string.title_activity_history),
                subtitle = stringResource(R.string.user_homepage_recent_desc),
                badgeText = "",
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                onClick = onOpenHistory
            )
            UserActionCard(
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequesters[1])
                    .onPreviewKeyEvent {
                        if (!it.isKeyDown()) return@onPreviewKeyEvent false
                        when {
                            it.isDpadLeft() -> {
                                focusRequesters[0].requestFocus()
                                true
                            }
                            it.isDpadRight() -> {
                                focusRequesters[2].requestFocus()
                                true
                            }
                            it.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP -> true
                            it.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                focusRequesters[4].requestFocus()
                                true
                            }
                            else -> false
                        }
                    },
                title = stringResource(R.string.title_activity_following_season),
                subtitle = stringResource(R.string.user_homepage_anime_desc),
                badgeText = "",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f),
                onClick = onOpenFollowingSeason
            )
            UserActionCard(
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequesters[2])
                    .onPreviewKeyEvent {
                        if (!it.isKeyDown()) return@onPreviewKeyEvent false
                        when {
                            it.isDpadLeft() -> {
                                focusRequesters[1].requestFocus()
                                true
                            }
                            it.isDpadRight() -> true
                            it.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP -> true
                            it.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                focusRequesters[5].requestFocus()
                                true
                            }
                            else -> false
                        }
                    },
                title = stringResource(R.string.title_activity_favorite),
                subtitle = stringResource(R.string.user_homepage_favorite_desc),
                badgeText = "",
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                onClick = onOpenFavorite
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            UserActionCard(
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequesters[3])
                    .onPreviewKeyEvent {
                        if (!it.isKeyDown()) return@onPreviewKeyEvent false
                        when {
                            it.isDpadLeft() -> false
                            it.isDpadRight() -> {
                                focusRequesters[4].requestFocus()
                                true
                            }
                            it.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                focusRequesters[0].requestFocus()
                                true
                            }
                            it.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN -> true
                            else -> false
                        }
                    },
                title = stringResource(R.string.user_homepage_follow),
                subtitle = stringResource(R.string.user_homepage_follow_desc),
                badgeText = followingUpCount.toString(),
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f),
                onClick = onOpenFollowingUser
            )
            UserActionCard(
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequesters[4])
                    .onPreviewKeyEvent {
                        if (!it.isKeyDown()) return@onPreviewKeyEvent false
                        when {
                            it.isDpadLeft() -> {
                                focusRequesters[3].requestFocus()
                                true
                            }
                            it.isDpadRight() -> {
                                focusRequesters[5].requestFocus()
                                true
                            }
                            it.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                focusRequesters[1].requestFocus()
                                true
                            }
                            it.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN -> true
                            else -> false
                        }
                    },
                title = stringResource(R.string.user_homepage_user_switch),
                subtitle = stringResource(R.string.user_homepage_user_switch_desc),
                badgeText = "",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.78f),
                onClick = onOpenUserSwitch
            )
            UserActionCard(
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequesters[5])
                    .onPreviewKeyEvent {
                        if (!it.isKeyDown()) return@onPreviewKeyEvent false
                        when {
                            it.isDpadLeft() -> {
                                focusRequesters[4].requestFocus()
                                true
                            }
                            it.isDpadRight() -> true
                            it.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                focusRequesters[2].requestFocus()
                                true
                            }
                            it.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN -> true
                            else -> false
                        }
                    },
                title = stringResource(R.string.user_info_Incognito_mode_title),
                subtitle = stringResource(R.string.user_homepage_incognito_desc),
                badgeText = if (incognitoModeEnabled) {
                    stringResource(R.string.user_info_Incognito_mode_on)
                } else {
                    stringResource(R.string.user_info_Incognito_mode_off)
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                onClick = onToggleIncognito
            )
        }
    }
}

@Composable
private fun UserActionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    badgeText: String,
    containerColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.height(116.dp),
        colors = ClickableSurfaceDefaults.colors(containerColor = containerColor),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.extraLarge),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                // 居右
                modifier = Modifier.align(Alignment.End),
                text = badgeText,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview
@Composable
private fun UserInfoPreview() {
    BVTheme {
        UserInfo(
            modifier = Modifier.fillMaxWidth(),
            face = "",
            username = "Username",
            uid = 12345,
            level = 6,
            currentExp = 1234,
            nextLevelExp = 2345,
            showLabel = false,
            labelUrl = "",
            followingUpCount = 466,
        )
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun UserQuickAccessSectionPreview() {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BVTheme {
        UserQuickAccessSection(
            modifier = Modifier.fillMaxWidth(),
            initialFocusRequester = focusRequester,
            followingUpCount = 466,
            incognitoModeEnabled = true,
            onOpenHistory = {},
            onOpenFollowingSeason = {},
            onOpenFavorite = {},
            onOpenFollowingUser = {},
            onOpenUserSwitch = {},
            onToggleIncognito = {}
        )
    }
}