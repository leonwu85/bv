package dev.aaa1115910.biliapi.entity.reply

import bilibili.main.community.reply.v1.Content
import bilibili.main.community.reply.v1.Member
import bilibili.main.community.reply.v1.Picture
import bilibili.main.community.reply.v1.ReplyControl
import bilibili.main.community.reply.v1.ReplyInfo
import bilibili.main.community.reply.v1.TranslationSwitch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
        assertFalse(comment.canTranslate)
        assertFalse(comment.showTranslation)
    }

    @Test
    fun `fromReplyInfo maps translated content fields`() {
        val replyInfo = ReplyInfo.newBuilder()
            .setId(1L)
            .setOid(2L)
            .setType(1L)
            .setMid(3L)
            .setParent(0L)
            .setCount(1L)
            .setLike(5L)
            .setContent(
                Content.newBuilder()
                    .setMessage("原文")
                    .build()
            )
            .setTranslatedContent(
                Content.newBuilder()
                    .setMessage("Translated")
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
                    .setTranslationSwitch(TranslationSwitch.TRANSLATION_SWITCH_SHOW_TRANSLATION)
                    .setShowTranslation(true)
                    .build()
            )
            .build()

        val comment = Comment.fromReplyInfo(replyInfo)

        assertTrue(comment.canTranslate)
        assertTrue(comment.showTranslation)
        assertEquals(TranslationSwitch.TRANSLATION_SWITCH_SHOW_TRANSLATION, comment.translationSwitch)
        assertEquals("Translated", comment.translatedContent.joinToString(separator = ""))
        assertEquals("Translated", comment.displayContent.joinToString(separator = ""))
    }
}