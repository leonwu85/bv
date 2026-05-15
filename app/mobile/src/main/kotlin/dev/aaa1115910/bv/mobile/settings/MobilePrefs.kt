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
import dev.aaa1115910.bv.entity.LiveQualityPreference
import dev.aaa1115910.bv.entity.PlayerType
import dev.aaa1115910.bv.entity.ThemeType
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.DanmakuType
import dev.aaa1115910.bv.player.entity.LiveCodec
import dev.aaa1115910.bv.player.entity.PlayMode
import dev.aaa1115910.bv.player.entity.PlayerDefaultStartPosition
import dev.aaa1115910.bv.player.entity.PortraitVideoFixMode
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.SponsorBlockSkipMode
import dev.aaa1115910.bv.player.entity.VideoCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

object MobilePrefs {
    private val dsm get() = BVApp.dataStoreManager

    const val DEFAULT_SEED_COLOR = 0xFF5CB67B.toInt()

    var themeType: ThemeType
        get() = ThemeType.entries[read(MobilePrefKeys.themeTypeRequest)]
        set(value) = write(MobilePrefKeys.themeTypeKey, value.ordinal)

    val themeTypeFlow: Flow<ThemeType>
        get() = dsm.getPreferenceFlow(MobilePrefKeys.themeTypeRequest)
            .transform { emit(ThemeType.entries[it]) }

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
        get() = PlayerType.entries[read(MobilePrefKeys.playerTypeRequest)]
        set(value) = write(MobilePrefKeys.playerTypeKey, value.ordinal)

    var apiType: ApiType
        get() = ApiType.entries[read(MobilePrefKeys.apiTypeRequest)]
        set(value) = write(MobilePrefKeys.apiTypeKey, value.ordinal)

    var defaultQuality: Resolution
        get() = Resolution.fromCode(read(MobilePrefKeys.defaultQualityRequest)) ?: Resolution.R1080P
        set(value) = write(MobilePrefKeys.defaultQualityKey, value.code)

    var defaultVideoCodec: VideoCodec
        get() = VideoCodec.fromCode(read(MobilePrefKeys.defaultVideoCodecRequest))
        set(value) = write(MobilePrefKeys.defaultVideoCodecKey, value.ordinal)

    var currentPlaySpeed: Float
        get() = read(MobilePrefKeys.currentPlaySpeedRequest)
        set(value) = write(MobilePrefKeys.currentPlaySpeedKey, value)

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

    var defaultAudio: Audio
        get() = Audio.fromCode(read(MobilePrefKeys.defaultAudioRequest)) ?: Audio.A192K
        set(value) = write(MobilePrefKeys.defaultAudioKey, value.code)

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

    var defaultDanmakuMask: Boolean
        get() = read(MobilePrefKeys.defaultDanmakuMaskRequest)
        set(value) = write(MobilePrefKeys.defaultDanmakuMaskKey, value)

    var defaultDanmakuFilterLevel: Int
        get() = read(MobilePrefKeys.defaultDanmakuFilterLevelRequest)
        set(value) = write(MobilePrefKeys.defaultDanmakuFilterLevelKey, value)

    var defaultDanmakuMergeEnabled: Boolean
        get() = read(MobilePrefKeys.defaultDanmakuMergeEnabledRequest)
        set(value) = write(MobilePrefKeys.defaultDanmakuMergeEnabledKey, value)

    var defaultLiveDanmakuFilterLevel: Int
        get() = read(MobilePrefKeys.defaultLiveDanmakuFilterLevelRequest)
        set(value) = write(MobilePrefKeys.defaultLiveDanmakuFilterLevelKey, value)

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

    var showLiveDanmakuEmoji: Boolean
        get() = read(MobilePrefKeys.showLiveDanmakuEmojiRequest)
        set(value) = write(MobilePrefKeys.showLiveDanmakuEmojiKey, value)

    var incognitoMode: Boolean
        get() = read(MobilePrefKeys.incognitoModeRequest)
        set(value) = write(MobilePrefKeys.incognitoModeKey, value)

    private fun <T> read(request: PreferenceRequest<T>): T =
        runBlocking { dsm.getPreferenceFlow(request).first() }

    private fun <T> write(key: androidx.datastore.preferences.core.Preferences.Key<T>, value: T) {
        runBlocking { dsm.editPreference(key, value) }
    }
}

object MobilePrefKeys {
    val themeTypeKey = intPreferencesKey("mobile_theme_type")
    val dynamicColorKey = booleanPreferencesKey("mobile_theme_dynamic_color")
    val seedColorKey = intPreferencesKey("mobile_theme_seed_color")
    val playerTypeKey = intPreferencesKey("mobile_player_type")
    val apiTypeKey = intPreferencesKey("mobile_api_type")
    val defaultQualityKey = intPreferencesKey("mobile_default_quality")
    val defaultVideoCodecKey = intPreferencesKey("mobile_default_video_codec")
    val currentPlaySpeedKey = floatPreferencesKey("mobile_current_play_speed")
    val enableTunnelingKey = booleanPreferencesKey("mobile_enable_tunneling")
    val enableMobileTunnelingKey = booleanPreferencesKey("mobile_enable_mobile_tunneling")
    val enableFfmpegAudioRendererKey = booleanPreferencesKey("mobile_enable_ffmpeg_audio_renderer")
    val enableAsyncQueueingKey = booleanPreferencesKey("mobile_enable_async_queueing")
    val enableAudioPlaybackParamsKey = booleanPreferencesKey("mobile_enable_audio_playback_params")
    val defaultAudioKey = intPreferencesKey("mobile_default_audio")
    val defaultDanmakuScaleKey = floatPreferencesKey("mobile_default_danmaku_scale")
    val defaultMobileDanmakuScaleKey = floatPreferencesKey("mobile_default_mobile_danmaku_scale")
    val defaultDanmakuOpacityKey = floatPreferencesKey("mobile_default_danmaku_opacity")
    val defaultDanmakuEnabledKey = booleanPreferencesKey("mobile_default_danmaku_enabled")
    val defaultDanmakuTypesKey = stringPreferencesKey("mobile_default_danmaku_types")
    val defaultDanmakuAreaKey = floatPreferencesKey("mobile_default_danmaku_area")
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
    val showLiveDanmakuEmojiKey = booleanPreferencesKey("mobile_show_live_danmaku_emoji")
    val incognitoModeKey = booleanPreferencesKey("mobile_incognito_mode")

    val themeTypeRequest = PreferenceRequest(themeTypeKey, ThemeType.Auto.ordinal)
    val dynamicColorRequest = PreferenceRequest(dynamicColorKey, false)
    val seedColorRequest = PreferenceRequest(seedColorKey, MobilePrefs.DEFAULT_SEED_COLOR)
    val playerTypeRequest = PreferenceRequest(playerTypeKey, PlayerType.Media3.ordinal)
    val apiTypeRequest = PreferenceRequest(apiTypeKey, ApiType.App.ordinal)
    val defaultQualityRequest = PreferenceRequest(defaultQualityKey, Resolution.R1080P.code)
    val defaultVideoCodecRequest = PreferenceRequest(defaultVideoCodecKey, VideoCodec.HEVC.ordinal)
    val currentPlaySpeedRequest = PreferenceRequest(currentPlaySpeedKey, 1f)
    val enableTunnelingRequest = PreferenceRequest(enableTunnelingKey, false)
    val enableMobileTunnelingRequest = PreferenceRequest(enableMobileTunnelingKey, false)
    val enableFfmpegAudioRendererRequest = PreferenceRequest(enableFfmpegAudioRendererKey, false)
    val enableAsyncQueueingRequest = PreferenceRequest(enableAsyncQueueingKey, true)
    val enableAudioPlaybackParamsRequest = PreferenceRequest(enableAudioPlaybackParamsKey, true)
    val defaultAudioRequest = PreferenceRequest(defaultAudioKey, Audio.A192K.code)
    val defaultDanmakuScaleRequest = PreferenceRequest(defaultDanmakuScaleKey, 1.25f)
    val defaultMobileDanmakuScaleRequest = PreferenceRequest(defaultMobileDanmakuScaleKey, 0.8f)
    val defaultDanmakuOpacityRequest = PreferenceRequest(defaultDanmakuOpacityKey, 0.8f)
    val defaultDanmakuEnabledRequest = PreferenceRequest(defaultDanmakuEnabledKey, true)
    val defaultDanmakuTypesRequest = PreferenceRequest(defaultDanmakuTypesKey, "0,1,2,3")
    val defaultDanmakuAreaRequest = PreferenceRequest(defaultDanmakuAreaKey, 0.5f)
    val defaultDanmakuMaskRequest = PreferenceRequest(defaultDanmakuMaskKey, true)
    val defaultDanmakuFilterLevelRequest = PreferenceRequest(defaultDanmakuFilterLevelKey, 0)
    val defaultDanmakuMergeEnabledRequest = PreferenceRequest(defaultDanmakuMergeEnabledKey, false)
    val defaultLiveDanmakuFilterLevelRequest = PreferenceRequest(defaultLiveDanmakuFilterLevelKey, 0)
    val defaultSubtitleFontSizeRequest = PreferenceRequest(defaultSubtitleFontSizeKey, 24)
    val defaultSubtitleBackgroundOpacityRequest = PreferenceRequest(defaultSubtitleBackgroundOpacityKey, 0.4f)
    val defaultSubtitleBottomPaddingRequest = PreferenceRequest(defaultSubtitleBottomPaddingKey, 12)
    val defaultSecondarySubtitleFontSizeRequest = PreferenceRequest(defaultSecondarySubtitleFontSizeKey, 24)
    val defaultSecondarySubtitleBackgroundOpacityRequest = PreferenceRequest(defaultSecondarySubtitleBackgroundOpacityKey, 0.4f)
    val defaultSecondarySubtitleBottomPaddingRequest = PreferenceRequest(defaultSecondarySubtitleBottomPaddingKey, 12)
    val defaultPlayModeRequest = PreferenceRequest(defaultPlayModeKey, PlayMode.Sequential.ordinal)
    val defaultLiveQnRequest = PreferenceRequest(defaultLiveQnKey, LiveQualityPreference.Origin.qn)
    val defaultLiveCodecRequest = PreferenceRequest(defaultLiveCodecKey, LiveCodec.FLV.ordinal)
    val isLoopRequest = PreferenceRequest(isLoopKey, false)
    val showDanmakuRequest = PreferenceRequest(showDanmakuKey, true)
    val enableSponsorBlockRequest = PreferenceRequest(enableSponsorBlockKey, false)
    val sponsorBlockSkipModeRequest = PreferenceRequest(sponsorBlockSkipModeKey, SponsorBlockSkipMode.Manual.value)
    val sponsorBlockApiServerRequest = PreferenceRequest(sponsorBlockApiServerKey, "https://sponsor.ajay.app")
    val portraitVideoFixModeRequest = PreferenceRequest(portraitVideoFixModeKey, PortraitVideoFixMode.None.value)
    val playerDefaultStartPositionRequest = PreferenceRequest(playerDefaultStartPositionKey, PlayerDefaultStartPosition.History.value)
    val playerShowDebugInfoRequest = PreferenceRequest(playerShowDebugInfoKey, false)
    val liveIncognitoModeRequest = PreferenceRequest(liveIncognitoModeKey, true)
    val preferOfficialCdnRequest = PreferenceRequest(preferOfficialCdnKey, false)
    val showLiveDanmakuEmojiRequest = PreferenceRequest(showLiveDanmakuEmojiKey, false)
    val incognitoModeRequest = PreferenceRequest(incognitoModeKey, false)
}
