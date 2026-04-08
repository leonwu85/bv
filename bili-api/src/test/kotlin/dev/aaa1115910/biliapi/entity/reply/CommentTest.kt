package dev.aaa1115910.biliapi.entity.reply

import bilibili.main.community.reply.v1.Content
import bilibili.main.community.reply.v1.Member
import bilibili.main.community.reply.v1.Picture
import bilibili.main.community.reply.v1.ReplyControl
import bilibili.main.community.reply.v1.ReplyInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class CommentTest {
    @Test
    fun `fromReplyInfo uses count when sub reply text is missing`() {
        val replyInfo = ReplyInfo.newBuilder()
            .setId(1L)
            .setOid(2L)
            .setType(1L)
            .setMid(3L)
            .setParent(0L)
            .setCount(13L)
            .setLike(5L)
            .setContent(
                Content.newBuilder()
                    .setMessage("测试评论")
                    .addPictures(
                        Picture.newBuilder()
                            .setImgSrc("http://i0.hdslb.com/test.jpg")
                            .setImgWidth(100.0)
                            .setImgHeight(80.0)
                            .build()
                    )
                    .build()
            )
            .setMember(
                Member.newBuilder()
                    .setMid(3L)
                    .setName("tester")
                    .setFace("https://i0.hdslb.com/avatar.jpg")
                    .build()
            )
            .setReplyControl(
                ReplyControl.newBuilder()
                    .setTimeDesc("刚刚")
                    .build()
            )
            .build()

        val comment = Comment.fromReplyInfo(replyInfo)

        assertEquals(13, comment.repliesCount)
        assertEquals(1, comment.pictures.size)
        assertEquals("https://i0.hdslb.com/test.jpg", comment.pictures.first().url)
    }
}