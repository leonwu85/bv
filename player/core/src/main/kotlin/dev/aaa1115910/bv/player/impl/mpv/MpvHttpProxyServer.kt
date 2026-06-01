package dev.aaa1115910.bv.player.impl.mpv

import android.content.Context
import dev.aaa1115910.bv.player.OkHttpUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Closeable
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.URI
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object MpvHttpProxyServer {
    private val logger = KotlinLogging.logger { }
    private val executor = Executors.newCachedThreadPool()
    private val proxyItems = ConcurrentHashMap<String, ProxyItem>()

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var clientContext: Context? = null

    fun register(
        context: Context,
        url: String,
        headers: Map<String, String>,
        userAgent: String?,
        referer: String?
    ): ProxiedUrl {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return ProxiedUrl(url, null)
        }

        clientContext = context.applicationContext
        val socket = ensureServer()
        val token = UUID.randomUUID().toString()
        proxyItems[token] = ProxyItem(
            url = url,
            headers = headers,
            userAgent = userAgent,
            referer = referer
        )
        return ProxiedUrl(
            url = "http://127.0.0.1:${socket.localPort}/mpv/$token/",
            token = token
        )
    }

    fun unregister(token: String?) {
        if (token != null) {
            proxyItems.remove(token)
        }
    }

    private fun ensureServer(): ServerSocket {
        serverSocket?.takeIf { !it.isClosed }?.let { return it }

        synchronized(this) {
            serverSocket?.takeIf { !it.isClosed }?.let { return it }
            val socket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
            serverSocket = socket
            executor.execute { acceptLoop(socket) }
            logger.info { "Started MPV local HTTP proxy on 127.0.0.1:${socket.localPort}" }
            return socket
        }
    }

    private fun acceptLoop(socket: ServerSocket) {
        while (!socket.isClosed) {
            runCatching {
                val client = socket.accept()
                executor.execute { handleClient(client) }
            }.onFailure { error ->
                if (!socket.isClosed) {
                    logger.warn(error) { "MPV local proxy accept failed" }
                }
            }
        }
    }

    private fun handleClient(socket: Socket) {
        socket.useQuietly {
            socket.soTimeout = REQUEST_TIMEOUT_MS
            val input = BufferedInputStream(socket.getInputStream())
            val reader = BufferedReader(InputStreamReader(input))
            val requestLine = reader.readLine().orEmpty()
            val requestParts = requestLine.split(" ")
            if (requestParts.size < 2) {
                writeTextResponse(socket, 400, "Bad Request", "Bad Request")
            } else {
                val method = requestParts[0]
                val target = requestParts[1]
                val path = target.substringBefore("?")
                val query = target.substringAfter("?", missingDelimiterValue = "")
                val headers = readRequestHeaders(reader)
                val proxyPath = path.substringAfter("/mpv/", missingDelimiterValue = "")
                val token = proxyPath.substringBefore("/")
                val relativePath = proxyPath.substringAfter("/", missingDelimiterValue = "")
                val item = proxyItems[token]
                if (item == null) {
                    writeTextResponse(socket, 404, "Not Found", "MPV proxy item not found")
                } else {
                    val upstreamUrl = item.resolve(relativePath, query)
                    proxyUpstream(socket, method, headers, item, upstreamUrl)
                }
            }
        }
    }

    private fun readRequestHeaders(reader: BufferedReader): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isEmpty()) break
            val separator = line.indexOf(':')
            if (separator > 0) {
                headers[line.substring(0, separator).trim()] = line.substring(separator + 1).trim()
            }
        }
        return headers
    }

    private fun proxyUpstream(
        socket: Socket,
        method: String,
        requestHeaders: Map<String, String>,
        item: ProxyItem,
        upstreamUrl: String
    ) {
        val context = clientContext
        if (context == null) {
            writeTextResponse(socket, 500, "Internal Server Error", "MPV proxy context missing")
            return
        }

        val requestBuilder = Request.Builder()
            .url(upstreamUrl)
            .header("Accept-Encoding", "identity")

        item.headers.forEach { (name, value) ->
            if (name.isProxySafeHeader() && value.isNotBlank()) {
                requestBuilder.header(name, value)
            }
        }
        item.userAgent?.takeIf { it.isNotBlank() }?.let {
            requestBuilder.header("User-Agent", it)
        }
        item.referer?.takeIf { it.isNotBlank() }?.let {
            requestBuilder.header("Referer", it)
        }
        requestHeaders.firstHeader("Range")?.let {
            requestBuilder.header("Range", it)
        }

        val request = if (method.equals("HEAD", ignoreCase = true)) {
            requestBuilder.head().build()
        } else {
            requestBuilder.get().build()
        }

        runCatching {
            OkHttpUtil.generateCustomSslOkHttpClient(context)
                .newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
                .newCall(request)
                .execute()
                .use { response ->
                    val output = socket.getOutputStream()
                    val writer = BufferedWriter(OutputStreamWriter(output))
                    writer.append("HTTP/1.1 ${response.code} ${response.message.ifBlank { "OK" }}\r\n")
                    writer.append("Connection: close\r\n")
                    writer.append("Accept-Ranges: bytes\r\n")
                    response.header("Content-Type")?.let { writer.append("Content-Type: $it\r\n") }
                    response.header("Content-Range")?.let { writer.append("Content-Range: $it\r\n") }
                    val contentLength = response.body.contentLength()
                    if (contentLength >= 0L) {
                        writer.append("Content-Length: $contentLength\r\n")
                    }
                    writer.append("\r\n")
                    writer.flush()

                    if (!method.equals("HEAD", ignoreCase = true)) {
                        response.body.byteStream().use { upstream ->
                            upstream.copyTo(output)
                            output.flush()
                        }
                    }
                }
        }.onFailure { error ->
            if (error.isExpectedClientDisconnect()) {
                logger.debug { "MPV local proxy client disconnected: ${error.message}" }
            } else {
                logger.warn(error) { "MPV local proxy upstream failed" }
                writeTextResponse(socket, 502, "Bad Gateway", error.message ?: "Upstream failed")
            }
        }
    }

    private fun writeTextResponse(socket: Socket, code: Int, message: String, body: String) {
        runCatching {
            val bytes = body.toByteArray()
            val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
            writer.append("HTTP/1.1 $code $message\r\n")
            writer.append("Connection: close\r\n")
            writer.append("Content-Type: text/plain; charset=utf-8\r\n")
            writer.append("Content-Length: ${bytes.size}\r\n")
            writer.append("\r\n")
            writer.flush()
            socket.getOutputStream().write(bytes)
            socket.getOutputStream().flush()
        }
    }

    private fun Map<String, String>.firstHeader(name: String): String? {
        return entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
    }

    private fun String.isProxySafeHeader(): Boolean {
        return !equals("Host", ignoreCase = true) &&
                !equals("Connection", ignoreCase = true) &&
                !equals("Accept-Encoding", ignoreCase = true) &&
                !equals("Range", ignoreCase = true)
    }

    private fun Throwable.isExpectedClientDisconnect(): Boolean {
        if (this !is IOException) return false
        val message = generateSequence(this as Throwable) { it.cause }
            .mapNotNull { it.message }
            .joinToString(" ")
            .lowercase()
        return message.contains("connection reset") ||
                message.contains("broken pipe") ||
                message.contains("socket closed")
    }

    private fun Closeable.useQuietly(block: () -> Unit) {
        try {
            block()
        } catch (error: Throwable) {
            logger.warn(error) { "MPV local proxy request failed" }
        } finally {
            runCatching { close() }
        }
    }

    data class ProxiedUrl(
        val url: String,
        val token: String?
    )

    private data class ProxyItem(
        val url: String,
        val headers: Map<String, String>,
        val userAgent: String?,
        val referer: String?
    ) {
        fun resolve(relativePath: String, query: String): String {
            if (relativePath.isBlank()) return url

            val relativeTarget = buildString {
                append(relativePath)
                if (query.isNotBlank()) {
                    append('?')
                    append(query)
                }
            }
            return runCatching { URI(url).resolve(relativeTarget).toString() }
                .getOrElse { url }
        }
    }

    private const val REQUEST_TIMEOUT_MS = 30_000
}
