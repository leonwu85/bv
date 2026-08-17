package dev.aaa1115910.bv.tv.screens.main.pgc

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.rounded.FilterAlt
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.aaa1115910.bv.tv.util.requireTvActivity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.pgc.PgcType
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.tv.component.pgc.IndexFilter
import dev.aaa1115910.bv.tv.component.videocard.SeasonCard
import dev.aaa1115910.bv.entity.carddata.SeasonCardData
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.tv.activities.video.SeasonInfoActivity
import dev.aaa1115910.bv.tv.screens.search.SearchTheme
import dev.aaa1115910.bv.tv.util.ProvideListBringIntoViewSpec
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.getDisplayName
import dev.aaa1115910.bv.viewmodel.index.PgcIndexViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun PgcIndexScreen(
    modifier: Modifier = Modifier,
    pgcIndexViewModel: PgcIndexViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val activity = requireTvActivity()
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger { }

    var currentSeasonIndex by remember { mutableIntStateOf(0) }
    val showLargeTitle by remember {
        derivedStateOf {
            currentSeasonIndex < 6
        }
    }
    val titleFontSize by animateFloatAsState(
        targetValue = if (showLargeTitle) 36f else 20f,
        label = "title font size"
    )

    val pgcItems by remember { derivedStateOf { pgcIndexViewModel.indexResultItems.toList() } }
    val noMore = pgcIndexViewModel.noMore
    var showFilter by remember { mutableStateOf(false) }
    val filterReady by remember { derivedStateOf { pgcIndexViewModel.isFilterReady } }
    val activeFilterTags by remember { derivedStateOf { pgcIndexViewModel.activeFilterTags } }
    val filterSignature by remember { derivedStateOf { pgcIndexViewModel.filterSignature } }
    val filterSections = pgcIndexViewModel.filterSections
    val selectedFilters = pgcIndexViewModel.selectedFilters
    val visibleActiveFilterTags = if (activeFilterTags.size > 4) {
        buildList {
            addAll(activeFilterTags.take(4))
            add("另有 ${activeFilterTags.size - 4} 项")
        }
    } else {
        activeFilterTags
    }
    val hasActiveFilter = activeFilterTags.isNotEmpty()

    val onLongClickSeason = {
        if (filterReady) {
            showFilter = true
        }
    }

    val reloadData = {
        scope.launch(Dispatchers.IO) {
            pgcIndexViewModel.clearData()
            pgcIndexViewModel.loadMore()
        }
    }

    LaunchedEffect(Unit) {
        val intent = activity.intent
        val pgcType = runCatching {
            PgcType.entries[intent.getIntExtra("pgcType", 0)]
        }.onFailure {
            logger.warn { "get pgcType from intent failed: ${it.stackTraceToString()}" }
        }.getOrDefault(PgcType.Anime)
        logger.fInfo { "index pgcType: $pgcType" }
        pgcIndexViewModel.changePgcType(pgcType)
    }

    LaunchedEffect(filterReady, filterSignature) {
        if (!filterReady) return@LaunchedEffect
        reloadData()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column(
                modifier = Modifier.padding(start = 48.dp, top = 12.dp, bottom = 4.dp, end = 48.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.title_activity_pgc_index) +
                                " - " + pgcIndexViewModel.pgcType.getDisplayName(context),
                        fontSize = titleFontSize.sp,
                    )
                    Box {
                        IndexFilterPill(
                            text = stringResource(R.string.filter_dialog_title),
                            hasActiveFilter = hasActiveFilter,
                            onClick = { showFilter = true }
                        )
                        if (hasActiveFilter) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 4.dp, end = 4.dp)
                                    .clip(CircleShape)
                                    .background(SearchTheme.accentPink)
                                    .padding(3.dp)
                            )
                        }
                    }
                }
                if (visibleActiveFilterTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
                    ) {
                        visibleActiveFilterTags.forEach { filterTag ->
                            IndexActiveFilterTag(text = filterTag)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        ProvideListBringIntoViewSpec {
            LazyVerticalGrid(
                modifier = Modifier.padding(innerPadding),
                columns = GridCells.Fixed(6),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                itemsIndexed(items = pgcItems) { index, pgcItem ->
                    SeasonCard(
                        data = SeasonCardData.fromPgcItem(pgcItem),
                        onFocus = {
                            currentSeasonIndex = index
                            if (index + 30 > pgcItems.size) {
                                println("load more by focus")
                                scope.launch(Dispatchers.IO) { pgcIndexViewModel.loadMore() }
                            }
                        },
                        onClick = {
                            SeasonInfoActivity.actionStart(
                                context = context,
                                seasonId = pgcItem.seasonId,
                                proxyArea = ProxyArea.checkProxyArea(pgcItem.title)
                            )
                        },
                        onLongClick = onLongClickSeason
                    )
                }
                if (pgcItems.isEmpty() && noMore) {
                    item(
                        span = { GridItemSpan(6) }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = stringResource(R.string.no_data))
                                OutlinedButton(onClick = onLongClickSeason) {
                                    Text(text = stringResource(R.string.filter_dialog_open_tip_click))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    IndexFilter(
        type = pgcIndexViewModel.pgcType,
        show = showFilter && filterReady,
        onDismissRequest = { showFilter = false },
        sections = filterSections,
        selectedFilters = selectedFilters,
        onFilterChange = { pgcIndexViewModel.updateFilter(it) },
        onResetFilters = { pgcIndexViewModel.resetFilters() }
    )
}

@Composable
private fun IndexFilterPill(
    text: String,
    hasActiveFilter: Boolean,
    onClick: () -> Unit
) {
    var hasFocus by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (hasFocus) 1.1f else 1f,
        animationSpec = tween(150),
        label = "index filter pill scale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (hasFocus) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
        animationSpec = tween(200),
        label = "index filter pill bg"
    )

    Surface(
        modifier = Modifier
            .scale(scale)
            .onFocusChanged { hasFocus = it.hasFocus },
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
            Icon(
                imageVector = Icons.Rounded.FilterAlt,
                contentDescription = null,
                tint = if (hasActiveFilter) SearchTheme.accentPink
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = text,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun IndexActiveFilterTag(text: String) {
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
                .clip(CircleShape)
                .background(SearchTheme.accentPink)
                .padding(2.dp)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            color = SearchTheme.accentPink
        )
    }
}
