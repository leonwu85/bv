package dev.aaa1115910.bv.mobile.screen.home

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Color.TRANSPARENT
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.OpenableColumns
import android.text.Editable
import android.text.InputType
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.util.Log
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.imageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.origeek.imageViewer.previewer.ImagePreviewerState
import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.biliapi.entity.user.DynamicEmoteDraft
import dev.aaa1115910.biliapi.entity.user.DynamicEmotePackageDraft
import dev.aaa1115910.biliapi.entity.user.DynamicItem
import dev.aaa1115910.biliapi.entity.user.DynamicMentionDraft
import dev.aaa1115910.biliapi.entity.user.DynamicPublishDraft
import dev.aaa1115910.biliapi.entity.user.DynamicReplyOption
import dev.aaa1115910.biliapi.entity.user.DynamicReserveDraft
import dev.aaa1115910.biliapi.entity.user.DynamicRichContent
import dev.aaa1115910.biliapi.entity.user.DynamicTopicDraft
import dev.aaa1115910.biliapi.entity.user.DynamicType
import dev.aaa1115910.biliapi.entity.user.DynamicUpUser
import dev.aaa1115910.biliapi.entity.user.DynamicVideo
import dev.aaa1115910.biliapi.entity.user.DynamicVoteDraft
import dev.aaa1115910.bv.entity.DynamicTabType
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.mobile.activities.DynamicDetailActivity
import dev.aaa1115910.bv.mobile.activities.UserSpaceActivity
import dev.aaa1115910.bv.mobile.activities.VideoPlayerActivity
import dev.aaa1115910.bv.mobile.component.home.dynamic.DynamicItem
import dev.aaa1115910.bv.mobile.component.user.UserAvatar
import dev.aaa1115910.bv.mobile.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.util.OnBottomReached
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.getLane
import dev.aaa1115910.bv.util.ifElse
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.home.DynamicViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3WindowSizeClassApi::class
)
@Composable
fun DynamicScreen(
    modifier: Modifier = Modifier,
    dynamicViewModel: DynamicViewModel = koinViewModel(),
    dynamicGridState: LazyStaggeredGridState,
    previewerState: ImagePreviewerState,
    onShowPreviewer: (newPictures: List<Picture>, afterSetPictures: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger("DynamicScreen")
    val windowSize = calculateWindowSizeClass(context as Activity).widthSizeClass
    val tabs = DynamicTabType.entries
    var selectedTab by rememberSaveable {
        androidx.compose.runtime.mutableStateOf(Prefs.dynamicDefaultTab)
    }
    var showCreateDynamicSheet by remember { mutableStateOf(false) }
    val selectedTabIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = selectedTabIndex,
        pageCount = { tabs.size }
    )
    val videoGridState = rememberLazyStaggeredGridState()
    val pgcGridState = rememberLazyStaggeredGridState()
    val articleGridState = rememberLazyStaggeredGridState()
    val upGridState = rememberLazyStaggeredGridState()

    val onClickDynamicItem: (DynamicItem) -> Unit = { dynamicItem ->
        logger.fInfo { "click dynamic type: ${dynamicItem.type}" }
        when (dynamicItem.type) {
            DynamicType.Av -> {
                VideoPlayerActivity.actionStart(
                    context = context,
                    aid = dynamicItem.video!!.aid,
                    fromSeason = dynamicItem.video!!.seasonId != null &&
                            dynamicItem.video!!.seasonId != 0,
                )
            }

            DynamicType.Pgc -> {
                VideoPlayerActivity.actionStart(
                    context = context,
                    aid = 0,
                    fromSeason = true,
                    epid = dynamicItem.pgc!!.epid,
                    seasonId = dynamicItem.pgc!!.seasonId,
                )
            }

            else -> {
                if (dynamicItem.id != null) {
                    DynamicDetailActivity.actionStart(context, dynamicItem.id!!)
                } else {
                    "原动态不存在".toast(context)
                }
            }
        }
    }

    LaunchedEffect(dynamicViewModel.isLogin) {
        if (dynamicViewModel.isLogin) {
            dynamicViewModel.loadFollowUpPanel()
            if (selectedTab == DynamicTabType.Up && dynamicViewModel.selectedUp == null) {
                dynamicViewModel.selectUp(dynamicViewModel.selfUp)
            }
            if (dynamicViewModel.itemCount(selectedTab) == 0) {
                dynamicViewModel.loadMoreByType(selectedTab)
            }
        }
    }

    LaunchedEffect(selectedTab) {
        if (!dynamicViewModel.isLogin) return@LaunchedEffect
        if (selectedTab == DynamicTabType.Up && dynamicViewModel.selectedUp == null) {
            dynamicViewModel.selectUp(dynamicViewModel.selfUp)
        }
        if (pagerState.currentPage != selectedTabIndex) {
            pagerState.animateScrollToPage(selectedTabIndex)
        }
        if (dynamicViewModel.itemCount(selectedTab) == 0) {
            dynamicViewModel.loadMoreByType(selectedTab)
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        val target = tabs[pagerState.settledPage]
        if (target != selectedTab) selectedTab = target
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    PrimaryScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        divider = {},
                        containerColor = Color.Transparent,
                        edgePadding = 0.dp,
                        minTabWidth = 0.dp
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            Tab(
                                selected = index == selectedTabIndex,
                                onClick = {
                                    selectedTab = tab
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                }
                            ) {
                                Box(
                                    modifier = Modifier.height(46.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        text = tab.getDisplayName(context),
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        modifier = Modifier.padding(end = 8.dp),
                        onClick = {
                            if (dynamicViewModel.isLogin) {
                                showCreateDynamicSheet = true
                            } else {
                                "请先登录后发布动态".toast(context)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "发布动态"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            if (!dynamicViewModel.isLogin) {
                DynamicCenteredMessage(text = "请先登录后查看动态")
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    DynamicUpPanel(
                        dynamicViewModel = dynamicViewModel,
                        selectedTab = selectedTab,
                        onSelectAll = {
                            selectedTab = DynamicTabType.All
                        },
                        onSelectUp = { up ->
                            Log.i("DynamicScreen", "select up from side panel: mid=${up.mid}, name=${up.uname}")
                            logger.fInfo { "select dynamic up: mid=${up.mid}, name=${up.uname}" }
                            dynamicViewModel.selectUp(up)
                            selectedTab = DynamicTabType.Up
                            scope.launch {
                                pagerState.animateScrollToPage(tabs.indexOf(DynamicTabType.Up))
                            }
                            scope.launch(Dispatchers.IO) {
                                dynamicViewModel.loadMoreUp()
                            }
                        },
                        onOpenUp = { up ->
                            Log.i("DynamicScreen", "open up from side panel: mid=${up.mid}, name=${up.uname}")
                            openDynamicUpSpace(context, up)
                        },
                        onLoadMore = {
                            scope.launch(Dispatchers.IO) {
                                dynamicViewModel.loadFollowUpPanel()
                            }
                        }
                    )
                    HorizontalPager(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        state = pagerState
                    ) { page ->
                        val type = tabs[page]
                        val state = when (type) {
                            DynamicTabType.All -> dynamicGridState
                            DynamicTabType.Video -> videoGridState
                            DynamicTabType.Pgc -> pgcGridState
                            DynamicTabType.Article -> articleGridState
                            DynamicTabType.Up -> upGridState
                        }
                        DynamicTabContent(
                            modifier = Modifier.fillMaxSize(),
                            type = type,
                            state = state,
                            windowSize = windowSize,
                            dynamicViewModel = dynamicViewModel,
                            previewerState = previewerState,
                            onShowPreviewer = onShowPreviewer,
                            onClickDynamicItem = onClickDynamicItem,
                            onClickVideo = { video ->
                                VideoPlayerActivity.actionStart(
                                    context = context,
                                    aid = video.aid,
                                    fromSeason = video.seasonId != null && video.seasonId != 0,
                                    epid = video.epid,
                                    seasonId = video.seasonId
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDynamicSheet) {
        CreateDynamicSheet(
            dynamicViewModel = dynamicViewModel,
            onDismiss = { showCreateDynamicSheet = false },
            onPublished = {
                showCreateDynamicSheet = false
                selectedTab = DynamicTabType.All
                scope.launch { pagerState.animateScrollToPage(tabs.indexOf(DynamicTabType.All)) }
            }
        )
    }
}

private fun openDynamicUpSpace(
    context: android.content.Context,
    up: DynamicUpUser
) {
    if (up.mid > 0L) {
        UserSpaceActivity.actionStart(
            context = context,
            mid = up.mid,
            name = up.uname.ifBlank { up.title }
        )
    } else {
        "UP 主不存在".toast(context)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateDynamicSheet(
    dynamicViewModel: DynamicViewModel,
    onDismiss: () -> Unit,
    onPublished: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var content by remember { mutableStateOf("") }
    var contentSelection by remember { mutableStateOf(DynamicTextSelection.Zero) }
    var title by remember { mutableStateOf("") }
    val richTokens = remember { mutableStateListOf<DynamicRichToken>() }
    val selectedImages = remember { mutableStateListOf<DynamicLocalImage>() }
    var activePanel by remember { mutableStateOf<DynamicCreatePanel?>(null) }
    var topic by remember { mutableStateOf<DynamicTopicDraft?>(null) }
    var topicKeyword by remember { mutableStateOf("") }
    val topicSuggestions = remember { mutableStateListOf<DynamicTopicDraft>() }
    var loadingTopics by remember { mutableStateOf(false) }
    var mentionKeyword by remember { mutableStateOf("") }
    val mentionSuggestions = remember { mutableStateListOf<DynamicMentionDraft>() }
    var loadingMentions by remember { mutableStateOf(false) }
    val emotePackages = remember { mutableStateListOf<DynamicEmotePackageDraft>() }
    var loadingEmotes by remember { mutableStateOf(false) }
    var publishTime by remember { mutableStateOf<Long?>(null) }
    var privatePub by remember { mutableStateOf(false) }
    var replyOption by remember { mutableStateOf(DynamicReplyOption.Allow) }
    var reserve by remember { mutableStateOf<DynamicReserveDraft?>(null) }
    var reserveTitle by remember { mutableStateOf("") }
    var reserveSubType by remember { mutableStateOf(0) }
    var reserveStartTime by remember { mutableStateOf(defaultReserveStartSeconds()) }
    var creatingReserve by remember { mutableStateOf(false) }
    var publishingDynamic by remember { mutableStateOf(false) }
    var enableVote by remember { mutableStateOf(false) }
    var voteTitle by remember { mutableStateOf("") }
    var voteDesc by remember { mutableStateOf("") }
    var choiceCnt by remember { mutableStateOf(1) }
    var durationDays by remember { mutableStateOf(1) }
    val voteOptions = remember { mutableStateListOf("", "") }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val remaining = 9 - selectedImages.size
        if (remaining <= 0) {
            "最多选择 9 张图片".toast(context)
            return@rememberLauncherForActivityResult
        }
        selectedImages.addAll(
            uris.take(remaining).map { uri ->
                DynamicLocalImage(
                    uri = uri,
                    fileName = context.dynamicUriFileName(uri)
                )
            }
        )
        if (uris.size > remaining) "已达到 9 张图片上限".toast(context)
    }

    fun appendRichToken(
        marker: String,
        rawText: String,
        type: Int,
        bizId: String = "",
        emoteUrl: String = "",
        emoteName: String = ""
    ) {
        val start = contentSelection.start.coerceIn(0, content.length)
        val end = contentSelection.end.coerceIn(0, content.length)
        val replaceStart = minOf(start, end)
        val replaceEnd = maxOf(start, end)
        content = content.replaceRange(replaceStart, replaceEnd, marker)
        contentSelection = DynamicTextSelection.collapsed(replaceStart + marker.length)
        richTokens.add(
            DynamicRichToken(
                marker = marker,
                rawText = rawText,
                type = type,
                bizId = bizId,
                preferredStart = replaceStart,
                emoteUrl = emoteUrl,
                emoteName = emoteName
            )
        )
    }

    LaunchedEffect(activePanel, topicKeyword) {
        if (activePanel != DynamicCreatePanel.Topic) return@LaunchedEffect
        if (topicKeyword.isNotBlank()) delay(250)
        loadingTopics = true
        val result = withContext(Dispatchers.IO) {
            dynamicViewModel.loadDynamicTopics(topicKeyword, content)
        }
        topicSuggestions.clear()
        topicSuggestions.addAll(result.getOrDefault(emptyList()))
        loadingTopics = false
    }

    LaunchedEffect(activePanel, mentionKeyword) {
        if (activePanel != DynamicCreatePanel.Mention) return@LaunchedEffect
        if (mentionKeyword.isNotBlank()) delay(250)
        loadingMentions = true
        val result = withContext(Dispatchers.IO) {
            dynamicViewModel.searchDynamicMention(mentionKeyword)
        }
        mentionSuggestions.clear()
        mentionSuggestions.addAll(result.getOrDefault(emptyList()))
        loadingMentions = false
    }

    LaunchedEffect(activePanel) {
        if (activePanel != DynamicCreatePanel.Emoji || emotePackages.isNotEmpty()) return@LaunchedEffect
        loadingEmotes = true
        val result = withContext(Dispatchers.IO) {
            dynamicViewModel.loadDynamicEmotePackages()
        }
        emotePackages.clear()
        emotePackages.addAll(result.getOrDefault(emptyList()))
        loadingEmotes = false
    }

    LaunchedEffect(privatePub) {
        if (privatePub) publishTime = null
    }

    val canPublish = (
            content.isNotBlank() ||
                    selectedImages.isNotEmpty() ||
                    reserve != null ||
                    enableVote
            ) && (!enableVote || (voteTitle.isNotBlank() && voteOptions.count { it.isNotBlank() } >= 2))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "发布动态",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    enabled = !dynamicViewModel.creatingDynamic,
                    onClick = onDismiss
                ) {
                    Text(text = "取消")
                }
                Button(
                    enabled = canPublish &&
                            !dynamicViewModel.creatingDynamic &&
                            !publishingDynamic &&
                            !creatingReserve,
                    onClick = {
                        val voteDraft = if (enableVote) {
                            DynamicVoteDraft(
                                title = voteTitle.trim(),
                                desc = voteDesc.trim(),
                                options = voteOptions.map(String::trim).filter(String::isNotEmpty),
                                choiceCnt = choiceCnt,
                                durationSeconds = durationDays * 24L * 60L * 60L
                            )
                        } else {
                            null
                        }
                        scope.launch(Dispatchers.IO) {
                            withContext(Dispatchers.Main) {
                                publishingDynamic = true
                            }
                            val result = runCatching {
                                val uploadedImages = selectedImages.map { image ->
                                    val bytes = context.dynamicUriBytes(image.uri)
                                        ?: error("无法读取图片：${image.fileName}")
                                    dynamicViewModel.uploadDynamicImage(
                                        fileName = image.fileName,
                                        bytes = bytes
                                    ).getOrThrow()
                                }
                                val richContents = buildDynamicRichContents(content, richTokens)
                                dynamicViewModel.publishDynamic(
                                    draft = DynamicPublishDraft(
                                        text = content,
                                        richContents = richContents,
                                        title = title,
                                        pictures = uploadedImages,
                                        publishTime = publishTime,
                                        replyOption = replyOption,
                                        privatePub = privatePub,
                                        topic = topic,
                                        voteDraft = voteDraft,
                                        reserve = reserve
                                    ),
                                    refreshType = DynamicTabType.All
                                ).getOrThrow()
                            }
                            withContext(Dispatchers.Main) {
                                result
                                    .onSuccess {
                                        "发布成功".toast(context)
                                        onPublished()
                                    }
                                    .onFailure {
                                        (it.localizedMessage ?: "发布失败").toast(context)
                                    }
                                publishingDynamic = false
                            }
                        }
                    }
                ) {
                    if (dynamicViewModel.creatingDynamic || publishingDynamic) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = if (publishTime == null) "发布" else "定时发布")
                    }
                }
            }

            topic?.let { selectedTopic ->
                InputChip(
                    selected = true,
                    onClick = { activePanel = DynamicCreatePanel.Topic },
                    label = { Text(text = "#${selectedTopic.name}") },
                    leadingIcon = {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.Default.Tag,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            modifier = Modifier.size(22.dp),
                            onClick = { topic = null }
                        ) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                imageVector = Icons.Default.Close,
                                contentDescription = "移除话题"
                            )
                        }
                    }
                )
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = title,
                onValueChange = { title = it.take(20) },
                label = { Text(text = "标题，选填 20 字") },
                singleLine = true
            )
            DynamicContentEditor(
                modifier = Modifier.fillMaxWidth(),
                value = content,
                selection = contentSelection,
                richTokens = richTokens.toList(),
                placeholder = if (enableVote) "我发起了一个投票" else "说点什么吧",
                onValueChange = { text, selection ->
                    content = text
                    contentSelection = selection
                }
            )

            if (selectedImages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(selectedImages) { image ->
                        Box(
                            modifier = Modifier
                                .size(82.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            AsyncImage(
                                modifier = Modifier.fillMaxSize(),
                                model = image.uri,
                                contentDescription = image.fileName,
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(28.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f),
                                        shape = CircleShape
                                    ),
                                onClick = { selectedImages.remove(image) }
                            ) {
                                Icon(
                                    modifier = Modifier.size(16.dp),
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "删除图片",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    DynamicToolChip(
                        label = "图片",
                        selected = selectedImages.isNotEmpty(),
                        icon = { Icon(Icons.Default.Image, contentDescription = null) },
                        onClick = { imagePicker.launch("image/*") }
                    )
                }
                item {
                    DynamicToolChip(
                        label = topic?.name ?: "话题",
                        selected = topic != null || activePanel == DynamicCreatePanel.Topic,
                        icon = { Icon(Icons.Default.Tag, contentDescription = null) },
                        onClick = {
                            activePanel = activePanel.toggle(DynamicCreatePanel.Topic)
                        }
                    )
                }
                item {
                    DynamicToolChip(
                        label = "@",
                        selected = activePanel == DynamicCreatePanel.Mention,
                        icon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                        onClick = {
                            activePanel = activePanel.toggle(DynamicCreatePanel.Mention)
                        }
                    )
                }
                item {
                    DynamicToolChip(
                        label = "表情",
                        selected = activePanel == DynamicCreatePanel.Emoji,
                        icon = { Icon(Icons.Default.EmojiEmotions, contentDescription = null) },
                        onClick = {
                            activePanel = activePanel.toggle(DynamicCreatePanel.Emoji)
                        }
                    )
                }
                item {
                    DynamicToolChip(
                        label = "投票",
                        selected = enableVote || activePanel == DynamicCreatePanel.Vote,
                        icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                        onClick = {
                            activePanel = activePanel.toggle(DynamicCreatePanel.Vote)
                            enableVote = true
                        }
                    )
                }
                item {
                    DynamicToolChip(
                        label = "直播预约",
                        selected = reserve != null || activePanel == DynamicCreatePanel.Reserve,
                        icon = { Icon(Icons.Default.LiveTv, contentDescription = null) },
                        onClick = {
                            activePanel = activePanel.toggle(DynamicCreatePanel.Reserve)
                        }
                    )
                }
                item {
                    DynamicToolChip(
                        label = publishTime?.let(::formatDynamicSeconds) ?: "定时",
                        selected = publishTime != null || activePanel == DynamicCreatePanel.Schedule,
                        icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                        onClick = {
                            activePanel = activePanel.toggle(DynamicCreatePanel.Schedule)
                        }
                    )
                }
            }

            when (activePanel) {
                DynamicCreatePanel.Topic -> DynamicTopicPanel(
                    keyword = topicKeyword,
                    onKeywordChange = { topicKeyword = it },
                    topics = topicSuggestions,
                    loading = loadingTopics,
                    onSelect = {
                        topic = it
                        activePanel = null
                    }
                )

                DynamicCreatePanel.Mention -> DynamicMentionPanel(
                    keyword = mentionKeyword,
                    onKeywordChange = { mentionKeyword = it },
                    mentions = mentionSuggestions,
                    loading = loadingMentions,
                    onSelect = {
                        appendRichToken(
                            marker = "@${it.name} ",
                            rawText = it.name,
                            type = 2,
                            bizId = it.uid
                        )
                    }
                )

                DynamicCreatePanel.Emoji -> DynamicEmojiPanel(
                    packages = emotePackages,
                    loading = loadingEmotes,
                    onSelect = { emoji ->
                        appendRichToken(
                            marker = emoji.text,
                            rawText = emoji.text,
                            type = 9,
                            emoteUrl = emoji.url,
                            emoteName = emoji.displayName
                        )
                    }
                )

                DynamicCreatePanel.Vote -> DynamicVotePanel(
                    enableVote = enableVote,
                    onEnableVoteChange = { enableVote = it },
                    voteTitle = voteTitle,
                    onVoteTitleChange = { voteTitle = it },
                    voteDesc = voteDesc,
                    onVoteDescChange = { voteDesc = it },
                    voteOptions = voteOptions,
                    choiceCnt = choiceCnt,
                    onChoiceCntChange = { choiceCnt = it },
                    durationDays = durationDays,
                    onDurationDaysChange = { durationDays = it }
                )

                DynamicCreatePanel.Reserve -> DynamicReservePanel(
                    reserve = reserve,
                    reserveTitle = reserveTitle,
                    onReserveTitleChange = { reserveTitle = it },
                    reserveStartTime = reserveStartTime,
                    onReserveStartTimeChange = { reserveStartTime = it },
                    reserveSubType = reserveSubType,
                    onReserveSubTypeChange = { reserveSubType = it },
                    creatingReserve = creatingReserve,
                    onCreateReserve = {
                        scope.launch(Dispatchers.IO) {
                            withContext(Dispatchers.Main) {
                                creatingReserve = true
                            }
                            val currentReserve = reserve
                            val result = if (currentReserve == null) {
                                dynamicViewModel.createLiveReserve(
                                    title = reserveTitle,
                                    livePlanStartTime = reserveStartTime,
                                    subType = reserveSubType
                                )
                            } else {
                                dynamicViewModel.updateLiveReserve(
                                    currentReserve.copy(
                                        title = reserveTitle,
                                        livePlanStartTime = reserveStartTime,
                                        subType = reserveSubType
                                    )
                                )
                            }
                            withContext(Dispatchers.Main) {
                                result
                                    .onSuccess {
                                        reserve = it
                                        "已保存直播预约".toast(context)
                                    }
                                    .onFailure {
                                        (it.localizedMessage ?: "直播预约创建失败").toast(context)
                                    }
                                creatingReserve = false
                            }
                        }
                    },
                    onClearReserve = { reserve = null }
                )

                DynamicCreatePanel.Schedule -> DynamicSchedulePanel(
                    publishTime = publishTime,
                    privatePub = privatePub,
                    onSelectTime = {
                        showDynamicDateTimePicker(
                            context = context,
                            initialSeconds = publishTime,
                            minMinutes = 6,
                            maxDays = 7,
                            onSelected = { publishTime = it }
                        )
                    },
                    onClearTime = { publishTime = null }
                )

                null -> Unit
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DynamicVisibilityMenu(
                    privatePub = privatePub,
                    scheduleSelected = publishTime != null,
                    onPrivatePubChange = { privatePub = it }
                )
                DynamicReplyMenu(
                    replyOption = replyOption,
                    onReplyOptionChange = { replyOption = it }
                )
            }
        }
    }
}

private enum class DynamicCreatePanel {
    Topic,
    Mention,
    Emoji,
    Vote,
    Reserve,
    Schedule
}

private data class DynamicLocalImage(
    val uri: Uri,
    val fileName: String
)

private data class DynamicTextSelection(
    val start: Int,
    val end: Int
) {
    companion object {
        val Zero = DynamicTextSelection(0, 0)

        fun collapsed(offset: Int) = DynamicTextSelection(offset, offset)
    }
}

private data class DynamicRichToken(
    val marker: String,
    val rawText: String,
    val type: Int,
    val bizId: String = "",
    val preferredStart: Int = -1,
    val emoteUrl: String = "",
    val emoteName: String = ""
)

private data class DynamicRichTokenRange(
    val token: DynamicRichToken,
    val start: Int,
    val end: Int
)

private fun DynamicCreatePanel?.toggle(panel: DynamicCreatePanel): DynamicCreatePanel? =
    if (this == panel) null else panel

@Composable
private fun DynamicToolChip(
    label: String,
    selected: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = icon,
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

@Composable
private fun DynamicContentEditor(
    modifier: Modifier = Modifier,
    value: String,
    selection: DynamicTextSelection,
    richTokens: List<DynamicRichToken>,
    placeholder: String,
    onValueChange: (String, DynamicTextSelection) -> Unit
) {
    val context = LocalContext.current
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val applyingExternalChange = remember { mutableStateOf(false) }
    val emoteDrawables = remember { mutableStateMapOf<String, Drawable>() }
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f).toArgb()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val emoteUrls = richTokens
        .mapNotNull { it.emoteUrl.takeIf(String::isNotBlank) }
        .distinct()

    LaunchedEffect(emoteUrls) {
        emoteUrls.forEach { url ->
            if (emoteDrawables[url] != null) return@forEach
            val result = withContext(Dispatchers.IO) {
                context.imageLoader.execute(
                    ImageRequest.Builder(context)
                        .data(url)
                        .allowHardware(false)
                        .size(96, 96)
                        .build()
                )
            }
            val drawable = (result as? SuccessResult)?.drawable ?: return@forEach
            emoteDrawables[url] = drawable
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = "动态内容",
                color = labelColor,
                style = MaterialTheme.typography.bodySmall
            )
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 112.dp, max = 220.dp)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                factory = { viewContext ->
                    DynamicSelectionEditText(viewContext).apply {
                        setBackgroundColor(TRANSPARENT)
                        setTextColor(textColor)
                        setHintTextColor(hintColor)
                        hint = placeholder
                        gravity = Gravity.TOP or Gravity.START
                        minLines = 4
                        maxLines = 8
                        setSingleLine(false)
                        setTextSize(16f)
                        inputType = InputType.TYPE_CLASS_TEXT or
                                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        imeOptions = EditorInfo.IME_ACTION_NONE
                        includeFontPadding = true
                        setPadding(0, 0, 0, 0)

                        val editText = this
                        addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(
                                s: CharSequence?,
                                start: Int,
                                count: Int,
                                after: Int
                            ) = Unit

                            override fun onTextChanged(
                                s: CharSequence?,
                                start: Int,
                                before: Int,
                                count: Int
                            ) = Unit

                            override fun afterTextChanged(s: Editable?) {
                                if (applyingExternalChange.value) return
                                currentOnValueChange(
                                    s?.toString().orEmpty(),
                                    DynamicTextSelection(
                                        start = editText.selectionStart.coerceAtLeast(0),
                                        end = editText.selectionEnd.coerceAtLeast(0)
                                    )
                                )
                            }
                        })
                        onSelectionChangedListener = { start, end ->
                            if (!applyingExternalChange.value) {
                                currentOnValueChange(
                                    editText.text?.toString().orEmpty(),
                                    DynamicTextSelection(
                                        start = start.coerceAtLeast(0),
                                        end = end.coerceAtLeast(0)
                                    )
                                )
                            }
                        }
                    }
                },
                update = { editText ->
                    editText.setTextColor(textColor)
                    editText.setHintTextColor(hintColor)
                    editText.hint = placeholder

                    val spanKey = buildDynamicEditorSpanKey(value, richTokens, emoteDrawables.keys)
                    if (editText.text?.toString().orEmpty() != value || editText.tag != spanKey) {
                        applyingExternalChange.value = true
                        editText.setText(
                            buildDynamicEditorSpannable(
                                context = context,
                                text = value,
                                tokens = richTokens,
                                emoteDrawables = emoteDrawables
                            )
                        )
                        editText.tag = spanKey
                        applyingExternalChange.value = false
                    }

                    val textLength = editText.text?.length ?: 0
                    val start = selection.start.coerceIn(0, textLength)
                    val end = selection.end.coerceIn(0, textLength)
                    if (editText.selectionStart != start || editText.selectionEnd != end) {
                        applyingExternalChange.value = true
                        editText.setSelection(start, end)
                        applyingExternalChange.value = false
                    }
                }
            )
        }
    }
}

@Composable
private fun DynamicTopicPanel(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    topics: List<DynamicTopicDraft>,
    loading: Boolean,
    onSelect: (DynamicTopicDraft) -> Unit
) {
    DynamicCreatePanelSurface {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = keyword,
            onValueChange = onKeywordChange,
            label = { Text(text = "搜索话题") },
            placeholder = { Text(text = "输入关键词，或直接选择推荐话题") },
            singleLine = true
        )
        if (loading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            }
        }
        topics.take(8).forEach { item ->
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSelect(item) }
            ) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = Icons.Default.Tag,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    modifier = Modifier.weight(1f),
                    text = item.name,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DynamicMentionPanel(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    mentions: List<DynamicMentionDraft>,
    loading: Boolean,
    onSelect: (DynamicMentionDraft) -> Unit
) {
    DynamicCreatePanelSurface {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = keyword,
            onValueChange = onKeywordChange,
            label = { Text(text = "搜索 UP 主") },
            placeholder = { Text(text = "输入昵称后添加 @") },
            singleLine = true
        )
        if (loading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            }
        }
        mentions.take(8).forEach { item ->
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSelect(item) }
            ) {
                AsyncImage(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape),
                    model = item.face,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = "UID ${item.uid}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DynamicEmojiPanel(
    packages: List<DynamicEmotePackageDraft>,
    loading: Boolean,
    onSelect: (DynamicEmoteDraft) -> Unit
) {
    val context = LocalContext.current
    val fallbackEmotes = remember {
        listOf(
            "[doge]", "[妙啊]", "[星星眼]", "[吃瓜]", "[滑稽]", "[笑哭]", "[喜极而泣]", "[脱单doge]",
            "[打call]", "[支持]", "[抱拳]", "[OK]", "[点赞]", "[鼓掌]", "[热词系列_知识增加]",
            "[热词系列_好家伙]", "[热词系列_破防了]", "[热词系列_泪目]", "[热词系列_三连]"
        ).map { DynamicEmoteDraft(text = it) }
    }
    val fallbackPackages = remember {
        listOf(DynamicEmotePackageDraft(type = 4, emotes = fallbackEmotes))
    }
    val panelPackages = packages.ifEmpty { fallbackPackages }
    var selectedPackageIndex by remember { mutableStateOf(0) }
    var previewEmote by remember { mutableStateOf<DynamicEmoteDraft?>(null) }
    val safeSelectedIndex = selectedPackageIndex.coerceIn(0, panelPackages.lastIndex)
    val selectedPackage = panelPackages[safeSelectedIndex]
    val isTextEmote = selectedPackage.type == 4
    val smallImageEmote = !isTextEmote && selectedPackage.emotes.firstOrNull()?.size == 1
    val cellSize = when {
        isTextEmote -> 96.dp
        smallImageEmote -> 40.dp
        else -> 60.dp
    }

    LaunchedEffect(panelPackages.size) {
        if (selectedPackageIndex !in panelPackages.indices) selectedPackageIndex = 0
    }
    LaunchedEffect(previewEmote) {
        if (previewEmote != null) {
            delay(1600)
            previewEmote = null
        }
    }

    DynamicCreatePanelSurface {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(236.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "表情",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                }
                LazyVerticalGrid(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 8.dp),
                    columns = GridCells.Adaptive(cellSize),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(selectedPackage.emotes) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(width = cellSize, height = if (isTextEmote) 40.dp else cellSize)
                                .clip(RoundedCornerShape(6.dp))
                                .combinedClickable(
                                    onClick = { onSelect(emoji) },
                                    onLongClick = {
                                        previewEmote = emoji
                                    }
                                )
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isTextEmote || emoji.url.isBlank()) {
                                Text(
                                    text = emoji.text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                AsyncImage(
                                    modifier = Modifier.fillMaxSize(),
                                    model = emoji.url,
                                    contentDescription = emoji.displayName,
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        modifier = Modifier.size(40.dp),
                        onClick = { "表情包管理暂未实装".toast(context) }
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Default.Settings,
                            contentDescription = "表情包管理",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(panelPackages.size) { index ->
                            val pack = panelPackages[index]
                            Surface(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { selectedPackageIndex = index },
                                color = if (index == safeSelectedIndex) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    Color.Transparent
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (pack.url.isNotBlank()) {
                                        AsyncImage(
                                            modifier = Modifier.size(24.dp),
                                            model = pack.url,
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Icon(
                                            modifier = Modifier.size(22.dp),
                                            imageVector = Icons.Default.EmojiEmotions,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            previewEmote?.let { emoji ->
                DynamicEmotePreview(
                    modifier = Modifier.align(Alignment.TopCenter),
                    emote = emoji
                )
            }
        }
    }
}

@Composable
private fun DynamicEmotePreview(
    modifier: Modifier = Modifier,
    emote: DynamicEmoteDraft
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (emote.url.isNotBlank()) {
                AsyncImage(
                    modifier = Modifier.size(68.dp),
                    model = emote.url,
                    contentDescription = emote.displayName,
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = emote.text,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = emote.displayName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private val DynamicEmoteDraft.displayName: String
    get() = alias.ifBlank {
        text.removePrefix("[").removeSuffix("]").ifBlank { text }
    }

@Composable
private fun DynamicVotePanel(
    enableVote: Boolean,
    onEnableVoteChange: (Boolean) -> Unit,
    voteTitle: String,
    onVoteTitleChange: (String) -> Unit,
    voteDesc: String,
    onVoteDescChange: (String) -> Unit,
    voteOptions: MutableList<String>,
    choiceCnt: Int,
    onChoiceCntChange: (Int) -> Unit,
    durationDays: Int,
    onDurationDaysChange: (Int) -> Unit
) {
    DynamicCreatePanelSurface {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onEnableVoteChange(!enableVote) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = enableVote,
                onCheckedChange = onEnableVoteChange
            )
            Column {
                Text(text = "添加投票")
                Text(
                    text = "发布时会先创建投票，再插入动态富文本",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (enableVote) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = voteTitle,
                onValueChange = { onVoteTitleChange(it.take(32)) },
                label = { Text(text = "投票标题") },
                singleLine = true
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = voteDesc,
                onValueChange = { onVoteDescChange(it.take(100)) },
                label = { Text(text = "投票说明") },
                maxLines = 3
            )
            voteOptions.forEachIndexed { index, option ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = option,
                        onValueChange = { voteOptions[index] = it.take(20) },
                        label = { Text(text = "选项${index + 1}") },
                        singleLine = true
                    )
                    if (voteOptions.size > 2) {
                        IconButton(
                            onClick = {
                                voteOptions.removeAt(index)
                                if (choiceCnt > voteOptions.size) onChoiceCntChange(voteOptions.size)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除选项"
                            )
                        }
                    }
                }
            }
            if (voteOptions.size < 20) {
                TextButton(onClick = { voteOptions.add("") }) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                    Text(text = "添加选项")
                }
            }
            DynamicStepperRow(
                title = if (choiceCnt == 1) "单选" else "最多选${choiceCnt}项",
                canDecrease = choiceCnt > 1,
                canIncrease = choiceCnt < voteOptions.size,
                onDecrease = { onChoiceCntChange(choiceCnt - 1) },
                onIncrease = { onChoiceCntChange(choiceCnt + 1) }
            )
            DynamicStepperRow(
                title = "截止时间：${durationDays}天后",
                canDecrease = durationDays > 1,
                canIncrease = durationDays < 90,
                onDecrease = { onDurationDaysChange(durationDays - 1) },
                onIncrease = { onDurationDaysChange(durationDays + 1) }
            )
        }
    }
}

@Composable
private fun DynamicReservePanel(
    reserve: DynamicReserveDraft?,
    reserveTitle: String,
    onReserveTitleChange: (String) -> Unit,
    reserveStartTime: Long,
    onReserveStartTimeChange: (Long) -> Unit,
    reserveSubType: Int,
    onReserveSubTypeChange: (Int) -> Unit,
    creatingReserve: Boolean,
    onCreateReserve: () -> Unit,
    onClearReserve: () -> Unit
) {
    val context = LocalContext.current
    val createReserveAction = {
        if (reserveTitle.isBlank()) {
            "请填写直播预约标题".toast(context)
        } else {
            onCreateReserve()
        }
    }
    DynamicCreatePanelSurface {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = "直播预约",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            reserve?.let {
                TextButton(onClick = onClearReserve) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = Icons.Default.Close,
                        contentDescription = null
                    )
                    Text(text = "移除")
                }
            }
        }
        reserve?.let {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "直播预约: ${it.title}")
                    Text(
                        text = "${formatDynamicSeconds(it.livePlanStartTime)} 直播",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        LaunchedEffect(reserve?.id) {
            reserve?.let {
                onReserveTitleChange(it.title)
                onReserveStartTimeChange(it.livePlanStartTime)
                onReserveSubTypeChange(it.subType)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = reserveSubType == 0,
                onClick = { onReserveSubTypeChange(0) },
                label = { Text(text = "公开直播") }
            )
            FilterChip(
                selected = reserveSubType == 1,
                onClick = { onReserveSubTypeChange(1) },
                label = { Text(text = "大航海直播") }
            )
        }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = reserveTitle,
            onValueChange = { onReserveTitleChange(it.take(14)) },
            label = { Text(text = "预约标题，最多 14 字") },
            singleLine = true
        )
        FilledTonalButton(
            onClick = {
                showDynamicDateTimePicker(
                    context = context,
                    initialSeconds = reserveStartTime,
                    minMinutes = 5,
                    maxDays = 90,
                    onSelected = onReserveStartTimeChange
                )
            }
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = Icons.Default.Schedule,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = formatDynamicSeconds(reserveStartTime))
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = reserveTitle.isNotBlank() && !creatingReserve,
            onClick = createReserveAction
        ) {
            if (creatingReserve) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text(text = if (reserve == null) "添加预约" else "保存预约")
            }
        }
    }
}

@Composable
private fun DynamicSchedulePanel(
    publishTime: Long?,
    privatePub: Boolean,
    onSelectTime: () -> Unit,
    onClearTime: () -> Unit
) {
    DynamicCreatePanelSurface {
        Text(
            text = "定时发布",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "定时发布需选择 6 分钟后、7 天内的时间，且不能与仅自己可见同时使用。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                enabled = !privatePub,
                onClick = onSelectTime
            ) {
                Text(text = publishTime?.let(::formatDynamicSeconds) ?: "选择时间")
            }
            if (publishTime != null) {
                TextButton(onClick = onClearTime) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = Icons.Default.Close,
                        contentDescription = null
                    )
                    Text(text = "取消定时")
                }
            }
        }
    }
}

@Composable
private fun DynamicVisibilityMenu(
    privatePub: Boolean,
    scheduleSelected: Boolean,
    onPrivatePubChange: (Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Icon(
                modifier = Modifier.size(19.dp),
                imageVector = if (privatePub) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = null,
                tint = if (privatePub) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = if (privatePub) "仅自己可见" else "所有人可见")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(text = "所有人可见") },
                onClick = {
                    onPrivatePubChange(false)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text(text = if (scheduleSelected) "仅自己可见（定时不可用）" else "仅自己可见") },
                enabled = !scheduleSelected,
                onClick = {
                    onPrivatePubChange(true)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun DynamicReplyMenu(
    replyOption: DynamicReplyOption,
    onReplyOptionChange: (DynamicReplyOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Icon(
                modifier = Modifier.size(19.dp),
                imageVector = Icons.AutoMirrored.Filled.Comment,
                contentDescription = null,
                tint = if (replyOption == DynamicReplyOption.Close) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.secondary
                }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = replyOption.displayName)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DynamicReplyOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option.displayName) },
                    onClick = {
                        onReplyOptionChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun DynamicCreatePanelSurface(
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun DynamicStepperRow(
    title: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(
                enabled = canDecrease,
                onClick = onDecrease
            ) {
                Text(text = "-")
            }
            TextButton(
                enabled = canIncrease,
                onClick = onIncrease
            ) {
                Text(text = "+")
            }
        }
    }
}

private val DynamicReplyOption.displayName: String
    get() = when (this) {
        DynamicReplyOption.Allow -> "允许评论"
        DynamicReplyOption.Close -> "关闭评论"
        DynamicReplyOption.Choose -> "精选评论"
    }

private fun buildDynamicRichContents(
    text: String,
    tokens: List<DynamicRichToken>
): List<DynamicRichContent> {
    if (tokens.isEmpty()) return emptyList()
    val contents = mutableListOf<DynamicRichContent>()
    var cursor = 0
    resolveDynamicRichTokenRanges(text, tokens).forEach { range ->
        val token = range.token
        if (range.start > cursor) {
            contents.add(DynamicRichContent(text.substring(cursor, range.start), 1))
        }
        contents.add(
            DynamicRichContent(
                rawText = token.rawText,
                type = token.type,
                bizId = token.bizId
            )
        )
        if (token.marker.endsWith(" ")) {
            contents.add(DynamicRichContent(" ", 1))
        }
        cursor = range.end
    }
    if (cursor < text.length) {
        contents.add(DynamicRichContent(text.substring(cursor), 1))
    }
    return contents.filter { it.rawText.isNotEmpty() }
}

private fun buildDynamicEditorSpanKey(
    text: String,
    tokens: List<DynamicRichToken>,
    loadedEmoteUrls: Set<String>
): String = buildString {
    append(text)
    append('|')
    tokens.forEach {
        append(it.marker)
        append('@')
        append(it.preferredStart)
        append(':')
        append(it.emoteUrl)
        append(';')
    }
    append('|')
    loadedEmoteUrls.sorted().forEach {
        append(it)
        append(';')
    }
}

private fun buildDynamicEditorSpannable(
    context: Context,
    text: String,
    tokens: List<DynamicRichToken>,
    emoteDrawables: Map<String, Drawable>
): SpannableStringBuilder {
    val spannable = SpannableStringBuilder(text)
    if (text.isEmpty()) return spannable
    val imageSize = (context.resources.displayMetrics.density * 24).toInt()
    resolveDynamicRichTokenRanges(text, tokens)
        .filter { it.token.type == 9 && it.token.emoteUrl.isNotBlank() }
        .forEach { range ->
            val drawable = emoteDrawables[range.token.emoteUrl]?.freshDrawable() ?: return@forEach
            drawable.setBounds(0, 0, imageSize, imageSize)
            spannable.setSpan(
                ImageSpan(drawable, DynamicDrawableSpan.ALIGN_CENTER),
                range.start,
                range.end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    return spannable
}

private fun resolveDynamicRichTokenRanges(
    text: String,
    tokens: List<DynamicRichToken>
): List<DynamicRichTokenRange> {
    if (text.isEmpty() || tokens.isEmpty()) return emptyList()
    val occupied = BooleanArray(text.length)
    return tokens.mapNotNull { token ->
        if (token.marker.isEmpty()) return@mapNotNull null
        findDynamicTokenRange(text, token, occupied)?.also { range ->
            for (index in range.start until range.end) occupied[index] = true
        }
    }.sortedBy { it.start }
}

private fun findDynamicTokenRange(
    text: String,
    token: DynamicRichToken,
    occupied: BooleanArray
): DynamicRichTokenRange? {
    val preferredStart = token.preferredStart
    if (
        preferredStart >= 0 &&
        preferredStart + token.marker.length <= text.length &&
        text.regionMatches(preferredStart, token.marker, 0, token.marker.length) &&
        (preferredStart until preferredStart + token.marker.length).all { !occupied[it] }
    ) {
        return DynamicRichTokenRange(token, preferredStart, preferredStart + token.marker.length)
    }

    var searchStart = 0
    while (searchStart <= text.length - token.marker.length) {
        val start = text.indexOf(token.marker, searchStart)
        if (start < 0) return null
        val end = start + token.marker.length
        if ((start until end).all { !occupied[it] }) {
            return DynamicRichTokenRange(token, start, end)
        }
        searchStart = start + 1
    }
    return null
}

private fun Drawable.freshDrawable(): Drawable =
    constantState?.newDrawable()?.mutate() ?: mutate()

private class DynamicSelectionEditText(context: Context) : EditText(context) {
    var onSelectionChangedListener: ((Int, Int) -> Unit)? = null

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        onSelectionChangedListener?.invoke(selStart, selEnd)
    }
}

private fun Context.dynamicUriFileName(uri: Uri): String {
    return runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
    }.getOrNull()?.takeIf { it.isNotBlank() }
        ?: "dynamic_${System.currentTimeMillis()}.jpg"
}

private fun Context.dynamicUriBytes(uri: Uri): ByteArray? =
    contentResolver.openInputStream(uri)?.use { it.readBytes() }

private fun defaultReserveStartSeconds(): Long {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 20)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis / 1000
}

private fun formatDynamicSeconds(seconds: Long): String =
    SimpleDateFormat("M月d日 HH:mm", Locale.getDefault()).format(Date(seconds * 1000))

private fun showDynamicDateTimePicker(
    context: Context,
    initialSeconds: Long?,
    minMinutes: Int,
    maxDays: Int,
    onSelected: (Long) -> Unit
) {
    val now = Calendar.getInstance()
    val min = Calendar.getInstance().apply { add(Calendar.MINUTE, minMinutes) }
    val max = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, maxDays) }
    val initial = Calendar.getInstance().apply {
        timeInMillis = (initialSeconds ?: min.timeInMillis / 1000) * 1000
        if (before(min)) timeInMillis = min.timeInMillis
    }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    val selected = Calendar.getInstance().apply {
                        set(year, month, day, hour, minute, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    when {
                        selected.before(min) -> "时间至少需要 ${minMinutes} 分钟之后".toast(context)
                        selected.after(max) -> "时间不能超过 ${maxDays} 天".toast(context)
                        else -> onSelected(selected.timeInMillis / 1000)
                    }
                },
                initial.get(Calendar.HOUR_OF_DAY),
                initial.get(Calendar.MINUTE),
                true
            ).show()
        },
        initial.get(Calendar.YEAR),
        initial.get(Calendar.MONTH),
        initial.get(Calendar.DAY_OF_MONTH)
    ).apply {
        datePicker.minDate = min.timeInMillis
        datePicker.maxDate = max.timeInMillis
    }.show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DynamicTabContent(
    modifier: Modifier = Modifier,
    type: DynamicTabType,
    state: LazyStaggeredGridState,
    windowSize: WindowWidthSizeClass,
    dynamicViewModel: DynamicViewModel,
    previewerState: ImagePreviewerState,
    onShowPreviewer: (newPictures: List<Picture>, afterSetPictures: () -> Unit) -> Unit,
    onClickDynamicItem: (DynamicItem) -> Unit,
    onClickVideo: (DynamicVideo) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lane by remember { derivedStateOf { state.getLane() } }
    val isLoading = dynamicViewModel.isLoading(type)
    val hasMore = dynamicViewModel.hasMore(type)
    val pullRefreshState = rememberPullToRefreshState()

    state.OnBottomReached(loading = isLoading || !hasMore) {
        scope.launch(Dispatchers.IO) {
            dynamicViewModel.loadMoreByType(type)
        }
    }

    PullToRefreshBox(
        modifier = modifier,
        state = pullRefreshState,
        isRefreshing = isLoading && dynamicViewModel.itemCount(type) == 0,
        onRefresh = {
            scope.launch(Dispatchers.IO) {
                if (type == DynamicTabType.All) dynamicViewModel.refreshFollowUpPanel()
                dynamicViewModel.refreshByType(type)
                dynamicViewModel.loadMoreByType(type)
            }
        }
    ) {
        LazyVerticalStaggeredGrid(
            modifier = Modifier
                .fillMaxSize()
                .ifElse(
                    { windowSize != WindowWidthSizeClass.Compact },
                    Modifier.clip(MaterialTheme.shapes.medium)
                )
                .background(MaterialTheme.colorScheme.surface),
            columns = StaggeredGridCells.Adaptive(300.dp),
            state = state,
            verticalItemSpacing = 8.dp,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(
                start = if (lane == 1) 0.dp else 8.dp,
                top = if (lane == 1) 0.dp else 8.dp,
                end = if (lane == 1) 0.dp else 8.dp,
                bottom = 100.dp
            )
        ) {
            when (type) {
                DynamicTabType.All -> {
                    items(dynamicViewModel.dynamicAllList) { dynamicItem ->
                        DynamicCard(
                            lane = lane,
                            dynamicItem = dynamicItem,
                            previewerState = previewerState,
                            onShowPreviewer = onShowPreviewer,
                            onTempBlockAuthor = {
                                dynamicViewModel.tempBlockAuthor(it.mid)
                                "已临时屏蔽${it.author}(${it.mid})，重启后恢复".toast(context)
                            },
                            onClick = onClickDynamicItem
                        )
                    }
                }

                DynamicTabType.Video -> {
                    items(dynamicViewModel.dynamicVideoList) { video ->
                        SmallVideoCard(
                            modifier = Modifier.ifElse(
                                lane != 1,
                                Modifier.clip(MaterialTheme.shapes.medium)
                            ),
                            data = video.toVideoCardData(),
                            onClick = { onClickVideo(video) }
                        )
                    }
                }

                DynamicTabType.Pgc -> {
                    items(dynamicViewModel.dynamicPgcList) { dynamicItem ->
                        DynamicCard(
                            lane = lane,
                            dynamicItem = dynamicItem,
                            previewerState = previewerState,
                            onShowPreviewer = onShowPreviewer,
                            onTempBlockAuthor = {
                                dynamicViewModel.tempBlockAuthor(it.mid)
                                "已临时屏蔽${it.author}(${it.mid})，重启后恢复".toast(context)
                            },
                            onClick = onClickDynamicItem
                        )
                    }
                }

                DynamicTabType.Article -> {
                    items(dynamicViewModel.dynamicArticleList) { dynamicItem ->
                        DynamicCard(
                            lane = lane,
                            dynamicItem = dynamicItem,
                            previewerState = previewerState,
                            onShowPreviewer = onShowPreviewer,
                            onTempBlockAuthor = {
                                dynamicViewModel.tempBlockAuthor(it.mid)
                                "已临时屏蔽${it.author}(${it.mid})，重启后恢复".toast(context)
                            },
                            onClick = onClickDynamicItem
                        )
                    }
                }

                DynamicTabType.Up -> {
                    items(dynamicViewModel.dynamicUpList) { dynamicItem ->
                        DynamicCard(
                            lane = lane,
                            dynamicItem = dynamicItem,
                            previewerState = previewerState,
                            onShowPreviewer = onShowPreviewer,
                            onTempBlockAuthor = {
                                dynamicViewModel.tempBlockAuthor(it.mid)
                                "已临时屏蔽${it.author}(${it.mid})，重启后恢复".toast(context)
                            },
                            onClick = onClickDynamicItem
                        )
                    }
                }
            }

            if (dynamicViewModel.itemCount(type) == 0 && !isLoading) {
                item {
                    DynamicCenteredMessage(
                        modifier = Modifier
                            .height(300.dp)
                            .fillMaxSize(),
                        text = if (type == DynamicTabType.Up && dynamicViewModel.selectedUp == null) {
                            "选择一个 UP 主查看动态"
                        } else {
                            "啥都没有"
                        }
                    )
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .height(96.dp)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicCard(
    lane: Int?,
    dynamicItem: DynamicItem,
    previewerState: ImagePreviewerState,
    onShowPreviewer: (newPictures: List<Picture>, afterSetPictures: () -> Unit) -> Unit,
    onTempBlockAuthor: (DynamicItem.DynamicAuthorModule) -> Unit,
    onClick: (DynamicItem) -> Unit
) {
    DynamicItem(
        modifier = Modifier.ifElse(lane != 1, Modifier.clip(MaterialTheme.shapes.medium)),
        dynamicItem = dynamicItem,
        previewerState = previewerState,
        onShowPreviewer = onShowPreviewer,
        onTempBlockAuthor = onTempBlockAuthor,
        onClick = onClick
    )
}

@Composable
private fun DynamicUpPanel(
    dynamicViewModel: DynamicViewModel,
    selectedTab: DynamicTabType,
    onSelectAll: () -> Unit,
    onSelectUp: (DynamicUpUser) -> Unit,
    onOpenUp: (DynamicUpUser) -> Unit,
    onLoadMore: () -> Unit
) {
    if (!dynamicViewModel.isLogin) return
    val context = LocalContext.current
    var showLiveUp by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .width(64.dp)
            .fillMaxHeight(),
        contentPadding = PaddingValues(bottom = 200.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            DynamicLiveUpItem(
                liveCount = dynamicViewModel.liveUpCount,
                expanded = showLiveUp,
                onClick = {
                    showLiveUp = !showLiveUp
                }
            )
        }
        if (showLiveUp && dynamicViewModel.liveUpList.isNotEmpty()) {
            items(dynamicViewModel.liveUpList, key = { "live-${it.roomId}-${it.mid}" }) { up ->
                DynamicUpChip(
                    face = up.face,
                    name = up.uname.ifBlank { up.title.ifBlank { "Live" } },
                    selected = false,
                    live = true,
                    hasUpdate = false,
                    onClick = {
                        if (up.roomId > 0) {
                            VideoPlayerActivity.actionStartLive(
                                context = context,
                                roomId = up.roomId.toInt(),
                                title = up.title.ifBlank { up.uname.ifBlank { "直播间" } },
                                upName = up.uname,
                                upFace = up.face,
                                upMid = up.mid
                            )
                        } else {
                            "直播间不存在".toast(context)
                        }
                    },
                    onLongClick = { onOpenUp(up) }
                )
            }
        }
        item {
            DynamicUpChip(
                face = "",
                name = "全部动态",
                selected = selectedTab == DynamicTabType.All,
                isAll = true,
                hasUpdate = false,
                onClick = onSelectAll
            )
        }
        item {
            DynamicUpChip(
                face = dynamicViewModel.selfUp.face,
                name = "我",
                selected = selectedTab == DynamicTabType.Up &&
                        dynamicViewModel.selectedUp?.mid == dynamicViewModel.selfUp.mid,
                hasUpdate = false,
                onClick = { onSelectUp(dynamicViewModel.selfUp) },
                onLongClick = { onOpenUp(dynamicViewModel.selfUp) }
            )
        }
        items(dynamicViewModel.followUpList, key = { it.mid }) { up ->
            if (up == dynamicViewModel.followUpList.lastOrNull() && dynamicViewModel.followUpHasMore) {
                LaunchedEffect(up.mid) { onLoadMore() }
            }
            DynamicUpChip(
                face = up.face,
                name = up.uname,
                selected = selectedTab == DynamicTabType.Up && dynamicViewModel.selectedUp?.mid == up.mid,
                hasUpdate = up.hasUpdate,
                onClick = { onSelectUp(up) },
                onLongClick = { onOpenUp(up) }
            )
        }
        if (dynamicViewModel.loadingFollowUp) {
            item {
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(76.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
        } else if (dynamicViewModel.followUpList.isEmpty()) {
            item {
                IconButton(onClick = onLoadMore) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新关注 UP"
                    )
                }
            }
        }
    }
}

@Composable
private fun DynamicLiveUpItem(
    liveCount: Int,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .height(60.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Live($liveCount)",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Icon(
            modifier = Modifier.size(14.dp),
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DynamicUpChip(
    face: String,
    name: String,
    selected: Boolean,
    hasUpdate: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isAll: Boolean = false,
    live: Boolean = false
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .height(76.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box {
            if (isAll) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = Color(0xFF5CB67B)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "BV",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            } else {
                UserAvatar(
                    avatar = face,
                    size = 42.dp
                )
            }
            if (hasUpdate) {
                Badge(
                    modifier = Modifier.align(Alignment.TopEnd),
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
            if (live) {
                Badge(
                    modifier = Modifier.align(Alignment.TopEnd),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Text(text = "Live")
                }
            }
        }
        Text(
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
            text = name.ifBlank { "-" },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun DynamicCenteredMessage(
    modifier: Modifier = Modifier.fillMaxSize(),
    text: String
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun DynamicVideo.toVideoCardData(): VideoCardData {
    return VideoCardData(
        avid = aid,
        bvid = bvid.orEmpty(),
        title = title,
        cover = cover,
        upName = author,
        upId = authorId,
        upFace = authorFace,
        play = play,
        danmaku = danmaku,
        time = duration.toLong() * 1000L,
        jumpToSeason = seasonId != null && seasonId != 0,
        epId = epid,
        seasonId = seasonId,
        pubTime = pubTime,
        isChargingArc = isChargingArc,
        badgeText = chargingArcBadge
    )
}
