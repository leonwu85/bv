package dev.aaa1115910.bv.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeetestChallengeRefreshTest {
    @Test
    fun acceptsAndNormalizesNewVoucher() {
        val attempted = mutableSetOf("voucher_old")
        assertEquals(
            "voucher_new",
            reserveFreshVVoucher(
                attemptedVVouchers = attempted,
                candidate = "  voucher_new  ",
            )
        )
        assertEquals(setOf("voucher_old", "voucher_new"), attempted)
    }

    @Test
    fun rejectsOldVoucherAfterAProgressionToANewerVoucher() {
        val attempted = mutableSetOf<String>()
        reserveFreshVVoucher(
            attemptedVVouchers = attempted,
            candidate = "voucher_old",
        )
        reserveFreshVVoucher(
            attemptedVVouchers = attempted,
            candidate = "voucher_newer",
        )
        assertFailsWith<IllegalStateException> {
            reserveFreshVVoucher(
                attemptedVVouchers = attempted,
                candidate = " voucher_old ",
            )
        }
        assertEquals(setOf("voucher_old", "voucher_newer"), attempted)
    }

    @Test
    fun rejectsBlankVoucher() {
        assertFailsWith<IllegalStateException> {
            reserveFreshVVoucher(
                attemptedVVouchers = mutableSetOf("voucher_used"),
                candidate = "   ",
            )
        }
    }

    @Test
    fun acceptsResultChallengeThatDiffersFromRegistrationChallenge() {
        assertEquals(
            expected = "result-refreshed",
            actual = validatedGeetestResultChallengeOrNull(
                expectedSourceChallenge = "registration",
                sourceChallenge = "registration",
                resultChallenge = " result-refreshed ",
            )
        )
    }

    @Test
    fun rejectsResultFromReplacedRegistrationChallenge() {
        assertEquals(
            expected = null,
            actual = validatedGeetestResultChallengeOrNull(
                expectedSourceChallenge = "registration-new",
                sourceChallenge = "registration-old",
                resultChallenge = "result-old",
            )
        )
    }

    @Test
    fun acceptsRegistrationOnlyForCurrentPlaybackSessionAndGeneration() {
        assertTrue(
            isCurrentGeetestRegistration(
                registrationGeneration = 7,
                currentRegistrationGeneration = 7,
                playbackSessionToken = 12,
                currentPlaybackSessionToken = 12,
            )
        )
        assertFalse(
            isCurrentGeetestRegistration(
                registrationGeneration = 7,
                currentRegistrationGeneration = 7,
                playbackSessionToken = 11,
                currentPlaybackSessionToken = 12,
            )
        )
        assertFalse(
            isCurrentGeetestRegistration(
                registrationGeneration = 6,
                currentRegistrationGeneration = 7,
                playbackSessionToken = 12,
                currentPlaybackSessionToken = 12,
            )
        )
    }
}
