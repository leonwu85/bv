package dev.aaa1115910.bv.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.repositories.OpusCard
import dev.aaa1115910.bv.viewmodel.user.OpusSpaceViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun OpusBrowser(mid: Long, tv: Boolean, onBack: () -> Unit, onOpen: (String) -> Unit, viewModel: OpusSpaceViewModel = koinViewModel()) {
    LaunchedEffect(mid) { viewModel.initialize(mid) }
    val collection = viewModel.selectedCollection
    val back = { if (collection != null) viewModel.selectCollection(null) else onBack() }
    BackHandler(onBack = back)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (tv) focus.requestFocus() }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(if (tv) 28.dp else 16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OpusAction("返回", tv, Modifier.focusRequester(focus), back)
                Text(collection?.title ?: "图文与文集", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).padding(vertical = 10.dp))
            }
            BoxWithConstraints(Modifier.weight(1f)) {
                val minWidth = if (tv) 180.dp else 140.dp
                val columns = (maxWidth / (minWidth + 12.dp)).toInt().coerceAtLeast(1)
                val grid = rememberLazyGridState()
                LaunchedEffect(collection?.id) { grid.scrollToItem(0) }
                LazyVerticalGrid(columns = GridCells.Adaptive(minWidth), state = grid,
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (collection == null && viewModel.collections.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) { Text("文集", style = MaterialTheme.typography.titleMedium) }
                        items(if (viewModel.expanded) viewModel.collections else viewModel.collections.take(columns), key = { "collection:${it.id}" }) { card ->
                            OpusBrowserCard(card, tv) { viewModel.selectCollection(card) }
                        }
                        if (viewModel.collections.size > columns) item(span = { GridItemSpan(maxLineSpan) }) {
                            OpusAction(if (viewModel.expanded) "收起文集" else "展开全部 ${viewModel.collections.size} 个文集", tv) { viewModel.expanded = !viewModel.expanded }
                        }
                        item(span = { GridItemSpan(maxLineSpan) }) { Text("全部图文", style = MaterialTheme.typography.titleMedium) }
                    }
                    val cards = if (collection == null) viewModel.items else viewModel.collectionItems
                    items(cards, key = { "opus:${it.id}" }) { card -> OpusBrowserCard(card, tv) { onOpen(card.url) } }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (viewModel.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                            viewModel.error?.let { Text(it) }
                            if (!viewModel.loading && cards.isEmpty() && viewModel.error == null) Text("暂无图文")
                            if (!viewModel.loading && (viewModel.error != null || collection == null && viewModel.hasMore)) {
                                OpusAction(if (viewModel.error == null) "加载更多" else "重试", tv) { viewModel.load() }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OpusBrowserCard(card: OpusCard, tv: Boolean, onClick: () -> Unit) {
    val content: @Composable () -> Unit = {
        Column {
            if (card.cover.isNotBlank()) AsyncImage(model = card.cover, contentDescription = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.35f), contentScale = ContentScale.Crop)
            Column(Modifier.padding(12.dp)) {
                Text(card.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                if (card.subtitle.isNotBlank()) Text(card.subtitle, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (tv) androidx.tv.material3.Surface(onClick = onClick, modifier = Modifier.fillMaxWidth()) { content() }
    else Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) { content() }
}

@Composable
fun OpusAction(text: String, tv: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    if (tv) androidx.tv.material3.Button(onClick = onClick, modifier = modifier) { androidx.tv.material3.Text(text) }
    else TextButton(onClick = onClick, modifier = modifier) { Text(text) }
}
