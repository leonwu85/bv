package dev.aaa1115910.bv.tv.screens.user

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.aaa1115910.bv.R as AppR
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.tv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.tv.R as TvR
import dev.aaa1115910.bv.tv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.tv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.tv.util.ProvideListBringIntoViewSpec
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.viewmodel.user.ToViewViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun ToViewScreen(
    modifier: Modifier = Modifier,
    ToViewViewModel: ToViewViewModel = koinViewModel(),
    showPageTitle: Boolean = true,
    onListEmpty: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentIndex by remember { mutableIntStateOf(0) }
    val showLargeTitle by remember { derivedStateOf { currentIndex < 4 } }
    val titleFontSize by animateFloatAsState(
        targetValue = if (showLargeTitle) 48f else 24f,
        label = "title font size"
    )

    var showMenuDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var selectedVideo by remember { mutableStateOf<VideoCardData?>(null) }
    val menuFocusRequester = remember { FocusRequester() }
    val deleteFocusRequester = remember { FocusRequester() }
    val contentFocusAnchorRequester = remember { FocusRequester() }

    val lazyGridState = rememberLazyGridState()
    val focusRequesters = remember { mutableMapOf<Long, FocusRequester>() }
    var pendingRestoreFocusIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        if (ToViewViewModel.histories.isEmpty()) {
            ToViewViewModel.clearData()
            ToViewViewModel.update()
        }
    }

    LaunchedEffect(showDeleteConfirmDialog) {
        if (showDeleteConfirmDialog) deleteFocusRequester.requestFocus(scope)
    }

    LaunchedEffect(ToViewViewModel.deletePhase) {
        when (ToViewViewModel.deletePhase) {
            1 -> Unit
            2 -> {
                if (ToViewViewModel.histories.isEmpty()) {
                    pendingRestoreFocusIndex = -1
                    onListEmpty?.invoke()
                } else {
                    pendingRestoreFocusIndex = ToViewViewModel.pendingFocusIndex.coerceIn(
                        minimumValue = 0,
                        maximumValue = ToViewViewModel.histories.size - 1
                    )
                }
                ToViewViewModel.resetDeletePhase()
            }
        }
    }

    LaunchedEffect(
        pendingRestoreFocusIndex,
        showDeleteConfirmDialog,
        ToViewViewModel.histories.size
    ) {
        if (pendingRestoreFocusIndex == -1 || showDeleteConfirmDialog) return@LaunchedEffect
        if (ToViewViewModel.histories.isEmpty()) {
            pendingRestoreFocusIndex = -1
            return@LaunchedEffect
        }

        val targetIndex = pendingRestoreFocusIndex.coerceIn(
            minimumValue = 0,
            maximumValue = ToViewViewModel.histories.size - 1
        )
        val targetHistory = ToViewViewModel.histories.getOrNull(targetIndex) ?: run {
            pendingRestoreFocusIndex = -1
            return@LaunchedEffect
        }
        currentIndex = targetIndex
        lazyGridState.scrollToItem(targetIndex)
        withFrameNanos { }
        withFrameNanos { }
        focusRequesters[targetHistory.avid]?.requestFocus(scope)
        pendingRestoreFocusIndex = -1
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
                            text = stringResource(AppR.string.title_activity_toview),
                            fontSize = titleFontSize.sp
                        )
                        if (ToViewViewModel.noMore) {
                            Text(
                                text = stringResource(
                                    AppR.string.load_data_count_no_more,
                                    ToViewViewModel.histories.size
                                ),
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        } else {
                            Text(
                                text = stringResource(
                                    AppR.string.load_data_count,
                                    ToViewViewModel.histories.size
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
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .width(1.dp)
                        .height(1.dp)
                        .focusRequester(contentFocusAnchorRequester)
                        .focusable()
                )
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
                        items = ToViewViewModel.histories,
                        key = { _, item -> item.avid }
                    ) { index, item ->
                        val itemFocusRequester = remember(item.avid) { FocusRequester() }
                        DisposableEffect(item.avid, itemFocusRequester) {
                            focusRequesters[item.avid] = itemFocusRequester
                            onDispose {
                                if (focusRequesters[item.avid] === itemFocusRequester) {
                                    focusRequesters.remove(item.avid)
                                }
                            }
                        }
                        SmallVideoCard(
                            modifier = Modifier.focusRequester(itemFocusRequester),
                            data = item,
                            onClick = {
                                VideoInfoActivity.actionStart(
                                    context = context,
                                    aid = item.avid,
                                    proxyArea = ProxyArea.checkProxyArea(item.title)
                                )
                            },
                            onLongClick = {
                                selectedVideo = item
                                showMenuDialog = true
                            },
                            onFocus = {
                                currentIndex = index
                                //预加载
                                // if (index + 12 > ToViewViewModel.histories.size) {
                                //     ToViewViewModel.update()
                                // }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showMenuDialog && selectedVideo != null) {
        ToViewMenuDialog(
            show = showMenuDialog,
            focusRequester = menuFocusRequester,
            onDismiss = {
                showMenuDialog = false
                selectedVideo = null
            },
            onDelete = {
                showMenuDialog = false
                showDeleteConfirmDialog = true
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

    if (showDeleteConfirmDialog && selectedVideo != null) {
        DeleteToViewConfirmDialog(
            show = showDeleteConfirmDialog,
            focusRequester = deleteFocusRequester,
            videoTitle = selectedVideo!!.title,
            onDismiss = {
                showDeleteConfirmDialog = false
                selectedVideo = null
            },
            onConfirm = {
                val targetIndex = maxOf(0, currentIndex - 1)
                ToViewViewModel.deleteToView(selectedVideo!!.avid, targetIndex)
                showDeleteConfirmDialog = false
                scope.launch {
                    withFrameNanos { }
                    contentFocusAnchorRequester.requestFocus(this)
                }
                selectedVideo = null
            }
        )
    }
}

@Composable
private fun ToViewMenuDialog(
    show: Boolean,
    focusRequester: FocusRequester,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onGotoUpSpace: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var hasFocused by remember { mutableStateOf(false) }
    val dummyFocusRequester = remember { FocusRequester() }

    LaunchedEffect(show) {
        if (show) {
            hasFocused = false
            // 初始焦点放在隐藏区块
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
                            .height(1.dp)
                            .focusRequester(dummyFocusRequester)
                            .focusable()
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown &&
                                    (keyEvent.key == Key.DirectionUp || keyEvent.key == Key.DirectionDown)) {
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
                            MenuButton(
                                modifier = Modifier.focusRequester(focusRequester),
                                text = stringResource(AppR.string.toview_menu_delete),
                                onClick = onDelete
                            )
                        }
                        item {
                            MenuButton(
                                text = stringResource(AppR.string.toview_menu_goto_up_space),
                                onClick = onGotoUpSpace
                            )
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
private fun DeleteToViewConfirmDialog(
    show: Boolean,
    focusRequester: FocusRequester,
    videoTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (show) {
        TvAlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = stringResource(AppR.string.toview_delete_confirm_dialog_title)) },
            text = {
                Text(
                    text = stringResource(
                        AppR.string.toview_delete_confirm_dialog_text,
                        videoTitle
                    )
                )
            },
            confirmButton = {
                Button(onClick = onConfirm) {
                    Text(text = stringResource(AppR.string.toview_delete_confirm_dialog_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(
                    modifier = Modifier.focusRequester(focusRequester),
                    onClick = onDismiss
                ) {
                    Text(text = stringResource(AppR.string.toview_delete_confirm_dialog_dismiss))
                }
            }
        )
    }
}

@Composable
private fun MenuButton(
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
