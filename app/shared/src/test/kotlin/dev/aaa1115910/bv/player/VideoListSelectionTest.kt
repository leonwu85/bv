package dev.aaa1115910.bv.player

import dev.aaa1115910.bv.player.entity.VideoListInteractiveNode
import dev.aaa1115910.bv.player.entity.VideoListItem
import dev.aaa1115910.bv.player.entity.VideoListPart
import dev.aaa1115910.bv.player.entity.VideoListPgcEpisode
import dev.aaa1115910.bv.player.entity.VideoListUgcEpisode
import dev.aaa1115910.bv.player.entity.VideoListUgcEpisodeTitle
import dev.aaa1115910.bv.player.entity.findCurrentVideoListItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class VideoListSelectionTest {
    @Test
    fun missingCurrentCidReturnsNullInsteadOfThrowing() {
        val items = listOf<VideoListItem>(
            VideoListUgcEpisodeTitle(index = 0, title = "合集"),
            part(cid = 1L, index = 0)
        )

        assertNull(items.findCurrentVideoListItem(currentVideoCid = 99L))
        assertNull(emptyList<VideoListItem>().findCurrentVideoListItem(currentVideoCid = 99L))
    }

    @Test
    fun findsCurrentItemAcrossCommonVideoListTypes() {
        val ugc = VideoListUgcEpisode(aid = 1L, cid = 2L, title = "UGC", index = 1)
        val pgc = VideoListPgcEpisode(aid = 1L, cid = 3L, title = "PGC", index = 2)
        val items = listOf(part(cid = 1L, index = 0), ugc, pgc)

        assertSame(items[0], items.findCurrentVideoListItem(currentVideoCid = 1L))
        assertSame(ugc, items.findCurrentVideoListItem(currentVideoCid = 2L))
        assertSame(pgc, items.findCurrentVideoListItem(currentVideoCid = 3L))
    }

    @Test
    fun interactiveCurrentNodeWinsEvenWhenCidHasMoved() {
        val currentInteractiveNode = VideoListInteractiveNode(
            aid = 1L,
            cid = 2L,
            title = "互动节点",
            index = 1,
            nodeId = 20L,
            isCurrent = true
        )
        val items = listOf(part(cid = 1L, index = 0), currentInteractiveNode)

        assertSame(currentInteractiveNode, items.findCurrentVideoListItem(currentVideoCid = 1L))
    }

    @Test
    fun interactiveNodeFallsBackToCidWhenNoNodeIsMarkedCurrent() {
        val interactiveNode = VideoListInteractiveNode(
            aid = 1L,
            cid = 2L,
            title = "互动节点",
            index = 1,
            nodeId = 20L
        )
        val items = listOf(part(cid = 1L, index = 0), interactiveNode)

        assertEquals(interactiveNode, items.findCurrentVideoListItem(currentVideoCid = 2L))
    }

    private fun part(
        cid: Long,
        index: Int
    ) = VideoListPart(
        aid = 1L,
        cid = cid,
        title = "P$index",
        index = index
    )
}
