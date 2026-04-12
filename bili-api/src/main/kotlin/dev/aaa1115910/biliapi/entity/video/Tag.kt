package dev.aaa1115910.biliapi.entity.video

data class Tag(
    val id: Int,
    val name: String,
    val tagType: String = "old_channel"
) {
    companion object {
        fun fromTag(tag: dev.aaa1115910.biliapi.http.entity.video.Tag) = Tag(
            id = tag.tagId,
            name = tag.tagName
        )

        fun fromTag(tag: bilibili.app.view.v1.Tag) = Tag(
            id = tag.id.toInt(),
            name = tag.name,
            tagType = tag.tagType
        )

        fun fromTag(tag: dev.aaa1115910.biliapi.http.entity.video.VideoDetail.Tag) = Tag(
            id = tag.tagId,
            name = tag.tagName,
            tagType = tag.tagType
        )
    }
}
