package dev.aaa1115910.biliapi.repositories

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TryLook1080PTest {
    @Test
    fun tryLookIsOnlyEnabledForAnonymousWebRequests() {
        assertTrue(shouldTryLook1080P(enabled = true, sessionData = null))
        assertTrue(shouldTryLook1080P(enabled = true, sessionData = ""))
        assertFalse(shouldTryLook1080P(enabled = false, sessionData = null))
        assertFalse(shouldTryLook1080P(enabled = true, sessionData = "sess"))
    }
}

