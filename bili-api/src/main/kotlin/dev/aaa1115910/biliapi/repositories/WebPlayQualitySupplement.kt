package dev.aaa1115910.biliapi.repositories

import dev.aaa1115910.biliapi.http.entity.video.PlayUrlData
import kotlinx.coroutines.CancellationException

/** A high-qn response may omit lower DASH qualities advertised in support_formats. */
internal suspend fun fetchWebPlayUrlWithQualitySupplement(
    initialQuality: Int = 127,
    fetch: suspend (Int) -> PlayUrlData,
): PlayUrlData {
    val original = fetch(initialQuality)
    if (original.isPreview == 1) return original
    val dash = original.dash ?: return original
    val availableQualities = dash.video.map { it.id }.toSet()
    val highestQuality = availableQualities.maxOrNull() ?: return original
    val missingQuality = original.supportFormats
        .map { it.quality }
        .filter { it > 0 && it < highestQuality && it !in availableQualities }
        .maxOrNull() ?: return original

    // This optional request must not discard a usable first response or swallow cancellation.
    val supplement = try {
        fetch(missingQuality)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        println("Supplement video quality qn=$missingQuality failed: ${error.javaClass.simpleName}")
        return original
    }
    if (supplement.isPreview == 1) return original
    val extraVideos = supplement.dash?.video.orEmpty()
    if (extraVideos.isEmpty()) return original

    val formats = (original.supportFormats + supplement.supportFormats)
        .groupBy { it.quality }
        .map { (_, entries) ->
            entries.first().copy(codecs = entries.flatMap { it.codecs.orEmpty() }.distinct())
        }

    // Keep the original audio, duration, preview state and other business metadata.
    return original.copy(
        dash = dash.copy(
            video = (dash.video + extraVideos)
                .distinctBy { it.id to it.codecId }
                .sortedByDescending { it.id }
        ),
        supportFormats = formats,
    )
}
