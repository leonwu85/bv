package dev.aaa1115910.bv.tv.screens.user

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.FavoriteFolderMetadata
import dev.aaa1115910.bv.R as AppR
import dev.aaa1115910.bv.tv.component.ContentStatusCard
import dev.aaa1115910.bv.tv.component.LoadingTip
import dev.aaa1115910.bv.tv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.tv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.tv.R as TvR
import dev.aaa1115910.bv.tv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.tv.util.ProvideListBringIntoViewSpec
import dev.aaa1115910.bv.util.ifElse
import dev.aaa1115910.bv.util.onDelayFocusChanged
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.viewmodel.user.FavoriteViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    val defaultFocusRequester = remember { FocusRequester() }
    var focusOnTabs by remember { mutableStateOf(true) }
    var focusOnGrid by remember { mutableStateOf(false) }
    val currentTabIndex by remember {
        derivedStateOf {
            if (favoriteViewModel.favoriteFolderMetadataList.indexOf(favoriteViewModel.currentFavoriteFolderMetadata) >= 0) favoriteViewModel.favoriteFolderMetadataList.indexOf(
                favoriteViewModel.currentFavoriteFolderMetadata
            ) else 0
        }
    }

    val updateCurrentFavoriteFolder: (folderMetadata: FavoriteFolderMetadata) -> Unit =
        { folderMetadata ->
            favoriteViewModel.currentFavoriteFolderMetadata = folderMetadata
            favoriteViewModel.favorites.clear()
            favoriteViewModel.resetPageNumber()
            favoriteViewModel.updateFolderItems(force = true)
        }

    BackHandler(
        enabled = focusOnGrid
    ) {
        scope.launch(Dispatchers.Main) {
            lazyGridState.scrollToItem(0)
            defaultFocusRequester.requestFocus()
            focusOnGrid = false
        }
    }

    LaunchedEffect(Unit) {
        if (favoriteViewModel.favoriteFolderMetadataList.isEmpty()) {
            favoriteViewModel.clearData()
            favoriteViewModel.updateFoldersInfo()
            if (showPageTitle) {
                delay(100)
                defaultFocusRequester.requestFocus()
            }
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
        val gridColumns = Prefs.gridColumns
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
            ProvideListBringIntoViewSpec(padding = 24.dp) {
                LazyVerticalGrid(
                    modifier = Modifier.padding(innerPadding),
                    state = lazyGridState,
                    columns = GridCells.Fixed(gridColumns),
                    contentPadding = PaddingValues(
                        top = if (showPageTitle) padding else 4.dp,
                        bottom = padding,
                        start = padding,
                        end = padding
                    ),
                    verticalArrangement = Arrangement.spacedBy(spacedBy),
                    horizontalArrangement = Arrangement.spacedBy(spacedBy)
                ) {
                    item(
                        span = { GridItemSpan(gridColumns) }
                    ) {
                        TabRow(
                            modifier = Modifier
                                .focusRequester(defaultFocusRequester)
                                .onFocusChanged { focusOnTabs = it.hasFocus }
                                .onDelayFocusChanged(50) {
                                    if (focusOnTabs) {
                                        focusRequester.requestFocus()
                                    }
                                },
                            selectedTabIndex = currentTabIndex,
                            separator = { Spacer(modifier = Modifier.width(12.dp)) },
                        ) {
                            favoriteViewModel.favoriteFolderMetadataList.forEachIndexed { index, folderMetadata ->
                                Tab(
                                    modifier = Modifier
                                        .onDelayFocusChanged {
                                            if (it.isFocused && favoriteViewModel.currentFavoriteFolderMetadata != folderMetadata) {
                                                updateCurrentFavoriteFolder(folderMetadata)
                                            }
                                        }
                                        .ifElse(
                                            index == currentTabIndex,
                                            Modifier.focusRequester(focusRequester)
                                        ),
                                    selected = currentTabIndex == index,
                                    onFocus = {},
                                    onClick = { updateCurrentFavoriteFolder(folderMetadata) }
                                ) {
                                    Box(
                                        modifier = Modifier.height(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp, vertical = 6.dp),
                                            text = folderMetadata.title,
                                            color = LocalContentColor.current,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (favoriteViewModel.favorites.isEmpty()) {
                        item(span = { GridItemSpan(gridColumns) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (favoriteViewModel.updatingFolderItems) {
                                    LoadingTip()
                                } else {
                                    ContentStatusCard(text = stringResource(AppR.string.no_data))
                                }
                            }
                        }
                    } else {
                        itemsIndexed(favoriteViewModel.favorites) { index, history ->
                            SmallVideoCard(
                                data = history,
                                onClick = { VideoInfoActivity.actionStart(context, history.avid) },
                                onLongClick = {
                                    UpInfoActivity.actionStart(
                                        context,
                                        mid = history.upId,
                                        name = history.upName,
                                        face = history.upFace
                                    )
                                },
                                onFocus = {
                                    focusOnGrid = true
                                    currentIndex = index
                                    //预加载
                                    if (index + 12 > favoriteViewModel.favorites.size) {
                                        favoriteViewModel.updateFolderItems()
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