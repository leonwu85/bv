package dev.aaa1115910.bv.player.entity

/** Selects the next playable entry, ignoring section titles and entries without a valid CID. */
fun List<VideoListItem>.findNextVideoListItem(
    currentVideoCid: Long,
    loop: Boolean = false
): VideoListItemData? {
    // Interactive branches must be selected by the viewer instead of playlist auto-advance.
    if (any { it is VideoListInteractiveNode }) return null

    val playableItems = filterIsInstance<VideoListItemData>().filter { (it.cid ?: 0L) > 0L }
    val currentIndex = playableItems.indexOfFirst { it.cid == currentVideoCid }
    // Preserve starting at the first playable entry when the current CID is not in the list.
    return playableItems.getOrNull(currentIndex + 1)
        ?: if (loop) playableItems.firstOrNull() else null
}

fun List<VideoListItem>.findCurrentVideoListItem(currentVideoCid: Long): VideoListItem? {
    firstOrNull { item ->
        item is VideoListInteractiveNode && item.isCurrent
    }?.let { return it }

    return firstOrNull { item ->
        item.matchesCurrentVideoCid(
            currentVideoCid = currentVideoCid,
            videoList = this
        )
    }
}

fun VideoListItem.matchesCurrentVideoCid(
    currentVideoCid: Long,
    videoList: List<VideoListItem>
): Boolean {
    return when (this) {
        is VideoListInteractiveNode -> {
            isCurrent || (cid == currentVideoCid && videoList.none { it is VideoListInteractiveNode && it.isCurrent })
        }

        is VideoListItemData -> cid == currentVideoCid
        else -> false
    }
}
