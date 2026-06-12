@file:Suppress("SpellCheckingInspection")

package dev.aaa1115910.bv.mobile.settings

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import de.schnettler.datastore.manager.PreferenceRequest
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.entity.CdnService
import dev.aaa1115910.bv.entity.LiveQualityPreference
import dev.aaa1115910.bv.entity.PlayerType
import dev.aaa1115910.bv.entity.ThemeType
import dev.aaa1115910.bv.mobile.theme.MobileThemePalette
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.DanmakuSpeedMode
import dev.aaa1115910.bv.player.entity.DanmakuType
import dev.aaa1115910.bv.player.entity.LiveCodec
import dev.aaa1115910.bv.player.entity.PlayMode
import dev.aaa1115910.bv.player.entity.PlayerDefaultStartPosition
import dev.aaa1115910.bv.player.entity.PortraitVideoFixMode
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.SponsorBlockSkipMode
import dev.aaa1115910.bv.player.entity.SuperResolutionType
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.player.util.DanmakuSpeedPolicy
import dev.aaa1115910.bv.util.PlaybackPreferenceSelector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt

object MobilePrefs {
    private val dsm get() = BVApp.dataStoreManager

    const val DEFAULT_SEED_COLOR = 0xFF5CB67B.toInt()

    private fun sanitizeDanmakuFilterLevel(value: Int) = value.coerceIn(1, 10)

    private fun sanitizeLiveDanmakuFilterLevel(value: Int) = value.coerceIn(0, 60)

    var themeType: ThemeType
        get() = ThemeType.entries[read(MobilePrefKeys.themeTypeRequest)]
        set(value) = write(MobilePrefKeys.themeTypeKey, value.ordinal)

    val themeTypeFlow: Flow<ThemeType>
        get() = dsm.getPreferenceFlow(MobilePrefKeys.themeTypeRequest)
            .transform { emit(ThemeType.entries[it]) }

    var themePalette: MobileThemePalette
        get() = MobileThemePalette.entries.getOrElse(
            read(MobilePrefKeys.themePaletteRequest)
        ) { MobileThemePalette.Default }
        set(value) = write(MobilePrefKeys.themePaletteKey, value.ordinal)

    val themePaletteFlow: Flow<MobileThemePalette>
        get() = dsm.getPreferenceFlow(MobilePrefKeys.themePaletteRequest)
            .transform { ordinal ->
                emit(MobileThemePalette.entries.getOrElse(ordinal) { MobileThemePalette.Default })
            }

    var dynamicColor: Boolean
        get() = read(MobilePrefKeys.dynamicColorRequest)
        set(value) = write(MobilePrefKeys.dynamicColorKey, value)

    val dynamicColorFlow: Flow<Boolean>
        get() = dsm.getPreferenceFlow(MobilePrefKeys.dynamicColorRequest)

    var seedColor: Int
        get() = read(MobilePrefKeys.seedColorRequest)
        set(value) = write(MobilePrefKeys.seedColorKey, value)

    val seedColorFlow: Flow<Int>
        get() = dsm.getPreferenceFlow(MobilePrefKeys.seedColorRequest)

    var playerType: PlayerType
        get() = resolveMobilePlayerType(read(MobilePrefKeys.playerTypeRequest))
        set(value) = write(MobilePrefKeys.playerTypeKey, resolveMobilePlayerType(value.ordinal).ordinal)

    fun sanitizePlayerType() {
        val rawPlayerType = read(MobilePrefKeys.playerTypeRequest)
        val resolvedPlayerType = resolveMobilePlayerType(rawPlayerType)
        if (rawPlayerType != resolvedPlayerType.ordinal) {
            playerType = resolvedPlayerType
        }
    }

    var apiType: ApiType
        get() = ApiType.entries[read(MobilePrefKeys.apiTypeRequest)]
        set(value) = write(MobilePrefKeys.apiTypeKey, value.ordinal)

    var defaultQuality: Resolution
        get() = Resolution.fromCode(read(MobilePrefKeys.defaultQualityRequest)) ?: Resolution.R1080P
        set(value) = write(MobilePrefKeys.defaultQualityKey, value.code)

    var defaultCellularQuality: Resolution
        get() = Resolution.fromCode(read(MobilePrefKeys.defaultCellularQualityRequest)) ?: defaultQuality
        set(value) = write(MobilePrefKeys.defaultCellularQualityKey, value.code)

    var defaultVideoCodec: VideoCodec
        get() = VideoCodec.fromCode(read(MobilePrefKeys.defaultVideoCodecRequest))
        set(value) = write(MobilePrefKeys.defaultVideoCodecKey, value.ordinal)

    var secondVideoCodec: VideoCodec
        get() = VideoCodec.fromCode(read(MobilePrefKeys.secondVideoCodecRequest))
        set(value) = write(MobilePrefKeys.secondVideoCodecKey, value.ordinal)

    var currentPlaySpeed: Float
        get() = read(MobilePrefKeys.currentPlaySpeedRequest)
        set(value) = write(MobilePrefKeys.currentPlaySpeedKey, value)

    var autoPlay: Boolean
        get() = read(MobilePrefKeys.autoPlayRequest)
        set(value) = write(MobilePrefKeys.autoPlayKey, value)

    var enableTunneling: Boolean
        get() = read(MobilePrefKeys.enableTunnelingRequest)
        set(value) = write(MobilePrefKeys.enableTunnelingKey, value)

    var enableMobileTunneling: Boolean
        get() = read(MobilePrefKeys.enableMobileTunnelingRequest)
        set(value) = write(MobilePrefKeys.enableMobileTunnelingKey, value)

    var enableFfmpegAudioRenderer: Boolean
        get() = read(MobilePrefKeys.enableFfmpegAudioRendererRequest)
        set(value) = write(MobilePrefKeys.enableFfmpegAudioRendererKey, value)

    var enableAsyncQueueing: Boolean
        get() = read(MobilePrefKeys.enableAsyncQueueingRequest)
        set(value) = write(MobilePrefKeys.enableAsyncQueueingKey, value)

    var enableAudioPlaybackParams: Boolean
        get() = read(MobilePrefKeys.enableAudioPlaybackParamsRequest)
        set(value) = write(MobilePrefKeys.enableAudioPlaybackParamsKey, value)

    var enableHardwareDecode: Boolean
        get() = read(MobilePrefKeys.enableHardwareDecodeRequest)
        set(value) = write(MobilePrefKeys.enableHardwareDecodeKey, value)

    var expandBuffer: Boolean
        get() = read(MobilePrefKeys.expandBufferRequest)
        set(value) = write(MobilePrefKeys.expandBufferKey, value)

    var defaultAudio: Audio
        get() = Audio.fromCode(read(MobilePrefKeys.defaultAudioRequest)) ?: Audio.A192K
        set(value) = write(MobilePrefKeys.defaultAudioKey, value.code)

    var defaultCellularAudio: Audio
        get() = Audio.fromCode(read(MobilePrefKeys.defaultCellularAudioRequest)) ?: defaultAudio
        set(value) = write(MobilePrefKeys.defaultCellularAudioKey, value.code)

    var defaultDanmakuScale: Float
        get() = read(MobilePrefKeys.defaultDanmakuScaleRequest)
        set(value) = write(MobilePrefKeys.defaultDanmakuScaleKey, value)

    var defaultMobileDanmakuScale: Float
        get() = read(MobilePrefKeys.defaultMobileDanmakuScaleRequest)
        set(value) = write(MobilePrefKeys.defaultMobileDanmakuScaleKey, value)

    var defaultDanmakuOpacity: Float
        get() = read(MobilePrefKeys.defaultDanmakuOpacityRequest)
        set(value) = write(MobilePrefKeys.defaultDanmakuOpacityKey, value)

    var defaultDanmakuEnabled: Boolean
        get() = read(MobilePrefKeys.defaultDanmakuEnabledRequest)
        set(value) = write(MobilePrefKeys.defaultDanmakuEnabledKey, value)

    var defaultDanmakuTypes: List<DanmakuType>
        get() {
            val value = read(MobilePrefKeys.defaultDanmakuTypesRequest)
            return if (value.isBlank()) emptyList()
            else value.split(",").mapNotNull { index ->
                index.toIntOrNull()?.let { DanmakuType.entries.getOrNull(it) }
            }
        }
        set(value) = write(
            MobilePrefKeys.defaultDanmakuTypesKey,
            value.joinToString(",") { it.ordinal.toString() }
        )

    var defaultDanmakuArea: Float
        get() = read(MobilePrefKeys.defaultDanmakuAreaRequest)
        set(value) = write(MobilePrefKeys.defaultDanmakuAreaKey, value)

    var defaultDanmakuSpeedMode: DanmakuSpeedMode
        get() = DanmakuSpeedMode.fromOrdinal(read(MobilePrefKeys.defaultDanmakuSpeedModeRequest))
        set(value) = write(MobilePrefKeys.defaultDanmakuSpeedModeKey, value.ordinal)

    var defaultDanmakuPresentationSpeed: Float
        get() = DanmakuSpeedPolicy.sanitizePresentationSpeed(
            read(MobilePrefKeys.defaultDanmakuPresentationSpeedRequest)
        )
        set(value) = write(
            MobilePrefKeys.defaultDanmakuPresentationSpeedKey,
            DanmakuSpeedPolicy.sanitizePresentationSpeed(value)
        )

    var defaultDanmakuMask: Boolean
        get() = read(MobilePrefKeys.defaultDanmakuMaskRequest)
        set(value) = write(MobilePrefKeys.defaultDanmakuMaskKey, value)

    var defaultDanmakuFilterLevel: Int
        get() = sanitizeDanmakuFilterLevel(read(MobilePrefKeys.defaultDanmakuFilterLevelRequest))
        set(value) = write(MobilePrefKeys.defaultDanmakuFilterLevelKey, sanitizeDanmakuFilterLevel(value))

    var defaultDanmakuMergeEnabled: Boolean
        get() = read(MobilePrefKeys.defaultDanmakuMergeEnabledRequest)
        set(value) = write(MobilePrefKeys.defaultDanmakuMergeEnabledKey, value)

    var defaultLiveDanmakuFilterLevel: Int
        get() = sanitizeLiveDanmakuFilterLevel(read(MobilePrefKeys.defaultLiveDanmakuFilterLevelRequest))
        set(value) = write(MobilePrefKeys.defaultLiveDanmakuFilterLevelKey, sanitizeLiveDanmakuFilterLevel(value))

    var defaultSubtitleFontSize: TextUnit
        get() = read(MobilePrefKeys.defaultSubtitleFontSizeRequest).sp
        set(value) = write(MobilePrefKeys.defaultSubtitleFontSizeKey, value.value.roundToInt())

    var defaultSubtitleBackgroundOpacity: Float
        get() = read(MobilePrefKeys.defaultSubtitleBackgroundOpacityRequest)
        set(value) = write(MobilePrefKeys.defaultSubtitleBackgroundOpacityKey, value)

    var defaultSubtitleBottomPadding: Dp
        get() = read(MobilePrefKeys.defaultSubtitleBottomPaddingRequest).dp
        set(value) = write(MobilePrefKeys.defaultSubtitleBottomPaddingKey, value.value.roundToInt())

    var defaultSecondarySubtitleFontSize: TextUnit
        get() = read(MobilePrefKeys.defaultSecondarySubtitleFontSizeRequest).sp
        set(value) = write(MobilePrefKeys.defaultSecondarySubtitleFontSizeKey, value.value.roundToInt())

    var defaultSecondarySubtitleBackgroundOpacity: Float
        get() = read(MobilePrefKeys.defaultSecondarySubtitleBackgroundOpacityRequest)
        set(value) = write(MobilePrefKeys.defaultSecondarySubtitleBackgroundOpacityKey, value)

    var defaultSecondarySubtitleBottomPadding: Dp
        get() = read(MobilePrefKeys.defaultSecondarySubtitleBottomPaddingRequest).dp
        set(value) = write(MobilePrefKeys.defaultSecondarySubtitleBottomPaddingKey, value.value.roundToInt())

    var defaultPlayMode: PlayMode
        get() = PlayMode.entries[read(MobilePrefKeys.defaultPlayModeRequest)]
        set(value) = write(MobilePrefKeys.defaultPlayModeKey, value.ordinal)

    var defaultLiveQn: Int
        get() = read(MobilePrefKeys.defaultLiveQnRequest)
        set(value) = write(MobilePrefKeys.defaultLiveQnKey, value)

    var defaultCellularLiveQn: Int
        get() = read(MobilePrefKeys.defaultCellularLiveQnRequest)
        set(value) = write(MobilePrefKeys.defaultCellularLiveQnKey, value)

    var defaultLiveCodec: LiveCodec
        get() = LiveCodec.fromCode(read(MobilePrefKeys.defaultLiveCodecRequest))
        set(value) = write(MobilePrefKeys.defaultLiveCodecKey, value.ordinal)

    var isLoop: Boolean
        get() = read(MobilePrefKeys.isLoopRequest)
        set(value) = write(MobilePrefKeys.isLoopKey, value)

    var showDanmaku: Boolean
        get() = read(MobilePrefKeys.showDanmakuRequest)
        set(value) = write(MobilePrefKeys.showDanmakuKey, value)

    var enableSponsorBlock: Boolean
        get() = read(MobilePrefKeys.enableSponsorBlockRequest)
        set(value) = write(MobilePrefKeys.enableSponsorBlockKey, value)

    var sponsorBlockSkipMode: SponsorBlockSkipMode
        get() = SponsorBlockSkipMode.fromValue(read(MobilePrefKeys.sponsorBlockSkipModeRequest))
        set(value) = write(MobilePrefKeys.sponsorBlockSkipModeKey, value.value)

    var sponsorBlockApiServer: String
        get() = read(MobilePrefKeys.sponsorBlockApiServerRequest)
        set(value) = write(MobilePrefKeys.sponsorBlockApiServerKey, value)

    var portraitVideoFixMode: PortraitVideoFixMode
        get() = PortraitVideoFixMode.fromValue(read(MobilePrefKeys.portraitVideoFixModeRequest))
        set(value) = write(MobilePrefKeys.portraitVideoFixModeKey, value.value)

    var playerDefaultStartPosition: PlayerDefaultStartPosition
        get() = PlayerDefaultStartPosition.fromValue(read(MobilePrefKeys.playerDefaultStartPositionRequest))
        set(value) = write(MobilePrefKeys.playerDefaultStartPositionKey, value.value)

    var playerShowDebugInfo: Boolean
        get() = read(MobilePrefKeys.playerShowDebugInfoRequest)
        set(value) = write(MobilePrefKeys.playerShowDebugInfoKey, value)

    var liveIncognitoMode: Boolean
        get() = read(MobilePrefKeys.liveIncognitoModeRequest)
        set(value) = write(MobilePrefKeys.liveIncognitoModeKey, value)

    var preferOfficialCdn: Boolean
        get() = read(MobilePrefKeys.preferOfficialCdnRequest)
        set(value) = write(MobilePrefKeys.preferOfficialCdnKey, value)

    var cdnService: CdnService
        get() = CdnService.fromOrdinal(read(MobilePrefKeys.cdnServiceRequest))
        set(value) = write(MobilePrefKeys.cdnServiceKey, value.ordinal)

    var liveCdnUrl: String
        get() = read(MobilePrefKeys.liveCdnUrlRequest)
        set(value) = write(MobilePrefKeys.liveCdnUrlKey, PlaybackPreferenceSelector.normalizeLiveCdnHost(value))

    var cdnSpeedTest: Boolean
        get() = read(MobilePrefKeys.cdnSpeedTestRequest)
        set(value) = write(MobilePrefKeys.cdnSpeedTestKey, value)

    var disableAudioCdn: Boolean
        get() = read(MobilePrefKeys.disableAudioCdnRequest)
        set(value) = write(MobilePrefKeys.disableAudioCdnKey, value)

    var tryLook1080P: Boolean
        get() = read(MobilePrefKeys.tryLook1080PRequest)
        set(value) = write(MobilePrefKeys.tryLook1080PKey, value)

    var autoSync: String
        get() = read(MobilePrefKeys.autoSyncRequest)
        set(value) = write(MobilePrefKeys.autoSyncKey, value.trim())

    var videoSync: String
        get() = read(MobilePrefKeys.videoSyncRequest)
        set(value) = write(MobilePrefKeys.videoSyncKey, value)

    var hardwareDecodeMode: String
        get() = read(MobilePrefKeys.hardwareDecodeModeRequest)
        set(value) = write(MobilePrefKeys.hardwareDecodeModeKey, value)

    var mpvHardwareDecodeCodecs: String
        get() = read(MobilePrefKeys.mpvHardwareDecodeCodecsRequest)
        set(value) = write(MobilePrefKeys.mpvHardwareDecodeCodecsKey, value.trim())

    var mpvVideoOutput: String
        get() = read(MobilePrefKeys.mpvVideoOutputRequest)
        set(value) = write(MobilePrefKeys.mpvVideoOutputKey, value.trim())

    var mpvGpuContext: String
        get() = read(MobilePrefKeys.mpvGpuContextRequest)
        set(value) = write(MobilePrefKeys.mpvGpuContextKey, value.trim())

    var mpvGpuApi: String
        get() = read(MobilePrefKeys.mpvGpuApiRequest)
        set(value) = write(MobilePrefKeys.mpvGpuApiKey, value.trim())

    var mpvCache: String
        get() = read(MobilePrefKeys.mpvCacheRequest)
        set(value) = write(MobilePrefKeys.mpvCacheKey, value.trim())

    var mpvDemuxerMaxBytes: String
        get() = read(MobilePrefKeys.mpvDemuxerMaxBytesRequest)
        set(value) = write(MobilePrefKeys.mpvDemuxerMaxBytesKey, value.trim())

    var mpvDemuxerMaxBackBytes: String
        get() = read(MobilePrefKeys.mpvDemuxerMaxBackBytesRequest)
        set(value) = write(MobilePrefKeys.mpvDemuxerMaxBackBytesKey, value.trim())

    var mpvVdQueueEnable: String
        get() = read(MobilePrefKeys.mpvVdQueueEnableRequest)
        set(value) = write(MobilePrefKeys.mpvVdQueueEnableKey, value.trim())

    var superResolutionType: SuperResolutionType
        get() = SuperResolutionType.fromValue(read(MobilePrefKeys.superResolutionTypeRequest))
        set(value) = write(MobilePrefKeys.superResolutionTypeKey, value.value)

    var audioOutputDevices: String
        get() = read(MobilePrefKeys.audioOutputDevicesRequest)
        set(value) = write(MobilePrefKeys.audioOutputDevicesKey, value.trim())

    var showLiveDanmakuEmoji: Boolean
        get() = read(MobilePrefKeys.showLiveDanmakuEmojiRequest)
        set(value) = write(MobilePrefKeys.showLiveDanmakuEmojiKey, value)

    var incognitoMode: Boolean
        get() = read(MobilePrefKeys.incognitoModeRequest)
        set(value) = write(MobilePrefKeys.incognitoModeKey, value)

    private fun resolveMobilePlayerType(ordinal: Int): PlayerType =
        PlayerType.entries.getOrElse(ordinal) { PlayerType.Media3 }
            .takeUnless { it == PlayerType.VLC }
            ?: PlayerType.Media3

    private fun <T> read(request: PreferenceRequest<T>): T =
        runBlocking { dsm.getPreferenceFlow(request).first() }

    private fun <T> write(key: androidx.datastore.preferences.core.Preferences.Key<T>, value: T) {
        runBlocking { dsm.editPreference(key, value) }
    }
}

object MobilePrefKeys {
    val themeTypeKey = intPreferencesKey("mobile_theme_type")
    val themePaletteKey = intPreferencesKey("mobile_theme_palette")
    val dynamicColorKey = booleanPreferencesKey("mobile_theme_dynamic_color")
    val seedColorKey = intPreferencesKey("mobile_theme_seed_color")
    val playerTypeKey = intPreferencesKey("mobile_player_type")
    val apiTypeKey = intPreferencesKey("mobile_api_type")
    val defaultQualityKey = intPreferencesKey("mobile_default_quality")
    val defaultCellularQualityKey = intPreferencesKey("mobile_default_cellular_quality")
    val defaultVideoCodecKey = intPreferencesKey("mobile_default_video_codec")
    val secondVideoCodecKey = intPreferencesKey("mobile_second_video_codec")
    val currentPlaySpeedKey = floatPreferencesKey("mobile_current_play_speed")
    val autoPlayKey = booleanPreferencesKey("mobile_player_auto_play")
    val enableTunnelingKey = booleanPreferencesKey("mobile_enable_tunneling")
    val enableMobileTunnelingKey = booleanPreferencesKey("mobile_enable_mobile_tunneling")
    val enableFfmpegAudioRendererKey = booleanPreferencesKey("mobile_enable_ffmpeg_audio_renderer")
    val enableAsyncQueueingKey = booleanPreferencesKey("mobile_enable_async_queueing")
    val enableAudioPlaybackParamsKey = booleanPreferencesKey("mobile_enable_audio_playback_params")
    val enableHardwareDecodeKey = booleanPreferencesKey("mobile_enable_hardware_decode")
    val expandBufferKey = booleanPreferencesKey("mobile_expand_buffer")
    val defaultAudioKey = intPreferencesKey("mobile_default_audio")
    val defaultCellularAudioKey = intPreferencesKey("mobile_default_cellular_audio")
    val defaultDanmakuScaleKey = floatPreferencesKey("mobile_default_danmaku_scale")
    val defaultMobileDanmakuScaleKey = floatPreferencesKey("mobile_default_mobile_danmaku_scale")
    val defaultDanmakuOpacityKey = floatPreferencesKey("mobile_default_danmaku_opacity")
    val defaultDanmakuEnabledKey = booleanPreferencesKey("mobile_default_danmaku_enabled")
    val defaultDanmakuTypesKey = stringPreferencesKey("mobile_default_danmaku_types")
    val defaultDanmakuAreaKey = floatPreferencesKey("mobile_default_danmaku_area")
    val defaultDanmakuSpeedModeKey = intPreferencesKey("mobile_default_danmaku_speed_mode")
    val defaultDanmakuPresentationSpeedKey = floatPreferencesKey("mobile_default_danmaku_presentation_speed")
    val defaultDanmakuMaskKey = booleanPreferencesKey("mobile_default_danmaku_mask")
    val defaultDanmakuFilterLevelKey = intPreferencesKey("mobile_default_danmaku_filter_level")
    val defaultDanmakuMergeEnabledKey = booleanPreferencesKey("mobile_default_danmaku_merge_enabled")
    val defaultLiveDanmakuFilterLevelKey = intPreferencesKey("mobile_default_live_danmaku_filter_level")
    val defaultSubtitleFontSizeKey = intPreferencesKey("mobile_default_subtitle_font_size")
    val defaultSubtitleBackgroundOpacityKey = floatPreferencesKey("mobile_default_subtitle_background_opacity")
    val defaultSubtitleBottomPaddingKey = intPreferencesKey("mobile_default_subtitle_bottom_padding")
    val defaultSecondarySubtitleFontSizeKey = intPreferencesKey("mobile_default_secondary_subtitle_font_size")
    val defaultSecondarySubtitleBackgroundOpacityKey = floatPreferencesKey("mobile_default_secondary_subtitle_background_opacity")
    val defaultSecondarySubtitleBottomPaddingKey = intPreferencesKey("mobile_default_secondary_subtitle_bottom_padding")
    val defaultPlayModeKey = intPreferencesKey("mobile_default_play_mode")
    val defaultLiveQnKey = intPreferencesKey("mobile_default_live_qn")
    val defaultCellularLiveQnKey = intPreferencesKey("mobile_default_cellular_live_qn")
    val defaultLiveCodecKey = intPreferencesKey("mobile_default_live_codec")
    val isLoopKey = booleanPreferencesKey("mobile_player_is_loop")
    val showDanmakuKey = booleanPreferencesKey("mobile_player_show_danmaku")
    val enableSponsorBlockKey = booleanPreferencesKey("mobile_enable_sponsor_block")
    val sponsorBlockSkipModeKey = intPreferencesKey("mobile_sponsor_block_skip_mode")
    val sponsorBlockApiServerKey = stringPreferencesKey("mobile_sponsor_block_api_server")
    val portraitVideoFixModeKey = intPreferencesKey("mobile_portrait_video_fix_mode")
    val playerDefaultStartPositionKey = intPreferencesKey("mobile_player_default_start_position")
    val playerShowDebugInfoKey = booleanPreferencesKey("mobile_player_show_debug_info")
    val liveIncognitoModeKey = booleanPreferencesKey("mobile_live_incognito_mode")
    val preferOfficialCdnKey = booleanPreferencesKey("mobile_prefer_official_cdn")
    val cdnServiceKey = intPreferencesKey("mobile_cdn_service")
    val liveCdnUrlKey = stringPreferencesKey("mobile_live_cdn_url")
    val cdnSpeedTestKey = booleanPreferencesKey("mobile_cdn_speed_test")
    val disableAudioCdnKey = booleanPreferencesKey("mobile_disable_audio_cdn")
    val tryLook1080PKey = booleanPreferencesKey("mobile_try_look_1080p")
    val autoSyncKey = stringPreferencesKey("mobile_auto_sync")
    val videoSyncKey = stringPreferencesKey("mobile_video_sync")
    val hardwareDecodeModeKey = stringPreferencesKey("mobile_hardware_decode_mode")
    val mpvHardwareDecodeCodecsKey = stringPreferencesKey("mobile_mpv_hardware_decode_codecs")
    val mpvVideoOutputKey = stringPreferencesKey("mobile_mpv_video_output")
    val mpvGpuContextKey = stringPreferencesKey("mobile_mpv_gpu_context")
    val mpvGpuApiKey = stringPreferencesKey("mobile_mpv_gpu_api")
    val mpvCacheKey = stringPreferencesKey("mobile_mpv_cache")
    val mpvDemuxerMaxBytesKey = stringPreferencesKey("mobile_mpv_demuxer_max_bytes")
    val mpvDemuxerMaxBackBytesKey = stringPreferencesKey("mobile_mpv_demuxer_max_back_bytes")
    val mpvVdQueueEnableKey = stringPreferencesKey("mobile_mpv_vd_queue_enable")
    val superResolutionTypeKey = intPreferencesKey("mobile_super_resolution_type")
    val audioOutputDevicesKey = stringPreferencesKey("mobile_audio_output_devices")
    val showLiveDanmakuEmojiKey = booleanPreferencesKey("mobile_show_live_danmaku_emoji")
    val incognitoModeKey = booleanPreferencesKey("mobile_incognito_mode")

    val themeTypeRequest = PreferenceRequest(themeTypeKey, ThemeType.Auto.ordinal)
    val themePaletteRequest = PreferenceRequest(themePaletteKey, MobileThemePalette.Default.ordinal)
    val dynamicColorRequest = PreferenceRequest(dynamicColorKey, false)
    val seedColorRequest = PreferenceRequest(seedColorKey, MobilePrefs.DEFAULT_SEED_COLOR)
    val playerTypeRequest = PreferenceRequest(playerTypeKey, PlayerType.Media3.ordinal)
    val apiTypeRequest = PreferenceRequest(apiTypeKey, ApiType.App.ordinal)
    val defaultQualityRequest = PreferenceRequest(defaultQualityKey, Resolution.R1080P.code)
    val defaultCellularQualityRequest = PreferenceRequest(defaultCellularQualityKey, Resolution.R1080P.code)
    val defaultVideoCodecRequest = PreferenceRequest(defaultVideoCodecKey, VideoCodec.HEVC.ordinal)
    val secondVideoCodecRequest = PreferenceRequest(secondVideoCodecKey, VideoCodec.AVC.ordinal)
    val currentPlaySpeedRequest = PreferenceRequest(currentPlaySpeedKey, 1f)
    val autoPlayRequest = PreferenceRequest(autoPlayKey, true)
    val enableTunnelingRequest = PreferenceRequest(enableTunnelingKey, false)
    val enableMobileTunnelingRequest = PreferenceRequest(enableMobileTunnelingKey, false)
    val enableFfmpegAudioRendererRequest = PreferenceRequest(enableFfmpegAudioRendererKey, false)
    val enableAsyncQueueingRequest = PreferenceRequest(enableAsyncQueueingKey, true)
    val enableAudioPlaybackParamsRequest = PreferenceRequest(enableAudioPlaybackParamsKey, true)
    val enableHardwareDecodeRequest = PreferenceRequest(enableHardwareDecodeKey, true)
    val expandBufferRequest = PreferenceRequest(expandBufferKey, false)
    val defaultAudioRequest = PreferenceRequest(defaultAudioKey, Audio.A192K.code)
    val defaultCellularAudioRequest = PreferenceRequest(defaultCellularAudioKey, Audio.A192K.code)
    val defaultDanmakuScaleRequest = PreferenceRequest(defaultDanmakuScaleKey, 1.25f)
    val defaultMobileDanmakuScaleRequest = PreferenceRequest(defaultMobileDanmakuScaleKey, 0.8f)
    val defaultDanmakuOpacityRequest = PreferenceRequest(defaultDanmakuOpacityKey, 0.8f)
    val defaultDanmakuEnabledRequest = PreferenceRequest(defaultDanmakuEnabledKey, true)
    val defaultDanmakuTypesRequest = PreferenceRequest(defaultDanmakuTypesKey, "0,1,2,3")
    val defaultDanmakuAreaRequest = PreferenceRequest(defaultDanmakuAreaKey, 0.5f)
    val defaultDanmakuSpeedModeRequest = PreferenceRequest(
        defaultDanmakuSpeedModeKey,
        DanmakuSpeedMode.FollowVideo.ordinal
    )
    val defaultDanmakuPresentationSpeedRequest =
        PreferenceRequest(defaultDanmakuPresentationSpeedKey, 1f)
    val defaultDanmakuMaskRequest = PreferenceRequest(defaultDanmakuMaskKey, true)
    val defaultDanmakuFilterLevelRequest = PreferenceRequest(defaultDanmakuFilterLevelKey, 1)
    val defaultDanmakuMergeEnabledRequest = PreferenceRequest(defaultDanmakuMergeEnabledKey, true)
    val defaultLiveDanmakuFilterLevelRequest = PreferenceRequest(defaultLiveDanmakuFilterLevelKey, 0)
    val defaultSubtitleFontSizeRequest = PreferenceRequest(defaultSubtitleFontSizeKey, 24)
    val defaultSubtitleBackgroundOpacityRequest = PreferenceRequest(defaultSubtitleBackgroundOpacityKey, 0.4f)
    val defaultSubtitleBottomPaddingRequest = PreferenceRequest(defaultSubtitleBottomPaddingKey, 12)
    val defaultSecondarySubtitleFontSizeRequest = PreferenceRequest(defaultSecondarySubtitleFontSizeKey, 24)
    val defaultSecondarySubtitleBackgroundOpacityRequest = PreferenceRequest(defaultSecondarySubtitleBackgroundOpacityKey, 0.4f)
    val defaultSecondarySubtitleBottomPaddingRequest = PreferenceRequest(defaultSecondarySubtitleBottomPaddingKey, 12)
    val defaultPlayModeRequest = PreferenceRequest(defaultPlayModeKey, PlayMode.Sequential.ordinal)
    val defaultLiveQnRequest = PreferenceRequest(defaultLiveQnKey, LiveQualityPreference.Origin.qn)
    val defaultCellularLiveQnRequest = PreferenceRequest(defaultCellularLiveQnKey, LiveQualityPreference.SuperHD.qn)
    val defaultLiveCodecRequest = PreferenceRequest(defaultLiveCodecKey, LiveCodec.HLS.ordinal)
    val isLoopRequest = PreferenceRequest(isLoopKey, false)
    val showDanmakuRequest = PreferenceRequest(showDanmakuKey, true)
    val enableSponsorBlockRequest = PreferenceRequest(enableSponsorBlockKey, false)
    val sponsorBlockSkipModeRequest = PreferenceRequest(sponsorBlockSkipModeKey, SponsorBlockSkipMode.Auto.value)
    val sponsorBlockApiServerRequest = PreferenceRequest(sponsorBlockApiServerKey, "bsbsb.top")
    val portraitVideoFixModeRequest = PreferenceRequest(portraitVideoFixModeKey, PortraitVideoFixMode.None.value)
    val playerDefaultStartPositionRequest = PreferenceRequest(playerDefaultStartPositionKey, PlayerDefaultStartPosition.History.value)
    val playerShowDebugInfoRequest = PreferenceRequest(playerShowDebugInfoKey, false)
    val liveIncognitoModeRequest = PreferenceRequest(liveIncognitoModeKey, true)
    val preferOfficialCdnRequest = PreferenceRequest(preferOfficialCdnKey, false)
    val cdnServiceRequest = PreferenceRequest(cdnServiceKey, CdnService.BackupUrl.ordinal)
    val liveCdnUrlRequest = PreferenceRequest(liveCdnUrlKey, "")
    val cdnSpeedTestRequest = PreferenceRequest(cdnSpeedTestKey, true)
    val disableAudioCdnRequest = PreferenceRequest(disableAudioCdnKey, false)
    val tryLook1080PRequest = PreferenceRequest(tryLook1080PKey, true)
    val autoSyncRequest = PreferenceRequest(autoSyncKey, "30")
    val videoSyncRequest = PreferenceRequest(videoSyncKey, "display-resample")
    val hardwareDecodeModeRequest = PreferenceRequest(hardwareDecodeModeKey, "auto-safe")
    val mpvHardwareDecodeCodecsRequest =
        PreferenceRequest(mpvHardwareDecodeCodecsKey, "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1")
    val mpvVideoOutputRequest = PreferenceRequest(mpvVideoOutputKey, "gpu")
    val mpvGpuContextRequest = PreferenceRequest(mpvGpuContextKey, "android")
    val mpvGpuApiRequest = PreferenceRequest(mpvGpuApiKey, "")
    val mpvCacheRequest = PreferenceRequest(mpvCacheKey, "yes")
    val mpvDemuxerMaxBytesRequest = PreferenceRequest(mpvDemuxerMaxBytesKey, "150MiB")
    val mpvDemuxerMaxBackBytesRequest = PreferenceRequest(mpvDemuxerMaxBackBytesKey, "50MiB")
    val mpvVdQueueEnableRequest = PreferenceRequest(mpvVdQueueEnableKey, "")
    val superResolutionTypeRequest = PreferenceRequest(superResolutionTypeKey, SuperResolutionType.Disable.value)
    val audioOutputDevicesRequest = PreferenceRequest(audioOutputDevicesKey, "opensles,aaudio,audiotrack")
    val showLiveDanmakuEmojiRequest = PreferenceRequest(showLiveDanmakuEmojiKey, false)
    val incognitoModeRequest = PreferenceRequest(incognitoModeKey, false)
}
