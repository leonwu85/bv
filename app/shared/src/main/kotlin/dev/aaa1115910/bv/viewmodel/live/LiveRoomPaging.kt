package dev.aaa1115910.bv.viewmodel.live

import dev.aaa1115910.biliapi.entity.live.LiveRoomItem

/** Builds a new cached page off the UI thread without modifying earlier area snapshots. */
internal fun mergeLiveRoomPage(
    previous: List<LiveRoomItem>,
    incoming: List<LiveRoomItem>,
    refresh: Boolean,
): List<LiveRoomItem> {
    if (refresh) return incoming.distinctBy { it.roomId }
    val knownIds = previous.mapTo(HashSet()) { it.roomId }
    val newRooms = incoming.filter { knownIds.add(it.roomId) }
    return if (newRooms.isEmpty()) previous else previous + newRooms
}
