package dev.aaa1115910.bv.util

import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val FocusRequestRetryDelayMillis = 16L

/**
 * Requests focus from a structured coroutine and retries once after approximately one frame.
 *
 * The returned value lets callers distinguish a rejected request from a successful one. Cancellation
 * remains owned by the caller (for example, a keyed `LaunchedEffect`).
 */
suspend fun FocusRequester.requestFocusWithRetry(): Boolean = withContext(Dispatchers.Main.immediate) {
    fun requestOrFalse(): Boolean = try {
        requestFocus()
    } catch (exception: IllegalStateException) {
        false
    }

    if (requestOrFalse()) return@withContext true

    delay(FocusRequestRetryDelayMillis)
    requestOrFalse().also { focused ->
        if (!focused) {
            println("Focus request failed after retry")
        }
    }
}

/**
 * 改进的请求焦点的方法，确保线程安全
 * 使用Immediate dispatcher避免不必要的调度延迟
 */
fun FocusRequester.requestFocus(scope: CoroutineScope) {
    scope.launch {
        requestFocusWithRetry()
    }
}
