package dev.aaa1115910.bv.mobile.screen.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.live.LiveHistoryItem
import dev.aaa1115910.biliapi.entity.live.LiveRoomItem
import dev.aaa1115910.biliapi.entity.pgc.PgcItem
import dev.aaa1115910.biliapi.entity.pgc.PgcType
import dev.aaa1115910.biliapi.entity.season.FollowingSeason
import dev.aaa1115910.biliapi.entity.season.Timeline
import dev.aaa1115910.biliapi.entity.season.TimelineEp
import dev.aaa1115910.biliapi.entity.ugc.UgcItem
import dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2
import dev.aaa1115910.biliapi.entity.ugc.toSmartDate
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.mobile.activities.PgcIndexActivity
import dev.aaa1115910.bv.mobile.activities.SeasonInfoActivity
import dev.aaa1115910.bv.mobile.activities.VideoPlayerActivity
import dev.aaa1115910.bv.mobile.component.MobileTabRow
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.OnBottomReached
import dev.aaa1115910.bv.util.getDisplayName
import dev.aaa1115910.bv.util.removeHtmlTags
import dev.aaa1115910.bv.util.resizedImageUrl
import dev.aaa1115910.bv.util.scrollToItemIfAvailable
import dev.aaa1115910.bv.viewmodel.live.LiveTabType
import dev.aaa1115910.bv.viewmodel.live.LiveViewModel
import dev.aaa1115910.bv.viewmodel.home.PopularViewModel
import dev.aaa1115910.bv.viewmodel.home.RankViewModel
import dev.aaa1115910.bv.viewmodel.pgc.FeedListType
import dev.aaa1115910.bv.viewmodel.pgc.PgcAnimeViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcDocumentaryViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcGuoChuangViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcHomeViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcMovieViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcTvViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcVarietyViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcAnimalViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcCarViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcCinephileViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcDanceViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcDougaViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcEntViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcFashionViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcFoodViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcGameViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcKnowledgeViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcKichikuViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcMusicViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcSportsViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcTechViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcViewModel
import kotlin.math.ceil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    initialUgcType: UgcTypeV2? = null,
    pgcAnimeViewModel: PgcAnimeViewModel = koinViewModel(),
    pgcGuoChuangViewModel: PgcGuoChuangViewModel = koinViewModel(),
    pgcMovieViewModel: PgcMovieViewModel = koinViewModel(),
    pgcDocumentaryViewModel: PgcDocumentaryViewModel = koinViewModel(),
    pgcTvViewModel: PgcTvViewModel = koinViewModel(),
    pgcVarietyViewModel: PgcVarietyViewModel = koinViewModel(),
    pgcHomeViewModel: PgcHomeViewModel = koinViewModel(),
    ugcDougaViewModel: UgcDougaViewModel = koinViewModel(),
    ugcGameViewModel: UgcGameViewModel = koinViewModel(),
    ugcMusicViewModel: UgcMusicViewModel = koinViewModel(),
    ugcDanceViewModel: UgcDanceViewModel = koinViewModel(),
    ugcCinephileViewModel: UgcCinephileViewModel = koinViewModel(),
    ugcEntViewModel: UgcEntViewModel = koinViewModel(),
    ugcKnowledgeViewModel: UgcKnowledgeViewModel = koinViewModel(),
    ugcTechViewModel: UgcTechViewModel = koinViewModel(),
    ugcSportsViewModel: UgcSportsViewModel = koinViewModel(),
    ugcCarViewModel: UgcCarViewModel = koinViewModel(),
    ugcFoodViewModel: UgcFoodViewModel = koinViewModel(),
    ugcAnimalViewModel: UgcAnimalViewModel = koinViewModel(),
    ugcKichikuViewModel: UgcKichikuViewModel = koinViewModel(),
    ugcFashionViewModel: UgcFashionViewModel = koinViewModel(),
    popularViewModel: PopularViewModel = koinViewModel(),
    rankViewModel: RankViewModel = koinViewModel(),
    liveViewModel: LiveViewModel = koinViewModel()
) {
    var section by rememberSaveable(lockedSection, initialSection) {
        mutableStateOf(lockedSection ?: initialSection)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = if (showTopBar) {
            ScaffoldDefaults.contentWindowInsets
        } else {
            WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
        },
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text(text = "频道") },
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (lockedSection == null) {
                MobileTabRow(
                    selectedTabIndex = section.ordinal,
                    tabs = ContentCenterSection.entries.map { it.title },
                    onTabSelected = { index -> section = ContentCenterSection.entries[index] },
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }

            when (section) {
                ContentCenterSection.Pgc -> PgcChannelPage(
                    initialType = initialPgcType,
                    showTypeTabs = lockedSection == null,
                    homeViewModel = pgcHomeViewModel,
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
                    rankItems = listOf(
                        RankContentItem.Rank("全站", rankViewModel),
                        RankContentItem.Pgc("番剧", PgcType.Anime, pgcAnimeViewModel),
                        RankContentItem.Pgc("国创", PgcType.GuoChuang, pgcGuoChuangViewModel),
                        RankContentItem.Ugc(UgcTypeV2.Douga, ugcDougaViewModel),
                        RankContentItem.Ugc(UgcTypeV2.Music, ugcMusicViewModel),
                        RankContentItem.Ugc(UgcTypeV2.Dance, ugcDanceViewModel),
                        RankContentItem.Ugc(UgcTypeV2.Game, ugcGameViewModel),
                        RankContentItem.Ugc(UgcTypeV2.Knowledge, ugcKnowledgeViewModel),
                        RankContentItem.Ugc(UgcTypeV2.Tech, ugcTechViewModel),
                        RankContentItem.Ugc(UgcTypeV2.Sports, ugcSportsViewModel),
                        RankContentItem.Ugc(UgcTypeV2.Car, ugcCarViewModel),
                        RankContentItem.Ugc(UgcTypeV2.Food, ugcFoodViewModel),
                        RankContentItem.Ugc(UgcTypeV2.Animal, ugcAnimalViewModel),
                        RankContentItem.Ugc(UgcTypeV2.Kichiku, ugcKichikuViewModel),
                        RankContentItem.Ugc(UgcTypeV2.Fashion, ugcFashionViewModel),
                        RankContentItem.Ugc(UgcTypeV2.Ent, ugcEntViewModel),
                        RankContentItem.Ugc(UgcTypeV2.Cinephile, ugcCinephileViewModel),
                        RankContentItem.Pgc("记录", PgcType.Documentary, pgcDocumentaryViewModel),
                        RankContentItem.Pgc("电影", PgcType.Movie, pgcMovieViewModel),
                        RankContentItem.Pgc("剧集", PgcType.Tv, pgcTvViewModel),
                        RankContentItem.Pgc("综艺", PgcType.Variety, pgcVarietyViewModel)
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
    showTypeTabs: Boolean,
    homeViewModel: PgcHomeViewModel,
    viewModels: Map<PgcType, PgcViewModel>
) {
    val context = LocalContext.current
    var selectedType by rememberSaveable(initialType) { mutableStateOf(initialType) }
    val pgcTypes = remember(viewModels) { viewModels.keys.toList() }
    val activeViewModel = viewModels.getValue(selectedType)
    val gridState = rememberLazyGridState()
    val showBangumiSections = selectedType == PgcType.Anime

    LaunchedEffect(activeViewModel) {
        if (activeViewModel.feedItems.isEmpty() && activeViewModel.carouselItems.isEmpty()) {
            activeViewModel.init()
        }
    }

    LaunchedEffect(showBangumiSections) {
        if (showBangumiSections) {
            homeViewModel.refresh()
        }
    }

    gridState.OnBottomReached(
        loading = activeViewModel.updating || !activeViewModel.hasNext,
        loadMore = activeViewModel::loadMore
    )

    val pgcItems = activeViewModel.feedItems
        .flatMap { item ->
            when (item.type) {
                FeedListType.Ep -> item.items.orEmpty()
                FeedListType.Rank -> item.rank?.items.orEmpty()
            }
        }
        .distinctBy { it.seasonId }

    Column(modifier = modifier.fillMaxSize()) {
        if (showTypeTabs) {
            MobileTabRow(
                selectedTabIndex = pgcTypes.indexOf(selectedType),
                tabs = pgcTypes.map { it.getDisplayName(context) },
                onTabSelected = { index -> selectedType = pgcTypes[index] },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        MaxExtentGrid(
            modifier = Modifier.fillMaxSize(),
            state = gridState,
            maxCrossAxisExtent = HomeContentStyle.PgcCardMaxWidth,
            childAspectRatio = HomeContentStyle.PgcCoverAspectRatio,
            mainAxisExtent = HomeContentStyle.PgcTextExtent,
            mainAxisSpacing = HomeContentStyle.CardSpace,
            crossAxisSpacing = HomeContentStyle.CardSpace,
            contentPadding = PaddingValues(
                start = HomeContentStyle.SafeSpace,
                top = 0.dp,
                end = HomeContentStyle.SafeSpace,
                bottom = HomeContentStyle.BottomContentPadding
            )
        ) { itemHeight ->
            if (showBangumiSections && homeViewModel.isLogin) {
                item(
                    key = "pgc-following",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    FollowingSeasonSection(
                        seasons = homeViewModel.followingSeasons,
                        total = homeViewModel.followingTotal,
                        loading = homeViewModel.followingLoading,
                        onRefresh = homeViewModel::refreshFollowing,
                        onLoadMore = homeViewModel::loadMoreFollowing,
                        onClickSeason = { season ->
                            SeasonInfoActivity.actionStart(
                                context = context,
                                seasonId = season.seasonId
                            )
                        }
                    )
                }
            }

            if (showBangumiSections && (homeViewModel.timelines.isNotEmpty() || homeViewModel.timelineLoading)) {
                item(
                    key = "pgc-timeline",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    TimelineSection(
                        timelines = homeViewModel.timelines,
                        loading = homeViewModel.timelineLoading,
                        onClickEpisode = { episode ->
                            SeasonInfoActivity.actionStart(
                                context = context,
                                epId = episode.episodeId,
                                seasonId = episode.seasonId
                            )
                        }
                    )
                }
            }

            if (pgcItems.isEmpty()) {
                item(
                    key = "pgc-recommend-empty",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    Column {
                        SectionTitle(
                            title = "推荐",
                            onMoreClick = { PgcIndexActivity.actionStart(context, selectedType) }
                        )
                        LoadingOrEmptyContent(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            loading = activeViewModel.updating,
                            emptyText = "暂无内容"
                        )
                    }
                }
            } else {
                item(
                    key = "pgc-recommend-title",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    SectionTitle(
                        title = "推荐",
                        onMoreClick = { PgcIndexActivity.actionStart(context, selectedType) }
                    )
                }
                itemsIndexed(pgcItems, key = { index, item -> "$index:${item.seasonId}" }) { _, item ->
                    PgcCard(
                        modifier = Modifier.height(itemHeight),
                        item = item,
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

@Composable
private fun FollowingSeasonSection(
    seasons: List<FollowingSeason>,
    total: Int,
    loading: Boolean,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onClickSeason: (FollowingSeason) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = buildString {
                    append("最近追番")
                    if (total >= 0) append(" $total")
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                modifier = Modifier.size(40.dp),
                onClick = onRefresh
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null
                )
            }
        }

        if (seasons.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HomeContentStyle.PgcRailHeight),
                contentAlignment = Alignment.Center
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                } else {
                    Text(
                        text = "还没有追番",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            val rowState = rememberLazyListState()
            rowState.OnBottomReached(
                loading = loading,
                loadMore = onLoadMore
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HomeContentStyle.PgcRailHeight),
                state = rowState,
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                items(
                    items = seasons,
                    key = { it.seasonId }
                ) { season ->
                    FollowingSeasonCard(
                        modifier = Modifier
                            .width(HomeContentStyle.PgcRailCardWidth)
                            .padding(end = HomeContentStyle.SafeSpace),
                        season = season,
                        onClick = { onClickSeason(season) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineSection(
    timelines: List<Timeline>,
    loading: Boolean,
    onClickEpisode: (TimelineEp) -> Unit
) {
    var selectedIndex by rememberSaveable(timelines.size) {
        mutableIntStateOf(timelines.indexOfFirst { it.isToday }.coerceAtLeast(0))
    }

    LaunchedEffect(timelines.size) {
        if (timelines.isNotEmpty()) {
            val todayIndex = timelines.indexOfFirst { it.isToday }.coerceAtLeast(0)
            selectedIndex = selectedIndex.coerceIn(0, timelines.lastIndex)
            if (selectedIndex == 0) selectedIndex = todayIndex
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(HomeContentStyle.TimelineSectionHeight)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(start = 4.dp, end = 12.dp),
                text = "追番时间表",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (timelines.isNotEmpty()) {
                TimelineTabRow(
                    modifier = Modifier.weight(1f),
                    selectedTabIndex = selectedIndex,
                    timelines = timelines,
                    onTabSelected = { selectedIndex = it }
                )
            }
        }

        when {
            loading && timelines.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }

            timelines.isEmpty() -> Unit

            else -> {
                val episodes = timelines.getOrNull(selectedIndex)?.episodes.orEmpty()
                if (episodes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "当天暂无放送",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(
                            items = episodes,
                            key = { "${it.seasonId}:${it.episodeId}" }
                        ) { episode ->
                            TimelineCard(
                                modifier = Modifier
                                    .width(HomeContentStyle.PgcRailCardWidth)
                                    .padding(end = HomeContentStyle.SafeSpace),
                                episode = episode,
                                onClick = { onClickEpisode(episode) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineTabRow(
    selectedTabIndex: Int,
    timelines: List<Timeline>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(selectedTabIndex, timelines.size) {
        if (selectedTabIndex in timelines.indices) {
            listState.animateScrollToItem(selectedTabIndex)
        }
    }

    LazyRow(
        modifier = modifier.height(42.dp),
        state = listState,
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(
            items = timelines,
            key = { index, timeline -> "$index:${timeline.dateString}" }
        ) { index, timeline ->
            val selected = selectedTabIndex == index
            Text(
                modifier = Modifier
                    .clip(HomeContentStyle.TimelineChipShape)
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            Color.Transparent
                        }
                    )
                    .clickable { onTabSelected(index) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                text = "${timeline.dateString} ${timeline.weekDisplay()}",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun FollowingSeasonCard(
    modifier: Modifier = Modifier,
    season: FollowingSeason,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = HomeContentStyle.MdShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(HomeContentStyle.PgcCoverAspectRatio)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = season.cover.resizedImageUrl(ImageSize.SeasonCoverThumbnail),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
                CardBadge(
                    text = season.badge,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp),
                    type = CardBadgeType.Primary
                )
                if (!season.isFinish) {
                    CardBadge(
                        text = season.renewalTime,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 6.dp, bottom = 6.dp),
                        type = CardBadgeType.Gray
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp, top = 5.dp, end = 0.dp, bottom = 3.dp)
            ) {
                Text(
                    text = season.title.removeHtmlTags(),
                    style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.3.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = season.progress.ifBlank { season.newEpIndexShow }.removeHtmlTags(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TimelineCard(
    modifier: Modifier = Modifier,
    episode: TimelineEp,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = HomeContentStyle.MdShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(HomeContentStyle.PgcCoverAspectRatio)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = episode.cover.resizedImageUrl(ImageSize.SeasonCoverThumbnail),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
                if (episode.isFollowing) {
                    CardBadge(
                        text = "已追番",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 6.dp),
                        type = CardBadgeType.Primary
                    )
                }
                CardBadge(
                    text = episode.publishTime,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 6.dp, bottom = 6.dp),
                    type = CardBadgeType.Gray
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp, top = 5.dp, end = 0.dp, bottom = 3.dp)
            ) {
                Text(
                    text = episode.title.removeHtmlTags(),
                    style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.3.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = episode.publishIndex.removeHtmlTags(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UgcChannelPage(
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") windowSize: WindowWidthSizeClass,
    initialType: UgcTypeV2? = null,
    rankItems: List<RankContentItem>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initialIndex = remember(initialType, rankItems) {
        initialType?.let { type ->
            rankItems.indexOfFirst { item ->
                item is RankContentItem.Ugc && item.type == type
            }.coerceAtLeast(0)
        } ?: 0
    }
    var selectedIndex by rememberSaveable(initialType, rankItems.size) { mutableStateOf(initialIndex) }
    val selectedItem = rankItems.getOrElse(selectedIndex) { rankItems.first() }
    val gridState = rememberLazyGridState()

    LaunchedEffect(selectedItem) {
        when (selectedItem) {
            is RankContentItem.Rank -> {
                if (selectedItem.viewModel.allRankVideoList.isEmpty()) {
                    withContext(Dispatchers.IO) {
                        selectedItem.viewModel.loadAllRank()
                    }
                }
            }
            is RankContentItem.Popular -> {
                if (selectedItem.viewModel.popularVideoList.isEmpty()) {
                    withContext(Dispatchers.IO) {
                        selectedItem.viewModel.loadMore()
                    }
                }
            }
            is RankContentItem.Pgc -> {
                if (selectedItem.viewModel.feedItems.isEmpty() && selectedItem.viewModel.carouselItems.isEmpty()) {
                    selectedItem.viewModel.init()
                }
            }
            is RankContentItem.Ugc -> {
                if (selectedItem.viewModel.ugcItems.isEmpty()) {
                    selectedItem.viewModel.loadDataWithDelay(0)
                }
            }
        }
    }

    LaunchedEffect(selectedIndex) {
        gridState.scrollToItemIfAvailable(0)
    }

    val loading = when (selectedItem) {
        is RankContentItem.Rank -> selectedItem.viewModel.loading
        is RankContentItem.Popular -> selectedItem.viewModel.loading
        is RankContentItem.Pgc -> selectedItem.viewModel.updating
        is RankContentItem.Ugc -> selectedItem.viewModel.updating
    }
    val hasMore = when (selectedItem) {
        is RankContentItem.Rank -> false
        is RankContentItem.Popular -> true
        is RankContentItem.Pgc -> selectedItem.viewModel.hasNext
        is RankContentItem.Ugc -> selectedItem.viewModel.hasMore
    }

    gridState.OnBottomReached(
        loading = loading || !hasMore,
        loadMore = {
            when (selectedItem) {
                is RankContentItem.Rank -> Unit
                is RankContentItem.Popular -> scope.launch(Dispatchers.IO) { selectedItem.viewModel.loadMore() }
                is RankContentItem.Pgc -> selectedItem.viewModel.loadMore()
                is RankContentItem.Ugc -> scope.launch { selectedItem.viewModel.loadMore() }
            }
        }
    )

    Row(modifier = modifier.fillMaxSize()) {
        VerticalTabBar(
            selectedTabIndex = selectedIndex,
            tabs = rankItems.map { it.label(context) },
            onTabSelected = { index -> selectedIndex = index },
            modifier = Modifier.fillMaxHeight()
        )

        val videos = selectedItem.videoItems()
        val pgcItems = selectedItem.pgcItems()

        if (videos.isEmpty() && pgcItems.isEmpty()) {
            LoadingOrEmptyContent(
                modifier = Modifier.weight(1f),
                loading = loading,
                emptyText = "暂无内容"
            )
        } else if (pgcItems.isNotEmpty()) {
            PgcRankGrid(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                state = gridState,
                items = pgcItems,
                onClick = { item ->
                    SeasonInfoActivity.actionStart(
                        context = context,
                        seasonId = item.seasonId
                    )
                }
            )
        } else {
            VideoRankGrid(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                state = gridState,
                items = videos,
                onClick = { item ->
                    VideoPlayerActivity.actionStart(
                        context = context,
                        video = item
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveChannelPage(
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") windowSize: WindowWidthSizeClass,
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

    LaunchedEffect(currentContentKey) {
        gridState.scrollToItemIfAvailable(0)
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

    LaunchedEffect(Unit) {
        if (liveViewModel.followingList.isEmpty()) {
            liveViewModel.loadFollowing(refresh = true)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        MobileTabRow(
            selectedTabIndex = liveTabs.indexOf(liveViewModel.currentTabType).coerceAtLeast(0),
            tabs = liveTabs.map { it.displayName() },
            onTabSelected = { index ->
                val type = liveTabs[index]
                if (type == LiveTabType.Area) {
                    liveViewModel.currentParentGroup?.let(liveViewModel::switchToAreaGroup)
                        ?: liveViewModel.parentAreaGroups.firstOrNull()
                            ?.let(liveViewModel::switchToAreaGroup)
                        ?: liveViewModel.switchTab(type)
                } else {
                    liveViewModel.switchTab(type)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )

        LiveContentFrame(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (liveViewModel.currentTabType == LiveTabType.Area) {
                    LiveAreaSelector(
                        liveViewModel = liveViewModel,
                        showSubAreas = true
                    )
                } else if (liveViewModel.currentTabType == LiveTabType.Recommend &&
                    liveViewModel.followingList.isNotEmpty()
                ) {
                    LiveFollowingStrip(
                        liveCount = liveViewModel.followingLiveCount,
                        rooms = liveViewModel.followingList,
                        onMoreClick = { liveViewModel.switchTab(LiveTabType.Following) },
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
                if (liveViewModel.currentTabType == LiveTabType.Recommend) {
                    LiveAreaSelector(
                        liveViewModel = liveViewModel,
                        showSubAreas = false
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
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
                        MaxExtentGrid(
                            modifier = Modifier.fillMaxSize(),
                            state = gridState,
                            maxCrossAxisExtent = HomeContentStyle.LiveCardMaxWidth,
                            childAspectRatio = HomeContentStyle.VideoAspectRatio,
                            mainAxisExtent = HomeContentStyle.LiveTextExtent,
                            mainAxisSpacing = HomeContentStyle.CardSpace,
                            crossAxisSpacing = HomeContentStyle.CardSpace,
                            contentPadding = PaddingValues(
                                top = HomeContentStyle.CardSpace,
                                bottom = HomeContentStyle.BottomContentPadding
                            )
                        ) { itemHeight ->
                            if (liveViewModel.currentTabType == LiveTabType.History) {
                                liveHistoryItems(
                                    itemHeight = itemHeight,
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
                                    itemHeight = itemHeight,
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
        }
    }
}

@Composable
private fun LiveAreaSelector(
    modifier: Modifier = Modifier,
    liveViewModel: LiveViewModel,
    showSubAreas: Boolean
) {
    val parentGroups = liveViewModel.parentAreaGroups
    if (parentGroups.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val selectedParentIndex = if (liveViewModel.currentTabType == LiveTabType.Area) {
            parentGroups
                .indexOfFirst { it.id == liveViewModel.currentParentGroup?.id }
                .takeIf { it >= 0 }
                ?.plus(1)
                ?: 0
        } else {
            0
        }

        ChipRow(
            selectedTabIndex = selectedParentIndex,
            tabs = listOf("推荐") + parentGroups.map { it.name },
            onTabSelected = { index ->
                if (index == 0) {
                    liveViewModel.switchTab(LiveTabType.Recommend)
                } else {
                    liveViewModel.switchToAreaGroup(parentGroups[index - 1])
                }
            },
            fontSize = 14.sp,
            topPadding = 5.dp,
            bottomPadding = if (showSubAreas) 2.dp else HomeContentStyle.CardSpace
        )

        val subAreas = liveViewModel.subAreaList
        if (showSubAreas && subAreas.isNotEmpty()) {
            val selectedSubIndex = subAreas
                .indexOfFirst { it.id == liveViewModel.currentSubArea?.id }
                .coerceAtLeast(0)

            ChipRow(
                selectedTabIndex = selectedSubIndex,
                tabs = subAreas.map { it.name },
                onTabSelected = { index -> liveViewModel.switchSubArea(subAreas[index]) },
                fontSize = 13.sp,
                topPadding = 2.dp,
                bottomPadding = HomeContentStyle.CardSpace
            )
        }
    }
}

@Composable
private fun LiveFollowingStrip(
    liveCount: Int,
    rooms: List<LiveRoomItem>,
    onMoreClick: () -> Unit,
    onClickRoom: (LiveRoomItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = HomeContentStyle.CardSpace)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = HomeContentStyle.CardSpace),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "我的关注  ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = liveCount.toString(),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
                Text(
                    text = "人正在直播",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1
                )
            }
            Row(
                modifier = Modifier
                    .clip(HomeContentStyle.MdShape)
                    .clickable(onClick = onMoreClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "查看更多",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1
                )
                Icon(
                    modifier = Modifier.size(22.dp),
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.Top
        ) {
            itemsIndexed(rooms, key = { _, room -> room.roomId }) { _, room ->
                Column(
                    modifier = Modifier
                        .width(65.dp)
                        .clickable { onClickRoom(room) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            modifier = Modifier
                                .size(45.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            model = room.face.resizedImageUrl(ImageSize.Icon),
                            contentDescription = null,
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = room.uname.removeHtmlTags(),
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun LazyGridScope.liveRoomItems(
    itemHeight: Dp,
    rooms: List<LiveRoomItem>,
    onClickRoom: (LiveRoomItem) -> Unit
) {
    itemsIndexed(rooms, key = { index, room -> "$index:${room.roomId}" }) { _, room ->
        LiveRoomCard(
            modifier = Modifier.height(itemHeight),
            room = room,
            onClick = { onClickRoom(room) }
        )
    }
}

private fun LazyGridScope.liveHistoryItems(
    itemHeight: Dp,
    histories: List<LiveHistoryItem>,
    onClickHistory: (LiveHistoryItem) -> Unit
) {
    itemsIndexed(histories, key = { index, history -> "$index:${history.roomId}:${history.viewAt}" }) { _, history ->
        LiveHistoryCard(
            modifier = Modifier.height(itemHeight),
            history = history,
            onClick = { onClickHistory(history) }
        )
    }
}

@Composable
private fun VideoRankGrid(
    modifier: Modifier = Modifier,
    state: LazyGridState,
    items: List<UgcItem>,
    onClick: (UgcItem) -> Unit
) {
    MaxExtentGrid(
        modifier = modifier,
        state = state,
        maxCrossAxisExtent = HomeContentStyle.ZoneCardMaxWidth,
        childAspectRatio = HomeContentStyle.ZoneCardAspectRatio,
        minHeight = HomeContentStyle.ZoneCardMinHeight,
        mainAxisSpacing = HomeContentStyle.ZoneMainAxisSpacing,
        crossAxisSpacing = 0.dp,
        contentPadding = PaddingValues(
            top = HomeContentStyle.ZoneTopPadding,
            bottom = HomeContentStyle.BottomContentPadding
        )
    ) { itemHeight ->
        itemsIndexed(items, key = { index, item -> "$index:${item.aid}:${item.bvid}" }) { _, item ->
            ZoneVideoCard(
                modifier = Modifier.height(itemHeight),
                item = item,
                onClick = { onClick(item) }
            )
        }
    }
}

@Composable
private fun PgcRankGrid(
    modifier: Modifier = Modifier,
    state: LazyGridState,
    items: List<PgcItem>,
    onClick: (PgcItem) -> Unit
) {
    MaxExtentGrid(
        modifier = modifier,
        state = state,
        maxCrossAxisExtent = HomeContentStyle.ZoneCardMaxWidth,
        childAspectRatio = HomeContentStyle.ZoneCardAspectRatio,
        minHeight = HomeContentStyle.ZoneCardMinHeight,
        mainAxisSpacing = HomeContentStyle.ZoneMainAxisSpacing,
        crossAxisSpacing = 0.dp,
        contentPadding = PaddingValues(
            top = HomeContentStyle.ZoneTopPadding,
            bottom = HomeContentStyle.BottomContentPadding
        )
    ) { itemHeight ->
        itemsIndexed(items, key = { index, item -> "$index:${item.seasonId}" }) { _, item ->
            PgcRankItem(
                modifier = Modifier.height(itemHeight),
                item = item,
                onClick = { onClick(item) }
            )
        }
    }
}

@Composable
private fun MaxExtentGrid(
    modifier: Modifier = Modifier,
    state: LazyGridState,
    maxCrossAxisExtent: Dp,
    childAspectRatio: Float,
    mainAxisSpacing: Dp,
    crossAxisSpacing: Dp,
    contentPadding: PaddingValues,
    mainAxisExtent: Dp = 0.dp,
    minHeight: Dp = 0.dp,
    userScrollEnabled: Boolean = true,
    content: LazyGridScope.(Dp) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val layoutDirection = LocalLayoutDirection.current
        val availableCrossAxisWidth = maxWidth -
            contentPadding.calculateLeftPadding(layoutDirection) -
            contentPadding.calculateRightPadding(layoutDirection)
        val columns = calculateGridColumns(
            availableCrossAxisWidth,
            maxCrossAxisExtent,
            crossAxisSpacing
        )
        val childWidth = calculateGridChildWidth(
            availableCrossAxisWidth,
            columns,
            crossAxisSpacing
        )
        val childHeight = (childWidth.value / childAspectRatio).dp + mainAxisExtent
        val itemHeight = maxOf(childHeight, minHeight)
        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            state = state,
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(crossAxisSpacing),
            verticalArrangement = Arrangement.spacedBy(mainAxisSpacing),
            contentPadding = contentPadding,
            userScrollEnabled = userScrollEnabled
        ) {
            content(itemHeight)
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 0.dp, top = 10.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier
                .clip(HomeContentStyle.MdShape)
                .clickable(onClick = onMoreClick)
                .padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "查看更多",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1
            )
            Icon(
                modifier = Modifier.size(22.dp),
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun VerticalTabBar(
    selectedTabIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(selectedTabIndex, tabs.size) {
        if (selectedTabIndex in tabs.indices) {
            listState.animateScrollToItem(selectedTabIndex)
        }
    }

    Box(
        modifier = modifier
            .width(HomeContentStyle.VerticalTabWidth)
            .background(colorScheme.surface)
            .selectableGroup()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxHeight(),
            state = listState,
            contentPadding = PaddingValues(bottom = HomeContentStyle.VerticalTabBottomPadding)
        ) {
            itemsIndexed(tabs, key = { index, label -> "$index:$label" }) { index, label ->
                val selected = selectedTabIndex == index
                Box(
                    modifier = Modifier
                        .width(HomeContentStyle.VerticalTabWidth)
                        .height(HomeContentStyle.VerticalTabHeight)
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onTabSelected(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .width(HomeContentStyle.IndicatorWidth)
                                .fillMaxHeight()
                                .clip(HomeContentStyle.VerticalIndicatorShape)
                                .background(colorScheme.primary)
                        )
                    }
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = HomeContentStyle.VerticalTabTextStartPadding,
                                end = HomeContentStyle.VerticalTabTextEndPadding
                            ),
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}

@Composable
private fun ChipRow(
    selectedTabIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    fontSize: TextUnit,
    topPadding: Dp,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(selectedTabIndex, tabs.size) {
        if (selectedTabIndex in tabs.indices) {
            listState.animateScrollToItem(selectedTabIndex)
        }
    }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = topPadding + bottomPadding + 10.dp + fontSize.value.dp)
            .padding(top = topPadding, bottom = bottomPadding),
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(tabs, key = { index, label -> "$index:$label" }) { index, label ->
            val selected = selectedTabIndex == index
            Text(
                modifier = Modifier
                    .clip(HomeContentStyle.MdShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                    )
                    .clickable { onTabSelected(index) }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = fontSize),
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun LiveContentFrame(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .padding(horizontal = HomeContentStyle.SafeSpace)
            .clip(HomeContentStyle.MdShape)
    ) {
        content()
    }
}

@Composable
private fun LiveRoomCard(
    modifier: Modifier = Modifier,
    room: LiveRoomItem,
    onClick: () -> Unit
) {
    LiveCard(
        modifier = modifier,
        cover = room.coverUrl(),
        title = room.title,
        upName = room.uname,
        areaText = room.areaName.ifBlank { room.parentName },
        statText = room.watchedShow?.textLarge?.ifBlank { null }
            ?: room.watchedShow?.textSmall?.ifBlank { null }
            ?: "${formatCount(room.online)}人看过",
        onClick = onClick
    )
}

@Composable
private fun LiveHistoryCard(
    modifier: Modifier = Modifier,
    history: LiveHistoryItem,
    onClick: () -> Unit
) {
    LiveCard(
        modifier = modifier,
        cover = history.cover,
        title = history.title,
        upName = history.uname,
        areaText = history.areaName.ifBlank { if (history.liveStatus == 1) "直播中" else "直播历史" },
        statText = history.viewAt.takeIf { it > 0 }?.toSmartDate().orEmpty(),
        onClick = onClick
    )
}

@Composable
private fun LiveCard(
    modifier: Modifier = Modifier,
    cover: String,
    title: String,
    upName: String,
    areaText: String,
    statText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = HomeContentStyle.MdShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(HomeContentStyle.VideoAspectRatio)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.BottomCenter
            ) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = cover.resizedImageUrl(ImageSize.SmallVideoCardCover),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.54f)
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, top = 26.dp, end = 10.dp, bottom = 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        modifier = Modifier.weight(1f, fill = false),
                        text = areaText,
                        fontSize = 11.sp,
                        lineHeight = 11.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statText,
                        fontSize = 11.sp,
                        lineHeight = 11.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 5.dp, top = 8.dp, end = 5.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title.removeHtmlTags(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = MaterialTheme.typography.bodyMedium.fontSize * 1.42f,
                        letterSpacing = 0.3.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = upName.removeHtmlTags(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PgcCard(
    modifier: Modifier = Modifier,
    item: PgcItem,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = HomeContentStyle.MdShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(HomeContentStyle.PgcCoverAspectRatio)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = item.cover.resizedImageUrl(ImageSize.SeasonCoverThumbnail),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
                CardBadge(
                    text = item.badge?.text.orEmpty(),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp),
                    type = CardBadgeType.Primary
                )
                CardBadge(
                    text = item.indexShow.orEmpty(),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 6.dp, bottom = 6.dp),
                    type = CardBadgeType.Gray
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp, top = 5.dp, end = 0.dp, bottom = 3.dp)
            ) {
                Text(
                    text = item.title.removeHtmlTags(),
                    style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.3.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = (item.indexShow ?: item.subTitle).removeHtmlTags(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PgcRankItem(
    modifier: Modifier = Modifier,
    item: PgcItem,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HomeContentStyle.ZoneCardMinHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = HomeContentStyle.SafeSpace, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(0.75f)
                    .clip(HomeContentStyle.PgcRankCoverShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                model = item.cover.resizedImageUrl(ImageSize.SeasonCoverThumbnail),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = item.title.removeHtmlTags(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val subTitle = item.indexShow ?: item.subTitle
                if (subTitle.isNotBlank()) {
                    Text(
                        text = subTitle.removeHtmlTags(),
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VideoStat(
                        iconRes = R.drawable.ic_play_count,
                        text = item.rating.takeIf { it.isNotBlank() && it != "0" } ?: item.subTitle
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoneVideoCard(
    modifier: Modifier = Modifier,
    item: UgcItem,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HomeContentStyle.ZoneCardMinHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = HomeContentStyle.SafeSpace, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(HomeContentStyle.VideoAspectRatio)
                    .clip(HomeContentStyle.MdShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.BottomEnd
            ) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = item.cover.resizedImageUrl(ImageSize.SmallVideoCardCover),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
                if (item.duration > 0) {
                    CardBadge(
                        text = (item.duration * 1000L).formatHourMinSec(),
                        modifier = Modifier.padding(end = 6.dp, bottom = 6.dp),
                        type = CardBadgeType.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = item.title.removeHtmlTags(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = MaterialTheme.typography.bodyMedium.fontSize * 1.42f,
                        letterSpacing = 0.3.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(
                        item.pubTime,
                        item.author.takeIf { it.isNotBlank() }
                    ).joinToString("  "),
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VideoStat(
                        iconRes = R.drawable.ic_play_count,
                        text = formatCount(item.play)
                    )
                    VideoStat(
                        iconRes = R.drawable.ic_danmaku_count,
                        text = formatCount(item.danmaku)
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoStat(
    iconRes: Int,
    text: String
) {
    if (text.isBlank()) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            modifier = Modifier.size(14.dp),
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1
        )
    }
}

@Composable
private fun CardBadge(
    text: String,
    modifier: Modifier = Modifier,
    type: CardBadgeType
) {
    if (text.isBlank()) return

    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor = when (type) {
        CardBadgeType.Primary -> colorScheme.secondaryContainer.copy(alpha = 0.5f)
        CardBadgeType.Gray -> Color.Black.copy(alpha = 0.45f)
    }
    val contentColor = when (type) {
        CardBadgeType.Primary -> colorScheme.onSecondaryContainer
        CardBadgeType.Gray -> Color.White
    }

    Text(
        modifier = modifier
            .clip(HomeContentStyle.BadgeShape)
            .background(backgroundColor)
            .padding(horizontal = 3.dp, vertical = 2.dp),
        text = text,
        fontSize = 11.sp,
        lineHeight = 11.sp,
        fontWeight = FontWeight.Bold,
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
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

private sealed class RankContentItem {
    abstract fun label(context: Context): String
    abstract fun videoItems(): List<UgcItem>
    abstract fun pgcItems(): List<PgcItem>

    data class Rank(
        val title: String,
        val viewModel: RankViewModel
    ) : RankContentItem() {
        override fun label(context: Context): String = title
        override fun videoItems(): List<UgcItem> = viewModel.allRankVideoList
        override fun pgcItems(): List<PgcItem> = emptyList()
    }

    data class Popular(
        val title: String,
        val viewModel: PopularViewModel
    ) : RankContentItem() {
        override fun label(context: Context): String = title
        override fun videoItems(): List<UgcItem> = viewModel.popularVideoList
        override fun pgcItems(): List<PgcItem> = emptyList()
    }

    data class Ugc(
        val type: UgcTypeV2,
        val viewModel: UgcViewModel
    ) : RankContentItem() {
        override fun label(context: Context): String = type.getDisplayName(context)
        override fun videoItems(): List<UgcItem> = viewModel.ugcItems
        override fun pgcItems(): List<PgcItem> = emptyList()
    }

    data class Pgc(
        val title: String,
        val type: PgcType,
        val viewModel: PgcViewModel
    ) : RankContentItem() {
        override fun label(context: Context): String = title
        override fun videoItems(): List<UgcItem> = emptyList()
        override fun pgcItems(): List<PgcItem> = viewModel.feedItems
            .flatMap { item ->
                when (item.type) {
                    FeedListType.Ep -> item.items.orEmpty()
                    FeedListType.Rank -> item.rank?.items.orEmpty()
                }
            }
            .distinctBy { it.seasonId }
    }
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

private fun formatCount(count: Long): String = when {
    count < 0 -> ""
    count >= 100_000_000L -> String.format("%.1f亿", count / 100_000_000.0)
    count >= 10_000L -> String.format("%.1f万", count / 10_000.0)
    else -> count.toString()
}

private fun Timeline.weekDisplay(): String {
    if (isToday) return "今天"
    return when (dayOfWeek) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        7 -> "周日"
        else -> ""
    }
}

private fun calculateGridColumns(
    availableWidth: Dp,
    maxCrossAxisExtent: Dp,
    crossAxisSpacing: Dp
): Int {
    val raw = (availableWidth.value - crossAxisSpacing.value) /
        (maxCrossAxisExtent.value + crossAxisSpacing.value)
    return ceil(raw).toInt().coerceAtLeast(1)
}

private fun calculateGridChildWidth(
    availableWidth: Dp,
    columns: Int,
    crossAxisSpacing: Dp
): Dp {
    val totalSpacing = crossAxisSpacing.value * (columns - 1)
    return ((availableWidth.value - totalSpacing).coerceAtLeast(0f) / columns).dp
}

private enum class CardBadgeType {
    Primary,
    Gray
}

private object HomeContentStyle {
    val CardSpace = 8.dp
    val SafeSpace = 12.dp
    val BottomContentPadding = 100.dp
    val MdShape = RoundedCornerShape(10.dp)
    val BadgeShape = RoundedCornerShape(4.dp)
    val TimelineChipShape = RoundedCornerShape(20.dp)
    val PgcRankCoverShape = RoundedCornerShape(6.dp)

    const val VideoAspectRatio = 1.6f
    const val PgcCoverAspectRatio = 0.75f
    const val ZoneCardAspectRatio = 1.6f * 2.2f

    val LiveCardMaxWidth = 240.dp
    val LiveTextExtent = 90.dp
    val PgcCardMaxWidth = 144.dp
    val PgcTextExtent = 50.dp
    val PgcRailCardWidth = 120.dp
    val PgcRailHeight = 210.dp
    val TimelineSectionHeight = 276.dp
    val ZoneCardMaxWidth = 480.dp
    val ZoneCardMinHeight = 90.dp
    val ZoneTopPadding = 7.dp
    val ZoneMainAxisSpacing = 2.dp

    val VerticalTabWidth = 72.dp
    val VerticalTabHeight = 46.dp
    val VerticalTabBottomPadding = 105.dp
    val IndicatorWidth = 3.dp
    val VerticalTabTextStartPadding = IndicatorWidth + 3.dp
    val VerticalTabTextEndPadding = 3.dp
    val VerticalIndicatorShape = RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp)
}
