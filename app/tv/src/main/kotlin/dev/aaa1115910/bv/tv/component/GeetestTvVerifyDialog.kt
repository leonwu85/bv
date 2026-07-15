package dev.aaa1115910.bv.tv.component

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.aaa1115910.bv.component.QrImage
import dev.aaa1115910.bv.network.GeetestCompanionService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger("GeetestTvVerify")

/**
 * Geetest 验证结果
 */
data class GeetestTvResult(
    val challenge: String,
    val validate: String,
    val seccode: String,
)

/** 验证交互模式（调试入口可指定初始页） */
enum class GeetestVerifyMode {
    /** 本机：遥控器十字光标 + WebView */
    TvRemote,
    /** 手机：局域网扫码代完成 */
    PhoneCompanion,
}

internal fun shouldRefreshGeetestChallenge(
    currentMode: GeetestVerifyMode,
    requestedMode: GeetestVerifyMode,
    challengeReady: Boolean,
    mockMode: Boolean,
    refreshAvailable: Boolean,
): Boolean =
    !mockMode &&
        refreshAvailable &&
        (requestedMode != currentMode || !challengeReady)

/**
 * TV 端 Geetest 风控验证弹窗
 *
 * 模式：
 * 1. 本机验证：WebView + 十字光标，遥控器方向键移动、确认键点击
 * 2. 手机验证：局域网 HTTP 页面 + 二维码，手机触屏完成极验后自动回传
 *
 * @param mockMode Debug 用：不请求真实极验，用可点击 mock 页测遥控器/手机链路
 */
@Composable
fun GeetestTvVerifyDialog(
    gt: String,
    challenge: String,
    onResult: (GeetestTvResult) -> Unit,
    onDismiss: () -> Unit,
    onRefreshChallenge: (suspend () -> Boolean)? = null,
    mockMode: Boolean = false,
    initialMode: GeetestVerifyMode = GeetestVerifyMode.TvRemote,
) {
    var mode by remember { mutableStateOf(initialMode) }
    val tvModeFocusRequester = remember { FocusRequester() }
    val phoneModeFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    // >0 表示用户在模式 Tab 按了确定，需要把焦点交回验证区
    var enterContentToken by remember { mutableIntStateOf(0) }
    var challengeReady by remember { mutableStateOf(true) }
    var challengeRefreshing by remember { mutableStateOf(false) }
    var challengeRefreshRequest by remember { mutableIntStateOf(0) }
    var challengeRefreshError by remember { mutableStateOf<String?>(null) }
    var enterAfterChallengeRefresh by remember { mutableStateOf(false) }

    // 当前选中模式对应的 tab 焦点，验证区按返回时落到这里
    val activeModeFocusRequester = when (mode) {
        GeetestVerifyMode.TvRemote -> tvModeFocusRequester
        GeetestVerifyMode.PhoneCompanion -> phoneModeFocusRequester
    }

    // 首次进入：焦点在验证区
    LaunchedEffect(Unit) {
        runCatching { contentFocusRequester.requestFocus() }
    }

    // 模式 Tab 按确定后：等 content 重组再抢焦点（避免左右选 tab 时抢焦点）
    LaunchedEffect(enterContentToken) {
        if (enterContentToken <= 0) return@LaunchedEffect
        delay(48)
        runCatching { contentFocusRequester.requestFocus() }
            .onFailure { logger.warn(it) { "focus content after confirm failed" } }
    }

    fun requestMode(newMode: GeetestVerifyMode, enterContent: Boolean) {
        val refreshRequired = shouldRefreshGeetestChallenge(
            currentMode = mode,
            requestedMode = newMode,
            challengeReady = challengeReady,
            mockMode = mockMode,
            refreshAvailable = onRefreshChallenge != null,
        )
        if (!refreshRequired) {
            mode = newMode
            challengeReady = true
            challengeRefreshError = null
            if (enterContent) enterContentToken += 1
            return
        }

        // 同一目标模式已在刷新时，确认键只记录“刷新后进入”，不重复请求。
        if (newMode == mode && challengeRefreshing) {
            enterAfterChallengeRefresh = enterAfterChallengeRefresh || enterContent
            return
        }

        // 先卸载旧 WebView/手机会话，获得新 challenge 后才创建目标验证页。
        mode = newMode
        challengeReady = false
        challengeRefreshError = null
        enterAfterChallengeRefresh = enterContent
        challengeRefreshRequest += 1
    }

    LaunchedEffect(challengeRefreshRequest) {
        if (challengeRefreshRequest <= 0) return@LaunchedEffect
        val refreshChallenge = onRefreshChallenge ?: return@LaunchedEffect
        challengeRefreshing = true
        val refreshed = try {
            refreshChallenge()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(e) { "Refresh Geetest challenge before mode switch failed" }
            false
        }
        challengeRefreshing = false
        challengeReady = refreshed
        challengeRefreshError = if (refreshed) {
            null
        } else {
            "获取新验证码失败，请在当前模式上按确定重试"
        }
        val shouldEnterContent = enterAfterChallengeRefresh
        enterAfterChallengeRefresh = false
        if (refreshed && shouldEnterContent) enterContentToken += 1
    }

    TvOverlayDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .widthIn(max = 720.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (mockMode) "Mock 人机验证（调试）" else "需要完成人机验证",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                ModeSwitcher(
                    mode = mode,
                    tvModeFocusRequester = tvModeFocusRequester,
                    phoneModeFocusRequester = phoneModeFocusRequester,
                    contentFocusRequester = contentFocusRequester,
                    onModeChange = { newMode -> requestMode(newMode, enterContent = false) },
                    onConfirmEnterContent = { selected -> requestMode(selected, enterContent = true) },
                    onBackFromModeTab = onDismiss,
                )

                Text(
                    text = when {
                        challengeRefreshing -> "正在获取新的验证码…"
                        challengeRefreshError != null -> challengeRefreshError.orEmpty()
                        else -> "验证区按返回 → 模式 Tab ｜ 模式上按确定 → 进入验证区 ｜ 模式上再返回关闭"
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (challengeRefreshError != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    },
                    textAlign = TextAlign.Center,
                )

                if (!challengeReady) {
                    GeetestChallengeRefreshContent(
                        refreshing = challengeRefreshing,
                        errorMessage = challengeRefreshError,
                        contentFocusRequester = contentFocusRequester,
                        modeTabFocusRequester = activeModeFocusRequester,
                    )
                } else when (mode) {
                    GeetestVerifyMode.TvRemote -> GeetestTvVerifyContent(
                        gt = gt,
                        challenge = challenge,
                        mockMode = mockMode,
                        contentFocusRequester = contentFocusRequester,
                        modeTabFocusRequester = activeModeFocusRequester,
                        onResult = onResult,
                    )

                    GeetestVerifyMode.PhoneCompanion -> GeetestPhoneCompanionContent(
                        gt = gt,
                        challenge = challenge,
                        mockMode = mockMode,
                        contentFocusRequester = contentFocusRequester,
                        modeTabFocusRequester = activeModeFocusRequester,
                        onResult = onResult,
                    )
                }
            }
        }
    }
}

@Composable
private fun GeetestChallengeRefreshContent(
    refreshing: Boolean,
    errorMessage: String?,
    contentFocusRequester: FocusRequester,
    modeTabFocusRequester: FocusRequester,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .onPreviewKeyEvent { event ->
                val isBack = event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                    event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK
                if (isBack) {
                    runCatching { modeTabFocusRequester.requestFocus() }
                }
                isBack
            }
            .focusRequester(contentFocusRequester)
            .focusProperties { up = modeTabFocusRequester }
            .focusable(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = when {
                refreshing -> "正在为当前验证方式获取新的验证参数…"
                errorMessage != null -> errorMessage
                else -> "正在准备新的验证码…"
            },
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.titleSmall,
            color = if (errorMessage != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ModeSwitcher(
    mode: GeetestVerifyMode,
    tvModeFocusRequester: FocusRequester,
    phoneModeFocusRequester: FocusRequester,
    contentFocusRequester: FocusRequester,
    onModeChange: (GeetestVerifyMode) -> Unit,
    onConfirmEnterContent: (GeetestVerifyMode) -> Unit,
    onBackFromModeTab: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        ModeChip(
            selected = mode == GeetestVerifyMode.TvRemote,
            label = "本机验证",
            focusRequester = tvModeFocusRequester,
            leftFocusRequester = null,
            rightFocusRequester = phoneModeFocusRequester,
            downFocusRequester = contentFocusRequester,
            onSelect = {
                // 左右移到 chip 时只预选模式，不立刻进内容
                onModeChange(GeetestVerifyMode.TvRemote)
            },
            onConfirm = { onConfirmEnterContent(GeetestVerifyMode.TvRemote) },
            onBack = onBackFromModeTab,
        )
        ModeChip(
            selected = mode == GeetestVerifyMode.PhoneCompanion,
            label = "手机验证",
            focusRequester = phoneModeFocusRequester,
            leftFocusRequester = tvModeFocusRequester,
            rightFocusRequester = null,
            downFocusRequester = contentFocusRequester,
            onSelect = { onModeChange(GeetestVerifyMode.PhoneCompanion) },
            onConfirm = { onConfirmEnterContent(GeetestVerifyMode.PhoneCompanion) },
            onBack = onBackFromModeTab,
        )
    }
}

@Composable
private fun ModeChip(
    selected: Boolean,
    label: String,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
    downFocusRequester: FocusRequester,
    onSelect: () -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    var hasFocus by remember { mutableStateOf(false) }

    // 深色模式下 inverseSurface/inverseOnSurface 对比不稳定，焦点态改用 primary 保证可读
    val containerColor = when {
        hasFocus -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    val contentColor = when {
        hasFocus -> MaterialTheme.colorScheme.onPrimary
        selected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderColor = when {
        hasFocus -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.border
        else -> MaterialTheme.colorScheme.border.copy(alpha = 0.35f)
    }

    Surface(
        // Surface 的 onClick 对应确认键：进入验证区
        onClick = onConfirm,
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusProperties {
                left = leftFocusRequester ?: FocusRequester.Cancel
                right = rightFocusRequester ?: FocusRequester.Cancel
                down = downFocusRequester
                up = FocusRequester.Cancel
            }
            .onFocusChanged { state ->
                hasFocus = state.isFocused
                // 左右移动焦点到该 tab 时，同步选中对应模式
                if (state.isFocused) onSelect()
            }
            .onPreviewKeyEvent { event ->
                val isDown = event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN
                if (!isDown) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_BACK -> {
                        onBack()
                        true
                    }

                    else -> false
                }
            },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(20.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = containerColor,
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            pressedContainerColor = MaterialTheme.colorScheme.primary,
            contentColor = contentColor,
            focusedContentColor = MaterialTheme.colorScheme.onPrimary,
            pressedContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        border = ClickableSurfaceDefaults.border(
            border = Border(border = BorderStroke(1.dp, borderColor)),
            focusedBorder = Border(
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
            ),
        ),
    ) {
        // 显式指定文字色，避免仅依赖 LocalContentColor 在深色/焦点态下对比不足
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
        )
    }
}

@Composable
private fun GeetestPhoneCompanionContent(
    gt: String,
    challenge: String,
    mockMode: Boolean = false,
    contentFocusRequester: FocusRequester,
    modeTabFocusRequester: FocusRequester,
    onResult: (GeetestTvResult) -> Unit,
) {
    val context = LocalContext.current
    val callbackScope = rememberCoroutineScope()
    var statusText by remember { mutableStateOf("正在准备手机验证…") }
    var qrContent by remember { mutableStateOf("") }
    var sessionId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(gt, challenge, mockMode) {
        statusText = "正在准备手机验证…"
        qrContent = ""
        sessionId?.let { GeetestCompanionService.removeSession(it) }

        // 等待本地 HTTP 服务就绪
        var port = 0
        repeat(20) {
            port = GeetestCompanionService.resolveServerPort()
            if (port > 0) return@repeat
            delay(100)
        }
        val host = withContext(Dispatchers.IO) {
            GeetestCompanionService.resolveLocalIpAddress(context)
        }
        if (port <= 0 || host.isBlank() || host == "0.0.0.0") {
            statusText = "无法获取本机地址，请确认电视已连接 Wi‑Fi/局域网"
            return@LaunchedEffect
        }

        val session = GeetestCompanionService.createSession(
            gt = gt,
            challenge = challenge,
            mockMode = mockMode,
            onResult = { payload ->
                // HTTP 服务的回调发生在后台线程，回到 Compose 主线程再通知 UI。
                callbackScope.launch {
                    onResult(
                        GeetestTvResult(
                            challenge = payload.challenge,
                            validate = payload.validate,
                            seccode = payload.seccode,
                        )
                    )
                }
            }
        )
        sessionId = session.id
        qrContent = GeetestCompanionService.buildVerifyUrl(host, port, session.id)
        statusText = if (mockMode) {
            "Mock：扫码后在手机点「模拟验证成功」（同一 Wi‑Fi）"
        } else {
            "请用手机扫描二维码完成验证（同一 Wi‑Fi）"
        }
        logger.info { "Phone companion verify url: $qrContent mock=$mockMode" }
    }

    DisposableEffect(Unit) {
        onDispose {
            sessionId?.let { GeetestCompanionService.removeSession(it) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                val isDown = event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN
                if (!isDown) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    // 返回：焦点回到模式 Tab（不关闭弹窗）
                    KeyEvent.KEYCODE_BACK -> {
                        runCatching { modeTabFocusRequester.requestFocus() }
                        true
                    }

                    else -> false
                }
            }
            .focusRequester(contentFocusRequester)
            .focusProperties {
                up = modeTabFocusRequester
            }
            .focusable()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = statusText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.border.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (qrContent.isNotBlank()) {
                QrImage(
                    modifier = Modifier.fillMaxSize(),
                    content = qrContent,
                    borderWidth = 16.dp,
                    shape = RoundedCornerShape(12.dp),
                )
            } else {
                Text(
                    text = "生成中…",
                    color = Color.DarkGray,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (qrContent.isNotBlank()) {
            Text(
                text = qrContent,
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
            )
        }

        Text(
            text = "手机与电视需在同一局域网 ｜ 返回回到模式 ｜ 模式上再返回关闭",
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun GeetestTvVerifyContent(
    gt: String,
    challenge: String,
    mockMode: Boolean = false,
    contentFocusRequester: FocusRequester,
    modeTabFocusRequester: FocusRequester,
    onResult: (GeetestTvResult) -> Unit,
) {
    val density = LocalDensity.current
    val callbackScope = rememberCoroutineScope()

    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    var containerHeightPx by remember { mutableFloatStateOf(0f) }

    var cursorX by remember { mutableFloatStateOf(0f) }
    var cursorY by remember { mutableFloatStateOf(0f) }
    var cursorInitialized by remember { mutableStateOf(false) }

    var statusText by remember {
        mutableStateOf(if (mockMode) "Mock：请用十字光标点下方绿色区域" else "正在加载验证码…")
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var overlayRef by remember { mutableStateOf<CrosshairOverlayView?>(null) }

    val baseStep = with(density) { 6.dp.toPx() }
    val fastStep = with(density) { 20.dp.toPx() }

    LaunchedEffect(containerWidthPx, containerHeightPx) {
        if (containerWidthPx > 0 && containerHeightPx > 0 && !cursorInitialized) {
            cursorX = containerWidthPx / 2f
            cursorY = containerHeightPx / 2f
            cursorInitialized = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { wv ->
                runCatching {
                    wv.removeJavascriptInterface("Android")
                    wv.stopLoading()
                    wv.destroy()
                }
            }
        }
    }

    fun clampCursor() {
        cursorX = cursorX.coerceIn(0f, containerWidthPx.coerceAtLeast(0f))
        cursorY = cursorY.coerceIn(0f, containerHeightPx.coerceAtLeast(0f))
    }

    fun moveCursor(dx: Float, dy: Float, fast: Boolean) {
        val step = if (fast) fastStep else baseStep
        cursorX += dx * step
        cursorY += dy * step
        clampCursor()
    }

    fun dispatchClickToWebView() {
        val wv = webViewRef ?: return
        val now = SystemClock.uptimeMillis()
        val x = cursorX
        val y = cursorY
        val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0)
        val up = MotionEvent.obtain(now, now + 50, MotionEvent.ACTION_UP, x, y, 0)
        wv.dispatchTouchEvent(down)
        wv.dispatchTouchEvent(up)
        down.recycle()
        up.recycle()
        logger.debug { "Dispatched click at ($x, $y)" }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                val isDown = event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN
                val isLongPress = event.nativeKeyEvent.repeatCount > 0
                val keyCode = event.nativeKeyEvent.keyCode

                if (!isDown) return@onPreviewKeyEvent false

                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        moveCursor(0f, -1f, isLongPress); true
                    }

                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        moveCursor(0f, 1f, isLongPress); true
                    }

                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        moveCursor(-1f, 0f, isLongPress); true
                    }

                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        moveCursor(1f, 0f, isLongPress); true
                    }

                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        dispatchClickToWebView(); true
                    }

                    // 返回：焦点回到模式 Tab（不关闭弹窗）
                    KeyEvent.KEYCODE_BACK -> {
                        runCatching { modeTabFocusRequester.requestFocus() }
                        true
                    }

                    else -> false
                }
            }
            .focusRequester(contentFocusRequester)
            .focusProperties {
                up = modeTabFocusRequester
            }
            .focusable(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = statusText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 4.dp),
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(8.dp)),
                factory = { ctx ->
                    val webView = WebView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        isFocusable = false
                        isFocusableInTouchMode = false
                        webChromeClient = WebChromeClient()

                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun onGeetestResult(
                                    validate: String?,
                                    seccode: String?,
                                    geetestChallenge: String?,
                                ) {
                                    val v = validate.orEmpty().trim()
                                    val s = seccode.orEmpty().trim()
                                    val c = geetestChallenge.orEmpty().trim()
                                    if (v.isBlank() || s.isBlank() || c.isBlank()) return
                                    logger.info { "Geetest verification succeeded" }
                                    // JavascriptInterface 在 WebView 私有后台线程回调。
                                    // 切回 Compose 主线程，避免丢失待验证令牌或跨线程更新状态。
                                    callbackScope.launch {
                                        onResult(
                                            GeetestTvResult(
                                                challenge = c,
                                                validate = v,
                                                seccode = s
                                            )
                                        )
                                    }
                                }

                                @JavascriptInterface
                                fun onStatusUpdate(text: String?) {
                                    val message = text ?: return
                                    callbackScope.launch { statusText = message }
                                }
                            },
                            "Android"
                        )

                        loadDataWithBaseURL(
                            "https://api.bilibili.com/",
                            if (mockMode) buildMockGeetestHtml() else buildGeetestHtml(gt, challenge),
                            "text/html",
                            "utf-8",
                            null,
                        )

                        webViewRef = this
                    }

                    val overlay = CrosshairOverlayView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        overlayRef = this
                    }

                    FrameLayout(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        addView(webView)
                        addView(overlay)
                    }
                },
                update = { _ ->
                    val wv = webViewRef ?: return@AndroidView
                    wv.post {
                        if (wv.width > 0 && wv.height > 0) {
                            containerWidthPx = wv.width.toFloat()
                            containerHeightPx = wv.height.toFloat()
                        }
                    }
                    overlayRef?.setCursorPosition(cursorX, cursorY)
                },
            )
        }

        Text(
            text = "方向键移动光标 ｜ 确认键点击 ｜ 返回回到模式 ｜ 模式上再返回关闭",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * 原生 View 十字光标覆盖层，渲染在 WebView 之上。
 */
private class CrosshairOverlayView(context: Context) : View(context) {
    private var cx = 0f
    private var cy = 0f

    private val density = context.resources.displayMetrics.density
    private val armLen = 18f * density
    private val gap = 5f * density
    private val strokeW = 2f * density
    private val shadowW = 3.5f * density
    private val dotRadius = 3f * density
    private val dotShadowRadius = 4f * density
    private val circleRadius = armLen + gap

    private val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = strokeW
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x99000000.toInt()
        style = Paint.Style.STROKE
        strokeWidth = shadowW
    }
    private val dotWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.FILL
    }
    private val dotShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x99000000.toInt()
        style = Paint.Style.FILL
    }
    private val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xB3FFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        pathEffect = DashPathEffect(floatArrayOf(6f * density, 4f * density), 0f)
    }

    fun setCursorPosition(x: Float, y: Float) {
        cx = x
        cy = y
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cx == 0f && cy == 0f) return

        canvas.drawLine(cx, cy - gap - armLen, cx, cy - gap, shadowPaint)
        canvas.drawLine(cx, cy + gap, cx, cy + gap + armLen, shadowPaint)
        canvas.drawLine(cx - gap - armLen, cy, cx - gap, cy, shadowPaint)
        canvas.drawLine(cx + gap, cy, cx + gap + armLen, cy, shadowPaint)

        canvas.drawLine(cx, cy - gap - armLen, cx, cy - gap, whitePaint)
        canvas.drawLine(cx, cy + gap, cx, cy + gap + armLen, whitePaint)
        canvas.drawLine(cx - gap - armLen, cy, cx - gap, cy, whitePaint)
        canvas.drawLine(cx + gap, cy, cx + gap + armLen, cy, whitePaint)

        canvas.drawCircle(cx, cy, dotShadowRadius, dotShadowPaint)
        canvas.drawCircle(cx, cy, dotRadius, dotWhitePaint)

        canvas.drawCircle(cx, cy, circleRadius, dashPaint)
    }
}

/**
 * Debug mock：无真实极验。页面上有大块可点击目标，遥控器移到目标后按确认即可回调成功。
 */
private fun buildMockGeetestHtml(): String {
    return """
<!DOCTYPE html>
<html>
<head>
  <meta name="viewport" content="width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no"/>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    html, body {
      width: 100%; height: 100%;
      background: #1a1d24;
      font-family: sans-serif;
      color: #eee;
      overflow: hidden;
    }
    body {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 16px;
      gap: 16px;
    }
    h2 { font-size: 16px; font-weight: 600; }
    p { font-size: 13px; color: #9aa; text-align: center; }
    #target {
      width: 160px; height: 160px;
      border-radius: 16px;
      background: linear-gradient(145deg, #3dd68c, #1f9d62);
      display: flex; align-items: center; justify-content: center;
      font-size: 18px; font-weight: 700; color: #062814;
      box-shadow: 0 8px 24px rgba(61, 214, 140, 0.35);
      user-select: none;
    }
    #target:active { transform: scale(0.96); }
    #hint { font-size: 12px; color: #6af; }
  </style>
</head>
<body>
  <h2>Mock 点选目标</h2>
  <p>用方向键移动十字光标到绿色方块，按确认键</p>
  <div id="target">点我</div>
  <div id="hint">不请求极验服务，仅测遥控器注入点击</div>
  <script>
    (function() {
      function success() {
        try { window.Android.onStatusUpdate('Mock 验证成功，正在回调…'); } catch(e) {}
        try {
          window.Android.onGeetestResult(
            'mock_validate',
            'mock_validate|jordan',
            'mock_challenge'
          );
        } catch(e) {}
      }
      var el = document.getElementById('target');
      el.addEventListener('click', success);
      el.addEventListener('touchend', function(e) { e.preventDefault(); success(); });
      try { window.Android.onStatusUpdate('Mock：请用十字光标点绿色区域'); } catch(e) {}
    })();
  </script>
</body>
</html>
    """.trimIndent()
}

internal fun buildGeetestHtml(gt: String, challenge: String): String {
    val safeGt = gt.replace("\\", "\\\\").replace("'", "\\'").replace("<", "&lt;")
    val safeChallenge = challenge.replace("\\", "\\\\").replace("'", "\\'").replace("<", "&lt;")
    return """
<!DOCTYPE html>
<html>
<head>
  <meta name="viewport" content="width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no"/>
  <script src="https://static.geetest.com/static/tools/gt.js"></script>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    html, body {
      width: 100%;
      height: 100%;
      background: transparent;
      font-family: sans-serif;
      overflow: hidden;
    }
    body {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 4px;
    }
    #captcha {
      width: 100%;
      min-height: 300px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    /*
     * bind 模式会把验证面板按 WebView 视口居中。不能改成 relative：
     * 极验保留的 top/left 偏移会叠加到普通文档流位置，导致面板下移并被裁切。
     */
    .geetest_panel { position: fixed !important; }
    .geetest_panel_box { position: absolute !important; }
    .geetest_panel_ghost,
    .geetest_close { display: none !important; }
  </style>
</head>
<body>
  <div id="captcha"></div>
  <script>
    (function() {
      function notify(msg) {
        try { window.Android.onStatusUpdate(msg); } catch(e) {}
      }
      if (typeof initGeetest !== 'function') {
        notify('验证码脚本加载失败，请检查网络');
        return;
      }
      notify('正在初始化验证…');
      initGeetest({
        gt: '$safeGt',
        challenge: '$safeChallenge',
        new_captcha: true,
        product: 'bind',
        offline: false,
        https: true
      }, function(captchaObj) {
        captchaObj.appendTo('#captcha');
        captchaObj.onReady(function() {
          notify('请使用方向键移动光标，确认键点击');
          captchaObj.verify();
        });
        captchaObj.onSuccess(function() {
          var res = captchaObj.getValidate();
          if (!res) return;
          notify('验证成功，正在提交…');
          try {
            window.Android.onGeetestResult(
              res.geetest_validate,
              res.geetest_seccode,
              res.geetest_challenge
            );
          } catch(e) {}
        });
        captchaObj.onError(function(e) {
          notify('验证出错：' + (e && (e.msg || e.error_code) || '未知错误'));
        });
        captchaObj.onClose(function() {
          notify('验证已关闭，正在重新打开…');
          setTimeout(function() { captchaObj.verify(); }, 500);
        });
      });
    })();
  </script>
</body>
</html>
    """.trimIndent()
}
