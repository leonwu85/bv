package dev.aaa1115910.bv.player.impl.mpv

/** Demuxer cache limits handed to mpv for one playback session. */
data class MpvCacheConfig(
    /** `--demuxer-max-bytes` */
    val maxBytes: Long,
    /** `--demuxer-max-back-bytes` */
    val maxBackBytes: Long,
    /** `--cache-secs`: readahead cap in seconds, so a low-bitrate audio demuxer cannot fill its byte quota */
    val cacheSecs: Int,
    /** `--cache-pause-wait`: how much data to collect before resuming after an underrun */
    val cachePauseWaitSecs: Double,
) {
    val maxBytesMiB: Long get() = maxBytes / MIB
    val maxBackBytesMiB: Long get() = maxBackBytes / MIB

    companion object {
        const val MIB = 1024L * 1024L
    }
}

/**
 * Picks demuxer cache sizes for the device instead of using one number for every TV.
 *
 * mpv applies `demuxer-max-bytes`/`demuxer-max-back-bytes` per demuxer. Bilibili DASH plays video
 * and audio from two URLs (`--audio-file`), so the worst case is twice the configured value, plus
 * whatever the rest of the app holds. On 1 GB boxes the previous flat 64/32 MiB could reach ~190 MiB
 * and get the process killed; on 4 GB devices it is unnecessarily small for 4K.
 */
object MpvCachePolicy {
    private const val LOW_RAM_LIMIT_BYTES = 1536L * MpvCacheConfig.MIB
    private const val MID_RAM_LIMIT_BYTES = 3072L * MpvCacheConfig.MIB

    private const val LOW_TIER_BYTES = 16L * MpvCacheConfig.MIB
    private const val MID_TIER_BYTES = 32L * MpvCacheConfig.MIB
    private const val HIGH_TIER_BYTES = 64L * MpvCacheConfig.MIB
    private const val EXPANDED_CAP_BYTES = 256L * MpvCacheConfig.MIB

    private const val MIN_LIVE_BYTES = 8L * MpvCacheConfig.MIB
    private const val LIVE_BACK_BYTES = 4L * MpvCacheConfig.MIB

    private const val VOD_CACHE_SECS = 120
    private const val EXPANDED_CACHE_SECS = 300
    private const val LIVE_CACHE_SECS = 30

    private const val VOD_PAUSE_WAIT_SECS = 3.0
    private const val LIVE_PAUSE_WAIT_SECS = 1.5

    /** Base forward cache for the device: 16 / 32 / 64 MiB by total RAM (low-RAM flag forces the smallest). */
    fun baseBytes(totalMemBytes: Long, isLowRamDevice: Boolean): Long = when {
        isLowRamDevice || (totalMemBytes in 1 until LOW_RAM_LIMIT_BYTES) -> LOW_TIER_BYTES
        totalMemBytes in 1 until MID_RAM_LIMIT_BYTES -> MID_TIER_BYTES
        else -> HIGH_TIER_BYTES
    }

    fun resolve(
        totalMemBytes: Long,
        isLowRamDevice: Boolean,
        isLive: Boolean,
        expandBuffer: Boolean,
    ): MpvCacheConfig {
        val base = baseBytes(totalMemBytes, isLowRamDevice)
        return when {
            isLive -> MpvCacheConfig(
                // Live streams arrive in real time; the cache only smooths jitter and nothing seeks backwards.
                maxBytes = (base / 2).coerceAtLeast(MIN_LIVE_BYTES),
                maxBackBytes = LIVE_BACK_BYTES,
                cacheSecs = LIVE_CACHE_SECS,
                cachePauseWaitSecs = LIVE_PAUSE_WAIT_SECS,
            )

            expandBuffer -> MpvCacheConfig(
                maxBytes = (base * 4).coerceAtMost(EXPANDED_CAP_BYTES),
                maxBackBytes = base,
                cacheSecs = EXPANDED_CACHE_SECS,
                cachePauseWaitSecs = VOD_PAUSE_WAIT_SECS,
            )

            else -> MpvCacheConfig(
                maxBytes = base,
                maxBackBytes = base / 2,
                cacheSecs = VOD_CACHE_SECS,
                cachePauseWaitSecs = VOD_PAUSE_WAIT_SECS,
            )
        }
    }
}
