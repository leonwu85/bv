package dev.aaa1115910.bv.mobile.screen.home

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.aaa1115910.biliapi.entity.search.SearchKeyword
import dev.aaa1115910.biliapi.repositories.SearchType
import dev.aaa1115910.biliapi.repositories.SearchTypeResult
import dev.aaa1115910.bv.mobile.activities.SeasonInfoActivity
import dev.aaa1115910.bv.mobile.activities.UserSpaceActivity
import dev.aaa1115910.bv.mobile.activities.VideoPlayerActivity
import dev.aaa1115910.bv.mobile.component.preferences.items.listItemPreference
import dev.aaa1115910.bv.mobile.component.preferences.preferenceGroups
import dev.aaa1115910.bv.mobile.screen.home.search.SearchInputContent
import dev.aaa1115910.bv.mobile.screen.home.search.SearchResultContent
import dev.aaa1115910.bv.mobile.screen.home.search.SearchTrendingRankingContent
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme
import dev.aaa1115910.bv.viewmodel.search.SearchInputViewModel
import dev.aaa1115910.bv.viewmodel.search.SearchResultViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    searchInputViewModel: SearchInputViewModel = koinViewModel(),
    searchResultViewModel: SearchResultViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val windowSizeClass = calculateWindowSizeClass(context as Activity)
    val windowSize = windowSizeClass.widthSizeClass

    val updateKeyword: (String) -> Unit = { newKeyword ->
        if (newKeyword != searchInputViewModel.keyword) {
            searchInputViewModel.keyword = newKeyword
            searchInputViewModel.updateSuggests()
        }
    }

    val onSearch: (String) -> Unit = {
        searchResultViewModel.keyword = it
        searchResultViewModel.update()
        searchInputViewModel.addSearchHistory(it)
    }

    val onOpenUgc: (Long) -> Unit = { aid ->
        VideoPlayerActivity.actionStart(context = context, aid = aid)
    }
    val onOpenPgc: (Int) -> Unit = {
        SeasonInfoActivity.actionStart(context = context, seasonId = it)
    }
    val onOpenUser: (SearchTypeResult.User) -> Unit = { user ->
        UserSpaceActivity.actionStart(
            context = context,
            mid = user.mid,
            name = user.name
        )
    }
    val onOpenLiveRoom: (SearchTypeResult.LiveRoom) -> Unit = { room ->
        VideoPlayerActivity.actionStartLive(
            context = context,
            roomId = room.roomId,
            title = room.title,
            upName = room.uname,
            upFace = room.uface,
            upMid = room.uid,
            watchedNum = room.online
        )
    }
    val onSearchTypeChange: (SearchType) -> Unit = { type ->
        if (searchResultViewModel.searchType != type) {
            searchResultViewModel.searchType = type
            if (searchResultViewModel.keyword.isNotBlank()) {
                searchResultViewModel.update()
            }
        }
    }

    SearchContent(
        modifier = modifier,
        windowSize = windowSize,
        hotwords = searchInputViewModel.hotwords,
        recommendKeywords = searchInputViewModel.recommendKeywords,
        trendingRankingKeywords = searchInputViewModel.trendingRankingKeywords,
        keywordSuggestions = searchInputViewModel.suggests,
        historyKeywords = searchInputViewModel.searchHistories.map { it.keyword },
        matchedHistory = searchInputViewModel.matchedSearchHistories.map { it.keyword },
        hotwordsLoading = searchInputViewModel.hotwordsLoading,
        recommendKeywordsLoading = searchInputViewModel.recommendKeywordsLoading,
        trendingRankingLoading = searchInputViewModel.trendingRankingLoading,
        hotwordsError = searchInputViewModel.hotwordsError,
        recommendKeywordsError = searchInputViewModel.recommendKeywordsError,
        trendingRankingError = searchInputViewModel.trendingRankingError,
        updateKeyword = updateKeyword,
        onSearch = onSearch,
        onDeleteHistory = searchInputViewModel::deleteSearchHistoryByKeyword,
        onClearHistories = searchInputViewModel::deleteAllSearchHistories,
        onRefreshHotwords = searchInputViewModel::refreshHotwords,
        onRefreshRecommendKeywords = searchInputViewModel::refreshRecommendKeywords,
        onRefreshTrendingRanking = searchInputViewModel::refreshTrendingRanking,
        searchType = searchResultViewModel.searchType,
        isLoading = searchResultViewModel.isLoading(searchResultViewModel.searchType),
        onSearchTypeChange = onSearchTypeChange,
        onLoadMore = searchResultViewModel::loadMore,
        onOpenUgc = onOpenUgc,
        onOpenPgc = onOpenPgc,
        onOpenUser = onOpenUser,
        onOpenLiveRoom = onOpenLiveRoom,
        videoSearchResult = searchResultViewModel.videoSearchResult.videos,
        mediaBangumiSearchResult = searchResultViewModel.mediaBangumiSearchResult.mediaBangumis,
        mediaFtSearchResult = searchResultViewModel.mediaFtSearchResult.mediaFts,
        biliUserSearchResult = searchResultViewModel.biliUserSearchResult.biliUsers,
        liveRoomSearchResult = searchResultViewModel.liveRoomSearchResult.liveRooms
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SearchContent(
    modifier: Modifier = Modifier,
    windowSize: WindowWidthSizeClass,
    hotwords: List<SearchKeyword> = emptyList(),
    recommendKeywords: List<SearchKeyword> = emptyList(),
    trendingRankingKeywords: List<SearchKeyword> = emptyList(),
    keywordSuggestions: List<String> = emptyList(),
    historyKeywords: List<String>,
    matchedHistory: List<String>,
    hotwordsLoading: Boolean = false,
    recommendKeywordsLoading: Boolean = false,
    trendingRankingLoading: Boolean = false,
    hotwordsError: String? = null,
    recommendKeywordsError: String? = null,
    trendingRankingError: String? = null,
    updateKeyword: (String) -> Unit = {},
    onSearch: (String) -> Unit = {},
    onDeleteHistory: (String) -> Unit = {},
    onClearHistories: () -> Unit = {},
    onRefreshHotwords: () -> Unit = {},
    onRefreshRecommendKeywords: () -> Unit = {},
    onRefreshTrendingRanking: () -> Unit = {},
    searchType: SearchType = SearchType.Video,
    isLoading: Boolean = false,
    onSearchTypeChange: (SearchType) -> Unit = {},
    onLoadMore: (SearchType) -> Unit = {},
    onOpenUgc: (Long) -> Unit = {},
    onOpenPgc: (Int) -> Unit = {},
    onOpenUser: (SearchTypeResult.User) -> Unit = {},
    onOpenLiveRoom: (SearchTypeResult.LiveRoom) -> Unit = {},
    videoSearchResult: List<SearchTypeResult.Video>,
    mediaBangumiSearchResult: List<SearchTypeResult.Pgc>,
    mediaFtSearchResult: List<SearchTypeResult.Pgc>,
    biliUserSearchResult: List<SearchTypeResult.User>,
    liveRoomSearchResult: List<SearchTypeResult.LiveRoom>
) {
    val scope = rememberCoroutineScope()
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    val navController = rememberNavController()

    var searchBarExpanded by remember { mutableStateOf(false) }
    var textFieldFocused by remember { mutableStateOf(false) }
    var searchResultSourceRoute by remember { mutableStateOf("searchInput") }

    LaunchedEffect(textFieldState.text, textFieldFocused) {
        searchBarExpanded = textFieldState.text != "" && textFieldFocused
        updateKeyword(textFieldState.text.toString())
    }

    val onSearchKeyword: (String) -> Unit = {
        val keyword = it.trim()
        if (keyword.isNotEmpty()) {
            onSearch(keyword)
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != "searchResult") {
                searchResultSourceRoute = currentRoute ?: "searchInput"
                navController.navigate("searchResult")
            }
            textFieldState.setTextAndPlaceCursorAtEnd(keyword)
            scope.launch {
                // 等到 searchBar 移动到顶部再收起
                delay(500)
                searchBarState.animateToCollapsed()
            }
        }
    }
    val onBackToSearchInput: () -> Unit = {
        val popped = navController.popBackStack("searchInput", inclusive = false)
        if (!popped && navController.currentDestination?.route != "searchInput") {
            navController.navigate("searchInput")
        }
        textFieldState.setTextAndPlaceCursorAtEnd("")
        textFieldFocused = false
        searchBarExpanded = false
        updateKeyword("")
        scope.launch {
            searchBarState.animateToCollapsed()
        }
    }
    val onBackFromSearchResult: () -> Unit = {
        if (searchResultSourceRoute == "searchTrendingRanking") {
            val popped = navController.popBackStack("searchTrendingRanking", inclusive = false)
            if (!popped && navController.currentDestination?.route != "searchTrendingRanking") {
                navController.navigate("searchTrendingRanking")
            }
            textFieldState.setTextAndPlaceCursorAtEnd("")
            textFieldFocused = false
            searchBarExpanded = false
            updateKeyword("")
            scope.launch {
                searchBarState.animateToCollapsed()
            }
        } else {
            onBackToSearchInput()
        }
    }

    val inputField = @Composable {
        SearchBarDefaults.InputField(
            modifier = Modifier.onFocusChanged { textFieldFocused = it.isFocused },
            searchBarState = searchBarState,
            textFieldState = textFieldState,
            onSearch = onSearchKeyword,
            placeholder = { Text(text = "在此处输入文字") },
            trailingIcon = {
                val keyword = textFieldState.text.toString()
                Row {
                    if (keyword.isNotEmpty()) {
                        IconButton(
                            modifier = Modifier.size(40.dp),
                            onClick = {
                                textFieldState.setTextAndPlaceCursorAtEnd("")
                                updateKeyword("")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null
                            )
                        }
                    }
                    IconButton(
                        modifier = Modifier.size(40.dp),
                        enabled = keyword.isNotBlank(),
                        onClick = { onSearchKeyword(keyword) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    }
                }
            }
        )
    }

    SharedTransitionLayout(
        modifier = modifier
    ) {
        NavHost(
            navController = navController,
            startDestination = "searchInput"
        ) {
            composable("searchInput") {
                SearchInputContent(
                    windowSize = windowSize,
                    hotwords = hotwords,
                    recommendKeywords = recommendKeywords,
                    keywordSuggestions = keywordSuggestions,
                    keywordHistories = historyKeywords,
                    matchedKeyworkHistories = matchedHistory,
                    hotwordsLoading = hotwordsLoading,
                    recommendKeywordsLoading = recommendKeywordsLoading,
                    hotwordsError = hotwordsError,
                    recommendKeywordsError = recommendKeywordsError,
                    searchBarState = searchBarState,
                    textFieldState = textFieldState,
                    searchBarExpanded = searchBarExpanded,
                    onSearchBarExpandedChange = { searchBarExpanded = it },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    inputField = inputField,
                    onSearch = onSearchKeyword,
                    onDeleteHistory = onDeleteHistory,
                    onClearHistories = onClearHistories,
                    onRefreshHotwords = onRefreshHotwords,
                    onRefreshRecommendKeywords = onRefreshRecommendKeywords,
                    onOpenTrendingRanking = {
                        navController.navigate("searchTrendingRanking")
                    }
                )
            }
            composable("searchTrendingRanking") {
                SearchTrendingRankingContent(
                    modifier = Modifier.fillMaxSize(),
                    keywords = trendingRankingKeywords,
                    loading = trendingRankingLoading,
                    error = trendingRankingError,
                    onSearch = onSearchKeyword,
                    onRefresh = onRefreshTrendingRanking,
                    onBack = {
                        navController.popBackStack("searchInput", inclusive = false)
                    }
                )
            }
            composable("searchResult") {
                SearchResultContent(
                    modifier = Modifier.fillMaxSize(),
                    searchBarState = searchBarState,
                    textFieldState = textFieldState,
                    keywordSuggestions = keywordSuggestions,
                    historyKeywords = historyKeywords,
                    matchedHistory = matchedHistory,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    inputField = inputField,
                    videoSearchResult = videoSearchResult,
                    mediaBangumiSearchResult = mediaBangumiSearchResult,
                    mediaFtSearchResult = mediaFtSearchResult,
                    biliUserSearchResult = biliUserSearchResult,
                    liveRoomSearchResult = liveRoomSearchResult,
                    onSearch = onSearchKeyword,
                    onDeleteHistory = onDeleteHistory,
                    onBackToSearchInput = onBackFromSearchResult,
                    searchType = searchType,
                    isLoading = isLoading,
                    onSearchTypeChange = onSearchTypeChange,
                    onLoadMore = onLoadMore,
                    onOpenUgc = onOpenUgc,
                    onOpenPgc = onOpenPgc,
                    onOpenUser = onOpenUser,
                    onOpenLiveRoom = onOpenLiveRoom
                )
            }
        }
    }

    if (windowSize == WindowWidthSizeClass.Compact) {
        ExpandedFullScreenSearchBar(
            state = searchBarState,
            inputField = inputField
        ) {
            SearchBarResultContent(
                modifier = Modifier.fillMaxSize(),
                keyword = textFieldState.text.toString(),
                recentHistory = historyKeywords,
                matchedHistory = matchedHistory,
                suggestions = keywordSuggestions,
                onSearch = onSearchKeyword,
                onDeleteHistory = onDeleteHistory
            )
        }
    }
}


@Composable
fun SearchBarResultContent(
    modifier: Modifier = Modifier,
    keyword: String,
    recentHistory: List<String>,
    matchedHistory: List<String>,
    suggestions: List<String>,
    onSearch: (String) -> Unit,
    onDeleteHistory: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listItemColors = ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )

    Surface(
        modifier = modifier.pointerInput(focusManager, keyboardController) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                if (waitForUpOrCancellation() != null) {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                }
            }
        },
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        LazyColumn(
            modifier = Modifier,
            contentPadding = PaddingValues(12.dp)
        ) {
            preferenceGroups(
                "历史记录" to {
                    if (keyword.isNotEmpty()) {
                        matchedHistory.take(10).map {
                            listItemPreference(
                                headlineContent = { Text(text = it) },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface),
                                    ) {
                                        Icon(
                                            modifier = Modifier.padding(3.dp),
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = "search history icon",
                                        )
                                    }
                                },
                                colors = listItemColors,
                                onClick = { onSearch(it) },
                                trailingContent = {
                                    TextButton(onClick = { onDeleteHistory(it) }) {
                                        Text(text = "删除")
                                    }
                                }
                            )
                        }
                    } else {
                        recentHistory.take(10).map {
                            listItemPreference(
                                headlineContent = { Text(text = it) },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface),
                                    ) {
                                        Icon(
                                            modifier = Modifier.padding(3.dp),
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = "search history icon",
                                        )
                                    }
                                },
                                colors = listItemColors,
                                onClick = { onSearch(it) },
                                trailingContent = {
                                    TextButton(onClick = { onDeleteHistory(it) }) {
                                        Text(text = "删除")
                                    }
                                }
                            )
                        }
                    }
                },
                "搜索建议" to {
                    if (keyword.isNotEmpty()) {
                        suggestions.map {
                            listItemPreference(
                                headlineContent = { Text(text = it) },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface),
                                    ) {
                                        Icon(
                                            modifier = Modifier.padding(3.dp),
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "search suggestion icon",
                                        )
                                    }
                                },
                                colors = listItemColors,
                                onClick = { onSearch(it) }
                            )
                        }
                    }
                }
            )
        }
    }
}

@Preview
@Composable
private fun SearchScreenMobilePreview() {
    BVMobileTheme {
        SearchContent(
            windowSize = WindowWidthSizeClass.Compact,
            videoSearchResult = emptyList(),
            mediaBangumiSearchResult = emptyList(),
            mediaFtSearchResult = emptyList(),
            biliUserSearchResult = emptyList(),
            liveRoomSearchResult = emptyList(),
            historyKeywords = emptyList(),
            matchedHistory = emptyList()
        )
    }
}

@Preview(device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
private fun SearchScreenTablePreview() {
    BVMobileTheme {
        SearchContent(
            windowSize = WindowWidthSizeClass.Expanded,
            videoSearchResult = emptyList(),
            mediaBangumiSearchResult = emptyList(),
            mediaFtSearchResult = emptyList(),
            biliUserSearchResult = emptyList(),
            liveRoomSearchResult = emptyList(),
            historyKeywords = emptyList(),
            matchedHistory = emptyList()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SearchBarResultCompatPreview() {
    val inputField = @Composable {
        SearchBarDefaults.InputField(
            searchBarState = rememberSearchBarState(),
            textFieldState = rememberTextFieldState(),
            onSearch = {},
            placeholder = { Text(text = "在此处输入文字") },
        )
    }

    BVMobileTheme {
        ExpandedFullScreenSearchBar(
            state = rememberSearchBarState(
                initialValue = SearchBarValue.Expanded
            ),
            inputField = inputField
        ) {
            SearchBarResultContent(
                modifier = Modifier.fillMaxSize(),
                keyword = "123",
                recentHistory = listOf("123", "456", "789"),
                matchedHistory = listOf("123", "456", "789"),
                suggestions = listOf("123", "456", "789"),
                onSearch = {},
                onDeleteHistory = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SearchBarResultDockedPreview() {
    val inputField = @Composable {
        SearchBarDefaults.InputField(
            searchBarState = rememberSearchBarState(),
            textFieldState = rememberTextFieldState(),
            onSearch = {},
            placeholder = { Text(text = "在此处输入文字") },
        )
    }

    BVMobileTheme {
        DockedSearchBar(
            expanded = true,
            onExpandedChange = {},
            inputField = inputField,
        ) {
            SearchBarResultContent(
                keyword = "123",
                recentHistory = listOf("123", "456", "789"),
                matchedHistory = listOf("123", "456", "789"),
                suggestions = listOf("123", "456", "789"),
                onSearch = {},
                onDeleteHistory = {}
            )
        }
    }
}
