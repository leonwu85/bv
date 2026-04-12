package dev.aaa1115910.bv.tv.screens.search

import android.content.Context
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.repositories.SearchFilterDuration
import dev.aaa1115910.biliapi.repositories.SearchFilterOrderType
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.util.Partition
import dev.aaa1115910.bv.util.PartitionUtil

@Composable
fun SearchResultVideoFilter(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideFilter: () -> Unit,
    selectedOrder: SearchFilterOrderType,
    selectedDuration: SearchFilterDuration,
    selectedPartition: Partition?,
    selectedChildPartition: Partition?,
    onSelectedOrderChange: (SearchFilterOrderType) -> Unit,
    onSelectedDurationChange: (SearchFilterDuration) -> Unit,
    onSelectedPartitionChange: (Partition?) -> Unit,
    onSelectedChildPartitionChange: (Partition?) -> Unit,
) {
    val context = LocalContext.current
    val partitions = remember { PartitionUtil.partitions }
    val defaultFocusRequester = remember { FocusRequester() }
    val durationFocusRequester = remember { FocusRequester() }
    val partitionFocusRequester = remember { FocusRequester() }
    val partitionChildFocusRequester = remember { FocusRequester() }

    // 用于防止对话框刚打开时误触发点击事件
    var isDialogJustOpened by remember { mutableStateOf(false) }

    val filterRowSpace = 8.dp

    LaunchedEffect(show) {
        if (show) {
            isDialogJustOpened = true
            // 延迟请求焦点，避免打开对话框的按键事件被新获得焦点的组件消费
            delay(100)
            defaultFocusRequester.requestFocus()
            // 等待一段时间后才允许点击事件
            delay(200)
            isDialogJustOpened = false
        }
    }

    if (show) {
        TvAlertDialog(
            modifier = modifier
                .fillMaxWidth(0.8f),
            onDismissRequest = onHideFilter,
            title = {
                Text(
                    text = stringResource(R.string.filter_dialog_title),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 排序方式
                    Column {
                        Text(
                            text = "排序方式",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        LazyRow(
                            modifier = Modifier.onPreviewKeyEvent {
                            if (it.key == Key.DirectionDown) {
                                if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                                    durationFocusRequester.requestFocus()
                                    return@onPreviewKeyEvent true
                                }
                                return@onPreviewKeyEvent true
                            }
                            false
                        },
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(filterRowSpace)
                    ) {
                        items(items = SearchFilterOrderType.webFilters) { orderType ->
                            FilterDialogFilterChip(
                                focusRequester = defaultFocusRequester,
                                selected = orderType == selectedOrder,
                                onClick = { onSelectedOrderChange(orderType) },
                                label = { Text(text = orderType.getDisplayName(context)) },
                                enabled = !isDialogJustOpened
                            )
                        }
                    }
                    }

                    // 时长筛选
                    Column {
                        Text(
                            text = "视频时长",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    LazyRow(
                        modifier = Modifier.onPreviewKeyEvent {
                            if (it.key == Key.DirectionDown) {
                                if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                                    partitionFocusRequester.requestFocus()
                                    return@onPreviewKeyEvent true
                                }
                                return@onPreviewKeyEvent true
                            }
                            if (it.key == Key.DirectionUp) {
                                if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                                    defaultFocusRequester.requestFocus()
                                    return@onPreviewKeyEvent true
                                }
                                return@onPreviewKeyEvent true
                            }
                            false
                        },
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(filterRowSpace)
                    ) {
                        items(items = SearchFilterDuration.entries) { duration ->
                            FilterDialogFilterChip(
                                focusRequester = durationFocusRequester,
                                selected = duration == selectedDuration,
                                onClick = { onSelectedDurationChange(duration) },
                                label = { Text(text = duration.getDisplayName(context)) },
                                enabled = !isDialogJustOpened
                            )
                        }
                    }
                    }

                    // 分区筛选
                    Column {
                        Text(
                            text = "分区",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        LazyRow(
                            modifier = Modifier.onPreviewKeyEvent {
                                if (it.key == Key.DirectionDown) {
                                    if (selectedChildPartition == null) return@onPreviewKeyEvent false
                                    if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                                        partitionChildFocusRequester.requestFocus()
                                        return@onPreviewKeyEvent true
                                    }
                                    return@onPreviewKeyEvent true
                                }
                                if (it.key == Key.DirectionUp) {
                                if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                                    durationFocusRequester.requestFocus()
                                    return@onPreviewKeyEvent true
                                }
                                return@onPreviewKeyEvent true
                            }
                            false
                        },
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(filterRowSpace)
                    ) {
                        item {
                            FilterDialogFilterChip(
                                focusRequester = partitionFocusRequester,
                                selected = null == selectedPartition,
                                onClick = {
                                    onSelectedPartitionChange(null)
                                    onSelectedChildPartitionChange(null)
                                },
                                label = { Text(text = "全部分区") },
                                enabled = !isDialogJustOpened
                            )
                        }
                        items(items = partitions) { partition ->
                            FilterDialogFilterChip(
                                focusRequester = partitionFocusRequester,
                                selected = partition == selectedPartition,
                                onClick = {
                                    onSelectedPartitionChange(partition)
                                    onSelectedChildPartitionChange(null)
                                },
                                label = { Text(text = partition.strRes) },
                                enabled = !isDialogJustOpened
                            )
                        }
                    }
                    }

                    // 子分区
                    AnimatedVisibility(visible = selectedPartition != null) {
                        LazyRow(
                            modifier = Modifier.onPreviewKeyEvent {
                                if (it.key == Key.DirectionUp) {
                                    if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                                        partitionFocusRequester.requestFocus()
                                        return@onPreviewKeyEvent true
                                    }
                                    return@onPreviewKeyEvent true
                                }
                                false
                            },
                            contentPadding = PaddingValues(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(filterRowSpace)
                        ) {
                            items(items = selectedPartition?.children ?: emptyList()) { partition ->
                                FilterDialogFilterChip(
                                    focusRequester = partitionChildFocusRequester,
                                    selected = partition == selectedChildPartition,
                                    onClick = {
                                        onSelectedChildPartitionChange(
                                            if (partition != selectedChildPartition) partition
                                            else null
                                        )
                                    },
                                    label = { Text(text = partition.strRes) },
                                    enabled = !isDialogJustOpened
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Surface(
                    onClick = {
                        if (!isDialogJustOpened) {
                            onSelectedOrderChange(SearchFilterOrderType.ComprehensiveSort)
                            onSelectedDurationChange(SearchFilterDuration.All)
                            onSelectedPartitionChange(null)
                            onSelectedChildPartitionChange(null)
                        }
                    },
                    shape = ClickableSurfaceDefaults.shape(SearchTheme.pillShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedContainerColor = SearchTheme.accentPink,
                        pressedContainerColor = SearchTheme.accentPink
                    )
                ) {
                    Text(
                        text = stringResource(R.string.filter_dialog_reset),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        )
    }

    BackHandler(
        enabled = show,
        onBack = onHideFilter
    )
}

@Composable
private fun FilterDialogFilterChip(
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester,
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    enabled: Boolean = true
) {
    var hasFocus by remember { mutableStateOf(false) }
    val focusRequesterModifier = if (selected)
        modifier.focusRequester(focusRequester)
    else modifier

    val bgColor by animateColorAsState(
        targetValue = when {
            selected -> SearchTheme.accentPink
            hasFocus -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        animationSpec = tween(150),
        label = "chip bg"
    )
    val contentColor = if (selected) Color.White
    else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = focusRequesterModifier
            .onFocusChanged { hasFocus = it.hasFocus },
        onClick = { if (enabled) onClick() },
        shape = ClickableSurfaceDefaults.shape(SearchTheme.chipShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = bgColor,
            focusedContainerColor = bgColor,
            pressedContainerColor = bgColor,
            contentColor = contentColor,
            focusedContentColor = contentColor,
            pressedContentColor = contentColor
        )
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            label()
        }
    }
}

fun SearchFilterOrderType.getDisplayName(context: Context) = when (this) {
    SearchFilterOrderType.ComprehensiveSort -> context.getString(R.string.search_result_filter_order_type_comprehensive_sort)
    SearchFilterOrderType.MostClicks -> context.getString(R.string.search_result_filter_order_type_most_clicks)
    SearchFilterOrderType.LatestPublish -> context.getString(R.string.search_result_filter_order_type_latest_publish)
    SearchFilterOrderType.MostDanmaku -> context.getString(R.string.search_result_filter_order_type_most_danmaku)
    SearchFilterOrderType.MostFavorites -> context.getString(R.string.search_result_filter_order_type_most_favorites)
    SearchFilterOrderType.MostComment -> "最多评论"
    SearchFilterOrderType.MostLikes -> "最多点赞"
}

fun SearchFilterDuration.getDisplayName(context: Context) = when (this) {
    SearchFilterDuration.All -> context.getString(R.string.search_result_filter_duration_all)
    SearchFilterDuration.LessThan10Minutes -> context.getString(R.string.search_result_filter_duration_less_than_10)
    SearchFilterDuration.Between10And30Minutes -> context.getString(R.string.search_result_filter_duration_10_to_30)
    SearchFilterDuration.Between30And60Minutes -> context.getString(R.string.search_result_filter_duration_30_to_60)
    SearchFilterDuration.MoreThan60Minutes -> context.getString(R.string.search_result_filter_duration_more_than_60)
}