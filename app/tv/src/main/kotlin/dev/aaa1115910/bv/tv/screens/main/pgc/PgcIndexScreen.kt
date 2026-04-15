package dev.aaa1115910.bv.tv.screens.main.pgc

import android.app.Activity
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
import dev.aaa1115910.biliapi.entity.pgc.index.Area
import dev.aaa1115910.biliapi.entity.pgc.index.Copyright
import dev.aaa1115910.biliapi.entity.pgc.index.IndexOrder
import dev.aaa1115910.biliapi.entity.pgc.index.IndexOrderType
import dev.aaa1115910.biliapi.entity.pgc.index.IsFinish
import dev.aaa1115910.biliapi.entity.pgc.index.Producer
import dev.aaa1115910.biliapi.entity.pgc.index.ReleaseDate
import dev.aaa1115910.biliapi.entity.pgc.index.SeasonMonth
import dev.aaa1115910.biliapi.entity.pgc.index.SeasonStatus
import dev.aaa1115910.biliapi.entity.pgc.index.SeasonVersion
import dev.aaa1115910.biliapi.entity.pgc.index.SpokenLanguage
import dev.aaa1115910.biliapi.entity.pgc.index.Style
import dev.aaa1115910.biliapi.entity.pgc.index.Year
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

    val pgcItems = pgcIndexViewModel.indexResultItems
    val noMore = pgcIndexViewModel.noMore
    var showFilter by remember { mutableStateOf(false) }
    val defaultOrder = remember(pgcIndexViewModel.pgcType) {
        IndexOrder.getList(pgcIndexViewModel.pgcType).firstOrNull() ?: IndexOrder.FollowCount
    }
    val activeFilterTags = buildList {
        if (pgcIndexViewModel.indexOrder != defaultOrder) {
            add(pgcIndexViewModel.indexOrder.getDisplayName(context))
        }
        if (pgcIndexViewModel.indexOrderType != IndexOrderType.Desc) {
            add(pgcIndexViewModel.indexOrderType.getDisplayName(context))
        }
        if (pgcIndexViewModel.seasonVersion != SeasonVersion.All) {
            add(pgcIndexViewModel.seasonVersion.getDisplayName(context))
        }
        if (pgcIndexViewModel.spokenLanguage != SpokenLanguage.All) {
            add(pgcIndexViewModel.spokenLanguage.getDisplayName(context))
        }
        if (pgcIndexViewModel.area != Area.All) {
            add(pgcIndexViewModel.area.getDisplayName(context))
        }
        if (pgcIndexViewModel.isFinish != IsFinish.All) {
            add(pgcIndexViewModel.isFinish.getDisplayName(context))
        }
        if (pgcIndexViewModel.copyright != Copyright.All) {
            add(pgcIndexViewModel.copyright.getDisplayName(context))
        }
        if (pgcIndexViewModel.seasonStatus != SeasonStatus.All) {
            add(pgcIndexViewModel.seasonStatus.getDisplayName(context))
        }
        if (pgcIndexViewModel.seasonMonth != SeasonMonth.All) {
            add(pgcIndexViewModel.seasonMonth.getDisplayName(context))
        }
        if (pgcIndexViewModel.producer != Producer.All) {
            add(pgcIndexViewModel.producer.getDisplayName(context))
        }
        if (pgcIndexViewModel.year != Year.All) {
            add(pgcIndexViewModel.year.getDisplayName(context))
        }
        if (pgcIndexViewModel.releaseDate != ReleaseDate.All) {
            add(pgcIndexViewModel.releaseDate.getDisplayName(context))
        }
        if (pgcIndexViewModel.style != Style.All) {
            add(pgcIndexViewModel.style.getDisplayName(context))
        }
    }
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
        showFilter = true
    }

    val reloadData = {
        scope.launch(Dispatchers.IO) {
            pgcIndexViewModel.clearData()
            pgcIndexViewModel.loadMore()
        }
    }

    LaunchedEffect(Unit) {
        val intent = (context as Activity).intent
        val pgcType = runCatching {
            PgcType.entries[intent.getIntExtra("pgcType", 0)]
        }.onFailure {
            logger.warn { "get pgcType from intent failed: ${it.stackTraceToString()}" }
        }.getOrDefault(PgcType.Anime)
        logger.fInfo { "index pgcType: $pgcType" }
        pgcIndexViewModel.changePgcType(pgcType)
        reloadData()
    }

    LaunchedEffect(
        pgcIndexViewModel.indexOrder,
        pgcIndexViewModel.indexOrderType,
        pgcIndexViewModel.seasonVersion,
        pgcIndexViewModel.spokenLanguage,
        pgcIndexViewModel.area,
        pgcIndexViewModel.isFinish,
        pgcIndexViewModel.copyright,
        pgcIndexViewModel.seasonStatus,
        pgcIndexViewModel.seasonMonth,
        pgcIndexViewModel.producer,
        pgcIndexViewModel.year,
        pgcIndexViewModel.releaseDate,
        pgcIndexViewModel.style,
    ) {
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
                                OutlinedButton(onClick = { showFilter = true }) {
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
        show = showFilter,
        onDismissRequest = { showFilter = false },
        order = pgcIndexViewModel.indexOrder,
        orderType = pgcIndexViewModel.indexOrderType,
        seasonVersion = pgcIndexViewModel.seasonVersion,
        spokenLanguage = pgcIndexViewModel.spokenLanguage,
        area = pgcIndexViewModel.area,
        isFinish = pgcIndexViewModel.isFinish,
        copyright = pgcIndexViewModel.copyright,
        seasonStatus = pgcIndexViewModel.seasonStatus,
        seasonMonth = pgcIndexViewModel.seasonMonth,
        producer = pgcIndexViewModel.producer,
        year = pgcIndexViewModel.year,
        releaseDate = pgcIndexViewModel.releaseDate,
        style = pgcIndexViewModel.style,
        onOrderChange = { pgcIndexViewModel.indexOrder = it },
        onOrderTypeChange = { pgcIndexViewModel.indexOrderType = it },
        onSeasonVersionChange = { pgcIndexViewModel.seasonVersion = it },
        onSpokenLanguageChange = { pgcIndexViewModel.spokenLanguage = it },
        onAreaChange = { pgcIndexViewModel.area = it },
        onIsFinishChange = { pgcIndexViewModel.isFinish = it },
        onCopyrightChange = { pgcIndexViewModel.copyright = it },
        onSeasonStatusChange = { pgcIndexViewModel.seasonStatus = it },
        onSeasonMonthChange = { pgcIndexViewModel.seasonMonth = it },
        onProducerChange = { pgcIndexViewModel.producer = it },
        onYearChange = { pgcIndexViewModel.year = it },
        onReleaseDateChange = { pgcIndexViewModel.releaseDate = it },
        onStyleChange = { pgcIndexViewModel.style = it }
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