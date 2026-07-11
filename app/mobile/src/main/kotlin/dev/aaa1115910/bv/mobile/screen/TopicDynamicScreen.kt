package dev.aaa1115910.bv.mobile.screen

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.entity.user.DynamicItem
import dev.aaa1115910.biliapi.entity.user.DynamicTopicFeedItem
import dev.aaa1115910.biliapi.entity.user.DynamicType
import dev.aaa1115910.bv.mobile.activities.DynamicDetailActivity
import dev.aaa1115910.bv.mobile.activities.VideoPlayerActivity
import dev.aaa1115910.bv.mobile.component.home.dynamic.DynamicItem as DynamicCard
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.home.TopicDynamicViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TopicDynamicScreen(
    topicId: Long,
    topicName: String,
    modifier: Modifier = Modifier,
    viewModel: TopicDynamicViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                ?: return@derivedStateOf viewModel.items.isNotEmpty()
            lastVisible.index >= listState.layoutInfo.totalItemsCount - 5
        }
    }

    LaunchedEffect(topicId, topicName) {
        viewModel.setTopic(topicId, topicName)
        viewModel.refresh()
    }
    LaunchedEffect(shouldLoadMore, viewModel.hasMore) {
        if (shouldLoadMore && viewModel.hasMore) {
            viewModel.loadMore()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = viewModel.topicName.ifBlank { "\u8bdd\u9898\u52a8\u6001" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "\u8fd4\u56de"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            state = pullToRefreshState,
            isRefreshing = viewModel.refreshing,
            onRefresh = {
                scope.launch { viewModel.refresh() }
            }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (viewModel.errorMessage != null && viewModel.items.isNotEmpty()) {
                    item {
                        TopicErrorCard(
                            message = viewModel.errorMessage.orEmpty(),
                            onRetry = { scope.launch { viewModel.loadMore() } }
                        )
                    }
                }

                itemsIndexed(
                    items = viewModel.items,
                    key = { index, item ->
                        when (item) {
                            is DynamicTopicFeedItem.DynamicCard ->
                                "dynamic-" + (item.dynamic.id ?: item.dynamic.jumpUrl ?: index)
                            is DynamicTopicFeedItem.FoldCard -> "fold-" + index + "-" + item.count
                        }
                    }
                ) { index, item ->
                    when (item) {
                        is DynamicTopicFeedItem.DynamicCard -> {
                            DynamicCard(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                dynamicItem = item.dynamic,
                                onClick = { dynamic -> openTopicDynamic(context, dynamic) }
                            )
                        }

                        is DynamicTopicFeedItem.FoldCard -> {
                            TopicFoldCard(
                                fold = item,
                                expanding = viewModel.expandingFold,
                                onClick = { scope.launch { viewModel.expandFold(index) } }
                            )
                        }
                    }
                }

                if (viewModel.loading && viewModel.items.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator()
                        }
                    }
                }

                if (viewModel.items.isEmpty() && !viewModel.loading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = viewModel.errorMessage ?: "\u6682\u65e0\u8bdd\u9898\u52a8\u6001"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicFoldCard(
    fold: DynamicTopicFeedItem.FoldCard,
    expanding: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clickable(enabled = !expanding, onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = fold.description.ifBlank {
                    "\u5c55\u5f00 " + fold.count + " \u6761\u88ab\u6298\u53e0\u7684\u52a8\u6001"
                }
            )
            if (expanding) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "\u5c55\u5f00",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun TopicErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = message,
            color = MaterialTheme.colorScheme.error,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        TextButton(onClick = onRetry) {
            Text("\u91cd\u8bd5")
        }
    }
}

private fun openTopicDynamic(context: Context, dynamicItem: DynamicItem) {
    when (dynamicItem.type) {
        DynamicType.Av, DynamicType.Pgc -> {
            VideoPlayerActivity.actionStart(context = context, dynamicItem = dynamicItem)
        }

        else -> {
            dynamicItem.id?.let { DynamicDetailActivity.actionStart(context, it) }
                ?: "\u539f\u52a8\u6001\u4e0d\u5b58\u5728".toast(context)
        }
    }
}
