package dev.aaa1115910.bv.tv.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.aaa1115910.bv.tv.util.requireTvActivity
import dev.aaa1115910.bv.player.entity.DefaultStartPosition
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerDanmakuMasksData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerHistoryData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerLoadStateData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerLogsData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerPaymentData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerSeekThumbData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerVideoInfoData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerVideoShotData
import dev.aaa1115910.bv.player.entity.PlaybackMediaMode
import dev.aaa1115910.bv.player.entity.VideoListItemData
import dev.aaa1115910.bv.player.entity.VideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.VideoPlayerDanmakuMasksData
import dev.aaa1115910.bv.player.entity.VideoPlayerHistoryData
import dev.aaa1115910.bv.player.entity.VideoPlayerLoadStateData
import dev.aaa1115910.bv.player.entity.VideoPlayerLogsData
import dev.aaa1115910.bv.player.entity.VideoPlayerPaymentData
import dev.aaa1115910.bv.player.entity.VideoPlayerSeekThumbData
import dev.aaa1115910.bv.player.entity.VideoPlayerVideoInfoData
import dev.aaa1115910.bv.player.entity.VideoPlayerVideoShotData
import dev.aaa1115910.bv.player.tv.BvPlayer
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.VideoPlayerV3ViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun OfflineVideoPlayerScreen(
    modifier: Modifier = Modifier,
    playerViewModel: VideoPlayerV3ViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val activity = requireTvActivity()
    val scope = rememberCoroutineScope()
    val videoPlayer = playerViewModel.videoPlayer ?: return

    fun playPlaylistOffset(offset: Int) {
        val playlist = playerViewModel.availableVideoList.filterIsInstance<VideoListItemData>()
        val currentIndex = playlist.indexOfFirst { it.cid == playerViewModel.currentCid }
        val target = playlist.getOrNull(currentIndex + offset) ?: return
        val targetCid = target.cid ?: return
        playerViewModel.playOfflinePlaylistItem(target.aid, targetCid)
            .onFailure { (it.localizedMessage ?: "离线视频不可用").toast(context) }
    }

    CompositionLocalProvider(
        LocalVideoPlayerSeekThumbData provides VideoPlayerSeekThumbData(),
        LocalVideoPlayerVideoInfoData provides VideoPlayerVideoInfoData(
            width = playerViewModel.currentVideoWidth,
            height = playerViewModel.currentVideoHeight,
            codec = playerViewModel.currentVideoCodec.name,
            cover = playerViewModel.cover,
            title = playerViewModel.title,
            partTitle = playerViewModel.partTitle,
            danmaku = playerViewModel.danmaku,
            upName = playerViewModel.upName,
            upAvatar = playerViewModel.upFace,
            isVerticalVideo = playerViewModel.isVerticalVideo
        ),
        LocalVideoPlayerLogsData provides VideoPlayerLogsData(logs = playerViewModel.logs),
        LocalVideoPlayerHistoryData provides VideoPlayerHistoryData(
            lastPlayed = 0,
            initialPlaybackPositionMs = playerViewModel.resolvedVodStartPositionMs,
            isInitialPlaybackPositionResolved = playerViewModel.hasResolvedVodStartPosition
        ),
        LocalVideoPlayerPaymentData provides VideoPlayerPaymentData(),
        LocalVideoPlayerLoadStateData provides VideoPlayerLoadStateData(
            loadState = playerViewModel.loadState,
            errorMessage = playerViewModel.errorMessage
        ),
        LocalVideoPlayerConfigData provides VideoPlayerConfigData(
            availableResolutions = playerViewModel.availableQuality,
            availableVideoCodec = playerViewModel.availableVideoCodec,
            availableAudio = playerViewModel.availableAudio,
            availableSubtitleTracks = emptyList(),
            availableVideoList = playerViewModel.availableVideoList,
            currentVideoCid = playerViewModel.currentCid,
            currentResolution = playerViewModel.currentQuality,
            currentVideoCodec = playerViewModel.currentVideoCodec,
            currentVideoAspectRatio = playerViewModel.currentVideoAspectRatio,
            currentVideoRotation = playerViewModel.currentVideoRotation,
            supportManualVideoRotation = false,
            currentVideoSpeed = playerViewModel.currentPlaySpeed,
            currentPlaybackMediaMode = PlaybackMediaMode.Normal,
            currentAudio = playerViewModel.currentAudio,
            currentDanmakuEnabled = playerViewModel.currentDanmakuEnabled,
            currentDanmakuEnabledList = playerViewModel.currentDanmakuTypes,
            currentDanmakuScale = playerViewModel.currentDanmakuScale,
            currentDanmakuOpacity = playerViewModel.currentDanmakuOpacity,
            currentDanmakuArea = playerViewModel.currentDanmakuArea,
            currentDanmakuMask = false,
            danmakuMaskSupported = false,
            currentDanmakuFilterLevel = playerViewModel.currentDanmakuFilterLevel,
            currentDanmakuMergeEnabled = playerViewModel.currentDanmakuMergeEnabled,
            currentDanmakuSpeedMode = playerViewModel.currentDanmakuSpeedMode,
            currentDanmakuPresentationSpeed = playerViewModel.currentDanmakuPresentationSpeed,
            currentPlayMode = playerViewModel.currentPlayMode,
            incognitoMode = true,
            isLoop = playerViewModel.isLoop,
            showDanmaku = playerViewModel.showDanmaku,
            showRelatedVideos = false,
            showNextVideoBtn = playerViewModel.availableVideoList.size > 1,
            defaultStartPosition = DefaultStartPosition.Beginning,
            enableStartPositionSwitch = false,
            clipInfoList = emptyList(),
            skipPgcIntroOutro = false,
            isLive = false
        ),
        LocalVideoPlayerDanmakuMasksData provides VideoPlayerDanmakuMasksData(),
        LocalVideoPlayerVideoShotData provides VideoPlayerVideoShotData()
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            BvPlayer(
                modifier = Modifier.fillMaxSize(),
                videoPlayer = videoPlayer,
                danmakuPlayer = playerViewModel.danmakuPlayer,
                danmakuOpacity = playerViewModel.currentDanmakuOpacity,
                playerSeekForwardStep = Prefs.playerSeekForwardStep,
                playerSeekBackwardStep = Prefs.playerSeekBackwardStep,
                showBottomProgressBar = Prefs.playerShowBottomProgressBar,
                bottomProgressBarColor = Prefs.playerBottomProgressBarColor.toComposeColor(),
                bottomControlPanelConfig = Prefs.playerBottomControlPanelConfig,
                offlinePlaybackMode = true,
                onSendHeartbeat = {},
                onClearBackToHistoryData = {},
                onReloadDanmakuAfterSeek = playerViewModel::reloadDanmakuAfterSeek,
                onDanmakuPlayerBound = playerViewModel::resyncDanmakuAfterPlayerBound,
                onEnsureDanmakuCoverage = playerViewModel::ensureDanmakuCoverage,
                onNearEnd = {},
                onLoadNextVideo = { playPlaylistOffset(1) },
                onLoadPrevVideo = { playPlaylistOffset(-1) },
                onExit = { activity.finish() },
                onLoadNewVideo = { item ->
                    val video = item as? VideoListItemData ?: return@BvPlayer
                    val cid = video.cid ?: return@BvPlayer
                    playerViewModel.playOfflinePlaylistItem(video.aid, cid)
                        .onFailure { (it.localizedMessage ?: "离线视频不可用").toast(context) }
                },
                onResolutionChange = { _, _, afterChange -> scope.launch { afterChange() } },
                onCodecChange = { _, afterChange -> scope.launch { afterChange() } },
                onAspectRatioChange = { playerViewModel.currentVideoAspectRatio = it },
                onRotationChange = { playerViewModel.currentVideoRotation = it },
                onPlaySpeedChange = {
                    Prefs.currentPlaySpeed = it
                    playerViewModel.currentPlaySpeed = it
                },
                onAudioChange = { _, afterChange -> scope.launch { afterChange() } },
                onPlaybackMediaModeChange = { _, _, afterChange -> scope.launch { afterChange() } },
                onDanmakuSwitchChange = { enabledTypes ->
                    playerViewModel.currentDanmakuTypes.clear()
                    playerViewModel.currentDanmakuTypes.addAll(enabledTypes)
                },
                onDanmakuSizeChange = { playerViewModel.currentDanmakuScale = it },
                onDanmakuOpacityChange = { playerViewModel.currentDanmakuOpacity = it },
                onDanmakuAreaChange = { playerViewModel.currentDanmakuArea = it },
                onDanmakuSpeedModeChange = { playerViewModel.currentDanmakuSpeedMode = it },
                onDanmakuPresentationSpeedChange = {
                    playerViewModel.currentDanmakuPresentationSpeed = it
                },
                onDanmakuMaskChange = {},
                onDanmakuMergeChange = { playerViewModel.updateDanmakuMergeEnabled(it) },
                onDanmakuFilterLevelChange = { playerViewModel.currentDanmakuFilterLevel = it },
                onSubtitleChange = {},
                onSubtitleSizeChange = {},
                onSubtitleBackgroundOpacityChange = {},
                onSubtitleBottomPadding = {},
                onSecondarySubtitleChange = {},
                onSecondarySubtitleSizeChange = {},
                onSecondarySubtitleBackgroundOpacityChange = {},
                onSecondarySubtitleBottomPadding = {},
                onPlayModeChange = { playerViewModel.currentPlayMode = it },
                onToggleRelatedVideos = {},
                showRelatedButton = false,
                autoOpenPlayListOnVideoEnd = false,
                onShowDanmakuChange = {
                    Prefs.showDanmaku = it
                    playerViewModel.showDanmaku = it
                },
                onLoopPlayModeChange = {
                    Prefs.isLoop = it
                    playerViewModel.isLoop = it
                },
                onRefreshVideo = { positionMs ->
                    playerViewModel.reloadCurrentOfflinePlayback(positionMs)
                        .onFailure { (it.localizedMessage ?: "重新加载失败").toast(context) }
                },
                shortcutKeyBindings = Prefs.playerShortcutKeyBindings,
                useTripleLikeOnLongPress = false,
                enableSponsorBlock = false,
                isLive = false
            )
        }
    }
}
