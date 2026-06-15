package dev.aaa1115910.biliapi.http.util

import java.util.Calendar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoginCryptoTest {
    @Test
    fun `login device id contains bcd timestamp and checksum`() {
        val timestampMillis = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 15, 12, 34, 56)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val deviceId = generateLoginDeviceId(timestampMillis)
        val bytes = deviceId.chunked(2).map { it.toInt(16) }
        val checksum = bytes.dropLast(1).sum() and 0xff

        assertEquals(64, deviceId.length)
        assertTrue(deviceId.matches(Regex("[0-9a-f]{64}")))
        assertEquals("20260615123456", deviceId.substring(32, 46))
        assertEquals(checksum, bytes.last())
    }
}
