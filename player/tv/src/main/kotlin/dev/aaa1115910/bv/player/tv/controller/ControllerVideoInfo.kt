package dev.aaa1115910.bv.player.tv.controller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerClockState
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerSeekState
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerSeekThumbData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerStateData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerVideoInfoData
import dev.aaa1115910.bv.player.entity.VideoPlayerClockState
import dev.aaa1115910.bv.player.entity.VideoPlayerSeekThumbData
import dev.aaa1115910.bv.player.entity.VideoPlayerVideoInfoData
import dev.aaa1115910.bv.player.entity.VideoRotation
import dev.aaa1115910.bv.player.tv.component.PlayerAnimations
import dev.aaa1115910.bv.player.tv.theme.PlayerColors

@Composable
fun ControllerVideoInfo(
    modifier: Modifier = Modifier,
    show: Boolean,
    playSpeed: Float = 1f,
    onInteraction: () -> Unit = {},
    onHideInfo: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onPlaySpeedChange: (Float) -> Unit,
    isAudioOnly: Boolean = false,
    onTogglePlaybackMediaMode: () -> Unit = {},
    onOpenUpSpace: () -> Unit,
    onRefreshVideo: () -> Unit,
    onOpenDanmaku: () -> Unit,
    onHideDanmaku: () -> Unit,
    onOpenPlayList: () -> Unit,
    onOpenRelatedVideo: () -> Unit,
    onOpenSetting: () -> Unit,
    onLoopPlayModeChange: (Boolean) -> Unit,
    onRotationChange: (VideoRotation) -> Unit,
    userActionContent: @Composable (
        modifier: Modifier,
        focusMap: Map<String, FocusRequester>,
        onFocus: (String) -> Unit,
        onPauseAutoHide: (Boolean) -> Unit
    ) -> Unit = { _, _, _, _ -> },
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSubtitleChange: (Subtitle) -> Unit,
    onLoadNextVideo: (Boolean) -> Unit,
    onLoadPrevVideo: () -> Unit = {},
    onShowComment: () -> Unit = {},
    onShowDescription: () -> Unit = {},
    onTripleLike: () -> Unit = {},
    onToggleFollow: () -> Unit = {},
    onReportLiveHistory: () -> Unit = {},
    liveIncognitoMode: Boolean = false,
) {
    val videoPlayerClockState = LocalVideoPlayerClockState.current
    val videoPlayerSeekState = LocalVideoPlayerSeekState.current
    val videoPlayerSeekThumbData = LocalVideoPlayerSeekThumbData.current
    val videoPlayerVideoInfoData = LocalVideoPlayerVideoInfoData.current
    val videoPlayerStateData = LocalVideoPlayerStateData.current
    val videoPlayerConfigData = LocalVideoPlayerConfigData.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent {
                if (show && it.type == KeyEventType.KeyDown) {
                    onInteraction()
                }
                false
            }
    ) {
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.TopEnd),
            visible = show,
            enter = PlayerAnimations.controllerEnter,
            exit = PlayerAnimations.controllerExit,
            label = "ControllerTopVideoInfo"
        ) {
            ControllerTopBar(
                clock = Triple(
                    videoPlayerClockState.hour,
                    videoPlayerClockState.minute,
                    videoPlayerClockState.second
                )
            )
        }
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = show,
            enter = PlayerAnimations.controllerEnter,
            exit = PlayerAnimations.controllerExit,
            label = "ControllerBottomVideoInfo"
        ) {
            ControllerBottomBar(
                show = show,
                onHideInfo = onHideInfo,
                seekData = videoPlayerSeekState,
                stateData = videoPlayerStateData,
                title = videoPlayerVideoInfoData.title,
                partTitle = videoPlayerVideoInfoData.partTitle,
                playSpeed = playSpeed,
                rotation = videoPlayerConfigData.currentVideoRotation,
                idleIcon = videoPlayerSeekThumbData.idleIcon,
                movingIcon = videoPlayerSeekThumbData.movingIcon,
                play = videoPlayerVideoInfoData.play,
                danmaku = videoPlayerVideoInfoData.danmaku,
                like = videoPlayerVideoInfoData.like,
                coin = videoPlayerVideoInfoData.coin,
                favorite = videoPlayerVideoInfoData.favorite,
                upName = videoPlayerVideoInfoData.upName,
                upAvatar = videoPlayerVideoInfoData.upAvatar,
                pubTime = videoPlayerVideoInfoData.pubTime,
                isPlaying = videoPlayerStateData.isPlaying || videoPlayerStateData.isBuffering,
                isLoop = videoPlayerConfigData.isLoop,
                showDanmaku = videoPlayerConfigData.showDanmaku,
                onPlay = onPlay,
                onPause = onPause,
                onPlaySpeedChange = onPlaySpeedChange,
                isAudioOnly = isAudioOnly,
                onTogglePlaybackMediaMode = onTogglePlaybackMediaMode,
                onOpenUpSpace = onOpenUpSpace,
                onRefreshVideo = onRefreshVideo,
                onOpenDanmaku = onOpenDanmaku,
                onHideDanmaku = onHideDanmaku,
                onOpenPlayList = onOpenPlayList,
                onOpenRelatedVideo = onOpenRelatedVideo,
                onOpenSetting = onOpenSetting,
                onLoopPlayModeChange = onLoopPlayModeChange,
                onRotationChange = onRotationChange,
                fromSeason = videoPlayerVideoInfoData.fromSeason,
                userActionContent = userActionContent,
                onSeekBack = onSeekBack,
                onSeekForward = onSeekForward,
                availableSubtitleTracks = videoPlayerConfigData.availableSubtitleTracks,
                currentSubtitleId = videoPlayerConfigData.currentSubtitleId,
                onSubtitleChange = { id ->
                    val track = videoPlayerConfigData.availableSubtitleTracks.firstOrNull { it.id == id }
                    track?.let { onSubtitleChange(it) }
                },
                isFollowingUp = videoPlayerVideoInfoData.isFollowingUp,
                onLoadNextVideo = onLoadNextVideo,
                onLoadPrevVideo = onLoadPrevVideo,
                onShowComment = onShowComment,
                onShowDescription = onShowDescription,
                onTripleLike = onTripleLike,
                onToggleFollow = onToggleFollow,
                onReportLiveHistory = onReportLiveHistory,
                liveIncognitoMode = liveIncognitoMode
            )
        }
    }
}

data class ControlButton(
    val id: String,
    val icon: ImageVector? = null,
    val text: String? = null,
    val onClick: () -> Unit,
    val visible: Boolean = true,
    val scale: Float = 1f,
    val painterId: Int? = null,
    val tint: Color = Color.White.copy(alpha = 0.8f),
    val selected: Boolean = false,
    val width: Int? = null,
    val alwaysShowBorder: Boolean = false,
    val fontWeight: FontWeight? = null,
)

@Preview(device = "id:tv_1080p")
@Composable
private fun ControllerVideoInfoPreview() {
    var show by remember { mutableStateOf(true) }

    val clockState = VideoPlayerClockState(hour = 12, minute = 30, second = 30)
    CompositionLocalProvider(
        LocalVideoPlayerVideoInfoData provides VideoPlayerVideoInfoData(
            title = "【A320】民航史上最佳逆袭！A320的前世今生！",
            partTitle = "2023车队车手介绍分析预测",
            upName = "upName",
            play = 1,
            danmaku = 1,
            pubTime = "2025-08-05"
        ),
        LocalVideoPlayerClockState provides clockState,
        LocalVideoPlayerSeekThumbData provides VideoPlayerSeekThumbData(
            idleIcon = "",
            movingIcon = ""
        )
    ) {
        MaterialTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
            )
            ControllerVideoInfo(
                modifier = Modifier.fillMaxSize(),
                show = show,
                playSpeed = 1.25f,
                onHideInfo = {},
                onPlay = {},
                onPause = {},
                onPlaySpeedChange = {},
                onOpenUpSpace = {},
                onRefreshVideo = {},
                onOpenDanmaku = {},
                onHideDanmaku = {},
                onOpenPlayList = {},
                onOpenRelatedVideo = {},
                onOpenSetting = {},
                onLoopPlayModeChange = {},
                onRotationChange = {},
                onSeekBack = {},
                onSeekForward = {},
                onSubtitleChange = {},
                onLoadNextVideo = {}
            )
        }
    }
}
