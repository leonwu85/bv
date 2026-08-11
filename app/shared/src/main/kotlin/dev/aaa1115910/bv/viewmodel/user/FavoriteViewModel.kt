package dev.aaa1115910.bv.viewmodel.user

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.FavoriteFolderMetadata
import dev.aaa1115910.biliapi.entity.FavoriteItemType
import dev.aaa1115910.biliapi.entity.ugc.toSmartDate
import dev.aaa1115910.biliapi.repositories.FavoriteRepository
import dev.aaa1115910.bv.BVApp.Companion.context
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.DeviceUtil
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.fWarn
import dev.aaa1115910.bv.util.swapList
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class FavoriteViewModel(
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    var favoriteFolderMetadataList = mutableStateListOf<FavoriteFolderMetadata>()
    var favorites = mutableStateListOf<VideoCardData>()

    var currentFavoriteFolderMetadata: FavoriteFolderMetadata? by mutableStateOf(null)

    var searchQuery by mutableStateOf("")
        private set
    var selectedOrder by mutableStateOf(FavoriteOrder.FavoriteTime)
        private set

    private var pageSize = 20
    private var pageNumber = 1
    private var hasMore = true

    var updatingFolders by mutableStateOf(false)
    var updatingFolderItems by mutableStateOf(false)
    var operating by mutableStateOf(false)
        private set

    init {
        if (!DeviceUtil.isTvDevice()) {
            updateFoldersInfo()
        } else {
            logger.fInfo { "Skip updating favorite folders on TV device" }
        }
    }

    fun updateFoldersInfo(selectedFolderId: Long? = currentFavoriteFolderMetadata?.id) {
        if (updatingFolders) return
        updatingFolders = true
        logger.fInfo { "Updating favorite folders" }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val favoriteFolderMetadataList =
                    favoriteRepository.getAllFavoriteFolderMetadataList(
                        mid = Prefs.uid,
                        preferApiType = Prefs.apiType
                    )
                withContext(Dispatchers.Main) {
                    this@FavoriteViewModel.favoriteFolderMetadataList
                        .swapList(favoriteFolderMetadataList)
                    currentFavoriteFolderMetadata = favoriteFolderMetadataList
                        .firstOrNull { it.id == selectedFolderId }
                        ?: favoriteFolderMetadataList.firstOrNull()
                    updateFolderItems(force = true)
                }
                logger.fInfo { "Update favorite folders success: ${favoriteFolderMetadataList.map { it.id }}" }
            }.onFailure {
                logger.fWarn { "Update favorite folders failed: ${it.stackTraceToString()}" }
                //这里返回的数据并不会有用户认证失败的错误返回，没必要做身份验证失败提示
            }
            withContext(Dispatchers.Main) { updatingFolders = false }
        }
    }

    private var updateJob: Job? = null
    private var filterJob: Job? = null
    private var itemsGeneration = 0

    fun updateSearchQuery(value: String) {
        if (searchQuery == value) return
        searchQuery = value
        filterJob?.cancel()
        if (value.isBlank()) {
            updateFolderItems(force = true)
            return
        }
        filterJob = viewModelScope.launch {
            delay(350)
            updateFolderItems(force = true)
        }
    }

    fun selectOrder(value: FavoriteOrder) {
        if (selectedOrder == value) return
        selectedOrder = value
        updateFolderItems(force = true)
    }

    fun updateFolderItems(force: Boolean = false) {
        if (force) {
            updateJob?.cancel()
            itemsGeneration++
            resetPageNumber()
            updatingFolderItems = false
            favorites.clear()
        }
        if (updatingFolderItems || !hasMore) return
        val folder = currentFavoriteFolderMetadata ?: return
        val generation = itemsGeneration
        updatingFolderItems = true
        logger.fInfo { "Updating favorite folder items with media id: ${folder.id}" }
        updateJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val favoriteFolderData = favoriteRepository.getFavoriteFolderData(
                    mediaId = folder.id,
                    pageSize = pageSize,
                    pageNumber = pageNumber,
                    keyword = searchQuery.trim().takeIf { it.isNotEmpty() },
                    order = selectedOrder.apiValue,
                    preferApiType = Prefs.apiType
                )
                val newItems = favoriteFolderData.medias.mapNotNull { favoriteItem ->
                    if (favoriteItem.type != FavoriteItemType.Video) return@mapNotNull null
                    VideoCardData(
                        avid = favoriteItem.id,
                        bvid = favoriteItem.bvid,
                        title = favoriteItem.title,
                        cover = favoriteItem.cover,
                        play = favoriteItem.cntInfo.play,
                        danmaku = favoriteItem.cntInfo.danmaku,
                        upName = favoriteItem.upper.name,
                        upId = favoriteItem.upper.mid,
                        upFace = favoriteItem.upper.face,
                        time = favoriteItem.duration * 1000L,
                        pubTime = favoriteItem.favTime.toSmartDate() + context.getString(R.string.favorite_at)
                    )
                }
                withContext(Dispatchers.Main) {
                    if (generation != itemsGeneration) return@withContext
                    favorites.addAll(newItems)
                    hasMore = favoriteFolderData.hasMore
                    pageNumber++
                }
                logger.fInfo { "Update favorite items success" }
            } catch (it: Throwable) {
                logger.fInfo { "Update favorite items failed: ${it.stackTraceToString()}" }
            } finally {
                withContext(Dispatchers.Main) {
                    if (generation == itemsGeneration) updatingFolderItems = false
                }
            }
        }
    }

    fun removeFavorite(video: VideoCardData) {
        val folder = currentFavoriteFolderMetadata ?: return
        runOperation("已从收藏夹移除") {
            favoriteRepository.delVideoFromFavoriteFolder(
                aid = video.avid,
                delMediaIds = listOf(folder.id),
                preferApiType = Prefs.apiType
            )
            withContext(Dispatchers.Main) {
                favorites.removeAll { it.avid == video.avid }
                val index = favoriteFolderMetadataList.indexOfFirst { it.id == folder.id }
                if (index >= 0) {
                    favoriteFolderMetadataList[index] = folder.copy(
                        mediaCount = (folder.mediaCount - 1).coerceAtLeast(0)
                    ).also { currentFavoriteFolderMetadata = it }
                }
            }
        }
    }

    fun addFolder(title: String, isPublic: Boolean) {
        runOperation("收藏夹已创建") {
            favoriteRepository.addFavoriteFolder(title = title, isPublic = isPublic)
            withContext(Dispatchers.Main) { updateFoldersInfo() }
        }
    }

    fun editCurrentFolder(title: String, isPublic: Boolean) {
        val folder = currentFavoriteFolderMetadata ?: return
        if (folder.isDefault) return
        runOperation("收藏夹已更新") {
            val details = favoriteRepository.getFavoriteFolderInfo(folder.id)
            favoriteRepository.editFavoriteFolder(
                mediaId = folder.id,
                title = title,
                intro = details.intro,
                cover = details.cover,
                isPublic = isPublic
            )
            withContext(Dispatchers.Main) { updateFoldersInfo(folder.id) }
        }
    }

    fun deleteCurrentFolder() {
        val folder = currentFavoriteFolderMetadata ?: return
        if (folder.isDefault) return
        runOperation("收藏夹已删除") {
            favoriteRepository.deleteFavoriteFolder(folder.id)
            withContext(Dispatchers.Main) { updateFoldersInfo(selectedFolderId = -1) }
        }
    }

    fun cleanCurrentFolder() {
        val folder = currentFavoriteFolderMetadata ?: return
        runOperation("失效内容已清理") {
            favoriteRepository.cleanFavoriteFolder(folder.id)
            withContext(Dispatchers.Main) { updateFoldersInfo(folder.id) }
        }
    }

    private fun runOperation(
        successMessage: String,
        action: suspend () -> Unit
    ) {
        if (operating) return
        operating = true
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { action() }
                .onSuccess {
                    withContext(Dispatchers.Main) { successMessage.toast(context) }
                }
                .onFailure {
                    logger.fWarn { "Favorite operation failed: ${it.stackTraceToString()}" }
                    withContext(Dispatchers.Main) {
                        (it.localizedMessage ?: "操作失败").toast(context)
                    }
                }
            withContext(Dispatchers.Main) { operating = false }
        }
    }

    fun resetPageNumber() {
        pageNumber = 1
        hasMore = true
    }

    fun clearData() {
        updateJob?.cancel()
        filterJob?.cancel()
        itemsGeneration++
        favorites.clear()
        resetPageNumber()
        logger.fInfo { "Favorite data cleared" }
    }
}

enum class FavoriteOrder(val apiValue: String) {
    FavoriteTime("mtime"),
    MostPlayed("view"),
    PublishTime("pubtime")
}
