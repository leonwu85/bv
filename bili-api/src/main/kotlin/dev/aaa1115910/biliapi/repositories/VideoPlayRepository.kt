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
import dev.aaa1115910.biliapi.http.entity.video.VideoPlayerInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import bilibili.pgc.gateway.player.v2.PlayURLGrpcKt as PgcPlayURLGrpcKt

internal fun shouldTryLook1080P(enabled: Boolean, sessionData: String?): Boolean =
    enabled && sessionData.isNullOrBlank()

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
                val qnCandidates = playUrlQnCandidates(127)
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
                            tryLook = tryLook,
                            gaiaVtoken = authRepository.gaiaVtoken
                        ).getResponseData()
                        val playData = PlayData.fromPlayUrlData(playUrlData)
                        if (!playData.needPay && !playData.hasPlayableVodStreams()) {
                            val audioCount = playData.playableAudioCount()
                            throw IllegalStateException(
                                "WBI qn=$candidateQn 未返回可播放音视频流：" +
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

                throw lastFailure ?: IllegalStateException("WBI 未返回可播放音视频流")
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
                            val playUniteReply = runCatching {
                                playerStub?.playViewUnite(request)
                                    ?: throw IllegalStateException("Player stub is not initialized")
                            }.onFailure {
                                // dont throw
                                runCatching { handleGrpcException(it) }
                                    .onFailure {
                                        println("get play data failed: [aid=$aid, cid=$cid, preferCodec=$codecType, preferApiType=$preferApiType]")
                                        it.printStackTrace()
                                    }
                            }.getOrNull()
                            playUniteReply
                        }
                    }.awaitAll()
                    val result = replies.map {
                        it?.let { PlayData.fromPlayViewUniteReply(it) }
                    }.reduce { acc, playData ->
                        acc?.let { playData?.let { acc + playData } ?: acc } ?: playData
                    } ?: throw IllegalStateException("All codec types are failed to get play data")
                    result
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

    private fun PlayData.hasPlayableVodStreams(): Boolean {
        return dashVideos.isNotEmpty() && playableAudioCount() > 0
    }

    private fun PlayData.playableAudioCount(): Int {
        return dashAudios.size + listOfNotNull(dolby, flac).size
    }

    private fun playUrlQnCandidates(qn: Int): List<Int> {
        val knownQualities = listOf(127, 126, 125, 120, 116, 112, 80, 74, 64, 32, 16, 6)
        return (listOf(qn) + knownQualities.filter { it < qn })
            .filter { it > 0 }
            .distinct()
    }

    private fun downloadQnCandidates(qn: Int): List<Int> {
        return playUrlQnCandidates(qn)
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
                            val playReply = runCatching {
                                if (enableProxy) {
                                    proxyPgcPlayUrlStub?.playView(req)
                                        ?: throw IllegalStateException("Proxy pgc play url stub is not initialized")
                                } else {
                                    pgcPlayUrlStub?.playView(req)
                                        ?: throw IllegalStateException("Pgc play url stub is not initialized")
                                }
                            }.onFailure {
                                // dont throw
                                runCatching { handleGrpcException(it) }
                                    .onFailure {
                                        println("get pgc play data failed: [aid=$aid, cid=$cid, epid=$epid, preferCodec=$codecType, preferApiType=$preferApiType]")
                                        it.printStackTrace()
                                    }
                            }.getOrNull()
                            playReply
                        }
                    }.awaitAll()
                    val result = replies.map {
                        it?.let { PlayData.fromPgcPlayViewReply(it) }
                    }.reduce { acc, playData ->
                        acc?.let { playData?.let { acc + playData } ?: acc } ?: playData
                    } ?: throw IllegalStateException("All codec types are failed to get play data")
                    result
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
