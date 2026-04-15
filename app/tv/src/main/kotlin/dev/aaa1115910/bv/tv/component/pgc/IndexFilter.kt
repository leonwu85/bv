package dev.aaa1115910.bv.tv.component.pgc

import android.view.KeyEvent
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.pgc.PgcType
import dev.aaa1115910.biliapi.entity.pgc.index.Area
import dev.aaa1115910.biliapi.entity.pgc.index.Copyright
import dev.aaa1115910.biliapi.entity.pgc.index.IndexOrder
import dev.aaa1115910.biliapi.entity.pgc.index.IndexOrderType
import dev.aaa1115910.biliapi.entity.pgc.index.IsFinish
import dev.aaa1115910.biliapi.entity.pgc.index.PgcIndexParam
import dev.aaa1115910.biliapi.entity.pgc.index.Producer
import dev.aaa1115910.biliapi.entity.pgc.index.ReleaseDate
import dev.aaa1115910.biliapi.entity.pgc.index.SeasonMonth
import dev.aaa1115910.biliapi.entity.pgc.index.SeasonStatus
import dev.aaa1115910.biliapi.entity.pgc.index.SeasonVersion
import dev.aaa1115910.biliapi.entity.pgc.index.SpokenLanguage
import dev.aaa1115910.biliapi.entity.pgc.index.Style
import dev.aaa1115910.biliapi.entity.pgc.index.Year
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.tv.screens.search.SearchTheme
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.getDisplayName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class IndexFilterSection(
    val title: String,
    val filters: List<PgcIndexParam>,
    val selectedFilter: PgcIndexParam,
    val onFilterChange: (PgcIndexParam) -> Unit
)

@Composable
fun IndexFilter(
    modifier: Modifier = Modifier,
    type: PgcType,
    show: Boolean,
    onDismissRequest: () -> Unit,
    order: IndexOrder,
    orderType: IndexOrderType,
    seasonVersion: SeasonVersion,
    spokenLanguage: SpokenLanguage,
    area: Area,
    isFinish: IsFinish,
    copyright: Copyright,
    seasonStatus: SeasonStatus,
    seasonMonth: SeasonMonth,
    producer: Producer,
    year: Year,
    releaseDate: ReleaseDate,
    style: Style,
    onOrderChange: (IndexOrder) -> Unit,
    onOrderTypeChange: (IndexOrderType) -> Unit,
    onSeasonVersionChange: (SeasonVersion) -> Unit,
    onSpokenLanguageChange: (SpokenLanguage) -> Unit,
    onAreaChange: (Area) -> Unit,
    onIsFinishChange: (IsFinish) -> Unit,
    onCopyrightChange: (Copyright) -> Unit,
    onSeasonStatusChange: (SeasonStatus) -> Unit,
    onSeasonMonthChange: (SeasonMonth) -> Unit,
    onProducerChange: (Producer) -> Unit,
    onYearChange: (Year) -> Unit,
    onReleaseDateChange: (ReleaseDate) -> Unit,
    onStyleChange: (Style) -> Unit
) {
    val context = LocalContext.current
    val sections = listOf(
        IndexFilterSection(
            title = stringResource(R.string.pgc_index_filter_order),
            filters = IndexOrder.getList(type),
            selectedFilter = order,
            onFilterChange = { onOrderChange(it as IndexOrder) }
        ),
        IndexFilterSection(
            title = stringResource(R.string.pgc_index_filter_order_type),
            filters = IndexOrderType.entries,
            selectedFilter = orderType,
            onFilterChange = { onOrderTypeChange(it as IndexOrderType) }
        ),
        IndexFilterSection(
            title = context.getString(R.string.pgc_index_filter_season_version),
            filters = SeasonVersion.getList(type),
            selectedFilter = seasonVersion,
            onFilterChange = { onSeasonVersionChange(it as SeasonVersion) }
        ),
        IndexFilterSection(
            title = context.getString(R.string.pgc_index_filter_spoken_language),
            filters = SpokenLanguage.getList(type),
            selectedFilter = spokenLanguage,
            onFilterChange = { onSpokenLanguageChange(it as SpokenLanguage) }
        ),
        IndexFilterSection(
            title = context.getString(R.string.pgc_index_filter_is_finish),
            filters = IsFinish.getList(type),
            selectedFilter = isFinish,
            onFilterChange = { onIsFinishChange(it as IsFinish) }
        ),
        IndexFilterSection(
            title = context.getString(R.string.pgc_index_filter_season_status),
            filters = SeasonStatus.getList(type),
            selectedFilter = seasonStatus,
            onFilterChange = { onSeasonStatusChange(it as SeasonStatus) }
        ),
        IndexFilterSection(
            title = context.getString(R.string.pgc_index_filter_area),
            filters = Area.getList(type),
            selectedFilter = area,
            onFilterChange = { onAreaChange(it as Area) }
        ),
        IndexFilterSection(
            title = context.getString(R.string.pgc_index_filter_copyright),
            filters = Copyright.getList(type),
            selectedFilter = copyright,
            onFilterChange = { onCopyrightChange(it as Copyright) }
        ),
        IndexFilterSection(
            title = context.getString(R.string.pgc_index_filter_season_month),
            filters = SeasonMonth.getList(type),
            selectedFilter = seasonMonth,
            onFilterChange = { onSeasonMonthChange(it as SeasonMonth) }
        ),
        IndexFilterSection(
            title = context.getString(R.string.pgc_index_filter_producer),
            filters = Producer.getList(type),
            selectedFilter = producer,
            onFilterChange = { onProducerChange(it as Producer) }
        ),
        IndexFilterSection(
            title = context.getString(R.string.pgc_index_filter_year),
            filters = Year.getList(type),
            selectedFilter = year,
            onFilterChange = { onYearChange(it as Year) }
        ),
        IndexFilterSection(
            title = context.getString(R.string.pgc_index_filter_release_date),
            filters = ReleaseDate.getList(type),
            selectedFilter = releaseDate,
            onFilterChange = { onReleaseDateChange(it as ReleaseDate) }
        ),
        IndexFilterSection(
            title = context.getString(R.string.pgc_index_filter_style),
            filters = Style.getList(type),
            selectedFilter = style,
            onFilterChange = { onStyleChange(it as Style) }
        )
    ).filter { it.filters.isNotEmpty() }

    IndexFilterContent(
        modifier = modifier,
        title = stringResource(R.string.pgc_index_filter_title_prefix) + type.getDisplayName(context),
        show = show,
        onDismissRequest = onDismissRequest,
        sections = sections,
        onResetFilters = {
            onOrderChange(IndexOrder.getList(type).first())
            onOrderTypeChange(IndexOrderType.Desc)
            onSeasonVersionChange(SeasonVersion.All)
            onSpokenLanguageChange(SpokenLanguage.All)
            onAreaChange(Area.All)
            onIsFinishChange(IsFinish.All)
            onCopyrightChange(Copyright.All)
            onSeasonStatusChange(SeasonStatus.All)
            onSeasonMonthChange(SeasonMonth.All)
            onProducerChange(Producer.All)
            onYearChange(Year.All)
            onReleaseDateChange(ReleaseDate.All)
            onStyleChange(Style.All)
        }
    )
}

@Composable
private fun IndexFilterContent(
    modifier: Modifier = Modifier,
    title: String,
    show: Boolean,
    onDismissRequest: () -> Unit,
    sections: List<IndexFilterSection>,
    onResetFilters: () -> Unit
) {
    val rowFocusRequesters = remember(sections.size) {
        List(sections.size) { FocusRequester() }
    }
    val rowBringIntoViewRequesters = remember(sections.size) {
        List(sections.size) { BringIntoViewRequester() }
    }
    var isDialogJustOpened by remember { mutableStateOf(false) }

    LaunchedEffect(show, sections.size) {
        if (show && sections.isNotEmpty()) {
            isDialogJustOpened = true
            delay(100)
            rowFocusRequesters.first().requestFocus()
            delay(200)
            isDialogJustOpened = false
        }
    }

    if (show) {
        TvAlertDialog(
            modifier = modifier
                .fillMaxWidth(0.8f),
            onDismissRequest = onDismissRequest,
            confirmButton = {
                Surface(
                    onClick = {
                        if (!isDialogJustOpened) {
                            onResetFilters()
                        }
                    },
                    shape = ClickableSurfaceDefaults.shape(SearchTheme.pillShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedContainerColor = SearchTheme.accentPink,
                        pressedContainerColor = SearchTheme.accentPink
                    )
                ) {
                    Text(
                        text = stringResource(R.string.filter_dialog_reset),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            },
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    sections.forEachIndexed { index, section ->
                        IndexFilterChipRow(
                            title = section.title,
                            filters = section.filters,
                            selectedFilter = section.selectedFilter,
                            focusRequester = rowFocusRequesters[index],
                            bringIntoViewRequester = rowBringIntoViewRequesters[index],
                            upFocusRequester = rowFocusRequesters.getOrNull(index - 1),
                            downFocusRequester = rowFocusRequesters.getOrNull(index + 1),
                            enabled = !isDialogJustOpened,
                            onFilterChange = section.onFilterChange
                        )
                    }
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        )
    }

    BackHandler(
        enabled = show,
        onBack = onDismissRequest
    )
}

@Composable
private fun IndexFilterChip(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    enabled: Boolean = true
) {
    var hasFocus by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(
        targetValue = when {
            selected -> SearchTheme.accentPink
            hasFocus -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        animationSpec = tween(150),
        label = "index chip bg"
    )
    val contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier
            .onFocusChanged { hasFocus = it.hasFocus },
        onClick = { if (enabled) onClick() },
        shape = ClickableSurfaceDefaults.shape(SearchTheme.chipShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = bgColor,
            focusedContainerColor = bgColor,
            pressedContainerColor = bgColor,
            contentColor = contentColor,
            focusedContentColor = contentColor,
            pressedContentColor = contentColor
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun IndexFilterChipRow(
    title: String,
    filters: List<PgcIndexParam>,
    selectedFilter: PgcIndexParam,
    focusRequester: FocusRequester,
    bringIntoViewRequester: BringIntoViewRequester,
    upFocusRequester: FocusRequester?,
    downFocusRequester: FocusRequester?,
    enabled: Boolean,
    onFilterChange: (PgcIndexParam) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged {
                if (it.hasFocus) {
                    scope.launch {
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        LazyRow(
            modifier = Modifier.onPreviewKeyEvent {
                when (it.key) {
                    Key.DirectionDown -> {
                        val nextFocusRequester = downFocusRequester ?: return@onPreviewKeyEvent false
                        if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                            nextFocusRequester.requestFocus()
                        }
                        true
                    }

                    Key.DirectionUp -> {
                        val previousFocusRequester = upFocusRequester ?: return@onPreviewKeyEvent false
                        if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                            previousFocusRequester.requestFocus()
                        }
                        true
                    }

                    else -> false
                }
            },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            items(items = filters) { filter ->
                IndexFilterChip(
                    modifier = if (selectedFilter == filter) Modifier.focusRequester(focusRequester) else Modifier,
                    selected = selectedFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = filter.getDisplayName(context),
                    enabled = enabled
                )
            }
        }
    }
}

private class PgcTypeProvider : PreviewParameterProvider<PgcType> {
    override val values = PgcType.entries.asSequence()
}

@Preview(device = "id:tv_1080p")
@Preview(device = "id:tv_1080p", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun IndexFilterPreview(
    @PreviewParameter(PgcTypeProvider::class) pgcType: PgcType
) {
    var order by remember { mutableStateOf(IndexOrder.PlayCount) }
    var orderType by remember { mutableStateOf(IndexOrderType.Desc) }
    var seasonVersion by remember { mutableStateOf(SeasonVersion.All) }
    var spokenLanguage by remember { mutableStateOf(SpokenLanguage.All) }
    var area by remember { mutableStateOf(Area.All) }
    var isFinish by remember { mutableStateOf(IsFinish.All) }
    var copyright by remember { mutableStateOf(Copyright.All) }
    var seasonStatus by remember { mutableStateOf(SeasonStatus.All) }
    var seasonMonth by remember { mutableStateOf(SeasonMonth.All) }
    var producer by remember { mutableStateOf(Producer.All) }
    var year by remember { mutableStateOf(Year.All) }
    var releaseDate by remember { mutableStateOf(ReleaseDate.All) }
    var style by remember { mutableStateOf(Style.All) }

    BVTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            IndexFilter(
                type = pgcType,
                show = true,
                onDismissRequest = { },
                order = order,
                orderType = orderType,
                seasonVersion = seasonVersion,
                spokenLanguage = spokenLanguage,
                area = area,
                isFinish = isFinish,
                copyright = copyright,
                seasonStatus = seasonStatus,
                seasonMonth = seasonMonth,
                producer = producer,
                year = year,
                releaseDate = releaseDate,
                style = style,
                onOrderChange = { order = it },
                onOrderTypeChange = { orderType = it },
                onSeasonVersionChange = { seasonVersion = it },
                onSpokenLanguageChange = { spokenLanguage = it },
                onAreaChange = { area = it },
                onIsFinishChange = { isFinish = it },
                onCopyrightChange = { copyright = it },
                onSeasonStatusChange = { seasonStatus = it },
                onSeasonMonthChange = { seasonMonth = it },
                onProducerChange = { producer = it },
                onYearChange = { year = it },
                onReleaseDateChange = { releaseDate = it },
                onStyleChange = { style = it }
            )
        }
    }
}