package dev.aaa1115910.bv.viewmodel.login

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SmsLoginViewModelTest {
    @Test
    fun parseSmsCaptchaUrlReadsAndDecodesAllParameters() {
        val captcha = parseSmsCaptchaUrl(
            "https://passport.bilibili.com/recaptcha" +
                "?recaptcha_token=token-1&gee_gt=gt-1&gee_challenge=challenge%2B1%2F2"
        )

        assertEquals("token-1", captcha?.token)
        assertEquals("gt-1", captcha?.gt)
        assertEquals("challenge+1/2", captcha?.challenge)
    }

    @Test
    fun parseSmsCaptchaUrlRejectsIncompleteParameters() {
        assertNull(
            parseSmsCaptchaUrl(
                "https://passport.bilibili.com/recaptcha?gee_gt=gt-1&gee_challenge=challenge-1"
            )
        )
    }
}
