package dev.aaa1115910.bv.mobile.component.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import dev.aaa1115910.biliapi.entity.video.Dimension
import dev.aaa1115910.biliapi.entity.video.InteractiveNode
import dev.aaa1115910.biliapi.entity.video.VideoPage
import dev.aaa1115910.biliapi.entity.video.season.Episode
import dev.aaa1115910.biliapi.entity.video.season.Section
import dev.aaa1115910.biliapi.entity.video.season.UgcSeason
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme
import dev.aaa1115910.bv.util.formatHourMinSec

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerPages(
    modifier: Modifier = Modifier,
    currentCid: Long,
    interactiveNodes: List<InteractiveNode>,
    pages: List<VideoPage>,
    ugcSeason: UgcSeason?,
    pgcSections: List<Section>,
    onClickInteractiveNode: (InteractiveNode) -> Unit,
    onClickPage: (VideoPage) -> Unit,
    onClickEpisode: (sectionIndex: Int, episode: Episode) -> Unit,
    onClickEpisodePage: (sectionIndex: Int, episode: Episode, page: VideoPage) -> Unit = { _, _, page ->
        onClickPage(page)
    }
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        confirmValueChange = { sheetValue ->
            println("confirmValueChange: $sheetValue")
            true
        }
    )
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }

    var currentSection by remember { mutableStateOf<Section?>(null) }

    LaunchedEffect(currentCid) {
        if (pgcSections.isNotEmpty()) {
            currentSection =
                pgcSections.find { it.episodes.any { episode -> episode.cid == currentCid } }
        } else if (ugcSeason != null) {
            currentSection = ugcSeason.sections.find {
                it.episodes.any { episode ->
                    episode.cid == currentCid || episode.pages.any { page -> page.cid == currentCid }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        if (pgcSections.isNotEmpty()) {
            // TODO pgc
        } else if (interactiveNodes.isNotEmpty()) {
            VideoPlayerInteractiveNodesRow(
                nodes = interactiveNodes,
                onClickMore = { openBottomSheet = !openBottomSheet },
                onClickNode = onClickInteractiveNode,
                currentCid = currentCid
            )
        } else if (ugcSeason != null) {
            // TODO ugc
            if (currentSection != null) {
                //VideoPlayerUgcSectionsFilter(
                //    sections = ugcSeason.sections,
                //    currentSection = currentSection!!,
                //    onSectionChange = { currentSection = it }
                //)
                VideoPlayerEpisodesRow(
                    //title = currentSection!!.title,
                    episodes = currentSection!!.episodes,
                    onClickMore = { openBottomSheet = !openBottomSheet },
                    onClickEpisode = { episode ->
                        onClickEpisode(ugcSeason.sections.indexOf(currentSection), episode)
                    },
                    onClickPage = { episode, page ->
                        onClickEpisodePage(ugcSeason.sections.indexOf(currentSection), episode, page)
                    },
                    currentCid = currentCid
                )
            }
        } else if (pages.size > 1) {
            VideoPlayerPagesRow(
                //title = "视频分 P",
                pages = pages,
                onClickMore = { openBottomSheet = !openBottomSheet },
                onClickPage = onClickPage,
                currentCid = currentCid
            )
        }
    }


    if (openBottomSheet) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { openBottomSheet = false },
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            VideoPlayerPartSheetContent(
                currentCid = currentCid,
                interactiveNodes = interactiveNodes,
                pages = pages,
                ugcSeason = ugcSeason,
                pgcSections = pgcSections,
                onClickInteractiveNode = onClickInteractiveNode,
                onClickPage = onClickPage,
                onClickEpisode = { episode ->
                    onClickEpisode(ugcSeason!!.sections.indexOf(currentSection), episode)
                },
                onClickEpisodePage = { episode, page ->
                    onClickEpisodePage(ugcSeason!!.sections.indexOf(currentSection), episode, page)
                }
            )
        }
    }
}

@Composable
fun VideoPlayerInteractiveNodesRow(
    modifier: Modifier = Modifier,
    nodes: List<InteractiveNode>,
    currentCid: Long,
    onClickMore: () -> Unit = {},
    onClickNode: (InteractiveNode) -> Unit = {}
) {
    val currentIndex = remember(nodes, currentCid) {
        nodes.indexOfFirst { it.cid == currentCid }
    }
    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) {
            listState.scrollToItem((currentIndex - 1).coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 68.dp,
                    top = 8.dp,
                    bottom = 8.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(nodes, key = { index, node -> "$index:${node.cid}" }) { index, node ->
                    VideoPlayerPageItem(
                        modifier = modifier,
                        text = "分支${index + 1} ${node.title.ifBlank { "未命名分支" }}",
                        onClick = { onClickNode(node) },
                        isPlaying = node.cid == currentCid
                    )
                }
            }
            MoreButton(
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = onClickMore
            )
        }
    }
}

@Composable
private fun VideoPlayerUgcSectionsFilter(
    modifier: Modifier = Modifier,
    sections: List<Section>,
    currentSection: Section,
    onSectionChange: (Section) -> Unit = {}
) {
    LazyRow {
        items(sections) { section ->
            VideoPlayerUgcSectionsFilterChip(
                modifier = modifier,
                section = section,
                selected = section == currentSection,
                onClick = { onSectionChange(section) }
            )
        }
    }
}

@Composable
fun VideoPlayerUgcSectionsFilterChip(
    modifier: Modifier = Modifier,
    section: Section,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        modifier = modifier,
        onClick = onClick,
        label = {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleSmall
            )
        },
        selected = selected,
        leadingIcon = (@Composable {
            Icon(
                imageVector = Icons.Filled.Done,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize)
            )
        }).takeIf { selected }
    )
}

@Composable
fun VideoPlayerEpisodesRow(
    modifier: Modifier = Modifier,
    title: String? = null,
    episodes: List<Episode>,
    currentCid: Long,
    onClickMore: () -> Unit = {},
    onClickEpisode: (Episode) -> Unit = {},
    onClickPage: (episode: Episode, page: VideoPage) -> Unit = { _, _ -> }
) {
    val currentEpisodeIndex = remember(episodes, currentCid) {
        episodes.indexOfFirst { it.matchesCurrentCid(currentCid) }
    }
    val currentEpisode = episodes.getOrNull(currentEpisodeIndex)
    val currentEpisodePageIndex = remember(currentEpisode, currentCid) {
        currentEpisode?.pages?.indexOfFirst { it.cid == currentCid } ?: -1
    }
    val episodeListState = rememberLazyListState()
    val pageListState = rememberLazyListState()

    LaunchedEffect(currentEpisodeIndex) {
        if (currentEpisodeIndex >= 0) {
            episodeListState.scrollToItem((currentEpisodeIndex - 1).coerceAtLeast(0))
        }
    }

    LaunchedEffect(currentEpisode, currentEpisodePageIndex) {
        if (currentEpisodePageIndex >= 0) {
            pageListState.scrollToItem((currentEpisodePageIndex - 1).coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (title != null) {
            Text(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
        }
        Box {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                state = episodeListState,
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 68.dp,
                    top = 8.dp,
                    bottom = 8.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(episodes) { index, episode ->
                    VideoPlayerPageItem(
                        modifier = modifier,
                        text = "EP${index + 1} ${episode.title}",
                        onClick = {
                            val firstPage = episode.pages.firstOrNull()
                            if (episode.pages.size > 1 && firstPage != null) {
                                onClickPage(episode, firstPage)
                            } else {
                                onClickEpisode(episode)
                            }
                        },
                        isPlaying = episode.matchesCurrentCid(currentCid)
                    )
                }
            }
            MoreButton(
                modifier = Modifier
                    .align(Alignment.CenterEnd),
                onClick = onClickMore
            )
        }
        if (currentEpisode != null && currentEpisode.pages.size > 1) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                state = pageListState,
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 0.dp,
                    bottom = 8.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(currentEpisode.pages) { index, page ->
                    VideoPlayerPageItem(
                        modifier = modifier,
                        text = "P${index + 1} ${page.title}",
                        onClick = { onClickPage(currentEpisode, page) },
                        isPlaying = page.cid == currentCid
                    )
                }
            }
        }
    }
}

@Composable
fun VideoPlayerPagesRow(
    modifier: Modifier = Modifier,
    title: String? = null,
    pages: List<VideoPage>,
    currentCid: Long,
    onClickMore: () -> Unit = {},
    onClickPage: (VideoPage) -> Unit = {}
) {
    val currentIndex = remember(pages, currentCid) {
        pages.indexOfFirst { it.cid == currentCid }
    }
    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) {
            listState.scrollToItem((currentIndex - 1).coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (title != null) {
            Text(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
        }
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 68.dp,
                    top = 8.dp,
                    bottom = 8.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(pages) { index, page ->
                    VideoPlayerPageItem(
                        modifier = modifier,
                        text = "P${index + 1} ${page.title}",
                        onClick = { onClickPage(page) },
                        isPlaying = page.cid == currentCid
                    )
                }
            }
            MoreButton(
                modifier = Modifier
                    .align(Alignment.CenterEnd),
                onClick = onClickMore
            )
        }
    }
}

@Composable
private fun MoreButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    var color by remember { mutableStateOf(Color.Red) }
    color = MaterialTheme.colorScheme.surface
    val colorStops = arrayOf(
        0.0f to Color.Transparent,
        0.4f to MaterialTheme.colorScheme.surface,
        1f to MaterialTheme.colorScheme.surface
    )
    Box(
        modifier = modifier
            .width(60.dp)
            .height(80.dp)
            .background(Brush.horizontalGradient(colorStops = colorStops)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 8.dp),
            onClick = onClick
        ) {
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun VideoPlayerPageItem(
    modifier: Modifier = Modifier,
    text: String,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val inlineContentMap = mapOf(
        "playingIcon" to InlineTextContent(
            Placeholder(
                width = with(density) { 20.dp.toSp() },
                height = with(density) { 20.dp.toSp() },
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            )
        ) {
            PlayingIcon()
        }
    )
    val annotatedString = buildAnnotatedString {
        if (isPlaying) appendInlineContent("playingIcon")
        append(text)
    }
    Box(
        modifier = modifier
            .width(160.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { onClick() }
    ) {
        Text(
            modifier = Modifier.padding(8.dp),
            text = annotatedString,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            inlineContent = inlineContentMap,
            color = if (isPlaying) MaterialTheme.colorScheme.primary else Color.Unspecified
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoPlayerPartSheetContent(
    modifier: Modifier = Modifier,
    currentCid: Long,
    interactiveNodes: List<InteractiveNode>,
    pages: List<VideoPage>,
    ugcSeason: UgcSeason?,
    pgcSections: List<Section>,
    onClickInteractiveNode: (InteractiveNode) -> Unit,
    onClickPage: (VideoPage) -> Unit,
    onClickEpisode: (Episode) -> Unit,
    onClickEpisodePage: (episode: Episode, page: VideoPage) -> Unit = { _, _ -> }
) {
    var currentSection by remember { mutableStateOf(ugcSeason?.sections?.first()) }
    val interactiveListState = rememberLazyListState()
    val ugcEpisodeListState = rememberLazyListState()
    val pageListState = rememberLazyListState()
    val currentInteractiveIndex = remember(interactiveNodes, currentCid) {
        interactiveNodes.indexOfFirst { it.cid == currentCid }
    }
    val currentEpisodeIndex = remember(currentSection, currentCid) {
        currentSection?.episodes?.indexOfFirst { it.matchesCurrentCid(currentCid) } ?: -1
    }
    val currentPageIndex = remember(pages, currentCid) {
        pages.indexOfFirst { it.cid == currentCid }
    }

    val onClickSectionTab: (Section) -> Unit = { section ->
        currentSection = section
    }

    LaunchedEffect(currentCid) {
        if (pgcSections.isNotEmpty()) {
            currentSection =
                pgcSections.find { it.episodes.any { episode -> episode.cid == currentCid } }
        } else if (ugcSeason != null) {
            currentSection = ugcSeason.sections.find {
                it.episodes.any { episode ->
                    episode.cid == currentCid || episode.pages.any { page -> page.cid == currentCid }
                }
            }
        }
    }

    LaunchedEffect(currentInteractiveIndex) {
        if (currentInteractiveIndex >= 0) {
            interactiveListState.scrollToItem((currentInteractiveIndex - 1).coerceAtLeast(0))
        }
    }

    LaunchedEffect(currentSection, currentEpisodeIndex) {
        if (currentEpisodeIndex >= 0) {
            ugcEpisodeListState.scrollToItem((currentEpisodeIndex - 1).coerceAtLeast(0))
        }
    }

    LaunchedEffect(currentPageIndex) {
        if (currentPageIndex >= 0) {
            pageListState.scrollToItem((currentPageIndex - 1).coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row {
            TopAppBar(
                title = {
                    Text(
                        text = if (pgcSections.isNotEmpty()) {
                            "视频选集"
                        } else if (interactiveNodes.isNotEmpty()) {
                            "互动分支"
                        } else if (ugcSeason != null) {
                            "视频选集"
                        } else {
                            "视频分 P"
                        },
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
        //Text("ugcSeason: $ugcSeason")
        if (pgcSections.isNotEmpty()) {
            // TODO pgc
            Text("pgc")
        } else if (interactiveNodes.isNotEmpty()) {
            HorizontalDivider()
            LazyColumn(
                state = interactiveListState
            ) {
                itemsIndexed(interactiveNodes, key = { index, node -> "$index:${node.cid}" }) { index, node ->
                    PageListItem(
                        modifier = modifier,
                        text = "分支${index + 1} ${node.title.ifBlank { "未命名分支" }}",
                        duration = null,
                        isPlaying = node.cid == currentCid,
                        onClick = { onClickInteractiveNode(node) }
                    )
                }
                item { Spacer(modifier = Modifier.navigationBarsPadding()) }
            }
        } else if (ugcSeason != null) {
            // TODO ugc
            if (currentSection != null) {
                if (ugcSeason.sections.size > 1) {
                    SecondaryScrollableTabRow(
                        selectedTabIndex = ugcSeason.sections.indexOf(currentSection!!),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        divider = {}
                    ) {
                        ugcSeason.sections.forEach { section ->
                            Tab(
                                selected = currentSection == section,
                                onClick = { onClickSectionTab(section) }
                            ) {
                                Box(
                                    modifier = Modifier.height(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        text = section.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                    state = ugcEpisodeListState,
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(currentSection!!.episodes) { epIndex, episode ->
                        if (episode.pages.size <= 1) {
                            PageListItem(
                                modifier = modifier,
                                text = "EP${epIndex + 1} ${episode.title}",
                                duration = episode.duration,
                                isPlaying = episode.matchesCurrentCid(currentCid),
                                onClick = { onClickEpisode(episode) }
                            )
                        } else {
                            Column {
                                var expand by remember { mutableStateOf(true) }
                                LaunchedEffect(currentSection) { expand = true }
                                PageListItem(
                                    modifier = modifier,
                                    text = "EP${epIndex + 1} ${episode.title}",
                                    duration = null,
                                    isPlaying = episode.matchesCurrentCid(currentCid),
                                    onClick = { expand = !expand }
                                )
                                AnimatedVisibility(
                                    visible = expand
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .padding(start = 16.dp)
                                    ) {
                                        episode.pages.forEachIndexed { pageIndex, page ->
                                            PageListItem(
                                                modifier = modifier,
                                                text = "P${pageIndex + 1} ${page.title}",
                                                duration = page.duration,
                                                isPlaying = page.cid == currentCid,
                                                onClick = { onClickEpisodePage(episode, page) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.navigationBarsPadding()) }
                }
            }
        } else if (pages.size > 1) {
            HorizontalDivider()
            LazyColumn(
                state = pageListState
            ) {
                itemsIndexed(pages) { index, page ->
                    PageListItem(
                        modifier = modifier,
                        text = "P${index + 1} ${page.title}",
                        duration = page.duration,
                        isPlaying = page.cid == currentCid,
                        onClick = { onClickPage(page) }
                    )
                }
                item { Spacer(modifier = Modifier.navigationBarsPadding()) }
            }
        }
    }
}


@Composable
private fun PageListItem(
    modifier: Modifier = Modifier,
    text: String,
    duration: Int?,
    isPlaying: Boolean,
    onClick: () -> Unit = {}
) {
    val density = LocalDensity.current
    val inlineContentMap = mapOf(
        "playingIcon" to InlineTextContent(
            Placeholder(
                width = with(density) { 20.dp.toSp() },
                height = with(density) { 20.dp.toSp() },
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            )
        ) {
            PlayingIcon()
        }
    )
    val annotatedString = buildAnnotatedString {
        if (isPlaying) appendInlineContent("playingIcon")
        append(text)
    }
    ListItem(
        modifier = modifier
            .height(40.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() },
        content = {
            Text(
                text = annotatedString,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                inlineContent = inlineContentMap,
            )
        },
        trailingContent = (@Composable {
            Text(
                text = (1000 * (duration?.toLong() ?: 0)).formatHourMinSec(),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
        }).takeIf { duration != null },
        colors = ListItemDefaults.colors(
            contentColor = if (isPlaying) MaterialTheme.colorScheme.primary else Color.Unspecified,
            containerColor = Color.Transparent
        ),
    )
}

private fun Episode.matchesCurrentCid(currentCid: Long): Boolean {
    return cid == currentCid || pages.any { it.cid == currentCid }
}

@Composable
private fun PlayingIcon(modifier: Modifier = Modifier) {
    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR_FILTER,
            value = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                MaterialTheme.colorScheme.primary.hashCode(),
                BlendModeCompat.SRC_ATOP
            ),
            keyPath = arrayOf(
                "**"
            )
        )
    )

    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.ic_playing)
    )
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever
    )

    LottieAnimation(
        modifier = Modifier
            .size(20.dp)
            .scale(2f),
        composition = composition,
        progress = { progress },
        dynamicProperties = dynamicProperties,
        clipTextToBoundingBox = true
    )
}

@Preview
@Composable
private fun VideoPlayerPageWithoutTitlePreview() {
    BVMobileTheme {
        VideoPlayerPagesRow(
            pages = List(10) {
                VideoPage(
                    cid = it.toLong(),
                    index = it,
                    title = "Page title $it",
                    duration = 1,
                    dimension = Dimension(0, 0)
                )
            },
            currentCid = 0
        )
    }
}

@Preview
@Composable
private fun VideoPlayerPageWithTitlePreview() {
    BVMobileTheme {
        VideoPlayerPagesRow(
            title = "Title",
            pages = List(10) { VideoPage(it.toLong(), it, "Title", 1, Dimension(0, 0)) },
            currentCid = 0
        )
    }
}

@Preview
@Composable
private fun PlayingIconPreview() {
    PlayingIcon()
}

@Preview
@Composable
private fun PageListPreview() {
    BVMobileTheme {
        Surface {
            Column {
                repeat(10) {
                    PageListItem(
                        isPlaying = it == 0,
                        duration = 233,
                        text = "This is  a page list item title"
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun VideoPlayerPartSheetContentPagesPreview() {
    val pages = List(10) {
        VideoPage(
            cid = it.toLong(),
            index = it,
            title = "Page title $it",
            duration = 1,
            dimension = Dimension(0, 0)
        )
    }
    BVMobileTheme {
        VideoPlayerPartSheetContent(
            currentCid = 1,
            interactiveNodes = emptyList(),
            pages = pages,
            ugcSeason = null,
            pgcSections = emptyList(),
            onClickInteractiveNode = {},
            onClickPage = {},
            onClickEpisode = {}
        )
    }
}

@Preview
@Composable
private fun VideoPlayerPartSheetContentUgcSeasonPreview() {
    val ugcSeason by remember {
        mutableStateOf(UgcSeason(
            id = 0,
            title = "Ugc Season Title",
            cover = "",
            sections = List(3) { sectionIndex ->
                Section(
                    id = sectionIndex.toLong(),
                    title = "Section $sectionIndex",
                    episodes = List(10) { episodeIndex ->
                        Episode(
                            id = episodeIndex,
                            cid = episodeIndex.toLong(),
                            title = "Section $sectionIndex Episode $episodeIndex",
                            aid = episodeIndex.toLong(),
                            bvid = "",
                            longTitle = "Episode long title $episodeIndex",
                            cover = "",
                            duration = 111,
                            dimension = Dimension(0, 0),
                            pages = if (episodeIndex == 3) {
                                List(10) { pageIndex ->
                                    VideoPage(
                                        cid = 100 + pageIndex.toLong(),
                                        index = pageIndex,
                                        title = "Pages in sections $pageIndex",
                                        duration = 100,
                                        dimension = Dimension(0, 0)
                                    )
                                }
                            } else {
                                emptyList()
                            }
                        )
                    }
                )
            }
        ))
    }
    BVMobileTheme {
        VideoPlayerPartSheetContent(
            currentCid = 102,
            interactiveNodes = emptyList(),
            pages = emptyList(),
            ugcSeason = ugcSeason,
            pgcSections = emptyList(),
            onClickInteractiveNode = {},
            onClickPage = {},
            onClickEpisode = {}
        )
    }
}

@Preview
@Composable
private fun VideoPlayerPartSheetContentPgcSectionsPreview() {
    val pages = List(10) {
        VideoPage(
            cid = it.toLong(),
            index = it,
            title = "Page title $it",
            duration = 1,
            dimension = Dimension(0, 0)
        )
    }
    BVMobileTheme {
        VideoPlayerPartSheetContent(
            currentCid = 1,
            interactiveNodes = emptyList(),
            pages = pages,
            ugcSeason = null,
            pgcSections = emptyList(),
            onClickInteractiveNode = {},
            onClickPage = {},
            onClickEpisode = {}
        )
    }
}
