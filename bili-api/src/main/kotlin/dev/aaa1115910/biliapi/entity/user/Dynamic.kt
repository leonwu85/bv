package dev.aaa1115910.biliapi.entity.user

import bilibili.app.dynamic.v2.DescType
import bilibili.app.dynamic.v2.DynModuleType
import bilibili.app.dynamic.v2.LinkNodeType
import bilibili.app.dynamic.v2.Module
import bilibili.app.dynamic.v2.ModuleDynamic.ModuleItemCase
import bilibili.app.dynamic.v2.Paragraph
import bilibili.app.dynamic.v2.TextNode
import bilibili.app.dynamic.v2.VideoType
import bilibili.app.dynamic.v2.emoteOrNull
import dev.aaa1115910.biliapi.entity.Picture
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.internal.toLongOrDefault

/**
 * 富文本节点类型
 */
enum class RichTextNodeType {
    Text, Emoji, At, Other
}

/**
 * 富文本节点，统一 Web API 和 gRPC 的表情数据表示
 */
data class RichTextNode(
    val text: String,
    val type: RichTextNodeType,
    val emoji: RichTextEmoji? = null,
    val rid: String? = null,
    val uri: String? = null
)

/**
 * 富文本表情数据
 * @param iconUrl 表情图片 URL
 * @param size 表情尺寸，1=小表情(行内), 2=大表情
 */
data class RichTextEmoji(
    val iconUrl: String,
    val size: Int = 1
)

/**
 * 将 Web API 的 Desc.richTextNodes 转换为统一的 RichTextNode 列表
 */
private fun dev.aaa1115910.biliapi.http.entity.dynamic.DynamicItem.Modules.Dynamic.Desc.toRichTextNodes(): List<RichTextNode> =
    richTextNodes.map { node ->
        when (node.type) {
            "RICH_TEXT_NODE_TYPE_EMOJI" -> RichTextNode(
                text = node.origText.ifBlank { node.text },
                type = RichTextNodeType.Emoji,
                emoji = node.emoji?.let {
                    RichTextEmoji(
                        iconUrl = it.webpUrl.ifBlank { it.gifUrl.ifBlank { it.iconUrl } },
                        size = it.size
                    )
                }
            )

            "RICH_TEXT_NODE_TYPE_AT" -> RichTextNode(
                text = node.origText.ifBlank { node.text },
                type = RichTextNodeType.At,
                rid = node.rid,
                uri = node.jumpUrl
            )

            "RICH_TEXT_NODE_TYPE_VOTE" -> RichTextNode(
                text = node.origText.ifBlank { node.text },
                type = RichTextNodeType.Other,
                rid = node.rid,
                uri = node.jumpUrl
            )

            else -> RichTextNode(
                text = node.origText.ifBlank { node.text },
                type = RichTextNodeType.Text,
                rid = node.rid,
                uri = node.jumpUrl
            )
        }
    }.filterNot { it.type == RichTextNodeType.Other && it.rid?.toLongOrNull() != null }

/**
 * 将 gRPC 的 TextNode 列表（来自 ModuleOpusSummary）转换为统一的 RichTextNode 列表
 */
private fun List<TextNode>.toRichTextNodes(): List<RichTextNode> =
    map { node ->
        when (node.nodeType) {
            TextNode.TextNodeType.EMOTE -> RichTextNode(
                text = node.rawText,
                type = RichTextNodeType.Emoji,
                emoji = node.emoteOrNull?.let {
                    RichTextEmoji(
                        iconUrl = it.emoteUrl,
                        size = it.emoteWidth.emojiSize.coerceIn(1, 2)
                    )
                }
            )

            TextNode.TextNodeType.AT -> RichTextNode(
                text = node.rawText,
                type = RichTextNodeType.At
            )

            TextNode.TextNodeType.BIZ_LINK -> RichTextNode(
                text = node.rawText,
                type = if (node.link.linkTypeEnum == LinkNodeType.VOTE) {
                    RichTextNodeType.Other
                } else {
                    RichTextNodeType.Text
                },
                rid = node.link.bizId,
                uri = node.link.link
            )

            else -> RichTextNode(
                text = node.rawText,
                type = RichTextNodeType.Text
            )
        }
    }.filterNot { it.type == RichTextNodeType.Other && it.rid?.toLongOrNull() != null }

/**
 * 将 gRPC 的 Description 列表（来自 ModuleDesc）转换为统一的 RichTextNode 列表
 */
private fun List<bilibili.app.dynamic.v2.Description>.toRichTextNodesFromDesc(): List<RichTextNode> =
    map { desc ->
        when (desc.type) {
            DescType.desc_type_emoji -> RichTextNode(
                text = desc.text,
                type = RichTextNodeType.Emoji,
                emoji = RichTextEmoji(
                    iconUrl = desc.iconUrl,
                    size = desc.emojiSize.coerceIn(1, 2)
                )
            )

            DescType.desc_type_aite -> RichTextNode(
                text = desc.text,
                type = RichTextNodeType.At,
                rid = desc.rid,
                uri = desc.uri
            )

            DescType.desc_type_vote -> RichTextNode(
                text = desc.text,
                type = RichTextNodeType.Other,
                rid = desc.rid,
                uri = desc.uri
            )

            else -> RichTextNode(
                text = desc.text,
                type = RichTextNodeType.Text,
                rid = desc.rid,
                uri = desc.uri
            )
        }
    }.filterNot { it.type == RichTextNodeType.Other && it.rid?.toLongOrNull() != null }

data class DynamicData(
    val dynamics: List<DynamicItem>,
    val hasMore: Boolean,
    val historyOffset: String,
    val updateBaseline: String
) {
    companion object {
        private val logger = KotlinLogging.logger { }
        private val availableDynamicTypes = listOf(
            DynamicType.Av,
            DynamicType.UgcSeason,
            DynamicType.Draw,
            DynamicType.Forward,
            DynamicType.Word,
            DynamicType.Live,
            DynamicType.LiveRcmd,
            DynamicType.Pgc,
            DynamicType.Article,
            DynamicType.Medialist,
            DynamicType.CoursesSeason,
            DynamicType.SubscriptionNew,
            DynamicType.CommonSquare,
            DynamicType.None
        )
        private val availableAppDynamicTypes = availableDynamicTypes.mapNotNull { it.appValue }

        fun fromDynamicData(data: dev.aaa1115910.biliapi.http.entity.dynamic.DynamicData) =
            DynamicData(
                dynamics = data.items
                    .mapNotNull {
                        if (DynamicType.fromWebValueOrNull(it.type) !in availableDynamicTypes) {
                            logger.warn { "unknown dynamic type ${it.type}, up: ${it.modules.moduleAuthor?.name}, date: ${it.modules.moduleAuthor?.pubTime}" }
                            return@mapNotNull null
                        }

                        if (it.type == DynamicType.Forward.webValue) {
                            if (it.orig != null && DynamicType.fromWebValueOrNull(it.orig.type) !in availableDynamicTypes) {
                                logger.warn { "unknown dynamic forward type ${it.orig.type}, up: ${it.modules.moduleAuthor?.name}, date: ${it.modules.moduleAuthor?.pubTime}" }
                                return@mapNotNull null
                            }
                        }

                        DynamicItem.fromDynamicItem(it)
                    },
                hasMore = data.hasMore,
                historyOffset = data.offset,
                updateBaseline = data.updateBaseline
            ).also {
                logger.info { "updateBaseline: ${data.updateBaseline}" }
                logger.info { "offset: ${data.offset}" }
            }

        fun fromDynamicData(data: bilibili.app.dynamic.v2.DynAllReply) = DynamicData(
            dynamics = data.dynamicList.listList
                .mapNotNull {
                    if (!availableAppDynamicTypes.contains(it.cardType)) {
                        logger.warn { "unknown dynamic type ${it.cardType.name}, up: ${it.getAuthorModule()?.author?.name}, date: ${it.getAuthorModule()?.ptimeLabelText}" }
                        return@mapNotNull null
                    }

                    if (it.cardType == bilibili.app.dynamic.v2.DynamicType.forward) {
                        // source not exist
                        if (it.getItemNullModule() != null) {
                            return@mapNotNull DynamicItem.fromDynamicItem(it)
                        } else if (!availableAppDynamicTypes.contains(it.getDynamicModule()?.dynForward?.item?.cardType)) {
                            logger.warn { "unknown dynamic forward type ${it.getDynamicModule()?.dynForward?.item?.cardType}, up: ${it.getAuthorModule()?.author?.name}, date: ${it.getAuthorModule()?.ptimeLabelText}" }
                            return@mapNotNull null
                        }
                    }

                    DynamicItem.fromDynamicItem(it)
                },
            hasMore = data.dynamicList.hasMore,
            historyOffset = data.dynamicList.historyOffset,
            updateBaseline = data.dynamicList.updateBaseline
        ).also {
            logger.info { "updateBaseline: ${data.dynamicList.updateBaseline}" }
            logger.info { "historyOffset: ${data.dynamicList.historyOffset}" }
        }
    }
}

data class DynamicItem(
    var id: String? = null,
    var commentId: Long = 0,
    var commentType: Long = 0,
    var type: DynamicType,
    val author: DynamicAuthorModule,
    var video: DynamicVideoModule? = null,
    var ugcSeason: DynamicUgcSeasonModule? = null,
    var draw: DynamicDrawModule? = null,
    var word: DynamicWordModule? = null,
    var live: DynamicLiveModule? = null,
    var liveRcmd: DynamicLiveRcmdModule? = null,
    var pgc: DynamicPgcModule? = null,
    var article: DynamicArticleModule? = null,
    var blocked: DynamicBlockedModule? = null,
    var majorCard: DynamicMajorCardModule? = null,
    var none: DynamicNoneModule? = null,
    var vote: DynamicVoteModule? = null,
    val footer: DynamicFooterModule? = null,
    var orig: DynamicItem? = null,
    var jumpUrl: String? = null
) {
    companion object {
        fun fromDynamicItem(item: dev.aaa1115910.biliapi.http.entity.dynamic.DynamicItem): DynamicItem {
            val dynamicType = DynamicType.fromWebValueOrNull(item.type) ?: DynamicType.None
            val dynamicItem = DynamicItem(
                id = item.idStr.ifBlank { null },
                commentId = item.basic.commentIdStr.toLongOrDefault(0),
                commentType = item.basic.commentType,
                type = dynamicType,
                author = DynamicAuthorModule.fromModuleAuthor(item.modules.moduleAuthor),
                footer = DynamicFooterModule.fromModuleStat(item.modules.moduleStat)
            )
            val major = item.modules.moduleDynamic.major
            dynamicItem.blocked = major?.blocked?.let(DynamicBlockedModule::fromModuleBlocked)
            when (dynamicType) {
                DynamicType.Av -> dynamicItem.video =
                    major?.archive?.let(DynamicVideoModule::fromModuleArchive)

                DynamicType.UgcSeason -> dynamicItem.ugcSeason =
                    major?.ugcSeason?.let(DynamicUgcSeasonModule::fromModuleUgcSeason)

                DynamicType.Forward -> dynamicItem.apply {
                    word = DynamicWordModule.fromModuleDynamic(item.modules.moduleDynamic)
                    orig = item.orig?.let { fromDynamicItem(it) } ?: missingForwardItem()
                    jumpUrl = item.orig?.basic?.jumpUrl ?: item.orig?.jumpUrl
                }

                DynamicType.Word -> dynamicItem.word =
                    DynamicWordModule.fromModuleDynamic(item.modules.moduleDynamic)

                DynamicType.Draw -> dynamicItem.draw =
                    DynamicDrawModule.fromModuleDynamic(item.modules.moduleDynamic)

                DynamicType.LiveRcmd -> dynamicItem.liveRcmd =
                    DynamicLiveRcmdModule.fromModuleDynamic(item.modules.moduleDynamic)

                DynamicType.Live -> dynamicItem.live =
                    major?.live?.let(DynamicLiveModule::fromModuleLive)

                DynamicType.Pgc -> dynamicItem.pgc =
                    major?.pgc?.let(DynamicPgcModule::fromModulePgc)

                DynamicType.Article -> dynamicItem.article =
                    DynamicArticleModule.fromModuleDynamic(item.modules.moduleDynamic)

                DynamicType.Medialist -> dynamicItem.majorCard =
                    major?.medialist?.let(DynamicMajorCardModule::fromModuleMedialist)

                DynamicType.CoursesSeason -> dynamicItem.majorCard =
                    major?.courses?.let(DynamicMajorCardModule::fromModuleCourses)

                DynamicType.SubscriptionNew -> dynamicItem.liveRcmd =
                    DynamicLiveRcmdModule.fromModuleDynamic(item.modules.moduleDynamic)

                DynamicType.CommonSquare -> dynamicItem.majorCard =
                    (major?.common ?: major?.upowerCommon)?.let(DynamicMajorCardModule::fromModuleCommon)

                DynamicType.None -> dynamicItem.none =
                    major?.none?.let(DynamicNoneModule::fromModuleDynamic)
                        ?: DynamicNoneModule("unsupported dynamic")
            }
            dynamicItem.vote = DynamicVoteModule.fromModuleDynamic(item.modules.moduleDynamic)
            return dynamicItem
        }

        private fun missingForwardItem() = DynamicItem(
            type = DynamicType.None,
            author = DynamicAuthorModule("", "", -1, "", ""),
            none = DynamicNoneModule("source dynamic unavailable")
        )

        fun fromDynamicItem(
            item: bilibili.app.dynamic.v2.DynamicItem,
            isForwardItem: Boolean = false
        ): DynamicItem {
            val dynamicType = DynamicType.fromAppValue(item.cardType)
            val commentType: Long = when (item.cardType) {
                bilibili.app.dynamic.v2.DynamicType.av,
                bilibili.app.dynamic.v2.DynamicType.pgc,
                bilibili.app.dynamic.v2.DynamicType.ugc_season -> 1

                bilibili.app.dynamic.v2.DynamicType.draw -> 11

                bilibili.app.dynamic.v2.DynamicType.forward,
                bilibili.app.dynamic.v2.DynamicType.word,
                bilibili.app.dynamic.v2.DynamicType.article,
                bilibili.app.dynamic.v2.DynamicType.live_rcmd -> 17

                else -> 17
            }
            val dynamicItem = DynamicItem(
                id = item.extend.dynIdStr,
                commentId = item.extend.businessId.toLongOrDefault(0),
                commentType = commentType,
                // item.extend.rType 总是为 0
                //commentType = item.extend.rType,
                type = dynamicType,
                author = if (dynamicType == DynamicType.None) {
                    DynamicAuthorModule("", "", -1, "", "")
                } else if (isForwardItem) {
                    DynamicAuthorModule.fromExtendAndModuleAuthorForward(
                        item.extend, item.getAuthorModuleForward()!!
                    )
                } else {
                    DynamicAuthorModule.fromModuleAuthor(item.getAuthorModule()!!)
                },
                video = item.getDynamicModule()?.let {
                    DynamicVideoModule.fromModuleDynamic(
                        moduleDynamic = it,
                        text = item.getDescModule()?.text ?: ""
                    )
                },
                footer = if (!isForwardItem) {
                    // 获取动态详情时 module_list 中没有 module_stat，但 module_bottom 中包含了 module_stat
                    (item.getStatModule() ?: item.getBottomModule()?.moduleStat)
                        ?.let(DynamicFooterModule::fromModuleStat)
                } else null
            )

            when (dynamicType) {
                DynamicType.Av -> dynamicItem.video = item.getDynamicModule()?.let {
                    DynamicVideoModule.fromModuleDynamic(
                        moduleDynamic = it,
                        text = item.getDescModule()?.text ?: ""
                    )
                }

                DynamicType.UgcSeason -> dynamicItem.ugcSeason =
                    item.getDynamicModule()?.dynUgcSeason?.let(DynamicUgcSeasonModule::fromModuleUgcSeason)

                DynamicType.Draw -> dynamicItem.draw =
                    item.getOpusSummaryModule()?.let { opusSummaryModule ->
                        DynamicDrawModule.fromModuleOpusSummaryAndModuleDynamic(
                            opusSummaryModule,
                            item.getDynamicModule()
                        )
                    } ?: let {
                        DynamicDrawModule.fromModuleDescAndModuleDynamic(
                            item.getParagraphModule(),
                            item.getDescModule()!!,
                            item.getDynamicModule()
                        )
                    }


                DynamicType.Word -> dynamicItem.word =
                    item.getOpusSummaryModule()?.let { opusSummaryModule ->
                        DynamicWordModule.fromModuleOpusSummary(opusSummaryModule)
                    } ?: let {
                        DynamicWordModule.fromModuleDesc(item.getDescModule()!!)
                    }

                DynamicType.Forward -> dynamicItem.apply {
                    word = DynamicWordModule.fromModuleDesc(item.getDescModule()!!)
                    val item2 = item.getDynamicModule()?.dynForward?.item
                    if (item2 == null) {
                        println()
                        val emptyDynamic = bilibili.app.dynamic.v2.dynamicItem {
                            cardType = bilibili.app.dynamic.v2.DynamicType.dyn_none
                            modules.addAll(item.modulesList)
                        }
                        orig = fromDynamicItem(emptyDynamic, true)
                    } else {
                        orig = fromDynamicItem(item2, true)
                    }
                }

                DynamicType.LiveRcmd -> dynamicItem.liveRcmd =
                    DynamicLiveRcmdModule.fromModuleDynamic(item.getDynamicModule()!!)

                DynamicType.Pgc -> dynamicItem.pgc =
                    DynamicPgcModule.fromModulePgc(item.getDynamicModule()!!.dynPgc)

                DynamicType.Article -> dynamicItem.article =
                    DynamicArticleModule.fromModuleArticle(item.getDynamicModule()!!.dynArticle)

                DynamicType.Medialist,
                DynamicType.CoursesSeason,
                DynamicType.SubscriptionNew,
                DynamicType.CommonSquare,
                DynamicType.Live -> Unit

                DynamicType.None -> dynamicItem.none =
                    DynamicNoneModule.fromModuleDynamic(item.getItemNullModule()!!)
            }

            return dynamicItem
        }
    }

    data class DynamicAuthorModule(
        val author: String,
        val avatar: String,
        val mid: Long,
        val pubTime: String,
        val pubAction: String,
        val badgeText: String = ""
    ) {
        companion object {
            fun fromModuleAuthor(moduleAuthor: dev.aaa1115910.biliapi.http.entity.dynamic.DynamicItem.Modules.Author?) =
                DynamicAuthorModule(
                    author = moduleAuthor?.name.orEmpty(),
                    avatar = moduleAuthor?.face.orEmpty(),
                    mid = moduleAuthor?.mid ?: 0L,
                    pubTime = moduleAuthor?.pubTime.orEmpty(),
                    pubAction = moduleAuthor?.pubAction.orEmpty(),
                    badgeText = moduleAuthor?.iconBadge?.text.orEmpty()
                )

            fun fromModuleAuthor(moduleAuthor: bilibili.app.dynamic.v2.ModuleAuthor) =
                DynamicAuthorModule(
                    author = moduleAuthor.author.name,
                    avatar = moduleAuthor.author.face,
                    mid = moduleAuthor.author.mid,
                    pubTime = moduleAuthor.ptimeLabelText,
                    pubAction = ""
                )

            fun fromExtendAndModuleAuthorForward(
                extend: bilibili.app.dynamic.v2.Extend,
                moduleAuthorForward: bilibili.app.dynamic.v2.ModuleAuthorForward
            ) =
                DynamicAuthorModule(
                    author = extend.origName,
                    avatar = extend.origFace,
                    mid = extend.uid,
                    pubTime = moduleAuthorForward.ptimeLabelText,
                    pubAction = ""
                )
        }
    }

    data class DynamicVideoModule(
        val aid: Long,
        val bvid: String? = null,
        val cid: Long,
        val epid: Int? = null,
        val seasonId: Int? = null,
        val title: String,
        var text: String,
        val cover: String,
        val duration: String,
        val play: String,
        val danmaku: String,
        val isChargingArc: Boolean = false,
        val chargingArcBadge: String = ""
    ) {
        companion object {
            fun fromModuleArchive(moduleArchive: dev.aaa1115910.biliapi.http.entity.dynamic.DynamicItem.Modules.Dynamic.Major.Archive): DynamicVideoModule {
                val badgeText = moduleArchive.badge?.text.orEmpty()
                val isChargingArc = badgeText.contains("充电") || badgeText.contains("限时免费")
                return DynamicVideoModule(
                    aid = moduleArchive.aid,
                    bvid = moduleArchive.bvid,
                    cid = 0,
                    title = moduleArchive.title,
                    text = moduleArchive.desc,
                    cover = moduleArchive.cover,
                    duration = moduleArchive.durationText,
                    play = moduleArchive.stat?.play.orEmpty(),
                    danmaku = moduleArchive.stat?.danmaku.orEmpty(),
                    isChargingArc = isChargingArc,
                    chargingArcBadge = if (isChargingArc) badgeText else ""
                )
            }

            fun fromModuleArchive(moduleArchive: bilibili.app.dynamic.v2.MdlDynArchive): DynamicVideoModule {
                val badgeText = moduleArchive.badgeList.firstOrNull()?.text ?: ""
                val isChargingArc = badgeText.contains("充电") || badgeText.contains("限时免费")
                return DynamicVideoModule(
                    aid = moduleArchive.avid,
                    bvid = moduleArchive.bvid,
                    cid = moduleArchive.cid,
                    epid = moduleArchive.episodeId.toInt(),
                    seasonId = moduleArchive.pgcSeasonId.toInt(),
                    title = moduleArchive.title,
                    text = "",
                    cover = moduleArchive.cover,
                    duration = moduleArchive.coverLeftText1,
                    play = moduleArchive.coverLeftText2,
                    danmaku = moduleArchive.coverLeftText3,
                    isChargingArc = isChargingArc,
                    chargingArcBadge = if (isChargingArc) badgeText else ""
                )
            }

            fun fromModuleDynamic(
                moduleDynamic: bilibili.app.dynamic.v2.ModuleDynamic,
                text: String = ""
            ): DynamicVideoModule? {
                return when (moduleDynamic.moduleItemCase) {
                    ModuleItemCase.DYN_ARCHIVE -> fromModuleArchive(moduleDynamic.dynArchive)
                    ModuleItemCase.DYN_CHARGING_ARCHIVE -> fromModuleArchive(
                        moduleDynamic.dynChargingArchive.archiveInfo
                    ).copy(
                        isChargingArc = true,
                        chargingArcBadge = "充电专属"
                    )

                    else -> null
                }?.apply {
                    this.text = text
                }
            }
        }
    }

    data class DynamicFooterModule(
        val like: Int,
        val comment: Int,
        val share: Int,
        val isLiked: Boolean = false
    ) {
        companion object {
            fun fromModuleStat(moduleStat: dev.aaa1115910.biliapi.http.entity.dynamic.DynamicItem.Modules.Stat?) =
                moduleStat?.let {
                    DynamicFooterModule(
                        like = moduleStat.like?.count ?: 0,
                        comment = moduleStat.comment?.count ?: 0,
                        share = moduleStat.forward?.count ?: 0,
                        isLiked = moduleStat.like?.status ?: false
                    )
                }

            fun fromModuleStat(moduleStat: bilibili.app.dynamic.v2.ModuleStat) =
                DynamicFooterModule(
                    like = moduleStat.like.toInt(),
                    comment = moduleStat.reply.toInt(),
                    share = moduleStat.repost.toInt(),
                    isLiked = moduleStat.likeInfo.isLike
                )

            fun fromModuleBottom(moduleButtom: bilibili.app.dynamic.v2.ModuleButtom) =
                fromModuleStat(moduleButtom.moduleStat)
        }
    }

    data class DynamicVoteModule(
        val voteId: Long,
        val title: String,
        val desc: String,
        val joinNum: Int,
        val url: String? = null
    ) {
        companion object {
            fun fromModuleDynamic(
                moduleDynamic: dev.aaa1115910.biliapi.http.entity.dynamic.DynamicItem.Modules.Dynamic
            ): DynamicVoteModule? {
                moduleDynamic.additional?.vote?.let { vote ->
                    if (vote.voteId > 0) {
                        return DynamicVoteModule(
                            voteId = vote.voteId.toLong(),
                            title = vote.title,
                            desc = vote.desc,
                            joinNum = vote.joinNum
                        )
                    }
                }

                val voteNode = moduleDynamic.desc?.richTextNodes
                    ?.firstOrNull { it.type == "RICH_TEXT_NODE_TYPE_VOTE" || it.rid?.toLongOrNull() != null && it.jumpUrl?.contains("vote") == true }
                    ?: moduleDynamic.major?.opus?.summary?.richTextNodes
                        ?.firstOrNull { it.type == "RICH_TEXT_NODE_TYPE_VOTE" || it.rid?.toLongOrNull() != null && it.jumpUrl?.contains("vote") == true }
                val voteId = voteNode?.rid?.toLongOrNull() ?: return null
                return DynamicVoteModule(
                    voteId = voteId,
                    title = voteNode.origText.ifBlank { voteNode.text },
                    desc = "",
                    joinNum = 0,
                    url = voteNode.jumpUrl
                )
            }
        }
    }

    data class DynamicDrawModule(
        val title: String?,
        val text: String,
        val images: List<Picture>,
        val richTextNodes: List<RichTextNode> = emptyList()
    ) {
        companion object {
            fun fromModuleDynamic(moduleDynamic: dev.aaa1115910.biliapi.http.entity.dynamic.DynamicItem.Modules.Dynamic): DynamicDrawModule {
                val desc = moduleDynamic.desc
                val opus = moduleDynamic.major?.opus
                val richNodes = desc?.toRichTextNodes()
                    ?: opus?.summary?.toRichTextNodes()
                    ?: emptyList()
                return DynamicDrawModule(
                    title = null,
                    text = desc?.text
                        ?: opus?.summary?.text
                        ?: "",
                    images = (moduleDynamic.major?.draw?.items?.map(Picture::fromPicture)
                        ?: opus?.pics?.map(Picture::fromPicture))
                        ?.distinctBy { it.url }
                        ?: emptyList(),
                    richTextNodes = richNodes
                )
            }

            fun fromModuleOpusSummaryAndModuleDynamic(
                moduleOpusSummary: bilibili.app.dynamic.v2.ModuleOpusSummary,
                moduleDynamic: bilibili.app.dynamic.v2.ModuleDynamic?
            ): DynamicDrawModule {
                var title = ""
                var text = ""
                var richTextNodes = emptyList<RichTextNode>()
                val images = mutableListOf<Picture>()

                when (val titleContentType = moduleOpusSummary.title.contentCase) {
                    Paragraph.ContentCase.TEXT -> title = moduleOpusSummary.title.text.nodesList
                        .joinToString("") { it.rawText }

                    else -> println("not implemented: ModuleOpusSummary titleContentType: $titleContentType")
                }

                when (val summaryContentType = moduleOpusSummary.summary.contentCase) {
                    Paragraph.ContentCase.TEXT -> {
                        val nodes = moduleOpusSummary.summary.text.nodesList
                        text = nodes.joinToString("") { it.rawText }
                        richTextNodes = nodes.toRichTextNodes()
                    }

                    else -> println("not implemented: ModuleOpusSummary summaryContentType: $summaryContentType")
                }

                when (val dynamicItemType = moduleDynamic?.moduleItemCase) {
                    null -> println("ModuleDynamic is null")
                    ModuleItemCase.DYN_DRAW -> images.addAll(
                        moduleDynamic.dynDraw.itemsList.map(Picture::fromPicture)
                    )

                    else -> println("not implemented: ModuleOpusSummary dynamicItemType $dynamicItemType")
                }

                return DynamicDrawModule(
                    title = title,
                    text = text,
                    images = images.distinctBy { it.url },
                    richTextNodes = richTextNodes
                )
            }

            fun fromModuleDescAndModuleDynamic(
                moduleParagraph: bilibili.app.dynamic.v2.ModuleParagraph?,
                moduleDesc: bilibili.app.dynamic.v2.ModuleDesc,
                moduleDynamic: bilibili.app.dynamic.v2.ModuleDynamic?
            ): DynamicDrawModule {
                var title = ""
                var text = ""
                val images = mutableListOf<Picture>()

                text = moduleDesc.descList.joinToString("") { it.text }
                val richTextNodes = moduleDesc.descList.toRichTextNodesFromDesc()

                if (moduleParagraph != null && moduleParagraph.isArticleTitle) {

                    when (val titleContentType = moduleParagraph.paragraph.contentCase) {
                        Paragraph.ContentCase.TEXT -> title =
                            moduleParagraph.paragraph.text.nodesList
                                .joinToString("") { it.rawText }

                        else -> println("not implemented: ModuleOpusSummary titleContentType: $titleContentType")
                    }
                }

                when (val dynamicItemType = moduleDynamic?.moduleItemCase) {
                    null -> println("ModuleDynamic is null")
                    ModuleItemCase.DYN_DRAW -> images.addAll(
                        moduleDynamic.dynDraw.itemsList.map(Picture::fromPicture)
                    )

                    else -> println("not implemented: ModuleOpusSummary dynamicItemType $dynamicItemType")
                }

                return DynamicDrawModule(
                    title = title,
                    text = text,
                    images = images.distinctBy { it.url },
                    richTextNodes = richTextNodes
                )
            }
        }
    }

    data class DynamicWordModule(
        val text: String,
        val richTextNodes: List<RichTextNode> = emptyList()
    ) {
        companion object {
            fun fromModuleDynamic(moduleDynamic: dev.aaa1115910.biliapi.http.entity.dynamic.DynamicItem.Modules.Dynamic): DynamicWordModule {
                val desc = moduleDynamic.desc
                val opus = moduleDynamic.major?.opus
                val richNodes = opus?.summary?.toRichTextNodes()
                    ?: desc?.toRichTextNodes()
                    ?: emptyList()
                return DynamicWordModule(
                    text = opus?.summary?.text
                        ?: desc?.text
                        ?: "empty content",
                    richTextNodes = richNodes
                )
            }

            fun fromModuleOpusSummary(moduleOpusSummary: bilibili.app.dynamic.v2.ModuleOpusSummary): DynamicWordModule {
                val nodes = moduleOpusSummary.summary.text.nodesList
                return DynamicWordModule(
                    text = nodes.joinToString("") { it.rawText },
                    richTextNodes = nodes.toRichTextNodes()
                )
            }

            fun fromModuleDesc(moduleDesc: bilibili.app.dynamic.v2.ModuleDesc) =
                DynamicWordModule(
                    text = moduleDesc.text,
                    richTextNodes = moduleDesc.descList.toRichTextNodesFromDesc()
                )
        }
    }

    data class DynamicLiveRcmdModule(
        val title: String,
        val cover: String,
        val roomId: Int
    ) {
        companion object {
            private val json = Json {
                coerceInputValues = true
                ignoreUnknownKeys = true
                prettyPrint = true
            }

            fun fromModuleDynamic(moduleDynamic: dev.aaa1115910.biliapi.http.entity.dynamic.DynamicItem.Modules.Dynamic): DynamicLiveRcmdModule {
                val content = moduleDynamic.major?.liveRcmd?.content
                    ?: moduleDynamic.major?.subscriptionNew?.liveRcmd?.content
                    ?: ""
                val liveRcmdContent = runCatching {
                    json.decodeFromString<LiveRcmdContent>(content)
                }.getOrDefault(LiveRcmdContent())
                return DynamicLiveRcmdModule(
                    title = liveRcmdContent.livePlayInfo.title,
                    cover = liveRcmdContent.livePlayInfo.cover,
                    roomId = liveRcmdContent.livePlayInfo.roomId
                )
            }

            fun fromModuleDynamic(moduleDynamic: bilibili.app.dynamic.v2.ModuleDynamic): DynamicLiveRcmdModule {
                val liveRcmdContent = runCatching {
                    json.decodeFromString<LiveRcmdContent>(moduleDynamic.dynLiveRcmd.content)
                }.getOrDefault(LiveRcmdContent())
                return DynamicLiveRcmdModule(
                    title = liveRcmdContent.livePlayInfo.title,
                    cover = liveRcmdContent.livePlayInfo.cover,
                    roomId = liveRcmdContent.livePlayInfo.roomId
                )
            }
        }

        @Serializable
        private data class LiveRcmdContent(
            @SerialName("live_play_info")
            val livePlayInfo: LivePlayInfo = LivePlayInfo(),
            @SerialName("live_record_info")
            val liveRecordInfo: JsonElement? = null,
            val type: Int = 0
        ) {
            @Serializable
            data class LivePlayInfo(
                val title: String = "",
                @SerialName("parent_area_name")
                val parentAreaName: String = "",
                val cover: String = "",
                val online: Int = 0,
                @SerialName("parent_area_id")
                val parentAreaId: Int = 0,
                @SerialName("live_start_time")
                val liveStartTime: Long = 0L,
                @SerialName("room_id")
                val roomId: Int = 0,
                @SerialName("live_status")
                val liveStatus: Int = 0,
                @SerialName("room_type")
                val roomType: Int = 0,
                @SerialName("play_type")
                val playType: Int = 0,
                val link: String = "",
                @SerialName("area_id")
                val areaId: Int = 0,
                @SerialName("area_name")
                val areaName: String = "",
                @SerialName("watched_show")
                val watchedShow: WatchedShow = WatchedShow(),
                @SerialName("room_paid_type")
                val roomPaidType: Int = 0,
                val uid: Long = 0L,
                @SerialName("live_screen_type")
                val liveScreenType: Int = 0,
                @SerialName("live_id")
                val liveId: Long = 0L,
                val pendants: Pendants = Pendants()
            ) {
                @Serializable
                data class WatchedShow(
                    val num: Int = 0,
                    @SerialName("text_small")
                    val textSmall: String = "",
                    @SerialName("text_large")
                    val textLarge: String = "",
                    val icon: String = "",
                    @SerialName("icon_location")
                    val iconLocation: String = "",
                    @SerialName("icon_web")
                    val iconWeb: String = "",
                    val switch: Boolean = false
                )

                @Serializable
                data class Pendants(
                    val list: JsonElement? = null
                )
            }
        }
    }

    data class DynamicLiveModule(
        val title: String,
        val cover: String,
        val roomId: Long,
        val liveState: Int,
        val desc: String
    ) {
        companion object {
            fun fromModuleLive(moduleLive: dev.aaa1115910.biliapi.http.entity.dynamic.DynamicItem.Modules.Dynamic.Major.Live) =
                DynamicLiveModule(
                    title = moduleLive.title,
                    cover = moduleLive.cover,
                    roomId = moduleLive.id,
                    liveState = moduleLive.liveState,
                    desc = moduleLive.descFirst
                )
        }
    }

    data class DynamicMajorCardModule(
        val title: String,
        val cover: String,
        val url: String,
        val subtitle: String = "",
        val badge: String = ""
    ) {
        companion object {
            fun fromModuleMedialist(moduleMedialist: dev.aaa1115910.biliapi.http.entity.dynamic.DynamicItem.Modules.Dynamic.Major.Medialist) =
                DynamicMajorCardModule(
                    title = moduleMedialist.title,
                    cover = moduleMedialist.cover,
                    url = moduleMedialist.jumpUrl,
                    subtitle = moduleMedialist.subTitle,
                    badge = moduleMedialist.badge?.text.orEmpty()
                )

            fun fromModuleCourses(moduleCourses: dev.aaa1115910.biliapi.http.entity.dynamic.DynamicItem.Modules.Dynamic.Major.Courses) =
                DynamicMajorCardModule(
                    title = moduleCourses.title,
                    cover = moduleCourses.cover,
                    url = moduleCourses.jumpUrl,
                    subtitle = moduleCourses.desc,
                    badge = moduleCourses.badge?.text.orEmpty()
                )

            fun fromModuleCommon(moduleCommon: dev.aaa1115910.biliapi.http.entity.dynamic.DynamicItem.Modules.Dynamic.Major.Common) =
                DynamicMajorCardModule(
                    title = moduleCommon.title,
                    cover = moduleCommon.cover,
                    url = moduleCommon.jumpUrl,
                    subtitle = moduleCommon.desc,
                    badge = moduleCommon.badge?.text.orEmpty()
                )
        }
    }

    data class DynamicPgcModule(
        val title: String,
        val epid: Int,
        val seasonId: Int,
        val cover: String,
        val aid: Long,
        val cid: Long
    ) {
        companion object {
            fun fromModulePgc(modulePgc: dev.aaa1115910.biliapi.http.entity.dynamic.DynamicItem.Modules.Dynamic.Major.Pgc) =
                DynamicPgcModule(
                    title = modulePgc.title,
                    epid = modulePgc.epid,
                    seasonId = modulePgc.seasonId,
                    cover = modulePgc.cover,
                    aid = modulePgc.aid,
                    cid = modulePgc.cid
                )

            fun fromModulePgc(modulePgc: bilibili.app.dynamic.v2.MdlDynPGC): DynamicPgcModule {
                return DynamicPgcModule(
                    title = modulePgc.title,
                    epid = modulePgc.epid.toInt(),
                    seasonId = modulePgc.seasonId.toInt(),
                    cover = modulePgc.cover,
                    aid = modulePgc.aid,
                    cid = modulePgc.cid
                )
            }
        }
    }

    data class DynamicArticleModule(
        val title: String,
        val text: String,
        val url: String,
        val label: String,
        val id: Int,
        val covers: List<String>,
        val coverPictures: List<Picture> = emptyList()
    ) {
        companion object {
            fun fromModuleArticle(moduleDynamic: dev.aaa1115910.biliapi.http.entity.dynamic.DynamicItem.Modules.Dynamic.Major.Article) =
                DynamicArticleModule(
                    title = moduleDynamic.title,
                    text = moduleDynamic.desc,
                    url = moduleDynamic.jumpUrl,
                    label = moduleDynamic.label,
                    id = moduleDynamic.id,
                    covers = moduleDynamic.covers
                )

            fun fromModuleArticle(moduleArticle: bilibili.app.dynamic.v2.MdlDynArticle): DynamicArticleModule {
                return DynamicArticleModule(
                    title = moduleArticle.title,
                    text = moduleArticle.desc,
                    url = moduleArticle.uri,
                    label = moduleArticle.label,
                    covers = moduleArticle.coversList,
                    id = moduleArticle.id.toInt()
                )
            }

            fun fromModuleDynamic(moduleDynamic: dev.aaa1115910.biliapi.http.entity.dynamic.DynamicItem.Modules.Dynamic): DynamicArticleModule {
                val article = moduleDynamic.major?.article
                val opus = moduleDynamic.major?.opus

                return if (article != null) {
                    fromModuleArticle(article)
                } else if (opus != null) {
                    DynamicArticleModule(
                        title = opus.title ?: "",
                        text = opus.summary.text,
                        url = opus.jumpUrl,
                        label = "",
                        id = 0,
                        covers = opus.pics.map { pic -> pic.url.ifBlank { pic.src.ifBlank { pic.liveUrl } } },
                        coverPictures = opus.pics.map(Picture::fromPicture)
                    )
                } else {
                    DynamicArticleModule(
                        title = "",
                        text = moduleDynamic.desc?.text ?: "",
                        url = "",
                        label = "",
                        id = 0,
                        covers = emptyList()
                    )
                }
            }
        }
    }

    data class DynamicNoneModule(
        val text: String
    ) {
        companion object {
            fun fromModuleDynamic(moduleNone: dev.aaa1115910.biliapi.http.entity.dynamic.DynamicItem.Modules.Dynamic.Major.None) =
                DynamicNoneModule(
                    text = moduleNone.tips
                )

            fun fromModuleDynamic(moduleNone: bilibili.app.dynamic.v2.ModuleItemNull): DynamicNoneModule {
                return DynamicNoneModule(
                    text = moduleNone.text
                )
            }
        }
    }

    data class DynamicBlockedModule(
        val title: String,
        val hintMessage: String,
        val blockedType: Int,
        val bgImage: BlockedImage? = null,
        val icon: BlockedImage? = null,
        val button: BlockedButton? = null
    ) {
        data class BlockedImage(
            val imgDay: String,
            val imgDark: String
        ) {
            fun url(isDark: Boolean): String {
                return if (isDark) {
                    imgDark.ifBlank { imgDay }
                } else {
                    imgDay.ifBlank { imgDark }
                }
            }
        }

        data class BlockedButton(
            val icon: String,
            val jumpUrl: String,
            val text: String
        )

        companion object {
            fun fromModuleBlocked(blocked: dev.aaa1115910.biliapi.http.entity.dynamic.DynamicItem.Modules.Dynamic.Major.Blocked) =
                DynamicBlockedModule(
                    title = blocked.title,
                    hintMessage = blocked.hintMessage,
                    blockedType = blocked.blockedType,
                    bgImage = blocked.bgImg?.let { BlockedImage(imgDay = it.imgDay, imgDark = it.imgDark) },
                    icon = blocked.icon?.let { BlockedImage(imgDay = it.imgDay, imgDark = it.imgDark) },
                    button = blocked.button?.let {
                        BlockedButton(
                            icon = it.icon,
                            jumpUrl = it.jumpUrl.orEmpty(),
                            text = it.text
                        )
                    }
                )
        }
    }

    data class DynamicUgcSeasonModule(
        val aid: Long,
        val bvid: String,
        val cover: String,
        val desc: String,
        val duration: String,
        val url: String,
        val play: String,
        val danmaku: String,
        val title: String
    ) {
        companion object {
            fun fromModuleUgcSeason(moduleDynamic: dev.aaa1115910.biliapi.http.entity.dynamic.DynamicItem.Modules.Dynamic.Major.UgcSeason) =
                DynamicUgcSeasonModule(
                    aid = moduleDynamic.aid,
                    bvid = moduleDynamic.bvid,
                    cover = moduleDynamic.cover,
                    desc = moduleDynamic.desc,
                    duration = moduleDynamic.durationText,
                    url = moduleDynamic.jumpUrl,
                    play = moduleDynamic.stat?.play.orEmpty(),
                    danmaku = moduleDynamic.stat?.danmaku.orEmpty(),
                    title = moduleDynamic.title
                )

            fun fromModuleUgcSeason(moduleDynamic: bilibili.app.dynamic.v2.MdlDynUGCSeason) =
                DynamicUgcSeasonModule(
                    aid = moduleDynamic.avid,
                    bvid = "",
                    cover = moduleDynamic.cover,
                    desc = "",
                    duration = moduleDynamic.coverLeftText1,
                    url = moduleDynamic.jumpUrl,
                    play = moduleDynamic.coverLeftText2,
                    danmaku = moduleDynamic.coverLeftText3,
                    title = moduleDynamic.title
                )
        }
    }
}

data class DynamicVideoData(
    val videos: List<DynamicVideo>,
    val hasMore: Boolean,
    val historyOffset: String,
    val updateBaseline: String
) {
    companion object {
        private val logger = KotlinLogging.logger { }
        fun fromDynamicData(data: dev.aaa1115910.biliapi.http.entity.dynamic.DynamicData) =
            DynamicVideoData(
                videos = data.items.mapNotNull { DynamicVideo.fromDynamicVideoItem(it) },
                hasMore = data.hasMore,
                historyOffset = data.offset,
                updateBaseline = data.updateBaseline
            ).also {
                logger.info { "updateBaseline: ${data.updateBaseline}" }
                logger.info { "offset: ${data.offset}" }
            }

        fun fromDynamicData(data: bilibili.app.dynamic.v2.DynVideoReply) = DynamicVideoData(
            videos = data.dynamicList.listList.mapNotNull { DynamicVideo.fromDynamicVideoItem(it) },
            hasMore = data.dynamicList.hasMore,
            historyOffset = data.dynamicList.historyOffset,
            updateBaseline = data.dynamicList.updateBaseline
        ).also {
            logger.info { "updateBaseline: ${data.dynamicList.updateBaseline}" }
            logger.info { "historyOffset: ${data.dynamicList.historyOffset}" }
        }
    }
}

/**
 * 动态视频
 *
 * @property aid 视频av号
 * @property bvid 视频bv号，grpc pgc 没有bv号
 * @property cid 视频cid，仅 grpc 接口
 * @property epid 番剧epid，仅 grpc 接口
 * @property seasonId 番剧seasonId，仅 grpc 接口
 * @property title 视频标题
 * @property cover 视频封面
 * @property author 视频作者
 * @property duration 视频时长，单位秒
 * @property play 视频播放量
 * @property danmaku 视频弹幕数
 * @property avatar 视频作者头像
 * @property pubTime 发布时间
 */
data class DynamicVideo(
    val aid: Long,
    val bvid: String? = null,
    val cid: Long,
    val epid: Int? = null,
    val seasonId: Int? = null,
    val title: String,
    val cover: String,
    val author: String,
    var authorId: Long = 0,
    var authorFace: String = "",
    val duration: Int,
    val play: Long,
    val danmaku: Int,
    val avatar: String,
    val time: Long = 0L,
    val pubTime: String? = null,
    val isChargingArc: Boolean = false,
    val chargingArcBadge: String = ""
) {
    companion object {
        fun fromDynamicVideoItem(item: dev.aaa1115910.biliapi.http.entity.dynamic.DynamicItem): DynamicVideo? {
            val major = item.modules.moduleDynamic.major ?: return null
            val author = item.modules.moduleAuthor
            val archive = major.archive
            if (archive != null) {
                val badgeText = archive.badge?.text.orEmpty()
                val isChargingArc = badgeText.contains("充电") || badgeText.contains("限时免费")
                return DynamicVideo(
                    aid = archive.aid,
                    bvid = archive.bvid,
                    cid = 0,
                    title = archive.title
                        .replace("动态视频｜", ""),
                    cover = archive.cover,
                    author = author?.name.orEmpty(),
                    authorId = author?.mid ?: 0L,
                    authorFace = author?.face.orEmpty(),
                    duration = convertStringTimeToSeconds(archive.durationText),
                    play = convertStringPlayCountToNumberPlayCount(archive.stat?.play.orEmpty()),
                    danmaku = convertStringPlayCountToNumberPlayCount(archive.stat?.danmaku.orEmpty()).toInt(),
                    avatar = author?.face.orEmpty(),
                    pubTime = author?.pubTime,
                    isChargingArc = isChargingArc,
                    chargingArcBadge = if (isChargingArc) badgeText else ""
                )
            }

            val ugcSeason = major.ugcSeason
            if (ugcSeason != null) {
                return DynamicVideo(
                    aid = ugcSeason.aid,
                    bvid = ugcSeason.bvid,
                    cid = 0,
                    title = ugcSeason.title,
                    cover = ugcSeason.cover,
                    author = author?.name.orEmpty(),
                    authorId = author?.mid ?: 0L,
                    authorFace = author?.face.orEmpty(),
                    duration = convertStringTimeToSeconds(ugcSeason.durationText),
                    play = convertStringPlayCountToNumberPlayCount(ugcSeason.stat?.play.orEmpty()),
                    danmaku = convertStringPlayCountToNumberPlayCount(ugcSeason.stat?.danmaku.orEmpty()).toInt(),
                    avatar = author?.face.orEmpty(),
                    pubTime = author?.pubTime
                )
            }

            val pgc = major.pgc ?: return null
            return DynamicVideo(
                aid = pgc.aid,
                bvid = pgc.bvid.ifBlank { null },
                cid = pgc.cid,
                epid = pgc.epid,
                seasonId = pgc.seasonId,
                title = pgc.title,
                cover = pgc.cover,
                author = author?.name.orEmpty(),
                authorId = author?.mid ?: 0L,
                authorFace = author?.face.orEmpty(),
                duration = convertStringTimeToSeconds(pgc.durationText),
                play = convertStringPlayCountToNumberPlayCount(pgc.stat?.play.orEmpty()),
                danmaku = convertStringPlayCountToNumberPlayCount(pgc.stat?.danmaku.orEmpty()).toInt(),
                avatar = author?.face.orEmpty(),
                pubTime = author?.pubTime
            )
        }

        fun fromDynamicVideoItem(item: bilibili.app.dynamic.v2.DynamicItem): DynamicVideo? {
            val author =
                item.modulesList.first { it.moduleType == DynModuleType.module_author }.moduleAuthor.author
            val dynamic =
                item.modulesList.first { it.moduleType == DynModuleType.module_dynamic }.moduleDynamic
            val desc =
                item.modulesList.firstOrNull { it.moduleType == DynModuleType.module_desc }?.moduleDesc
            val isDynamicVideo = dynamic.dynArchive?.stype == VideoType.video_type_dynamic
            when (dynamic.moduleItemCase) {
                ModuleItemCase.DYN_ARCHIVE -> {
                    val archive = dynamic.dynArchive
                    val badgeText = archive.badgeList.firstOrNull()?.text ?: ""
                    val isChargingArc = badgeText.contains("充电") || badgeText.contains("限时免费")
                    return DynamicVideo(
                        aid = archive.avid,
                        bvid = archive.bvid,
                        cid = archive.cid,
                        title = if (!isDynamicVideo) archive.title else {
                            desc?.text?.replace("动态视频｜", "") ?: "NO TITLE"
                        },
                        cover = archive.cover,
                        author = author.name,
                        authorId = author.mid,
                        authorFace = author.face,
                        duration = convertStringTimeToSeconds(archive.coverLeftText1),
                        play = convertStringPlayCountToNumberPlayCount(archive.coverLeftText2),
                        danmaku = convertStringPlayCountToNumberPlayCount(archive.coverLeftText3).toInt(),
                        avatar = author.face,
                        isChargingArc = isChargingArc,
                        chargingArcBadge = if (isChargingArc) badgeText else ""
                    )
                }

                ModuleItemCase.DYN_PGC -> {
                    val pgc = dynamic.dynPgc
                    return DynamicVideo(
                        aid = pgc.aid,
                        bvid = null,
                        cid = pgc.cid,
                        epid = pgc.cid.toInt(),
                        seasonId = pgc.seasonId.toInt(),
                        title = pgc.title,
                        cover = pgc.cover,
                        author = author.name,
                        duration = convertStringTimeToSeconds(pgc.coverLeftText1),
                        play = convertStringPlayCountToNumberPlayCount(pgc.coverLeftText2),
                        danmaku = convertStringPlayCountToNumberPlayCount(pgc.coverLeftText3).toInt(),
                        avatar = author.face
                    )
                }

                ModuleItemCase.DYN_CHARGING_ARCHIVE -> {
                    val chargingArchiveInfo = dynamic.dynChargingArchive.archiveInfo
                    return DynamicVideo(
                        aid = chargingArchiveInfo.avid,
                        bvid = chargingArchiveInfo.bvid,
                        cid = chargingArchiveInfo.cid,
                        title = chargingArchiveInfo.title,
                        cover = chargingArchiveInfo.cover,
                        author = author.name,
                        duration = convertStringTimeToSeconds(chargingArchiveInfo.coverLeftText1),
                        play = convertStringPlayCountToNumberPlayCount(chargingArchiveInfo.coverLeftText2),
                        danmaku = convertStringPlayCountToNumberPlayCount(chargingArchiveInfo.coverLeftText3).toInt(),
                        avatar = author.face,
                        isChargingArc = true,
                        chargingArcBadge = "充电专属"
                    )
                }

                else -> {
                    println("unsupported dynamic moduleItemCase: ${dynamic.moduleItemCase}")
                    return null
                }
            }
        }
    }
}

private fun convertStringTimeToSeconds(time: String): Int {
    //部分稿件可能没有时长，Web 接口返回 NaN:NaN:NaN，App 接口返回空字符串
    if (time.startsWith("NaN") || time.isBlank()) return 0

    val parts = time.split(":")
    if (parts.size < 2) return time.toIntOrNull() ?: 0
    val hours = if (parts.size == 3) parts[0].toIntOrNull() ?: 0 else 0
    val minutes = parts[parts.size - 2].toIntOrNull() ?: 0
    val seconds = parts[parts.size - 1].toIntOrNull() ?: 0
    return (hours * 3600) + (minutes * 60) + seconds
}

//web 接口获取到的是“xx万”，而 grpc 接口获取到的是“xx.x万播放”
private fun convertStringPlayCountToNumberPlayCount(play: String): Long {
    if (play.isBlank()) return 0
    if (play.startsWith("-")) return 0
    runCatching {
        val number = play
            .replace("弹幕", "")
            .replace("观看", "")
            .replace("播放", "")
            .substringBefore("万").toFloat()
        return (if (play.contains("万")) number * 10000 else number).toLong()
    }.onFailure {
        println("convert play count [$play] failed: ${it.stackTraceToString()}")
    }
    return -1
}

enum class DynamicType(
    val webValue: String,
    val appValue: bilibili.app.dynamic.v2.DynamicType? = null
) {
    Av("DYNAMIC_TYPE_AV", bilibili.app.dynamic.v2.DynamicType.av),

    // bilibili hd 端的接口并不会返回合集更新动态
    UgcSeason("DYNAMIC_TYPE_UGC_SEASON", bilibili.app.dynamic.v2.DynamicType.ugc_season),
    Forward("DYNAMIC_TYPE_FORWARD", bilibili.app.dynamic.v2.DynamicType.forward),
    Word("DYNAMIC_TYPE_WORD", bilibili.app.dynamic.v2.DynamicType.word),
    Draw("DYNAMIC_TYPE_DRAW", bilibili.app.dynamic.v2.DynamicType.draw),
    Live("DYNAMIC_TYPE_LIVE"),
    LiveRcmd("DYNAMIC_TYPE_LIVE_RCMD", bilibili.app.dynamic.v2.DynamicType.live_rcmd),
    Pgc("DYNAMIC_TYPE_PGC_UNION", bilibili.app.dynamic.v2.DynamicType.pgc),
    Article("DYNAMIC_TYPE_ARTICLE", bilibili.app.dynamic.v2.DynamicType.article),
    Medialist("DYNAMIC_TYPE_MEDIALIST"),
    CoursesSeason("DYNAMIC_TYPE_COURSES_SEASON"),
    SubscriptionNew("DYNAMIC_TYPE_SUBSCRIPTION_NEW"),
    CommonSquare("DYNAMIC_TYPE_COMMON_SQUARE"),
    None("DYNAMIC_TYPE_NONE", bilibili.app.dynamic.v2.DynamicType.dyn_none);

    companion object {
        fun fromWebValueOrNull(webValue: String?) = when (webValue) {
            "DYNAMIC_TYPE_PGC" -> Pgc
            else -> entries.firstOrNull { it.webValue == webValue }
        }

        fun fromWebValue(webValue: String) = fromWebValueOrNull(webValue)
            ?: throw IllegalArgumentException("unknown type $webValue")

        fun fromAppValue(appValue: bilibili.app.dynamic.v2.DynamicType) =
            entries.firstOrNull { it.appValue == appValue }
                ?: throw IllegalArgumentException("unknown type ${appValue.name}")
    }
}

private fun Module.isAuthorModule() = moduleType == DynModuleType.module_author
private fun Module.isAuthorModuleForward() = moduleType == DynModuleType.module_author_forward
private fun Module.isDescModule() = moduleType == DynModuleType.module_desc
private fun Module.isDynamicModule() = moduleType == DynModuleType.module_dynamic
private fun Module.isModuleOpusSummary() = moduleType == DynModuleType.module_opus_summary
private fun Module.isStatModule() = moduleType == DynModuleType.module_stat
private fun Module.isBottomModel() = moduleType == DynModuleType.module_bottom
private fun Module.isItemNullModel() = moduleType == DynModuleType.module_item_null
private fun Module.isParagraphModel() = moduleType == DynModuleType.module_paragraph

private fun bilibili.app.dynamic.v2.DynamicItem.getAuthorModule() =
    modulesList.firstOrNull { it.isAuthorModule() }?.moduleAuthor

private fun bilibili.app.dynamic.v2.DynamicItem.getAuthorModuleForward() =
    modulesList.firstOrNull { it.isAuthorModuleForward() }?.moduleAuthorForward

private fun bilibili.app.dynamic.v2.DynamicItem.getDescModule() =
    modulesList.firstOrNull { it.isDescModule() }?.moduleDesc

private fun bilibili.app.dynamic.v2.DynamicItem.getDynamicModule() =
    modulesList.firstOrNull { it.isDynamicModule() }?.moduleDynamic

private fun bilibili.app.dynamic.v2.DynamicItem.getOpusSummaryModule() =
    modulesList.firstOrNull { it.isModuleOpusSummary() }?.moduleOpusSummary

private fun bilibili.app.dynamic.v2.DynamicItem.getStatModule() =
    modulesList.firstOrNull { it.isStatModule() }?.moduleStat

private fun bilibili.app.dynamic.v2.DynamicItem.getBottomModule() =
    modulesList.firstOrNull { it.isBottomModel() }?.moduleButtom

private fun bilibili.app.dynamic.v2.DynamicItem.getItemNullModule() =
    modulesList.firstOrNull { it.isItemNullModel() }?.moduleItemNull

private fun bilibili.app.dynamic.v2.DynamicItem.getParagraphModule() =
    modulesList.firstOrNull { it.isParagraphModel() }?.moduleParagraph

// ============== 专栏段落数据模型 ==============

/**
 * 段落类型枚举
 */
enum class ParagraphType {
    Text, Pictures, Line, Unknown
}

/**
 * 文本节点类型枚举
 */
enum class TextNodeType {
    Plain, Emoji, Link
}

/**
 * 段落数据模型 - sealed class
 */
sealed class ArticleParagraph {
    abstract val type: ParagraphType

    data class TextParagraph(
        override val type: ParagraphType = ParagraphType.Text,
        val nodes: List<ArticleTextNode>
    ) : ArticleParagraph()

    data class PicturesParagraph(
        override val type: ParagraphType = ParagraphType.Pictures,
        val pictures: List<ArticlePicture>
    ) : ArticleParagraph()

    data class LineParagraph(
        override val type: ParagraphType = ParagraphType.Line,
        val picture: ArticlePicture? = null
    ) : ArticleParagraph()
}

/**
 * 文本节点
 */
data class ArticleTextNode(
    val text: String,
    val type: TextNodeType = TextNodeType.Plain,
    val emojiUrl: String? = null,
    val linkUrl: String? = null,
    val isBold: Boolean = false,
    val isItalic: Boolean = false
)

/**
 * 图片数据
 */
data class ArticlePicture(
    val url: String,
    val width: Int,
    val height: Int
)
