package dev.aaa1115910.bv.mobile.component.reply

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.aaa1115910.biliapi.entity.reply.CommentVote
import dev.aaa1115910.biliapi.entity.user.DynamicItem
import dev.aaa1115910.bv.mobile.component.home.dynamic.DynamicVoteCard

/**
 * A comment-area vote rendered with the same interaction as a dynamic vote.
 */
@Composable
fun CommentVoteCard(
    vote: CommentVote,
    modifier: Modifier = Modifier
) {
    DynamicVoteCard(
        modifier = modifier,
        dynamicId = null,
        vote = DynamicItem.DynamicVoteModule(
            voteId = vote.id,
            title = vote.title,
            desc = "",
            joinNum = vote.count.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        )
    )
}
