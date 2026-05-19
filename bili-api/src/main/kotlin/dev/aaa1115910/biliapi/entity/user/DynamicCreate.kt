package dev.aaa1115910.biliapi.entity.user

enum class DynamicReplyOption {
    Allow,
    Close,
    Choose
}

data class DynamicRichContent(
    val rawText: String,
    val type: Int,
    val bizId: String = ""
)

data class DynamicImageDraft(
    val imgSrc: String,
    val imgWidth: Int = 0,
    val imgHeight: Int = 0,
    val imgSize: Double = 0.0
)

data class DynamicTopicDraft(
    val id: Long,
    val name: String
)

data class DynamicMentionDraft(
    val uid: String,
    val name: String,
    val face: String = "",
    val fans: Int = 0
)

data class DynamicEmoteDraft(
    val text: String,
    val url: String = "",
    val size: Int = 1,
    val alias: String = ""
)

data class DynamicEmotePackageDraft(
    val url: String = "",
    val type: Int = 0,
    val emotes: List<DynamicEmoteDraft> = emptyList()
)

data class DynamicReserveDraft(
    val id: Long,
    val title: String,
    val livePlanStartTime: Long,
    val subType: Int = 0
)

data class DynamicVoteDraft(
    val title: String,
    val desc: String = "",
    val options: List<String>,
    val choiceCnt: Int = 1,
    val durationSeconds: Long = 24 * 60 * 60
)

data class DynamicPublishDraft(
    val text: String = "",
    val richContents: List<DynamicRichContent> = emptyList(),
    val title: String = "",
    val pictures: List<DynamicImageDraft> = emptyList(),
    val publishTime: Long? = null,
    val replyOption: DynamicReplyOption = DynamicReplyOption.Allow,
    val privatePub: Boolean = false,
    val topic: DynamicTopicDraft? = null,
    val voteDraft: DynamicVoteDraft? = null,
    val reserve: DynamicReserveDraft? = null
)
