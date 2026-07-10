package dev.aaa1115910.bv.player.tv.controller

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.bv.player.entity.LiveStreamLine
import dev.aaa1115910.bv.player.entity.VideoRotation
import dev.aaa1115910.bv.player.shared.R
import dev.aaa1115910.bv.player.tv.theme.PlayerColors
import dev.aaa1115910.bv.player.tv.LocalTvUiSurfaceEmbedded
import dev.aaa1115910.bv.util.requestFocus
import kotlinx.coroutines.delay

@Composable
internal fun SpeedDialog(
    modifier: Modifier = Modifier,
    onHideDialog: () -> Unit,
    speed: Float,
    step: Float = 0.25f,
    min: Float = 0.25f,
    max: Float = 3f,
    onSpeedChange: (Float) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    fun touch() {
        lastInteractionTime = System.currentTimeMillis()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus(scope)
    }

    LaunchedEffect(lastInteractionTime) {
        val base = lastInteractionTime
        delay(15000)
        if (base == lastInteractionTime) onHideDialog()
    }

    PlayerOverlayDialog(onDismissRequest = onHideDialog) {
        Surface(
            modifier = modifier.width(240.dp),
            color = PlayerColors.dialogBackground,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "播放速度",
                    color = PlayerColors.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp
                )

                Column(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .focusable()
                        .fillMaxWidth()
                        .onPreviewKeyEvent {
                            if (it.key == Key.DirectionUp || it.key == Key.DirectionDown || it.key == Key.DirectionLeft || it.key == Key.DirectionRight) {
                                if (it.type == KeyEventType.KeyDown) {
                                    touch()
                                    var newValue =
                                        if (it.key == Key.DirectionUp || it.key == Key.DirectionRight)
                                            speed + step
                                        else
                                            speed - step
                                    if (newValue < min) newValue = min
                                    if (newValue > max) newValue = max
                                    onSpeedChange(newValue)
                                }
                            }
                            false
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowDropUp,
                        contentDescription = null,
                        tint = PlayerColors.textPrimary
                    )
                    Text(text = "${speed}x", color = PlayerColors.textPrimary, fontSize = 16.sp)
                    Icon(
                        imageVector = Icons.Rounded.ArrowDropDown,
                        contentDescription = null,
                        tint = PlayerColors.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
internal fun LiveLineDialog(
    modifier: Modifier = Modifier,
    lines: List<LiveStreamLine>,
    currentLineIndex: Int,
    onHideDialog: () -> Unit,
    onLineChange: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val focusRequesters = remember(lines) {
        lines.associate { it.index to FocusRequester() }
    }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    fun touch() {
        lastInteractionTime = System.currentTimeMillis()
    }

    LaunchedEffect(lines, currentLineIndex) {
        val requester = focusRequesters[currentLineIndex]
            ?: lines.firstOrNull()?.let { focusRequesters[it.index] }
        requester?.requestFocus(scope)
    }

    LaunchedEffect(lastInteractionTime) {
        val base = lastInteractionTime
        delay(15000)
        if (base == lastInteractionTime) onHideDialog()
    }

    PlayerOverlayDialog(onDismissRequest = onHideDialog) {
        Surface(
            modifier = modifier.width(360.dp),
            color = PlayerColors.dialogBackground,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "直播线路",
                    color = PlayerColors.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp
                )

                Column {
                    lines.forEach { line ->
                        val selected = line.index == currentLineIndex
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, start = 8.dp, end = 8.dp)
                                .focusRequester(focusRequesters[line.index]!!),
                            shape = ButtonDefaults.shape(MaterialTheme.shapes.medium),
                            scale = ButtonDefaults.scale(focusedScale = 1f),
                            colors = ButtonDefaults.colors(
                                containerColor = if (selected) MaterialTheme.colorScheme.inverseSurface.copy(
                                    alpha = 0.4f
                                ) else PlayerColors.buttonDefault,
                                contentColor = PlayerColors.textPrimary,
                                focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
                                focusedContentColor = androidx.compose.ui.graphics.Color.Black
                            ),
                            onClick = {
                                touch()
                                onLineChange(line.index)
                                onHideDialog()
                            }
                        ) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = line.displayName,
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun RotationDialog(
    modifier: Modifier = Modifier,
    rotation: VideoRotation,
    onHideDialog: () -> Unit,
    onRotationChange: (VideoRotation) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val options = remember { VideoRotation.entries }
    val context = LocalContext.current
    val focusRequesters = remember { options.associateWith { FocusRequester() } }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    fun touch() {
        lastInteractionTime = System.currentTimeMillis()
    }

    LaunchedEffect(rotation) {
        focusRequesters[rotation]?.requestFocus(scope)
    }

    LaunchedEffect(lastInteractionTime) {
        val base = lastInteractionTime
        delay(15000)
        if (base == lastInteractionTime) onHideDialog()
    }

    PlayerOverlayDialog(onDismissRequest = onHideDialog) {
        Surface(
            modifier = modifier.width(240.dp),
            color = PlayerColors.dialogBackground,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = stringResource(R.string.video_player_menu_picture_rotation),
                    color = PlayerColors.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp
                )

                Column {
                    options.forEach { option ->
                        val selected = option == rotation
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, start = 8.dp, end = 8.dp)
                                .focusRequester(focusRequesters[option]!!),
                            shape = ButtonDefaults.shape(MaterialTheme.shapes.medium),
                            scale = ButtonDefaults.scale(focusedScale = 1f),
                            colors = ButtonDefaults.colors(
                                containerColor = if (selected) MaterialTheme.colorScheme.inverseSurface.copy(
                                    alpha = 0.4f
                                ) else PlayerColors.buttonDefault,
                                contentColor = PlayerColors.textPrimary,
                                focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
                                focusedContentColor = androidx.compose.ui.graphics.Color.Black
                            ),
                            onClick = { touch(); onRotationChange(option) }
                        ) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = option.getDisplayName(context),
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SubtitleDialog(
    modifier: Modifier = Modifier,
    subtitle: Subtitle,
    availableSubtitleTracks: List<Subtitle>,
    onHideDialog: () -> Unit,
    onSubtitleChange: (Subtitle) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val focusRequesters =
        remember { availableSubtitleTracks.map { it.id }.associateWith { FocusRequester() } }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    fun touch() {
        lastInteractionTime = System.currentTimeMillis()
    }

    LaunchedEffect(subtitle) {
        focusRequesters[subtitle.id]?.requestFocus(scope)
    }

    LaunchedEffect(lastInteractionTime) {
        val base = lastInteractionTime
        delay(15000)
        if (base == lastInteractionTime) onHideDialog()
    }

    PlayerOverlayDialog(onDismissRequest = onHideDialog) {
        Surface(
            modifier = modifier.width(240.dp),
            color = PlayerColors.dialogBackground,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = stringResource(R.string.video_player_menu_subtitle_switch),
                    color = PlayerColors.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp
                )

                Column {
                    availableSubtitleTracks.forEach { option ->
                        val selected = option == subtitle
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, start = 8.dp, end = 8.dp)
                                .focusRequester(focusRequesters[option.id]!!),
                            shape = ButtonDefaults.shape(MaterialTheme.shapes.medium),
                            scale = ButtonDefaults.scale(focusedScale = 1f),
                            colors = ButtonDefaults.colors(
                                containerColor = if (selected) MaterialTheme.colorScheme.inverseSurface.copy(
                                    alpha = 0.4f
                                ) else PlayerColors.buttonDefault,
                                contentColor = PlayerColors.textPrimary,
                                focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
                                focusedContentColor = androidx.compose.ui.graphics.Color.Black
                            ),
                            onClick = { touch(); onSubtitleChange(option); onHideDialog() }
                        ) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = option.langDoc,
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerOverlayDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (!LocalTvUiSurfaceEmbedded.current) {
        Dialog(onDismissRequest = onDismissRequest, content = content)
        return
    }

    BackHandler(onBack = onDismissRequest)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onDismissRequest),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.clickable(onClick = {})) {
            content()
        }
    }
}
