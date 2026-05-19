package dev.aaa1115910.bv.player.entity

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
