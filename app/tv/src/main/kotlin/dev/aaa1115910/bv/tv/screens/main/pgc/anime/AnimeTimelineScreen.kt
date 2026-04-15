package dev.aaa1115910.bv.tv.screens.main.pgc.anime

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.season.Timeline
import dev.aaa1115910.biliapi.entity.season.TimelineFilter
import dev.aaa1115910.biliapi.repositories.SeasonRepository
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.carddata.SeasonCardData
import dev.aaa1115910.bv.tv.activities.video.SeasonInfoActivity
import dev.aaa1115910.bv.tv.component.videocard.SeasonCard
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.addAllWithMainContext
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.util.resizedImageUrl
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.getKoin

private fun getWeekString(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "周一"
    2 -> "周二"
    3 -> "周三"
    4 -> "周四"
    5 -> "周五"
    6 -> "周六"
    7 -> "周日"
    else -> "未知"
}

@Composable
fun AnimeTimelineScreen(
    modifier: Modifier = Modifier,
    filter: TimelineFilter = TimelineFilter.Anime,
    titleResId: Int = R.string.title_activity_anime_timeline,
    seasonRepository: SeasonRepository = getKoin().get()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger { }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var isGridFocused by remember { mutableStateOf(false) }
    val showLargeTitle by remember {
        derivedStateOf { !isGridFocused }
    }
    val titleFontSize by animateFloatAsState(
        targetValue = if (showLargeTitle) 48f else 24f,
        label = "title font size"
    )

    val tabRowFocusRequester = remember { FocusRequester() }
    val defaultFocusRequester = remember { FocusRequester() }
    val timelines = remember { mutableStateListOf<Timeline>() }

    val selectedEpisodes by remember {
        derivedStateOf {
            timelines.getOrNull(selectedTabIndex)?.episodes ?: emptyList()
        }
    }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                timelines.addAllWithMainContext {
                    seasonRepository.getTimeline(
                        filter = filter,
                        preferApiType = Prefs.apiType
                    )
                }
                // 数据加载完成后，默认选中"今天"
                val todayIndex = timelines.indexOfFirst { it.isToday }
                if (todayIndex >= 0) {
                    withContext(Dispatchers.Main) {
                        selectedTabIndex = todayIndex
                    }
                }
                runCatching {
                    delay(200)
                    defaultFocusRequester.requestFocus(scope)
                }
            }.onFailure {
                logger.fInfo { "Get timeline failed: ${it.stackTraceToString()}" }
                withContext(Dispatchers.Main) {
                    "获取放送时间表失败: ${it.message}".toast(context)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column(
                modifier = Modifier.padding(start = 48.dp, top = 24.dp, end = 48.dp)
            ) {
                Text(
                    text = stringResource(id = titleResId),
                    fontSize = titleFontSize.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (timelines.size > 1) {
                    TabRow(
                        modifier = Modifier
                            .focusRestorer()
                            .focusRequester(tabRowFocusRequester),
                        selectedTabIndex = selectedTabIndex,
                        separator = { Spacer(modifier = Modifier.width(4.dp)) },
                    ) {
                        timelines.forEachIndexed { index, timeline ->
                            Tab(
                                modifier = if (timeline.isToday) {
                                    Modifier.focusRequester(defaultFocusRequester)
                                } else {
                                    Modifier
                                },
                                selected = index == selectedTabIndex,
                                onFocus = { selectedTabIndex = index },
                            ) {
                                Column(
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 8.dp
                                    ),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = timeline.dateString,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = LocalContentColor.current,
                                    )
                                    Text(
                                        text = buildString {
                                            append(getWeekString(timeline.dayOfWeek))
                                            if (timeline.isToday) append(" ·今天")
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LocalContentColor.current,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (selectedEpisodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (timelines.isEmpty()) "加载中..." else "当天暂无放送",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyVerticalGrid(
                modifier = Modifier
                    .padding(innerPadding)
                    .onFocusChanged { isGridFocused = it.hasFocus },
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(
                    start = 48.dp,
                    end = 48.dp,
                    top = 24.dp,
                    bottom = 48.dp
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                itemsIndexed(
                    items = selectedEpisodes,
                    key = { _, episode -> episode.seasonId }
                ) { index, episode ->
                    SeasonCard(
                        data = SeasonCardData(
                            title = episode.title,
                            cover = episode.cover.resizedImageUrl(ImageSize.SeasonCoverThumbnail),
                            seasonId = episode.seasonId,
                            rating = null,
                            coverLabel = buildString {
                                if (episode.publishIndex.isNotBlank()) append(episode.publishIndex)
                                if (episode.publishTime.isNotBlank()) {
                                    if (isNotEmpty()) append(" ")
                                    append(episode.publishTime)
                                }
                                if (isNotEmpty()) append("更新")
                            }.ifEmpty { null }
                        ),
                        onClick = {
                            SeasonInfoActivity.actionStart(
                                context = context,
                                seasonId = episode.seasonId
                            )
                        }
                    )
                }

                // 当天放送数量提示
                item(span = { GridItemSpan(5) }) {
                    Text(
                        text = "共 ${selectedEpisodes.size} 部",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}