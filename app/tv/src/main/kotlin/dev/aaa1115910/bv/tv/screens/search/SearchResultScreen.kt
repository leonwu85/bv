package dev.aaa1115910.bv.tv.screens.search

import android.app.Activity
import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.live.LiveRoomItem
import dev.aaa1115910.biliapi.entity.ugc.toSmartDate
import dev.aaa1115910.biliapi.repositories.SearchFilterDuration
import dev.aaa1115910.biliapi.repositories.SearchFilterOrderType
import dev.aaa1115910.biliapi.repositories.SearchType
import dev.aaa1115910.biliapi.repositories.SearchTypeResult
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.tv.R as TvR
import dev.aaa1115910.bv.tv.component.LoadingTip
import dev.aaa1115910.bv.tv.component.live.LiveRoomCard
import dev.aaa1115910.bv.tv.component.videocard.SeasonCard
import dev.aaa1115910.bv.tv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.entity.carddata.SeasonCardData
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.tv.activities.video.SeasonInfoActivity
import dev.aaa1115910.bv.tv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.tv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.tv.activities.video.VideoPlayerV3Activity
import dev.aaa1115910.bv.tv.screens.user.UpCard
import dev.aaa1115910.bv.tv.util.ProvideListBringIntoViewSpec
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.focusedScale
import dev.aaa1115910.bv.util.removeHtmlTags
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.viewmodel.search.SearchResultViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun SearchResultScreen(
    modifier: Modifier = Modifier,
    searchResultViewModel: SearchResultViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger { }
    val tabRowFocusRequester = remember { FocusRequester() }

    var rowSize by remember { mutableIntStateOf(4) }
    var currentIndex by remember { mutableIntStateOf(0) }

    var searchKeyword by remember { mutableStateOf("") }

    val searchResult = when (searchResultViewModel.searchType) {
        SearchType.Video -> searchResultViewModel.videoSearchResult
        SearchType.MediaBangumi -> searchResultViewModel.mediaBangumiSearchResult
        SearchType.MediaFt -> searchResultViewModel.mediaFtSearchResult
        SearchType.BiliUser -> searchResultViewModel.biliUserSearchResult
        SearchType.LiveRoom -> searchResultViewModel.liveRoomSearchResult
    }

    var showFilter by remember { mutableStateOf(false) }

    val selectedOrder = searchResultViewModel.selectedOrder
    val selectedDuration = searchResultViewModel.selectedDuration
    val selectedPartition = searchResultViewModel.selectedPartition
    val selectedChildPartition = searchResultViewModel.selectedChildPartition

    val onClickResult: (SearchTypeResult.SearchTypeResultItem) -> Unit = { resultItem ->
        when (resultItem) {
            is SearchTypeResult.Video -> {
                VideoInfoActivity.actionStart(
                    context = context,
                    aid = resultItem.aid,
                    fromSeason = false
                )
            }

            is SearchTypeResult.Pgc -> {
                SeasonInfoActivity.actionStart(
                    context = context,
                    seasonId = resultItem.seasonId,
                    proxyArea = ProxyArea.checkProxyArea(resultItem.title)
                )
            }

            is SearchTypeResult.User -> {
                UpInfoActivity.actionStart(
                    context = context,
                    mid = resultItem.mid,
                    name = resultItem.name,
                    face = resultItem.avatar
                )
            }

            is SearchTypeResult.LiveRoom -> {
                VideoPlayerV3Activity.actionStartLive(
                    context = context,
                    roomId = resultItem.roomId,
                    title = resultItem.title.removeHtmlTags(),
                    upName = resultItem.uname,
                    upFace = resultItem.uface,
                    upMid = resultItem.uid,
                    watchedNum = resultItem.online
                )
            }

            else -> {}
        }
    }

    val backToTabRow: () -> Unit = {
        tabRowFocusRequester.requestFocus(scope)
    }

    val onLongClickSearchResultItem = {
        if (searchResultViewModel.searchType == SearchType.Video) {
            if (Prefs.apiType == ApiType.Web) showFilter = true
        }
    }

    LaunchedEffect(Unit) {
        val intent = (context as Activity).intent
        if (intent.hasExtra("keyword")) {
            searchKeyword = intent.getStringExtra("keyword") ?: ""
            val enableProxy = intent.getBooleanExtra("enableProxy", false)
            if (searchKeyword == "") context.finish()
            searchResultViewModel.enableProxySearchResult = enableProxy
            searchResultViewModel.keyword = searchKeyword
        } else {
            context.finish()
        }
    }

    LaunchedEffect(searchResultViewModel.searchType) {
        val gridColumns = Prefs.gridColumns
        rowSize = when (searchResultViewModel.searchType) {
            SearchType.Video -> gridColumns
            SearchType.MediaBangumi, SearchType.MediaFt -> gridColumns + 2
            SearchType.BiliUser -> gridColumns - 1
            SearchType.LiveRoom -> gridColumns
        }
    }

    LaunchedEffect(
        selectedOrder, selectedDuration, selectedPartition, selectedChildPartition
    ) {
        logger.fInfo { "Start update search result because filter updated" }
        searchResultViewModel.update()
    }

    LaunchedEffect(currentIndex) {
        if (currentIndex + 12 > searchResult.count) {
            searchResultViewModel.loadMore(searchResult.type)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(searchBackgroundGradient())
    ) {
        Column {
            // 顶部栏: 胶囊关键词 + 结果计数
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp, top = 12.dp, bottom = 8.dp, end = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 胶囊 chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = SearchTheme.accentPink
                    )
                    Text(
                        text = searchKeyword,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 右侧信息
                Column(
                    horizontalAlignment = Alignment.End,
                ) {
                    if (searchResultViewModel.searchType == SearchType.Video) {
                        Text(
                            text = stringResource(R.string.filter_dialog_open_tip),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.load_data_count,
                            searchResult.count
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            // Tab 栏 + 内容
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                val searchTypes = remember { SearchType.entries.filter { it != SearchType.LiveRoom } }
                val allSearchTypes = remember { SearchType.entries.toList() }
                val visibleSearchTypes = if (Prefs.showLiveInSidebar) allSearchTypes else searchTypes

                LaunchedEffect(visibleSearchTypes) {
                    if (searchResultViewModel.searchType == SearchType.LiveRoom &&
                        !Prefs.showLiveInSidebar) {
                        searchResultViewModel.searchType = SearchType.Video
                    }
                }

                // Pill 形状标签栏 + 筛选按钮
                val showFilterButton = searchResultViewModel.searchType == SearchType.Video &&
                        Prefs.apiType == ApiType.Web
                val hasActiveFilter = selectedOrder != SearchFilterOrderType.ComprehensiveSort ||
                        selectedDuration != SearchFilterDuration.All ||
                        selectedPartition != null

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧 Tabs
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        visibleSearchTypes.forEach { type ->
                            val isSelected = type == searchResultViewModel.searchType
                            val tabModifier =
                                if (isSelected) Modifier.focusRequester(tabRowFocusRequester) else Modifier
                            SearchTabPill(
                                modifier = tabModifier,
                                text = type.getDisplayName(context),
                                isSelected = isSelected,
                                onFocus = {
                                    scope.launch {
                                        searchResultViewModel.searchType = type
                                        searchResultViewModel.init(type)
                                    }
                                },
                                onClick = {}
                            )
                        }
                    }

                    // 右侧筛选按钮
                    if (showFilterButton) {
                        Box {
                            SearchTabPill(
                                text = stringResource(R.string.filter_dialog_title),
                                isSelected = false,
                                icon = {
                                    Icon(
                                        modifier = Modifier.size(16.dp),
                                        imageVector = Icons.Rounded.FilterAlt,
                                        contentDescription = null,
                                        tint = if (hasActiveFilter) SearchTheme.accentPink
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                },
                                onFocus = {},
                                onClick = { showFilter = true }
                            )
                            // 活跃筛选指示器圆点
                            if (hasActiveFilter) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-4).dp, y = 4.dp)
                                        .size(6.dp)
                                        .background(SearchTheme.accentPink, CircleShape)
                                )
                            }
                        }
                    }
                }

                // 筛选标签行
                if (showFilterButton && hasActiveFilter) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 48.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
                    ) {
                        if (selectedOrder != SearchFilterOrderType.ComprehensiveSort) {
                            ActiveFilterTag(text = selectedOrder.getDisplayName(context))
                        }
                        if (selectedDuration != SearchFilterDuration.All) {
                            ActiveFilterTag(text = selectedDuration.getDisplayName(context))
                        }
                        if (selectedPartition != null) {
                            val partitionText = if (selectedChildPartition != null) {
                                "${selectedPartition!!.strRes} > ${selectedChildPartition!!.strRes}"
                            } else {
                                selectedPartition!!.strRes
                            }
                            ActiveFilterTag(text = partitionText)
                        }
                    }
                }

                // 网格区域
                ProvideListBringIntoViewSpec(padding = 26.dp) {
                    val padding = dimensionResource(TvR.dimen.grid_padding) / 2
                    val spacedBy = dimensionResource(TvR.dimen.grid_spacedBy) / 2
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        val items = when (searchResult.type) {
                            SearchType.Video -> searchResult.videos
                            SearchType.MediaBangumi -> searchResult.mediaBangumis
                            SearchType.MediaFt -> searchResult.mediaFts
                            SearchType.BiliUser -> searchResult.biliUsers
                            SearchType.LiveRoom -> searchResult.liveRooms
                        }

                        if (items.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (searchResultViewModel.isLoading(searchResultViewModel.searchType) ||
                                    !searchResultViewModel.isInitialized(searchResultViewModel.searchType)
                                ) {
                                    LoadingTip()
                                } else {
                                    Text(
                                        text = stringResource(R.string.no_data),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        LazyVerticalGrid(
                            modifier = Modifier.onPreviewKeyEvent {
                                when (it.key) {
                                    Key.Back -> {
                                        if (it.type == KeyEventType.KeyUp) backToTabRow()
                                        return@onPreviewKeyEvent true
                                    }
                                }
                                false
                            },
                            columns = GridCells.Fixed(rowSize),
                            contentPadding = PaddingValues(padding),
                            verticalArrangement = Arrangement.spacedBy(spacedBy),
                            horizontalArrangement = Arrangement.spacedBy(spacedBy)
                        ) {
                            itemsIndexed(items = items) { index, searchResultItem ->
                                SearchResultListItem(
                                    searchResult = searchResultItem,
                                    onClick = { onClickResult(searchResultItem) },
                                    onLongClick = onLongClickSearchResultItem,
                                    onFocus = { currentIndex = index }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    SearchResultVideoFilter(
        show = showFilter,
        onHideFilter = { showFilter = false },
        selectedOrder = selectedOrder,
        selectedDuration = selectedDuration,
        selectedPartition = selectedPartition,
        selectedChildPartition = selectedChildPartition,
        onSelectedOrderChange = { searchResultViewModel.selectedOrder = it },
        onSelectedDurationChange = { searchResultViewModel.selectedDuration = it },
        onSelectedPartitionChange = { searchResultViewModel.selectedPartition = it },
        onSelectedChildPartitionChange = { searchResultViewModel.selectedChildPartition = it }
    )
}

@Composable
private fun SearchResultListItem(
    modifier: Modifier = Modifier,
    searchResult: SearchTypeResult.SearchTypeResultItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocus: () -> Unit
) {
    when (searchResult) {
        is SearchTypeResult.Video -> {
            SmallVideoCard(
                modifier = modifier,
                data = VideoCardData(
                    avid = searchResult.aid,
                    title = searchResult.title.removeHtmlTags(),
                    cover = searchResult.cover,
                    play = with(searchResult.play) { if (this == -1L) null else this },
                    danmaku = with(searchResult.danmaku) { if (this == -1) null else this },
                    upName = searchResult.author,
                    time = searchResult.duration * 1000L,
                    pubTime = searchResult.pubTime.toLong().toSmartDate()
                ),
                onClick = onClick,
                onLongClick = onLongClick,
                onFocus = onFocus
            )
        }

        is SearchTypeResult.Pgc -> {
            SeasonCard(
                modifier = modifier,
                data = SeasonCardData(
                    seasonId = searchResult.seasonId,
                    title = searchResult.title.removeHtmlTags(),
                    cover = searchResult.cover,
                    rating = String.format("%.1f", searchResult.star)
                ),
                onClick = onClick,
                onLongClick = onLongClick,
                onFocus = onFocus
            )
        }

        is SearchTypeResult.User -> {
            UpCard(
                modifier = modifier.focusedScale(0.95f),
                face = searchResult.avatar,
                sign = searchResult.sign,
                username = searchResult.name,
                onFocusChange = { if (it) onFocus() },
                onClick = onClick,
                onLongClick = onLongClick
            )
        }

        is SearchTypeResult.LiveRoom -> {
            LiveRoomCard(
                modifier = modifier,
                data = LiveRoomItem(
                    roomId = searchResult.roomId,
                    uid = searchResult.uid,
                    title = searchResult.title.removeHtmlTags(),
                    uname = searchResult.uname,
                    online = searchResult.online,
                    cover = searchResult.cover,
                    face = searchResult.uface,
                    areaName = searchResult.cateName
                ),
                onClick = onClick,
                onFocus = onFocus
            )
        }

        else -> {

        }
    }
}

fun SearchType.getDisplayName(context: Context) = when (this) {
    SearchType.Video -> context.getString(R.string.search_result_type_name_video)
    SearchType.MediaBangumi -> context.getString(R.string.search_result_type_name_media_bangumi)
    SearchType.MediaFt -> context.getString(R.string.search_result_type_name_media_ft)
    SearchType.BiliUser -> context.getString(R.string.search_result_type_name_bili_user)
    SearchType.LiveRoom -> context.getString(R.string.search_result_type_name_live_room)
}

@Composable
private fun SearchTabPill(
    modifier: Modifier = Modifier,
    text: String,
    isSelected: Boolean,
    icon: (@Composable () -> Unit)? = null,
    onFocus: () -> Unit,
    onClick: () -> Unit
) {
    var hasFocus by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (hasFocus) 1.1f else 1.0f,
        animationSpec = tween(150),
        label = "tab scale"
    )
    val bgColor by animateColorAsState(
        targetValue = when {
            isSelected -> SearchTheme.accentPink
            hasFocus -> MaterialTheme.colorScheme.surfaceVariant
            else -> Color.Transparent
        },
        animationSpec = tween(200),
        label = "tab bg"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .onFocusChanged {
                hasFocus = it.hasFocus
                if (it.hasFocus) onFocus()
            },
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(SearchTheme.pillShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = bgColor,
            focusedContainerColor = bgColor,
            pressedContainerColor = bgColor
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) icon()
            Text(
                text = text,
                fontSize = 13.sp,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ActiveFilterTag(text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SearchTheme.accentPink.copy(alpha = 0.15f))
            .border(1.dp, SearchTheme.accentPink.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(SearchTheme.accentPink, CircleShape)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            color = SearchTheme.accentPink
        )
    }
}
