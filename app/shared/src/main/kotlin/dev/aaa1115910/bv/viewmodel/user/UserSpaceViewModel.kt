package dev.aaa1115910.bv.viewmodel.user

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.season.FollowingSeason
import dev.aaa1115910.biliapi.entity.season.FollowingSeasonStatus
import dev.aaa1115910.biliapi.entity.season.FollowingSeasonType
import dev.aaa1115910.biliapi.entity.ugc.toSmartDate
import dev.aaa1115910.biliapi.entity.user.DynamicItem
import dev.aaa1115910.biliapi.entity.user.SpaceVideoPage
import dev.aaa1115910.biliapi.entity.user.SpaceVideo
import dev.aaa1115910.biliapi.http.entity.user.AppUserSpaceData
import dev.aaa1115910.biliapi.http.entity.user.AppUserSpaceElecUser
import dev.aaa1115910.biliapi.http.entity.user.AppUserSpaceImages
import dev.aaa1115910.biliapi.http.entity.user.AppUserSpaceTag
import dev.aaa1115910.biliapi.http.entity.user.favorite.SpaceFavoriteData
import dev.aaa1115910.biliapi.http.entity.user.favorite.SpaceFavoriteMediaListResponse
import dev.aaa1115910.biliapi.repositories.FavoriteRepository
import dev.aaa1115910.biliapi.repositories.SeasonRepository
import dev.aaa1115910.biliapi.http.entity.user.UserCardData
import dev.aaa1115910.biliapi.http.entity.user.UserInfoData
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

enum class UserSpaceTab(val title: String) {
    Home("主页"),
    Dynamic("动态"),
    Video("投稿"),
    Favorite("收藏"),
    Bangumi("追番")
}

@KoinViewModel
class UserSpaceViewModel(
    private val userRepository: UserRepository,
    private val favoriteRepository: FavoriteRepository,
    private val seasonRepository: SeasonRepository
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    var upFace by mutableStateOf("")
    var upName by mutableStateOf("")
    var sign by mutableStateOf("") // 个性签名
    var fans by mutableIntStateOf(0) // 粉丝数
    var friend by mutableIntStateOf(0) // 关注数
    var upMid by mutableLongStateOf(0L)
    var tvSpaceVideos = mutableStateListOf<VideoCardData>()
    var spaceVideos = mutableStateListOf<SpaceVideo>()
    val dynamicItems = mutableStateListOf<DynamicItem>()
    val favoriteGroups = mutableStateListOf<SpaceFavoriteData>()
    val followingSeasons = mutableStateListOf<FollowingSeason>()

    var userInfo by mutableStateOf<UserInfoData?>(null)
    var userCardInfo by mutableStateOf<UserCardData?>(null)
    var appSpaceData by mutableStateOf<AppUserSpaceData?>(null)
    var appSpaceImages by mutableStateOf<AppUserSpaceImages?>(null)
    var profileLoading by mutableStateOf(false)
    var profileError by mutableStateOf<String?>(null)
    var selectedTab by mutableStateOf(UserSpaceTab.Home)
    var isFollowing by mutableStateOf<Boolean?>(null)
    var relationLoading by mutableStateOf(false)
    var videoLoading by mutableStateOf(false)
    var dynamicLoading by mutableStateOf(false)
    var favoriteLoading by mutableStateOf(false)
    var favoriteError by mutableStateOf<String?>(null)
    var followingSeasonType by mutableStateOf(FollowingSeasonType.Bangumi)
    var bangumiLoading by mutableStateOf(false)
    var bangumiHasMore by mutableStateOf(true)
    var bangumiError by mutableStateOf<String?>(null)
    var dynamicHasMore by mutableStateOf(true)
    var archiveCount by mutableIntStateOf(0)
    var articleCount by mutableIntStateOf(0)
    var likeCount by mutableIntStateOf(0)
    var appRelation by mutableIntStateOf(0)
    var isFollowedByUp by mutableStateOf<Int?>(null)

    private var page = SpaceVideoPage()
    private var updatingVideo = false
    private var currentDynamicPage = 0
    private var dynamicHistoryOffset: String? = null
    private var dynamicUpdateBaseline: String? = null
    private var bangumiPage = 1
    private val favoriteGroupKinds = mutableMapOf<Int, FavoriteGroupKind>()
    private val favoriteGroupPages = mutableMapOf<Int, Int>()
    private val favoriteGroupEnd = mutableStateMapOf<Int, Boolean>()
    private val favoriteGroupLoading = mutableStateListOf<Int>()
    private val expandedFavoriteGroupIds = mutableStateListOf<Int>()
    val noMore get() = !page.hasNext
    val availableTabs: List<UserSpaceTab>
        get() = buildList {
            add(UserSpaceTab.Home)
            add(UserSpaceTab.Dynamic)
            add(UserSpaceTab.Video)
            add(UserSpaceTab.Favorite)
            add(UserSpaceTab.Bangumi)
        }

    val topPhoto: String
        get() = appSpaceTopImageUrl
            ?: listOf(
                userCardInfo?.space?.lImg,
                userCardInfo?.space?.sImg,
                userInfo?.topPhoto
            ).firstNotNullOfOrNull { it?.normalizedImageUrl()?.takeIf(String::isNotBlank) }.orEmpty()

    val topPhotoAlignmentY: Float
        get() = appSpaceImages
            ?.collectionTopSimple
            ?.top
            ?.result
            ?.firstOrNull()
            ?.alignmentY ?: 0f

    private val appSpaceTopImageUrl: String?
        get() {
            val collectionTop = appSpaceImages
                ?.collectionTopSimple
                ?.top
                ?.result
                ?.firstOrNull()
                ?.header
                ?.normalizedImageUrl()
                ?.takeIf(String::isNotBlank)
            return collectionTop
                ?: appSpaceImages?.imgUrl?.normalizedImageUrl()?.takeIf(String::isNotBlank)
                ?: appSpaceImages?.nightImgUrl?.normalizedImageUrl()?.takeIf(String::isNotBlank)
        }

    val liveRoom: UserInfoData.LiveRoom?
        get() = userInfo?.live_room

    val displayUid: String
        get() = userCardInfo?.card?.mid
            ?: userInfo?.mid?.toString()
            ?: appSpaceData?.card?.mid
            ?: upMid.toString()

    val visibleSpaceTags: List<AppUserSpaceTag>
        get() = appSpaceData
            ?.card
            ?.spaceTags
            .orEmpty()
            .filter { tag ->
                tag.title?.isNotBlank() == true && tag.type in setOf("location", "real_name")
            }

    val chargeTotal: Int
        get() = appSpaceData?.elec?.total ?: 0

    val chargeUsers: List<AppUserSpaceElecUser>
        get() = appSpaceData?.elec?.list.orEmpty()

    val isBlacklisted: Boolean
        get() = appRelation == 128

    fun update() {
        loadMoreVideos()
    }

    fun initialize(mid: Long, name: String, face: String = "") {
        if (upMid == mid && (userInfo != null || userCardInfo != null || profileLoading)) return
        upMid = mid
        upName = name
        upFace = face
        resetSpace()
        viewModelScope.launch(Dispatchers.IO) {
            loadProfile()
            loadMoreForSelectedTabInternal()
        }
    }

    fun refresh() {
        resetSpace(keepProfile = false)
        viewModelScope.launch(Dispatchers.IO) {
            loadProfile()
            loadMoreForSelectedTabInternal()
        }
    }

    fun refreshSelectedTab() {
        resetSelectedTab(selectedTab)
        viewModelScope.launch(Dispatchers.IO) {
            loadMoreForSelectedTabInternal()
        }
    }

    fun selectTab(tab: UserSpaceTab) {
        selectedTab = tab
        viewModelScope.launch(Dispatchers.Default) {
            loadMoreForSelectedTabInternal()
        }
    }

    fun loadMoreForSelectedTab() {
        viewModelScope.launch(Dispatchers.Default) {
            loadMoreForSelectedTabInternal()
        }
    }

    fun selectFollowingSeasonType(type: FollowingSeasonType) {
        if (followingSeasonType == type) return
        followingSeasonType = type
        followingSeasons.clear()
        bangumiPage = 1
        bangumiHasMore = true
        bangumiError = null
        loadMoreForSelectedTab()
    }

    fun toggleFavoriteGroup(group: SpaceFavoriteData) {
        val key = favoriteGroupKey(group)
        if (key < 0) return
        if (expandedFavoriteGroupIds.contains(key)) {
            expandedFavoriteGroupIds.remove(key)
        } else {
            expandedFavoriteGroupIds.add(key)
        }
    }

    fun isFavoriteGroupExpanded(group: SpaceFavoriteData): Boolean {
        val key = favoriteGroupKey(group)
        return key >= 0 && expandedFavoriteGroupIds.contains(key)
    }

    fun isFavoriteGroupEnd(group: SpaceFavoriteData): Boolean {
        val key = favoriteGroupKey(group)
        return key < 0 || favoriteGroupEnd[key] == true
    }

    fun isFavoriteGroupLoading(group: SpaceFavoriteData): Boolean {
        val key = favoriteGroupKey(group)
        return key >= 0 && favoriteGroupLoading.contains(key)
    }

    fun loadMoreFavoriteGroup(group: SpaceFavoriteData) {
        val index = favoriteGroups.indexOf(group)
        if (index < 0 || upMid <= 0L) return
        val key = favoriteGroupKey(index, group)
        if (favoriteGroupLoading.contains(key) || favoriteGroupEnd[key] == true) return
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                favoriteGroupLoading.add(key)
            }
            runCatching {
                val pageNumber = favoriteGroupPages[key] ?: 2
                val result = when (favoriteGroupKinds[key]) {
                    FavoriteGroupKind.Created -> favoriteRepository.getCreatedFavoriteFolderPage(
                        mid = upMid,
                        pageNumber = pageNumber
                    )

                    FavoriteGroupKind.Collected -> favoriteRepository.getCollectedFavoriteFolderPage(
                        mid = upMid,
                        pageNumber = pageNumber
                    )

                    null -> return@runCatching
                }
                withContext(Dispatchers.Main) {
                    val currentGroup = favoriteGroups.getOrNull(index) ?: return@withContext
                    val currentResponse = currentGroup.mediaListResponse ?: SpaceFavoriteMediaListResponse()
                    val mergedList = currentResponse.list + result.list
                    val count = result.count.takeIf { it > 0 } ?: currentResponse.count
                    favoriteGroups[index] = currentGroup.copy(
                        mediaListResponse = currentResponse.copy(
                            count = count,
                            list = mergedList
                        )
                    )
                    favoriteGroupPages[key] = pageNumber + 1
                    favoriteGroupEnd[key] = !result.hasMore || mergedList.size >= count
                }
            }.onFailure {
                logger.fInfo { "Load more user space favorite group failed: ${it.stackTraceToString()}" }
                withContext(Dispatchers.Main) {
                    favoriteError = it.localizedMessage ?: "加载收藏夹失败"
                }
            }
            withContext(Dispatchers.Main) {
                favoriteGroupLoading.remove(key)
            }
        }
    }

    fun toggleFollow(afterModify: (followed: Boolean, success: Boolean) -> Unit = { _, _ -> }) {
        if (relationLoading || upMid <= 0L) return
        viewModelScope.launch(Dispatchers.IO) {
            relationLoading = true
            val targetFollow = isFollowing != true
            val success = runCatching {
                if (targetFollow) {
                    userRepository.followUser(upMid, Prefs.apiType)
                } else {
                    userRepository.unfollowUser(upMid, Prefs.apiType)
                }
            }.getOrDefault(false)
            withContext(Dispatchers.Main) {
                if (success) {
                    isFollowing = targetFollow
                    userCardInfo = userCardInfo?.copy(following = targetFollow)
                    userInfo = userInfo?.copy(isFollowed = targetFollow)
                    appRelation = if (targetFollow) 2 else 0
                }
                relationLoading = false
                afterModify(targetFollow, success)
            }
        }
    }

    fun toggleBlacklist(afterModify: (blocked: Boolean, success: Boolean) -> Unit = { _, _ -> }) {
        if (relationLoading || upMid <= 0L) return
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                relationLoading = true
            }
            val targetBlocked = !isBlacklisted
            val success = runCatching {
                if (targetBlocked) {
                    userRepository.blacklistUser(upMid, Prefs.apiType)
                } else {
                    userRepository.unblacklistUser(upMid, Prefs.apiType)
                }
            }.getOrDefault(false)
            withContext(Dispatchers.Main) {
                if (success) {
                    appRelation = if (targetBlocked) 128 else 0
                    if (targetBlocked) {
                        isFollowing = false
                        userCardInfo = userCardInfo?.copy(following = false)
                        userInfo = userInfo?.copy(isFollowed = false)
                    }
                }
                relationLoading = false
                afterModify(targetBlocked, success)
            }
        }
    }

    fun removeFan(afterModify: (success: Boolean) -> Unit = {}) {
        if (relationLoading || upMid <= 0L) return
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                relationLoading = true
            }
            val success = runCatching {
                userRepository.removeFan(upMid, Prefs.apiType)
            }.getOrDefault(false)
            withContext(Dispatchers.Main) {
                if (success) {
                    isFollowedByUp = null
                    if (appRelation == 4) appRelation = 2
                }
                relationLoading = false
                afterModify(success)
            }
        }
    }

    private suspend fun loadMoreForSelectedTabInternal() {
        when (selectedTab) {
            UserSpaceTab.Home -> {
                if (dynamicItems.isEmpty()) loadMoreDynamicInternal()
                if (spaceVideos.isEmpty()) loadMoreVideosInternal()
            }

            UserSpaceTab.Dynamic -> loadMoreDynamicInternal()
            UserSpaceTab.Video -> loadMoreVideosInternal()
            UserSpaceTab.Favorite -> loadFavoriteGroupsInternal()
            UserSpaceTab.Bangumi -> loadMoreBangumiInternal()
        }
    }

    private fun loadMoreVideos() {
        viewModelScope.launch(Dispatchers.Default) {
            loadMoreVideosInternal()
        }
    }

    private suspend fun loadProfile() {
        if (profileLoading || upMid <= 0L) return
        profileLoading = true
        profileError = null
        runCatching {
            val cardInfo = userRepository.getUserCardInfo(upMid, photo = true)
            val info = runCatching { userRepository.getUserInfo(upMid) }.getOrNull()
            val appSpace = runCatching { userRepository.getAppUserSpace(upMid) }.getOrNull()
            withContext(Dispatchers.Main) {
                userCardInfo = cardInfo
                userInfo = info
                appSpaceData = appSpace
                appSpaceImages = appSpace?.images
                upName = cardInfo.card.name
                upFace = cardInfo.card.face
                sign = cardInfo.card.sign.replace("\n", " ")
                fans = cardInfo.card.fans
                friend = cardInfo.card.attention
                archiveCount = cardInfo.archiveCount
                articleCount = cardInfo.articleCount
                likeCount = cardInfo.likeNum
                isFollowing = info?.isFollowed ?: cardInfo.following
                appRelation = appSpace?.resolvedRelation ?: if (isFollowing == true) 2 else 0
                isFollowedByUp = appSpace?.card?.relation?.isFollowed
            }
            logger.fInfo {
                "User space top photo: app=${appSpace?.images?.imgUrl}, " +
                        "appCollection=${appSpace?.images?.collectionTopSimple?.top?.result?.firstOrNull()?.header}, " +
                        "cardLarge=${cardInfo.space?.lImg}, cardSmall=${cardInfo.space?.sImg}, info=${info?.topPhoto}"
            }
        }.onFailure {
            logger.fInfo { "Load up profile failed: ${it.stackTraceToString()}" }
            withContext(Dispatchers.Main) {
                profileError = it.localizedMessage ?: "加载用户资料失败"
            }
        }
        withContext(Dispatchers.Main) {
            profileLoading = false
        }
    }

    private suspend fun loadMoreVideosInternal() {
        if (updatingVideo || noMore || upMid <= 0L) return
        logger.fInfo { "Updating up [mid=$upMid] space videos from page $page" }
        updatingVideo = true
        videoLoading = true
        runCatching {
            val spaceVideoData = userRepository.getSpaceVideos(
                mid = upMid,
                page = page,
                preferApiType = Prefs.apiType
            )
            withContext(Dispatchers.Main) {
                spaceVideos.addAll(spaceVideoData.videos)
                spaceVideoData.videos.forEach { spaceVideoItem ->
                    tvSpaceVideos.add(
                        spaceVideoItem.toVideoCardData()
                    )
                }
                page = spaceVideoData.page
            }
            logger.fInfo { "Update up space videos success" }
        }.onFailure {
            logger.fInfo { "Update up space videos failed: ${it.stackTraceToString()}" }
        }
        withContext(Dispatchers.Main) {
            updatingVideo = false
            videoLoading = false
        }
    }

    private suspend fun loadMoreDynamicInternal() {
        if (dynamicLoading || !dynamicHasMore || upMid <= 0L) return
        dynamicLoading = true
        runCatching {
            val dynamicData = userRepository.getDynamicsByUp(
                mid = upMid,
                page = ++currentDynamicPage,
                offset = dynamicHistoryOffset.orEmpty(),
                updateBaseline = dynamicUpdateBaseline.orEmpty(),
                preferApiType = Prefs.apiType
            )
            withContext(Dispatchers.Main) {
                dynamicItems.addAll(dynamicData.dynamics)
                dynamicHistoryOffset = dynamicData.historyOffset
                dynamicUpdateBaseline = dynamicData.updateBaseline
                dynamicHasMore = dynamicData.hasMore
            }
        }.onFailure {
            if (it.message?.contains("4101132") == true || it.message == "请求数据发生错误，请刷新或稍后重试") {
                withContext(Dispatchers.Main) {
                    dynamicHasMore = false
                }
            } else {
                logger.fInfo { "Update up dynamics failed: ${it.stackTraceToString()}" }
            }
        }
        withContext(Dispatchers.Main) {
            dynamicLoading = false
        }
    }

    private suspend fun loadFavoriteGroupsInternal() {
        if (favoriteLoading || favoriteGroups.isNotEmpty() || upMid <= 0L) return
        favoriteLoading = true
        favoriteError = null
        runCatching {
            favoriteRepository.getSpaceFavoriteGroups(upMid)
        }.onSuccess { groups ->
            withContext(Dispatchers.Main) {
                favoriteGroups.clear()
                favoriteGroups.addAll(groups)
                favoriteGroupKinds.clear()
                favoriteGroupPages.clear()
                favoriteGroupEnd.clear()
                favoriteGroupLoading.clear()
                expandedFavoriteGroupIds.clear()
                groups.forEachIndexed { index, group ->
                    val key = favoriteGroupKey(index, group)
                    favoriteGroupKinds[key] = if (index == 0) FavoriteGroupKind.Created else FavoriteGroupKind.Collected
                    favoriteGroupPages[key] = 2
                    favoriteGroupEnd[key] = (group.mediaListResponse?.list?.size ?: 0) >=
                            (group.mediaListResponse?.count ?: 0)
                    expandedFavoriteGroupIds.add(key)
                }
            }
        }.onFailure {
            logger.fInfo { "Load user space favorite groups failed: ${it.stackTraceToString()}" }
            withContext(Dispatchers.Main) {
                favoriteError = it.localizedMessage ?: "加载收藏夹失败"
            }
        }
        withContext(Dispatchers.Main) {
            favoriteLoading = false
        }
    }

    private suspend fun loadMoreBangumiInternal() {
        if (bangumiLoading || !bangumiHasMore || upMid <= 0L) return
        bangumiLoading = true
        bangumiError = null
        runCatching {
            seasonRepository.getUserFollowingSeasons(
                mid = upMid,
                type = followingSeasonType,
                status = FollowingSeasonStatus.All,
                pageNumber = bangumiPage,
                pageSize = 30
            )
        }.onSuccess { data ->
            withContext(Dispatchers.Main) {
                followingSeasons.addAll(data.list)
                bangumiPage++
                bangumiHasMore = data.list.isNotEmpty() && followingSeasons.size < data.total
            }
        }.onFailure {
            logger.fInfo { "Load user space following seasons failed: ${it.stackTraceToString()}" }
            withContext(Dispatchers.Main) {
                bangumiError = it.localizedMessage ?: "加载追番失败"
                bangumiHasMore = false
            }
        }
        withContext(Dispatchers.Main) {
            bangumiLoading = false
        }
    }

    private fun resetSpace(keepProfile: Boolean = true) {
        if (!keepProfile) {
            userInfo = null
            userCardInfo = null
            appSpaceData = null
            appSpaceImages = null
            profileError = null
            archiveCount = 0
            articleCount = 0
            likeCount = 0
            appRelation = 0
            isFollowedByUp = null
        }
        tvSpaceVideos.clear()
        spaceVideos.clear()
        dynamicItems.clear()
        page = SpaceVideoPage()
        updatingVideo = false
        videoLoading = false
        currentDynamicPage = 0
        dynamicHistoryOffset = null
        dynamicUpdateBaseline = null
        dynamicHasMore = true
        dynamicLoading = false
        favoriteGroups.clear()
        favoriteLoading = false
        favoriteError = null
        favoriteGroupKinds.clear()
        favoriteGroupPages.clear()
        favoriteGroupEnd.clear()
        favoriteGroupLoading.clear()
        expandedFavoriteGroupIds.clear()
        followingSeasons.clear()
        bangumiPage = 1
        bangumiHasMore = true
        bangumiLoading = false
        bangumiError = null
    }

    private fun resetSelectedTab(tab: UserSpaceTab) {
        when (tab) {
            UserSpaceTab.Home -> {
                resetDynamic()
                resetVideos()
            }

            UserSpaceTab.Dynamic -> resetDynamic()
            UserSpaceTab.Video -> resetVideos()
            UserSpaceTab.Favorite -> resetFavorites()
            UserSpaceTab.Bangumi -> resetBangumi()
        }
    }

    private fun resetVideos() {
        tvSpaceVideos.clear()
        spaceVideos.clear()
        page = SpaceVideoPage()
        updatingVideo = false
        videoLoading = false
    }

    private fun resetDynamic() {
        dynamicItems.clear()
        currentDynamicPage = 0
        dynamicHistoryOffset = null
        dynamicUpdateBaseline = null
        dynamicHasMore = true
        dynamicLoading = false
    }

    private fun resetFavorites() {
        favoriteGroups.clear()
        favoriteLoading = false
        favoriteError = null
        favoriteGroupKinds.clear()
        favoriteGroupPages.clear()
        favoriteGroupEnd.clear()
        favoriteGroupLoading.clear()
        expandedFavoriteGroupIds.clear()
    }

    private fun resetBangumi() {
        followingSeasons.clear()
        bangumiPage = 1
        bangumiHasMore = true
        bangumiLoading = false
        bangumiError = null
    }

    private fun favoriteGroupKey(group: SpaceFavoriteData): Int {
        val index = favoriteGroups.indexOf(group)
        return if (index >= 0) favoriteGroupKey(index, group) else group.id ?: -1
    }

    private fun favoriteGroupKey(index: Int, group: SpaceFavoriteData): Int {
        return group.id ?: index
    }
}

private enum class FavoriteGroupKind {
    Created,
    Collected
}

private fun String.normalizedImageUrl(): String {
    return trim().let { url ->
        when {
            url.startsWith("//") -> "https:$url"
            url.startsWith("http://") -> url.replaceFirst("http://", "https://")
            url.startsWith("bfs/") -> "https://i0.hdslb.com/$url"
            else -> url
        }
    }
}

fun SpaceVideo.toVideoCardData(): VideoCardData {
    return VideoCardData(
        avid = aid,
        bvid = bvid,
        title = title,
        cover = cover,
        play = play,
        danmaku = danmaku,
        upName = author,
        upId = authorId,
        time = duration * 1000L,
        pubTime = publishDate.getTime().toSmartDate(),
        isInteractive = isInteractive,
        isChargingArc = isChargingArc,
        badgeText = chargingArcBadge
    )
}
