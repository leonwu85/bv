package dev.aaa1115910.bv.mobile.screen.home.search

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExpandedDockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.ugc.toSmartDate
import dev.aaa1115910.biliapi.repositories.SearchType
import dev.aaa1115910.biliapi.repositories.SearchTypeResult
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.carddata.SeasonCardData
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.mobile.component.search.UgcListItem
import dev.aaa1115910.bv.mobile.component.user.UserAvatar
import dev.aaa1115910.bv.mobile.component.videocard.SeasonCard
import dev.aaa1115910.bv.mobile.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.mobile.screen.home.SearchBarResultContent
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.OnBottomReached
import dev.aaa1115910.bv.util.removeHtmlTags
import dev.aaa1115910.bv.util.resizedImageUrl

@OptIn(
    ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3WindowSizeClassApi::class
)
@Composable
fun SearchResultContent(
    modifier: Modifier = Modifier,
    searchBarState: SearchBarState = rememberSearchBarState(),
    textFieldState: TextFieldState = rememberTextFieldState(),
    keywordSuggestions: List<String>,
    historyKeywords: List<String>,
    matchedHistory: List<String>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    inputField: @Composable () -> Unit = {},
    videoSearchResult: List<SearchTypeResult.Video>,
    mediaBangumiSearchResult: List<SearchTypeResult.Pgc>,
    mediaFtSearchResult: List<SearchTypeResult.Pgc>,
    biliUserSearchResult: List<SearchTypeResult.User>,
    liveRoomSearchResult: List<SearchTypeResult.LiveRoom>,
    onSearch: (String) -> Unit,
    onDeleteHistory: (String) -> Unit = {},
    onBackToSearchInput: () -> Unit,
    searchType: SearchType,
    isLoading: Boolean,
    onSearchTypeChange: (SearchType) -> Unit,
    onLoadMore: (SearchType) -> Unit,
    onOpenUgc: (Long) -> Unit,
    onOpenPgc: (Int) -> Unit,
    onOpenUser: (SearchTypeResult.User) -> Unit,
    onOpenLiveRoom: (SearchTypeResult.LiveRoom) -> Unit
) {
    val context = LocalContext.current
    val windowSize = calculateWindowSizeClass(context as Activity).widthSizeClass

    BackHandler(onBack = onBackToSearchInput)

    Scaffold(
        modifier = modifier,
        topBar = {
            when (windowSize) {
                WindowWidthSizeClass.Compact -> {
                    Column(modifier = Modifier.statusBarsPadding()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBackToSearchInput) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                with(sharedTransitionScope) {
                                    SearchBar(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
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
                        SearchTypeTabRow(
                            searchType = searchType,
                            onSearchTypeChange = onSearchTypeChange
                        )
                    }
                }

                else -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .statusBarsPadding()
                    ) {
                        IconButton(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(start = 16.dp),
                            onClick = onBackToSearchInput
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                        with(sharedTransitionScope) {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp, horizontal = 16.dp)
                                    .sharedElement(
                                        sharedContentState = rememberSharedContentState("dockedSearchBar"),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                            ) {
                                SearchBar(
                                    state = searchBarState,
                                    inputField = inputField
                                )
                                ExpandedDockedSearchBar(
                                    state = searchBarState,
                                    inputField = inputField
                                ) {
                                    SearchBarResultContent(
                                        keyword = textFieldState.text.toString(),
                                        recentHistory = historyKeywords,
                                        matchedHistory = matchedHistory,
                                        suggestions = keywordSuggestions,
                                        onSearch = onSearch,
                                        onDeleteHistory = onDeleteHistory
                                    )
                                }
                            }
                        }

                        SearchTypeTabRow(
                            modifier = Modifier.align(Alignment.Bottom),
                            searchType = searchType,
                            onSearchTypeChange = onSearchTypeChange,
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    }
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
            shape = if (windowSize == WindowWidthSizeClass.Compact) {
                RoundedCornerShape(0.dp)
            } else {
                MaterialTheme.shapes.large
            },
        ) {
            when (searchType) {
                SearchType.Video -> VideoSearchResult(
                    videoList = videoSearchResult,
                    isLoading = isLoading,
                    onLoadMore = { onLoadMore(SearchType.Video) },
                    onClickVideo = onOpenUgc
                )

                SearchType.MediaBangumi -> PgcSearchResult(
                    pgcList = mediaBangumiSearchResult,
                    isLoading = isLoading,
                    onLoadMore = { onLoadMore(SearchType.MediaBangumi) },
                    onClickPgc = onOpenPgc
                )

                SearchType.MediaFt -> PgcSearchResult(
                    pgcList = mediaFtSearchResult,
                    isLoading = isLoading,
                    onLoadMore = { onLoadMore(SearchType.MediaFt) },
                    onClickPgc = onOpenPgc
                )

                SearchType.BiliUser -> BiliUserSearchResult(
                    biliUserList = biliUserSearchResult,
                    isLoading = isLoading,
                    onLoadMore = { onLoadMore(SearchType.BiliUser) },
                    onClickUser = onOpenUser
                )

                SearchType.LiveRoom -> LiveRoomSearchResult(
                    liveRoomList = liveRoomSearchResult,
                    isLoading = isLoading,
                    onLoadMore = { onLoadMore(SearchType.LiveRoom) },
                    onClickLiveRoom = onOpenLiveRoom
                )
            }
        }
    }
}

@Composable
private fun SearchTypeTabRow(
    modifier: Modifier = Modifier,
    searchType: SearchType,
    onSearchTypeChange: (SearchType) -> Unit,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainer
) {
    PrimaryScrollableTabRow(
        modifier = modifier,
        selectedTabIndex = searchType.ordinal,
        containerColor = containerColor
    ) {
        SearchType.entries.forEachIndexed { index, type ->
            Tab(
                selected = searchType.ordinal == index,
                onClick = { onSearchTypeChange(type) },
                text = { Text(text = type.displayName()) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
private fun VideoSearchResult(
    modifier: Modifier = Modifier,
    videoList: List<SearchTypeResult.Video>,
    isLoading: Boolean,
    onLoadMore: () -> Unit,
    onClickVideo: (aid: Long) -> Unit
) {
    val context = LocalContext.current
    val windowSize = calculateWindowSizeClass(context as Activity).widthSizeClass

    when (windowSize) {
        WindowWidthSizeClass.Compact -> {
            if (videoList.isEmpty()) {
                SearchResultEmptyContent(
                    modifier = modifier,
                    isLoading = isLoading
                )
                return
            }

            val listState = rememberLazyListState()
            listState.OnBottomReached(loading = isLoading, loadMore = onLoadMore)
            LazyColumn(
                modifier = modifier,
                state = listState
            ) {
                items(videoList) { video ->
                    UgcListItem(
                        data = VideoCardData(
                            avid = video.aid,
                            bvid = video.bvid,
                            title = video.title.removeHtmlTags(),
                            cover = video.cover,
                            play = video.play,
                            danmaku = video.danmaku,
                            upName = video.author,
                            upId = video.upId,
                            upFace = video.upFace,
                            time = video.duration * 1000L,
                            pubTime = video.pubDate.toLong().toSmartDate()
                        ),
                        onClick = { onClickVideo(video.aid) }
                    )
                }
            }
        }

        else -> {
            if (videoList.isEmpty()) {
                SearchResultEmptyContent(
                    modifier = modifier,
                    isLoading = isLoading
                )
                return
            }

            val gridState = rememberLazyGridState()
            gridState.OnBottomReached(loading = isLoading, loadMore = onLoadMore)
            LazyVerticalGrid(
                modifier = modifier,
                state = gridState,
                columns = GridCells.Adaptive(220.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(videoList) { video ->
                    SmallVideoCard(
                        data = VideoCardData(
                            avid = video.aid,
                            bvid = video.bvid,
                            title = video.title.removeHtmlTags(),
                            cover = video.cover,
                            play = video.play,
                            danmaku = video.danmaku,
                            upName = video.author,
                            upId = video.upId,
                            upFace = video.upFace,
                            time = video.duration * 1000L,
                            pubTime = video.pubDate.toLong().toSmartDate()
                        ),
                        onClick = { onClickVideo(video.aid) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
private fun PgcSearchResult(
    modifier: Modifier = Modifier,
    pgcList: List<SearchTypeResult.Pgc>,
    isLoading: Boolean,
    onLoadMore: () -> Unit,
    onClickPgc: (seasonId: Int) -> Unit
) {
    val context = LocalContext.current
    val windowSize = calculateWindowSizeClass(context as Activity).widthSizeClass

    when (windowSize) {
        WindowWidthSizeClass.Compact -> {
            if (pgcList.isEmpty()) {
                SearchResultEmptyContent(
                    modifier = modifier,
                    isLoading = isLoading
                )
                return
            }

            val listState = rememberLazyListState()
            listState.OnBottomReached(loading = isLoading, loadMore = onLoadMore)
            LazyColumn(
                modifier = modifier,
                state = listState
            ) {
                itemsIndexed(
                    items = pgcList,
                    key = { index, pgc -> "$index:${pgc.seasonId}" }
                ) { _, pgc ->
                    PgcListItem(
                        pgc = pgc,
                        onClick = { onClickPgc(pgc.seasonId) }
                    )
                }
            }
        }

        else -> {
            if (pgcList.isEmpty()) {
                SearchResultEmptyContent(
                    modifier = modifier,
                    isLoading = isLoading
                )
                return
            }

            val gridState = rememberLazyGridState()
            gridState.OnBottomReached(loading = isLoading, loadMore = onLoadMore)
            LazyVerticalGrid(
                modifier = modifier,
                state = gridState,
                columns = GridCells.Adaptive(120.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                itemsIndexed(pgcList, key = { index, pgc -> "$index:${pgc.seasonId}" }) { _, pgc ->
                    SeasonCard(
                        data = SeasonCardData(
                            seasonId = pgc.seasonId,
                            title = pgc.title.removeHtmlTags(),
                            cover = pgc.cover,
                            rating = pgc.star.takeIf { it > 0f }?.let { String.format("%.1f", it) }
                        ),
                        onClick = { onClickPgc(pgc.seasonId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BiliUserSearchResult(
    modifier: Modifier = Modifier,
    biliUserList: List<SearchTypeResult.User>,
    isLoading: Boolean,
    onLoadMore: () -> Unit,
    onClickUser: (SearchTypeResult.User) -> Unit
) {
    if (biliUserList.isEmpty()) {
        SearchResultEmptyContent(
            modifier = modifier,
            isLoading = isLoading
        )
        return
    }

    val listState = rememberLazyListState()
    listState.OnBottomReached(loading = isLoading, loadMore = onLoadMore)

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        itemsIndexed(
            items = biliUserList,
            key = { index, user -> "$index:${user.mid}" }
        ) { _, user ->
            UserSearchListItem(
                user = user,
                onClick = { onClickUser(user) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
private fun LiveRoomSearchResult(
    modifier: Modifier = Modifier,
    liveRoomList: List<SearchTypeResult.LiveRoom>,
    isLoading: Boolean,
    onLoadMore: () -> Unit,
    onClickLiveRoom: (SearchTypeResult.LiveRoom) -> Unit
) {
    val context = LocalContext.current
    val windowSize = calculateWindowSizeClass(context as Activity).widthSizeClass

    when (windowSize) {
        WindowWidthSizeClass.Compact -> {
            if (liveRoomList.isEmpty()) {
                SearchResultEmptyContent(
                    modifier = modifier,
                    isLoading = isLoading
                )
                return
            }

            val listState = rememberLazyListState()
            listState.OnBottomReached(loading = isLoading, loadMore = onLoadMore)
            LazyColumn(
                modifier = modifier,
                state = listState
            ) {
                itemsIndexed(
                    items = liveRoomList,
                    key = { index, room -> "$index:${room.roomId}" }
                ) { _, room ->
                    LiveRoomListItem(
                        room = room,
                        onClick = { onClickLiveRoom(room) }
                    )
                }
            }
        }

        else -> {
            if (liveRoomList.isEmpty()) {
                SearchResultEmptyContent(
                    modifier = modifier,
                    isLoading = isLoading
                )
                return
            }

            val gridState = rememberLazyGridState()
            gridState.OnBottomReached(loading = isLoading, loadMore = onLoadMore)
            LazyVerticalGrid(
                modifier = modifier,
                state = gridState,
                columns = GridCells.Adaptive(240.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                itemsIndexed(liveRoomList, key = { index, room -> "$index:${room.roomId}" }) { _, room ->
                    LiveRoomGridCard(
                        room = room,
                        onClick = { onClickLiveRoom(room) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultEmptyContent(
    modifier: Modifier = Modifier,
    isLoading: Boolean
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Column(
                modifier = Modifier.widthIn(max = 280.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            modifier = Modifier.size(26.dp),
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Text(
                    text = "没有找到相关内容",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "换个关键词或分类试试看",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun PgcListItem(
    modifier: Modifier = Modifier,
    pgc: SearchTypeResult.Pgc,
    onClick: () -> Unit
) {
    Surface(onClick = onClick) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(132.dp)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(0.75f)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                model = pgc.cover.resizedImageUrl(ImageSize.SeasonCoverThumbnail),
                contentDescription = null,
                contentScale = ContentScale.FillBounds
            )
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = pgc.title.removeHtmlTags(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (pgc.star > 0f) {
                    Text(
                        text = String.format("评分 %.1f", pgc.star),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun UserSearchListItem(
    modifier: Modifier = Modifier,
    user: SearchTypeResult.User,
    onClick: () -> Unit
) {
    Surface(onClick = onClick) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UserAvatar(
                size = 56.dp,
                avatar = user.avatar
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = user.name.removeHtmlTags(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = user.sign.ifBlank { "这个人还没有签名" }.removeHtmlTags(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LiveRoomListItem(
    modifier: Modifier = Modifier,
    room: SearchTypeResult.LiveRoom,
    onClick: () -> Unit
) {
    Surface(onClick = onClick) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(104.dp)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AsyncImage(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1.6f)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                model = room.cover.resizedImageUrl(ImageSize.SmallVideoCardCover),
                contentDescription = null,
                contentScale = ContentScale.FillBounds
            )
            LiveRoomTextContent(
                modifier = Modifier.weight(1f),
                room = room
            )
        }
    }
}

@Composable
private fun LiveRoomGridCard(
    modifier: Modifier = Modifier,
    room: SearchTypeResult.LiveRoom,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            model = room.cover.resizedImageUrl(ImageSize.SmallVideoCardCover),
            contentDescription = null,
            contentScale = ContentScale.FillBounds
        )
        LiveRoomTextContent(
            modifier = Modifier.padding(10.dp),
            room = room
        )
    }
}

@Composable
private fun LiveRoomTextContent(
    modifier: Modifier = Modifier,
    room: SearchTypeResult.LiveRoom
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = room.title.removeHtmlTags(),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = room.uname.removeHtmlTags(),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = room.cateName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatCount(room.online) + "人气",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SearchType.displayName(): String = when (this) {
    SearchType.Video -> stringResource(R.string.search_result_type_name_video)
    SearchType.MediaBangumi -> stringResource(R.string.search_result_type_name_media_bangumi)
    SearchType.MediaFt -> stringResource(R.string.search_result_type_name_media_ft)
    SearchType.BiliUser -> stringResource(R.string.search_result_type_name_bili_user)
    SearchType.LiveRoom -> stringResource(R.string.search_result_type_name_live_room)
}

private fun formatCount(count: Int): String = when {
    count >= 100_000_000 -> String.format("%.1f亿", count / 100_000_000.0)
    count >= 10_000 -> String.format("%.1f万", count / 10_000.0)
    else -> count.toString()
}
