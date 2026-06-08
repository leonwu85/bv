package dev.aaa1115910.bv.mobile.settings

import dev.aaa1115910.bv.settings.PlayerSettingsProvider
import dev.aaa1115910.bv.settings.PlayerSettingsSource

object MobilePlayerSettingsSource : PlayerSettingsSource {
    override val apiType get() = MobilePrefs.apiType
    override val playerType get() = MobilePrefs.playerType
    override val defaultQuality get() = MobilePrefs.defaultQuality
    override val defaultCellularQuality get() = MobilePrefs.defaultCellularQuality
    override val defaultVideoCodec get() = MobilePrefs.defaultVideoCodec
    override val secondVideoCodec get() = MobilePrefs.secondVideoCodec
    override val currentPlaySpeed get() = MobilePrefs.currentPlaySpeed
    override val autoPlay get() = MobilePrefs.autoPlay
    override val enableTunneling get() = MobilePrefs.enableTunneling
    override val enableMobileTunneling get() = MobilePrefs.enableMobileTunneling
    override val enableFfmpegAudioRenderer get() = MobilePrefs.enableFfmpegAudioRenderer
    override val enableAsyncQueueing get() = MobilePrefs.enableAsyncQueueing
    override val enableAudioPlaybackParams get() = MobilePrefs.enableAudioPlaybackParams
    override val enableHardwareDecode get() = MobilePrefs.enableHardwareDecode
    override val expandBuffer get() = MobilePrefs.expandBuffer
    override val defaultAudio get() = MobilePrefs.defaultAudio
    override val defaultCellularAudio get() = MobilePrefs.defaultCellularAudio
    override val defaultDanmakuScale get() = MobilePrefs.defaultDanmakuScale
    override val defaultMobileDanmakuScale get() = MobilePrefs.defaultMobileDanmakuScale
    override var defaultMobileDanmakuScaleMutable
        get() = MobilePrefs.defaultMobileDanmakuScale
        set(value) {
            MobilePrefs.defaultMobileDanmakuScale = value
        }
    override val defaultDanmakuOpacity get() = MobilePrefs.defaultDanmakuOpacity
    override var defaultDanmakuOpacityMutable
        get() = MobilePrefs.defaultDanmakuOpacity
        set(value) {
            MobilePrefs.defaultDanmakuOpacity = value
        }
    override val defaultDanmakuEnabled get() = MobilePrefs.defaultDanmakuEnabled
    override var defaultDanmakuEnabledMutable
        get() = MobilePrefs.defaultDanmakuEnabled
        set(value) {
            MobilePrefs.defaultDanmakuEnabled = value
        }
    override val defaultDanmakuTypes get() = MobilePrefs.defaultDanmakuTypes
    override val defaultDanmakuArea get() = MobilePrefs.defaultDanmakuArea
    override var defaultDanmakuAreaMutable
        get() = MobilePrefs.defaultDanmakuArea
        set(value) {
            MobilePrefs.defaultDanmakuArea = value
        }
    override val defaultDanmakuMask get() = MobilePrefs.defaultDanmakuMask
    override val defaultDanmakuFilterLevel get() = MobilePrefs.defaultDanmakuFilterLevel
    override val defaultDanmakuMergeEnabled get() = MobilePrefs.defaultDanmakuMergeEnabled
    override val defaultLiveDanmakuFilterLevel get() = MobilePrefs.defaultLiveDanmakuFilterLevel
    override val defaultSubtitleFontSize get() = MobilePrefs.defaultSubtitleFontSize
    override val defaultSubtitleBackgroundOpacity get() = MobilePrefs.defaultSubtitleBackgroundOpacity
    override val defaultSubtitleBottomPadding get() = MobilePrefs.defaultSubtitleBottomPadding
    override val defaultSecondarySubtitleFontSize get() = MobilePrefs.defaultSecondarySubtitleFontSize
    override val defaultSecondarySubtitleBackgroundOpacity get() = MobilePrefs.defaultSecondarySubtitleBackgroundOpacity
    override val defaultSecondarySubtitleBottomPadding get() = MobilePrefs.defaultSecondarySubtitleBottomPadding
    override val subtitleSmartDisplay get() = false
    override val defaultPlayMode get() = MobilePrefs.defaultPlayMode
    override var defaultPlayModeMutable
        get() = MobilePrefs.defaultPlayMode
        set(value) {
            MobilePrefs.defaultPlayMode = value
        }
    override val defaultLiveQn get() = MobilePrefs.defaultLiveQn
    override val defaultCellularLiveQn get() = MobilePrefs.defaultCellularLiveQn
    override val defaultLiveCodec get() = MobilePrefs.defaultLiveCodec
    override val isLoop get() = MobilePrefs.isLoop
    override val showDanmaku get() = MobilePrefs.showDanmaku
    override val enableSponsorBlock get() = MobilePrefs.enableSponsorBlock
    override val sponsorBlockSkipMode get() = MobilePrefs.sponsorBlockSkipMode
    override val sponsorBlockApiServer get() = MobilePrefs.sponsorBlockApiServer
    override val portraitVideoFixMode get() = MobilePrefs.portraitVideoFixMode
    override val playerDefaultStartPosition get() = MobilePrefs.playerDefaultStartPosition
    override val playerShowDebugInfo get() = MobilePrefs.playerShowDebugInfo
    override val liveIncognitoMode get() = MobilePrefs.liveIncognitoMode
    override val preferOfficialCdn get() = MobilePrefs.preferOfficialCdn
    override val cdnService get() = MobilePrefs.cdnService
    override val liveCdnUrl get() = MobilePrefs.liveCdnUrl
    override val cdnSpeedTest get() = MobilePrefs.cdnSpeedTest
    override val disableAudioCdn get() = MobilePrefs.disableAudioCdn
    override val tryLook1080P get() = MobilePrefs.tryLook1080P
    override val autoSync get() = MobilePrefs.autoSync
    override val videoSync get() = MobilePrefs.videoSync
    override val hardwareDecodeMode get() = MobilePrefs.hardwareDecodeMode
    override val superResolutionType get() = MobilePrefs.superResolutionType
    override val audioOutputDevices get() = MobilePrefs.audioOutputDevices
    override val showLiveDanmakuEmoji get() = MobilePrefs.showLiveDanmakuEmoji
    override val incognitoMode get() = MobilePrefs.incognitoMode
}

object MobileRuntime {
    fun install() {
        MobilePrefs.sanitizePlayerType()
        PlayerSettingsProvider.current = MobilePlayerSettingsSource
    }
}
