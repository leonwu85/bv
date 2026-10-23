package dev.aaa1115910.bv.util

import dev.aaa1115910.biliapi.entity.DashAudio
import dev.aaa1115910.biliapi.entity.PlayData
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.Resolution

/** Choose only from returned streams, with the preference acting as a quality ceiling. */
object MediaQualitySelector {
    fun video(available: Collection<Resolution>, preferred: Resolution): Resolution? =
        available.firstOrNull { it == preferred }
            ?: available.filter { it.code <= preferred.code }.maxByOrNull { it.code }
            ?: available.minByOrNull { it.code }

    fun audio(available: Collection<Audio>, preferred: Audio): Audio? =
        available.firstOrNull { it == preferred }
            ?: available.filter { it.rank <= preferred.rank }.maxByOrNull { it.rank }
            ?: available.minByOrNull { it.rank }
}

fun PlayData.playableAudioTracks(): List<DashAudio> =
    (dashAudios + listOfNotNull(dolby, flac)).filter { it.baseUrl.isNotBlank() }
        .distinctBy { it.codecId }

fun PlayData.selectAudioTrack(preferred: Audio): DashAudio? {
    val tracks = playableAudioTracks()
    val quality = MediaQualitySelector.audio(tracks.mapNotNull { Audio.fromCode(it.codecId) }, preferred)
    return tracks.firstOrNull { it.codecId == quality?.code }
}
