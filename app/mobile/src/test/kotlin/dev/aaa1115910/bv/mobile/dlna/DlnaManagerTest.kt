package dev.aaa1115910.bv.mobile.dlna

import kotlin.test.Test
import kotlin.test.assertEquals

class DlnaManagerTest {
    @Test
    fun `SSDP headers are parsed case-insensitively`() {
        val headers = DlnaManager.parseSsdpHeaders(
            """
            HTTP/1.1 200 OK
            LOCATION: http://192.168.1.10:1400/xml/device_description.xml
            UsN: uuid:renderer::urn:schemas-upnp-org:device:MediaRenderer:1
            CACHE-CONTROL: max-age=1800
            """.trimIndent()
        )

        assertEquals(
            "http://192.168.1.10:1400/xml/device_description.xml",
            headers["location"]
        )
        assertEquals(
            "uuid:renderer::urn:schemas-upnp-org:device:MediaRenderer:1",
            headers["usn"]
        )
    }

    @Test
    fun `DLNA seek time supports long videos`() {
        assertEquals("00:00:00", DlnaManager.formatDlnaTime(-1))
        assertEquals("01:02:03", DlnaManager.formatDlnaTime(3_723_999))
        assertEquals("27:00:00", DlnaManager.formatDlnaTime(97_200_000))
    }

    @Test
    fun `XML values are escaped for SOAP`() {
        assertEquals(
            "&lt;A &amp; &quot;B&quot; &apos;C&apos;&gt;",
            DlnaManager.escapeXml("<A & \"B\" 'C'>")
        )
    }
}
