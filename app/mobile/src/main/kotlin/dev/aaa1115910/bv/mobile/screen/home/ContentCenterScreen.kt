package dev.aaa1115910.bv.mobile.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.live.LiveHistoryItem
import dev.aaa1115910.biliapi.entity.live.LiveRoomItem
import dev.aaa1115910.biliapi.entity.pgc.PgcItem
import dev.aaa1115910.biliapi.entity.pgc.PgcType
import dev.aaa1115910.biliapi.entity.ugc.UgcItem
import dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2
import dev.aaa1115910.biliapi.entity.ugc.toSmartDate
import dev.aaa1115910.bv.entity.carddata.SeasonCardData
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.mobile.activities.PgcIndexActivity
import dev.aaa1115910.bv.mobile.activities.SeasonInfoActivity
import dev.aaa1115910.bv.mobile.activities.VideoPlayerActivity
import dev.aaa1115910.bv.mobile.component.videocard.SeasonCard
import dev.aaa1115910.bv.mobile.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.OnBottomReached
import dev.aaa1115910.bv.util.getDisplayName
import dev.aaa1115910.bv.util.removeHtmlTags
import dev.aaa1115910.bv.util.resizedImageUrl
import dev.aaa1115910.bv.viewmodel.live.LiveTabType
import dev.aaa1115910.bv.viewmodel.live.LiveViewModel
import dev.aaa1115910.bv.viewmodel.pgc.FeedListType
import dev.aaa1115910.bv.viewmodel.pgc.PgcAnimeViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcDocumentaryViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcGuoChuangViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcMovieViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcTvViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcVarietyViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcCinephileViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcDougaViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcFoodViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcGameViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcKnowledgeViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcMusicViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcTechViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentCenterScreen(
    modifier: Modifier = Modifier,
    windowSize: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    showTopBar: Boolean = true,
    initialSection: ContentCenterSection = ContentCenterSection.Pgc,
    lockedSection: ContentCenterSection? = null,
    initialPgcType: PgcType = PgcType.Anime,
    initialUgcType: UgcTypeV2 = UgcTypeV2.Douga,
    pgcAnimeViewModel: PgcAnimeViewModel = koinViewModel(),
    pgcGuoChuangViewModel: PgcGuoChuangViewModel = koinViewModel(),
    pgcMovieViewModel: PgcMovieViewModel = koinViewModel(),
    pgcDocumentaryViewModel: PgcDocumentaryViewModel = koinViewModel(),
    pgcTvViewModel: PgcTvViewModel = koinViewModel(),
    pgcVarietyViewModel: PgcVarietyViewModel = koinViewModel(),
    ugcDougaViewModel: UgcDougaViewModel = koinViewModel(),
    ugcGameViewModel: UgcGameViewModel = koinViewModel(),
    ugcMusicViewModel: UgcMusicViewModel = koinViewModel(),
    ugcCinephileViewModel: UgcCinephileViewModel = koinViewModel(),
    ugcKnowledgeViewModel: UgcKnowledgeViewModel = koinViewModel(),
    ugcTechViewModel: UgcTechViewModel = koinViewModel(),
    ugcFoodViewModel: UgcFoodViewModel = koinViewModel(),
    liveViewModel: LiveViewModel = koinViewModel()
) {
    var section by rememberSaveable(lockedSection, initialSection) {
        mutableStateOf(lockedSection ?: initialSection)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text(text = "频道") },
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            if (lockedSection == null) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = section.ordinal,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    ContentCenterSection.entries.forEach { item ->
                        Tab(
                            selected = section == item,
                            onClick = { section = item },
                            text = { Text(text = item.title) }
                        )
                    }
                }
            }

            when (section) {
                ContentCenterSection.Pgc -> PgcChannelPage(
                    initialType = initialPgcType,
                    viewModels = mapOf(
                        PgcType.Anime to pgcAnimeViewModel,
                        PgcType.GuoChuang to pgcGuoChuangViewModel,
                        PgcType.Movie to pgcMovieViewModel,
                        PgcType.Documentary to pgcDocumentaryViewModel,
                        PgcType.Tv to pgcTvViewModel,
                        PgcType.Variety to pgcVarietyViewModel
                    )
                )

                ContentCenterSection.Ugc -> UgcChannelPage(
                    windowSize = windowSize,
                    initialType = initialUgcType,
                    viewModels = mapOf(
                        UgcTypeV2.Douga to ugcDougaViewModel,
                        UgcTypeV2.Game to ugcGameViewModel,
                        UgcTypeV2.Music to ugcMusicViewModel,
                        UgcTypeV2.Cinephile to ugcCinephileViewModel,
                        UgcTypeV2.Knowledge to ugcKnowledgeViewModel,
                        UgcTypeV2.Tech to ugcTechViewModel,
                        UgcTypeV2.Food to ugcFoodViewModel
                    )
                )

                ContentCenterSection.Live -> LiveChannelPage(
                    windowSize = windowSize,
                    liveViewModel = liveViewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PgcChannelPage(
    modifier: Modifier = Modifier,
    initialType: PgcType = PgcType.Anime,
    viewModels: Map<PgcType, PgcViewModel>
) {
    val context = LocalContext.current
    var selectedType by rememberSaveable(initialType) { mutableStateOf(initialType) }
    val pgcTypes = remember(viewModels) { viewModels.keys.toList() }
    val activeViewModel = viewModels.getValue(selectedType)
    val gridState = rememberLazyGridState()

    LaunchedEffect(activeViewModel) {
        if (activeViewModel.feedItems.isEmpty() && activeViewModel.carouselItems.isEmpty()) {
            activeViewModel.init()
        }
    }

    gridState.OnBottomReached(
        loading = activeViewModel.updating || !activeViewModel.hasNext,
        loadMore = activeViewModel::loadMore
    )

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(
            selectedTabIndex = pgcTypes.indexOf(selectedType),
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            pgcTypes.forEach { type ->
                Tab(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    text = { Text(text = type.getDisplayName(context)) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${selectedType.getDisplayName(context)}推荐",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )
            TextButton(
                onClick = { PgcIndexActivity.actionStart(context, selectedType) }
            ) {
                Text(text = "索引")
            }
        }

        val pgcItems = activeViewModel.feedItems
            .flatMap { item ->
                when (item.type) {
                    FeedListType.Ep -> item.items.orEmpty()
                    FeedListType.Rank -> item.rank?.items.orEmpty()
                }
            }
            .distinctBy { it.seasonId }

        if (pgcItems.isEmpty()) {
            LoadingOrEmptyContent(
                loading = activeViewModel.updating,
                emptyText = "暂无内容"
            )
        } else {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                state = gridState,
                columns = GridCells.Adaptive(120.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(pgcItems, key = { it.seasonId }) { item ->
                    SeasonCard(
                        data = item.toSeasonCardData(),
                        onClick = {
                            SeasonInfoActivity.actionStart(
                                context = context,
                                seasonId = item.seasonId
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UgcChannelPage(
    modifier: Modifier = Modifier,
    windowSize: WindowWidthSizeClass,
    initialType: UgcTypeV2 = UgcTypeV2.Douga,
    viewModels: Map<UgcTypeV2, UgcViewModel>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedType by rememberSaveable(initialType) { mutableStateOf(initialType) }
    val ugcTypes = remember(viewModels) { viewModels.keys.toList() }
    val activeViewModel = viewModels.getValue(selectedType)
    val gridState = rememberLazyGridState()
    val isCompact = windowSize == WindowWidthSizeClass.Compact

    LaunchedEffect(activeViewModel) {
        if (activeViewModel.ugcItems.isEmpty()) {
            activeViewModel.loadDataWithDelay(0)
        }
    }

    gridState.OnBottomReached(
        loading = activeViewModel.updating || !activeViewModel.hasMore,
        loadMore = { scope.launch { activeViewModel.loadMore() } }
    )

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(
            selectedTabIndex = ugcTypes.indexOf(selectedType),
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            ugcTypes.forEach { type ->
                Tab(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    text = { Text(text = type.getDisplayName(context)) }
                )
            }
        }

        if (activeViewModel.ugcItems.isEmpty()) {
            LoadingOrEmptyContent(
                loading = activeViewModel.updating,
                emptyText = "暂无内容"
            )
        } else {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                state = gridState,
                columns = GridCells.Fixed(if (isCompact) 2 else 5),
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp),
                contentPadding = PaddingValues(
                    horizontal = if (isCompact) 8.dp else 12.dp,
                    vertical = if (isCompact) 8.dp else 12.dp
                )
            ) {
                items(activeViewModel.ugcItems, key = { it.aid }) { item ->
                    SmallVideoCard(
                        data = item.toVideoCardData(),
                        onClick = {
                            VideoPlayerActivity.actionStart(
                                context = context,
                                aid = item.aid
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveChannelPage(
    modifier: Modifier = Modifier,
    windowSize: WindowWidthSizeClass,
    liveViewModel: LiveViewModel
) {
    val context = LocalContext.current
    val liveTabs = remember {
        listOf(
            LiveTabType.Recommend,
            LiveTabType.Following,
            LiveTabType.History,
            LiveTabType.Area
        )
    }
    val gridState = rememberLazyGridState()
    val currentContentKey = liveViewModel.currentContentKey()
    val rooms = liveViewModel.getCurrentRoomList()
    val histories = liveViewModel.historyList
    val isCompact = windowSize == WindowWidthSizeClass.Compact

    LaunchedEffect(currentContentKey) {
        gridState.scrollToItem(0)
    }

    LaunchedEffect(liveViewModel.currentTabType, liveViewModel.parentAreaGroups.size) {
        if (liveViewModel.currentTabType == LiveTabType.Area &&
            liveViewModel.currentParentGroup == null &&
            liveViewModel.parentAreaGroups.isNotEmpty()
        ) {
            liveViewModel.switchToAreaGroup(liveViewModel.parentAreaGroups.first())
        }
    }

    gridState.OnBottomReached(
        loading = liveViewModel.loading || !liveViewModel.currentHasMore(),
        loadMore = liveViewModel::loadMore
    )

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(
            selectedTabIndex = liveTabs.indexOf(liveViewModel.currentTabType).coerceAtLeast(0),
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            liveTabs.forEach { type ->
                Tab(
                    selected = liveViewModel.currentTabType == type,
                    onClick = {
                        if (type == LiveTabType.Area) {
                            liveViewModel.currentParentGroup?.let(liveViewModel::switchToAreaGroup)
                                ?: liveViewModel.parentAreaGroups.firstOrNull()
                                    ?.let(liveViewModel::switchToAreaGroup)
                                ?: liveViewModel.switchTab(type)
                        } else {
                            liveViewModel.switchTab(type)
                        }
                    },
                    text = { Text(text = type.displayName()) }
                )
            }
        }

        if (liveViewModel.currentTabType == LiveTabType.Area) {
            LiveAreaSelector(liveViewModel = liveViewModel)
        }

        if (liveViewModel.currentTabType == LiveTabType.History && histories.isEmpty()) {
            LoadingOrEmptyContent(
                loading = liveViewModel.loading,
                emptyText = "暂无直播历史"
            )
        } else if (liveViewModel.currentTabType != LiveTabType.History && rooms.isEmpty()) {
            LoadingOrEmptyContent(
                loading = liveViewModel.loading,
                emptyText = liveViewModel.liveEmptyText()
            )
        } else {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                state = gridState,
                columns = GridCells.Fixed(if (isCompact) 2 else 5),
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp),
                contentPadding = PaddingValues(
                    horizontal = if (isCompact) 8.dp else 12.dp,
                    vertical = if (isCompact) 8.dp else 12.dp
                )
            ) {
                if (liveViewModel.currentTabType == LiveTabType.History) {
                    liveHistoryItems(
                        histories = histories,
                        onClickHistory = { history ->
                            VideoPlayerActivity.actionStartLive(
                                context = context,
                                roomId = history.roomId,
                                title = history.title,
                                upName = history.uname,
                                upFace = history.face,
                                upMid = history.uid,
                                watchedNum = 0
                            )
                        }
                    )
                } else {
                    liveRoomItems(
                        rooms = rooms,
                        onClickRoom = { room ->
                            VideoPlayerActivity.actionStartLive(
                                context = context,
                                roomId = room.roomId,
                                title = room.title,
                                upName = room.uname,
                                upFace = room.face,
                                upMid = room.uid,
                                watchedNum = room.watchedShow?.num ?: room.online
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveAreaSelector(
    modifier: Modifier = Modifier,
    liveViewModel: LiveViewModel
) {
    val parentGroups = liveViewModel.parentAreaGroups
    if (parentGroups.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        val selectedParentIndex = parentGroups
            .indexOfFirst { it.id == liveViewModel.currentParentGroup?.id }
            .coerceAtLeast(0)

        PrimaryScrollableTabRow(
            selectedTabIndex = selectedParentIndex,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            parentGroups.forEach { group ->
                Tab(
                    selected = group.id == liveViewModel.currentParentGroup?.id,
                    onClick = { liveViewModel.switchToAreaGroup(group) },
                    text = { Text(text = group.name) }
                )
            }
        }

        val subAreas = liveViewModel.subAreaList
        if (subAreas.isNotEmpty()) {
            val selectedSubIndex = subAreas
                .indexOfFirst { it.id == liveViewModel.currentSubArea?.id }
                .coerceAtLeast(0)

            PrimaryScrollableTabRow(
                selectedTabIndex = selectedSubIndex,
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                subAreas.forEach { area ->
                    Tab(
                        selected = area.id == liveViewModel.currentSubArea?.id,
                        onClick = { liveViewModel.switchSubArea(area) },
                        text = { Text(text = area.name) }
                    )
                }
            }
        }
    }
}

private fun LazyGridScope.liveRoomItems(
    rooms: List<LiveRoomItem>,
    onClickRoom: (LiveRoomItem) -> Unit
) {
    items(rooms, key = { it.roomId }) { room ->
        LiveRoomCard(
            room = room,
            onClick = { onClickRoom(room) }
        )
    }
}

private fun LazyGridScope.liveHistoryItems(
    histories: List<LiveHistoryItem>,
    onClickHistory: (LiveHistoryItem) -> Unit
) {
    items(histories, key = { "${it.roomId}:${it.viewAt}" }) { history ->
        LiveHistoryCard(
            history = history,
            onClick = { onClickHistory(history) }
        )
    }
}

@Composable
private fun LiveHistoryCard(
    modifier: Modifier = Modifier,
    history: LiveHistoryItem,
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
            model = history.cover.resizedImageUrl(ImageSize.SmallVideoCardCover),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = history.title.removeHtmlTags(),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = history.uname.removeHtmlTags(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = history.areaName.ifBlank { if (history.liveStatus == 1) "直播中" else "直播历史" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = history.viewAt.takeIf { it > 0 }?.toSmartDate().orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LiveRoomCard(
    modifier: Modifier = Modifier,
    room: LiveRoomItem,
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
            model = room.coverUrl().resizedImageUrl(ImageSize.SmallVideoCardCover),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = room.title.removeHtmlTags(),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = room.uname.removeHtmlTags(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = room.areaName.ifBlank { room.parentName },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = room.watchedShow?.textSmall?.ifBlank { null } ?: "${formatCount(room.online)}人气",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LoadingOrEmptyContent(
    modifier: Modifier = Modifier,
    loading: Boolean,
    emptyText: String
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator()
        } else {
            Text(
                text = emptyText,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

enum class ContentCenterSection(val title: String) {
    Pgc("PGC"),
    Ugc("分区"),
    Live("直播")
}

private fun PgcItem.toSeasonCardData(): SeasonCardData {
    return SeasonCardData.fromPgcItem(this)
}

private fun UgcItem.toVideoCardData(): VideoCardData {
    return VideoCardData(
        avid = aid,
        title = title.removeHtmlTags(),
        cover = cover,
        upName = author,
        upId = authorId,
        upFace = authorFace,
        play = play,
        danmaku = danmaku,
        time = duration * 1000L,
        pubTime = pubTime,
        isInteractive = isInteractive
    )
}

private fun LiveTabType.displayName(): String = when (this) {
    LiveTabType.Recommend -> "推荐"
    LiveTabType.Following -> "关注"
    LiveTabType.History -> "历史"
    LiveTabType.Area -> "分区"
}

private fun LiveRoomItem.coverUrl(): String {
    return userCover.ifBlank { cover.ifBlank { systemCover } }
}

private fun LiveViewModel.liveEmptyText(): String {
    return when (currentTabType) {
        LiveTabType.Recommend -> "暂无推荐直播"
        LiveTabType.Following -> "暂无关注的 UP 主直播中"
        LiveTabType.History -> "暂无直播历史"
        LiveTabType.Area -> {
            val areaName = currentSubArea?.name ?: currentParentGroup?.name
            if (areaName.isNullOrBlank()) "暂无分区直播" else "暂无${areaName}分区直播"
        }
    }
}

private fun formatCount(count: Int): String = when {
    count >= 100_000_000 -> String.format("%.1f亿", count / 100_000_000.0)
    count >= 10_000 -> String.format("%.1f万", count / 10_000.0)
    else -> count.toString()
}
