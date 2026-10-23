package dev.aaa1115910.bv.util

import androidx.compose.runtime.*
import dev.aaa1115910.biliapi.entity.reply.Comment
import dev.aaa1115910.bv.BVApp

/** Operates on the original text and server mention metadata, never translated content. */
data class CommentFilter(val pureMention: Boolean = false, val mentionLimit: Int = 0, val copyIntro: Boolean = false) {
    fun hides(comment: Comment, description: String = ""): Boolean {
        val text = comment.content.joinToString("").trim()
        val mentions = comment.mentions.keys.filter { it.isNotBlank() }
        val remaining = mentions.sortedByDescending { it.length }.fold(text) { result, name ->
            result.replace("@${name.removePrefix("@")}", "")
        }.trim()
        if (pureMention && mentions.isNotEmpty() && remaining.isEmpty() && comment.pictures.isEmpty() && comment.emotes.isEmpty()) return true
        if (mentionLimit > 0 && mentions.size >= mentionLimit) return true
        return copyIntro && description.isNotBlank() && text == description.trim() && comment.pictures.isEmpty()
    }

    fun apply(comments: List<Comment>, description: String = ""): List<Comment> = comments
        .filterNot { hides(it, description) }
        .map { if (it.replies.isEmpty()) it else it.copy(replies = apply(it.replies, description)) }
}

@Composable
fun rememberFilteredComments(comments: List<Comment>, description: String = ""): List<Comment> {
    val dsm = BVApp.dataStoreManager
    val pure by dsm.getPreferenceFlow(PrefKeys.commentPureMentionRequest).collectAsState(initial = false)
    val limit by dsm.getPreferenceFlow(PrefKeys.commentMentionLimitRequest).collectAsState(initial = 0)
    val copy by dsm.getPreferenceFlow(PrefKeys.commentCopyIntroRequest).collectAsState(initial = false)
    return CommentFilter(pure, limit, copy).apply(comments, description)
}
