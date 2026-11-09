package dev.aaa1115910.biliapi.entity.reply

/** One pager per target and sort. Failed requests leave the last committed cursor intact. */
class CommentPager {
    private var next = CommentPage()
    private val consumed = mutableSetOf<CommentPage>()
    private val seenIds = mutableSetOf<Long>()

    suspend fun load(
        isVisible: (Comment) -> Boolean,
        fetch: suspend (CommentPage) -> CommentsData,
    ): CommentsData {
        var page = next
        val pages = mutableSetOf<CommentPage>()
        val ids = mutableSetOf<Long>()
        val comments = mutableListOf<Comment>()
        var vote: CommentVote? = null
        var hasNext: Boolean
        do {
            pages.add(page)
            val result = fetch(page)
            val fresh = result.comments.filter { it.rpid !in seenIds && ids.add(it.rpid) }
            comments.addAll(fresh)
            if (page == CommentPage()) vote = result.vote
            page = result.nextPage
            hasNext = result.hasNext && page != CommentPage() && page !in consumed && page !in pages
        } while (hasNext && fresh.none(isVisible) && pages.size < 3)
        next = page
        consumed.addAll(pages)
        seenIds.addAll(ids)
        return CommentsData(comments, page, hasNext, vote)
    }
}
