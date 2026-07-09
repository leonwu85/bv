package dev.aaa1115910.bv.tv.screens.user

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bv.R as AppR
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.tv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.tv.R as TvR
import dev.aaa1115910.bv.tv.activities.video.SeasonInfoActivity
import dev.aaa1115910.bv.tv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.tv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.tv.util.ProvideListBringIntoViewSpec
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.util.scrollToItemIfAvailable
import dev.aaa1115910.bv.viewmodel.user.HistoryViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    historyViewModel: HistoryViewModel = koinViewModel(),
    showPageTitle: Boolean = true,
    lazyGridState: LazyGridState = rememberLazyGridState()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentIndex by remember { mutableIntStateOf(0) }
    val showLargeTitle by remember { derivedStateOf { currentIndex < 4 } }
    val titleFontSize by animateFloatAsState(
        targetValue = if (showLargeTitle) 48f else 24f,
        label = "title font size"
    )
    var deleteMode by remember { mutableStateOf(false) }
    var showMenuDialog by remember { mutableStateOf(false) }
    var selectedVideo by remember { mutableStateOf<VideoCardData?>(null) }
    val menuFocusRequester = remember { FocusRequester() }
    val emptyFocusRequester = remember { FocusRequester() }
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    // LazyGrid / FocusRequester 共用同一套 key（含 index，与 itemsIndexed 一致）
    val historyItemKey: (VideoCardData, Int) -> String = remember {
        { item, index ->
            "${item.historyBusiness}_${item.historyKid ?: item.avid}_${item.historyViewAt ?: 0L}_${item.avid}#$index"
        }
    }
    var pendingRestoreFocusIndex by remember { mutableIntStateOf(-1) }
    var shouldFocusEmptyState by remember { mutableStateOf(false) }

    val requestDelete: (VideoCardData, Int) -> Unit = { history, index ->
        if (!historyViewModel.deleting) {
            val targetIndex = when {
                historyViewModel.histories.size <= 1 -> -1
                index >= historyViewModel.histories.lastIndex -> index - 1
                else -> index
            }
            historyViewModel.deleteHistory(history, targetIndex)
        }
    }

    LaunchedEffect(Unit) {
        // 预加载可能已在拉数：空列表且未在加载时才请求，禁止 clearData 打断进行中的请求（会导致重复 item key）
        if (historyViewModel.histories.isEmpty() && !historyViewModel.updating) {
            historyViewModel.update()
        }
    }

    LaunchedEffect(historyViewModel.deletePhase) {
        when (historyViewModel.deletePhase) {
            1 -> Unit
            2 -> {
                if (historyViewModel.histories.isEmpty()) {
                    shouldFocusEmptyState = true
                    if (!historyViewModel.noMore) {
                        pendingRestoreFocusIndex = 0
                        if (!historyViewModel.updating) {
                            historyViewModel.update()
                        }
                    } else {
                        pendingRestoreFocusIndex = -1
                        deleteMode = false
                    }
                } else {
                    shouldFocusEmptyState = false
                    pendingRestoreFocusIndex = historyViewModel.pendingFocusIndex.coerceIn(
                        minimumValue = 0,
                        maximumValue = historyViewModel.histories.size - 1
                    )
                }
                historyViewModel.resetDeletePhase()
            }
        }
    }

    LaunchedEffect(
        pendingRestoreFocusIndex,
        historyViewModel.histories.size
    ) {
        if (pendingRestoreFocusIndex == -1) return@LaunchedEffect
        if (historyViewModel.histories.isEmpty()) {
            return@LaunchedEffect
        }

        val targetIndex = pendingRestoreFocusIndex.coerceIn(
            minimumValue = 0,
            maximumValue = historyViewModel.histories.size - 1
        )
        val targetHistory = historyViewModel.histories.getOrNull(targetIndex) ?: run {
            pendingRestoreFocusIndex = -1
            return@LaunchedEffect
        }
        currentIndex = targetIndex
        lazyGridState.scrollToItemIfAvailable(targetIndex)
        withFrameNanos { }
        withFrameNanos { }
        // 必须与 itemsIndexed / DisposableEffect 注册的 key 完全一致
        focusRequesters[historyItemKey(targetHistory, targetIndex)]?.requestFocus(scope)
        shouldFocusEmptyState = false
        pendingRestoreFocusIndex = -1
    }

    LaunchedEffect(historyViewModel.histories.size, shouldFocusEmptyState, pendingRestoreFocusIndex) {
        if (historyViewModel.histories.isNotEmpty() &&
            shouldFocusEmptyState &&
            pendingRestoreFocusIndex == -1
        ) {
            pendingRestoreFocusIndex = 0
        }
    }

    LaunchedEffect(shouldFocusEmptyState, historyViewModel.histories.size) {
        if (!shouldFocusEmptyState || historyViewModel.histories.isNotEmpty()) return@LaunchedEffect
        withFrameNanos { }
        withFrameNanos { }
        emptyFocusRequester.requestFocus(scope)
    }

    LaunchedEffect(
        shouldFocusEmptyState,
        historyViewModel.histories.size,
        historyViewModel.noMore,
        historyViewModel.updating
    ) {
        if (shouldFocusEmptyState &&
            historyViewModel.histories.isEmpty() &&
            historyViewModel.noMore &&
            !historyViewModel.updating
        ) {
            deleteMode = false
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (showPageTitle) {
                Box(
                    modifier = Modifier.padding(
                        start = 48.dp,
                        top = 24.dp,
                        bottom = 8.dp,
                        end = 48.dp
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(AppR.string.user_homepage_recent),
                            fontSize = titleFontSize.sp
                        )
                        if (deleteMode) {
                            Text(
                                text = stringResource(AppR.string.history_delete_mode_hint),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                            )
                        } else if (historyViewModel.noMore) {
                            Text(
                                text = stringResource(
                                    AppR.string.load_data_count_no_more,
                                    historyViewModel.histories.size
                                ),
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        } else {
                            Text(
                                text = stringResource(
                                    AppR.string.load_data_count,
                                    historyViewModel.histories.size
                                ),
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val gridColumns = Prefs.gridColumns
        val padding = dimensionResource(TvR.dimen.grid_padding) / 2
        val spacedBy = dimensionResource(TvR.dimen.grid_spacedBy) / 2
        ProvideListBringIntoViewSpec(padding = 26.dp) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onPreviewKeyEvent { keyEvent ->
                        when (keyEvent.key) {
                            Key.Menu -> {
                                if (keyEvent.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                                if (historyViewModel.histories.isNotEmpty()) {
                                    deleteMode = !deleteMode
                                }
                                return@onPreviewKeyEvent true
                            }

                            Key.Back -> {
                                if (!deleteMode) return@onPreviewKeyEvent false
                                if (keyEvent.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                                deleteMode = false
                                return@onPreviewKeyEvent true
                            }

                            else -> false
                        }
                    }
            ) {
                if (historyViewModel.histories.isEmpty()) {
                    HistoryStatusCard(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(innerPadding)
                            .focusRequester(emptyFocusRequester)
                            .focusable(),
                        text = if (historyViewModel.updating && !historyViewModel.noMore) {
                            stringResource(AppR.string.loading)
                        } else {
                            stringResource(AppR.string.no_data)
                        }
                    )
                } else {
                    LazyVerticalGrid(
                        modifier = Modifier
                            .padding(innerPadding)
                            .focusRestorer(),
                        state = lazyGridState,
                        columns = GridCells.Fixed(gridColumns),
                        contentPadding = PaddingValues(padding),
                        verticalArrangement = Arrangement.spacedBy(spacedBy),
                        horizontalArrangement = Arrangement.spacedBy(spacedBy)
                    ) {
                        itemsIndexed(
                            items = historyViewModel.histories,
                            // 附带 index，避免接口/竞态下业务 key 重复导致 LazyGrid 崩溃
                            key = { index, item -> historyItemKey(item, index) }
                        ) { index, history ->
                            val itemKey = historyItemKey(history, index)
                            val itemFocusRequester = remember(itemKey) { FocusRequester() }
                            DisposableEffect(itemKey, itemFocusRequester) {
                                focusRequesters[itemKey] = itemFocusRequester
                                onDispose {
                                    if (focusRequesters[itemKey] === itemFocusRequester) {
                                        focusRequesters.remove(itemKey)
                                    }
                                }
                            }

                            Box {
                                SmallVideoCard(
                                    modifier = Modifier.focusRequester(itemFocusRequester),
                                    data = history,
                                    onClick = {
                                        if (deleteMode) {
                                            requestDelete(history, index)
                                        } else if (history.jumpToSeason) {
                                            SeasonInfoActivity.actionStart(
                                                context = context,
                                                epId = history.epId,
                                                seasonId = history.seasonId,
                                                proxyArea = ProxyArea.checkProxyArea(history.title)
                                            )
                                        } else {
                                            VideoInfoActivity.actionStart(
                                                context = context,
                                                aid = history.avid,
                                                proxyArea = ProxyArea.checkProxyArea(history.title)
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        if (!deleteMode) {
                                            currentIndex = index
                                            selectedVideo = history
                                            showMenuDialog = true
                                        }
                                    },
                                    onFocus = {
                                        currentIndex = index
                                        if (!deleteMode && index + 12 > historyViewModel.histories.size) {
                                            historyViewModel.update()
                                        }
                                    },
                                    overlay = { hasFocus ->
                                        if (deleteMode) {
                                            HistoryDeleteBadge(isFocused = hasFocus)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMenuDialog && selectedVideo != null) {
        HistoryMenuDialog(
            show = showMenuDialog,
            focusRequester = menuFocusRequester,
            showGotoUpSpace = selectedVideo!!.upId > 0,
            onDismiss = {
                showMenuDialog = false
                selectedVideo = null
            },
            onEnterDeleteMode = {
                showMenuDialog = false
                deleteMode = true
                selectedVideo = null
            },
            onGotoUpSpace = {
                showMenuDialog = false
                UpInfoActivity.actionStart(
                    context,
                    mid = selectedVideo!!.upId,
                    name = selectedVideo!!.upName,
                    face = selectedVideo!!.upFace
                )
                selectedVideo = null
            }
        )
    }
}

@Composable
private fun BoxScope.HistoryDeleteBadge(
    isFocused: Boolean
) {
    Row(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 12.dp, end = 12.dp)
            .background(
                color = if (isFocused) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
                } else {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.92f)
                },
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            modifier = Modifier.size(14.dp),
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            tint = Color.White
        )
        Text(
            text = stringResource(
                if (isFocused) {
                    AppR.string.history_delete_mode_badge_focused
                } else {
                    AppR.string.history_delete_mode_badge
                }
            ),
            color = Color.White,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun HistoryStatusCard(
    modifier: Modifier = Modifier,
    text: String
) {
    var hasFocus by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .width(360.dp)
            .border(
                width = 2.dp,
                color = if (hasFocus) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                shape = MaterialTheme.shapes.large
            )
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                shape = MaterialTheme.shapes.large
            )
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .onFocusChanged { hasFocus = it.isFocused },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.88f)
        )
    }
}

@Composable
private fun HistoryMenuDialog(
    show: Boolean,
    focusRequester: FocusRequester,
    showGotoUpSpace: Boolean,
    onDismiss: () -> Unit,
    onEnterDeleteMode: () -> Unit,
    onGotoUpSpace: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var hasFocused by remember { mutableStateOf(false) }
    val dummyFocusRequester = remember { FocusRequester() }

    LaunchedEffect(show) {
        if (show) {
            hasFocused = false
            dummyFocusRequester.requestFocus(scope)
        }
    }

    if (show) {
        TvAlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = stringResource(AppR.string.toview_menu_title)) },
            text = {
                Box {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .size(1.dp)
                            .focusRequester(dummyFocusRequester)
                            .focusable()
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown &&
                                    (keyEvent.key == Key.DirectionUp || keyEvent.key == Key.DirectionDown)
                                ) {
                                    if (!hasFocused) {
                                        focusRequester.requestFocus(scope)
                                        hasFocused = true
                                        return@onKeyEvent true
                                    }
                                }
                                false
                            }
                            .background(Color.Transparent)
                    )

                    LazyColumn(
                        modifier = Modifier.width(240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        item {
                            HistoryMenuButton(
                                modifier = Modifier.focusRequester(focusRequester),
                                text = stringResource(AppR.string.history_menu_enter_delete_mode),
                                onClick = onEnterDeleteMode
                            )
                        }
                        if (showGotoUpSpace) {
                            item {
                                HistoryMenuButton(
                                    text = stringResource(AppR.string.toview_menu_goto_up_space),
                                    onClick = onGotoUpSpace
                                )
                            }
                        }
                    }
                }
            },
            dismissButton = {},
            confirmButton = {}
        )
    }
}

@Composable
private fun HistoryMenuButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = ButtonDefaults.shape(shape = MaterialTheme.shapes.medium),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
