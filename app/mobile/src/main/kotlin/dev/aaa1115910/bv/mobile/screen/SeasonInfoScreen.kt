package dev.aaa1115910.bv.mobile.screen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import dev.aaa1115910.bv.viewmodel.SeasonViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonInfoScreen(
    modifier: Modifier = Modifier,
    seasonViewModel: SeasonViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val activity = context as Activity
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(Unit) {
        val epId = activity.intent.getIntExtra("epid", 0).takeIf { it > 0 }
        val seasonId = activity.intent.getIntExtra("seasonid", 0).takeIf { it > 0 }
        val proxyAreaOrdinal = activity.intent.getIntExtra("proxy_area", ProxyArea.MainLand.ordinal)
        seasonViewModel.epId = epId
        seasonViewModel.seasonId = seasonId
        seasonViewModel.proxyArea = ProxyArea.entries.getOrElse(proxyAreaOrdinal) { ProxyArea.MainLand }
        if (seasonViewModel.epId != null || seasonViewModel.seasonId != null) {
            seasonViewModel.updateSeasonData()
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.title_activity_season_info)) },
                navigationIcon = {
                    IconButton(onClick = { activity.finish() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        val seasonData = seasonViewModel.seasonData
        if (seasonData == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                if (seasonViewModel.tip == "Loading") {
                    CircularProgressIndicator()
                } else {
                    Text(
                        text = seasonViewModel.tip,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            SeasonInfoContent(
                modifier = Modifier.padding(innerPadding),
                seasonData = seasonData,
                onClickEpisode = { episode ->
                    VideoPlayerActivity.actionStart(
                        context = context,
                        aid = episode.aid,
                        fromSeason = true,
                        cover = episode.cover.ifBlank { seasonData.cover },
                        title = seasonData.title,
                        partTitle = episode.longTitle.ifBlank { episode.title },
                        play = episode.viewCount,
                        danmaku = episode.danmakuCount,
                        epid = episode.epid,
                        seasonId = seasonData.seasonId
                    )
                }
            )
        }
    }
}

@Composable
private fun SeasonInfoContent(
    modifier: Modifier = Modifier,
    seasonData: SeasonDetail,
    onClickEpisode: (Episode) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SeasonHeader(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                seasonData = seasonData
            )
        }

        if (seasonData.episodes.isNotEmpty()) {
            item {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = "剧集",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            itemsIndexed(
                items = seasonData.episodes,
                key = { index, episode -> "positive:$index:${episode.id}" }
            ) { _, episode ->
                EpisodeListItem(
                    episode = episode,
                    onClick = { onClickEpisode(episode) }
                )
            }
        }

        seasonData.sections.forEachIndexed { sectionIndex, section ->
            item {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            itemsIndexed(
                items = section.episodes,
                key = { episodeIndex, episode ->
                    "section:$sectionIndex:${section.id}:$episodeIndex:${episode.id}"
                }
            ) { _, episode ->
                EpisodeListItem(
                    episode = episode,
                    onClick = { onClickEpisode(episode) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun SeasonHeader(
    modifier: Modifier = Modifier,
    seasonData: SeasonDetail
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AsyncImage(
                modifier = Modifier
                    .width(112.dp)
                    .aspectRatio(0.75f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                model = seasonData.cover.resizedImageUrl(ImageSize.SeasonCoverThumbnail),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = seasonData.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                seasonData.originTitle?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (seasonData.publish.publishDate.isNotBlank()) {
                    Text(
                        text = seasonData.publish.publishDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
                if (seasonData.styles.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        seasonData.styles.take(3).forEach { style ->
                            AssistChip(
                                onClick = {},
                                label = { Text(text = style) }
                            )
                        }
                    }
                }
            }
        }

        if (seasonData.description.isNotBlank()) {
            Text(
                text = seasonData.description.removeHtmlTags(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun EpisodeListItem(
    modifier: Modifier = Modifier,
    episode: Episode,
    onClick: () -> Unit
) {
    Surface(onClick = onClick) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(96.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AsyncImage(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1.6f)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                model = episode.cover.resizedImageUrl(ImageSize.SmallVideoCardCover),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = buildEpisodeTitle(episode),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    episode.badge.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = (episode.duration * 1000L).formatHourMinSec(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                    )
                }
            }
        }
    }
}

private fun buildEpisodeTitle(episode: Episode): String {
    return listOf(episode.title, episode.longTitle)
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(" ")
        .ifBlank { "EP${episode.epid ?: episode.id}" }
}
