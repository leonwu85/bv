package dev.aaa1115910.bv.mobile.screen.home.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.search.SearchKeyword
import dev.aaa1115910.bv.mobile.screen.home.SearchBarResultContent

private val SearchSectionShape = RoundedCornerShape(8.dp)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SearchInputContent(
    modifier: Modifier = Modifier,
    windowSize: WindowWidthSizeClass,
    hotwords: List<SearchKeyword> = emptyList(),
    recommendKeywords: List<SearchKeyword> = emptyList(),
    keywordSuggestions: List<String> = emptyList(),
    keywordHistories: List<String> = emptyList(),
    matchedKeyworkHistories: List<String> = emptyList(),
    hotwordsLoading: Boolean = false,
    recommendKeywordsLoading: Boolean = false,
    hotwordsError: String? = null,
    recommendKeywordsError: String? = null,
    searchBarState: SearchBarState = rememberSearchBarState(),
    textFieldState: TextFieldState = rememberTextFieldState(),
    searchBarExpanded: Boolean,
    onSearchBarExpandedChange: (Boolean) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    inputField: @Composable () -> Unit = {},
    onSearch: (String) -> Unit,
    onDeleteHistory: (String) -> Unit = {},
    onClearHistories: () -> Unit = {},
    onRefreshHotwords: () -> Unit = {},
    onRefreshRecommendKeywords: () -> Unit = {},
    onOpenTrendingRanking: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            when (windowSize) {
                WindowWidthSizeClass.Compact -> {
                    SearchInputTopBar(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        searchBarState = searchBarState,
                        inputField = inputField
                    )
                }

                else -> {
                    TopAppBar(
                        title = {
                            with(sharedTransitionScope) {
                                DockedSearchBar(
                                    modifier = Modifier
                                        .imePadding()
                                        .sharedElement(
                                            sharedContentState = rememberSharedContentState("dockedSearchBar"),
                                            animatedVisibilityScope = animatedVisibilityScope
                                        ),
                                    inputField = inputField,
                                    expanded = searchBarExpanded,
                                    onExpandedChange = onSearchBarExpandedChange,
                                ) {
                                    SearchBarResultContent(
                                        keyword = textFieldState.text.toString(),
                                        recentHistory = keywordHistories,
                                        matchedHistory = matchedKeyworkHistories,
                                        suggestions = keywordSuggestions,
                                        onSearch = onSearch,
                                        onDeleteHistory = onDeleteHistory
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
            shape = if (windowSize == WindowWidthSizeClass.Compact) RoundedCornerShape(0.dp) else MaterialTheme.shapes.large,
        ) {
            SearchInputBody(
                windowSize = windowSize,
                keyword = textFieldState.text.toString(),
                hotwords = hotwords,
                recommendKeywords = recommendKeywords,
                keywordSuggestions = keywordSuggestions,
                keywordHistories = keywordHistories,
                matchedKeyworkHistories = matchedKeyworkHistories,
                hotwordsLoading = hotwordsLoading,
                recommendKeywordsLoading = recommendKeywordsLoading,
                hotwordsError = hotwordsError,
                recommendKeywordsError = recommendKeywordsError,
                onSearch = onSearch,
                onDeleteHistory = onDeleteHistory,
                onClearHistories = onClearHistories,
                onRefreshHotwords = onRefreshHotwords,
                onRefreshRecommendKeywords = onRefreshRecommendKeywords,
                onOpenTrendingRanking = onOpenTrendingRanking
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun SearchInputTopBar(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    searchBarState: SearchBarState,
    inputField: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        with(sharedTransitionScope) {
            SearchBar(
                modifier = Modifier
                    .sharedElement(
                        sharedContentState = rememberSharedContentState("searchBar"),
                        animatedVisibilityScope = animatedVisibilityScope
                    ),
                state = searchBarState,
                inputField = inputField
            )
        }
    }
}

@Composable
private fun SearchInputBody(
    windowSize: WindowWidthSizeClass,
    keyword: String,
    hotwords: List<SearchKeyword>,
    recommendKeywords: List<SearchKeyword>,
    keywordSuggestions: List<String>,
    keywordHistories: List<String>,
    matchedKeyworkHistories: List<String>,
    hotwordsLoading: Boolean,
    recommendKeywordsLoading: Boolean,
    hotwordsError: String?,
    recommendKeywordsError: String?,
    onSearch: (String) -> Unit,
    onDeleteHistory: (String) -> Unit,
    onClearHistories: () -> Unit,
    onRefreshHotwords: () -> Unit,
    onRefreshRecommendKeywords: () -> Unit,
    onOpenTrendingRanking: () -> Unit
) {
    if (keyword.isNotBlank()) {
        SearchSuggestionList(
            keywordSuggestions = keywordSuggestions,
            matchedKeyworkHistories = matchedKeyworkHistories,
            onSearch = onSearch,
            onDeleteHistory = onDeleteHistory
        )
        return
    }

    if (windowSize == WindowWidthSizeClass.Compact) {
        SearchDiscoveryList(
            hotwords = hotwords,
            recommendKeywords = recommendKeywords,
            keywordHistories = keywordHistories,
            hotwordsLoading = hotwordsLoading,
            recommendKeywordsLoading = recommendKeywordsLoading,
            hotwordsError = hotwordsError,
            recommendKeywordsError = recommendKeywordsError,
            onSearch = onSearch,
            onDeleteHistory = onDeleteHistory,
            onClearHistories = onClearHistories,
            onRefreshHotwords = onRefreshHotwords,
            onRefreshRecommendKeywords = onRefreshRecommendKeywords,
            onOpenTrendingRanking = onOpenTrendingRanking
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 1120.dp)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SearchDiscoveryList(
                    modifier = Modifier.weight(0.62f),
                    contentPadding = PaddingValues(0.dp),
                    sectionSpacing = 14.dp,
                    hotwords = hotwords,
                    recommendKeywords = recommendKeywords,
                    keywordHistories = emptyList(),
                    hotwordsLoading = hotwordsLoading,
                    recommendKeywordsLoading = recommendKeywordsLoading,
                    hotwordsError = hotwordsError,
                    recommendKeywordsError = recommendKeywordsError,
                    onSearch = onSearch,
                    onDeleteHistory = onDeleteHistory,
                    onClearHistories = onClearHistories,
                    onRefreshHotwords = onRefreshHotwords,
                    onRefreshRecommendKeywords = onRefreshRecommendKeywords,
                    onOpenTrendingRanking = onOpenTrendingRanking
                )
                SearchHistorySection(
                    modifier = Modifier.weight(0.38f),
                    histories = keywordHistories,
                    onSearch = onSearch,
                    onDeleteHistory = onDeleteHistory,
                    onClearHistories = onClearHistories
                )
            }
        }
    }
}

@Composable
private fun SearchSuggestionList(
    keywordSuggestions: List<String>,
    matchedKeyworkHistories: List<String>,
    onSearch: (String) -> Unit,
    onDeleteHistory: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
    ) {
        items(matchedKeyworkHistories.take(10)) { history ->
            SearchSuggestionItem(
                text = history,
                isHistory = true,
                onClick = { onSearch(history) },
                onDelete = { onDeleteHistory(history) }
            )
        }
        items(keywordSuggestions) { suggestion ->
            SearchSuggestionItem(
                text = suggestion,
                isHistory = false,
                onClick = { onSearch(suggestion) }
            )
        }
    }
}

@Composable
private fun SearchSuggestionItem(
    text: String,
    isHistory: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onDelete?.invoke() }
            )
            .padding(horizontal = 4.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = if (isHistory) Icons.Default.Close else Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SearchDiscoveryList(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    sectionSpacing: androidx.compose.ui.unit.Dp = 14.dp,
    hotwords: List<SearchKeyword>,
    recommendKeywords: List<SearchKeyword>,
    keywordHistories: List<String>,
    hotwordsLoading: Boolean,
    recommendKeywordsLoading: Boolean,
    hotwordsError: String?,
    recommendKeywordsError: String?,
    onSearch: (String) -> Unit,
    onDeleteHistory: (String) -> Unit,
    onClearHistories: () -> Unit,
    onRefreshHotwords: () -> Unit,
    onRefreshRecommendKeywords: () -> Unit,
    onOpenTrendingRanking: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
    ) {
        item {
            SearchKeywordSection(
                title = "大家都在搜",
                keywords = hotwords,
                loading = hotwordsLoading,
                error = hotwordsError,
                showRankingButton = true,
                onSearch = onSearch,
                onRefresh = onRefreshHotwords,
                onOpenRanking = onOpenTrendingRanking
            )
        }

        if (keywordHistories.isNotEmpty()) {
            item {
                SearchHistorySection(
                    histories = keywordHistories,
                    onSearch = onSearch,
                    onDeleteHistory = onDeleteHistory,
                    onClearHistories = onClearHistories
                )
            }
        }

        item {
            SearchKeywordSection(
                title = "搜索发现",
                keywords = recommendKeywords,
                loading = recommendKeywordsLoading,
                error = recommendKeywordsError,
                onSearch = onSearch,
                onRefresh = onRefreshRecommendKeywords
            )
        }
    }
}

@Composable
private fun SearchKeywordSection(
    modifier: Modifier = Modifier,
    title: String,
    keywords: List<SearchKeyword>,
    loading: Boolean,
    error: String?,
    showRankingButton: Boolean = false,
    onSearch: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenRanking: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = SearchSectionShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            SearchSectionHeader(
                title = title,
                showRankingButton = showRankingButton,
                onRefresh = onRefresh,
                onOpenRanking = onOpenRanking
            )
            Spacer(modifier = Modifier.height(8.dp))
            when {
                loading && keywords.isEmpty() -> SearchSectionLoading()
                error != null && keywords.isEmpty() -> SearchSectionError(
                    message = error,
                    onRetry = onRefresh
                )
                else -> SearchKeywordGrid(
                    keywords = keywords,
                    onSearch = onSearch
                )
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(
    title: String,
    showRankingButton: Boolean,
    onRefresh: () -> Unit,
    onOpenRanking: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )
        if (showRankingButton) {
            Spacer(modifier = Modifier.width(14.dp))
            TextButton(
                contentPadding = PaddingValues(horizontal = 10.dp),
                onClick = onOpenRanking
            ) {
                Text(
                    text = "完整榜单",
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodySmall
                )
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(
            contentPadding = PaddingValues(horizontal = 10.dp),
            onClick = onRefresh
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "刷新",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SearchSectionLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun SearchSectionError(
    message: String,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall
        )
        TextButton(onClick = onRetry) {
            Text(text = "重试")
        }
    }
}

@Composable
fun SearchKeywordGrid(
    modifier: Modifier = Modifier,
    keywords: List<SearchKeyword>,
    onSearch: (String) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        keywords.chunked(2).forEach { rowKeywords ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowKeywords.forEach { keyword ->
                    SearchKeywordRowItem(
                        modifier = Modifier.weight(1f),
                        keyword = keyword,
                        onSearch = onSearch
                    )
                }
                if (rowKeywords.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SearchKeywordRowItem(
    modifier: Modifier = Modifier,
    keyword: SearchKeyword,
    onSearch: (String) -> Unit
) {
    Row(
        modifier = modifier
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(3.dp))
            .combinedClickable(
                onClick = { onSearch(keyword.keyword) },
                onLongClick = {}
            )
            .padding(start = 8.dp, top = 5.dp, end = 10.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val recommendReason = keyword.recommendReason
        Text(
            text = keyword.displayName,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )
        when {
            keyword.icon != null -> {
                Spacer(modifier = Modifier.width(4.dp))
                AsyncImage(
                    model = keyword.icon,
                    contentDescription = null,
                    modifier = Modifier.height(15.dp),
                    contentScale = ContentScale.FillHeight
                )
            }

            keyword.showLiveIcon -> {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "直播",
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.error)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    color = MaterialTheme.colorScheme.onError,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            recommendReason != null -> {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = recommendReason,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchHistorySection(
    modifier: Modifier = Modifier,
    histories: List<String>,
    onSearch: (String) -> Unit,
    onDeleteHistory: (String) -> Unit,
    onClearHistories: () -> Unit
) {
    var showClearConfirm by remember { mutableStateOf(false) }

    if (histories.isEmpty()) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = SearchSectionShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "搜索历史",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    onClick = { showClearConfirm = true }
                ) {
                    Text(
                        text = "清空",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            SearchHistoryChipFlow(
                histories = histories,
                onSearch = onSearch,
                onDeleteHistory = onDeleteHistory
            )
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(text = "清空搜索历史") },
            text = { Text(text = "确定清空所有搜索历史？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        onClearHistories()
                    }
                ) {
                    Text(text = "确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(text = "取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun SearchHistoryChipFlow(
    histories: List<String>,
    onSearch: (String) -> Unit,
    onDeleteHistory: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        histories.take(20).forEach { history ->
            SearchHistoryChip(
                text = history,
                onSearch = onSearch,
                onDeleteHistory = onDeleteHistory
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchHistoryChip(
    modifier: Modifier = Modifier,
    text: String,
    onSearch: (String) -> Unit,
    onDeleteHistory: (String) -> Unit
) {
    Text(
        text = text,
        modifier = modifier
            .widthIn(max = 220.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .combinedClickable(
                onClick = { onSearch(text) },
                onLongClick = { onDeleteHistory(text) }
            )
            .padding(horizontal = 11.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodyMedium
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchTrendingRankingContent(
    modifier: Modifier = Modifier,
    keywords: List<SearchKeyword>,
    loading: Boolean,
    error: String?,
    onSearch: (String) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (keywords.isEmpty()) onRefresh()
    }

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "大家都在搜") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onRefresh) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "刷新")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            loading && keywords.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            error != null && keywords.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                SearchSectionError(
                    message = error,
                    onRetry = onRefresh
                )
            }

            else -> {
                val listState = rememberLazyListState()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    itemsIndexed(keywords) { index, keyword ->
                        SearchTrendingRankingItem(
                            index = index + 1,
                            keyword = keyword,
                            onSearch = onSearch
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchTrendingRankingItem(
    index: Int,
    keyword: SearchKeyword,
    onSearch: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .combinedClickable(
                onClick = { onSearch(keyword.keyword) },
                onLongClick = {}
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = index.toString(),
            modifier = Modifier.width(28.dp),
            color = when (index) {
                1 -> MaterialTheme.colorScheme.error
                2 -> MaterialTheme.colorScheme.primary
                3 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.outline
            },
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = keyword.displayName,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )
        keyword.icon?.let {
            Spacer(modifier = Modifier.width(8.dp))
            AsyncImage(
                model = it,
                contentDescription = null,
                modifier = Modifier.height(15.dp),
                contentScale = ContentScale.FillHeight
            )
        }
    }
}
