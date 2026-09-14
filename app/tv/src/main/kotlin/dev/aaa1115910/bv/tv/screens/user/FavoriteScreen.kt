package dev.aaa1115910.bv.tv.screens.user

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.FavoriteFolderMetadata
import dev.aaa1115910.bv.R as AppR
import dev.aaa1115910.bv.tv.R as TvR
import dev.aaa1115910.bv.tv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.tv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.tv.component.CollectionAccent
import dev.aaa1115910.bv.tv.component.ContentStatusCard
import dev.aaa1115910.bv.tv.component.LoadingTip
import dev.aaa1115910.bv.tv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.tv.util.ProvideListBringIntoViewSpec
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.ifElse
import dev.aaa1115910.bv.util.onDelayFocusChanged
import dev.aaa1115910.bv.util.scrollToItemIfAvailable
import dev.aaa1115910.bv.viewmodel.user.FavoriteViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun FavoriteScreen(
    modifier: Modifier = Modifier,
    favoriteViewModel: FavoriteViewModel = koinViewModel(),
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
    val focusRequester = remember { FocusRequester() }
    val controlsFocusRequester = remember { FocusRequester() }
    val videoFocusRequester = remember { FocusRequester() }
    var focusedVideoId by remember { mutableStateOf<Long?>(null) }
    var focusOnGrid by remember { mutableStateOf(false) }
    val currentTabIndex by remember {
        derivedStateOf {
            if (favoriteViewModel.favoriteFolderMetadataList.indexOf(favoriteViewModel.currentFavoriteFolderMetadata) >= 0) favoriteViewModel.favoriteFolderMetadataList.indexOf(
                favoriteViewModel.currentFavoriteFolderMetadata
            ) else 0
        }
    }

    val updateCurrentFavoriteFolder: (FavoriteFolderMetadata) -> Unit = favoriteViewModel::selectFolder

    BackHandler(
        enabled = focusOnGrid && !favoriteViewModel.selectionMode
    ) {
        scope.launch(Dispatchers.Main) {
            lazyGridState.scrollToItemIfAvailable(0)
            focusRequester.requestFocus()
            focusOnGrid = false
        }
    }

    LaunchedEffect(Unit) {
        if (favoriteViewModel.favoriteFolderMetadataList.isEmpty()) {
            favoriteViewModel.clearData()
            favoriteViewModel.updateFoldersInfo()
        }
    }

    var initialFocusRequested by remember { mutableStateOf(false) }
    LaunchedEffect(showPageTitle, favoriteViewModel.favoriteFolderMetadataList.isNotEmpty()) {
        if (showPageTitle && !initialFocusRequested && favoriteViewModel.favoriteFolderMetadataList.isNotEmpty()) {
            withFrameNanos { }
            focusRequester.requestFocus()
            initialFocusRequested = true
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
                        val title = favoriteViewModel.currentFavoriteFolderMetadata?.title?.let {
                            "${stringResource(AppR.string.user_homepage_favorite)} - $it"
                        } ?: stringResource(AppR.string.user_homepage_favorite)
                        Text(
                            text = title,
                            fontSize = titleFontSize.sp
                        )
                        Text(
                            text = stringResource(
                                AppR.string.load_data_count,
                                favoriteViewModel.favorites.size
                            ),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val gridColumns = Prefs.gridColumns.coerceAtLeast(1)
        val padding = dimensionResource(TvR.dimen.grid_padding) / 2
        val spacedBy = dimensionResource(TvR.dimen.grid_spacedBy) / 2
        if (favoriteViewModel.favoriteFolderMetadataList.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (favoriteViewModel.updatingFolders) {
                    LoadingTip()
                } else {
                    ContentStatusCard(text = stringResource(AppR.string.no_data))
                }
            }
        } else {
            val focusedVideoIndex = favoriteViewModel.favorites.indexOfFirst { it.avid == focusedVideoId }
                .coerceAtLeast(0)
            Column(
                modifier = Modifier.padding(innerPadding).fillMaxSize()
                    .focusProperties {
                        // Entry from the home navigation always starts at the folder tabs.
                        onEnter = {
                            if (requestedFocusDirection == FocusDirection.Down) focusRequester.requestFocus()
                        }
                    }
                    .focusGroup()
            ) {
                TabRow(
                    modifier = Modifier.padding(horizontal = padding, vertical = 8.dp)
                        .focusRestorer(focusRequester),
                    selectedTabIndex = currentTabIndex,
                    separator = { Spacer(modifier = Modifier.width(12.dp)) },
                ) {
                    favoriteViewModel.favoriteFolderMetadataList.forEachIndexed { index, folderMetadata ->
                        Tab(
                            modifier = Modifier
                                .onDelayFocusChanged {
                                    if (it.isFocused && favoriteViewModel.currentFavoriteFolderMetadata != folderMetadata) {
                                        updateCurrentFavoriteFolder(folderMetadata)
                                        focusedVideoId = null
                                        currentIndex = 0
                                        scope.launch { lazyGridState.scrollToItemIfAvailable(0) }
                                    }
                                }
                                .ifElse(index == currentTabIndex, Modifier.focusRequester(focusRequester))
                                .focusProperties { down = controlsFocusRequester },
                            selected = currentTabIndex == index,
                            onFocus = {},
                            onClick = { updateCurrentFavoriteFolder(folderMetadata) }
                        ) {
                            Box(modifier = Modifier.height(32.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    text = folderMetadata.title,
                                    color = LocalContentColor.current,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
                Box(Modifier.padding(horizontal = padding, vertical = 4.dp)) {
                    FavoriteTransferControls(
                        favoriteViewModel,
                        focusRequester = controlsFocusRequester,
                        onUp = { focusRequester.requestFocus() },
                        onDown = {
                            if (favoriteViewModel.favorites.isNotEmpty()) scope.launch {
                                if (lazyGridState.layoutInfo.visibleItemsInfo.none { it.index == focusedVideoIndex }) {
                                    lazyGridState.scrollToItem(focusedVideoIndex)
                                    withFrameNanos { }
                                }
                                videoFocusRequester.requestFocus()
                            }
                        }
                    )
                }
                ProvideListBringIntoViewSpec(padding = 24.dp) {
                    LazyVerticalGrid(
                        modifier = Modifier.weight(1f)
                            .onFocusChanged { focusOnGrid = it.hasFocus },
                        state = lazyGridState,
                        columns = GridCells.Fixed(gridColumns),
                        contentPadding = PaddingValues(top = 16.dp, bottom = padding, start = padding, end = padding),
                        verticalArrangement = Arrangement.spacedBy(spacedBy),
                        horizontalArrangement = Arrangement.spacedBy(spacedBy)
                    ) {
                        if (favoriteViewModel.favorites.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                                    if (favoriteViewModel.updatingFolderItems) LoadingTip()
                                    else ContentStatusCard(text = stringResource(AppR.string.no_data))
                                }
                            }
                        } else {
                            itemsIndexed(favoriteViewModel.favorites, key = { _, video -> video.avid }) { index, video ->
                                val isSelected = video.avid in favoriteViewModel.selectedIds
                                SmallVideoCard(
                                    modifier = Modifier
                                        .then(if (index == focusedVideoIndex) Modifier.focusRequester(videoFocusRequester) else Modifier)
                                        .focusProperties { if (index < gridColumns) up = controlsFocusRequester }
                                        .semantics { if (favoriteViewModel.selectionMode) selected = isSelected }
                                        .border(2.dp, if (isSelected) CollectionAccent else Color.Transparent, RoundedCornerShape(12.dp)),
                                    data = video,
                                    onClick = {
                                        if (favoriteViewModel.selectionMode) favoriteViewModel.toggleSelected(video.avid)
                                        else if (!favoriteViewModel.operating) VideoInfoActivity.actionStart(context, video.avid)
                                    },
                                    onLongClick = {
                                        if (favoriteViewModel.selectionMode) favoriteViewModel.toggleSelected(video.avid)
                                        else UpInfoActivity.actionStart(context, mid = video.upId, name = video.upName, face = video.upFace)
                                    },
                                    onFocus = {
                                        currentIndex = index
                                        focusedVideoId = video.avid
                                        if (index + 12 > favoriteViewModel.favorites.size) favoriteViewModel.updateFolderItems()
                                    },
                                    // Draw inside Surface: TV raises a focused Surface above its external siblings.
                                    overlay = { hasFocus ->
                                        if (favoriteViewModel.selectionMode) {
                                            Box(
                                                modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                                                    .size(24.dp)
                                                    .background(if (isSelected) CollectionAccent else Color(0xE61A1C25), RoundedCornerShape(5.dp))
                                                    .border(1.5.dp, if (hasFocus || isSelected) Color.White else Color(0xFFB6BAC6), RoundedCornerShape(5.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) Icon(Icons.Rounded.Check, contentDescription = null,
                                                    tint = Color.White, modifier = Modifier.size(18.dp))
                                            }
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
}
