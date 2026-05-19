package dev.aaa1115910.bv.mobile.screen

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.season.FollowingSeasonType
import dev.aaa1115910.biliapi.entity.user.DynamicItem
import dev.aaa1115910.biliapi.entity.user.DynamicType
import dev.aaa1115910.biliapi.entity.user.SpaceVideo
import dev.aaa1115910.biliapi.http.entity.user.AppUserSpaceElecUser
import dev.aaa1115910.biliapi.http.entity.user.AppUserSpaceTag
import dev.aaa1115910.biliapi.http.entity.user.favorite.SpaceFavoriteData
import dev.aaa1115910.biliapi.http.entity.user.favorite.SpaceFavoriteItem
import dev.aaa1115910.bv.mobile.activities.DynamicDetailActivity
import dev.aaa1115910.bv.mobile.activities.SeasonInfoActivity
import dev.aaa1115910.bv.mobile.activities.VideoPlayerActivity
import dev.aaa1115910.bv.entity.carddata.SeasonCardData
import dev.aaa1115910.bv.mobile.component.home.dynamic.DynamicItem as DynamicItemCard
import dev.aaa1115910.bv.mobile.component.user.UserAvatar
import dev.aaa1115910.bv.mobile.component.videocard.SeasonCard
import dev.aaa1115910.bv.mobile.component.videocard.UpSpaceVideoItem
import dev.aaa1115910.bv.util.OnBottomReached
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.getDisplayName
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.user.UserSpaceTab
import dev.aaa1115910.bv.viewmodel.user.UserSpaceViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun UserSpaceScreen(
    modifier: Modifier = Modifier,
    userSpaceViewModel: UserSpaceViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val refreshState = rememberPullToRefreshState()
    val tabs = userSpaceViewModel.availableTabs
    val selectedTab = userSpaceViewModel.selectedTab
    val selectedTabIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    val refreshing = userSpaceViewModel.profileLoading ||
            userSpaceViewModel.videoLoading ||
            userSpaceViewModel.dynamicLoading ||
            userSpaceViewModel.favoriteLoading ||
            userSpaceViewModel.bangumiLoading
    val appBarOverHeader = listState.firstVisibleItemIndex == 0 &&
            listState.firstVisibleItemScrollOffset < 120
    val appBarContainerColor = if (appBarOverHeader) Color.Transparent else MaterialTheme.colorScheme.surface
    val appBarContentColor = if (appBarOverHeader) Color.White else MaterialTheme.colorScheme.onSurface

    LaunchedEffect(Unit) {
        val intent = (context as Activity).intent
        if (intent.hasExtra("mid")) {
            userSpaceViewModel.initialize(
                mid = intent.getLongExtra("mid", 0),
                name = intent.getStringExtra("name").orEmpty(),
                face = intent.getStringExtra("face").orEmpty()
            )
        } else {
            context.finish()
        }
    }

    LaunchedEffect(selectedTab) {
        if (listState.firstVisibleItemIndex > 0) {
            listState.animateScrollToItem(1)
        } else {
            listState.animateScrollToItem(0)
        }
    }

    listState.OnBottomReached(loading = refreshing) {
        userSpaceViewModel.loadMoreForSelectedTab()
    }

    BackHandler {
        (context as Activity).finish()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { (context as Activity).finish() }) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = userSpaceViewModel::refresh) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    UserSpaceMoreMenu(
                        viewModel = userSpaceViewModel,
                        onShare = {
                            shareUserSpace(context, userSpaceViewModel.upMid, userSpaceViewModel.upName)
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBarContainerColor,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = appBarContentColor,
                    actionIconContentColor = appBarContentColor
                )
            )
        }
    ) {
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize(),
            state = refreshState,
            isRefreshing = refreshing,
            onRefresh = userSpaceViewModel::refresh
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(selectedTabIndex, tabs.size) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                totalDrag += dragAmount
                            },
                            onDragEnd = {
                                val threshold = 96f
                                if (abs(totalDrag) >= threshold) {
                                    val targetIndex = when {
                                        totalDrag < 0f -> (selectedTabIndex + 1).coerceAtMost(tabs.lastIndex)
                                        else -> (selectedTabIndex - 1).coerceAtLeast(0)
                                    }
                                    if (targetIndex != selectedTabIndex) {
                                        userSpaceViewModel.selectTab(tabs[targetIndex])
                                    }
                                }
                            }
                        )
                    },
                state = listState,
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                item {
                    UserSpaceHeader(
                        viewModel = userSpaceViewModel,
                        onFollow = {
                            if (!Prefs.isLogin) {
                                "请先登录".toast(context)
                            } else {
                                userSpaceViewModel.toggleFollow { followed, success ->
                                    (if (success) {
                                        if (followed) "关注成功" else "已取消关注"
                                    } else {
                                        if (followed) "关注失败" else "取消关注失败"
                                    }).toast(context)
                                }
                            }
                        },
                        onLiveClick = {
                            val live = userSpaceViewModel.liveRoom
                            if (live != null && live.roomStatus == 1) {
                                VideoPlayerActivity.actionStartLive(
                                    context = context,
                                    roomId = live.roomId,
                                    title = live.title,
                                    upName = userSpaceViewModel.upName,
                                    upFace = userSpaceViewModel.upFace,
                                    upMid = userSpaceViewModel.upMid
                                )
                            }
                        }
                    )
                }

                stickyHeader {
                    PrimaryScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        edgePadding = 0.dp,
                        minTabWidth = 0.dp,
                        divider = {},
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            Tab(
                                selected = index == selectedTabIndex,
                                onClick = {
                                    userSpaceViewModel.selectTab(tab)
                                    scope.launch {
                                        if (listState.firstVisibleItemIndex > 0) {
                                            listState.animateScrollToItem(1)
                                        }
                                    }
                                },
                                text = { Text(text = tab.title) }
                            )
                        }
                    }
                }

                when (selectedTab) {
                    UserSpaceTab.Home -> userSpaceHomeContent(
                        viewModel = userSpaceViewModel,
                        onSelectTab = userSpaceViewModel::selectTab,
                        onClickDynamic = { openDynamicItem(context, it) },
                        onClickVideo = { openSpaceVideo(context, it) }
                    )

                    UserSpaceTab.Dynamic -> userSpaceDynamicContent(
                        viewModel = userSpaceViewModel,
                        onClickDynamic = { openDynamicItem(context, it) }
                    )

                    UserSpaceTab.Video -> userSpaceVideoContent(
                        viewModel = userSpaceViewModel,
                        onClickVideo = { openSpaceVideo(context, it) }
                    )

                    UserSpaceTab.Favorite -> userSpaceFavoriteContent(
                        viewModel = userSpaceViewModel,
                        onClickFavorite = { openFavoriteItem(context, it) },
                        onLoadMoreGroup = userSpaceViewModel::loadMoreFavoriteGroup,
                        onToggleGroup = userSpaceViewModel::toggleFavoriteGroup
                    )

                    UserSpaceTab.Bangumi -> userSpaceBangumiContent(
                        viewModel = userSpaceViewModel,
                        onTypeChange = userSpaceViewModel::selectFollowingSeasonType,
                        onClickSeason = {
                            SeasonInfoActivity.actionStart(
                                context = context,
                                seasonId = it.seasonId
                            )
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }
    }
}

@Composable
private fun UserSpaceHeader(
    viewModel: UserSpaceViewModel,
    onFollow: () -> Unit,
    onLiveClick: () -> Unit
) {
    val context = LocalContext.current
    val profile = viewModel.userInfo
    val card = viewModel.userCardInfo
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            val topPhoto = viewModel.topPhoto
            if (topPhoto.isNotBlank()) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = topPhoto,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = BiasAlignment(0f, viewModel.topPhotoAlignmentY)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.05f),
                                Color.Black.copy(alpha = 0.55f)
                            )
                        )
                    )
            )
            if (viewModel.chargeTotal > 0 || viewModel.chargeUsers.isNotEmpty()) {
                UserSpaceChargeRow(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 92.dp, end = 16.dp),
                    users = viewModel.chargeUsers,
                    count = viewModel.chargeTotal,
                    onClick = {
                        openUrl(context, userSpaceChargeRankUrl(viewModel.upMid))
                    }
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UserAvatar(
                    modifier = Modifier.clip(CircleShape),
                    avatar = viewModel.upFace,
                    size = 76.dp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = viewModel.upName.ifBlank { "UP 主" },
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ElevatedAssistChip(
                            onClick = {},
                            label = { Text(text = "LV${profile?.level ?: card?.card?.levelInfo?.currentLevel ?: 0}") }
                        )
                        val vipText = profile?.vip?.label?.text ?: card?.card?.vip?.label?.text
                        if (!vipText.isNullOrBlank()) {
                            AssistChip(onClick = {}, label = { Text(text = vipText) })
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (viewModel.profileError != null) {
                Text(
                    text = viewModel.profileError.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (viewModel.sign.isNotBlank()) {
                Text(
                    text = viewModel.sign,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            UserSpaceExtraInfo(
                uid = viewModel.displayUid,
                tags = viewModel.visibleSpaceTags,
                onCopyUid = {
                    copyText(context, "UID", viewModel.displayUid)
                    "已复制 UID".toast(context)
                },
                onOpenTag = { tag ->
                    tag.uri?.takeIf(String::isNotBlank)?.let { openUrl(context, it) }
                }
            )
            val official = profile?.official?.title
                ?: card?.card?.officialVerify?.desc
                ?: card?.card?.official?.title
            if (!official.isNullOrBlank()) {
                Text(
                    text = official,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                UserSpaceStat(count = viewModel.friend, label = "关注")
                UserSpaceStat(count = viewModel.fans, label = "粉丝")
                UserSpaceStat(count = viewModel.likeCount, label = "获赞")
                UserSpaceStat(count = viewModel.archiveCount, label = "投稿")
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onFollow,
                    enabled = !viewModel.relationLoading && viewModel.upMid != Prefs.uid,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (viewModel.isFollowing == true) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    )
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = if (viewModel.isFollowing == true) Icons.Default.Done else Icons.Default.Favorite,
                        contentDescription = null
                    )
                    Text(
                        modifier = Modifier.padding(start = 6.dp),
                        text = when {
                            viewModel.upMid == Prefs.uid -> "自己"
                            viewModel.isFollowing == true -> "已关注"
                            else -> "关注"
                        }
                    )
                }
                if (viewModel.upMid != Prefs.uid) {
                    FilledTonalButton(
                        onClick = { "私信功能暂未实现".toast(context) }
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.Rounded.Email,
                            contentDescription = null
                        )
                        Text(
                            modifier = Modifier.padding(start = 6.dp),
                            text = "私信"
                        )
                    }
                }
                if (viewModel.liveRoom?.roomStatus == 1) {
                    FilledTonalButton(onClick = onLiveClick) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Text(
                            modifier = Modifier.padding(start = 6.dp),
                            text = if (viewModel.liveRoom?.liveStatus == 1) "直播中" else "直播间"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserSpaceStat(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.formatCount(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UserSpaceExtraInfo(
    uid: String,
    tags: List<AppUserSpaceTag>,
    onCopyUid: () -> Unit,
    onOpenTag: (AppUserSpaceTag) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = onCopyUid,
            label = { Text(text = "UID: $uid") }
        )
        tags.forEach { tag ->
            AssistChip(
                onClick = { onOpenTag(tag) },
                label = { Text(text = tag.title.orEmpty()) }
            )
        }
    }
}

@Composable
private fun UserSpaceChargeRow(
    modifier: Modifier = Modifier,
    users: List<AppUserSpaceElecUser>,
    count: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = Color.Black.copy(alpha = 0.44f),
        contentColor = Color.White,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            users.take(3).forEachIndexed { index, user ->
                AsyncImage(
                    modifier = Modifier
                        .padding(start = if (index == 0) 0.dp else 0.dp)
                        .size(20.dp)
                        .clip(CircleShape),
                    model = user.avatar,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }
            Text(
                modifier = Modifier.padding(start = if (users.isEmpty()) 0.dp else 6.dp),
                text = "${count.formatCount()}人为TA充电",
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

private fun LazyListScope.userSpaceHomeContent(
    viewModel: UserSpaceViewModel,
    onSelectTab: (UserSpaceTab) -> Unit,
    onClickDynamic: (DynamicItem) -> Unit,
    onClickVideo: (SpaceVideo) -> Unit
) {
    item {
        UserSpaceSectionHeader(
            title = "近期动态",
            action = "查看全部",
            onAction = { onSelectTab(UserSpaceTab.Dynamic) }
        )
    }
    if (viewModel.dynamicItems.isEmpty() && viewModel.dynamicLoading) {
        item { UserSpaceLoading() }
    } else if (viewModel.dynamicItems.isEmpty()) {
        item { UserSpaceEmpty(text = "暂无动态") }
    } else {
        items(viewModel.dynamicItems.take(3)) { dynamicItem ->
            DynamicItemCard(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                dynamicItem = dynamicItem,
                onClick = onClickDynamic
            )
        }
    }

    item {
        UserSpaceSectionHeader(
            title = "投稿视频",
            action = "查看全部",
            onAction = { onSelectTab(UserSpaceTab.Video) }
        )
    }
    if (viewModel.spaceVideos.isEmpty() && viewModel.videoLoading) {
        item { UserSpaceLoading() }
    } else if (viewModel.spaceVideos.isEmpty()) {
        item { UserSpaceEmpty(text = "暂无投稿") }
    } else {
        items(viewModel.spaceVideos.take(6)) { video ->
            UpSpaceVideoItem(
                spaceVideo = video,
                onClick = onClickVideo
            )
        }
    }
}

private fun LazyListScope.userSpaceDynamicContent(
    viewModel: UserSpaceViewModel,
    onClickDynamic: (DynamicItem) -> Unit
) {
    if (viewModel.dynamicItems.isEmpty() && viewModel.dynamicLoading) {
        item { UserSpaceLoading() }
    } else if (viewModel.dynamicItems.isEmpty()) {
        item { UserSpaceEmpty(text = "暂无动态") }
    } else {
        items(viewModel.dynamicItems) { dynamicItem ->
            DynamicItemCard(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                dynamicItem = dynamicItem,
                onClick = onClickDynamic
            )
        }
        if (viewModel.dynamicLoading) item { UserSpaceLoading() }
        if (!viewModel.dynamicHasMore) item { UserSpaceEnd(text = "没有更多动态了") }
    }
}

private fun LazyListScope.userSpaceVideoContent(
    viewModel: UserSpaceViewModel,
    onClickVideo: (SpaceVideo) -> Unit
) {
    if (viewModel.spaceVideos.isEmpty() && viewModel.videoLoading) {
        item { UserSpaceLoading() }
    } else if (viewModel.spaceVideos.isEmpty()) {
        item { UserSpaceEmpty(text = "暂无投稿") }
    } else {
        items(viewModel.spaceVideos) { video ->
            UpSpaceVideoItem(
                spaceVideo = video,
                onClick = onClickVideo
            )
        }
        if (viewModel.videoLoading) item { UserSpaceLoading() }
        if (viewModel.noMore) item { UserSpaceEnd(text = "没有更多投稿了") }
    }
}

private fun LazyListScope.userSpaceFavoriteContent(
    viewModel: UserSpaceViewModel,
    onClickFavorite: (SpaceFavoriteItem) -> Unit,
    onLoadMoreGroup: (SpaceFavoriteData) -> Unit,
    onToggleGroup: (SpaceFavoriteData) -> Unit
) {
    when {
        viewModel.favoriteGroups.isEmpty() && viewModel.favoriteLoading -> item { UserSpaceLoading() }
        viewModel.favoriteGroups.isEmpty() && viewModel.favoriteError != null -> item {
            UserSpacePlaceholder(
                title = "收藏",
                message = viewModel.favoriteError.orEmpty()
            )
        }

        viewModel.favoriteGroups.isEmpty() -> item { UserSpaceEmpty(text = "暂无公开收藏夹") }
        else -> {
            viewModel.favoriteGroups.forEach { group ->
                item {
                    UserSpaceFavoriteGroupHeader(
                        group = group,
                        expanded = viewModel.isFavoriteGroupExpanded(group),
                        onToggle = { onToggleGroup(group) }
                    )
                }
                if (viewModel.isFavoriteGroupExpanded(group)) {
                    val list = group.mediaListResponse?.list.orEmpty()
                    if (list.isEmpty()) {
                        item { UserSpaceEmpty(text = "这一组暂时没有内容") }
                    } else {
                        items(
                            items = list,
                            key = { "${group.id}:${it.id}:${it.type}:${it.title}" }
                        ) { favorite ->
                            UserSpaceFavoriteItem(
                                item = favorite,
                                onClick = { onClickFavorite(favorite) }
                            )
                        }
                    }
                    item {
                        when {
                            viewModel.isFavoriteGroupLoading(group) -> UserSpaceLoading()
                            !viewModel.isFavoriteGroupEnd(group) -> Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                TextButton(onClick = { onLoadMoreGroup(group) }) {
                                    Text(text = "查看更多内容")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.userSpaceBangumiContent(
    viewModel: UserSpaceViewModel,
    onTypeChange: (FollowingSeasonType) -> Unit,
    onClickSeason: (SeasonCardData) -> Unit
) {
    item {
        UserSpaceBangumiTypeRow(
            selectedType = viewModel.followingSeasonType,
            onTypeChange = onTypeChange
        )
    }
    when {
        viewModel.followingSeasons.isEmpty() && viewModel.bangumiLoading -> item { UserSpaceLoading() }
        viewModel.followingSeasons.isEmpty() && viewModel.bangumiError != null -> item {
            UserSpacePlaceholder(
                title = viewModel.followingSeasonType.title(),
                message = viewModel.bangumiError.orEmpty()
            )
        }

        viewModel.followingSeasons.isEmpty() -> item {
            UserSpaceEmpty(text = "暂无公开${viewModel.followingSeasonType.title()}")
        }

        else -> {
            items(
                items = viewModel.followingSeasons.chunked(3),
                key = { row -> row.joinToString(":") { it.seasonId.toString() } }
            ) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { season ->
                        val data = SeasonCardData.fromFollowingSeason(season)
                        SeasonCard(
                            modifier = Modifier.weight(1f),
                            data = data,
                            onClick = { onClickSeason(data) }
                        )
                    }
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            if (viewModel.bangumiLoading) item { UserSpaceLoading() }
            if (!viewModel.bangumiHasMore) item {
                UserSpaceEnd(text = "没有更多${viewModel.followingSeasonType.title()}了")
            }
        }
    }
}

@Composable
private fun UserSpaceFavoriteGroupHeader(
    group: SpaceFavoriteData,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        TextButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            onClick = onToggle
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = buildString {
                    append(if (expanded) "收起 " else "展开 ")
                    append(group.name ?: "收藏夹")
                    append(" ")
                    append(group.mediaListResponse?.count ?: 0)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        HorizontalDivider()
    }
}

@Composable
private fun UserSpaceFavoriteItem(
    item: SpaceFavoriteItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(112.dp)
                    .height(70.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = item.cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
                val badge = when (item.type) {
                    11 -> "收藏夹"
                    21 -> "合集"
                    else -> null
                }
                if (badge != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                        color = Color.Black.copy(alpha = 0.58f),
                        contentColor = Color.White,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            text = badge,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .height(70.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.title.orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.favoriteSubtitle(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun UserSpaceBangumiTypeRow(
    selectedType: FollowingSeasonType,
    onTypeChange: (FollowingSeasonType) -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FollowingSeasonType.entries.forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeChange(type) },
                label = { Text(text = type.getDisplayName(context)) }
            )
        }
    }
}

@Composable
private fun UserSpaceSectionHeader(
    title: String,
    action: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        TextButton(onClick = onAction) {
            Text(text = action)
        }
    }
}

@Composable
private fun UserSpaceLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator()
    }
}

@Composable
private fun UserSpaceEmpty(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UserSpaceEnd(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UserSpacePlaceholder(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UserSpaceMoreMenu(
    viewModel: UserSpaceViewModel,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var showBlacklistDialog by remember { mutableStateOf(false) }
    var showRemoveFanDialog by remember { mutableStateOf(false) }
    val isSelf = viewModel.upMid == Prefs.uid
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "更多")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(text = "搜索TA的视频") },
                leadingIcon = {
                    Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    openUrl(context, userSpaceSearchUrl(viewModel.upMid))
                }
            )
            if (Prefs.isLogin && !isSelf) {
                DropdownMenuItem(
                    text = { Text(text = if (viewModel.isBlacklisted) "移出黑名单" else "加入黑名单") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Block, contentDescription = null)
                    },
                    onClick = {
                        expanded = false
                        showBlacklistDialog = true
                    }
                )
                if (viewModel.isFollowedByUp == 1) {
                    DropdownMenuItem(
                        text = { Text(text = "移除粉丝") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Rounded.PersonRemove, contentDescription = null)
                        },
                        onClick = {
                            expanded = false
                            showRemoveFanDialog = true
                        }
                    )
                }
            }
            DropdownMenuItem(
                text = { Text(text = if (isSelf) "分享我的主页" else "分享UP主") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    onShare()
                }
            )
            DropdownMenuItem(
                text = { Text(text = "复制UID") },
                leadingIcon = {
                    Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    copyText(context, "UID", viewModel.displayUid)
                    "已复制 UID".toast(context)
                }
            )
            DropdownMenuItem(
                text = { Text(text = "浏览器打开") },
                leadingIcon = {
                    Icon(imageVector = Icons.Rounded.OpenInBrowser, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    openUrl(context, userSpaceUrl(viewModel.upMid))
                }
            )
            DropdownMenuItem(
                text = { Text(text = "网页投稿") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    openUrl(context, userSpaceArchiveUrl(viewModel.upMid))
                }
            )
            if (viewModel.chargeTotal > 0 || viewModel.chargeUsers.isNotEmpty()) {
                DropdownMenuItem(
                    text = { Text(text = "充电排行榜") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Favorite, contentDescription = null)
                    },
                    onClick = {
                        expanded = false
                        openUrl(context, userSpaceChargeRankUrl(viewModel.upMid))
                    }
                )
            }
        }
    }

    if (showBlacklistDialog) {
        AlertDialog(
            onDismissRequest = { showBlacklistDialog = false },
            title = { Text(text = if (viewModel.isBlacklisted) "移出黑名单" else "加入黑名单") },
            text = {
                Text(
                    text = if (viewModel.isBlacklisted) {
                        "确定将 ${viewModel.upName} 移出黑名单？"
                    } else {
                        "确定拉黑 ${viewModel.upName}？被拉黑后可以在 B 站黑名单管理中解除。"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBlacklistDialog = false
                        viewModel.toggleBlacklist { blocked, success ->
                            (if (success) {
                                if (blocked) "已加入黑名单" else "已移出黑名单"
                            } else {
                                if (blocked) "加入黑名单失败" else "移出黑名单失败"
                            }).toast(context)
                        }
                    }
                ) {
                    Text(text = "确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlacklistDialog = false }) {
                    Text(text = "取消")
                }
            }
        )
    }

    if (showRemoveFanDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveFanDialog = false },
            title = { Text(text = "移除粉丝") },
            text = { Text(text = "确定将 ${viewModel.upName} 从你的粉丝中移除？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveFanDialog = false
                        viewModel.removeFan { success ->
                            (if (success) "移除成功" else "移除失败").toast(context)
                        }
                    }
                ) {
                    Text(text = "确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveFanDialog = false }) {
                    Text(text = "取消")
                }
            }
        )
    }
}

private fun openSpaceVideo(context: Context, video: SpaceVideo) {
    VideoPlayerActivity.actionStart(
        context = context,
        aid = video.aid
    )
}

private fun openDynamicItem(context: Context, dynamicItem: DynamicItem) {
    when (dynamicItem.type) {
        DynamicType.Av -> {
            VideoPlayerActivity.actionStart(
                context = context,
                aid = dynamicItem.video?.aid ?: return,
                fromSeason = dynamicItem.video?.seasonId != null && dynamicItem.video?.seasonId != 0,
                epid = dynamicItem.video?.epid,
                seasonId = dynamicItem.video?.seasonId
            )
        }

        DynamicType.Pgc -> {
            VideoPlayerActivity.actionStart(
                context = context,
                aid = 0,
                fromSeason = true,
                epid = dynamicItem.pgc?.epid ?: return,
                seasonId = dynamicItem.pgc?.seasonId
            )
        }

        else -> {
            val id = dynamicItem.id
            if (id == null) {
                "原动态不存在".toast(context)
            } else {
                DynamicDetailActivity.actionStart(context, id)
            }
        }
    }
}

private fun openFavoriteItem(context: Context, item: SpaceFavoriteItem) {
    if (item.state == 1) {
        "内容已失效".toast(context)
        return
    }
    val url = when (item.type) {
        21 -> "https://space.bilibili.com/${item.mid ?: item.upper?.mid ?: 0}/channel/collectiondetail?sid=${item.id ?: 0}"
        else -> "https://www.bilibili.com/medialist/detail/ml${item.id ?: item.mediaId ?: 0}"
    }
    openUrl(context, url)
}

private fun shareUserSpace(context: Context, mid: Long, username: String) {
    val text = "${username.ifBlank { "UP主" }} ${userSpaceUrl(mid)}"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "分享UP主"))
}

private fun openUrl(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

private fun copyText(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun userSpaceUrl(mid: Long): String = "https://space.bilibili.com/$mid"

private fun userSpaceArchiveUrl(mid: Long): String = "https://space.bilibili.com/$mid/video"

private fun userSpaceSearchUrl(mid: Long): String = "https://space.bilibili.com/$mid/search/video"

private fun userSpaceChargeRankUrl(mid: Long): String {
    return "https://www.bilibili.com/h5/upower/charge-rank-list?mid=$mid&navhide=1&source=1"
}

private fun Int.formatCount(): String {
    return when {
        this >= 100_000_000 -> String.format("%.1f亿", this / 100_000_000f)
        this >= 10_000 -> String.format("%.1f万", this / 10_000f)
        else -> toString()
    }
}

private fun SpaceFavoriteItem.favoriteSubtitle(): String {
    val count = mediaCount ?: count ?: 0
    return when (type) {
        21 -> buildString {
            append("创建者: ")
            append(upper?.name ?: "未知")
            append(" · ")
            append(count.formatCount())
            append("个视频")
            if ((viewCount ?: 0) > 0) {
                append(" · ")
                append(viewCount!!.formatCount())
                append("播放")
            }
        }

        11 -> "${count.formatCount()}个内容 · ${upper?.name ?: "收藏夹"}"
        else -> "${count.formatCount()}个内容 · ${favoriteVisibilityText()}"
    }
}

private fun SpaceFavoriteItem.favoriteVisibilityText(): String {
    isPublic?.let { return if (it == 1) "公开" else "私密" }
    return if (((attr ?: 0) and 1) == 0) "公开" else "私密"
}

private fun FollowingSeasonType.title(): String {
    return when (this) {
        FollowingSeasonType.Bangumi -> "追番"
        FollowingSeasonType.Cinema -> "追剧"
    }
}
