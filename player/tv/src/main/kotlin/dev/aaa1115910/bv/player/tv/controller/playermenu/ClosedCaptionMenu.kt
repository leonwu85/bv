package dev.aaa1115910.bv.player.tv.controller.playermenu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.VideoPlayerClosedCaptionMenuItem
import dev.aaa1115910.bv.player.tv.controller.LocalMenuFocusStateData
import dev.aaa1115910.bv.player.tv.controller.MenuFocusState
import dev.aaa1115910.bv.player.tv.controller.playermenu.component.MenuListItem
import dev.aaa1115910.bv.player.tv.controller.playermenu.component.RadioMenuList
import dev.aaa1115910.bv.player.tv.controller.playermenu.component.StepLessMenuItem
import dev.aaa1115910.bv.player.tv.theme.PlayerColors
import dev.aaa1115910.bv.util.ifElse
import java.text.NumberFormat

@Composable
fun ClosedCaptionMenuList(
    modifier: Modifier = Modifier,
    onSubtitleChange: (Subtitle) -> Unit,
    onSubtitleSizeChange: (TextUnit) -> Unit,
    onSubtitleBackgroundOpacityChange: (Float) -> Unit,
    onSubtitleBottomPadding: (Dp) -> Unit,
    onSecondarySubtitleChange: (Subtitle) -> Unit,
    onSecondarySubtitleSizeChange: (TextUnit) -> Unit,
    onSecondarySubtitleBackgroundOpacityChange: (Float) -> Unit,
    onSecondarySubtitleBottomPadding: (Dp) -> Unit,
    onFocusStateChange: (MenuFocusState) -> Unit
) {
    val context = LocalContext.current
    val videoPlayerConfigData = LocalVideoPlayerConfigData.current
    val focusState = LocalMenuFocusStateData.current
    val parentMenuFocusRequester = remember { FocusRequester() }
    val parentMenuPositionFocusRequester = remember { FocusRequester() }
    var preferredClosedCaptionMenuItem by remember {
        mutableStateOf(VideoPlayerClosedCaptionMenuItem.Switch)
    }
    val secondarySubtitleTracks = videoPlayerConfigData.availableSubtitleTracks.filter {
        it.id == -1L || it.id != videoPlayerConfigData.currentSubtitleId
    }
    val visibleMenuItems = VideoPlayerClosedCaptionMenuItem.entries.filter {
        videoPlayerConfigData.currentSubtitleId != -1L || !it.isSecondary
    }
    val selectedClosedCaptionMenuItem = preferredClosedCaptionMenuItem.takeIf { it in visibleMenuItems }
        ?: visibleMenuItems.firstOrNull()
        ?: VideoPlayerClosedCaptionMenuItem.Switch

    Row(
        modifier = modifier.fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val menuItemsModifier = Modifier
            .width(216.dp)
            .padding(horizontal = 8.dp)
        val showDetailPane = focusState.focusState != MenuFocusState.MenuNav
        if (showDetailPane) {
            when (selectedClosedCaptionMenuItem) {
                VideoPlayerClosedCaptionMenuItem.Switch -> RadioMenuList(
                    modifier = menuItemsModifier,
                    items = videoPlayerConfigData.availableSubtitleTracks.map { it.langDoc },
                    selected = videoPlayerConfigData.availableSubtitleTracks
                        .indexOfFirst { it.id == videoPlayerConfigData.currentSubtitleId },
                    onSelectedChanged = { onSubtitleChange(videoPlayerConfigData.availableSubtitleTracks[it]) },
                    onFocusBackToParent = {
                        onFocusStateChange(MenuFocusState.Menu)
                        parentMenuFocusRequester.requestFocus()
                    },
                )

                VideoPlayerClosedCaptionMenuItem.Size -> StepLessMenuItem(
                    modifier = menuItemsModifier,
                    value = videoPlayerConfigData.currentSubtitleFontSize.value.toInt(),
                    step = 1,
                    range = 12..48,
                    text = "${videoPlayerConfigData.currentSubtitleFontSize.value.toInt()} SP",
                    onValueChange = { onSubtitleSizeChange(it.sp) },
                    onFocusBackToParent = { onFocusStateChange(MenuFocusState.Menu) }
                )

                VideoPlayerClosedCaptionMenuItem.Opacity -> StepLessMenuItem(
                    modifier = menuItemsModifier,
                    value = videoPlayerConfigData.currentSubtitleBackgroundOpacity,
                    step = 0.01f,
                    range = 0f..1f,
                    text = NumberFormat.getPercentInstance()
                        .apply { maximumFractionDigits = 0 }
                        .format(videoPlayerConfigData.currentSubtitleBackgroundOpacity),
                    onValueChange = onSubtitleBackgroundOpacityChange,
                    onFocusBackToParent = { onFocusStateChange(MenuFocusState.Menu) }
                )

                VideoPlayerClosedCaptionMenuItem.Padding -> StepLessMenuItem(
                    modifier = menuItemsModifier,
                    value = videoPlayerConfigData.currentSubtitleBottomPadding.value.toInt(),
                    step = 1,
                    range = 0..48,
                    text = "${videoPlayerConfigData.currentSubtitleBottomPadding.value.toInt()} DP",
                    onValueChange = { onSubtitleBottomPadding(it.dp) },
                    onFocusBackToParent = { onFocusStateChange(MenuFocusState.Menu) }
                )

                VideoPlayerClosedCaptionMenuItem.SecondarySwitch -> RadioMenuList(
                    modifier = menuItemsModifier,
                    items = secondarySubtitleTracks.map { it.langDoc },
                    selected = secondarySubtitleTracks
                        .indexOfFirst { it.id == videoPlayerConfigData.currentSecondarySubtitleId },
                    onSelectedChanged = { onSecondarySubtitleChange(secondarySubtitleTracks[it]) },
                    onFocusBackToParent = {
                        onFocusStateChange(MenuFocusState.Menu)
                        parentMenuFocusRequester.requestFocus()
                    },
                )

                VideoPlayerClosedCaptionMenuItem.SecondarySize -> StepLessMenuItem(
                    modifier = menuItemsModifier,
                    value = videoPlayerConfigData.currentSecondarySubtitleFontSize.value.toInt(),
                    step = 1,
                    range = 12..48,
                    text = "${videoPlayerConfigData.currentSecondarySubtitleFontSize.value.toInt()} SP",
                    onValueChange = { onSecondarySubtitleSizeChange(it.sp) },
                    onFocusBackToParent = { onFocusStateChange(MenuFocusState.Menu) }
                )

                VideoPlayerClosedCaptionMenuItem.SecondaryOpacity -> StepLessMenuItem(
                    modifier = menuItemsModifier,
                    value = videoPlayerConfigData.currentSecondarySubtitleBackgroundOpacity,
                    step = 0.01f,
                    range = 0f..1f,
                    text = NumberFormat.getPercentInstance()
                        .apply { maximumFractionDigits = 0 }
                        .format(videoPlayerConfigData.currentSecondarySubtitleBackgroundOpacity),
                    onValueChange = onSecondarySubtitleBackgroundOpacityChange,
                    onFocusBackToParent = { onFocusStateChange(MenuFocusState.Menu) }
                )

                VideoPlayerClosedCaptionMenuItem.SecondaryPadding -> StepLessMenuItem(
                    modifier = menuItemsModifier,
                    value = videoPlayerConfigData.currentSecondarySubtitleBottomPadding.value.toInt(),
                    step = 1,
                    range = 0..48,
                    text = "${videoPlayerConfigData.currentSecondarySubtitleBottomPadding.value.toInt()} DP",
                    onValueChange = { onSecondarySubtitleBottomPadding(it.dp) },
                    onFocusBackToParent = { onFocusStateChange(MenuFocusState.Menu) }
                )
            }
        }

        // 竖线分隔
        if (showDetailPane) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.6f)
                    .width(1.dp)
                    .background(PlayerColors.menuGlassBorder)
            )
        }

        LazyColumn(
            modifier = Modifier
                .focusRequester(parentMenuFocusRequester)
                .padding(horizontal = 8.dp)
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyUp) {
                        if (listOf(Key.Enter, Key.DirectionCenter).contains(it.key)) {
                            return@onPreviewKeyEvent false
                        }
                        return@onPreviewKeyEvent true
                    }
                    when (it.key) {
                        Key.DirectionRight -> onFocusStateChange(MenuFocusState.MenuNav)
                        Key.DirectionLeft -> onFocusStateChange(MenuFocusState.Items)
                        else -> {}
                    }
                    false
                }
                .focusRestorer(parentMenuPositionFocusRequester),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            itemsIndexed(
                items = visibleMenuItems,
                key = { _, item -> item.name }
            ) { index, item ->
                MenuListItem(
                    modifier = Modifier
                        .ifElse(
                            index == 0,
                            Modifier.focusRequester(parentMenuPositionFocusRequester)
                        ),
                    text = item.getDisplayName(context),
                    selected = selectedClosedCaptionMenuItem == item,
                    onClick = {},
                    onFocus = { preferredClosedCaptionMenuItem = item },
                )
            }
        }
    }
}
