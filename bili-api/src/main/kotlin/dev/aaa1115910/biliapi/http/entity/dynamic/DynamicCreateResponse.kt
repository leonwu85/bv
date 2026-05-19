package dev.aaa1115910.biliapi.http.entity.dynamic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateDynamicData(
    @SerialName("dyn_id")
    @Serializable(with = FlexibleLongSerializer::class)
    val dynId: Long = 0L,
    @SerialName("dyn_id_str")
    @Serializable(with = FlexibleStringSerializer::class)
    val dynIdStr: String = ""
)

@Serializable
data class CreateVoteData(
    @SerialName("vote_id")
    @Serializable(with = FlexibleLongSerializer::class)
    val voteId: Long = 0L
)

@Serializable
data class DynamicVoteInfoData(
    @SerialName("vote_info")
    val voteInfo: DynamicVoteInfo = DynamicVoteInfo(),
    @SerialName("my_votes")
    val myVotes: List<Int> = emptyList()
)

@Serializable
data class DynamicVoteResultData(
    @SerialName("vote_info")
    val voteInfo: DynamicVoteInfo = DynamicVoteInfo()
)

@Serializable
data class DynamicVoteInfo(
    @SerialName("vote_id")
    @Serializable(with = FlexibleLongSerializer::class)
    val voteId: Long = 0L,
    val title: String = "",
    val desc: String = "",
    @SerialName("choice_cnt")
    @Serializable(with = FlexibleIntSerializer::class)
    val choiceCnt: Int = 1,
    @SerialName("end_time")
    @Serializable(with = FlexibleLongSerializer::class)
    val endTime: Long = 0L,
    @SerialName("join_num")
    @Serializable(with = FlexibleIntSerializer::class)
    val joinNum: Int = 0,
    @SerialName("my_votes")
    val myVotes: List<Int> = emptyList(),
    val options: List<DynamicVoteOption> = emptyList(),
    @Serializable(with = FlexibleIntSerializer::class)
    val status: Int = 0,
    @Serializable(with = FlexibleIntSerializer::class)
    val type: Int = 0
)

@Serializable
data class DynamicVoteOption(
    @SerialName("opt_idx")
    @Serializable(with = FlexibleIntSerializer::class)
    val optIdx: Int = 0,
    @SerialName("opt_desc")
    val optDesc: String = "",
    @Serializable(with = FlexibleIntSerializer::class)
    val cnt: Int = 0,
    @SerialName("img_url")
    val imgUrl: String = ""
)

@Serializable
data class DynamicImageUploadData(
    @SerialName("image_url")
    val imageUrl: String = "",
    @SerialName("image_width")
    @Serializable(with = FlexibleIntSerializer::class)
    val imageWidth: Int = 0,
    @SerialName("image_height")
    @Serializable(with = FlexibleIntSerializer::class)
    val imageHeight: Int = 0,
    @SerialName("img_size")
    val imgSize: Double = 0.0
)

@Serializable
data class DynamicTopicListData(
    @SerialName("topic_items")
    val topicItems: List<DynamicTopicItemData> = emptyList()
)

@Serializable
data class DynamicTopicItemData(
    @Serializable(with = FlexibleLongSerializer::class)
    val id: Long = 0L,
    val name: String = "",
    @Serializable(with = FlexibleIntSerializer::class)
    val view: Int = 0,
    @Serializable(with = FlexibleIntSerializer::class)
    val discuss: Int = 0
)

@Serializable
data class DynamicMentionData(
    val groups: List<DynamicMentionGroupData> = emptyList()
)

@Serializable
data class DynamicMentionGroupData(
    @SerialName("group_name")
    val groupName: String = "",
    val items: List<DynamicMentionItemData> = emptyList()
)

@Serializable
data class DynamicMentionItemData(
    val face: String = "",
    @Serializable(with = FlexibleIntSerializer::class)
    val fans: Int = 0,
    val name: String = "",
    @Serializable(with = FlexibleStringSerializer::class)
    val uid: String = ""
)

@Serializable
data class DynamicEmotePanelData(
    val packages: List<DynamicEmotePackageData> = emptyList()
)

@Serializable
data class DynamicEmotePackageData(
    val url: String = "",
    @Serializable(with = FlexibleIntSerializer::class)
    val type: Int = 0,
    val emote: List<DynamicEmoteItemData> = emptyList()
)

@Serializable
data class DynamicEmoteItemData(
    val text: String = "",
    val url: String = "",
    val meta: DynamicEmoteMetaData = DynamicEmoteMetaData()
)

@Serializable
data class DynamicEmoteMetaData(
    @Serializable(with = FlexibleIntSerializer::class)
    val size: Int = 1,
    val alias: String = ""
)

@Serializable
data class CreateReserveData(
    @Serializable(with = FlexibleLongSerializer::class)
    val sid: Long = 0L
)

@Serializable
data class DynamicReserveInfoData(
    @Serializable(with = FlexibleLongSerializer::class)
    val id: Long = 0L,
    val title: String = "",
    @SerialName("live_plan_start_time")
    @Serializable(with = FlexibleLongSerializer::class)
    val livePlanStartTime: Long = 0L
)
