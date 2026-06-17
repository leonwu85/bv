package dev.aaa1115910.biliapi.repositories

import bilibili.app.im.v1.Session as AppDirectSession
import bilibili.app.im.v1.ThreeDotItem
import bilibili.app.im.v1.UnreadStyle
import bilibili.app.im.v1.imGrpcKt as AppImGrpcKt
import bilibili.app.im.v1.SessionPageType
import bilibili.app.im.v1.clearUnreadReq
import bilibili.app.im.v1.deleteSessionListReq
import bilibili.app.im.v1.offset
import bilibili.app.im.v1.paginationParams
import bilibili.app.im.v1.sessionMainReq
import bilibili.dagw.component.avatar.v1.AvatarItem
import bilibili.dagw.component.avatar.v1.BasicLayerResource
import bilibili.im.interface1.v1.ImInterfaceGrpcKt
import bilibili.im.interface1.v1.reqGetSessions
import bilibili.im.interface1.v1.reqRemoveSession
import bilibili.im.interface1.v1.reqSendMsg
import bilibili.im.interface1.v1.reqSessionDetail
import bilibili.im.interface1.v1.reqSessionDetails
import bilibili.im.interface1.v1.reqSessionMsg
import bilibili.im.interface1.v1.reqSetTop
import bilibili.im.interface1.v1.reqSingleUnread
import bilibili.im.interface1.v1.reqTotalUnread
import bilibili.im.interface1.v1.reqUpdateAck
import bilibili.im.type.Msg
import bilibili.im.type.MsgSource
import bilibili.im.type.MsgType
import bilibili.im.type.RecverType
import bilibili.im.type.SessionInfo
import bilibili.im.type.msg
import dev.aaa1115910.biliapi.entity.message.DirectMessage
import dev.aaa1115910.biliapi.entity.message.DirectMessageAction
import dev.aaa1115910.biliapi.entity.message.DirectMessageContent
import dev.aaa1115910.biliapi.entity.message.DirectMessageEmote
import dev.aaa1115910.biliapi.entity.message.DirectMessageFeedUnread
import dev.aaa1115910.biliapi.entity.message.DirectMessageHistoryPage
import dev.aaa1115910.biliapi.entity.message.DirectMessageImageDraft
import dev.aaa1115910.biliapi.entity.message.DirectMessageOffset
import dev.aaa1115910.biliapi.entity.message.DirectMessagePage
import dev.aaa1115910.biliapi.entity.message.DirectMessageSession
import dev.aaa1115910.biliapi.entity.message.MessageFeedItem
import dev.aaa1115910.biliapi.entity.message.MessageFeedPage
import dev.aaa1115910.biliapi.entity.message.MessageFeedType
import dev.aaa1115910.biliapi.entity.user.DynamicEmoteDraft
import dev.aaa1115910.biliapi.entity.user.DynamicEmotePackageDraft
import dev.aaa1115910.biliapi.grpc.utils.handleGrpcException
import dev.aaa1115910.biliapi.http.BiliHttpApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import org.koin.core.annotation.Single
import java.util.UUID

@Single
class MessageRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository
) {
    private val service
        get() = runCatching {
            ImInterfaceGrpcKt.ImInterfaceCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()

    private val appService
        get() = runCatching {
            AppImGrpcKt.imCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private fun requireService(): ImInterfaceGrpcKt.ImInterfaceCoroutineStub =
        service ?: error("消息服务未初始化")

    private fun requireAppService(): AppImGrpcKt.imCoroutineStub =
        appService ?: error("消息服务未初始化")

    suspend fun getUnreadCount(): Int {
        val reply = runCatching {
            requireService().singleUnread(reqSingleUnread {
                unreadType = 0
                showUnfollowList = 1
                uid = authRepository.mid ?: 0L
            })
        }.onFailure { handleGrpcException(it) }.getOrThrow()
        return listOf(
            reply.followUnread,
            reply.unfollowUnread,
            reply.bizMsgFollowUnread,
            reply.bizMsgUnfollowUnread,
            reply.dustbinUnread
        ).sumOf { it }.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    suspend fun getFeedUnread(): DirectMessageFeedUnread {
        val reply = runCatching {
            requireService().getTotalUnread(reqTotalUnread {
                unreadType = 2
                showUnfollowList = 1
                uid = authRepository.mid ?: 0L
            })
        }.onFailure { handleGrpcException(it) }.getOrThrow()
        val unread = reply.msgFeedUnread.unreadMap
        return DirectMessageFeedUnread(
            reply = unread["reply"]?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt() ?: 0,
            at = unread["at"]?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt() ?: 0,
            like = unread["like"]?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt() ?: 0,
            sysMsg = unread["sys_msg"]?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt() ?: 0
        )
    }

    suspend fun getSessions(
        cursor: Long? = null,
        pageSize: Int = 20,
        offsets: Map<Int, DirectMessageOffset> = emptyMap()
    ): DirectMessagePage = runCatching {
        getMainSessions(offsets)
    }.getOrElse { error ->
        if (cursor == null && offsets.isNotEmpty()) {
            handleGrpcException(error)
            throw error
        }
        getInterfaceSessions(cursor, pageSize)
    }

    private suspend fun getMainSessions(
        offsets: Map<Int, DirectMessageOffset>
    ): DirectMessagePage {
        val reply = runCatching {
            requireAppService().sessionMain(sessionMainReq {
                paginationParams = paginationParams {
                    offsets.forEach { (key, value) ->
                        this.offsets[key] = offset {
                            normalOffset = value.normalOffset
                            topOffset = value.topOffset
                        }
                    }
                }
            })
        }.onFailure { handleGrpcException(it) }.getOrThrow()
        return DirectMessagePage(
            sessions = reply.sessionsList
                .mapNotNull(::mapMainSession)
                .filter { it.talkerId > 0L },
            hasMore = reply.paginationParams.hasMore,
            nextOffsets = reply.paginationParams.offsetsMap.mapValues { (_, value) ->
                DirectMessageOffset(
                    normalOffset = value.normalOffset,
                    topOffset = value.topOffset
                )
            },
            actions = reply.threeDotItemsList.map(::mapAction),
            outsideActions = reply.outsideItemList.map(::mapAction)
        )
    }

    private suspend fun getInterfaceSessions(
        cursor: Long? = null,
        pageSize: Int = 20
    ): DirectMessagePage {
        val reply = runCatching {
            requireService().getSessions(reqGetSessions {
                sessionType = 1
                groupFold = 1
                unfollowFold = 0
                sortRule = 2
                size = pageSize.coerceIn(1, 100)
                cursor?.takeIf { it > 0L }?.let { endTs = it - 1 }
            })
        }.onFailure { handleGrpcException(it) }.getOrThrow()
        val sessions = reply.sessionListList
            .map(::mapSession)
            .filter { it.talkerId > 0L }
        return DirectMessagePage(
            sessions = fillSessionProfiles(sessions),
            hasMore = reply.hasMore != 0,
            nextCursor = sessions.lastOrNull()?.timestampMicros
        )
    }

    suspend fun getConversation(
        talkerId: Long,
        endSeqno: Long? = null,
        pageSize: Int = 20
    ): DirectMessageHistoryPage {
        val reply = runCatching {
            requireService().syncFetchSessionMsgs(reqSessionMsg {
                this.talkerId = talkerId
                sessionType = 1
                size = pageSize.coerceIn(1, 50)
                devId = "1"
                endSeqno?.takeIf { it > 0L }?.let {
                    beginSeqno = 0L
                    this.endSeqno = it
                }
            })
        }.onFailure { handleGrpcException(it) }.getOrThrow()
        val messages = reply.messagesList
            .filterNot { it.msgType == MsgType.EN_MSG_TYPE_DRAW_BACK }
            .map(::mapMessage)
            .sortedWith(compareBy<DirectMessage> { it.timestampSeconds }.thenBy { it.msgSeqno })
        return DirectMessageHistoryPage(
            messages = messages,
            hasMore = reply.hasMore != 0,
            minSeqno = reply.minSeqno.takeIf { it > 0L } ?: messages.minOfOrNull { it.msgSeqno },
            maxSeqno = reply.maxSeqno.takeIf { it > 0L } ?: messages.maxOfOrNull { it.msgSeqno },
            emotes = reply.eInfosList
                .filter { it.text.isNotBlank() && (it.gifUrl.isNotBlank() || it.url.isNotBlank()) }
                .distinctBy { it.text }
                .map {
                    DirectMessageEmote(
                        text = it.text,
                        url = it.gifUrl.ifBlank { it.url },
                        size = it.size
                    )
                }
        )
    }

    suspend fun getEmotePackages(): List<DynamicEmotePackageDraft> {
        val response = BiliHttpApi.getDynamicEmotes(
            business = "reply",
            sessData = authRepository.sessionData,
            dedeUserID = authRepository.mid,
            buvid3 = authRepository.buvid3
        )
        if (response.code != 0) throw Exception(response.message)
        return response.getResponseData().packages
            .map {
                DynamicEmotePackageDraft(
                    url = it.url,
                    type = it.type,
                    emotes = it.emote
                        .filter { emote -> emote.text.isNotBlank() }
                        .distinctBy { emote -> emote.text }
                        .map { emote ->
                            DynamicEmoteDraft(
                                text = emote.text,
                                url = emote.url,
                                size = emote.meta.size,
                                alias = emote.meta.alias
                            )
                        }
                )
            }
            .filter { it.emotes.isNotEmpty() }
    }

    suspend fun sendText(
        senderUid: Long,
        receiverId: Long,
        text: String
    ) {
        val content = buildJsonObject {
            put("content", text)
        }.toString()
        sendRaw(
            senderUid = senderUid,
            receiverId = receiverId,
            content = content,
            msgType = MsgType.EN_MSG_TYPE_TEXT
        )
    }

    suspend fun uploadImage(fileName: String, bytes: ByteArray): DirectMessageImageDraft {
        val response = BiliHttpApi.uploadDynamicImage(
            fileName = fileName,
            bytes = bytes,
            csrf = authRepository.biliJct ?: error("账号未登录"),
            sessData = authRepository.sessionData ?: error("账号未登录"),
            dedeUserID = authRepository.mid,
            buvid3 = authRepository.buvid3,
            category = null,
            biz = "im"
        )
        if (response.code != 0) throw Exception(response.message)
        val data = response.getResponseData()
        if (data.imageUrl.isBlank()) throw Exception("图片上传失败")
        return DirectMessageImageDraft(
            url = data.imageUrl,
            width = data.imageWidth,
            height = data.imageHeight,
            imageType = fileName.substringAfterLast('.', "jpg").lowercase().ifBlank { "jpg" },
            size = data.imgSize
        )
    }

    suspend fun sendImage(
        senderUid: Long,
        receiverId: Long,
        image: DirectMessageImageDraft
    ) {
        val content = buildJsonObject {
            put("url", image.url)
            put("height", image.height)
            put("width", image.width)
            put("imageType", image.imageType)
            put("original", 1)
            put("size", image.size)
        }.toString()
        sendRaw(
            senderUid = senderUid,
            receiverId = receiverId,
            content = content,
            msgType = MsgType.EN_MSG_TYPE_PIC
        )
    }

    suspend fun withdraw(
        senderUid: Long,
        receiverId: Long,
        msgKey: Long
    ) {
        sendRaw(
            senderUid = senderUid,
            receiverId = receiverId,
            content = msgKey.toString(),
            msgType = MsgType.EN_MSG_TYPE_DRAW_BACK
        )
    }

    suspend fun report(
        message: DirectMessage,
        reasonType: Int,
        reasonDesc: String
    ) {
        if (message.senderUid <= 0L || message.msgKey <= 0L) return
        val response = BiliHttpApi.reportDirectMessage(
            accusedUid = message.senderUid,
            msgKey = message.msgKey,
            reasonType = reasonType,
            reasonDesc = reasonDesc,
            csrf = authRepository.biliJct ?: error("账号未登录"),
            sessData = authRepository.sessionData ?: error("账号未登录"),
            dedeUserID = authRepository.mid,
            buvid3 = authRepository.buvid3
        )
        if (response.code != 0) throw Exception(response.message)
    }

    private suspend fun sendRaw(
        senderUid: Long,
        receiverId: Long,
        content: String,
        msgType: MsgType
    ) {
        runCatching {
            requireService().sendMsg(reqSendMsg {
                this.msg = msg {
                    this.senderUid = senderUid
                    receiverType = RecverType.EN_RECVER_TYPE_PEER
                    this.receiverId = receiverId
                    this.msgType = msgType
                    this.content = content
                    timestamp = System.currentTimeMillis() / 1000
                    msgStatus = 0
                    msgSource = MsgSource.EN_MSG_SOURCE_ANDRIOD
                    newFaceVersion = 1
                }
                devId = UUID.randomUUID().toString()
            })
        }.onFailure { handleGrpcException(it) }.getOrThrow()
    }

    suspend fun markRead(talkerId: Long, seqno: Long) {
        if (seqno <= 0L) return
        runCatching {
            requireService().updateAck(reqUpdateAck {
                this.talkerId = talkerId
                sessionType = 1
                ackSeqno = seqno
            })
        }.onFailure { handleGrpcException(it) }.getOrThrow()
    }

    suspend fun setPinned(talkerId: Long, pinned: Boolean) {
        runCatching {
            requireService().setTop(reqSetTop {
                this.talkerId = talkerId
                sessionType = 1
                opType = if (pinned) 0 else 1
            })
        }.onFailure { handleGrpcException(it) }.getOrThrow()
    }

    suspend fun removeSession(talkerId: Long) {
        runCatching {
            requireService().removeSession(reqRemoveSession {
                this.talkerId = talkerId
                sessionType = 1
            })
        }.onFailure { handleGrpcException(it) }.getOrThrow()
    }

    suspend fun clearAllUnread() {
        runCatching {
            requireAppService().clearUnread(clearUnreadReq {
                pageType = SessionPageType.SESSION_PAGE_TYPE_HOME
            })
        }.onFailure { handleGrpcException(it) }.getOrThrow()
    }

    suspend fun deleteSessionList() {
        runCatching {
            requireAppService().deleteSessionList(deleteSessionListReq {
                pageType = SessionPageType.SESSION_PAGE_TYPE_HOME
            })
        }.onFailure { handleGrpcException(it) }.getOrThrow()
    }

    suspend fun getMessageFeed(
        type: MessageFeedType,
        cursorId: Long? = null,
        cursorTime: Long? = null
    ): MessageFeedPage {
        if (type == MessageFeedType.System) {
            return getSystemMessageFeed(cursorId)
        }
        val data = BiliHttpApi.getMessageFeedData(
            type = type,
            cursorId = cursorId,
            cursorTime = cursorTime,
            sessData = authRepository.sessionData,
            dedeUserID = authRepository.mid,
            buvid3 = authRepository.buvid3
        )
        return when (type) {
            MessageFeedType.Reply -> replyMessageFeed(data)
            MessageFeedType.At -> atMessageFeed(data)
            MessageFeedType.Like -> likeMessageFeed(data)
            MessageFeedType.System -> getSystemMessageFeed(cursorId)
        }
    }

    suspend fun deleteMessageFeedItem(item: MessageFeedItem) {
        if (item.id.isBlank()) return
        when (item.type) {
            MessageFeedType.System -> BiliHttpApi.deleteSystemMessageFeedItem(
                id = item.id,
                csrf = authRepository.biliJct,
                sessData = authRepository.sessionData,
                dedeUserID = authRepository.mid,
                buvid3 = authRepository.buvid3
            )

            else -> BiliHttpApi.deleteMessageFeedItem(
                type = item.type,
                id = item.id,
                csrf = authRepository.biliJct,
                sessData = authRepository.sessionData,
                dedeUserID = authRepository.mid,
                buvid3 = authRepository.buvid3
            )
        }
    }

    private suspend fun getSystemMessageFeed(cursor: Long?): MessageFeedPage {
        val data = BiliHttpApi.getSystemMessageFeedData(
            cursor = cursor,
            sessData = authRepository.sessionData,
            dedeUserID = authRepository.mid,
            buvid3 = authRepository.buvid3
        )
        val items = data.mapNotNull(::systemMessageFeedItem)
        if (cursor == null) {
            items.firstOrNull()?.timestampSeconds?.let { firstCursor ->
                runCatching {
                    BiliHttpApi.updateSystemMessageCursor(
                        cursor = firstCursor,
                        csrf = authRepository.biliJct,
                        sessData = authRepository.sessionData,
                        dedeUserID = authRepository.mid,
                        buvid3 = authRepository.buvid3
                    )
                }
            }
        }
        return MessageFeedPage(
            items = items,
            hasMore = items.isNotEmpty(),
            cursorId = items.lastOrNull()?.timestampSeconds
        )
    }

    private fun replyMessageFeed(data: JsonObject): MessageFeedPage {
        val cursor = data.obj("cursor")
        val items = data.array("items")
            ?.mapNotNull(::replyMessageFeedItem)
            .orEmpty()
        return MessageFeedPage(
            items = items,
            hasMore = cursor?.bool("is_end") != true && items.isNotEmpty(),
            cursorId = cursor?.long("id"),
            cursorTime = cursor?.long("time")
        )
    }

    private fun atMessageFeed(data: JsonObject): MessageFeedPage {
        val cursor = data.obj("cursor")
        val items = data.array("items")
            ?.mapNotNull(::atMessageFeedItem)
            .orEmpty()
        return MessageFeedPage(
            items = items,
            hasMore = cursor?.bool("is_end") != true && items.isNotEmpty(),
            cursorId = cursor?.long("id"),
            cursorTime = cursor?.long("time")
        )
    }

    private fun likeMessageFeed(data: JsonObject): MessageFeedPage {
        val latest = data.obj("latest")
            ?.array("items")
            ?.mapNotNull { likeMessageFeedItem(it, "最新") }
            .orEmpty()
        val total = data.obj("total")
        val totalItems = total
            ?.array("items")
            ?.mapNotNull { likeMessageFeedItem(it, "累计") }
            .orEmpty()
        val cursor = total?.obj("cursor")
        return MessageFeedPage(
            items = latest + totalItems,
            hasMore = cursor?.bool("is_end") != true && totalItems.isNotEmpty(),
            cursorId = cursor?.long("id"),
            cursorTime = cursor?.long("time")
        )
    }

    private fun replyMessageFeedItem(element: JsonElement): MessageFeedItem? {
        val item = element.objectOrNull() ?: return null
        val user = item.obj("user") ?: JsonObject(emptyMap())
        val content = item.obj("item") ?: JsonObject(emptyMap())
        val nickname = user.string("nickname").ifBlank { "用户" }
        val business = content.string("business").ifBlank { "内容" }
        val counts = item.int("counts") ?: 0
        val title = buildString {
            append(nickname)
            if (item.int("is_multi") == 1) append(" 等人")
            append(" 对我的")
            append(business)
            append("发布了")
            if (counts > 0) append(counts).append("条")
            append("评论")
        }
        return MessageFeedItem(
            id = item.string("id"),
            type = MessageFeedType.Reply,
            userMid = user.long("mid"),
            username = nickname,
            avatar = user.string("avatar"),
            title = title,
            body = content.string("source_content"),
            quote = firstNonBlank(
                content.string("target_reply_content"),
                content.string("root_reply_content")
            ),
            timestampSeconds = item.long("reply_time"),
            jumpUri = content.string("native_uri"),
            deleteType = 1
        )
    }

    private fun atMessageFeedItem(element: JsonElement): MessageFeedItem? {
        val item = element.objectOrNull() ?: return null
        val user = item.obj("user") ?: JsonObject(emptyMap())
        val content = item.obj("item") ?: JsonObject(emptyMap())
        val nickname = user.string("nickname").ifBlank { "用户" }
        val business = content.string("business").ifBlank { "内容" }
        return MessageFeedItem(
            id = item.string("id"),
            type = MessageFeedType.At,
            userMid = user.long("mid"),
            username = nickname,
            avatar = user.string("avatar"),
            title = "$nickname 在${business}中@了我",
            body = content.string("source_content"),
            image = content.string("image"),
            timestampSeconds = item.long("at_time"),
            jumpUri = content.string("native_uri"),
            deleteType = 2
        )
    }

    private fun likeMessageFeedItem(element: JsonElement, section: String): MessageFeedItem? {
        val item = element.objectOrNull() ?: return null
        val content = item.obj("item") ?: JsonObject(emptyMap())
        val users = item.array("users").orEmpty().mapNotNull { it.objectOrNull() }
        val firstUser = users.firstOrNull() ?: JsonObject(emptyMap())
        val nickname = firstUser.string("nickname").ifBlank { "用户" }
        val business = content.string("business").ifBlank { "内容" }
        val counts = item.int("counts") ?: users.size
        val title = buildString {
            append(nickname)
            if (counts > 1) append(" 等").append(counts).append("人")
            append(" 赞了我的")
            append(business)
        }
        return MessageFeedItem(
            id = item.string("id"),
            type = MessageFeedType.Like,
            section = section,
            username = nickname,
            avatar = firstUser.string("avatar"),
            title = title,
            body = content.string("title"),
            image = content.string("image"),
            timestampSeconds = item.long("like_time"),
            jumpUri = content.string("native_uri"),
            deleteType = 0
        )
    }

    private fun systemMessageFeedItem(element: JsonElement): MessageFeedItem? {
        val item = element.objectOrNull() ?: return null
        val cursor = item.long("cursor")
        return MessageFeedItem(
            id = item.string("id"),
            type = MessageFeedType.System,
            title = item.string("title").ifBlank { "系统通知" },
            body = systemMessageContent(item.string("content")),
            timeText = item.string("time_at"),
            timestampSeconds = cursor,
            deleteType = 4
        )
    }

    private fun systemMessageContent(raw: String): String {
        if (raw.isBlank()) return ""
        return runCatching {
            val content = json.parseToJsonElement(raw).jsonObject.string("web")
            content.ifBlank { raw }
        }.getOrDefault(raw)
    }

    private fun mapMainSession(session: AppDirectSession): DirectMessageSession? {
        if (!session.id.hasPrivateId()) return null
        val talkerId = session.id.privateId.talkerUid
        val summary = listOf(
            session.msgSummary.prefixText,
            session.msgSummary.rawMsg
        ).filter { it.isNotBlank() }.joinToString(" ").ifBlank { " " }
        return DirectMessageSession(
            talkerId = talkerId,
            name = session.sessionInfo.sessionName.ifBlank { talkerId.toString() },
            face = avatarUrl(session.sessionInfo.avatar),
            summary = summary,
            timestampMicros = session.timestamp,
            unreadCount = session.unread.number.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            hasUnread = session.hasUnread() && session.unread.style != UnreadStyle.UNREAD_STYLE_NONE,
            maxSeqno = session.sequenceNumber,
            isPinned = session.isPinned,
            isMuted = session.isMuted,
            isFollowed = false,
            isLive = session.sessionInfo.isLive
        )
    }

    private fun mapAction(item: ThreeDotItem): DirectMessageAction = DirectMessageAction(
        title = item.title,
        url = item.url,
        type = item.typeValue,
        hasRedDot = item.hasRedDot
    )

    private fun avatarUrl(avatar: AvatarItem): String {
        val resources = avatar.fallbackLayers.layersList.map { it.resource } +
                avatar.layersList.flatMap { group -> group.layersList.map { it.resource } }
        return resources.firstNotNullOfOrNull(::resourceUrl).orEmpty()
    }

    private fun resourceUrl(resource: BasicLayerResource): String? = when {
        resource.hasResImage() -> resource.resImage.imageSrc.remote.url
        resource.hasResAnimation() -> resource.resAnimation.webpSrc.remote.url
        resource.hasResNativeDraw() -> resource.resNativeDraw.drawSrc.remote.url
        else -> null
    }?.takeIf { it.isNotBlank() }

    private suspend fun fillSessionProfiles(
        sessions: List<DirectMessageSession>
    ): List<DirectMessageSession> {
        if (sessions.isEmpty()) return sessions
        val detailMap = runCatching {
            requireService().batchSessDetail(reqSessionDetails {
                sessIds.addAll(
                    sessions.map { session ->
                        reqSessionDetail {
                            talkerId = session.talkerId
                            sessionType = 1
                            uid = authRepository.mid ?: 0L
                        }
                    }
                )
            }).sessInfosList.associateBy { it.talkerId }
        }.getOrDefault(emptyMap())
        return sessions.map { session ->
            val detail = detailMap[session.talkerId]
            val detailSession = detail?.let(::mapSession)
            session.copy(
                name = session.name.takeUnless { it.isBlank() || it == session.talkerId.toString() }
                    ?: detailSession?.name?.takeUnless { it.isBlank() || it == session.talkerId.toString() }
                    ?: session.name,
                face = session.face.ifBlank { detailSession?.face.orEmpty() },
                isLive = session.isLive || detailSession?.isLive == true,
                isFollowed = session.isFollowed || detailSession?.isFollowed == true
            )
        }
    }

    private fun mapSession(session: SessionInfo): DirectMessageSession {
        val account = session.accountInfo
        val name = account.name.ifBlank {
            session.groupName.ifBlank { session.talkerId.toString() }
        }
        return DirectMessageSession(
            talkerId = session.talkerId,
            name = name,
            face = account.picUrl,
            summary = summarize(session.lastMsg),
            timestampMicros = session.sessionTs,
            unreadCount = session.unreadCount,
            hasUnread = session.unreadCount > 0,
            maxSeqno = session.maxSeqno,
            isPinned = session.topTs > 0L,
            isMuted = session.isDnd != 0,
            isFollowed = session.isFollow != 0,
            isLive = session.liveStatus == 1
        )
    }

    private fun mapMessage(item: Msg): DirectMessage = DirectMessage(
        senderUid = item.senderUid,
        receiverId = item.receiverId,
        msgType = item.msgTypeValue,
        content = parseContent(item),
        rawContent = item.content,
        msgSeqno = item.msgSeqno,
        msgKey = item.msgKey,
        timestampSeconds = item.timestamp,
        status = item.msgStatus,
        source = item.msgSourceValue
    )

    private fun summarize(item: Msg): String {
        if (item.msgTypeValue == 16) return "[卡片]"
        return when (item.msgType) {
            MsgType.EN_MSG_TYPE_TEXT -> textFromContent(item.content).ifBlank { "[文本]" }
            MsgType.EN_MSG_TYPE_PIC, MsgType.EN_MSG_TYPE_CUSTOM_FACE -> "[图片]"
            MsgType.EN_MSG_TYPE_AUDIO -> "[语音]"
            MsgType.EN_MSG_TYPE_SHARE, MsgType.EN_MSG_TYPE_SHARE_V2 -> "[分享]"
            MsgType.EN_MSG_TYPE_VIDEO_CARD -> "[视频]"
            MsgType.EN_MSG_TYPE_ARTICLE_CARD -> "[专栏]"
            MsgType.EN_MSG_TYPE_PICTURE_CARD -> "[图片卡片]"
            MsgType.EN_MSG_TYPE_COMMON_SHARE_CARD -> "[分享]"
            MsgType.EN_MSG_TYPE_NOTIFY_MSG -> noticeFromContent(item.content).ifBlank { "[通知]" }
            MsgType.EN_MSG_TYPE_DRAW_BACK -> "[撤回消息]"
            MsgType.UNRECOGNIZED, MsgType.EN_INVALID_MSG_TYPE -> ""
            else -> "[消息]"
        }
    }

    private fun parseContent(item: Msg): DirectMessageContent {
        if (item.msgTypeValue == 16) return type16CardFromContent(item.content)
        return when (item.msgType) {
            MsgType.EN_MSG_TYPE_TEXT -> DirectMessageContent.Text(textFromContent(item.content))
            MsgType.EN_MSG_TYPE_PIC, MsgType.EN_MSG_TYPE_CUSTOM_FACE -> imageFromContent(item.content)
            MsgType.EN_MSG_TYPE_NOTIFY_MSG -> notifyCardFromContent(item.content)
            MsgType.EN_MSG_TYPE_SHARE_V2 -> shareCardFromContent(item.content)
            MsgType.EN_MSG_TYPE_VIDEO_CARD -> videoCardFromContent(item.content)
            MsgType.EN_MSG_TYPE_ARTICLE_CARD -> articleCardFromContent(item.content)
            MsgType.EN_MSG_TYPE_PICTURE_CARD -> pictureCardFromContent(item.content)
            MsgType.EN_MSG_TYPE_COMMON_SHARE_CARD -> commonShareCardFromContent(item.content)
            else -> DirectMessageContent.Unsupported(summarize(item).ifBlank { item.content })
        }
    }

    private fun imageFromContent(raw: String): DirectMessageContent {
        val obj = parseObject(raw) ?: return DirectMessageContent.Unsupported(raw)
        val url = obj.string("url")
        if (url.isBlank()) return DirectMessageContent.Unsupported(raw)
        return DirectMessageContent.Image(
            url = url,
            width = obj.int("width") ?: 0,
            height = obj.int("height") ?: 0
        )
    }

    private fun notifyCardFromContent(raw: String): DirectMessageContent {
        val obj = parseObject(raw) ?: return DirectMessageContent.Notice(raw)
        val modules = obj.array("modules")?.mapNotNull { item ->
            val module = runCatching { item.jsonObject }.getOrNull()
            val title = module?.string("title").orEmpty()
            val detail = module?.string("detail").orEmpty()
            when {
                title.isNotBlank() && detail.isNotBlank() -> "$title：$detail"
                detail.isNotBlank() -> detail
                else -> null
            }
        }.orEmpty()
        val text = listOf(obj.string("text"), modules.joinToString("\n"))
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .ifBlank { noticeFromContent(raw) }
        val jumpTexts = listOf(
            obj.string("jump_text"),
            obj.string("jump_text_2"),
            obj.string("jump_text_3")
        ).filter { it.isNotBlank() }
        return DirectMessageContent.Card(
            title = obj.string("title").ifBlank { "通知" },
            subtitle = (listOf(text) + jumpTexts).filter { it.isNotBlank() }.joinToString("\n"),
            cover = "",
            badge = "通知",
            jumpUrl = firstNonBlank(
                obj.string("jump_uri"),
                obj.string("jump_uri_2"),
                obj.string("jump_uri_3")
            )
        )
    }

    private fun videoCardFromContent(raw: String): DirectMessageContent {
        val obj = parseObject(raw) ?: return DirectMessageContent.Unsupported(raw)
        val bvid = obj.string("bvid")
        val duration = obj.long("times")?.takeIf { it > 0L }?.let(::formatDuration).orEmpty()
        val attach = obj.obj("attach_msg")?.string("content").orEmpty()
        return DirectMessageContent.Card(
            title = obj.string("title").ifBlank { if (duration.isBlank()) "内容已失效" else "视频" },
            subtitle = firstNonBlank(attach, duration),
            cover = obj.string("cover"),
            badge = "视频",
            jumpUrl = bvid.takeIf { it.isNotBlank() }?.let { "https://www.bilibili.com/video/$it" }.orEmpty()
        )
    }

    private fun articleCardFromContent(raw: String): DirectMessageContent {
        val obj = parseObject(raw) ?: return DirectMessageContent.Unsupported(raw)
        val rid = firstNonBlank(obj.string("rid"), obj.string("id"))
        return DirectMessageContent.Card(
            title = obj.string("title").ifBlank { "专栏" },
            subtitle = obj.string("summary"),
            cover = obj.array("image_urls")?.firstString().orEmpty(),
            badge = "专栏",
            jumpUrl = rid.takeIf { it.isNotBlank() }?.let { "https://www.bilibili.com/read/cv$it" }.orEmpty()
        )
    }

    private fun pictureCardFromContent(raw: String): DirectMessageContent {
        val obj = parseObject(raw) ?: return DirectMessageContent.Unsupported(raw)
        return DirectMessageContent.Card(
            title = obj.string("title"),
            subtitle = obj.string("desc"),
            cover = firstNonBlank(obj.string("pic_url"), obj.string("cover"), obj.string("url")),
            badge = "图片",
            jumpUrl = obj.string("jump_url")
        )
    }

    private fun commonShareCardFromContent(raw: String): DirectMessageContent {
        val obj = parseObject(raw) ?: return DirectMessageContent.Unsupported(raw)
        val source = obj.string("source")
        val id = firstNonBlank(obj.string("sourceID"), obj.string("source_id"), obj.string("id"))
        val badge = source.ifBlank { "分享" }
        val jumpUrl = when (source) {
            "直播" -> id.takeIf { it.isNotBlank() }?.let { "https://live.bilibili.com/$it" }.orEmpty()
            else -> obj.string("jump_url")
        }
        return DirectMessageContent.Card(
            title = obj.string("title").ifBlank { badge },
            subtitle = listOf(obj.string("author"), badge).filter { it.isNotBlank() }.joinToString(" · "),
            cover = firstNonBlank(obj.string("cover"), obj.string("thumb"), obj.string("pic")),
            badge = badge,
            jumpUrl = jumpUrl
        )
    }

    private fun shareCardFromContent(raw: String): DirectMessageContent {
        val obj = parseObject(raw) ?: return DirectMessageContent.Unsupported(raw)
        val sourceType = obj.int("source") ?: obj.string("source").toIntOrNull()
        val badge = when (sourceType) {
            2 -> "相簿"
            5 -> "视频"
            6 -> "专栏"
            11 -> "动态"
            16 -> "番剧"
            else -> "分享"
        }
        val id = obj.string("id")
        val jumpUrl = when (sourceType) {
            5 -> firstNonBlank(
                obj.string("bvid").takeIf { it.isNotBlank() }?.let { "https://www.bilibili.com/video/$it" }.orEmpty(),
                id.takeIf { it.isNotBlank() }?.let { "https://www.bilibili.com/video/av$it" }.orEmpty()
            )
            6 -> id.takeIf { it.isNotBlank() }?.let { "https://www.bilibili.com/read/cv$it" }.orEmpty()
            11 -> id.takeIf { it.isNotBlank() }?.let { "https://t.bilibili.com/$it" }.orEmpty()
            16 -> id.takeIf { it.isNotBlank() }?.let { "https://www.bilibili.com/bangumi/play/ep$it" }.orEmpty()
            else -> obj.string("jump_url")
        }
        val title = firstNonBlank(obj.string("title"), obj.string("headline"), badge)
        val subtitle = listOf(
            obj.string("headline").takeUnless { it == title }.orEmpty(),
            obj.string("author")
        ).filter { it.isNotBlank() }.joinToString(" · ")
        return DirectMessageContent.Card(
            title = title,
            subtitle = subtitle,
            cover = firstNonBlank(obj.string("thumb"), obj.string("cover"), obj.string("pic")),
            badge = badge,
            jumpUrl = jumpUrl
        )
    }

    private fun type16CardFromContent(raw: String): DirectMessageContent {
        val obj = parseObject(raw) ?: return DirectMessageContent.Unsupported(raw)
        val subCard = obj.array("sub_cards")?.firstObject()
        val fields = listOf(
            subCard?.string("field1").orEmpty(),
            subCard?.string("field2").orEmpty(),
            subCard?.string("field3").orEmpty()
        ).filter { it.isNotBlank() }
        return DirectMessageContent.Card(
            title = obj.string("main_title").ifBlank { fields.firstOrNull().orEmpty() },
            subtitle = fields.drop(1).joinToString("\n"),
            cover = subCard?.string("cover_url").orEmpty(),
            badge = "卡片",
            jumpUrl = subCard?.string("jump_url").orEmpty()
        )
    }

    private fun textFromContent(raw: String): String {
        val obj = parseObject(raw) ?: return raw
        return obj.string("content").ifBlank { raw }
    }

    private fun noticeFromContent(raw: String): String {
        val obj = parseObject(raw) ?: return raw
        obj.string("text").takeIf { it.isNotBlank() }?.let { return it }
        obj.string("title").takeIf { it.isNotBlank() }?.let { return it }
        val content = obj["content"]
        if (content is JsonPrimitive) {
            val nested = runCatching { json.parseToJsonElement(content.content).jsonArray }.getOrNull()
            nested?.joinToString("\n") { item ->
                item.jsonObject.string("text")
            }?.takeIf { it.isNotBlank() }?.let { return it }
        }
        if (content is JsonArray) {
            return content.joinToString("\n") { item -> item.jsonObject.string("text") }
        }
        return raw
    }

    private fun parseObject(raw: String): JsonObject? =
        runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull()

    private fun JsonObject.string(key: String): String =
        this[key]?.jsonPrimitive?.contentOrNull.orEmpty()

    private fun JsonObject.int(key: String): Int? =
        this[key]?.jsonPrimitive?.intOrNull

    private fun JsonObject.long(key: String): Long? =
        this[key]?.jsonPrimitive?.longOrNull

    private fun JsonObject.bool(key: String): Boolean? =
        this[key]?.jsonPrimitive?.booleanOrNull

    private fun JsonObject.obj(key: String): JsonObject? =
        runCatching { this[key]?.jsonObject }.getOrNull()

    private fun JsonObject.array(key: String): JsonArray? =
        runCatching { this[key]?.jsonArray }.getOrNull()

    private fun JsonElement.objectOrNull(): JsonObject? =
        runCatching { jsonObject }.getOrNull()

    private fun JsonArray.firstString(): String? =
        firstOrNull()?.jsonPrimitive?.contentOrNull

    private fun JsonArray.firstObject(): JsonObject? =
        firstOrNull()?.let { runCatching { it.jsonObject }.getOrNull() }

    private fun firstNonBlank(vararg values: String): String =
        values.firstOrNull { it.isNotBlank() }.orEmpty()

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, secs)
        } else {
            "%02d:%02d".format(minutes, secs)
        }
    }
}
