package dev.aaa1115910.bv.player

import dev.aaa1115910.bv.player.entity.VideoListInteractiveNode
import dev.aaa1115910.bv.player.entity.VideoListItem
import dev.aaa1115910.bv.player.entity.VideoListItemData
import dev.aaa1115910.bv.player.entity.VideoListPart
import dev.aaa1115910.bv.player.entity.VideoListPgcEpisode
import dev.aaa1115910.bv.player.entity.VideoListUgcEpisode
import dev.aaa1115910.bv.player.entity.VideoListUgcEpisodeTitle
import dev.aaa1115910.bv.player.entity.findCurrentVideoListItem
import dev.aaa1115910.bv.player.entity.findNextVideoListItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class VideoListSelectionTest {
    @Test
    fun autoAdvanceStopsWhenThereAreNoPlayableEntries() {
        val lists = listOf(
            emptyList(),
            listOf(VideoListUgcEpisodeTitle(index = 0, title = "空合集")),
            listOf(unplayableItem(null), unplayableItem(0L), unplayableItem(-1L))
        )

        for (items in lists) {
            assertNull(items.findNextVideoListItem(currentVideoCid = 1L))
            assertNull(items.findNextVideoListItem(currentVideoCid = 1L, loop = true))
        }
    }

    @Test
    fun autoAdvanceSkipsSectionTitlesAndInvalidCids() {
        val first = part(cid = 1L, index = 0)
        val ugc = VideoListUgcEpisode(aid = 2L, cid = 2L, title = "UGC", index = 1)
        val pgc = VideoListPgcEpisode(aid = 3L, cid = 3L, title = "PGC", index = 2)
        val items = listOf(
            first,
            VideoListUgcEpisodeTitle(index = 1, title = "合集"),
            unplayableItem(null),
            unplayableItem(0L),
            unplayableItem(-1L),
            ugc,
            pgc
        )

        for (loop in listOf(false, true)) {
            assertSame(ugc, items.findNextVideoListItem(currentVideoCid = 1L, loop = loop))
            assertSame(pgc, items.findNextVideoListItem(currentVideoCid = 2L, loop = loop))
        }
    }

    @Test
    fun sequentialPlaybackStopsAfterLastPlayableEntry() {
        val items = listOf(
            part(cid = 1L, index = 0),
            part(cid = 2L, index = 1),
            VideoListUgcEpisodeTitle(index = 2, title = "空分组"),
            unplayableItem(null)
        )

        assertNull(items.findNextVideoListItem(currentVideoCid = 2L))
        assertNull(items.take(2).findNextVideoListItem(currentVideoCid = 2L))
    }

    @Test
    fun listLoopWrapsToFirstPlayableEntryEvenWithTrailingSectionTitles() {
        val first = part(cid = 1L, index = 1)
        val items = listOf(
            VideoListUgcEpisodeTitle(index = 0, title = "合集"),
            unplayableItem(null),
            first,
            part(cid = 2L, index = 2),
            VideoListUgcEpisodeTitle(index = 3, title = "空分组")
        )

        assertSame(first, items.findNextVideoListItem(currentVideoCid = 2L, loop = true))
    }

    @Test
    fun singleEntryListLoopsOnlyWhenRequested() {
        val current = part(cid = 1L, index = 0)
        val items = listOf(current)

        assertNull(items.findNextVideoListItem(currentVideoCid = 1L))
        assertSame(current, items.findNextVideoListItem(currentVideoCid = 1L, loop = true))
    }

    @Test
    fun missingCurrentCidStartsAtFirstPlayableEntry() {
        val first = part(cid = 1L, index = 1)
        val items = listOf(
            VideoListUgcEpisodeTitle(index = 0, title = "合集"),
            unplayableItem(null),
            first
        )

        assertSame(first, items.findNextVideoListItem(currentVideoCid = 99L))
        assertSame(first, items.findNextVideoListItem(currentVideoCid = 99L, loop = true))
    }

    @Test
    fun autoAdvanceNeverSelectsInteractiveBranches() {
        val items = listOf(
            part(cid = 1L, index = 0),
            VideoListInteractiveNode(aid = 1L, cid = 2L, title = "分支", index = 1, nodeId = 20L),
            part(cid = 3L, index = 2)
        )

        assertNull(items.findNextVideoListItem(currentVideoCid = 1L))
        assertNull(items.findNextVideoListItem(currentVideoCid = 3L, loop = true))
    }

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

    private fun unplayableItem(cid: Long?) = VideoListItemData(
        aid = 1L,
        cid = cid,
        title = "缺少有效 CID",
        index = 0
    )

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
