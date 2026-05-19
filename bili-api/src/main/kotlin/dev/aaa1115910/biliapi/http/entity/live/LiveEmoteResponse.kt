package dev.aaa1115910.biliapi.http.entity.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LiveEmoteData(
    val data: List<LiveEmotePackage> = emptyList()
)

@Serializable
data class LiveEmotePackage(
    val emoticons: List<LiveEmoticon> = emptyList(),
    @SerialName("pkg_type")
    val pkgType: Int = 0,
    @SerialName("current_cover")
    val currentCover: String = ""
)

@Serializable
data class LiveEmoticon(
    val emoji: String = "",
    val url: String = "",
    val width: Int? = null,
    val height: Int? = null,
    @SerialName("emoticon_unique")
    val emoticonUnique: String = ""
) {
    val displayName: String
        get() = emoji
            .removePrefix("[")
            .removeSuffix("]")
            .ifBlank { emoticonUnique }

    val messageText: String
        get() = emoji.ifBlank { emoticonUnique }
}
