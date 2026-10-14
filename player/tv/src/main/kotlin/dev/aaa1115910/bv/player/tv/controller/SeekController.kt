package dev.aaa1115910.bv.player.tv.controller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import dev.aaa1115910.biliapi.entity.video.VideoShot
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerSeekState
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerSeekThumbData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.VideoPlayerViewPoint
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerVideoShotData
import dev.aaa1115910.bv.player.seekbar.SeekMoveState
import dev.aaa1115910.bv.player.seekbar.buildViewPointTimeline
import dev.aaa1115910.bv.player.seekbar.viewPointAt
import dev.aaa1115910.bv.player.tv.VideoSeekBar
import dev.aaa1115910.bv.player.tv.component.PlayerAnimations
import dev.aaa1115910.bv.player.tv.theme.PlayerColors

@Composable
fun SeekController(
    modifier: Modifier = Modifier,
    show: Boolean,
    goTime: Long,
    moveState: SeekMoveState,
    bottomProgressBarColor: Color = PlayerColors.bottomProgressBar,
) {
    val videoPlayerVideoShotData = LocalVideoPlayerVideoShotData.current
    val videoPlayerSeekState = LocalVideoPlayerSeekState.current
    val videoPlayerSeekThumbData = LocalVideoPlayerSeekThumbData.current
    val videoPlayerConfigData = LocalVideoPlayerConfigData.current

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = show,
            enter = PlayerAnimations.seekEnter,
            exit = PlayerAnimations.seekExit,
            label = "SeekControllerVisible"
        ) {
            SeekController(
                duration = videoPlayerSeekState.duration,
                position = goTime,
                moveState = moveState,
                idleIcon = videoPlayerSeekThumbData.idleIcon,
                movingIcon = videoPlayerSeekThumbData.movingIcon,
                videoShot = videoPlayerVideoShotData.videoShot,
                bottomProgressBarColor = bottomProgressBarColor,
                viewPoints = videoPlayerConfigData.viewPoints,
                bufferedPercentage = videoPlayerSeekState.bufferedPercentage,
            )
        }
    }
}

@Composable
private fun SeekController(
    modifier: Modifier = Modifier,
    duration: Long,
    position: Long,
    moveState: SeekMoveState,
    idleIcon: String,
    movingIcon: String,
    videoShot: VideoShot? = null,
    bottomProgressBarColor: Color = PlayerColors.bottomProgressBar,
    viewPoints: List<VideoPlayerViewPoint> = emptyList(),
    bufferedPercentage: Int = 0,
) {
    val timeline = remember(viewPoints, duration) { buildViewPointTimeline(viewPoints, duration) }
    val chapterTitle = timeline.viewPointAt(position)?.title
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (videoShot != null) {
            VideoShot(
                modifier = Modifier
                    .padding(horizontal = 48.dp),
                videoShot = videoShot,
                position = position,
                duration = duration,
                coercedOffset = (-24).dp
            )
        }

        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            PlayerColors.dialogBackground
                        )
                    )),
            verticalArrangement = Arrangement.Bottom
        ) {
            if (timeline.isNotEmpty()) {
                Text(
                    text = chapterTitle.orEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 8.dp),
                    color = PlayerColors.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            VideoSeekBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 2.dp),
                duration = duration,
                position = position,
                bufferedPercentage = bufferedPercentage,
                moveState = moveState,
                idleIcon = idleIcon,
                movingIcon = movingIcon,
                showPosition = true,
                playedTrackBrush = SolidColor(bottomProgressBarColor),
                viewPoints = viewPoints,
            )
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun SegmentedSeekControllerPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(Modifier.fillMaxSize().background(Color.DarkGray)) {
            SeekController(
                modifier = Modifier.align(Alignment.BottomCenter),
                duration = 600_000L,
                position = 240_000L,
                moveState = SeekMoveState.Forward,
                idleIcon = "",
                movingIcon = "",
                bufferedPercentage = 70,
                viewPoints = listOf(
                    VideoPlayerViewPoint(0, 80, "开场"),
                    VideoPlayerViewPoint(80, 236, "设计与背景"),
                    VideoPlayerViewPoint(236, 500, "从第一代产品到今天：设计理念与关键技术的演进"),
                    VideoPlayerViewPoint(500, 600, "总结"),
                ),
            )
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun VideoProgressSeekPreview(@PreviewParameter(VideoProgressProvider::class) data: Pair<Long, Long>) {
    MaterialTheme {
        SeekController(
            modifier = Modifier
                .background(Color.White),
            duration = data.first,
            position = data.second,
            moveState = SeekMoveState.Idle,
            idleIcon = "",
            movingIcon = "",
            videoShot = VideoShot(
                times = emptyList(),
                imageCountX = 0,
                imageCountY = 0,
                imageWidth = 0,
                imageHeight = 0,
                images = emptyList()
            ),
            bottomProgressBarColor = PlayerColors.bottomProgressBar
        )
    }
}

private class VideoProgressProvider : PreviewParameterProvider<Pair<Long, Long>> {
    override val values = sequenceOf(
        Pair(1234_000L, 0L),
        Pair(1234_000L, 234_000L),
        Pair(1234_000L, 555_000L),
        Pair(1234_000L, 999_000L),
        Pair(1234_000L, 1234_000L),
    )
}
