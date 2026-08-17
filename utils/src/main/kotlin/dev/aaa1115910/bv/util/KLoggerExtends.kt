package dev.aaa1115910.bv.util

import dev.aaa1115910.bv.utils.BuildConfig
import io.github.oshai.kotlinlogging.KLogger

fun KLogger.fInfo(msg: () -> Any?) {
    info(msg)
    firebaseLog("[Info] ${msg.toStringSafe()}")
}

fun KLogger.fWarn(msg: () -> Any?) {
    warn(msg)
    firebaseLog("[Warn] ${msg.toStringSafe()}")
}

fun KLogger.fDebug(msg: () -> Any?) {
    if (BuildConfig.DEBUG) {
        info(msg)
        firebaseLog("[Debug] ${msg.toStringSafe()}")
    }
}

fun KLogger.fError(msg: () -> Any?) {
    error(msg)
    firebaseLog("[Error] ${msg.toStringSafe()}")
}

fun KLogger.fException(throwable: Throwable, msg: () -> Any?) {
    // Coroutine cancellation is expected control flow, not an actionable failure.
    if (throwable.isCancellationFailure()) return
    // Connectivity failures are recoverable and should not pollute Crashlytics non-fatals.
    if (throwable.isExpectedNetworkFailure()) {
        warn { "$msg: ${throwable.localizedMessage}" }
        return
    }
    warn { "$msg: ${throwable.stackTraceToString()}" }
    firebaseLog("[Exception] ${msg.toStringSafe()}: ${throwable.localizedMessage}")
    FirebaseUtil.recordException(throwable)
}

private fun firebaseLog(msg: String) {
    FirebaseUtil.log(msg)
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun (() -> Any?).toStringSafe(): String {
    return try {
        invoke().toString()
    } catch (e: Exception) {
        ErrorMessageProducer.getErrorLog(e)
    }
}

internal object ErrorMessageProducer {
    fun getErrorLog(e: Exception): String {
        if (System.getProperties().containsKey("kotlin-logging.throwOnMessageError")) {
            throw e
        } else {
            return "Log message invocation failed: $e"
        }
    }
}
