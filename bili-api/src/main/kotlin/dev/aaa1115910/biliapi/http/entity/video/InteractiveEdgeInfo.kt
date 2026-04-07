package dev.aaa1115910.biliapi.http.entity.video

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InteractiveEdgeInfo(
    val title: String? = null,
    @SerialName("edge_id")
    val edgeId: Long? = null,
    @SerialName("story_list")
    val storyList: List<StoryNode> = emptyList(),
    @SerialName("is_leaf")
    val isLeaf: Int? = null,
) {
    @Serializable
    data class StoryNode(
        @SerialName("node_id")
        val nodeId: Long,
        @SerialName("edge_id")
        val edgeId: Long? = null,
        val title: String = "",
        val cid: Long,
        @SerialName("start_pos")
        val startPos: Int? = null,
        val cover: String? = null,
        @SerialName("is_current")
        val isCurrent: Int? = null,
        val cursor: Int? = null,
    )
}