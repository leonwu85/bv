package dev.aaa1115910.biliapi.repositories

import bilibili.app.dynamic.v2.OpusGrpcKt
import bilibili.app.dynamic.v2.opusSpaceFlowReq
import bilibili.pagination.pagination
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.BiliLiveHttpApi
import kotlinx.serialization.json.*
import org.koin.core.annotation.Single

data class OpusCard(val id: Long, val title: String, val cover: String = "", val subtitle: String = "", val url: String = "")
data class OpusSpacePage(val items: List<OpusCard>, val collections: List<OpusCard>, val next: String)

@Single
class OpusRepository(private val channels: ChannelRepository, private val auth: AuthRepository) {
    suspend fun space(mid: Long, next: String = ""): OpusSpacePage {
        val result = OpusGrpcKt.OpusCoroutineStub(checkNotNull(channels.defaultChannel)).opusSpaceFlow(opusSpaceFlowReq {
            hostMid = mid
            filterType = "all"
            localTime = 8
            pagination = pagination { pageSize = 20; this.next = next }
        })
        return OpusSpacePage(
            result.itemListList.filter { it.oid > 0 && it.hasFlowItemOpus() }.map { item ->
                val opus = item.flowItemOpus
                OpusCard(item.oid, opus.textParagraph.text.nodesList.joinToString("") { it.rawText.ifBlank { it.word.words } }.ifBlank { "图文 ${item.oid}" },
                    opus.coverPic.src, opus.bottomLeftText1.text, "https://www.bilibili.com/opus/${item.oid}")
            },
            result.hostUpOpusCollection.allCollectionsList.map { collection ->
                val info = collection.collectionInfo
                OpusCard(info.collectionId, info.title.textsList.joinToString("") { it.text }, collection.coverPic, collection.subTitleText)
            }, result.nextPage.next
        )
    }

    suspend fun collection(id: Long): List<OpusCard> {
        val data = BiliHttpApi.getArticleCollection(id, auth.sessionData).getResponseData()
        return data["articles"]?.jsonArray.orEmpty().mapNotNull { item ->
            val article = item.jsonObject
            val cv = article["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            val dynamic = article["dyn_id_str"]?.jsonPrimitive?.contentOrNull?.takeIf { it.toLongOrNull()?.let { id -> id > 0 } == true }
            OpusCard(cv, article["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                article["image_urls"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.contentOrNull.orEmpty(),
                article["summary"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                if (dynamic != null) "https://www.bilibili.com/opus/$dynamic" else "https://www.bilibili.com/read/cv$cv")
        }
    }

    suspend fun medal(mid: Long): String? = BiliLiveHttpApi.getMasterInfo(mid).getResponseData()["medal_name"]
        ?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
}
