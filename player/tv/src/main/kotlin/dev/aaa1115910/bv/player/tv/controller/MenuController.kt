package dev.aaa1115910.bv.player.tv.controller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import dev.aaa1115910.bv.player.tv.component.PlayerAnimations
import dev.aaa1115910.bv.player.tv.theme.PlayerColors
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.entity.video.SubtitleAiStatus
import dev.aaa1115910.biliapi.entity.video.SubtitleAiType
import dev.aaa1115910.biliapi.entity.video.SubtitleType
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.DanmakuSpeedMode
import dev.aaa1115910.bv.player.entity.DanmakuType
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.PlayMode
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoAspectRatio
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.player.entity.LiveCodec
import dev.aaa1115910.bv.player.entity.VideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.VideoPlayerMenuNavItem
import dev.aaa1115910.bv.player.entity.VideoRotation
import dev.aaa1115910.bv.player.tv.controller.playermenu.ClosedCaptionMenuList
import dev.aaa1115910.bv.player.tv.controller.playermenu.DanmakuMenuList
import dev.aaa1115910.bv.player.tv.controller.playermenu.MenuNavList
import dev.aaa1115910.bv.player.tv.controller.playermenu.OthersMenuList
import dev.aaa1115910.bv.player.tv.controller.playermenu.PictureMenuList
import dev.aaa1115910.bv.util.requestFocusWithRetry
import dev.aaa1115910.bv.util.swapList

@Composable
fun MenuController(
    modifier: Modifier = Modifier,
    show: Boolean,
    onInteraction: () -> Unit = {},
    onResolutionChange: (Resolution) -> Unit = {},
    onCodecChange: (VideoCodec) -> Unit = {},
    onAspectRatioChange: (VideoAspectRatio) -> Unit,
    onRotationChange: (VideoRotation) -> Unit,
    onPlaySpeedChange: (Float) -> Unit = {},
    onAudioChange: (Audio) -> Unit,
    onLiveQualityChange: (Int) -> Unit = {},
    onLiveCodecChange: (LiveCodec) -> Unit = {},
    onLiveLineChange: (Int) -> Unit = {},
    onDanmakuSwitchChange: (List<DanmakuType>) -> Unit,
    onDanmakuSizeChange: (Float) -> Unit,
    onDanmakuOpacityChange: (Float) -> Unit,
    onDanmakuAreaChange: (Float) -> Unit,
    onDanmakuSpeedModeChange: (DanmakuSpeedMode) -> Unit,
    onDanmakuPresentationSpeedChange: (Float) -> Unit,
    onDanmakuMaskChange: (Boolean) -> Unit = {},
    onDanmakuMergeChange: (Boolean) -> Unit = {},
    onDanmakuFilterLevelChange: (Int) -> Unit = {},
    isLive: Boolean = false,
    onSubtitleChange: (Subtitle) -> Unit,
    onSubtitleSizeChange: (TextUnit) -> Unit,
    onSubtitleBackgroundOpacityChange: (Float) -> Unit,
    onSubtitleBottomPadding: (Dp) -> Unit,
    onSecondarySubtitleChange: (Subtitle) -> Unit,
    onSecondarySubtitleSizeChange: (TextUnit) -> Unit,
    onSecondarySubtitleBackgroundOpacityChange: (Float) -> Unit,
    onSecondarySubtitleBottomPadding: (Dp) -> Unit,
    onPlayModeChange: (PlayMode) -> Unit
) {
    val defaultFocusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent {
                if (show && it.type == KeyEventType.KeyDown) {
                    onInteraction()
                }
                false
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        AnimatedVisibility(
            visible = show,
            enter = PlayerAnimations.menuEnter,
            exit = PlayerAnimations.menuExit
        ) {
            // 在动画内容中处理焦点请求
            LaunchedEffect(Unit) {
                defaultFocusRequester.requestFocusWithRetry()
            }
            MenuController(
                defaultFocusRequester = defaultFocusRequester,
                onResolutionChange = onResolutionChange,
                onCodecChange = onCodecChange,
                onAspectRatioChange = onAspectRatioChange,
                onRotationChange = onRotationChange,
                onPlaySpeedChange = onPlaySpeedChange,
                onAudioChange = onAudioChange,
                onLiveQualityChange = onLiveQualityChange,
                onLiveCodecChange = onLiveCodecChange,
                onLiveLineChange = onLiveLineChange,
                onDanmakuSwitchChange = onDanmakuSwitchChange,
                onDanmakuSizeChange = onDanmakuSizeChange,
                onDanmakuOpacityChange = onDanmakuOpacityChange,
                onDanmakuAreaChange = onDanmakuAreaChange,
                onDanmakuSpeedModeChange = onDanmakuSpeedModeChange,
                onDanmakuPresentationSpeedChange = onDanmakuPresentationSpeedChange,
                onDanmakuMaskChange = onDanmakuMaskChange,
                onDanmakuMergeChange = onDanmakuMergeChange,
                onDanmakuFilterLevelChange = onDanmakuFilterLevelChange,
                isLive = isLive,
                onSubtitleChange = onSubtitleChange,
                onSubtitleSizeChange = onSubtitleSizeChange,
                onSubtitleBackgroundOpacityChange = onSubtitleBackgroundOpacityChange,
                onSubtitleBottomPadding = onSubtitleBottomPadding,
                onSecondarySubtitleChange = onSecondarySubtitleChange,
                onSecondarySubtitleSizeChange = onSecondarySubtitleSizeChange,
                onSecondarySubtitleBackgroundOpacityChange = onSecondarySubtitleBackgroundOpacityChange,
                onSecondarySubtitleBottomPadding = onSecondarySubtitleBottomPadding,
                onPlayModeChange = onPlayModeChange
            )
        }
    }
}

@Composable
fun MenuController(
    modifier: Modifier = Modifier,
    defaultFocusRequester: FocusRequester,
    onResolutionChange: (Resolution) -> Unit = {},
    onCodecChange: (VideoCodec) -> Unit = {},
    onAspectRatioChange: (VideoAspectRatio) -> Unit,
    onRotationChange: (VideoRotation) -> Unit,
    onPlaySpeedChange: (Float) -> Unit,
    onAudioChange: (Audio) -> Unit,
    onLiveQualityChange: (Int) -> Unit = {},
    onLiveCodecChange: (LiveCodec) -> Unit = {},
    onLiveLineChange: (Int) -> Unit = {},
    onDanmakuSwitchChange: (List<DanmakuType>) -> Unit,
    onDanmakuSizeChange: (Float) -> Unit,
    onDanmakuOpacityChange: (Float) -> Unit,
    onDanmakuAreaChange: (Float) -> Unit,
    onDanmakuSpeedModeChange: (DanmakuSpeedMode) -> Unit,
    onDanmakuPresentationSpeedChange: (Float) -> Unit,
    onDanmakuMaskChange: (Boolean) -> Unit = {},
    onDanmakuMergeChange: (Boolean) -> Unit = {},
    onDanmakuFilterLevelChange: (Int) -> Unit = {},
    isLive: Boolean = false,
    onSubtitleChange: (Subtitle) -> Unit,
    onSubtitleSizeChange: (TextUnit) -> Unit,
    onSubtitleBackgroundOpacityChange: (Float) -> Unit,
    onSubtitleBottomPadding: (Dp) -> Unit,
    onSecondarySubtitleChange: (Subtitle) -> Unit,
    onSecondarySubtitleSizeChange: (TextUnit) -> Unit,
    onSecondarySubtitleBackgroundOpacityChange: (Float) -> Unit,
    onSecondarySubtitleBottomPadding: (Dp) -> Unit,
    onPlayModeChange: (PlayMode) -> Unit
) {
    var focusedNavItem by remember { mutableStateOf(VideoPlayerMenuNavItem.Picture) }
    var activeNavItem by remember { mutableStateOf(VideoPlayerMenuNavItem.Picture) }
    var focusState by remember { mutableStateOf(MenuFocusState.MenuNav) }
    val displayNavItem = if (focusState == MenuFocusState.MenuNav) focusedNavItem else activeNavItem

    // 伪毛玻璃容器
    Box(
        modifier = modifier
            .fillMaxHeight(0.85f)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = PlayerColors.menuGlassBorder,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        // 底层：深色玻璃背景
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(PlayerColors.menuGlassOverlay)
        )
        // 中层：微带蓝紫色调
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(PlayerColors.menuGlassBackground)
        )
        // 顶层：高光渐变
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PlayerColors.menuGlassHighlightStart,
                            PlayerColors.menuGlassHighlightEnd
                        )
                    )
                )
        )

        CompositionLocalProvider(
            LocalMenuFocusStateData provides MenuFocusStateData(
                focusState = focusState
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                MenuList(
                    selectedNavMenu = displayNavItem,
                    onResolutionChange = onResolutionChange,
                    onCodecChange = onCodecChange,
                    onPlaySpeedChange = onPlaySpeedChange,
                    onAspectRatioChange = onAspectRatioChange,
                    onRotationChange = onRotationChange,
                    onAudioChange = onAudioChange,
                    onLiveQualityChange = onLiveQualityChange,
                    onLiveCodecChange = onLiveCodecChange,
                    onLiveLineChange = onLiveLineChange,
                    onDanmakuSwitchChange = onDanmakuSwitchChange,
                    onDanmakuSizeChange = onDanmakuSizeChange,
                    onDanmakuOpacityChange = onDanmakuOpacityChange,
                    onDanmakuAreaChange = onDanmakuAreaChange,
                    onDanmakuSpeedModeChange = onDanmakuSpeedModeChange,
                    onDanmakuPresentationSpeedChange = onDanmakuPresentationSpeedChange,
                    onDanmakuMaskChange = onDanmakuMaskChange,
                    onDanmakuMergeChange = onDanmakuMergeChange,
                    onDanmakuFilterLevelChange = onDanmakuFilterLevelChange,
                    isLive = isLive,
                    onFocusStateChange = {
                        if (it == MenuFocusState.MenuNav) {
                            focusedNavItem = activeNavItem
                        }
                        focusState = it
                    },
                    onSubtitleChange = onSubtitleChange,
                    onSubtitleSizeChange = onSubtitleSizeChange,
                    onSubtitleBackgroundOpacityChange = onSubtitleBackgroundOpacityChange,
                    onSubtitleBottomPadding = onSubtitleBottomPadding,
                    onSecondarySubtitleChange = onSecondarySubtitleChange,
                    onSecondarySubtitleSizeChange = onSecondarySubtitleSizeChange,
                    onSecondarySubtitleBackgroundOpacityChange = onSecondarySubtitleBackgroundOpacityChange,
                    onSecondarySubtitleBottomPadding = onSecondarySubtitleBottomPadding,
                    onPlayModeChange = onPlayModeChange
                )
                // 导航栏区域：稍深的半透明背景
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(PlayerColors.menuNavBackground)
                ) {
                    MenuNavList(
                        modifier = Modifier
                            .focusRequester(defaultFocusRequester)
                            .onPreviewKeyEvent {
                                when {
                                    it.type == KeyEventType.KeyUp -> {
                                        return@onPreviewKeyEvent true
                                    }

                                    it.key == Key.DirectionLeft -> {
                                        activeNavItem = focusedNavItem
                                        focusState = MenuFocusState.Menu
                                    }
                                }
                                false
                            },
                        selectedMenu = displayNavItem,
                        onSelectedChanged = { focusedNavItem = it },
                        isFocusing = focusState == MenuFocusState.MenuNav
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuList(
    modifier: Modifier = Modifier,
    selectedNavMenu: VideoPlayerMenuNavItem,
    onResolutionChange: (Resolution) -> Unit,
    onCodecChange: (VideoCodec) -> Unit,
    onAspectRatioChange: (VideoAspectRatio) -> Unit,
    onRotationChange: (VideoRotation) -> Unit,
    onPlaySpeedChange: (Float) -> Unit,
    onAudioChange: (Audio) -> Unit,
    onLiveQualityChange: (Int) -> Unit = {},
    onLiveCodecChange: (LiveCodec) -> Unit = {},
    onLiveLineChange: (Int) -> Unit = {},
    onDanmakuSwitchChange: (List<DanmakuType>) -> Unit,
    onDanmakuSizeChange: (Float) -> Unit,
    onDanmakuOpacityChange: (Float) -> Unit,
    onDanmakuAreaChange: (Float) -> Unit,
    onDanmakuSpeedModeChange: (DanmakuSpeedMode) -> Unit,
    onDanmakuPresentationSpeedChange: (Float) -> Unit,
    onDanmakuMaskChange: (Boolean) -> Unit = {},
    onDanmakuMergeChange: (Boolean) -> Unit = {},
    onDanmakuFilterLevelChange: (Int) -> Unit = {},
    isLive: Boolean = false,
    onSubtitleChange: (Subtitle) -> Unit,
    onSubtitleSizeChange: (TextUnit) -> Unit,
    onSubtitleBackgroundOpacityChange: (Float) -> Unit,
    onSubtitleBottomPadding: (Dp) -> Unit,
    onSecondarySubtitleChange: (Subtitle) -> Unit,
    onSecondarySubtitleSizeChange: (TextUnit) -> Unit,
    onSecondarySubtitleBackgroundOpacityChange: (Float) -> Unit,
    onSecondarySubtitleBottomPadding: (Dp) -> Unit,
    onPlayModeChange: (PlayMode) -> Unit,
    onFocusStateChange: (MenuFocusState) -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (selectedNavMenu) {
            VideoPlayerMenuNavItem.Picture -> {
                PictureMenuList(
                    onResolutionChange = onResolutionChange,
                    onCodecChange = onCodecChange,
                    onAspectRatioChange = onAspectRatioChange,
                    onRotationChange = onRotationChange,
                    onPlaySpeedChange = onPlaySpeedChange,
                    onAudioChange = onAudioChange,
                    onLiveQualityChange = onLiveQualityChange,
                    onLiveCodecChange = onLiveCodecChange,
                    onLiveLineChange = onLiveLineChange,
                    onFocusStateChange = onFocusStateChange
                )
            }

            VideoPlayerMenuNavItem.Danmaku -> {
                DanmakuMenuList(
                    onDanmakuSwitchChange = onDanmakuSwitchChange,
                    onDanmakuSizeChange = onDanmakuSizeChange,
                    onDanmakuOpacityChange = onDanmakuOpacityChange,
                    onDanmakuAreaChange = onDanmakuAreaChange,
                    onDanmakuSpeedModeChange = onDanmakuSpeedModeChange,
                    onDanmakuPresentationSpeedChange = onDanmakuPresentationSpeedChange,
                    onFocusStateChange = onFocusStateChange,
                    onDanmakuMaskChange = onDanmakuMaskChange,
                    onDanmakuMergeChange = onDanmakuMergeChange,
                    onDanmakuFilterLevelChange = onDanmakuFilterLevelChange,
                    isLive = isLive
                )
            }

            VideoPlayerMenuNavItem.ClosedCaption -> {
                ClosedCaptionMenuList(
                    onSubtitleChange = onSubtitleChange,
                    onSubtitleSizeChange = onSubtitleSizeChange,
                    onSubtitleBackgroundOpacityChange = onSubtitleBackgroundOpacityChange,
                    onSubtitleBottomPadding = onSubtitleBottomPadding,
                    onSecondarySubtitleChange = onSecondarySubtitleChange,
                    onSecondarySubtitleSizeChange = onSecondarySubtitleSizeChange,
                    onSecondarySubtitleBackgroundOpacityChange = onSecondarySubtitleBackgroundOpacityChange,
                    onSecondarySubtitleBottomPadding = onSecondarySubtitleBottomPadding,
                    onFocusStateChange = onFocusStateChange
                )
            }

//            VideoPlayerMenuNavItem.Others -> {
//                OthersMenuList(
//                    onPlayModeChange = onPlayModeChange,
//                    onFocusStateChange = onFocusStateChange
//                )
//            }
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
fun MenuControllerPreview() {

    val defaultFocusRequester = remember { FocusRequester() }

    var currentResolution by remember { mutableStateOf(Resolution.R240P) }
    var currentCodec by remember { mutableStateOf(VideoCodec.HEVC) }
    var currentVideoAspectRatio by remember { mutableStateOf(VideoAspectRatio.Default) }
    var currentVideoRotation by remember { mutableStateOf(VideoRotation.Original) }
    var currentPlaySpeed by remember { mutableFloatStateOf(1f) }
    var currentAudio by remember { mutableStateOf(Audio.A192K) }

    val currentDanmakuSwitch = remember { mutableStateListOf<DanmakuType>() }
    var currentDanmakuSize by remember { mutableFloatStateOf(1f) }
    var currentDanmakuOpacity by remember { mutableFloatStateOf(1f) }
    var currentDanmakuArea by remember { mutableFloatStateOf(1f) }
    var currentDanmakuSpeedMode by remember { mutableStateOf(DanmakuSpeedMode.FollowVideo) }
    var currentDanmakuPresentationSpeed by remember { mutableFloatStateOf(1f) }
    var currentDanmakuMask by remember { mutableStateOf(false) }
    var currentDanmakuMergeEnabled by remember { mutableStateOf(false) }

    var currentSubtitleId by remember { mutableLongStateOf(-1L) }
    val currentSubtitleList = remember { mutableStateListOf<Subtitle>() }
    var currentSubtitleFontSize by remember { mutableStateOf(24.sp) }
    var currentSubtitleBackgroundOpacity by remember { mutableFloatStateOf(0.4f) }
    var currentSubtitleBottomPadding by remember { mutableStateOf(8.dp) }
    var currentSecondarySubtitleId by remember { mutableLongStateOf(-1L) }
    var currentSecondarySubtitleFontSize by remember { mutableStateOf(24.sp) }
    var currentSecondarySubtitleBackgroundOpacity by remember { mutableFloatStateOf(0.4f) }
    var currentSecondarySubtitleBottomPadding by remember { mutableStateOf(8.dp) }

    var currentPlayMode by remember { mutableStateOf(PlayMode.Sequential) }

    LaunchedEffect(Unit) {
        currentSubtitleList.apply {
            addAll(
                listOf(
                    Subtitle(
                        id = -1,
                        langDoc = "关闭",
                        lang = "",
                        url = "",
                        type = SubtitleType.CC,
                        aiType = SubtitleAiType.Normal,
                        aiStatus = SubtitleAiStatus.None
                    ),
                    Subtitle(
                        id = 1111,
                        langDoc = "ai-zh",
                        lang = "中文（自动翻译）",
                        url = "",
                        type = SubtitleType.CC,
                        aiType = SubtitleAiType.Normal,
                        aiStatus = SubtitleAiStatus.None
                    ),
                    Subtitle(
                        id = 222,
                        lang = "zh",
                        langDoc = "中文",
                        url = "",
                        type = SubtitleType.CC,
                        aiType = SubtitleAiType.Normal,
                        aiStatus = SubtitleAiStatus.None
                    ),
                    Subtitle(
                        id = 1333,
                        lang = "ai-en",
                        langDoc = "English",
                        url = "",
                        type = SubtitleType.CC,
                        aiType = SubtitleAiType.Normal,
                        aiStatus = SubtitleAiStatus.None
                    )
                )
            )
        }
    }

    MaterialTheme {
        Surface(
            colors = SurfaceDefaults.colors(
                containerColor = Color.White
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                CompositionLocalProvider(
                    LocalVideoPlayerConfigData provides VideoPlayerConfigData(
                        availableResolutions = Resolution.entries,
                        availableVideoCodec = VideoCodec.entries,
                        availableAudio = Audio.entries,

                        currentResolution = currentResolution,
                        currentVideoCodec = currentCodec,
                        currentVideoAspectRatio = currentVideoAspectRatio,
                        currentVideoRotation = currentVideoRotation,
                        currentVideoSpeed = currentPlaySpeed,
                        currentAudio = currentAudio,

                        currentDanmakuEnabledList = currentDanmakuSwitch,
                        currentDanmakuScale = currentDanmakuSize,
                        currentDanmakuOpacity = currentDanmakuOpacity,
                        currentDanmakuArea = currentDanmakuArea,
                        currentDanmakuSpeedMode = currentDanmakuSpeedMode,
                        currentDanmakuPresentationSpeed = currentDanmakuPresentationSpeed,
                        currentDanmakuMask = currentDanmakuMask,
                        currentDanmakuMergeEnabled = currentDanmakuMergeEnabled,

                        currentSubtitleId = currentSubtitleId,
                        availableSubtitleTracks = currentSubtitleList,
                        currentSubtitleFontSize = currentSubtitleFontSize,
                        currentSubtitleBackgroundOpacity = currentSubtitleBackgroundOpacity,
                        currentSubtitleBottomPadding = currentSubtitleBottomPadding,
                        currentSecondarySubtitleId = currentSecondarySubtitleId,
                        currentSecondarySubtitleFontSize = currentSecondarySubtitleFontSize,
                        currentSecondarySubtitleBackgroundOpacity = currentSecondarySubtitleBackgroundOpacity,
                        currentSecondarySubtitleBottomPadding = currentSecondarySubtitleBottomPadding,

                        currentPlayMode = currentPlayMode
                    )
                ) {
                    MenuController(
                        modifier = Modifier
                            .align(Alignment.CenterEnd),
                        defaultFocusRequester = defaultFocusRequester,
                        onResolutionChange = { currentResolution = it },
                        onCodecChange = { currentCodec = it },
                        onAspectRatioChange = { currentVideoAspectRatio = it },
                        onRotationChange = { currentVideoRotation = it },
                        onPlaySpeedChange = { currentPlaySpeed = it },
                        onAudioChange = { currentAudio = it },
                        onDanmakuSwitchChange = {
                            val a = currentDanmakuSwitch.toList()
                            currentDanmakuSwitch.swapList(it)
                            val b = currentDanmakuSwitch.toList()
                            println("a=$a")
                            println("b=$b")

                        },
                        onDanmakuSizeChange = { currentDanmakuSize = it },
                        onDanmakuOpacityChange = { currentDanmakuOpacity = it },
                        onDanmakuAreaChange = { currentDanmakuArea = it },
                        onDanmakuSpeedModeChange = { currentDanmakuSpeedMode = it },
                        onDanmakuPresentationSpeedChange = { currentDanmakuPresentationSpeed = it },
                        onDanmakuMaskChange = { currentDanmakuMask = it },
                        onDanmakuMergeChange = { currentDanmakuMergeEnabled = it },
                        onSubtitleChange = { currentSubtitleId = it.id },
                        onSubtitleSizeChange = { currentSubtitleFontSize = it },
                        onSubtitleBackgroundOpacityChange = {
                            currentSubtitleBackgroundOpacity = it
                        },
                        onSubtitleBottomPadding = { currentSubtitleBottomPadding = it },
                        onSecondarySubtitleChange = { currentSecondarySubtitleId = it.id },
                        onSecondarySubtitleSizeChange = { currentSecondarySubtitleFontSize = it },
                        onSecondarySubtitleBackgroundOpacityChange = {
                            currentSecondarySubtitleBackgroundOpacity = it
                        },
                        onSecondarySubtitleBottomPadding = { currentSecondarySubtitleBottomPadding = it },
                        onPlayModeChange = { currentPlayMode = it }
                    )
                }
            }
        }
    }
}

enum class MenuFocusState {
    MenuNav, Menu, Items
}

data class MenuFocusStateData(
    val focusState: MenuFocusState = MenuFocusState.MenuNav
)

val LocalMenuFocusStateData = compositionLocalOf { MenuFocusStateData() }
