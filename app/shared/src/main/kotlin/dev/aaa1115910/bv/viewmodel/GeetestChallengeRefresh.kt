package dev.aaa1115910.bv.viewmodel

internal class VVoucherAlreadyAttemptedException :
    IllegalStateException("播放接口返回了已使用的 v_voucher")

/**
 * Reserves a voucher before Gaia registration. A reservation is intentionally never rolled back:
 * a timed-out or cancelled request may still have consumed the one-shot voucher on the server.
 */
internal fun reserveFreshVVoucher(
    attemptedVVouchers: MutableSet<String>,
    candidate: String,
): String {
    val normalized = candidate.trim()
    check(normalized.isNotEmpty()) { "播放接口返回了空的 v_voucher" }
    if (!attemptedVVouchers.add(normalized)) {
        throw VVoucherAlreadyAttemptedException()
    }
    return normalized
}

internal fun validatedGeetestResultChallengeOrNull(
    expectedSourceChallenge: String,
    sourceChallenge: String?,
    resultChallenge: String,
): String? {
    val normalizedResult = resultChallenge.trim()
    if (normalizedResult.isEmpty()) return null
    val normalizedSource = sourceChallenge?.trim()
    if (normalizedSource != null && normalizedSource != expectedSourceChallenge.trim()) {
        return null
    }
    return normalizedResult
}

internal fun isCurrentGeetestRegistration(
    registrationGeneration: Long,
    currentRegistrationGeneration: Long,
    playbackSessionToken: Long,
    currentPlaybackSessionToken: Long,
): Boolean =
    registrationGeneration == currentRegistrationGeneration &&
        playbackSessionToken == currentPlaybackSessionToken
