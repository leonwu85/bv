package dev.aaa1115910.bv.player.impl.mpv

import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MpvCaBundleTest {
    @Test
    fun pemHasMarkersAndSixtyFourColumnLines() {
        val der = ByteArray(200) { (it * 7).toByte() }
        val pem = MpvCaBundle.toPem(der)
        val lines = pem.trimEnd().lines()

        assertEquals("-----BEGIN CERTIFICATE-----", lines.first())
        assertEquals("-----END CERTIFICATE-----", lines.last())
        val body = lines.subList(1, lines.size - 1)
        assertTrue(body.isNotEmpty())
        body.dropLast(1).forEach { assertEquals(64, it.length, "line '$it' must be 64 columns") }
        assertTrue(body.last().length in 1..64)
        assertTrue(pem.endsWith("\n"), "bundle entries must be newline terminated so they can be concatenated")
    }

    @Test
    fun pemBodyRoundTripsToOriginalDer() {
        val der = ByteArray(1000) { (it % 251).toByte() }
        val body = MpvCaBundle.toPem(der)
            .lines()
            .filter { it.isNotBlank() && !it.startsWith("-----") }
            .joinToString("")
        assertContentEquals(der, Base64.Default.decode(body))
    }
}
