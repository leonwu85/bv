package dev.aaa1115910.bv.util

import android.widget.Toast
import dev.aaa1115910.biliapi.entity.live.LiveCodec
import dev.aaa1115910.biliapi.entity.live.LiveRoomPlayInfoResponse
import dev.aaa1115910.bv.player.entity.LiveCodec as AppLiveCodec
import dev.aaa1115910.biliapi.entity.live.LiveStream
import dev.aaa1115910.biliapi.repositories.LiveRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.util.Prefs
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject

object LiveStreamUrlFetcher {
    private val logger = KotlinLogging.logger("LiveStreamUrlFetcher")
    private val liveRepository: LiveRepository by inject(LiveRepository::class.java)

    // Codec 优先级: HEVC > AV1 > AVC
    private val codecPriority = listOf("hevc", "av1", "avc")

    /**
     * 获取直播流URL
     * @param roomId 直播间ID
     * @param qn 画质编号，默认30000（杜比，最高值），服务端会自动降级到实际最高可用画质
     * @param preferredCodec 首选编码格式，默认 HLS 自动选择最佳编码
     * @param liveCdnHost 自定义直播 CDN host，空值表示使用服务端返回的 host
     * @return 直播播放信息，包含流URL和可用画质列表，如果未开播或获取失败则返回null
     */
    suspend fun fetchLiveStreamUrl(
        roomId: Int,
        qn: Int = 30000,
        preferredCodec: AppLiveCodec = AppLiveCodec.HLS,
        liveCdnHost: String? = null
    ): LivePlayInfo? = withContext(Dispatchers.IO) {
        try {
            val sessData = Prefs.sessData
            val response = liveRepository.getLiveRoomPlayInfo(roomId, qn, sessData)
            
            if (response.code != 0) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        BVApp.context,
                        "获取直播流失败: ${response.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@withContext null
            }

            val data = response.data
            if (data == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        BVApp.context,
                        "获取直播流失败: 数据为空",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@withContext null
            }

            // 检查直播状态
            if (data.liveStatus != 1) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        BVApp.context,
                        "主播未开播",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@withContext null
            }

            // 构建画质描述映射表
            val qnDescMap = mutableMapOf<Int, String>()
            data.playUrlInfo?.playurl?.gQnDesc?.forEach { desc ->
                qnDescMap[desc.qn] = desc.desc
            }

            // 解析播放URL和画质信息
            val result = parsePlayUrl(response, preferredCodec, liveCdnHost)
            if (result == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        BVApp.context,
                        "无法获取播放地址",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@withContext null
            }

            logger.info { "Successfully fetched live stream URL for room $roomId: ${result.url}" }
            logger.info { "Current qn: ${result.currentQn}, accept_qn: ${result.acceptQn}" }
            LivePlayInfo(
                roomId = roomId,
                streamUrl = result.url,
                isLive = true,
                currentQn = result.currentQn,
                acceptQn = result.acceptQn,
                qnDescMap = qnDescMap,
                expiresAt = result.expiresAt
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to fetch live stream URL for room $roomId" }
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    BVApp.context,
                    "获取直播流失败: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            null
        }
    }

    private data class ParseResult(
        val url: String,
        val currentQn: Int,
        val acceptQn: List<Int>,
        val expiresAt: Long = 0
    )

    /**
     * 从URL的extra参数中解析expires时间戳
     * @param extra URL查询参数字符串，如 "?expires=1773483819&..."
     * @return 过期时间的毫秒时间戳，解析失败返回0
     */
    fun parseExpiresFromExtra(extra: String): Long {
        return runCatching {
            val regex = Regex("""[?&]expires=(\d+)""")
            val match = regex.find(extra)
            match?.groupValues?.get(1)?.toLong()?.times(1000) ?: 0L
        }.getOrElse { 0L }
    }

    internal fun buildLiveUrl(
        host: String,
        baseUrl: String,
        extra: String,
        liveCdnHost: String?
    ): String {
        val customHost = liveCdnHost?.takeIf { it.isNotBlank() }
        return if (customHost != null) {
            "${customHost.trimEnd('/')}/${baseUrl.trimStart('/')}$extra"
        } else {
            "$host$baseUrl$extra"
        }
    }

    /**
     * 解析播放URL
     * @param response 直播房间播放信息响应
     * @param preferredCodec 首选编码格式
     */
    private fun parsePlayUrl(
        response: LiveRoomPlayInfoResponse,
        preferredCodec: AppLiveCodec,
        liveCdnHost: String?
    ): ParseResult? {
        val streams = response.data?.playUrlInfo?.playurl?.stream ?: return null

        when (preferredCodec) {
            AppLiveCodec.HLS -> {
                // HLS 自动选择最佳编码（HEVC > AV1 > AVC）
                val hlsStream = streams.find { it.protocolName == "http_hls" }
                if (hlsStream != null) {
                    val result = buildUrlFromStream(hlsStream, null, liveCdnHost)
                    if (result != null) {
                        logger.info { "Using HLS stream with auto codec" }
                        return result
                    }
                }
                // 回退到 FLV
                val flvStream = streams.find { it.protocolName == "http_stream" }
                if (flvStream != null) {
                    val result = buildUrlFromStream(flvStream, "avc", liveCdnHost)
                    if (result != null) {
                        logger.info { "Using FLV stream (fallback)" }
                        return result
                    }
                }
            }
            AppLiveCodec.FLV -> {
                // 强制使用 FLV（仅支持 AVC）
                val flvStream = streams.find { it.protocolName == "http_stream" }
                if (flvStream != null) {
                    val result = buildUrlFromStream(flvStream, "avc", liveCdnHost)
                    if (result != null) {
                        logger.info { "Using FLV stream" }
                        return result
                    }
                }
                // 回退到 HLS
                val hlsStream = streams.find { it.protocolName == "http_hls" }
                if (hlsStream != null) {
                    val result = buildUrlFromStream(hlsStream, "avc", liveCdnHost)
                    if (result != null) {
                        logger.info { "Using HLS stream with AVC (fallback)" }
                        return result
                    }
                }
            }
            AppLiveCodec.AVC -> {
                // HLS 强制 AVC
                val hlsStream = streams.find { it.protocolName == "http_hls" }
                if (hlsStream != null) {
                    val result = buildUrlFromStream(hlsStream, "avc", liveCdnHost)
                    if (result != null) {
                        logger.info { "Using HLS stream with AVC" }
                        return result
                    }
                }
                // 回退到 FLV
                val flvStream = streams.find { it.protocolName == "http_stream" }
                if (flvStream != null) {
                    val result = buildUrlFromStream(flvStream, "avc", liveCdnHost)
                    if (result != null) {
                        logger.info { "Using FLV stream (fallback)" }
                        return result
                    }
                }
            }
        }

        // 使用第一个可用的流作为兜底
        for (stream in streams) {
            val result = buildUrlFromStream(stream, null, liveCdnHost)
            if (result != null) {
                logger.info { "Using fallback stream: ${stream.protocolName}" }
                return result
            }
        }

        return null
    }

    /**
     * 从编解码器列表中按优先级选择最佳编解码器 (HEVC > AV1 > AVC)
     */
    private fun selectBestCodec(codecs: List<LiveCodec>): LiveCodec? {
        for (preferredCodec in codecPriority) {
            val codec = codecs.find { it.codecName == preferredCodec }
            if (codec != null && codec.urlInfo.isNotEmpty()) {
                logger.debug { "Selected codec: ${codec.codecName}" }
                return codec
            }
        }
        // 如果没有匹配的，取第一个有效的
        return codecs.firstOrNull { it.urlInfo.isNotEmpty() }
    }

    /**
     * 从流信息构建URL
     * @param stream 直播流信息
     * @param preferredCodecName 首选编码名称，null 表示按优先级自动选择
     */
    private fun buildUrlFromStream(
        stream: LiveStream,
        preferredCodecName: String?,
        liveCdnHost: String?
    ): ParseResult? {
        // 优先选择 fmp4，其次 ts，最后 flv
        val formatOrder = listOf("fmp4", "ts", "flv")

        for (formatName in formatOrder) {
            val format = stream.format.find { it.formatName == formatName }
            if (format != null && format.codec.isNotEmpty()) {
                val codec = if (preferredCodecName != null) {
                    // 指定编码时，查找指定编码
                    format.codec.find { it.codecName == preferredCodecName && it.urlInfo.isNotEmpty() }
                } else {
                    // 自动选择最佳编码
                    selectBestCodec(format.codec)
                } ?: continue
                val urlInfo = codec.urlInfo.first()
                val fullUrl = buildLiveUrl(urlInfo.host, codec.baseUrl, urlInfo.extra, liveCdnHost)
                val expiresAt = parseExpiresFromExtra(urlInfo.extra)
                logger.debug { "Built URL with format $formatName, codec ${codec.codecName}: $fullUrl" }
                return ParseResult(
                    url = fullUrl,
                    currentQn = codec.currentQn,
                    acceptQn = codec.acceptQn,
                    expiresAt = expiresAt
                )
            }
        }

        // 如果没有找到特定格式，使用第一个可用的
        for (format in stream.format) {
            if (format.codec.isNotEmpty()) {
                val codec = if (preferredCodecName != null) {
                    format.codec.find { it.codecName == preferredCodecName && it.urlInfo.isNotEmpty() }
                } else {
                    selectBestCodec(format.codec)
                } ?: continue
                val urlInfo = codec.urlInfo.first()
                val fullUrl = buildLiveUrl(urlInfo.host, codec.baseUrl, urlInfo.extra, liveCdnHost)
                val expiresAt = parseExpiresFromExtra(urlInfo.extra)
                logger.debug { "Built URL with fallback format ${format.formatName}, codec ${codec.codecName}: $fullUrl" }
                return ParseResult(
                    url = fullUrl,
                    currentQn = codec.currentQn,
                    acceptQn = codec.acceptQn,
                    expiresAt = expiresAt
                )
            }
        }

        return null
    }
}

/**
 * 直播播放信息
 * @param expiresAt URL过期时间戳（毫秒），0表示未知
 */
data class LivePlayInfo(
    val roomId: Int,
    val streamUrl: String,
    val isLive: Boolean = true,
    val currentQn: Int = 0,
    val acceptQn: List<Int> = emptyList(),
    val qnDescMap: Map<Int, String> = emptyMap(),
    val expiresAt: Long = 0
)
