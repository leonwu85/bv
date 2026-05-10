package dev.aaa1115910.bv.mobile.screen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.entity.pgc.PgcType
import dev.aaa1115910.biliapi.entity.pgc.index.PgcIndexOption
import dev.aaa1115910.biliapi.entity.pgc.index.PgcIndexSection
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.carddata.SeasonCardData
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.mobile.activities.SeasonInfoActivity
import dev.aaa1115910.bv.mobile.component.videocard.SeasonCard
import dev.aaa1115910.bv.util.OnBottomReached
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.getDisplayName
import dev.aaa1115910.bv.viewmodel.index.PgcIndexViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PgcIndexScreen(
    modifier: Modifier = Modifier,
    pgcIndexViewModel: PgcIndexViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger("PgcIndexScreen")
    val gridState = rememberLazyGridState()

    val pgcItems = pgcIndexViewModel.indexResultItems
    val filterReady by remember { derivedStateOf { pgcIndexViewModel.isFilterReady } }
    val activeFilterTags by remember { derivedStateOf { pgcIndexViewModel.activeFilterTags } }
    val filterSignature by remember { derivedStateOf { pgcIndexViewModel.filterSignature } }
    var showFilter by remember { mutableStateOf(false) }

    val reloadData = {
        scope.launch(Dispatchers.IO) {
            pgcIndexViewModel.clearData()
            pgcIndexViewModel.loadMore()
        }
    }

    LaunchedEffect(Unit) {
        val pgcType = runCatching {
            PgcType.entries[activity.intent.getIntExtra("pgcType", 0)]
        }.onFailure {
            logger.warn { "get pgcType from intent failed: ${it.stackTraceToString()}" }
        }.getOrDefault(PgcType.Anime)
        logger.fInfo { "mobile index pgcType: $pgcType" }
        pgcIndexViewModel.changePgcType(pgcType)
    }

    LaunchedEffect(filterReady, filterSignature) {
        if (filterReady) reloadData()
    }

    gridState.OnBottomReached(
        loading = pgcIndexViewModel.updating || pgcIndexViewModel.noMore || !filterReady,
        loadMore = { scope.launch(Dispatchers.IO) { pgcIndexViewModel.loadMore() } }
    )

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.title_activity_pgc_index) +
                                    " - " + pgcIndexViewModel.pgcType.getDisplayName(context),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = activity::finish) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = null
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            enabled = filterReady,
                            onClick = { showFilter = true }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FilterAlt,
                                contentDescription = stringResource(R.string.filter_dialog_title),
                                tint = if (activeFilterTags.isNotEmpty()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
                ActiveFilterTags(tags = activeFilterTags)
            }
        }
    ) { innerPadding ->
        when {
            pgcItems.isEmpty() && pgcIndexViewModel.updating -> LoadingOrEmptyContent(
                modifier = Modifier.padding(innerPadding),
                loading = true,
                emptyText = stringResource(R.string.no_data)
            )

            pgcItems.isEmpty() && pgcIndexViewModel.noMore -> LoadingOrEmptyContent(
                modifier = Modifier.padding(innerPadding),
                loading = false,
                emptyText = stringResource(R.string.no_data)
            )

            else -> LazyVerticalGrid(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                state = gridState,
                columns = GridCells.Adaptive(120.dp),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pgcItems, key = { "${it.seasonId}:${it.title}" }) { pgcItem ->
                    SeasonCard(
                        data = SeasonCardData.fromPgcItem(pgcItem),
                        onClick = {
                            SeasonInfoActivity.actionStart(
                                context = context,
                                seasonId = pgcItem.seasonId,
                                proxyArea = ProxyArea.checkProxyArea(pgcItem.title)
                            )
                        }
                    )
                }
            }
        }
    }

    if (showFilter && filterReady) {
        PgcIndexFilterSheet(
            sections = pgcIndexViewModel.filterSections,
            selectedFilters = pgcIndexViewModel.selectedFilters,
            onFilterChange = pgcIndexViewModel::updateFilter,
            onResetFilters = pgcIndexViewModel::resetFilters,
            onDismissRequest = { showFilter = false }
        )
    }
}


@Composable
private fun ActiveFilterTags(
    modifier: Modifier = Modifier,
    tags: List<String>
) {
    if (tags.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            Text(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                text = tag,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PgcIndexFilterSheet(
    sections: List<PgcIndexSection>,
    selectedFilters: Map<String, PgcIndexOption>,
    onFilterChange: (PgcIndexOption) -> Unit,
    onResetFilters: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.filter_dialog_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Row {
                    TextButton(onClick = onResetFilters) {
                        Text(text = "重置")
                    }
                    TextButton(onClick = onDismissRequest) {
                        Text(text = "完成")
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(sections, key = { it.field }) { section ->
                    FilterSection(
                        section = section,
                        selectedOption = selectedFilters[section.field],
                        onFilterChange = onFilterChange
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterSection(
    modifier: Modifier = Modifier,
    section: PgcIndexSection,
    selectedOption: PgcIndexOption?,
    onFilterChange: (PgcIndexOption) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            section.options.forEach { option ->
                FilterChip(
                    selected = selectedOption == option,
                    onClick = { onFilterChange(option) },
                    label = {
                        Text(
                            text = option.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
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