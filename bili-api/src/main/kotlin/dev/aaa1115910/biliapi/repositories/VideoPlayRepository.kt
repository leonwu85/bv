package dev.aaa1115910.biliapi.repositories

import bilibili.app.playerunite.v1.PlayerGrpcKt
import bilibili.app.playerunite.v1.PlayViewUniteReq
import bilibili.app.playerunite.v1.playViewUniteReq
import bilibili.community.service.dm.v1.DMGrpcKt
import bilibili.community.service.dm.v1.dmSegMobileReq
import bilibili.community.service.dm.v1.dmViewReq
import bilibili.pgc.gateway.player.v2.playViewReq
import bilibili.playershared.videoVod
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.CodeType
import dev.aaa1115910.biliapi.entity.PlayData
import dev.aaa1115910.biliapi.entity.PlayDataUnavailableException
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMask
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMaskSegment
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMaskType
import dev.aaa1115910.biliapi.entity.video.HeartbeatVideoType
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.entity.video.VideoShot
import dev.aaa1115910.biliapi.grpc.utils.handleGrpcException
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.BiliHttpProxyApi
import dev.aaa1115910.biliapi.http.entity.AuthFailureException
import dev.aaa1115910.biliapi.http.entity.BiliAuthFailureHandler
import dev.aaa1115910.biliapi.http.entity.danmaku.DanmakuData
import dev.aaa1115910.biliapi.http.entity.video.PlayUrlData
import dev.aaa1115910.biliapi.http.entity.video.VideoPlayerInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import bilibili.pgc.gateway.player.v2.PlayURLGrpcKt as PgcPlayURLGrpcKt
import java.net.URI
import java.util.Locale

internal fun shouldTryLook1080P(enabled: Boolean, sessionData: String?): Boolean =
    enabled && sessionData.isNullOrBlank()

data class DlnaPlayResource(
    val url: String,
    val mimeType: String,
)

internal fun PlayUrlData.toDlnaPlayResource(): DlnaPlayResource {
    // Match PiliPlus' DLNA path: the dedicated TV play-url endpoint provides the
    // renderer source in its first durl entry. Remaining URLs on that entry are
    // CDN fallbacks, rather than additional media parts.
    val castEntry = durl.firstOrNull()
    checkNotNull(castEntry) { "当前视频不支持投屏" }
    val url = (listOf(castEntry.url) + castEntry.backupUrl)
        .asSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .firstOrNull { candidate ->
            val scheme = runCatching {
                URI(candidate).scheme?.lowercase(Locale.ROOT)
            }.getOrNull()
            scheme == "http" || scheme == "https"
        }
    checkNotNull(url) { "投屏地址不是有效的 HTTP 链接" }

    val normalizedFormat = listOf(format, type)
        .joinToString(separator = " ")
        .lowercase(Locale.ROOT)
    val path = runCatching { URI(url).path.orEmpty().lowercase(Locale.ROOT) }.getOrDefault("")
    val mimeType = when {
        "mp4" in normalizedFormat || path.endsWith(".mp4") -> "video/mp4"
        "flv" in normalizedFormat || path.endsWith(".flv") -> "video/x-flv"
        else -> null
    }
    checkNotNull(mimeType) {
        val displayFormat = format.ifBlank { type }.ifBlank { "未知" }
        "当前视频投屏格式不受支持：$displayFormat"
    }

    return DlnaPlayResource(url = url, mimeType = mimeType)
}

@Single
class VideoPlayRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository
) {
    private val playerStub
        get() = runCatching {
            PlayerGrpcKt.PlayerCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()
    private val pgcPlayUrlStub
        get() = runCatching {
            PgcPlayURLGrpcKt.PlayURLCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()
    private val danmakuStub
        get() = runCatching {
            DMGrpcKt.DMCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()

    private val proxyPgcPlayUrlStub
        get() = runCatching {
            PgcPlayURLGrpcKt.PlayURLCoroutineStub(channelRepository.proxyChannel!!)
        }.getOrNull()

    private fun notifyPlayUrlAuthFailureIfNeeded(error: Throwable) {
        if (error is AuthFailureException && !authRepository.sessionData.isNullOrBlank()) {
            BiliAuthFailureHandler.notify("获取播放链接: ${error.message ?: "账号未登录"}")
        }
    }

    suspend fun getAppDanmakuSegment(
        aid: Long,
        cid: Long,
        segmentIndex: Int
    ): List<DanmakuData> = withContext(Dispatchers.IO) {
        val reply = runCatching {
            danmakuStub?.dmSegMobile(dmSegMobileReq {
                pid = aid
                oid = cid
                type = 1
                this.segmentIndex = segmentIndex.toLong()
            })
        }.onFailure { handleGrpcException(it) }.getOrThrow()

        reply?.elemsList.orEmpty().map { elem ->
            DanmakuData(
                time = elem.progress / 1000f,
                type = elem.mode,
                size = elem.fontsize,
                color = elem.color,
                timestamp = (elem.ctime / 1000).toInt(),
                pool = elem.pool,
                midHash = elem.midHash,
                dmid = elem.id,
                level = elem.weight,
                text = elem.content
            )
        }
    }


    suspend fun getPlayData(
        aid: Long,
        cid: Long,
        bvid: String = "",
        preferApiType: ApiType = ApiType.Web,
        tryLook1080P: Boolean = false
    ): PlayData {
        return when (preferApiType) {
            ApiType.Web -> {
                val tryLook = shouldTryLook1080P(tryLook1080P, authRepository.sessionData)
                runCatching {
                    val requestedQn = 127
                    val playUrlData = BiliHttpApi.getVideoWbiPlayUrl(
                        av = aid,
                        bv = bvid.takeIf { it.isNotBlank() },
                        cid = cid,
                        fnval = 4048,
                        qn = requestedQn,
                        fnver = 0,
                        fourk = 1,
                        sessData = authRepository.sessionData,
                        dedeUserID = authRepository.mid,
                        buvid3 = authRepository.buvid3,
                        dedeUserIDCkMd5 = authRepository.dedeUserIDCkMd5,
                        biliJct = authRepository.biliJct,
                        sid = authRepository.sid,
                        tryLook = tryLook,
                        gaiaVtoken = authRepository.gaiaVtoken
                    ).getResponseData()
                    val playData = PlayData.fromPlayUrlData(playUrlData)
                    if (!playData.needPay && !playData.hasPlayableVodStreams()) {
                        throw PlayDataUnavailableException(
                            "WBI qn=$requestedQn 未返回可播放音视频流：" +
                                "video=${playData.dashVideos.size}, " +
                                "audio=${playData.playableAudioCount()}, " +
                                "durl=${playUrlData.durl.size}, " +
                                "acceptQuality=${playUrlData.acceptQuality}, " +
                                "responseQuality=${playUrlData.quality}, " +
                                "result=${playUrlData.result}, " +
                                "message=${playUrlData.message}"
                        )
                    }
                    playData
                }.onFailure(::notifyPlayUrlAuthFailureIfNeeded)
                    .getOrThrow()
            }

            ApiType.App -> {
                withContext(Dispatchers.IO) {
                    val codecTypes = listOf(
                        CodeType.Code264,
                        CodeType.Code265,
                        CodeType.CodeAv1
                    )
                    val requests = codecTypes.map { codecType ->
                        codecType to buildPlayViewUniteRequest(
                            aid = aid,
                            cid = cid,
                            bvid = bvid,
                            codecType = codecType
                        )
                    }
                    warmUpPlayViewUniteReqSerialization(requests.map { it.second })

                    val replies = requests.map { (codecType, request) ->
                        async {
                            runCatching {
                                playerStub?.playViewUnite(request)
                                    ?: throw IllegalStateException("Player stub is not initialized")
                            }.fold(
                                onSuccess = { Result.success(it) },
                                onFailure = {
                                    Result.failure(normalizeGrpcFailure(it).also { failure ->
                                        println("get play data failed: [aid=$aid, cid=$cid, preferCodec=$codecType, preferApiType=$preferApiType, reason=${failure.message}]")
                                    })
                                }
                            )
                        }
                    }.awaitAll()
                    replies.mapNotNull { result ->
                        result.getOrNull()?.let { PlayData.fromPlayViewUniteReply(it) }
                    }
                        .reduceOrNull(PlayData::plus)
                        ?: throw playDataUnavailableException(
                            message = "APP 接口的所有编码均未获取到播放数据",
                            failures = replies.mapNotNull { it.exceptionOrNull() }
                        )
                }
            }
        }
    }

    suspend fun getDownloadPlayData(
        aid: Long,
        bvid: String,
        cid: Long,
        qn: Int,
        tryLook1080P: Boolean = false
    ): PlayData {
        val tryLook = shouldTryLook1080P(tryLook1080P, authRepository.sessionData)
        val qnCandidates = downloadQnCandidates(qn)
        var lastFailure: Throwable? = null

        qnCandidates.forEach { candidateQn ->
            val result = runCatching {
                val playUrlData = BiliHttpApi.getVideoWbiPlayUrl(
                    av = aid,
                    bv = bvid.takeIf { it.isNotBlank() },
                    cid = cid,
                    fnval = 4048,
                    qn = candidateQn,
                    fnver = 0,
                    fourk = 1,
                    sessData = authRepository.sessionData,
                    dedeUserID = authRepository.mid,
                    buvid3 = authRepository.buvid3,
                    dedeUserIDCkMd5 = authRepository.dedeUserIDCkMd5,
                    biliJct = authRepository.biliJct,
                    sid = authRepository.sid,
                    tryLook = tryLook
                ).getResponseData()
                val playData = PlayData.fromPlayUrlData(playUrlData)
                if (!playData.hasCacheableDownloadStreams()) {
                    val audioCount = playData.dashAudios.size +
                        listOfNotNull(playData.dolby, playData.flac).size
                    throw IllegalStateException(
                        "WBI qn=$candidateQn 未返回可缓存音视频流：" +
                            "video=${playData.dashVideos.size}, " +
                            "audio=$audioCount, " +
                            "acceptQuality=${playUrlData.acceptQuality}, " +
                            "responseQuality=${playUrlData.quality}, " +
                            "result=${playUrlData.result}, " +
                            "message=${playUrlData.message}"
                    )
                }
                playData
            }

            result.onSuccess { return it }
            result.onFailure(::notifyPlayUrlAuthFailureIfNeeded)
            lastFailure = result.exceptionOrNull()
        }

        throw lastFailure ?: IllegalStateException("WBI 未返回可缓存音视频流")
    }

    private fun PlayData.hasCacheableDownloadStreams(): Boolean {
        return dashVideos.isNotEmpty() && (dashAudios.isNotEmpty() || dolby != null || flac != null)
    }

    private fun downloadQnCandidates(qn: Int): List<Int> {
        val knownQualities = listOf(127, 126, 125, 120, 116, 112, 80, 74, 64, 32, 16, 6)
        return (listOf(qn) + knownQualities.filter { it < qn })
            .filter { it > 0 }
            .distinct()
    }

    /**
     * Returns the progressive stream used by a DLNA renderer.
     *
     * The TV play-url endpoint differs from the regular DASH endpoint by returning a
     * muxed audio/video stream that standalone renderers can consume directly.
     */
    suspend fun getDlnaPlayResource(
        aid: Long,
        cid: Long,
        epid: Int? = null,
        qn: Int = 80,
    ): DlnaPlayResource {
        require(aid > 0L) { "无效的视频 aid" }
        require(cid > 0L) { "无效的视频 cid" }

        val pgcEpid = epid?.takeIf { it > 0 }
        val response = runCatching {
            BiliHttpApi.getTvPlayUrl(
                accessKey = authRepository.accessToken,
                cid = cid,
                objectId = pgcEpid?.toLong() ?: aid,
                playurlType = if (pgcEpid != null) 2 else 1,
                qn = qn.coerceAtLeast(1),
            )
        }.onFailure(::notifyPlayUrlAuthFailureIfNeeded)
            .getOrThrow()

        return response.getResponseData().toDlnaPlayResource()
    }

    private fun buildPlayViewUniteRequest(
        aid: Long,
        cid: Long,
        bvid: String,
        codecType: CodeType
    ): PlayViewUniteReq = playViewUniteReq {
        vod = videoVod {
            this.aid = aid
            this.cid = cid
            fnval = 4048
            qn = 127
            fnver = 0
            fourk = true
            preferCodecType = codecType.toPlayerSharedCodeType()
        }
        if (bvid.isNotBlank()) this.bvid = bvid
    }

    private fun warmUpPlayViewUniteReqSerialization(requests: List<PlayViewUniteReq>) {
        runCatching {
            PlayViewUniteReq.getDefaultInstance().serializedSize
            requests.forEach { it.serializedSize }
        }.getOrElse {
            throw IllegalStateException("Initialize PlayViewUnite request serialization failed", it)
        }
    }

    private fun normalizeGrpcFailure(error: Throwable): Throwable {
        if (error is CancellationException) throw error
        return runCatching { handleGrpcException(error) }.exceptionOrNull() ?: error
    }

    private fun playDataUnavailableException(
        message: String,
        failures: List<Throwable>
    ): PlayDataUnavailableException {
        val failureSummary = failures
            .map { it.localizedMessage ?: it.javaClass.simpleName }
            .distinct()
            .joinToString("；")
        val detailedMessage = failureSummary.takeIf { it.isNotBlank() }
            ?.let { "$message：$it" }
            ?: message
        val exception = PlayDataUnavailableException(detailedMessage, failures.firstOrNull())
        failures.drop(1).forEach(exception::addSuppressed)
        return exception
    }

    suspend fun getPgcPlayData(
        aid: Long?,
        cid: Long?,
        epid: Int,
        preferCodec: CodeType = CodeType.NoCode,
        preferApiType: ApiType = ApiType.Web,
        enableProxy: Boolean = false,
        proxyArea: String = "",
        tryLook1080P: Boolean = false
    ): PlayData {
        println("get pgc play data: [aid=$aid, cid=$cid, epid=$epid, preferCodec=$preferCodec, preferApiType=$preferApiType, enableProxy=$enableProxy, proxyArea=$proxyArea]")
        return when (preferApiType) {
            ApiType.Web -> {
                val tryLook = shouldTryLook1080P(tryLook1080P, authRepository.sessionData)
                val playUrlData = runCatching {
                    if (enableProxy) {
                        BiliHttpProxyApi.getPgcVideoPlayUrlV2(
                            av = aid,
                            cid = cid,
                            epid = epid,
                            fnval = 4048,
                            qn = 127,
                            fnver = 0,
                            fourk = 1,
                            sessData = authRepository.sessionData,
                            tryLook = tryLook,
                            gaiaVtoken = authRepository.gaiaVtoken
//                            buvid3 = authRepository.buvid3
                        )
                    } else {
                        BiliHttpApi.getPgcVideoPlayUrlV2(
                            av = aid,
                            cid = cid,
                            epid = epid,
                            fnval = 4048,
                            qn = 127,
                            fnver = 0,
                            fourk = 1,
                            sessData = authRepository.sessionData,
                            tryLook = tryLook,
                            gaiaVtoken = authRepository.gaiaVtoken
//                            buvid3 = authRepository.buvid3
                        )
                    }.getResponseData()
                }.onFailure(::notifyPlayUrlAuthFailureIfNeeded)
                    .getOrThrow()

                PlayData.fromPlayUrlV2Data(playUrlData)
            }

            ApiType.App -> {
                withContext(Dispatchers.IO) {
                    val codecTypes = listOf(
                        CodeType.Code264,
                        CodeType.Code265,
                        CodeType.CodeAv1
                    )
                    val replies = codecTypes.map { codecType ->
                        val req = playViewReq {
                            this.epid = epid.toLong()
                            cid?.let { this.cid = it }
                            qn = 127
                            fnver = 0
                            fnval = 4048
                            fourk = true
                            forceHost = 0
                            download = 0
                            preferCodecType = codecType.toPgcPlayUrlCodeType()
                        }
                        async {
                            runCatching {
                                if (enableProxy) {
                                    proxyPgcPlayUrlStub?.playView(req)
                                        ?: throw IllegalStateException("Proxy pgc play url stub is not initialized")
                                } else {
                                    pgcPlayUrlStub?.playView(req)
                                        ?: throw IllegalStateException("Pgc play url stub is not initialized")
                                }
                            }.fold(
                                onSuccess = { Result.success(it) },
                                onFailure = {
                                    Result.failure(normalizeGrpcFailure(it).also { failure ->
                                        println("get pgc play data failed: [aid=$aid, cid=$cid, epid=$epid, preferCodec=$codecType, preferApiType=$preferApiType, reason=${failure.message}]")
                                    })
                                }
                            )
                        }
                    }.awaitAll()
                    replies.mapNotNull { result ->
                        result.getOrNull()?.let { PlayData.fromPgcPlayViewReply(it) }
                    }
                        .reduceOrNull(PlayData::plus)
                        ?: throw playDataUnavailableException(
                            message = "APP 接口的所有编码均未获取到番剧播放数据",
                            failures = replies.mapNotNull { it.exceptionOrNull() }
                        )
                }
            }
        }
    }

    suspend fun getVideoPlayerWbiInfo(
        aid: Long,
        cid: Long,
        epid: Int? = null,
        seasonId: Int? = null,
    ): VideoPlayerInfo = BiliHttpApi.getVideoPlayerWbiInfo(
        av = aid,
        cid = cid,
        epid = epid,
        seasonId = seasonId,
        sessData = authRepository.sessionData
    ).getResponseData()

    suspend fun getSubtitle(
        aid: Long,
        cid: Long,
        preferApiType: ApiType = ApiType.Web
    ): List<Subtitle> {
        return when (preferApiType) {
            ApiType.Web -> {
                val response = BiliHttpApi.getVideoMoreInfo(
                    avid = aid,
                    cid = cid,
                    sessData = authRepository.sessionData ?: "",
                    buvid3 = authRepository.buvid3 ?: ""
                ).getResponseData()

                if (response.subtitle == null) {
                    println("get subtitle failed")
                } else {
                    println("get subtitle success")
                }
                response.subtitle?.subtitles
                    ?.map { Subtitle.fromSubtitleItem(it) }
                    ?: emptyList()
            }

            ApiType.App -> {
                val dmViewReply = runCatching {
                    danmakuStub?.dmView(dmViewReq {
                        pid = aid.toLong()
                        oid = cid.toLong()
                        type = 1
                    })
                }.onFailure { handleGrpcException(it) }.getOrThrow()
                dmViewReply?.subtitle?.subtitlesList
                    ?.map { Subtitle.fromSubtitleItem(it) }
                    ?: emptyList()
            }
        }
    }

    suspend fun sendHeartbeat(
        aid: Long,
        cid: Long,
        time: Int,
        type: HeartbeatVideoType = HeartbeatVideoType.Video,
        subType: Int? = null,
        epid: Int? = null,
        seasonId: Int? = null,
        preferApiType: ApiType = ApiType.Web
    ) {
        val result = when (preferApiType) {
            ApiType.Web -> BiliHttpApi.sendHeartbeat(
                avid = aid.toLong(),
                cid = cid,
                playedTime = time,
                type = type.value,
                subType = subType,
                epid = epid,
                sid = seasonId,
                csrf = authRepository.biliJct,
                sessData = authRepository.sessionData ?: ""
            )

            ApiType.App -> BiliHttpApi.sendHeartbeat(
                avid = aid.toLong(),
                cid = cid,
                playedTime = time,
                type = type.value,
                subType = subType,
                epid = epid,
                sid = seasonId,
                accessKey = authRepository.accessToken ?: ""
            )
        }
        println("send heartbeat result: $result")
    }

    suspend fun sendDanmaku(
        cid: Long,
        bvid: String,
        message: String,
        progress: Int,
        mode: Int = 1,
        fontSize: Int = 25,
        color: Int = 0xFFFFFF
    ): Long? {
        val csrf = authRepository.biliJct ?: error("账号未登录")
        val sessData = authRepository.sessionData ?: error("账号未登录")
        val response = BiliHttpApi.postDanmaku(
            cid = cid,
            bvid = bvid,
            message = message,
            progress = progress,
            mode = mode,
            fontSize = fontSize,
            color = color,
            csrf = csrf,
            sessData = sessData,
            dedeUserID = authRepository.mid,
            buvid3 = authRepository.buvid3
        )
        if (response.code != 0) throw IllegalStateException(response.message)
        return response.getResponseData().dmid
    }

    suspend fun getDanmakuMask(
        aid: Long,
        cid: Long,
        preferApiType: ApiType = ApiType.Web
    ): List<DanmakuMaskSegment> {
        val danmakuMaskUrl = when (preferApiType) {
            ApiType.Web -> {
                val response = BiliHttpApi.getVideoMoreInfo(
                    avid = aid,
                    cid = cid,
                    sessData = authRepository.sessionData ?: "",
                    buvid3 = authRepository.buvid3 ?: ""
                ).getResponseData()
                response.dmMask?.maskUrl
            }

            ApiType.App -> {
                val dmViewReply = runCatching {
                    danmakuStub?.dmView(dmViewReq {
                        pid = aid
                        oid = cid
                        type = 1
                    })
                }.onFailure { handleGrpcException(it) }.getOrThrow()
                dmViewReply?.mask?.maskUrl
            }
        } ?: return emptyList()

        val maskBinary = BiliHttpApi.download(danmakuMaskUrl.apply {
            when (preferApiType) {
                ApiType.Web -> replace("mobmask", "webmask")
                ApiType.App -> replace("webmask", "mobmask")
            }
        })
        val danmakuMaskType = when (preferApiType) {
            ApiType.Web -> DanmakuMaskType.WebMask
            ApiType.App -> DanmakuMaskType.MobMask
        }
        return DanmakuMask.fromBinary(maskBinary, danmakuMaskType).segments
    }

    suspend fun getVideoShot(
        aid: Long,
        cid: Long,
        preferApiType: ApiType = ApiType.Web
    ): VideoShot? {
        val videoShortResponse = when (preferApiType) {
            ApiType.Web -> BiliHttpApi.getWebVideoShot(aid = aid, cid = cid)
            ApiType.App -> BiliHttpApi.getAppVideoShot(aid = aid, cid = cid)
        }
        val videoShot = VideoShot.fromVideoShot(videoShortResponse.getResponseData())
        return videoShot
    }
}
