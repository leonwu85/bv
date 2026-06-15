package dev.aaa1115910.bv.tv.screens.user

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.biliapi.entity.user.DynamicItem
import dev.aaa1115910.biliapi.entity.user.DynamicType
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.tv.activities.dynamic.DynamicDetailActivity
import dev.aaa1115910.bv.tv.activities.video.SeasonInfoActivity
import dev.aaa1115910.bv.tv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.tv.activities.video.VideoPlayerV3Activity
import dev.aaa1115910.bv.tv.component.ContentStatusCard
import dev.aaa1115910.bv.tv.component.LoadingTip
import dev.aaa1115910.bv.tv.R
import dev.aaa1115910.bv.tv.component.TvDynamicImageUseCase
import dev.aaa1115910.bv.tv.component.TvSafeDynamicImage
import dev.aaa1115910.bv.tv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.tv.manager.FollowStateManager
import dev.aaa1115910.bv.tv.util.ProvideListBringIntoViewSpec
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.resizedImageUrl
import dev.aaa1115910.bv.util.scrollToItemIfAvailable
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.user.UserSpaceTab
import dev.aaa1115910.bv.viewmodel.user.UserSpaceViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin

private enum class TvUpSpaceTab(val title: String) {
    Dynamic("动态"),
    Video("投稿")
}

private val UpSpaceCardShape = RoundedCornerShape(8.dp)
private val UpSpaceAccent = Color(0xFFFB7299)

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun UpSpaceScreen(
    modifier: Modifier = Modifier,
    userSpaceViewModel: UserSpaceViewModel = koinViewModel(),
    userRepository: UserRepository = getKoin().get(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger { }
    var selectedTab by remember { mutableStateOf(TvUpSpaceTab.Dynamic) }
    var contentHasFocus by remember { mutableStateOf(false) }
    var isFollowing by remember { mutableStateOf(false) }
    var isLongPress by remember { mutableStateOf(false) }
    val dynamicFocusRequester = remember { FocusRequester() }
    val videoFocusRequester = remember { FocusRequester() }
    val tabFocusRequester = remember { FocusRequester() }
    val dynamicGridState = rememberLazyStaggeredGridState()
    val videoGridState = rememberLazyGridState()
    val headerCollapsed by remember {
        derivedStateOf {
            contentHasFocus && when (selectedTab) {
                TvUpSpaceTab.Dynamic -> dynamicGridState.isScrolledPastTop()
                TvUpSpaceTab.Video -> videoGridState.isScrolledPastTop()
            }
        }
    }

    val followStateMap by FollowStateManager.followStateMap.collectAsState()

    LaunchedEffect(Unit) {
        val intent = (context as Activity).intent
        if (intent.hasExtra("mid")) {
            val mid = intent.getLongExtra("mid", 0)
            val name = intent.getStringExtra("name").orEmpty()
            val face = intent.getStringExtra("face").orEmpty()
            userSpaceViewModel.initialize(mid = mid, name = name, face = face)
            userSpaceViewModel.selectTab(UserSpaceTab.Dynamic)
        } else {
            context.finish()
        }
    }

    LaunchedEffect(followStateMap, userSpaceViewModel.upMid) {
        FollowStateManager.getFollowState(userSpaceViewModel.upMid)?.let { following ->
            isFollowing = following
        }
    }

    LaunchedEffect(userSpaceViewModel.isFollowing) {
        userSpaceViewModel.isFollowing?.let { isFollowing = it }
    }

    val addFollow: (afterModify: (success: Boolean) -> Unit) -> Unit = { afterModify ->
        scope.launch(Dispatchers.IO) {
            val userMid = userSpaceViewModel.upMid
            logger.fInfo { "Add follow to user $userMid" }
            val success = userRepository.followUser(
                mid = userMid,
                preferApiType = Prefs.apiType
            )
            logger.fInfo { "Add follow result: $success" }
            if (success) {
                FollowStateManager.updateFollowState(userMid, true)
                withContext(Dispatchers.Main) {
                    userSpaceViewModel.isFollowing = true
                }
            }
            afterModify(success)
        }
    }

    val delFollow: (afterModify: (success: Boolean) -> Unit) -> Unit = { afterModify ->
        scope.launch(Dispatchers.IO) {
            val userMid = userSpaceViewModel.upMid
            logger.fInfo { "Del follow to user $userMid" }
            val success = userRepository.unfollowUser(
                mid = userMid,
                preferApiType = Prefs.apiType
            )
            logger.fInfo { "Del follow result: $success" }
            if (success) {
                FollowStateManager.updateFollowState(userMid, false)
                withContext(Dispatchers.Main) {
                    userSpaceViewModel.isFollowing = false
                }
            }
            afterModify(success)
        }
    }

    fun selectTab(tab: TvUpSpaceTab) {
        if (selectedTab == tab) return
        selectedTab = tab
        when (tab) {
            TvUpSpaceTab.Dynamic -> userSpaceViewModel.selectTab(UserSpaceTab.Dynamic)
            TvUpSpaceTab.Video -> userSpaceViewModel.selectTab(UserSpaceTab.Video)
        }
    }

    fun refreshSelectedTab() {
        contentHasFocus = false
        scope.launch {
            when (selectedTab) {
                TvUpSpaceTab.Dynamic -> dynamicGridState.scrollToItemIfAvailable(0)
                TvUpSpaceTab.Video -> videoGridState.scrollToItemIfAvailable(0)
            }
            userSpaceViewModel.refreshSelectedTab()
        }
    }

    Scaffold(
        modifier = modifier
            .onPreviewKeyEvent {
                val isDpadCenter = listOf(Key.Enter, Key.DirectionCenter).contains(it.key)
                if (isDpadCenter && it.type == KeyEventType.KeyDown) {
                    isLongPress = it.nativeKeyEvent.repeatCount > 0
                }
                false
            },
        topBar = {
            Column {
                UpSpaceHeader(
                    viewModel = userSpaceViewModel,
                    collapsed = headerCollapsed,
                    isFollowing = isFollowing,
                    onFollowClick = {
                        if (!Prefs.isLogin) {
                            "请先登录".toast(context)
                        } else if (isFollowing) {
                            delFollow { success ->
                                scope.launch(Dispatchers.Main) {
                                    (if (success) "已取消关注" else "取消关注失败").toast(context)
                                }
                            }
                        } else {
                            addFollow { success ->
                                scope.launch(Dispatchers.Main) {
                                    (if (success) "关注成功" else "关注失败").toast(context)
                                }
                            }
                        }
                    },
                    onLiveClick = {
                        val live = userSpaceViewModel.liveRoom
                        if (live != null && live.roomStatus == 1) {
                            VideoPlayerV3Activity.actionStartLive(
                                context = context,
                                roomId = live.roomId,
                                title = live.title,
                                upName = userSpaceViewModel.upName,
                                upFace = userSpaceViewModel.upFace,
                                upMid = userSpaceViewModel.upMid
                            )
                        } else {
                            "暂无直播".toast(context)
                        }
                    },
                    onMoreClick = { "更多功能暂未实现".toast(context) }
                )
                UpSpaceTabRow(
                    selectedTab = selectedTab,
                    headerCollapsed = headerCollapsed,
                    focusRequester = tabFocusRequester,
                    onSelectTab = ::selectTab,
                    onTabFocused = {
                        contentHasFocus = false
                    },
                    onRefreshSelectedTab = ::refreshSelectedTab
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                TvUpSpaceTab.Dynamic -> UpSpaceDynamicContent(
                    viewModel = userSpaceViewModel,
                    gridState = dynamicGridState,
                    firstItemFocusRequester = dynamicFocusRequester,
                    tabFocusRequester = tabFocusRequester,
                    onContentFocused = {
                        contentHasFocus = true
                    },
                    onReturnToTab = {
                        contentHasFocus = false
                    },
                    onClickDynamic = { dynamic ->
                        openDynamicItem(context, dynamic)
                    },
                    onLoadMore = {
                        userSpaceViewModel.loadMoreForSelectedTab()
                    }
                )

                TvUpSpaceTab.Video -> UpSpaceVideoContent(
                    viewModel = userSpaceViewModel,
                    gridState = videoGridState,
                    firstItemFocusRequester = videoFocusRequester,
                    tabFocusRequester = tabFocusRequester,
                    onContentFocused = {
                        contentHasFocus = true
                    },
                    onReturnToTab = {
                        contentHasFocus = false
                    },
                    isLongPress = isLongPress,
                    onOpenVideo = { aid, title ->
                        VideoInfoActivity.actionStart(
                            context = context,
                            aid = aid,
                            proxyArea = ProxyArea.checkProxyArea(title)
                        )
                    },
                    onLoadMore = {
                        userSpaceViewModel.loadMoreForSelectedTab()
                    }
                )
            }
        }
    }
}

private fun LazyStaggeredGridState.isScrolledPastTop(): Boolean {
    return firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 8
}

private fun LazyGridState.isScrolledPastTop(): Boolean {
    return firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 8
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun UpSpaceHeader(
    viewModel: UserSpaceViewModel,
    collapsed: Boolean,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onLiveClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val avatarSize by animateDpAsState(
        targetValue = if (collapsed) 38.dp else 58.dp,
        label = "up header avatar size"
    )
    val topPadding by animateDpAsState(
        targetValue = if (collapsed) 6.dp else 12.dp,
        label = "up header top padding"
    )
    val bottomPadding by animateDpAsState(
        targetValue = if (collapsed) 6.dp else 10.dp,
        label = "up header bottom padding"
    )
    val nameFontSize by animateFloatAsState(
        targetValue = if (collapsed) 22f else 30f,
        label = "up header name font size"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 24.dp, top = topPadding, end = 24.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(if (collapsed) 10.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            avatar = viewModel.upFace,
            modifier = Modifier.size(avatarSize)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (collapsed) 0.dp else 6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f, fill = false),
                    text = viewModel.upName.ifBlank { "UP 主" },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = nameFontSize.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!collapsed) {
                    HeaderBadges(viewModel = viewModel)
                }
            }

            if (!collapsed) {
                Text(
                    text = buildString {
                        append("关注 ${viewModel.friend.formatCount()}")
                        append(" · 粉丝 ${viewModel.fans.formatCount()}")
                        if (viewModel.sign.isNotBlank()) {
                            append("   |   ")
                            append(viewModel.sign.replace("\n", " "))
                        }
                    },
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                HeaderStats(viewModel = viewModel)
            }
        }

        if (!collapsed) {
            HeaderActions(
                viewModel = viewModel,
                isFollowing = isFollowing,
                onFollowClick = onFollowClick,
                onLiveClick = onLiveClick,
                onMoreClick = onMoreClick
            )
        }
    }
}

@Composable
private fun UserAvatar(
    avatar: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .border(3.dp, Color.White.copy(alpha = 0.92f), CircleShape)
    ) {
        if (avatar.isNotBlank()) {
            AsyncImage(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                model = avatar.resizedImageUrl(ImageSize.Icon),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun HeaderBadges(viewModel: UserSpaceViewModel) {
    val level = viewModel.userInfo?.level ?: viewModel.userCardInfo?.card?.levelInfo?.currentLevel ?: 0
    val vipText = viewModel.userInfo?.vip?.label?.text
        ?: viewModel.userCardInfo?.card?.vip?.label?.text

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderBadge(text = "LV$level")
        if (!vipText.isNullOrBlank()) HeaderBadge(text = vipText, color = UpSpaceAccent)
        HeaderBadge(text = "UID ${viewModel.displayUid}")
    }
}

@Composable
private fun HeaderBadge(
    text: String,
    color: Color = Color.White.copy(alpha = 0.16f)
) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(color)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HeaderStats(viewModel: UserSpaceViewModel) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HeaderStat(label = "关注", count = viewModel.friend)
        HeaderStat(label = "粉丝", count = viewModel.fans)
        HeaderStat(label = "获赞", count = viewModel.likeCount)
        HeaderStat(label = "投稿", count = viewModel.archiveCount.takeIf { it > 0 } ?: viewModel.tvSpaceVideos.size)
    }
}

@Composable
private fun HeaderStat(label: String, count: Int) {
    Column(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.formatCount(),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HeaderActions(
    viewModel: UserSpaceViewModel,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onLiveClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val isSelf = viewModel.upMid == Prefs.uid
    val followText = when {
        isSelf -> "自己"
        isFollowing -> "已关注"
        else -> "关注"
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderActionButton(
            modifier = Modifier.width(
                when {
                    isSelf -> 54.dp
                    followText == "已关注" -> 78.dp
                    else -> 66.dp
                }
            ),
            text = followText,
            icon = when {
                isSelf -> null
                isFollowing -> Icons.Rounded.Done
                else -> Icons.Rounded.Add
            },
            enabled = !isSelf,
            containerColor = if (isFollowing) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
            } else {
                UpSpaceAccent
            },
            onClick = onFollowClick
        )
        HeaderActionButton(
            modifier = Modifier.width(72.dp),
            text = if (viewModel.liveRoom?.liveStatus == 1) "直播中" else "直播间",
            containerColor = Color(0xFF35C98B),
            onClick = onLiveClick
        )
        HeaderActionButton(
            modifier = Modifier.width(58.dp),
            text = "更多",
            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
            onClick = onMoreClick
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HeaderActionButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(34.dp),
        enabled = enabled,
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = containerColor,
            focusedContainerColor = containerColor,
            pressedContainerColor = containerColor.copy(alpha = 0.78f),
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.82f)
                ),
                shape = MaterialTheme.shapes.small
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    modifier = Modifier.size(14.dp),
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun UpSpaceTabRow(
    selectedTab: TvUpSpaceTab,
    headerCollapsed: Boolean,
    focusRequester: FocusRequester,
    onSelectTab: (TvUpSpaceTab) -> Unit,
    onTabFocused: () -> Unit,
    onRefreshSelectedTab: () -> Unit
) {
    val tabs = TvUpSpaceTab.entries
    val topPadding by animateDpAsState(
        targetValue = if (headerCollapsed) 0.dp else 4.dp,
        label = "up tab top padding"
    )
    val bottomPadding by animateDpAsState(
        targetValue = if (headerCollapsed) 10.dp else 12.dp,
        label = "up tab bottom padding"
    )
    TabRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = topPadding, end = 24.dp, bottom = bottomPadding)
            .clip(UpSpaceCardShape)
            .focusRestorer(focusRequester),
        selectedTabIndex = tabs.indexOf(selectedTab)
    ) {
        tabs.forEach { tab ->
            Tab(
                modifier = if (tab == selectedTab) Modifier.focusRequester(focusRequester) else Modifier,
                selected = tab == selectedTab,
                onFocus = {
                    onTabFocused()
                    onSelectTab(tab)
                },
                onClick = {
                    if (tab == selectedTab) {
                        onRefreshSelectedTab()
                    } else {
                        onSelectTab(tab)
                    }
                }
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                    text = tab.title,
                    fontSize = 16.sp,
                    fontWeight = if (tab == selectedTab) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

private fun Modifier.focusUpToTab(
    tabFocusRequester: FocusRequester,
    onReturnToTab: () -> Unit
): Modifier {
    return onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
            onReturnToTab()
            runCatching { tabFocusRequester.requestFocus() }
            true
        } else {
            false
        }
    }
}

private fun Modifier.backToTab(
    tabFocusRequester: FocusRequester,
    onReturnToTab: () -> Unit
): Modifier {
    return onPreviewKeyEvent { event ->
        if (event.key == Key.Back) {
            if (event.type == KeyEventType.KeyDown) {
                onReturnToTab()
                runCatching { tabFocusRequester.requestFocus() }
            }
            true
        } else {
            false
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UpSpaceDynamicContent(
    viewModel: UserSpaceViewModel,
    gridState: LazyStaggeredGridState,
    firstItemFocusRequester: FocusRequester,
    tabFocusRequester: FocusRequester,
    onContentFocused: () -> Unit,
    onReturnToTab: () -> Unit,
    onClickDynamic: (DynamicItem) -> Unit,
    onLoadMore: () -> Unit
) {
    when {
        viewModel.dynamicItems.isEmpty() && viewModel.dynamicLoading -> LoadingBox()
        viewModel.dynamicItems.isEmpty() -> EmptyBox(text = "暂无动态")
        else -> ProvideListBringIntoViewSpec {
            LazyVerticalStaggeredGrid(
                modifier = Modifier
                    .fillMaxSize()
                    .backToTab(tabFocusRequester, onReturnToTab),
                columns = StaggeredGridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 28.dp),
                verticalItemSpacing = 16.dp,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(
                    items = viewModel.dynamicItems,
                    key = { index, item -> item.id ?: "dynamic-$index" }
                ) { index, item ->
                    val itemModifier = Modifier
                        .then(if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier)
                        .then(
                            if (index < 2) {
                                Modifier.focusUpToTab(tabFocusRequester, onReturnToTab)
                            } else {
                                Modifier
                            }
                        )
                    UpDynamicCard(
                        modifier = itemModifier,
                        dynamicItem = item,
                        onClick = { onClickDynamic(item) },
                        onFocus = {
                            onContentFocused()
                            if (index + 6 > viewModel.dynamicItems.size) onLoadMore()
                        }
                    )
                }
                if (viewModel.dynamicLoading) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        LoadingLine()
                    }
                }
                if (!viewModel.dynamicHasMore) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        EndLine(text = "没有更多动态了")
                    }
                }
            }
        }
    }
}

@Composable
private fun UpSpaceVideoContent(
    viewModel: UserSpaceViewModel,
    gridState: LazyGridState,
    firstItemFocusRequester: FocusRequester,
    tabFocusRequester: FocusRequester,
    onContentFocused: () -> Unit,
    onReturnToTab: () -> Unit,
    isLongPress: Boolean,
    onOpenVideo: (aid: Long, title: String) -> Unit,
    onLoadMore: () -> Unit
) {
    val padding = dimensionResource(R.dimen.grid_padding) / 2
    val spacedBy = dimensionResource(R.dimen.grid_spacedBy) / 2

    when {
        viewModel.tvSpaceVideos.isEmpty() && viewModel.videoLoading -> LoadingBox()
        viewModel.tvSpaceVideos.isEmpty() -> EmptyBox(text = "暂无投稿")
        else -> ProvideListBringIntoViewSpec {
            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxSize()
                    .backToTab(tabFocusRequester, onReturnToTab),
                columns = GridCells.Fixed(Prefs.gridColumns),
                state = gridState,
                contentPadding = PaddingValues(
                    start = padding,
                    top = 32.dp,
                    end = padding,
                    bottom = padding
                ),
                verticalArrangement = Arrangement.spacedBy(spacedBy),
                horizontalArrangement = Arrangement.spacedBy(spacedBy)
            ) {
                itemsIndexed(
                    items = viewModel.tvSpaceVideos,
                    key = { index, video -> video.bvid.ifBlank { "${video.avid}-$index" } }
                ) { index, video ->
                    val itemModifier = Modifier
                        .then(if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier)
                        .then(
                            if (index < Prefs.gridColumns) {
                                Modifier.focusUpToTab(tabFocusRequester, onReturnToTab)
                            } else {
                                Modifier
                            }
                        )
                    SmallVideoCard(
                        modifier = itemModifier,
                        data = video,
                        onClick = {
                            if (!isLongPress) onOpenVideo(video.avid, video.title)
                        },
                        onFocus = {
                            onContentFocused()
                            if (index + 12 > viewModel.tvSpaceVideos.size) onLoadMore()
                        }
                    )
                }
                if (viewModel.videoLoading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LoadingLine()
                    }
                }
                if (viewModel.noMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EndLine(text = "没有更多投稿了")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun UpDynamicCard(
    modifier: Modifier = Modifier,
    dynamicItem: DynamicItem,
    onClick: () -> Unit,
    onFocus: () -> Unit
) {
    var hasFocus by remember { mutableStateOf(false) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged {
                hasFocus = it.isFocused
                if (it.isFocused) onFocus()
            },
        onClick = onClick,
        shape = CardDefaults.shape(UpSpaceCardShape),
        colors = CardDefaults.colors(
            containerColor = if (hasFocus) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(3.dp, UpSpaceAccent),
                shape = UpSpaceCardShape
            )
        ),
        scale = CardDefaults.scale(focusedScale = 1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DynamicAuthorRow(dynamicItem.author)
            DynamicMainContent(dynamicItem)
            DynamicFooter(dynamicItem.footer)
        }
    }
}

@Composable
private fun DynamicAuthorRow(author: DynamicItem.DynamicAuthorModule) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            avatar = author.avatar,
            modifier = Modifier.size(42.dp)
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = author.author.ifBlank { "UP 主" },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOf(author.pubTime, author.pubAction).filter { it.isNotBlank() }.joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DynamicMainContent(dynamicItem: DynamicItem) {
    when (dynamicItem.type) {
        DynamicType.Av -> {
            val video = dynamicItem.video
            if (video != null) {
                DynamicText(text = video.text)
                DynamicVideoPreview(
                    cover = video.cover,
                    title = video.title,
                    play = video.play,
                    danmaku = video.danmaku,
                    duration = video.duration
                )
            }
        }

        DynamicType.UgcSeason -> {
            val season = dynamicItem.ugcSeason
            if (season != null) {
                DynamicText(text = season.desc)
                DynamicVideoPreview(
                    cover = season.cover,
                    title = season.title,
                    play = season.play,
                    danmaku = season.danmaku,
                    duration = season.duration
                )
            }
        }

        DynamicType.Draw -> {
            val draw = dynamicItem.draw
            if (draw != null) {
                DynamicText(text = draw.title?.takeIf { it.isNotBlank() } ?: draw.text)
                DynamicImageStrip(images = draw.images)
            }
        }

        DynamicType.Word -> DynamicText(text = dynamicItem.word?.text.orEmpty())
        DynamicType.Article -> {
            val article = dynamicItem.article
            if (article != null) {
                DynamicText(text = article.title.ifBlank { article.text })
                val images = article.coverPictures.takeIf { it.isNotEmpty() }
                    ?: article.covers.map { Picture(url = it, width = 0, height = 0, key = it) }
                DynamicImageStrip(images = images)
            }
        }

        DynamicType.Pgc -> {
            val pgc = dynamicItem.pgc
            if (pgc != null) {
                DynamicVideoPreview(
                    cover = pgc.cover,
                    title = pgc.title,
                    play = "",
                    danmaku = "",
                    duration = ""
                )
            }
        }

        DynamicType.Forward -> {
            DynamicText(text = dynamicItem.word?.text.orEmpty())
            dynamicItem.orig?.let { orig ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(UpSpaceCardShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = orig.dynamicSummary(),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        else -> DynamicText(text = dynamicItem.dynamicSummary())
    }
}

@Composable
private fun DynamicText(text: String) {
    if (text.isBlank()) return
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun DynamicVideoPreview(
    cover: String,
    title: String,
    play: String,
    danmaku: String,
    duration: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 7f)
                .clip(UpSpaceCardShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.BottomStart
        ) {
            if (cover.isNotBlank()) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = cover.resizedImageUrl(ImageSize.DynamicPreview),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.52f))
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (play.isNotBlank()) {
                    Text(text = play, color = Color.White, fontSize = 13.sp)
                }
                if (danmaku.isNotBlank()) {
                    Text(text = "弹幕 $danmaku", color = Color.White, fontSize = 13.sp)
                }
            }
            if (duration.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(Color.Black.copy(alpha = 0.62f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = duration, color = Color.White, fontSize = 12.sp)
                }
            }
        }
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DynamicImageStrip(images: List<Picture>) {
    if (images.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 150.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        images.take(3).forEach { image ->
            val isLongImage = image.width > 0 && image.height > image.width * 3
            TvSafeDynamicImage(
                url = image.url,
                sourceWidth = image.width,
                sourceHeight = image.height,
                modifier = Modifier
                    .weight(1f)
                    .height(140.dp),
                useCase = TvDynamicImageUseCase.ListPreview,
                imageSize = ImageSize.DynamicPreview,
                contentScale = ContentScale.Crop,
                alignment = if (isLongImage) Alignment.TopCenter else Alignment.Center,
                shape = UpSpaceCardShape,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun DynamicFooter(footer: DynamicItem.DynamicFooterModule?) {
    if (footer == null) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        DynamicFooterText(text = footer.like.formatCount())
        DynamicFooterText(text = "评论 ${footer.comment.formatCount()}")
        DynamicFooterText(text = "转发 ${footer.share.formatCount()}")
    }
}

@Composable
private fun DynamicFooterText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f),
        fontSize = 12.sp
    )
}

@Composable
private fun LoadingBox() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LoadingTip()
    }
}

@Composable
private fun EmptyBox(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ContentStatusCard(text = text)
    }
}

@Composable
private fun LoadingLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        contentAlignment = Alignment.Center
    ) {
        LoadingTip()
    }
}

@Composable
private fun EndLine(text: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        text = text,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f),
        textAlign = TextAlign.Center
    )
}

private fun openDynamicItem(context: android.content.Context, dynamic: DynamicItem) {
    when (dynamic.type) {
        DynamicType.Av -> dynamic.video?.let {
            VideoInfoActivity.actionStart(
                context = context,
                aid = it.aid,
                proxyArea = ProxyArea.checkProxyArea(it.title)
            )
        }

        DynamicType.UgcSeason -> dynamic.ugcSeason?.let {
            VideoInfoActivity.actionStart(
                context = context,
                aid = it.aid,
                proxyArea = ProxyArea.checkProxyArea(it.title)
            )
        }

        DynamicType.Pgc -> dynamic.pgc?.let {
            SeasonInfoActivity.actionStart(
                context = context,
                epId = it.epid,
                seasonId = it.seasonId
            )
        }

        else -> dynamic.id?.let { DynamicDetailActivity.actionStart(context, it) }
    }
}

private fun DynamicItem.dynamicSummary(): String {
    return when (type) {
        DynamicType.Av -> video?.title.orEmpty()
        DynamicType.UgcSeason -> ugcSeason?.title.orEmpty()
        DynamicType.Draw -> draw?.title?.takeIf { it.isNotBlank() } ?: draw?.text.orEmpty()
        DynamicType.Word -> word?.text.orEmpty()
        DynamicType.Article -> article?.title?.takeIf { it.isNotBlank() } ?: article?.text.orEmpty()
        DynamicType.Pgc -> pgc?.title.orEmpty()
        DynamicType.Forward -> word?.text.orEmpty()
        else -> none?.text ?: blocked?.hintMessage ?: "暂不支持的动态类型"
    }
}

private fun Int.formatCount(): String = when {
    this >= 100_000_000 -> String.format("%.1f亿", this / 100_000_000.0)
    this >= 10_000 -> String.format("%.1f万", this / 10_000.0)
    else -> toString()
}

//https://i2.hdslb.com/bfs/face/ea9b2fd60b04b123d0b48477838f60532b6271cd.jpg
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun UpFacePreview() {
    BVTheme {
        AsyncImage(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape),
            model = "https://i2.hdslb.com/bfs/face/ea9b2fd60b04b123d0b48477838f60532b6271cd.jpg@80h_80w_1c.webp",
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
    }
}
