package dev.aaa1115910.biliapi.entity.video

import dev.aaa1115910.biliapi.http.entity.video.InteractiveEdgeInfo

data class InteractiveNode(
    val nodeId: Long,
    val edgeId: Long? = null,
    val cid: Long,
    val title: String,
    val cover: String? = null,
    val startPos: Int? = null,
    val cursor: Int? = null,
    val isCurrent: Boolean = false,
) {
    companion object {
        fun fromStoryNode(node: InteractiveEdgeInfo.StoryNode) = InteractiveNode(
            nodeId = node.nodeId,
            edgeId = node.edgeId,
            cid = node.cid,
            title = node.title,
            cover = node.cover,
            startPos = node.startPos,
            cursor = node.cursor,
            isCurrent = node.isCurrent == 1,
        )
    }
}