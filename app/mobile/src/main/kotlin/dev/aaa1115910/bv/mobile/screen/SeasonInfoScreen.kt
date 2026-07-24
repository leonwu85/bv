@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.aaa1115910.bv.mobile.screen

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.video.season.Episode
import dev.aaa1115910.biliapi.entity.video.season.SeasonDetail
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.mobile.activities.VideoPlayerActivity
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.removeHtmlTags
import dev.aaa1115910.bv.util.resizedImageUrl
import dev.aaa1115910.bv.viewmodel.CommentViewModel
import dev.aaa1115910.bv.viewmodel.SeasonViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private val BiliPink = Color(0xFFFF6699)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonInfoScreen(
    modifier: Modifier = Modifier,
    seasonViewModel: SeasonViewModel = koinViewModel(),
    commentViewModel: CommentViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val activity = context as Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val appBarThreshold = with(LocalDensity.current) { 176.dp.roundToPx() }
    var showComments by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var pausedForPlayback by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val epId = activity.intent.getIntExtra("epid", 0).takeIf { it > 0 }
        val seasonId = activity.intent.getIntExtra("seasonid", 0).takeIf { it > 0 }
        val proxyAreaOrdinal = activity.intent.getIntExtra("proxy_area", ProxyArea.MainLand.ordinal)
        seasonViewModel.epId = epId
        seasonViewModel.seasonId = seasonId
        seasonViewModel.proxyArea =
            ProxyArea.entries.getOrElse(proxyAreaOrdinal) { ProxyArea.MainLand }
        if (seasonViewModel.epId != null || seasonViewModel.seasonId != null) {
            seasonViewModel.updateSeasonData()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> pausedForPlayback = true
                Lifecycle.Event.ON_RESUME -> {
                    if (pausedForPlayback && seasonViewModel.seasonData != null) {
                        pausedForPlayback = false
                        scope.launch(Dispatchers.IO) {
                            seasonViewModel.updateLastPlayProgress()
                        }
                    }
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val seasonData = seasonViewModel.seasonData
    val allEpisodes = remember(seasonData) { seasonData?.allEpisodes().orEmpty() }
    val lastPlayProgress = seasonViewModel.lastPlayProgress
    val historyEpisode = remember(allEpisodes, lastPlayProgress) {
        allEpisodes.findEpisode(lastPlayProgress?.lastEpId)
    }
    val requestedEpisode = remember(allEpisodes, seasonViewModel.epId) {
        allEpisodes.findEpisode(seasonViewModel.epId)
    }
    val primaryEpisode = historyEpisode ?: requestedEpisode ?: allEpisodes.firstOrNull()

    val appBarTransition by remember(seasonData, listState, appBarThreshold) {
        derivedStateOf {
            appBarTransitionFraction(
                hasContent = seasonData != null,
                firstVisibleItemIndex = listState.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
                threshold = appBarThreshold
            )
        }
    }
    val appBarContainerColor = lerp(
        start = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
        stop = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        fraction = appBarTransition
    )
    val appBarContentColor = lerp(
        start = Color.White,
        stop = MaterialTheme.colorScheme.onSurface,
        fraction = appBarTransition
    )

    val playEpisode: (Episode, Boolean) -> Unit = { episode, resumeHistory ->
        VideoPlayerActivity.actionStart(
            context = context,
            aid = episode.aid,
            cid = episode.cid,
            fromSeason = true,
            cover = episode.cover.ifBlank { seasonData?.cover.orEmpty() },
            title = seasonData?.title.orEmpty(),
            partTitle = episode.longTitle.ifBlank { episode.title },
            play = episode.viewCount,
            danmaku = episode.danmakuCount,
            epid = episode.epid ?: episode.id.takeIf { it > 0 },
            seasonId = seasonData?.seasonId,
            resumeHistory = resumeHistory
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (seasonData == null) {
            SeasonLoadingState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp),
                tip = seasonViewModel.tip
            )
        } else {
            SeasonInfoContent(
                modifier = Modifier.fillMaxSize(),
                listState = listState,
                seasonData = seasonData,
                lastPlayProgress = lastPlayProgress,
                historyEpisode = historyEpisode,
                primaryEpisode = primaryEpisode,
                onContinuePlay = {
                    primaryEpisode?.let { episode ->
                        playEpisode(
                            episode,
                            historyEpisode != null && episode.sameEpisodeAs(historyEpisode)
                        )
                    }
                },
                onShowComments = { showComments = true },
                onShowSettings = { showSettings = true },
                onClickEpisode = { episode ->
                    playEpisode(
                        episode,
                        historyEpisode != null && episode.sameEpisodeAs(historyEpisode)
                    )
                }
            )
        }

        TopAppBar(
            title = {
                Text(
                    text = seasonData?.title.orEmpty(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = activity::finish) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = appBarContainerColor,
                navigationIconContentColor = appBarContentColor,
                titleContentColor = appBarContentColor
            )
        )
    }

    if (showComments && seasonData != null && allEpisodes.isNotEmpty()) {
        SeasonCommentsSheet(
            seasonTitle = seasonData.title,
            episodes = allEpisodes,
            initialEpisodeId = historyEpisode?.id ?: primaryEpisode?.id,
            commentViewModel = commentViewModel,
            onDismissRequest = { showComments = false }
        )
    }

    if (showSettings && seasonData != null) {
        SeasonSettingsSheet(
            seasonData = seasonData,
            episodeCount = allEpisodes.size,
            onDismissRequest = { showSettings = false }
        )
    }
}

@Composable
private fun SeasonLoadingState(
    modifier: Modifier = Modifier,
    tip: String
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (tip == "Loading") {
            CircularProgressIndicator()
        } else {
            Text(
                text = tip,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SeasonInfoContent(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    seasonData: SeasonDetail,
    lastPlayProgress: SeasonDetail.UserStatus.Progress?,
    historyEpisode: Episode?,
    primaryEpisode: Episode?,
    onContinuePlay: () -> Unit,
    onShowComments: () -> Unit,
    onShowSettings: () -> Unit,
    onClickEpisode: (Episode) -> Unit
) {
    val heroEpisode = historyEpisode ?: primaryEpisode
    val heroCover = heroEpisode?.cover?.takeIf { it.isNotBlank() } ?: seasonData.cover
    var descriptionExpanded by rememberSaveable(seasonData.seasonId) { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item(key = "hero") {
            SeasonHero(
                cover = heroCover,
                title = seasonData.title
            )
        }

        item(key = "summary") {
            SeasonSummary(
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp),
                seasonData = seasonData
            )
        }

        primaryEpisode?.let { episode ->
            item(key = "resume") {
                ResumePlaybackButton(
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp),
                    episode = episode,
                    progress = lastPlayProgress.takeIf {
                        historyEpisode != null && episode.sameEpisodeAs(historyEpisode)
                    },
                    onClick = onContinuePlay
                )
            }
        }

        item(key = "actions") {
            SeasonActions(
                modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp),
                onShowComments = onShowComments,
                onShowSettings = onShowSettings
            )
        }

        if (seasonData.description.isNotBlank()) {
            item(key = "description") {
                SeasonDescription(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    description = seasonData.description.removeHtmlTags(),
                    expanded = descriptionExpanded,
                    onToggleExpanded = { descriptionExpanded = !descriptionExpanded }
                )
            }
        } else {
            item(key = "summary-bottom-space") {
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        item(key = "episode-divider") {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        }

        if (seasonData.episodes.isNotEmpty()) {
            item(key = "positive-title") {
                EpisodeSectionTitle(
                    modifier = Modifier.padding(start = 20.dp, top = 22.dp, end = 20.dp),
                    title = stringResource(R.string.season_info_episode_section),
                    count = seasonData.episodes.size
                )
            }
            itemsIndexed(
                items = seasonData.episodes,
                key = { index, episode -> "positive:$index:${episode.id}" }
            ) { _, episode ->
                EpisodeListItem(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    episode = episode,
                    isHistory = historyEpisode?.let(episode::sameEpisodeAs) == true,
                    historyTimeSeconds = lastPlayProgress?.lastTime ?: 0,
                    onClick = { onClickEpisode(episode) }
                )
            }
        }

        seasonData.sections.forEachIndexed { sectionIndex, section ->
            item(key = "section-title:$sectionIndex:${section.id}") {
                EpisodeSectionTitle(
                    modifier = Modifier.padding(start = 20.dp, top = 22.dp, end = 20.dp),
                    title = section.title,
                    count = section.episodes.size
                )
            }
            itemsIndexed(
                items = section.episodes,
                key = { episodeIndex, episode ->
                    "section:$sectionIndex:${section.id}:$episodeIndex:${episode.id}"
                }
            ) { _, episode ->
                EpisodeListItem(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    episode = episode,
                    isHistory = historyEpisode?.let(episode::sameEpisodeAs) == true,
                    historyTimeSeconds = lastPlayProgress?.lastTime ?: 0,
                    onClick = { onClickEpisode(episode) }
                )
            }
        }

        item(key = "bottom-space") {
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun SeasonHero(
    modifier: Modifier = Modifier,
    cover: String,
    title: String
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = cover.resizedImageUrl(ImageSize.LargeCover),
            contentDescription = "$title 剧集封面",
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.56f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        )
    }
}

@Composable
private fun SeasonSummary(
    modifier: Modifier = Modifier,
    seasonData: SeasonDetail
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = seasonData.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (seasonData.publish.publishDate.isNotBlank()) {
            Text(
                text = seasonData.publish.publishDate,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (seasonData.styles.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                seasonData.styles.forEach { style ->
                    SeasonMetadataTag(text = style)
                }
            }
        }
    }
}

@Composable
private fun SeasonMetadataTag(
    modifier: Modifier = Modifier,
    text: String
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1
        )
    }
}

@Composable
private fun ResumePlaybackButton(
    modifier: Modifier = Modifier,
    episode: Episode,
    progress: SeasonDetail.UserStatus.Progress?,
    onClick: () -> Unit
) {
    val episodeLabel = resumeEpisodeLabel(
        progressIndex = progress?.lastEpIndex,
        episodeTitle = episode.title
    )
    val title = if (progress != null) {
        stringResource(R.string.season_info_continue_play, episodeLabel)
    } else {
        stringResource(R.string.season_info_start_play, episodeLabel)
    }
    val lastTime = progress?.lastTime
        ?.takeIf { it > 0 }
        ?.let { (it * 1000L).formatHourMinSec() }
    val semanticsDescription = listOfNotNull(
        title,
        lastTime?.let { stringResource(R.string.season_info_last_watched, it) }
    ).joinToString("，")

    Button(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 78.dp)
            .semantics { contentDescription = semanticsDescription },
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BiliPink,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp)
    ) {
        Icon(
            modifier = Modifier.size(34.dp),
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            lastTime?.let {
                Text(
                    text = stringResource(R.string.season_info_last_watched, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.86f)
                )
            }
        }
    }
}

@Composable
private fun SeasonActions(
    modifier: Modifier = Modifier,
    onShowComments: () -> Unit,
    onShowSettings: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SeasonAction(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.season_info_comments),
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Comment,
                    contentDescription = null
                )
            },
            onClick = onShowComments
        )
        SeasonAction(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.season_info_settings),
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null
                )
            },
            onClick = onShowSettings
        )
    }
}

@Composable
private fun SeasonAction(
    modifier: Modifier = Modifier,
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.heightIn(min = 62.dp),
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SeasonDescription(
    modifier: Modifier = Modifier,
    description: String,
    expanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = onToggleExpanded,
        color = Color.Transparent,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .rotate(if (expanded) 180f else 0f),
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "收起简介" else "展开简介",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EpisodeSectionTitle(
    modifier: Modifier = Modifier,
    title: String,
    count: Int
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            modifier = Modifier.padding(start = 10.dp, bottom = 2.dp),
            text = stringResource(R.string.season_info_episode_count, count),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EpisodeListItem(
    modifier: Modifier = Modifier,
    episode: Episode,
    isHistory: Boolean,
    historyTimeSeconds: Int,
    onClick: () -> Unit
) {
    val progress = episodeProgress(
        lastTimeSeconds = historyTimeSeconds,
        duration = episode.duration
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (isHistory) BiliPink.copy(alpha = 0.055f) else Color.Transparent,
        border = if (isHistory) {
            BorderStroke(1.dp, BiliPink.copy(alpha = 0.68f))
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 108.dp)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.width(146.dp)
            ) {
                AsyncImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.6f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    model = episode.cover.resizedImageUrl(ImageSize.SmallVideoCardCover),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
                if (isHistory && progress > 0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .height(3.dp)
                            .clip(CircleShape),
                        color = BiliPink,
                        trackColor = BiliPink.copy(alpha = 0.18f)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = buildEpisodeTitle(episode),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isHistory) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isHistory) BiliPink else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    episode.badge.takeIf { it.isNotBlank() }?.let {
                        EpisodeBadge(text = it)
                    }
                    if (isHistory) {
                        Text(
                            text = stringResource(R.string.season_info_last_watched_badge),
                            style = MaterialTheme.typography.labelMedium,
                            color = BiliPink
                        )
                    }
                    Text(
                        text = normalizedEpisodeDurationMillis(episode.duration).formatHourMinSec(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeBadge(
    text: String
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = BiliPink.copy(alpha = 0.12f)
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = BiliPink,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeasonSettingsSheet(
    seasonData: SeasonDetail,
    episodeCount: Int,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.season_info_settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                AsyncImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    model = seasonData.allEpisodes()
                        .firstOrNull()
                        ?.cover
                        ?.resizedImageUrl(ImageSize.LargeCover)
                        ?: seasonData.cover.resizedImageUrl(ImageSize.SeasonCoverThumbnail),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }
            seasonData.originTitle?.takeIf { it.isNotBlank() }?.let { originTitle ->
                item {
                    SeasonSettingRow(
                        label = stringResource(R.string.season_info_original_title),
                        value = originTitle
                    )
                }
            }
            if (seasonData.styles.isNotEmpty()) {
                item {
                    SeasonSettingRow(
                        label = stringResource(R.string.season_info_genres),
                        value = seasonData.styles.joinToString(" / ")
                    )
                }
            }
            if (seasonData.publish.publishDate.isNotBlank()) {
                item {
                    SeasonSettingRow(
                        label = stringResource(R.string.season_info_published_at),
                        value = seasonData.publish.publishDate
                    )
                }
            }
            if (seasonData.newEpDesc.isNotBlank()) {
                item {
                    SeasonSettingRow(
                        label = stringResource(R.string.season_info_latest_update),
                        value = seasonData.newEpDesc
                    )
                }
            }
            item {
                SeasonSettingRow(
                    label = stringResource(R.string.season_info_episode_section),
                    value = stringResource(R.string.season_info_episode_count, episodeCount)
                )
            }
            if (seasonData.description.isNotBlank()) {
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.season_info_description),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = seasonData.description.removeHtmlTags(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonSettingRow(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

internal fun SeasonDetail.allEpisodes(): List<Episode> {
    return buildList {
        addAll(episodes)
        sections.forEach { addAll(it.episodes) }
    }.distinctBy { episode ->
        when {
            episode.id > 0 -> "id:${episode.id}"
            episode.epid != null -> "epid:${episode.epid}"
            else -> "aid:${episode.aid}:cid:${episode.cid}"
        }
    }
}

internal fun List<Episode>.findEpisode(episodeId: Int?): Episode? {
    if (episodeId == null || episodeId <= 0) return null
    return firstOrNull { episode ->
        episode.id == episodeId || episode.epid == episodeId
    }
}

internal fun Episode.sameEpisodeAs(other: Episode): Boolean {
    return when {
        id > 0 && other.id > 0 -> id == other.id
        epid != null && other.epid != null -> epid == other.epid
        else -> aid == other.aid && cid == other.cid
    }
}

internal fun normalizedEpisodeDurationMillis(duration: Int): Long {
    if (duration <= 0) return 0L
    return if (duration >= 100_000) duration.toLong() else duration * 1000L
}

internal fun appBarTransitionFraction(
    hasContent: Boolean,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    threshold: Int
): Float {
    if (!hasContent || firstVisibleItemIndex > 0) return 1f
    return (
        firstVisibleItemScrollOffset /
            threshold.coerceAtLeast(1).toFloat()
        ).coerceIn(0f, 1f)
}

internal fun episodeProgress(
    lastTimeSeconds: Int,
    duration: Int
): Float {
    if (lastTimeSeconds <= 0 || duration <= 0) return 0f
    val durationSeconds = normalizedEpisodeDurationMillis(duration) / 1000f
    if (durationSeconds <= 0f) return 0f
    return (lastTimeSeconds / durationSeconds).coerceIn(0f, 1f)
}

internal fun resumeEpisodeLabel(
    progressIndex: String?,
    episodeTitle: String
): String {
    val raw = progressIndex?.trim().takeUnless { it.isNullOrBlank() }
        ?: episodeTitle.trim()
    return raw.toIntOrNull()?.let { "第${it}集" }
        ?: raw.ifBlank { "本集" }
}

internal fun buildEpisodeTitle(episode: Episode): String {
    return listOf(episode.title, episode.longTitle)
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(" ")
        .ifBlank { "EP${episode.epid ?: episode.id}" }
}
