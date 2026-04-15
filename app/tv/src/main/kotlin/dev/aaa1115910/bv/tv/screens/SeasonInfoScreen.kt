package dev.aaa1115910.bv.tv.screens

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.video.season.Episode
import dev.aaa1115910.biliapi.entity.video.season.PgcSeason
import dev.aaa1115910.biliapi.entity.video.season.SeasonDetail
import dev.aaa1115910.biliapi.entity.video.season.Section
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.component.QrImage
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.player.entity.VideoListPgcEpisode
import dev.aaa1115910.bv.repository.VideoInfoRepository
import dev.aaa1115910.bv.tv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.tv.component.CommentPanel
import dev.aaa1115910.bv.tv.component.LoadingTip
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.tv.component.buttons.SeasonInfoButtons
import dev.aaa1115910.bv.tv.util.launchPlayerActivity
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.focusedScale
import dev.aaa1115910.bv.util.ifElse
import dev.aaa1115910.bv.util.onBackPressed
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.util.resizedImageUrl
import dev.aaa1115910.bv.util.swapList
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.SeasonViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlin.math.ceil

/**
 * 生成剧集标题
 * @param episode 剧集信息
 * @param sectionTitle 所属章节标题
 * @return 格式化后的剧集标题
 */
private fun generateEpisodeTitle(
    episode: Episode?,
    sectionTitle: String
): String {
    if(episode == null) return ""

    return if (episode.longTitle.isNotEmpty()) {
        runCatching {
            "第 ${episode.title.toInt()} 集 "
        }.getOrDefault("") + episode.longTitle
    } else if (sectionTitle == "正片") {
        //如果 title 是数字的话，就会返回 "第 x 集"
        //如果 title 不是数字的话（例如 SP），就会原样使用 title
        runCatching {
            "第 ${episode.title.toInt()} 集"
        }.getOrDefault(episode.title)
    } else {
        episode.title
    }
}

private fun formatEpisodeStatCount(count: Long): String {
    return when {
        count <= 0L -> ""
        count >= 10000L -> "${count / 10000}万"
        else -> "$count"
    }
}

private fun parseEpisodeBadgeColor(color: String?, fallback: Color): Color {
    if (color.isNullOrBlank()) return fallback
    return runCatching {
        Color(AndroidColor.parseColor(color))
    }.getOrDefault(fallback)
}

private const val SeasonVipRequiredStatus = 13
private const val SeasonPaidRequiredStatus = 8
private const val VipOpenUrl = "https://account.bilibili.com/big"

private enum class SeasonQrDialogType {
    OpenVip,
    Pay
}

private data class SeasonAccessState(
    val showOpenVipButton: Boolean = false,
    val showPayButton: Boolean = false,
    val payUrl: String = ""
)

private fun resolveSeasonAccessState(
    seasonData: SeasonDetail,
    lastPlayedEpId: Int?
): SeasonAccessState {
    val positiveEpisodes = seasonData.episodes
    val payEpisode = positiveEpisodes.firstOrNull {
        it.id == lastPlayedEpId && it.status == SeasonPaidRequiredStatus && it.shortLink.isNotBlank()
    } ?: positiveEpisodes.firstOrNull {
        it.status == SeasonPaidRequiredStatus && it.shortLink.isNotBlank()
    }

    return SeasonAccessState(
        showOpenVipButton = !seasonData.userStatus.isVip && positiveEpisodes.any {
            it.status == SeasonVipRequiredStatus
        },
        showPayButton = !seasonData.userStatus.pay && payEpisode != null,
        payUrl = payEpisode?.shortLink.orEmpty()
    )
}

@Composable
fun SeasonInfoScreen(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    videoInfoRepository: VideoInfoRepository = koinInject(),
    seasonViewModel: SeasonViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val intent = (context as Activity).intent
    val logger = KotlinLogging.logger { }

    var paused by remember { mutableStateOf(false) }
    var showSeasonSelector by remember { mutableStateOf(false) }
    var showCommentPanel by remember { mutableStateOf(false) }
    var showDescriptionDialog by remember { mutableStateOf(false) }
    var showAccessQrDialog by remember { mutableStateOf<SeasonQrDialogType?>(null) }
    val playButtonFocusRequester = remember { FocusRequester() }
    val commentButtonFocusRequester = remember { FocusRequester() }
    val vipButtonFocusRequester = remember { FocusRequester() }
    val payButtonFocusRequester = remember { FocusRequester() }

    val onClickVideo: (avid: Long, cid: Long, epid: Int, episodeTitle: String, startTime: Int) -> Unit =
        { avid, cid, epid, episodeTitle, startTime ->
            logger.debug { "onClickVideo: [avid=$avid, cid=$cid, epid=$epid, episodeTitle=$episodeTitle, startTime=$startTime]" }
            if (cid != 0L) {
                launchPlayerActivity(
                    context = context,
                    avid = avid,
                    cid = cid,
                    title = seasonViewModel.seasonData!!.title,
                    partTitle = episodeTitle,
                    played = startTime * 1000,
                    fromSeason = true,
                    subType = seasonViewModel.seasonData?.subType,
                    epid = epid,
                    seasonId = seasonViewModel.seasonData?.seasonId,
                    proxyArea = seasonViewModel.proxyArea,
                    playerIconIdle = seasonViewModel.seasonData?.playerIcon?.idle ?: "",
                    playerIconMoving = seasonViewModel.seasonData?.playerIcon?.moving ?: ""
                )
            } else {
                //如果 cid==0，就需要跳转回 VideoInfoActivity 去获取 cid 再跳转播放器
                VideoInfoActivity.actionStart(
                    context = context,
                    aid = avid,
                    fromSeason = true
                )
            }
        }

    val onClickFollow: (Boolean) -> Unit = {
        scope.launch(Dispatchers.IO) {
            if (seasonViewModel.isFollowing) seasonViewModel.unFollowSeason() else seasonViewModel.followSeason()
        }
    }

    val onClickCover = {
        if (seasonViewModel.seasonData?.seasons?.isNotEmpty() == true) showSeasonSelector = true
    }

    val onShowComment = {
        showCommentPanel = true
    }

    val getCommentAid = {
        val lastEpId = seasonViewModel.lastPlayProgress?.lastEpId
        if (lastEpId != null) {
            // 查找最后播放的剧集
            seasonViewModel.seasonData?.episodes?.find { it.id == lastEpId }?.aid
                ?: seasonViewModel.seasonData?.sections?.mapNotNull { section ->
                    section.episodes.find { it.id == lastEpId }?.aid
                }?.firstOrNull()
        } else {
            // 没有播放记录，使用第一集
            seasonViewModel.seasonData?.episodes?.firstOrNull()?.aid
        } ?: 0L
    }

    LaunchedEffect(Unit) {
        videoInfoRepository.relatedVideos.clear()

        val epId = intent.getIntExtra("epid", 0).takeIf { it > 0 }
        val seasonId = intent.getIntExtra("seasonid", 0).takeIf { it > 0 }
        val proxyAreaIndex = intent.getIntExtra("proxy_area", 0)
        val proxyArea = ProxyArea.entries[proxyAreaIndex]
        logger.fInfo { "Read extras from content: [epId=$epId, seasonId=$seasonId, proxyArea=$proxyArea]" }

        seasonViewModel.epId = epId
        seasonViewModel.seasonId = seasonId
        seasonViewModel.proxyArea = proxyArea

        if (seasonViewModel.epId != null || seasonViewModel.seasonId != null) {
            scope.launch(Dispatchers.IO) {
                seasonViewModel.updateSeasonData()
            }
        } else {
            context.finish()
        }
    }

    LaunchedEffect(seasonViewModel.seasonData) {
        seasonViewModel.seasonData?.let {
            seasonViewModel.lastPlayProgress = it.userStatus.progress
            //请求默认焦点到播放按钮上
            delay(300)
            playButtonFocusRequester.requestFocus(scope)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                paused = true
            } else if (event == Lifecycle.Event.ON_RESUME) {
                // 如果 pause==true 那可能是从播放页返回回来的，此时更新历史记录
                if (paused) {
                    scope.launch(Dispatchers.IO) {
                        seasonViewModel.updateLastPlayProgress()
                    }
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val seasonAccessState = seasonViewModel.seasonData?.let { seasonData ->
        remember(seasonData, seasonViewModel.lastPlayProgress?.lastEpId) {
            resolveSeasonAccessState(
                seasonData = seasonData,
                lastPlayedEpId = seasonViewModel.lastPlayProgress?.lastEpId
            )
        }
    }

    val hideAccessQrDialog = {
        val dialogType = showAccessQrDialog
        showAccessQrDialog = null
        when (dialogType) {
            SeasonQrDialogType.OpenVip -> vipButtonFocusRequester.requestFocus(scope)
            SeasonQrDialogType.Pay -> payButtonFocusRequester.requestFocus(scope)
            null -> Unit
        }
    }

    if (seasonViewModel.seasonData == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (seasonViewModel.tip == "Loading") {
                LoadingTip()
            } else {
                Text(
                    text = seasonViewModel.tip
                )
            }
        }
    } else {
        val seasonData = seasonViewModel.seasonData!!
        val accessState = seasonAccessState ?: SeasonAccessState()

        val heroBackgroundCover = remember(seasonData, seasonViewModel.lastPlayProgress) {
            seasonData.seasons.find { it.seasonId == seasonData.seasonId }?.horizontalCover
                ?: run {
                    val lastEpId = seasonViewModel.lastPlayProgress?.lastEpId
                    if (lastEpId != null) {
                        seasonData.episodes.find { it.id == lastEpId }?.cover
                            ?: seasonData.sections.flatMap { it.episodes }.find { it.id == lastEpId }?.cover
                    } else null
                }
                ?: seasonData.episodes.lastOrNull()?.cover
                ?: seasonData.cover
        }

        // 背景图加载动画
        val bgLoaded = remember { mutableStateOf(false) }
        val bgAnimatedAlpha by animateFloatAsState(
            targetValue = if (bgLoaded.value) 1f else 0f,
            animationSpec = tween(durationMillis = 600),
            label = "hero bg alpha"
        )

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LocalConfiguration.current.screenHeightDp.dp * 0.6f),
                model = heroBackgroundCover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = bgAnimatedAlpha,
                onSuccess = { bgLoaded.value = true },
                onError = { bgLoaded.value = false }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LocalConfiguration.current.screenHeightDp.dp * 0.6f)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.9f),
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent
                            ),
                            startX = 0f,
                            endX = Float.MAX_VALUE * 0.5f
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LocalConfiguration.current.screenHeightDp.dp * 0.6f * 0.4f)
                    .align(Alignment.TopStart)
                    .padding(top = LocalConfiguration.current.screenHeightDp.dp * 0.6f * 0.6f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f),
                                Color.Black
                            )
                        )
                    )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    SeasonHeroSection(
                        playButtonFocusRequester = playButtonFocusRequester,
                        commentButtonFocusRequester = commentButtonFocusRequester,
                        vipButtonFocusRequester = vipButtonFocusRequester,
                        payButtonFocusRequester = payButtonFocusRequester,
                        backgroundCover = heroBackgroundCover,
                        title = seasonData.title,
                        styles = seasonData.styles,
                        newEpDesc = seasonData.newEpDesc,
                        description = seasonData.description,
                        lastPlayedIndex = seasonViewModel.lastPlayProgress?.lastEpId ?: -1,
                        lastPlayedTitle = generateEpisodeTitle(
                            seasonData.episodes.find { it.id == seasonViewModel.lastPlayProgress?.lastEpId },
                            seasonData.title
                        ),
                        following = seasonViewModel.isFollowing,
                        isPublished = seasonData.publish.isPublished,
                        publishDate = seasonData.publish.publishDate,
                        hasMultipleSeasons = seasonData.seasons.size > 1,
                        seasonCount = seasonData.seasons.size,
                        showOpenVipButton = accessState.showOpenVipButton,
                        showPayButton = accessState.showPayButton,
                        onPlay = {
                            logger.fInfo { "Click play button" }
                            var playAid = -1L
                            var playCid = -1L
                            val playEpid: Int
                            var episodeList: List<Episode> = emptyList()
                            if (seasonViewModel.lastPlayProgress == null) {
                                logger.fInfo { "Didn't find any play record" }
                                playAid = seasonViewModel.seasonData?.episodes?.first()?.aid ?: -1
                                playCid = seasonViewModel.seasonData?.episodes?.first()?.cid ?: -1
                                playEpid = seasonViewModel.seasonData?.episodes?.first()?.id ?: -1
                                if (playCid == -1L) {
                                    R.string.season_no_feature_film.toast(context)
                                } else {
                                    episodeList =
                                        seasonViewModel.seasonData?.episodes ?: emptyList()
                                }
                            } else {
                                logger.fInfo { "Find play record: ${seasonViewModel.lastPlayProgress}" }
                                playEpid = seasonViewModel.lastPlayProgress!!.lastEpId
                                seasonViewModel.seasonData?.episodes?.forEach {
                                    if (it.id == playEpid) {
                                        playAid = it.aid
                                        playCid = it.cid
                                        episodeList =
                                            seasonViewModel.seasonData?.episodes ?: emptyList()
                                    }
                                }
                                if (playCid == -1L) {
                                    seasonViewModel.seasonData?.sections?.forEach { section ->
                                        section.episodes.forEach {
                                            if (it.id == playEpid) {
                                                playAid = it.aid
                                                playCid = it.cid
                                                episodeList = section.episodes
                                            }
                                        }
                                    }
                                }
                                if (playCid == -1L) {
                                    logger.fInfo { "Can't find cid" }
                                    "无法判断最后播放的剧集".toast(context)
                                }
                            }

                            logger.fInfo { "Play aid: $playAid, cid: $playCid" }
                            val lastEpId = seasonViewModel.lastPlayProgress?.lastEpId
                            val ep = seasonViewModel.seasonData?.episodes?.find { it.id == lastEpId }
                                ?: seasonViewModel.seasonData?.episodes?.find { it.cid == playCid }
                            if (playCid != -1L) {
                                onClickVideo(
                                    playAid,
                                    playCid,
                                    playEpid,
                                    generateEpisodeTitle(ep, seasonViewModel.seasonData!!.title),
                                    seasonViewModel.lastPlayProgress?.lastTime ?: 0
                                )

                                val partVideoList = episodeList.mapIndexed { index, episode ->
                                    VideoListPgcEpisode(
                                        aid = episode.aid,
                                        cid = episode.cid,
                                        epid = episode.id,
                                        seasonId = seasonViewModel.seasonData?.seasonId,
                                        title = seasonViewModel.seasonData!!.title,
                                        partTitle = runCatching {
                                            "第 ${episode.title.toInt()} 集"
                                        }.getOrDefault(episode.title) + " " + episode.longTitle,
                                        index = index,
                                        cover = episode.cover,
                                        duration = episode.duration,
                                        viewCount = episode.viewCount,
                                        danmakuCount = episode.danmakuCount,
                                    )
                                }
                                videoInfoRepository.videoList.clear()
                                videoInfoRepository.videoList.addAll(partVideoList)
                            }
                        },
                        onClickFollow = onClickFollow,
                        onClickSeasonSelector = onClickCover,
                        onShowComment = onShowComment,
                        onOpenVip = { showAccessQrDialog = SeasonQrDialogType.OpenVip },
                        onPay = { showAccessQrDialog = SeasonQrDialogType.Pay },
                        onShowDescription = { showDescriptionDialog = true }
                    )
                }
                if (seasonViewModel.seasonData?.episodes?.isNotEmpty() == true) {
                    item {
                        SeasonEpisodeRow(
                            title = stringResource(R.string.season_feature_film),
                            episodes = seasonViewModel.seasonData?.episodes ?: emptyList(),
                            lastPlayedId = seasonViewModel.lastPlayProgress?.lastEpId ?: 0,
                            lastPlayedTime = seasonViewModel.lastPlayProgress?.lastTime ?: 0,
                            onClick = { avid, cid, epid, episodeTitle, startTime ->
                                onClickVideo(avid, cid, epid, episodeTitle, startTime)

                                val partVideoList =
                                    seasonViewModel.seasonData?.episodes?.mapIndexed { index, episode ->
                                        VideoListPgcEpisode(
                                            aid = episode.aid,
                                            cid = episode.cid,
                                            epid = episode.id,
                                            seasonId = seasonViewModel.seasonData?.seasonId,
                                            title = seasonViewModel.seasonData!!.title,
                                            partTitle = runCatching {
                                                "第 ${episode.title.toInt()} 集"
                                            }.getOrDefault(episode.title) + " " + episode.longTitle,
                                            index = index,
                                            cover = episode.cover,
                                            duration = episode.duration,
                                            viewCount = episode.viewCount,
                                            danmakuCount = episode.danmakuCount,
                                        )
                                    } ?: emptyList()
                                videoInfoRepository.videoList.clear()
                                videoInfoRepository.videoList.addAll(partVideoList)
                            }
                        )
                    }
                }
                seasonViewModel.seasonData?.sections?.forEach { section ->
                    item {
                        SeasonEpisodeRow(
                            title = section.title,
                            episodes = section.episodes,
                            lastPlayedId = seasonViewModel.lastPlayProgress?.lastEpId ?: 0,
                            lastPlayedTime = seasonViewModel.lastPlayProgress?.lastTime ?: 0,
                            onClick = { avid, cid, epid, episodeTitle, startTime ->
                                onClickVideo(avid, cid, epid, episodeTitle, startTime)

                                val partVideoList = section.episodes.mapIndexed { index, episode ->
                                    VideoListPgcEpisode(
                                        aid = episode.aid,
                                        cid = episode.cid,
                                        epid = episode.id,
                                        seasonId = seasonViewModel.seasonData?.seasonId,
                                        title = runCatching {
                                            "第 ${episode.title.toInt()} 集"
                                        }.getOrDefault(episode.title) + " " + episode.longTitle,
                                        index = index,
                                        cover = episode.cover,
                                        duration = episode.duration,
                                        viewCount = episode.viewCount,
                                        danmakuCount = episode.danmakuCount,
                                    )
                                }
                                videoInfoRepository.videoList.clear()
                                videoInfoRepository.videoList.addAll(partVideoList)
                            }
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
    }

    SeasonSelector(
        show = showSeasonSelector,
        onHideSelector = {
            showSeasonSelector = false
            runCatching {
                playButtonFocusRequester.requestFocus(scope)
            }
        },
        currentSeasonId = seasonViewModel.seasonId ?: 0,
        seasons = seasonViewModel.seasonData?.seasons ?: emptyList(),
        onClickSeason = { sid ->
            if ((seasonViewModel.seasonId ?: 0) != sid) {
                seasonViewModel.seasonData = null
                seasonViewModel.seasonId = sid
                seasonViewModel.epId = null
                scope.launch(Dispatchers.IO) { seasonViewModel.updateSeasonData() }
            }
        }
    )

    CommentPanel(
        show = showCommentPanel,
        oid = getCommentAid(),
        onHide = {
            showCommentPanel = false
            commentButtonFocusRequester.requestFocus(scope)
        },
        episodes = seasonViewModel.seasonData?.episodes ?: emptyList(),
        sections = seasonViewModel.seasonData?.sections ?: emptyList(),
        initialEpisodeId = seasonViewModel.lastPlayProgress?.lastEpId ?: -1,
        onEpisodeChange = { episode ->
            logger.debug { "User viewed comments for episode: ${episode.id} (${episode.title})" }
        }
    )

    if (showAccessQrDialog != null && seasonAccessState != null) {
        when (showAccessQrDialog) {
            SeasonQrDialogType.OpenVip -> {
                SeasonAccessQrDialog(
                    title = "开通会员",
                    description = "手机扫码前往开通会员",
                    qrContent = VipOpenUrl,
                    onDismissRequest = hideAccessQrDialog
                )
            }

            SeasonQrDialogType.Pay -> {
                seasonAccessState.payUrl.takeIf { it.isNotBlank() }?.let { payUrl ->
                    SeasonAccessQrDialog(
                        title = "前往付费",
                        description = "手机扫码前往影片页面付费",
                        qrContent = payUrl,
                        onDismissRequest = hideAccessQrDialog
                    )
                }
            }

            null -> Unit
        }
    }

    // 简介弹窗
    if (showDescriptionDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showDescriptionDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0xFF1a1a2e).copy(alpha = 0.7f))
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = seasonViewModel.seasonData?.title ?: "",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    val scrollState = rememberLazyListState()
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        state = scrollState
                    ) {
                        item {
                            Text(
                                text = seasonViewModel.seasonData?.description ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Hero 风格
 */
@Composable
fun SeasonHeroSection(
    modifier: Modifier = Modifier,
    playButtonFocusRequester: FocusRequester,
    commentButtonFocusRequester: FocusRequester = remember { FocusRequester() },
    vipButtonFocusRequester: FocusRequester = remember { FocusRequester() },
    payButtonFocusRequester: FocusRequester = remember { FocusRequester() },
    backgroundCover: String,
    title: String,
    styles: List<String> = emptyList(),
    newEpDesc: String,
    description: String,
    lastPlayedIndex: Int,
    lastPlayedTitle: String = "",
    following: Boolean,
    isPublished: Boolean,
    publishDate: String,
    hasMultipleSeasons: Boolean = false,
    seasonCount: Int = 0,
    showOpenVipButton: Boolean = false,
    showPayButton: Boolean = false,
    onPlay: () -> Unit,
    onOpenVip: () -> Unit = {},
    onPay: () -> Unit = {},
    onClickFollow: (follow: Boolean) -> Unit,
    onClickSeasonSelector: () -> Unit = {},
    onShowComment: () -> Unit = {},
    onShowDescription: () -> Unit = {}
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    val metaLine = remember(styles, newEpDesc) {
        buildString {
            if (styles.isNotEmpty()) {
                append(styles.joinToString(" / "))
            }
            if (newEpDesc.isNotEmpty()) {
                if (isNotEmpty()) append("  ·  ")
                append(newEpDesc)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(screenHeight * 0.6f)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp, end = 48.dp, bottom = 24.dp)
                .fillMaxWidth(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 标题
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.White
            )

            if (metaLine.isNotEmpty()) {
                Text(
                    text = metaLine,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            SeasonInfoButtons(
                focusRequester = playButtonFocusRequester,
                openVipButtonFocusRequester = vipButtonFocusRequester,
                payButtonFocusRequester = payButtonFocusRequester,
                lastPlayedIndex = lastPlayedIndex,
                lastPlayedTitle = lastPlayedTitle,
                following = following,
                isPublished = isPublished,
                publishDate = publishDate,
                showOpenVipButton = showOpenVipButton,
                showPayButton = showPayButton,
                onPlay = onPlay,
                onOpenVip = onOpenVip,
                onPay = onPay,
                onClickFollow = onClickFollow,
                onShowComment = onShowComment,
                commentButtonFocusRequester = commentButtonFocusRequester,
                seasonCount = seasonCount,
                onClickSeasonSelector = onClickSeasonSelector,
                onShowDescription = onShowDescription
            )
        }
    }
}

@Composable
private fun SeasonAccessQrDialog(
    title: String,
    description: String,
    qrContent: String,
    onDismissRequest: () -> Unit
) {
    TvAlertDialog(
        modifier = Modifier.widthIn(min = 760.dp, max = 860.dp),
        // title = {
        //     Text(text = title)
        // },
        text = {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QrImage(
                    modifier = Modifier.size(220.dp),
                    content = qrContent,
                    borderWidth = 20.dp,
                    showLoadingWhenContentChanged = false
                )
                Column(
                    modifier = Modifier.widthIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    Text(
                        text = "请使用哔哩哔哩手机客户端扫码",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text(text = "关闭")
            }
        },
        containerColor = Color(0xFF111111).copy(alpha = 0.96f),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}


@Composable
fun SeasonEpisodeButton(
    modifier: Modifier = Modifier,
    partTitle: String = "",
    title: String,
    cover: String,
    duration: Int,
    badge: String = "",
    badgeInfo: Episode.BadgeInfo? = null,
    viewCount: Long = 0,
    danmakuCount: Int = 0,
    played: Int = 0,
    isLastPlayed: Boolean = false,
    onClick: () -> Unit
) {
    val isPreview = LocalInspectionMode.current
    val lastPlayedColor = Color(0xFFE39B17)
    val focusedColor = Color(0xFFF1CD8B)
    val useDarkBadgeColor = isSystemInDarkTheme()

    // 格式化时长显示
    val durationText = remember(duration) {
        if (duration <= 0) ""
        else {
            val totalSec = duration / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            "%d:%02d".format(min, sec)
        }
    }
    val playCountText = remember(viewCount) { formatEpisodeStatCount(viewCount) }
    val danmakuCountText = remember(danmakuCount) { formatEpisodeStatCount(danmakuCount.toLong()) }
    val topRightBadgeText = remember(badge, badgeInfo) {
        badgeInfo?.text?.takeIf { it.isNotBlank() } ?: badge.takeIf { it.isNotBlank() }
    }
    val topRightBadgeBackground = remember(badgeInfo, useDarkBadgeColor) {
        parseEpisodeBadgeColor(
            color = badgeInfo?.let {
                if (useDarkBadgeColor) it.bgColorNight else it.bgColor
            },
            fallback = Color.Black.copy(alpha = 0.7f)
        )
    }

    // 播放进度比例
    val progressFraction = remember(played, duration) {
        if (duration == 0) 0f
        else if (played < 0) 1f
        else (played * 1000f / duration).coerceIn(0f, 1f)
    }

    Surface(
        modifier = modifier.width(220.dp),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            pressedContainerColor = Color.Transparent
        ),
        scale = ClickableSurfaceDefaults.scale(scale = 1f, focusedScale = 1.05f),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        border = ClickableSurfaceDefaults.border(
            border = Border.None,
            focusedBorder = Border(
                border = BorderStroke(2.dp, if (isLastPlayed) lastPlayedColor else focusedColor),
                shape = MaterialTheme.shapes.medium
            ),
            pressedBorder = Border.None
        ),
        onClick = onClick
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(if (isPreview) Color.Gray else Color.Black)
            ) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = cover.resizedImageUrl(ImageSize.TvEpisodeCover),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )

                if (!topRightBadgeText.isNullOrEmpty()) {
                    Text(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .widthIn(max = 150.dp)
                            .background(
                                color = topRightBadgeBackground,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        text = topRightBadgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isLastPlayed) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier.size(36.dp),
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                if (playCountText.isNotEmpty() || danmakuCountText.isNotEmpty() || durationText.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(start = 6.dp, end = 6.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (playCountText.isNotEmpty() || danmakuCountText.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .background(
                                        Color.Black.copy(alpha = 0.7f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (playCountText.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            modifier = Modifier.size(12.dp),
                                            painter = painterResource(id = R.drawable.ic_play_count),
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                        Text(
                                            text = playCountText,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                                if (danmakuCountText.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            modifier = Modifier.size(12.dp),
                                            painter = painterResource(id = R.drawable.ic_danmaku_count),
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                        Text(
                                            text = danmakuCountText,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier)
                        }

                        if (durationText.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color.Black.copy(alpha = 0.7f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = durationText,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // 底部进度条
                if (progressFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressFraction)
                                .background(lastPlayedColor)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = partTitle,
                    fontSize = 11.sp,
                    color = if (isLastPlayed) lastPlayedColor else Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = title,
                    fontSize = 13.sp,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SeasonEpisodesDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    title: String,
    episodes: List<Episode>,
    lastPlayedId: Int = 0,
    lastPlayedTime: Int = 0,
    onHideDialog: () -> Unit,
    onClick: (avid: Long, cid: Long, epid: Int, episodeTitle: String, startTime: Int) -> Unit
) {
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabCount by remember { mutableIntStateOf(ceil(episodes.size / 20.0).toInt()) }
    val selectedEpisodes = remember { mutableStateListOf<Episode>() }

    val tabFocusRequester = remember { FocusRequester() }
    val tabRowFocusRequester = remember { FocusRequester() }
    val videoListFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyGridState()

    LaunchedEffect(selectedTabIndex) {
        val fromIndex = selectedTabIndex * 20
        var toIndex = (selectedTabIndex + 1) * 20
        if (toIndex >= episodes.size) {
            toIndex = episodes.size
        }
        selectedEpisodes.swapList(episodes.subList(fromIndex, toIndex))
    }

    LaunchedEffect(show) {
        if (show && tabCount > 1) tabFocusRequester.requestFocus(scope)
        if (show && tabCount == 1) videoListFocusRequester.requestFocus(scope)
    }

    if (show) {
        TvAlertDialog(
            modifier = modifier,
            title = { Text(text = title) },
            onDismissRequest = { onHideDialog() },
            confirmButton = {},
            properties = DialogProperties(usePlatformDefaultWidth = false),
            text = {
                Column(
                    modifier = Modifier
                        .size(700.dp, 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // TabRow 只有一项 Tab 时会导致崩溃，但如果只有一项 Tab 的时候也没必要显示
                    // https://issuetracker.google.com/issues/264018028
                    if (tabCount > 1) {
                        TabRow(
                            modifier = Modifier
                                .onFocusChanged {
                                    if (it.hasFocus) {
                                        scope.launch(Dispatchers.Main) {
                                            listState.scrollToItem(0)
                                        }
                                    }
                                }
                                .focusRestorer()
                                .focusRequester(tabRowFocusRequester),
                            selectedTabIndex = selectedTabIndex,
                            separator = { Spacer(modifier = Modifier.width(12.dp)) },
                        ) {
                            for (i in 0 until tabCount) {
                                Tab(
                                    modifier = if (i == 0) Modifier.focusRequester(
                                        tabFocusRequester
                                    ) else Modifier,
                                    selected = i == selectedTabIndex,
                                    onFocus = { selectedTabIndex = i },
                                ) {
                                    Text(
                                        text = "P${i * 20 + 1}-${(i + 1) * 20}",
                                        fontSize = 12.sp,
                                        color = LocalContentColor.current,
                                        modifier = Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 6.dp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    LazyVerticalGrid(
                        modifier = Modifier
                            .onBackPressed {
                                if (tabCount > 1) tabRowFocusRequester.requestFocus() else onHideDialog()
                            },
                        state = listState,
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(
                            items = selectedEpisodes,
                            key = { _, episode -> episode.aid + episode.cid }
                        ) { index, episode ->
                            val episodeTitle by remember { mutableStateOf(generateEpisodeTitle(episode, title)) }
                            val buttonModifier =
                                if (index == 0) Modifier.focusRequester(videoListFocusRequester) else Modifier
                            SeasonEpisodeButton(
                                modifier = buttonModifier
                                    .focusedScale(0.95f),
                                partTitle = if (title == "正片") {
                                    //如果 title 是数字的话，就会返回 "第 x 集"
                                    //如果 title 不是数字的话（例如 SP），就会原样使用 title
                                    runCatching {
                                        "第 ${episode.title.toInt()} 集"
                                    }.getOrDefault(episode.title)
                                } else {
                                    "P${index + 1 + selectedTabIndex * 20}"
                                },
                                title = episodeTitle,
                                cover = episode.cover,
                                badge = episode.badge,
                                badgeInfo = episode.badgeInfo,
                                viewCount = episode.viewCount,
                                danmakuCount = episode.danmakuCount,
                                played = if (episode.id == lastPlayedId) lastPlayedTime else 0,
                                isLastPlayed = episode.id == lastPlayedId,
                                duration = episode.duration,
                                onClick = {
                                    onClick(
                                        episode.aid,
                                        episode.cid,
                                        episode.id,
                                        generateEpisodeTitle(episode, title),
                                        if (episode.id == lastPlayedId) lastPlayedTime else 0
                                    )
                                }
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun SeasonEpisodeRow(
    modifier: Modifier = Modifier,
    title: String,
    episodes: List<Episode>,
    lastPlayedId: Int = 0,
    lastPlayedTime: Int = 0,
    onClick: (avid: Long, cid: Long, epid: Int, episodeTitle: String, startTime: Int) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val rowState = rememberLazyListState()
    var hasFocus by remember { mutableStateOf(false) }
    val titleColor = if (hasFocus) Color.White else Color.White.copy(alpha = 0.6f)
    val titleFontSize by animateFloatAsState(
        targetValue = if (hasFocus) 30f else 14f,
        label = "title font size"
    )

    var showEpisodesDialog by remember { mutableStateOf(false) }

    // 当存在历史记录时，滚动到对应集
    LaunchedEffect(lastPlayedId, episodes) {
        if (lastPlayedId != 0 && episodes.isNotEmpty()) {
            val lastPlayedIndex = episodes.indexOfFirst { it.id == lastPlayedId }
            if (lastPlayedIndex != -1) {
                rowState.scrollToItem(lastPlayedIndex)
            }
        }
    }

    Column(
        modifier = modifier
            .padding(bottom = 24.dp)
            .onFocusChanged { hasFocus = it.hasFocus },
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.padding(start = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontSize = titleFontSize.sp,
                color = titleColor
            )
            Surface(
                modifier = Modifier.size(32.dp),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
                    pressedContainerColor = MaterialTheme.colorScheme.inverseSurface
                ),
                shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
                onClick = { showEpisodesDialog = true }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(90f),
                        imageVector = Icons.Rounded.ViewModule,
                        contentDescription = null
                    )
                }
            }
        }

        LazyRow(
            modifier = Modifier
                .padding(top = 15.dp)
                .focusRestorer(focusRequester),
            state = rowState,
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(items = episodes) { index, episode ->
                val episodeTitle by remember { mutableStateOf(if (episode.longTitle != "") episode.longTitle else episode.title) }
                SeasonEpisodeButton(
                    modifier = Modifier
                        .ifElse(index == 0, Modifier.focusRequester(focusRequester)),
                    partTitle = if (title == "正片") {
                        //如果 title 是数字的话，就会返回 "第 x 集"
                        //如果 title 不是数字的话（例如 SP），就会原样使用 title
                        runCatching {
                            "第 ${episode.title.toInt()} 集"
                        }.getOrDefault(episode.title)
                    } else {
                        "P${index + 1}"
                    },
                    title = episodeTitle,
                    cover = episode.cover,
                    badge = episode.badge,
                    badgeInfo = episode.badgeInfo,
                    viewCount = episode.viewCount,
                    danmakuCount = episode.danmakuCount,
                    played = if (episode.id == lastPlayedId) lastPlayedTime else 0,
                    isLastPlayed = episode.id == lastPlayedId,
                    duration = episode.duration,
                    onClick = {
                        val pTitle = generateEpisodeTitle(episode, title)
                        onClick(
                            episode.aid,
                            episode.cid,
                            episode.id,
                            pTitle,
                            if (episode.id == lastPlayedId) lastPlayedTime else 0
                        )
                    }
                )
            }
        }
    }

    SeasonEpisodesDialog(
        show = showEpisodesDialog,
        title = title,
        episodes = episodes,
        lastPlayedId = lastPlayedId,
        lastPlayedTime = lastPlayedTime,
        onHideDialog = { showEpisodesDialog = false },
        onClick = onClick
    )
}

@Composable
fun SeasonSelector(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideSelector: () -> Unit,
    currentSeasonId: Int,
    seasons: List<PgcSeason>,
    onClickSeason: (Int) -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(show) {
        if (show) {
            focusRequester.requestFocus()
        }
    }

    if (show) {
        SeasonSelectorContent(
            modifier = modifier
                .focusRequester(focusRequester),
            seasons = seasons,
            currentSeasonId = currentSeasonId,
            onClickSeason = { seasonId ->
                onClickSeason(seasonId)
                onHideSelector()
            }
        )
    }

    BackHandler(show) {
        onHideSelector()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SeasonSelectorContent(
    modifier: Modifier = Modifier,
    currentSeasonId: Int,
    seasons: List<PgcSeason>,
    onClickSeason: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val rowState = rememberLazyListState()
    val logger = KotlinLogging.logger {}
    val currentSeasonFocusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    var scrolling by remember { mutableStateOf(false) }
    var currentSeasonIndex by remember { mutableIntStateOf(0) }
    val isCurrentSeasonInScreen by remember {
        derivedStateOf {
            rowState.layoutInfo.visibleItemsInfo.first().index <= currentSeasonIndex
                    && rowState.layoutInfo.visibleItemsInfo.last().index >= currentSeasonIndex
        }
    }

    val scrollToCurrentSeason = {
        currentSeasonIndex = seasons.indexOfFirst { it.seasonId == currentSeasonId }
        logger.info { "Season row scroll to index $currentSeasonIndex" }
        if (currentSeasonIndex != -1) {
            if (isCurrentSeasonInScreen) {
                currentSeasonFocusRequester.requestFocus()
            } else {
                scope.launch {
                    scrolling = true
                    rowState.scrollToItem(currentSeasonIndex)
                }
            }
        }
    }

    LaunchedEffect(rowState.firstVisibleItemScrollOffset) {
        if (scrolling && isCurrentSeasonInScreen) {
            scrolling = false
            delay(300)
            currentSeasonFocusRequester.requestFocus()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .onFocusChanged {
                if (it.hasFocus) scrollToCurrentSeason()
            },
        shape = RoundedCornerShape(0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                AsyncImage(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .fillMaxHeight(0.7f)
                        .graphicsLayer { alpha = 0.99f }
                        .drawWithContent {
                            val colors = listOf(
                                Color.Black,
                                Color.Transparent
                            )
                            drawContent()
                            drawRect(
                                brush = Brush.horizontalGradient(colors),
                                blendMode = BlendMode.DstOut
                            )
                            drawRect(
                                brush = Brush.verticalGradient(colors),
                                blendMode = BlendMode.DstIn
                            )
                        },
                    model = seasons[currentSeasonIndex].horizontalCover ?: "",
                    contentDescription = null,
                    contentScale = ContentScale.FillHeight,
                    alpha = 1f
                )
                Text(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 48.dp, top = 36.dp),
                    text = "选择系列",
                    style = MaterialTheme.typography.headlineMedium
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(
                            start = 48.dp,
                            end = 48.dp,
                            bottom = 300.dp
                        )
                ) {
                    Text(
                        text = seasons[currentSeasonIndex].title
                            ?: seasons[currentSeasonIndex].shortTitle,
                        style = MaterialTheme.typography.displayMedium
                    )
                }
            }

            Box(
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                LazyRow(
                    modifier = Modifier.padding(bottom = 48.dp),
                    state = rowState,
                    contentPadding = PaddingValues(horizontal = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    itemsIndexed(items = seasons) { index, season ->
                        Card(
                            modifier = Modifier
                                .onFocusChanged {
                                    if (it.hasFocus) currentSeasonIndex = index
                                }
                                .ifElse(
                                    season.seasonId == currentSeasonId,
                                    Modifier.focusRequester(currentSeasonFocusRequester)
                                )
                                .ifElse(
                                    season.seasonId == currentSeasonId,
                                    Modifier.bringIntoViewRequester(bringIntoViewRequester)
                                ),
                            glow = CardDefaults.glow(
                                focusedGlow = Glow(
                                    elevationColor = MaterialTheme.colorScheme.inverseSurface,
                                    elevation = 16.dp
                                )
                            ),
                            border = if (Build.VERSION.SDK_INT < 31) {
                                CardDefaults.border()
                            } else {
                                CardDefaults.border(
                                    focusedBorder = Border(BorderStroke(0.dp, Color.Transparent))
                                )
                            },
                            onClick = {
                                onClickSeason(season.seasonId)
                            }
                        ) {
                            AsyncImage(
                                modifier = Modifier
                                    .width(160.dp)
                                    .aspectRatio(0.75f),
                                model = seasons[index].cover,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(device = "id:tv_1080p")
@Preview(device = "id:tv_1080p", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SeasonHeroSectionPreview() {
    BVTheme {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            SeasonHeroSection(
                playButtonFocusRequester = remember { FocusRequester() },
                backgroundCover = "http://i0.hdslb.com/bfs/bangumi/e3993240914c3d881d97e4527a52efa2a9dcdeaf.jpg",
                title = "人生一串",
                styles = listOf("纪录片", "美食", "人文"),
                newEpDesc = "已完结, 全8集",
                description = "由bilibili和旗帜传媒联合出品的《人生一串》是国内首档汇聚民间烧烤美食，呈现国人烧烤情结的深夜美食纪录片，本片将镜头伸向街头巷尾，讲述平民美食和市井传奇，以最独特的视角真实展现烧烤美食背后的独特情感。",
                lastPlayedIndex = 3,
                lastPlayedTitle = "拯救灵依计划",
                following = false,
                isPublished = true,
                publishDate = "2021-04-30",
                hasMultipleSeasons = true,
                onPlay = {},
                onClickFollow = {},
                onClickSeasonSelector = {},
                onShowComment = {}
            )
        }
    }
}

@Preview(device = "id:tv_1080p")
@Preview(device = "id:tv_1080p", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SeasonEpisodeRowPreview() {
    val episodes = remember { mutableStateListOf<Episode>() }
    for (i in 0..10) {
        episodes.add(
            Episode(
                id = 0,
                aid = 0,
                bvid = "",
                cid = 0,
                epid = 1000 + i,
                title = "这可能是我这辈子距离梅西最近的一次",
                longTitle = "",
                cover = "",
                duration = 0,
                dimension = null,
                pages = emptyList()
            )
        )
    }
    BVTheme {
        SeasonEpisodeRow(
            title = "正片",
            episodes = episodes,
            onClick = { _, _, _, _, _ -> }
        )
    }
}

@Preview(device = "id:tv_1080p")
@Preview(device = "id:tv_1080p", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SeasonSelectorPreview() {
    val seasons = listOf(
        PgcSeason(
            seasonId = 25210,
            title = "命运之夜  06版",
            shortTitle = "FATE TV",
            cover = "http://i0.hdslb.com/bfs/bangumi/1113d844ad3a9b42af576d80142146cbecc1b7ff.jpg",
            horizontalCover = "http://i0.hdslb.com/bfs/bangumi/e3993240914c3d881d97e4527a52efa2a9dcdeaf.jpg"
        ),
        PgcSeason(
            seasonId = 29006,
            title = "Fate/stay night UNLIMITED BLADE WORKS",
            shortTitle = "UBW 剧场版",
            cover = "http://i0.hdslb.com/bfs/bangumi/image/b7ee578ff3c258f173587db3f687fa2d56e3b8c1.jpg",
            horizontalCover = "http://i0.hdslb.com/bfs/archive/ae6fcc22f6c627a899bfe2d736765cc83cd4e827.png"
        ),
        PgcSeason(
            seasonId = 1586,
            title = "Fate/stay night [Unlimited Blade Works] 第一季",
            shortTitle = "UBW第一季",
            cover = "http://i0.hdslb.com/bfs/bangumi/image/e67e09c9e48a32371a81100e0f65a61b18aabb24.png",
            horizontalCover = "http://i0.hdslb.com/bfs/bangumi/25e9da6dd71e4aaa23a7dc04b6f97a94ea1ddd9d.jpg"
        ),
        PgcSeason(
            seasonId = 25210,
            title = "命运之夜  06版",
            shortTitle = "FATE TV",
            cover = "http://i0.hdslb.com/bfs/bangumi/1113d844ad3a9b42af576d80142146cbecc1b7ff.jpg",
            horizontalCover = "http://i0.hdslb.com/bfs/bangumi/e3993240914c3d881d97e4527a52efa2a9dcdeaf.jpg"
        ),
        PgcSeason(
            seasonId = 29006,
            title = "Fate/stay night UNLIMITED BLADE WORKS",
            shortTitle = "UBW 剧场版",
            cover = "http://i0.hdslb.com/bfs/bangumi/image/b7ee578ff3c258f173587db3f687fa2d56e3b8c1.jpg",
            horizontalCover = "http://i0.hdslb.com/bfs/archive/ae6fcc22f6c627a899bfe2d736765cc83cd4e827.png"
        ),
        PgcSeason(
            seasonId = 1586,
            title = "Fate/stay night [Unlimited Blade Works] 第一季",
            shortTitle = "UBW第一季",
            cover = "http://i0.hdslb.com/bfs/bangumi/image/e67e09c9e48a32371a81100e0f65a61b18aabb24.png",
            horizontalCover = "http://i0.hdslb.com/bfs/bangumi/25e9da6dd71e4aaa23a7dc04b6f97a94ea1ddd9d.jpg"
        ),
    )
    BVTheme {
        SeasonSelectorContent(
            seasons = seasons,
            currentSeasonId = 25210,
            onClickSeason = {}
        )
    }
}