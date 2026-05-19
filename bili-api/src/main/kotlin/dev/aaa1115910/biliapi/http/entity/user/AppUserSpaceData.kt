package dev.aaa1115910.biliapi.http.entity.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

@Serializable
data class AppUserSpaceData(
    val images: AppUserSpaceImages? = null,
    val card: AppUserSpaceCard? = null,
    val elec: AppUserSpaceElec? = null,
    val relation: Int? = null,
    @SerialName("rel_special")
    val relSpecial: Int? = null
) {
    val resolvedRelation: Int
        get() = when {
            relation == -1 -> 128
            card?.relation?.isFollow == 1 -> {
                if (relSpecial == 1) -10 else card.relation.status ?: 2
            }

            else -> 0
        }
}

@Serializable
data class AppUserSpaceCard(
    val mid: String? = null,
    val name: String? = null,
    val face: String? = null,
    val sign: String? = null,
    val fans: Int? = null,
    val friend: Int? = null,
    val attention: Int? = null,
    @SerialName("space_tag")
    val spaceTags: List<AppUserSpaceTag> = emptyList(),
    val relation: AppUserSpaceRelation? = null
)

@Serializable
data class AppUserSpaceRelation(
    val status: Int? = null,
    @SerialName("is_follow")
    val isFollow: Int? = null,
    @SerialName("is_followed")
    val isFollowed: Int? = null
)

@Serializable
data class AppUserSpaceTag(
    val type: String? = null,
    val title: String? = null,
    val uri: String? = null,
    val icon: String? = null
)

@Serializable
data class AppUserSpaceElec(
    val total: Int? = null,
    val list: List<AppUserSpaceElecUser> = emptyList()
)

@Serializable
data class AppUserSpaceElecUser(
    val uname: String? = null,
    val avatar: String? = null
)

@Serializable
data class AppUserSpaceImages(
    @SerialName("imgUrl")
    val imgUrl: String? = null,
    @SerialName("night_imgurl")
    val nightImgUrl: String? = null,
    @SerialName("collection_top_simple")
    val collectionTopSimple: AppUserSpaceCollectionTopSimple? = null
)

@Serializable
data class AppUserSpaceCollectionTopSimple(
    val top: AppUserSpaceTop? = null
)

@Serializable
data class AppUserSpaceTop(
    val result: List<AppUserSpaceTopImage> = emptyList()
)

@Serializable
data class AppUserSpaceTopImage(
    val item: JsonElement? = null,
    val cover: String? = null
) {
    val header: String
        get() = imageObject?.string("default_image")?.takeIf(String::isNotBlank)
            ?: cover.orEmpty()

    val alignmentY: Float
        get() {
            val image = imageObject ?: animationObject ?: return 0f
            val location = image.string("location").orEmpty()
            if (location.isBlank()) return 0f
            return runCatching {
                val anchors = location.split("-")
                    .drop(1)
                    .take(2)
                    .map(String::toDouble)
                val height = image.double("height") ?: return@runCatching 0f
                if (anchors.size == 2 && height > 0.0) {
                    ((anchors[0] + anchors[1]) / height - 1.0).toFloat()
                } else {
                    0f
                }
            }.getOrDefault(0f).coerceIn(-1f, 1f)
        }

    private val imageObject: JsonObject?
        get() = item.jsonObjectOrNull?.get("image").jsonObjectOrNull

    private val animationObject: JsonObject?
        get() = item.jsonObjectOrNull?.get("animation").jsonObjectOrNull
}

private val JsonElement?.jsonObjectOrNull: JsonObject?
    get() = this as? JsonObject

private fun JsonObject.string(key: String): String? {
    return (get(key) as? JsonPrimitive)?.contentOrNull
}

private fun JsonObject.double(key: String): Double? {
    return (get(key) as? JsonPrimitive)?.doubleOrNull
}
