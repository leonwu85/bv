package dev.aaa1115910.bv.viewmodel.video

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.FavoriteFolderMetadata
import dev.aaa1115910.biliapi.entity.user.Author
import dev.aaa1115910.biliapi.entity.video.Dimension
import dev.aaa1115910.biliapi.repositories.CoinRepository
import dev.aaa1115910.biliapi.repositories.FavoriteRepository
import dev.aaa1115910.biliapi.repositories.LikeRepository
import dev.aaa1115910.biliapi.repositories.TripleLikeRepository
import dev.aaa1115910.biliapi.repositories.ToViewRepository
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.biliapi.entity.video.VideoDetail
import dev.aaa1115910.biliapi.entity.video.UserActions
import dev.aaa1115910.biliapi.entity.video.VideoPage
import dev.aaa1115910.biliapi.repositories.VideoDetailRepository
import dev.aaa1115910.bv.player.autoplay.toInteractivePlaybackContextOrNull
import dev.aaa1115910.bv.player.autoplay.toRelatedVideoCardDataList
import dev.aaa1115910.bv.player.autoplay.toVideoListForTargetCid
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.player.entity.VideoListItem
import dev.aaa1115910.bv.player.entity.VideoListInteractiveNode
import dev.aaa1115910.bv.player.entity.VideoListPart
import dev.aaa1115910.bv.player.entity.VideoListUgcEpisode
import dev.aaa1115910.bv.player.entity.VideoListUgcEpisodeTitle
import dev.aaa1115910.bv.offline.OfflineVideoCacheEntry
import dev.aaa1115910.bv.repository.VideoInfoRepository
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.swapListWithMainContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel
import java.util.Date

data class UpOwnerStats(
    val followerCount: Int,
    val archiveCount: Int
)

@KoinViewModel
class VideoDetailViewModel(
    private val videoDetailRepository: VideoDetailRepository,
    private val videoInfoRepository: VideoInfoRepository,
    private val likeRepository: LikeRepository,
    private val coinRepository: CoinRepository,
    private val favoriteRepository: FavoriteRepository,
    private val tripleLikeRepository: TripleLikeRepository,
    private val toViewRepository: ToViewRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    companion object {
        val DETAIL_API_TYPE: ApiType
            get() = Prefs.apiType
    }

    private val logger = KotlinLogging.logger { }
    var state by mutableStateOf(VideoInfoState.Loading)
    var videoDetail: VideoDetail? by mutableStateOf(null)

    var relatedVideos = mutableStateListOf<VideoCardData>()
    var favoriteFolders = mutableStateListOf<FavoriteFolderMetadata>()
    var favoriteFolderIds = mutableStateListOf<Long>()
    var upOwnerStats: UpOwnerStats? by mutableStateOf(null)
    var inToView by mutableStateOf(false)
    var userActionUpdating by mutableStateOf(false)
    var favoriteFoldersLoading by mutableStateOf(false)

    suspend fun loadDetail(aid: Long, fromPgcSeason: Boolean = false, withUserActions: Boolean = true) {
        logger.fInfo { "Load detail: [avid=$aid, preferApiType=${DETAIL_API_TYPE.name}]" }
        state = VideoInfoState.Loading
        var loadedVideoDetail: VideoDetail? = null
        runCatching {
            val videoDetailData = videoDetailRepository.getVideoDetail(
                aid = aid,
                preferApiType = DETAIL_API_TYPE,
                withUserActions = withUserActions
            )
            loadedVideoDetail = videoDetailData
            withContext(Dispatchers.Main) {
                videoDetail = videoDetailData
                upOwnerStats = null
            }
            if (!fromPgcSeason) updateVideoList(aid)
        }.onFailure {
            state = VideoInfoState.Error
            logger.fInfo { "Load video av$aid failed: ${it.stackTraceToString()}" }
        }.onSuccess {
            state = VideoInfoState.Success
            logger.fInfo { "Load video av$aid success" }

            updateRelatedVideos()
            refreshUpOwnerStats(loadedVideoDetail?.author?.mid ?: 0L)
            if (Prefs.isLogin) {
                refreshFavoriteFolders(aid)
            }
        }.getOrThrow()
    }

    suspend fun loadDetailOnlyUpdateHistory(aid: Long) {
        logger.fInfo { "Load detail only update history: [avid=$aid, preferApiType=${DETAIL_API_TYPE.name}]" }
        runCatching {
            val historyData = videoDetailRepository.getVideoDetail(
                aid = aid,
                preferApiType = DETAIL_API_TYPE,
                withUserActions = false
            ).history
            withContext(Dispatchers.Main) { videoDetail?.history = historyData }
        }.onFailure {
            logger.fInfo { "Load video av$aid only update history failed: ${it.stackTraceToString()}" }
        }.onSuccess {
            logger.fInfo { "Load video av$aid only update history success: ${videoDetail?.history}" }
        }
    }

    suspend fun applyOfflineCacheFallback(
        entry: OfflineVideoCacheEntry,
        entries: List<OfflineVideoCacheEntry> = listOf(entry)
    ) {
        val offlineDetail = entry.toOfflineVideoDetail(entries)
        logger.fInfo { "Apply offline detail fallback: [avid=${entry.aid}, cid=${entry.cid}]" }
        withContext(Dispatchers.Main) {
            videoDetail = offlineDetail
            state = VideoInfoState.Success
            upOwnerStats = null
            relatedVideos.clear()
            videoInfoRepository.videoList.clear()
            videoInfoRepository.videoList.addAll(offlineDetail.toVideoListForTargetCid(entry.cid))
        }
    }

    private suspend fun updateRelatedVideos() {
        logger.fInfo { "Start update relate video" }
        val relateVideoCardDataList = videoDetail?.toRelatedVideoCardDataList() ?: emptyList()
        relatedVideos.swapListWithMainContext(relateVideoCardDataList)
        logger.fInfo { "Update ${relateVideoCardDataList.size} relate videos" }
    }

    private fun syncInteractivePlaybackContext() {
        videoInfoRepository.interactivePlaybackContext = videoDetail?.toInteractivePlaybackContextOrNull()
    }

    private fun updateVideoList(aid: Long) {
        syncInteractivePlaybackContext()
        val detail = videoDetail ?: return
        videoInfoRepository.videoList.clear()
        videoInfoRepository.videoList.addAll(detail.toVideoListForTargetCid(detail.cid))
    }

    private fun OfflineVideoCacheEntry.toOfflineVideoDetail(entries: List<OfflineVideoCacheEntry>): VideoDetail {
        val pages = entries
            .filter { it.aid == aid && it.completed }
            .distinctBy { it.cid }
            .ifEmpty { listOf(this) }
            .mapIndexed { index, item ->
                VideoPage(
                    cid = item.cid,
                    index = index + 1,
                    title = item.partTitle.ifBlank { item.title },
                    duration = (item.durationMs / 1000L)
                        .coerceAtMost(Int.MAX_VALUE.toLong())
                        .toInt(),
                    dimension = Dimension(
                        width = item.width,
                        height = item.height
                    )
                )
            }

        return VideoDetail(
            bvid = bvid,
            aid = aid,
            cid = cid,
            cover = cover,
            title = title.ifBlank { partTitle },
            publishDate = Date(0L),
            description = "",
            stat = VideoDetail.Stat(
                view = 0,
                danmaku = 0,
                reply = 0,
                favorite = 0,
                coin = 0,
                share = 0,
                like = 0,
                historyRank = 0
            ),
            author = Author(
                mid = 0L,
                name = upName,
                face = ""
            ),
            pages = pages,
            ugcSeason = null,
            relatedVideos = emptyList(),
            redirectToEp = false,
            epid = null,
            argueTip = null,
            tags = emptyList(),
            userActions = UserActions(),
            history = VideoDetail.History(
                progress = 0,
                lastPlayedCid = cid
            )
        )
    }

    fun updateUgcSeasonSectionVideoList(sectionIndex: Int) {
        val partVideoList = mutableListOf<VideoListItem>()
        videoDetail!!.ugcSeason!!.sections[sectionIndex].episodes.mapIndexed { epIndex, episode ->
            if (episode.pages.size == 1) {
                episode.pages.mapIndexed { pageInd, videoPage ->
                    partVideoList.add(
                        VideoListUgcEpisode(
                            aid = episode.aid,
                            cid = videoPage.cid,
                            title = episode.title,
                            partTitle = "",
                            index = epIndex
                        )
                    )
                }
            } else {
                partVideoList.add(
                    VideoListUgcEpisodeTitle(
                        title = episode.title,
                        index = epIndex,
                    )
                )
                episode.pages.mapIndexed { pageIndex, videoPage ->
                    partVideoList.add(
                        VideoListPart(
                            aid = episode.aid,
                            cid = videoPage.cid,
                            title = episode.title,
                            partTitle = videoPage.title,
                            index = pageIndex,
                        )
                    )
                }
            }
        }
        videoInfoRepository.videoList.clear()
        videoInfoRepository.videoList.addAll(partVideoList)
    }

    private fun ensureLoggedIn() {
        check(Prefs.isLogin) { "账号未登录" }
    }

    private fun currentAid(): Long {
        return videoDetail?.aid?.takeIf { it > 0L } ?: error("视频信息未加载")
    }

    private suspend fun <T> runUserAction(block: suspend () -> T): Result<T> {
        if (userActionUpdating) return Result.failure(IllegalStateException("操作过快，请稍后再试"))
        return runCatching {
            ensureLoggedIn()
            withContext(Dispatchers.Main) { userActionUpdating = true }
            block()
        }.also {
            withContext(Dispatchers.Main) { userActionUpdating = false }
        }
    }

    private suspend fun updateDetailOnMain(transform: (VideoDetail) -> VideoDetail) {
        withContext(Dispatchers.Main) {
            videoDetail = videoDetail?.let(transform)
        }
    }

    private suspend fun setLikeState(liked: Boolean) {
        updateDetailOnMain { detail ->
            val oldLiked = detail.userActions.like
            val likeDelta = when {
                liked && !oldLiked -> 1
                !liked && oldLiked -> -1
                else -> 0
            }
            detail.copy(
                stat = detail.stat.copy(
                    like = (detail.stat.like + likeDelta).coerceAtLeast(0)
                ),
                userActions = detail.userActions.copy(
                    like = liked,
                    dislike = if (liked) false else detail.userActions.dislike
                )
            )
        }
    }

    private suspend fun setDislikeState(disliked: Boolean) {
        updateDetailOnMain { detail ->
            val wasLiked = detail.userActions.like
            detail.copy(
                stat = detail.stat.copy(
                    like = (detail.stat.like + if (disliked && wasLiked) -1 else 0)
                        .coerceAtLeast(0)
                ),
                userActions = detail.userActions.copy(
                    like = if (disliked) false else detail.userActions.like,
                    dislike = disliked
                )
            )
        }
    }

    private suspend fun setCoinState(coined: Boolean) {
        updateDetailOnMain { detail ->
            val oldCoined = detail.userActions.coin
            detail.copy(
                stat = detail.stat.copy(
                    coin = (detail.stat.coin + if (coined && !oldCoined) 1 else 0)
                        .coerceAtLeast(0)
                ),
                userActions = detail.userActions.copy(coin = coined)
            )
        }
    }

    private suspend fun setFavoriteState(favorited: Boolean) {
        updateDetailOnMain { detail ->
            val oldFavorited = detail.userActions.favorite
            val favoriteDelta = when {
                favorited && !oldFavorited -> 1
                !favorited && oldFavorited -> -1
                else -> 0
            }
            detail.copy(
                stat = detail.stat.copy(
                    favorite = (detail.stat.favorite + favoriteDelta).coerceAtLeast(0)
                ),
                userActions = detail.userActions.copy(favorite = favorited)
            )
        }
    }

    suspend fun setInToView(value: Boolean) {
        withContext(Dispatchers.Main) {
            inToView = value
        }
    }

    private suspend fun refreshUpOwnerStats(mid: Long) {
        if (mid <= 0L) return
        runCatching {
            userRepository.getUserCardInfo(mid)
        }.onSuccess { card ->
            withContext(Dispatchers.Main) {
                upOwnerStats = UpOwnerStats(
                    followerCount = card.follower,
                    archiveCount = card.archiveCount
                )
            }
        }.onFailure {
            logger.fInfo { "Load up owner stats failed: ${it.stackTraceToString()}" }
        }
    }

    suspend fun refreshFavoriteFolders(aid: Long = currentAid()): Result<Unit> {
        if (!Prefs.isLogin || aid <= 0L || Prefs.uid <= 0L) return Result.success(Unit)
        return runCatching {
            withContext(Dispatchers.Main) { favoriteFoldersLoading = true }
            val folders = favoriteRepository.getAllFavoriteFolderMetadataList(
                mid = Prefs.uid,
                rid = aid,
                preferApiType = DETAIL_API_TYPE
            )
            favoriteFolders.swapListWithMainContext(folders)
            favoriteFolderIds.swapListWithMainContext(
                folders.filter { it.videoInThisFav }.map { it.id }
            )
            Unit
        }.also {
            withContext(Dispatchers.Main) { favoriteFoldersLoading = false }
        }
    }

    suspend fun toggleLike(): Result<String> = runUserAction {
        val aid = currentAid()
        val liked = videoDetail?.userActions?.like == true
        if (liked) {
            likeRepository.delVideoLike(aid = aid, preferApiType = DETAIL_API_TYPE)
            setLikeState(false)
            "已取消点赞"
        } else {
            likeRepository.addVideoLike(aid = aid, preferApiType = DETAIL_API_TYPE)
            setLikeState(true)
            "点赞成功"
        }
    }

    suspend fun toggleDislike(): Result<String> = runUserAction {
        check(Prefs.accessToken.isNotBlank()) { "点踩需要 App 登录凭证，请重新登录" }
        val aid = currentAid()
        val disliked = videoDetail?.userActions?.dislike == true
        if (disliked) {
            likeRepository.delVideoDislike(aid = aid)
            setDislikeState(false)
            "已取消点踩"
        } else {
            likeRepository.addVideoDislike(aid = aid)
            setDislikeState(true)
            "点踩成功"
        }
    }

    suspend fun addCoin(): Result<String> = runUserAction {
        val aid = currentAid()
        check(videoDetail?.userActions?.coin != true) { "已经投过币了" }
        coinRepository.addVideoCoin(aid = aid, preferApiType = DETAIL_API_TYPE)
        setCoinState(true)
        "投币成功"
    }

    suspend fun updateFavoriteFolders(folderIds: List<Long>): Result<String> = runUserAction {
        val aid = currentAid()
        if (favoriteFolders.isEmpty()) {
            refreshFavoriteFolders(aid).getOrThrow()
        }
        val currentFolders = withContext(Dispatchers.Main) { favoriteFolders.toList() }
        val currentFolderIds = currentFolders.map { it.id }
        favoriteRepository.updateVideoToFavoriteFolder(
            aid = aid,
            addMediaIds = folderIds,
            delMediaIds = currentFolderIds - folderIds.toSet(),
            preferApiType = DETAIL_API_TYPE
        )
        val updatedFolders = currentFolders.map { folder ->
            folder.copy(videoInThisFav = folderIds.contains(folder.id))
        }
        favoriteFolders.swapListWithMainContext(updatedFolders)
        favoriteFolderIds.swapListWithMainContext(folderIds)
        setFavoriteState(folderIds.isNotEmpty())
        if (folderIds.isNotEmpty()) "收藏成功" else "已取消收藏"
    }

    suspend fun addToDefaultFavoriteFolder(): Result<String> = runUserAction {
        val aid = currentAid()
        if (favoriteFolders.isEmpty()) {
            refreshFavoriteFolders(aid).getOrThrow()
        }
        val currentFolders = withContext(Dispatchers.Main) { favoriteFolders.toList() }
        val defaultFolder = currentFolders.firstOrNull { it.title == "默认收藏夹" }
            ?: currentFolders.firstOrNull()
            ?: error("未找到收藏夹")
        favoriteRepository.updateVideoToFavoriteFolder(
            aid = aid,
            addMediaIds = listOf(defaultFolder.id),
            delMediaIds = currentFolders.map { it.id } - setOf(defaultFolder.id),
            preferApiType = DETAIL_API_TYPE
        )
        val folderIds = listOf(defaultFolder.id)
        favoriteFolders.swapListWithMainContext(
            currentFolders.map { it.copy(videoInThisFav = folderIds.contains(it.id)) }
        )
        favoriteFolderIds.swapListWithMainContext(folderIds)
        setFavoriteState(true)
        "已添加到${defaultFolder.title}"
    }

    suspend fun tripleLike(): Result<String> = runUserAction {
        val aid = currentAid()
        val oldActions = videoDetail?.userActions ?: error("视频信息未加载")
        val result = tripleLikeRepository.tripleLike(
            aid = aid,
            preferApiType = DETAIL_API_TYPE
        )
        updateDetailOnMain { detail ->
            detail.copy(
                stat = detail.stat.copy(
                    like = (detail.stat.like + if (result.like && !oldActions.like) 1 else 0)
                        .coerceAtLeast(0),
                    coin = (detail.stat.coin + if (result.coin && !oldActions.coin) 2 else 0)
                        .coerceAtLeast(0),
                    favorite = (detail.stat.favorite + if (result.fav && !oldActions.favorite) 1 else 0)
                        .coerceAtLeast(0)
                ),
                userActions = detail.userActions.copy(
                    like = result.like || detail.userActions.like,
                    coin = result.coin || detail.userActions.coin,
                    favorite = result.fav || detail.userActions.favorite,
                    dislike = false
                )
            )
        }
        refreshFavoriteFolders(aid)
        "三连成功"
    }

    suspend fun toggleToView(): Result<String> = runUserAction {
        val aid = currentAid()
        if (inToView) {
            toViewRepository.deleteToViewOrThrow(aid)
            withContext(Dispatchers.Main) {
                inToView = false
            }
            "已移出稍后再看"
        } else {
            toViewRepository.addToView(
                avid = aid,
                bvid = videoDetail?.bvid?.takeIf { it.isNotBlank() }
            )
            withContext(Dispatchers.Main) {
                inToView = true
            }
            "已添加到稍后再看"
        }
    }
}

enum class VideoInfoState {
    Loading,
    Success,
    Error
}
