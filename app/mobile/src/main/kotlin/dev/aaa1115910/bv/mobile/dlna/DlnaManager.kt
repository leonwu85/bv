package dev.aaa1115910.bv.mobile.dlna

import android.content.Context
import android.net.wifi.WifiManager
import android.os.SystemClock
import android.util.Xml
import dev.aaa1115910.bv.viewmodel.DlnaMediaSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale

data class DlnaDevice(
    val id: String,
    val name: String,
    val descriptionUrl: String,
    val avTransportControlUrl: String,
    val avTransportServiceType: String,
)

class DlnaManager(context: Context) {
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    suspend fun discoverDevices(timeoutMillis: Long = DEFAULT_DISCOVERY_TIMEOUT_MS): List<DlnaDevice> =
        coroutineScope {
            val candidates = withContext(Dispatchers.IO) {
                discoverSsdpCandidates(timeoutMillis.coerceIn(1_000L, 10_000L))
            }
            candidates
                .take(MAX_DESCRIPTION_REQUESTS)
                .map { candidate ->
                    async(Dispatchers.IO) {
                        try {
                            resolveDevice(candidate)
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Throwable) {
                            null
                        }
                    }
                }
                .awaitAll()
                .filterNotNull()
                .distinctBy(DlnaDevice::id)
                .sortedBy { it.name.lowercase(Locale.getDefault()) }
        }

    suspend fun cast(device: DlnaDevice, source: DlnaMediaSource) {
        require(source.url.startsWith("http://") || source.url.startsWith("https://")) {
            "投屏地址必须是 HTTP 或 HTTPS 链接"
        }
        setAvTransportUri(device, source)
        play(device)
        if (source.positionMs >= MIN_RESUME_POSITION_MS) {
            delay(SEEK_AFTER_PLAY_DELAY_MS)
            try {
                seek(device, source.positionMs)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                // Seeking is optional and not implemented consistently across renderers.
            }
        }
    }

    suspend fun play(device: DlnaDevice) {
        sendAvTransportAction(
            device = device,
            action = "Play",
            arguments = "<InstanceID>0</InstanceID><Speed>1</Speed>",
        )
    }

    suspend fun pause(device: DlnaDevice) {
        sendAvTransportAction(
            device = device,
            action = "Pause",
            arguments = "<InstanceID>0</InstanceID>",
        )
    }

    suspend fun stop(device: DlnaDevice) {
        sendAvTransportAction(
            device = device,
            action = "Stop",
            arguments = "<InstanceID>0</InstanceID>",
        )
    }

    suspend fun seek(device: DlnaDevice, positionMs: Long) {
        sendAvTransportAction(
            device = device,
            action = "Seek",
            arguments = buildString {
                append("<InstanceID>0</InstanceID>")
                append("<Unit>REL_TIME</Unit>")
                append("<Target>")
                append(formatDlnaTime(positionMs))
                append("</Target>")
            },
        )
    }

    private suspend fun setAvTransportUri(device: DlnaDevice, source: DlnaMediaSource) {
        val metadata = buildDidlLiteMetadata(source)
        sendAvTransportAction(
            device = device,
            action = "SetAVTransportURI",
            arguments = buildString {
                append("<InstanceID>0</InstanceID>")
                append("<CurrentURI>")
                append(escapeXml(source.url))
                append("</CurrentURI>")
                append("<CurrentURIMetaData>")
                append(escapeXml(metadata))
                append("</CurrentURIMetaData>")
            },
        )
    }

    private suspend fun discoverSsdpCandidates(timeoutMillis: Long): List<SsdpCandidate> {
        val multicastLock = wifiManager
            ?.createMulticastLock(MULTICAST_LOCK_TAG)
            ?.apply { setReferenceCounted(false) }
        var lockAcquired = false

        try {
            if (multicastLock != null) {
                try {
                    multicastLock.acquire()
                    lockAcquired = true
                } catch (error: SecurityException) {
                    throw IllegalStateException("无法开启局域网设备发现，请检查 Wi-Fi 权限", error)
                }
            }

            return DatagramSocket().use { socket ->
                socket.reuseAddress = true
                socket.broadcast = true
                val address = InetAddress.getByName(SSDP_HOST)
                SSDP_SEARCH_TARGETS.forEach { searchTarget ->
                    currentCoroutineContext().ensureActive()
                    val request = buildSsdpSearchRequest(searchTarget)
                        .toByteArray(StandardCharsets.UTF_8)
                    socket.send(DatagramPacket(request, request.size, address, SSDP_PORT))
                }

                val deadline = SystemClock.elapsedRealtime() + timeoutMillis
                val candidatesByLocation = linkedMapOf<String, SsdpCandidate>()
                while (SystemClock.elapsedRealtime() < deadline) {
                    currentCoroutineContext().ensureActive()
                    val remaining = deadline - SystemClock.elapsedRealtime()
                    socket.soTimeout = remaining
                        .coerceAtMost(SSDP_RECEIVE_POLL_MS)
                        .coerceAtLeast(1L)
                        .toInt()
                    val buffer = ByteArray(SSDP_RESPONSE_BUFFER_SIZE)
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                    } catch (_: SocketTimeoutException) {
                        continue
                    }
                    val response = String(
                        packet.data,
                        packet.offset,
                        packet.length,
                        StandardCharsets.UTF_8,
                    )
                    val headers = parseSsdpHeaders(response)
                    val location = headers["location"]?.trim().orEmpty()
                    if (location.startsWith("http://") || location.startsWith("https://")) {
                        if (location !in candidatesByLocation) {
                            candidatesByLocation[location] = SsdpCandidate(
                                location = location,
                                usn = headers["usn"]?.trim().orEmpty(),
                            )
                        }
                    }
                }
                candidatesByLocation.values.toList()
            }
        } finally {
            if (lockAcquired && multicastLock?.isHeld == true) {
                multicastLock.release()
            }
        }
    }

    private suspend fun resolveDevice(candidate: SsdpCandidate): DlnaDevice? {
        currentCoroutineContext().ensureActive()
        val connection = openHttpConnection(candidate.location).apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/xml,text/xml,*/*")
        }
        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) return null
            val description = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use {
                it.readText()
            }
            currentCoroutineContext().ensureActive()
            val parsed = parseDeviceDescription(description, candidate.location) ?: return null
            if (!parsed.deviceType.contains("MediaRenderer", ignoreCase = true)) return null
            return DlnaDevice(
                id = candidate.usn.ifBlank { candidate.location },
                name = parsed.friendlyName.ifBlank {
                    runCatching { URI(candidate.location).host }.getOrNull()
                        ?: "DLNA 设备"
                },
                descriptionUrl = candidate.location,
                avTransportControlUrl = parsed.avTransportControlUrl,
                avTransportServiceType = parsed.avTransportServiceType,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun parseDeviceDescription(xml: String, descriptionUrl: String): ParsedDeviceDescription? {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            runCatching { setFeature("http://xmlpull.org/v1/doc/features.html#process-docdecl", false) }
            setInput(xml.reader())
        }

        var deviceType = ""
        var friendlyName = ""
        var urlBase = ""
        var insideService = false
        var serviceType = ""
        var controlUrl = ""
        var avTransportControlUrl = ""
        var avTransportServiceType = ""

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name.lowercase(Locale.ROOT)) {
                    "devicetype" -> if (deviceType.isBlank()) {
                        deviceType = parser.nextText().trim()
                    }

                    "friendlyname" -> if (friendlyName.isBlank()) {
                        friendlyName = parser.nextText().trim()
                    }

                    "urlbase" -> urlBase = parser.nextText().trim()
                    "service" -> {
                        insideService = true
                        serviceType = ""
                        controlUrl = ""
                    }

                    "servicetype" -> if (insideService) {
                        serviceType = parser.nextText().trim()
                    }

                    "controlurl" -> if (insideService) {
                        controlUrl = parser.nextText().trim()
                    }
                }

                XmlPullParser.END_TAG -> if (
                    parser.name.equals("service", ignoreCase = true) && insideService
                ) {
                    if (
                        avTransportControlUrl.isBlank() &&
                        serviceType.contains(":service:AVTransport:", ignoreCase = true) &&
                        controlUrl.isNotBlank()
                    ) {
                        avTransportControlUrl = resolveUrl(
                            baseUrl = urlBase.ifBlank { descriptionUrl },
                            value = controlUrl,
                        ).orEmpty()
                        avTransportServiceType = serviceType
                    }
                    insideService = false
                }
            }
            parser.next()
        }

        if (avTransportControlUrl.isBlank()) return null
        return ParsedDeviceDescription(
            deviceType = deviceType,
            friendlyName = friendlyName,
            avTransportControlUrl = avTransportControlUrl,
            avTransportServiceType = avTransportServiceType,
        )
    }

    private suspend fun sendAvTransportAction(
        device: DlnaDevice,
        action: String,
        arguments: String,
    ) = withContext(Dispatchers.IO) {
        currentCoroutineContext().ensureActive()
        val body = buildSoapEnvelope(
            action = action,
            arguments = arguments,
            serviceType = device.avTransportServiceType,
        )
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val connection = openHttpConnection(device.avTransportControlUrl).apply {
            requestMethod = "POST"
            doOutput = true
            useCaches = false
            setFixedLengthStreamingMode(bytes.size)
            setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
            setRequestProperty(
                "SOAPACTION",
                "\"${device.avTransportServiceType}#$action\""
            )
            setRequestProperty("Connection", "close")
        }
        try {
            connection.outputStream.use { it.write(bytes) }
            currentCoroutineContext().ensureActive()
            val responseCode = connection.responseCode
            val responseBody = if (responseCode in 200..299) {
                connection.inputStream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }
            }.orEmpty()
            if (responseCode !in 200..299) {
                val fault = parseSoapFault(responseBody)
                throw IllegalStateException(
                    fault.ifBlank { "设备执行 $action 失败（HTTP $responseCode）" }
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun openHttpConnection(url: String): HttpURLConnection {
        val parsed = URL(url)
        require(parsed.protocol == "http" || parsed.protocol == "https") {
            "不支持的设备地址协议"
        }
        return (parsed.openConnection() as HttpURLConnection).apply {
            connectTimeout = HTTP_CONNECT_TIMEOUT_MS
            readTimeout = HTTP_READ_TIMEOUT_MS
            instanceFollowRedirects = true
        }
    }

    private data class SsdpCandidate(
        val location: String,
        val usn: String,
    )

    private data class ParsedDeviceDescription(
        val deviceType: String,
        val friendlyName: String,
        val avTransportControlUrl: String,
        val avTransportServiceType: String,
    )

    companion object {
        private const val SSDP_HOST = "239.255.255.250"
        private const val SSDP_PORT = 1900
        private const val SSDP_RESPONSE_BUFFER_SIZE = 8 * 1024
        private const val SSDP_RECEIVE_POLL_MS = 400L
        private const val DEFAULT_DISCOVERY_TIMEOUT_MS = 4_000L
        private const val MAX_DESCRIPTION_REQUESTS = 24
        private const val HTTP_CONNECT_TIMEOUT_MS = 4_000
        private const val HTTP_READ_TIMEOUT_MS = 5_000
        private const val MIN_RESUME_POSITION_MS = 1_000L
        private const val SEEK_AFTER_PLAY_DELAY_MS = 350L
        private const val MULTICAST_LOCK_TAG = "bv-dlna-discovery"
        private const val AV_TRANSPORT_SERVICE_TYPE =
            "urn:schemas-upnp-org:service:AVTransport:1"

        private val SSDP_SEARCH_TARGETS = listOf(
            "urn:schemas-upnp-org:device:MediaRenderer:1",
            "urn:schemas-upnp-org:device:MediaRenderer:2",
            "ssdp:all",
        )

        private fun buildSsdpSearchRequest(searchTarget: String): String = buildString {
            append("M-SEARCH * HTTP/1.1\r\n")
            append("HOST: $SSDP_HOST:$SSDP_PORT\r\n")
            append("MAN: \"ssdp:discover\"\r\n")
            append("MX: 2\r\n")
            append("ST: $searchTarget\r\n")
            append("\r\n")
        }

        internal fun parseSsdpHeaders(response: String): Map<String, String> =
            response.lineSequence()
                .drop(1)
                .mapNotNull { line ->
                    val separator = line.indexOf(':')
                    if (separator <= 0) return@mapNotNull null
                    line.substring(0, separator).trim().lowercase(Locale.ROOT) to
                        line.substring(separator + 1).trim()
                }
                .toMap()

        private fun resolveUrl(baseUrl: String, value: String): String? = runCatching {
            URI(baseUrl).resolve(value).toString().takeIf {
                it.startsWith("http://") || it.startsWith("https://")
            }
        }.getOrNull()

        private fun buildSoapEnvelope(
            action: String,
            arguments: String,
            serviceType: String = AV_TRANSPORT_SERVICE_TYPE,
        ): String =
            """<?xml version="1.0" encoding="utf-8"?>""" +
                """<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" """ +
                """s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">""" +
                """<s:Body><u:$action xmlns:u="$serviceType">""" +
                arguments +
                """</u:$action></s:Body></s:Envelope>"""

        private fun buildDidlLiteMetadata(source: DlnaMediaSource): String =
            """<DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" """ +
                """xmlns:dc="http://purl.org/dc/elements/1.1/" """ +
                """xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/">""" +
                """<item id="0" parentID="0" restricted="1">""" +
                """<dc:title>${escapeXml(source.displayTitle)}</dc:title>""" +
                """<upnp:class>object.item.videoItem</upnp:class>""" +
                """<res protocolInfo="http-get:*:video/mp4:*">${escapeXml(source.url)}</res>""" +
                """</item></DIDL-Lite>"""

        internal fun escapeXml(value: String): String = buildString(value.length) {
            value.forEach { character ->
                append(
                    when (character) {
                        '&' -> "&amp;"
                        '<' -> "&lt;"
                        '>' -> "&gt;"
                        '"' -> "&quot;"
                        '\'' -> "&apos;"
                        else -> character
                    }
                )
            }
        }

        internal fun formatDlnaTime(positionMs: Long): String {
            val totalSeconds = positionMs.coerceAtLeast(0L) / 1_000L
            val hours = totalSeconds / 3_600L
            val minutes = totalSeconds % 3_600L / 60L
            val seconds = totalSeconds % 60L
            return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
        }

        private fun parseSoapFault(response: String): String {
            val match = Regex(
                """<(?:\w+:)?errorDescription(?:\s[^>]*)?>(.*?)</(?:\w+:)?errorDescription>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            ).find(response)
            return match?.groupValues?.getOrNull(1)
                ?.replace("&lt;", "<")
                ?.replace("&gt;", ">")
                ?.replace("&quot;", "\"")
                ?.replace("&apos;", "'")
                ?.replace("&amp;", "&")
                ?.trim()
                .orEmpty()
        }
    }
}
