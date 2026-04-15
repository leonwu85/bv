package dev.aaa1115910.bv.tv.component.pgc

import android.content.res.Configuration
import android.view.KeyEvent
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
import androidx.compose.runtime.mutableStateMapOf
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
import dev.aaa1115910.biliapi.entity.pgc.index.PGC_INDEX_ORDER_FIELD
import dev.aaa1115910.biliapi.entity.pgc.index.PgcIndexOption
import dev.aaa1115910.biliapi.entity.pgc.index.PgcIndexSection
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.tv.screens.search.SearchTheme
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.getDisplayName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun IndexFilter(
    modifier: Modifier = Modifier,
    type: PgcType,
    show: Boolean,
    onDismissRequest: () -> Unit,
    sections: List<PgcIndexSection>,
    selectedFilters: Map<String, PgcIndexOption>,
    onFilterChange: (PgcIndexOption) -> Unit,
    onResetFilters: () -> Unit
) {
    val context = LocalContext.current

    IndexFilterContent(
        modifier = modifier,
        title = stringResource(R.string.pgc_index_filter_title_prefix) + type.getDisplayName(context),
        show = show,
        onDismissRequest = onDismissRequest,
        sections = sections,
        selectedFilters = selectedFilters,
        onFilterChange = onFilterChange,
        onResetFilters = onResetFilters
    )
}

@Composable
private fun IndexFilterContent(
    modifier: Modifier = Modifier,
    title: String,
    show: Boolean,
    onDismissRequest: () -> Unit,
    sections: List<PgcIndexSection>,
    selectedFilters: Map<String, PgcIndexOption>,
    onFilterChange: (PgcIndexOption) -> Unit,
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
                            options = section.options,
                            selectedFilter = selectedFilters[section.field],
                            focusRequester = rowFocusRequesters[index],
                            bringIntoViewRequester = rowBringIntoViewRequesters[index],
                            upFocusRequester = rowFocusRequesters.getOrNull(index - 1),
                            downFocusRequester = rowFocusRequesters.getOrNull(index + 1),
                            enabled = !isDialogJustOpened,
                            onFilterChange = onFilterChange
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
    options: List<PgcIndexOption>,
    selectedFilter: PgcIndexOption?,
    focusRequester: FocusRequester,
    bringIntoViewRequester: BringIntoViewRequester,
    upFocusRequester: FocusRequester?,
    downFocusRequester: FocusRequester?,
    enabled: Boolean,
    onFilterChange: (PgcIndexOption) -> Unit
) {
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
            items(
                items = options,
                key = { option -> "${option.field}:${option.keyword}:${option.sort.orEmpty()}" }
            ) { option ->
                IndexFilterChip(
                    modifier = if (selectedFilter == option) Modifier.focusRequester(focusRequester) else Modifier,
                    selected = selectedFilter == option,
                    onClick = { onFilterChange(option) },
                    label = option.name,
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
    val sections = remember {
        listOf(
            PgcIndexSection(
                field = PGC_INDEX_ORDER_FIELD,
                title = "排序",
                options = listOf(
                    PgcIndexOption(PGC_INDEX_ORDER_FIELD, "8", "综合排序", sort = "0"),
                    PgcIndexOption(PGC_INDEX_ORDER_FIELD, "3", "最多追番", sort = "0"),
                    PgcIndexOption(PGC_INDEX_ORDER_FIELD, "0", "最近更新", sort = "0")
                )
            ),
            PgcIndexSection(
                field = "area",
                title = "地区",
                options = listOf(
                    PgcIndexOption("area", "-1", "全部地区"),
                    PgcIndexOption("area", "1,6,7", "国产"),
                    PgcIndexOption("area", "2", "日本"),
                    PgcIndexOption("area", "3", "美国")
                )
            ),
            PgcIndexSection(
                field = "season_status",
                title = "付费类型",
                options = listOf(
                    PgcIndexOption("season_status", "-1", "全部付费"),
                    PgcIndexOption("season_status", "1", "免费"),
                    PgcIndexOption("season_status", "2,6", "付费"),
                    PgcIndexOption("season_status", "4,6", "大会员")
                )
            )
        )
    }
    val selectedFilters = remember {
        mutableStateMapOf<String, PgcIndexOption>().apply {
            sections.forEach { section ->
                section.options.firstOrNull()?.let { option ->
                    put(section.field, option)
                }
            }
        }
    }

    BVTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            IndexFilter(
                type = pgcType,
                show = true,
                onDismissRequest = { },
                sections = sections,
                selectedFilters = selectedFilters,
                onFilterChange = { option -> selectedFilters[option.field] = option },
                onResetFilters = {
                    sections.forEach { section ->
                        section.options.firstOrNull()?.let { option ->
                            selectedFilters[section.field] = option
                        }
                    }
                }
            )
        }
    }
}