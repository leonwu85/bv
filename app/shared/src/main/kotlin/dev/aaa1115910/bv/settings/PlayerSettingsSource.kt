package dev.aaa1115910.bv.settings

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.bv.entity.CdnService
import dev.aaa1115910.bv.entity.PlayerType
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.DanmakuType
import dev.aaa1115910.bv.player.entity.LiveCodec
import dev.aaa1115910.bv.player.entity.PlayMode
import dev.aaa1115910.bv.player.entity.PlayerDefaultStartPosition
import dev.aaa1115910.bv.player.entity.PortraitVideoFixMode
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.SponsorBlockSkipMode
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.util.Prefs

interface PlayerSettingsSource {
    val apiType: ApiType
    val playerType: PlayerType
    val defaultQuality: Resolution
    val defaultCellularQuality: Resolution
    val defaultVideoCodec: VideoCodec
    val secondVideoCodec: VideoCodec
    val currentPlaySpeed: Float
    val enableTunneling: Boolean
    val enableMobileTunneling: Boolean
    val enableFfmpegAudioRenderer: Boolean
    val enableAsyncQueueing: Boolean
    val enableAudioPlaybackParams: Boolean
    val enableHardwareDecode: Boolean
    val expandBuffer: Boolean
    val defaultAudio: Audio
    val defaultCellularAudio: Audio
    val defaultDanmakuScale: Float
    val defaultMobileDanmakuScale: Float
    var defaultMobileDanmakuScaleMutable: Float
    val defaultDanmakuOpacity: Float
    var defaultDanmakuOpacityMutable: Float
    val defaultDanmakuEnabled: Boolean
    var defaultDanmakuEnabledMutable: Boolean
    val defaultDanmakuTypes: List<DanmakuType>
    val defaultDanmakuArea: Float
    var defaultDanmakuAreaMutable: Float
    val defaultDanmakuMask: Boolean
    val defaultDanmakuFilterLevel: Int
    val defaultDanmakuMergeEnabled: Boolean
    val defaultLiveDanmakuFilterLevel: Int
    val defaultSubtitleFontSize: TextUnit
    val defaultSubtitleBackgroundOpacity: Float
    val defaultSubtitleBottomPadding: Dp
    val defaultSecondarySubtitleFontSize: TextUnit
    val defaultSecondarySubtitleBackgroundOpacity: Float
    val defaultSecondarySubtitleBottomPadding: Dp
    val defaultPlayMode: PlayMode
    var defaultPlayModeMutable: PlayMode
    val defaultLiveQn: Int
    val defaultCellularLiveQn: Int
    val defaultLiveCodec: LiveCodec
    val isLoop: Boolean
    val showDanmaku: Boolean
    val enableSponsorBlock: Boolean
    val sponsorBlockSkipMode: SponsorBlockSkipMode
    val sponsorBlockApiServer: String
    val portraitVideoFixMode: PortraitVideoFixMode
    val playerDefaultStartPosition: PlayerDefaultStartPosition
    val playerShowDebugInfo: Boolean
    val liveIncognitoMode: Boolean
    val preferOfficialCdn: Boolean
    val cdnService: CdnService
    val liveCdnUrl: String
    val cdnSpeedTest: Boolean
    val disableAudioCdn: Boolean
    val tryLook1080P: Boolean
    val autoSync: String
    val videoSync: String
    val hardwareDecodeMode: String
    val audioOutputDevices: String
    val showLiveDanmakuEmoji: Boolean
    val incognitoMode: Boolean
}

object DefaultPlayerSettingsSource : PlayerSettingsSource {
    override val apiType get() = Prefs.apiType
    override val playerType get() = Prefs.playerType
    override val defaultQuality get() = Prefs.defaultQuality
    override val defaultCellularQuality get() = Prefs.defaultQuality
    override val defaultVideoCodec get() = Prefs.defaultVideoCodec
    override val secondVideoCodec get() = VideoCodec.AVC
    override val currentPlaySpeed get() = Prefs.currentPlaySpeed
    override val enableTunneling get() = Prefs.enableTunneling
    override val enableMobileTunneling get() = Prefs.enableMobileTunneling
    override val enableFfmpegAudioRenderer get() = Prefs.enableFfmpegAudioRenderer
    override val enableAsyncQueueing get() = Prefs.enableAsyncQueueing
    override val enableAudioPlaybackParams get() = Prefs.enableAudioPlaybackParams
    override val enableHardwareDecode get() = true
    override val expandBuffer get() = false
    override val defaultAudio get() = Prefs.defaultAudio
    override val defaultCellularAudio get() = Prefs.defaultAudio
    override val defaultDanmakuScale get() = Prefs.defaultDanmakuScale
    override val defaultMobileDanmakuScale get() = Prefs.defaultMobileDanmakuScale
    override var defaultMobileDanmakuScaleMutable: Float
        get() = Prefs.defaultMobileDanmakuScale
        set(value) {
            Prefs.defaultMobileDanmakuScale = value
        }
    override val defaultDanmakuOpacity get() = Prefs.defaultDanmakuOpacity
    override var defaultDanmakuOpacityMutable: Float
        get() = Prefs.defaultDanmakuOpacity
        set(value) {
            Prefs.defaultDanmakuOpacity = value
        }
    override val defaultDanmakuEnabled get() = Prefs.defaultDanmakuEnabled
    override var defaultDanmakuEnabledMutable: Boolean
        get() = Prefs.defaultDanmakuEnabled
        set(value) {
            Prefs.defaultDanmakuEnabled = value
        }
    override val defaultDanmakuTypes get() = Prefs.defaultDanmakuTypes
    override val defaultDanmakuArea get() = Prefs.defaultDanmakuArea
    override var defaultDanmakuAreaMutable: Float
        get() = Prefs.defaultDanmakuArea
        set(value) {
            Prefs.defaultDanmakuArea = value
        }
    override val defaultDanmakuMask get() = Prefs.defaultDanmakuMask
    override val defaultDanmakuFilterLevel get() = Prefs.defaultDanmakuFilterLevel
    override val defaultDanmakuMergeEnabled get() = Prefs.defaultDanmakuMergeEnabled
    override val defaultLiveDanmakuFilterLevel get() = Prefs.defaultLiveDanmakuFilterLevel
    override val defaultSubtitleFontSize get() = Prefs.defaultSubtitleFontSize
    override val defaultSubtitleBackgroundOpacity get() = Prefs.defaultSubtitleBackgroundOpacity
    override val defaultSubtitleBottomPadding get() = Prefs.defaultSubtitleBottomPadding
    override val defaultSecondarySubtitleFontSize get() = Prefs.defaultSecondarySubtitleFontSize
    override val defaultSecondarySubtitleBackgroundOpacity get() = Prefs.defaultSecondarySubtitleBackgroundOpacity
    override val defaultSecondarySubtitleBottomPadding get() = Prefs.defaultSecondarySubtitleBottomPadding
    override val defaultPlayMode get() = Prefs.defaultPlayMode
    override var defaultPlayModeMutable: PlayMode
        get() = Prefs.defaultPlayMode
        set(value) {
            Prefs.defaultPlayMode = value
        }
    override val defaultLiveQn get() = Prefs.defaultLiveQn
    override val defaultCellularLiveQn get() = Prefs.defaultLiveQn
    override val defaultLiveCodec get() = Prefs.defaultLiveCodec
    override val isLoop get() = Prefs.isLoop
    override val showDanmaku get() = Prefs.showDanmaku
    override val enableSponsorBlock get() = Prefs.enableSponsorBlock
    override val sponsorBlockSkipMode get() = Prefs.sponsorBlockSkipMode
    override val sponsorBlockApiServer get() = Prefs.sponsorBlockApiServer
    override val portraitVideoFixMode get() = Prefs.portraitVideoFixMode
    override val playerDefaultStartPosition get() = Prefs.playerDefaultStartPosition
    override val playerShowDebugInfo get() = Prefs.playerShowDebugInfo
    override val liveIncognitoMode get() = Prefs.liveIncognitoMode
    override val preferOfficialCdn get() = Prefs.preferOfficialCdn
    override val cdnService get() = Prefs.cdnService
    override val liveCdnUrl get() = ""
    override val cdnSpeedTest get() = true
    override val disableAudioCdn get() = false
    override val tryLook1080P get() = false
    override val autoSync get() = ""
    override val videoSync get() = "audio"
    override val hardwareDecodeMode get() = "auto-safe"
    override val audioOutputDevices get() = ""
    override val showLiveDanmakuEmoji get() = Prefs.showLiveDanmakuEmoji
    override val incognitoMode get() = Prefs.incognitoMode
}

object PlayerSettingsProvider {
    var current: PlayerSettingsSource = DefaultPlayerSettingsSource
}
