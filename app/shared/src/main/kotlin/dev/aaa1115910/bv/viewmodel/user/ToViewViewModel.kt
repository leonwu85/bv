package dev.aaa1115910.bv.viewmodel.user

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.ugc.toSmartDate
import dev.aaa1115910.biliapi.http.entity.AuthFailureException
import dev.aaa1115910.biliapi.repositories.ToViewRepository
import dev.aaa1115910.biliapi.repositories.ToViewCleanType
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.repository.UserRepository
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.addWithMainContext
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.fWarn
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class ToViewViewModel(
    private val userRepository: UserRepository,
    private val ToViewRepository: ToViewRepository
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    var histories = mutableStateListOf<VideoCardData>()
    var noMore by mutableStateOf(false)

    var searchQuery by mutableStateOf("")
        private set
    var selectedSort by mutableStateOf(ToViewSort.Default)
        private set

    val visibleHistories: List<VideoCardData>
        get() {
            val query = searchQuery.trim()
            val filtered = if (query.isEmpty()) histories else histories.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.upName.contains(query, ignoreCase = true) ||
                    it.bvid.contains(query, ignoreCase = true)
            }
            return when (selectedSort) {
                ToViewSort.Default -> filtered
                ToViewSort.LatestPublish -> filtered.sortedByDescending { it.pubTimeTimestamp ?: 0L }
                ToViewSort.MostPlayed -> filtered.sortedByDescending { it.play ?: -1L }
                ToViewSort.Title -> filtered.sortedBy { it.title.lowercase() }
            }
        }

    fun updateSearchQuery(value: String) {
        searchQuery = value
    }

    fun selectSort(value: ToViewSort) {
        selectedSort = value
    }

    private var cursor = 0L
    var updating by mutableStateOf(false)
        private set

    private val loadMutex = Mutex()
    private var loadGeneration = 0

    fun update() {
        viewModelScope.launch(Dispatchers.IO) {
            updateToView()
        }
    }

    private suspend fun updateToView(context: Context = BVApp.context) {
        val generation = loadGeneration
        loadMutex.withLock {
            if (generation != loadGeneration || noMore) return@withLock
            logger.fInfo { "Updating toview with params [cursor=$cursor, apiType=${Prefs.apiType}]" }
            withContext(Dispatchers.Main) {
                if (generation == loadGeneration) updating = true
            }
            try {
                if (generation != loadGeneration) return@withLock
                val existingAids = withContext(Dispatchers.Main) {
                    histories.mapTo(mutableSetOf()) { it.avid }
                }
                val data = ToViewRepository.getToView(
                    cursor = cursor,
                    preferApiType = Prefs.apiType
                )
                if (generation != loadGeneration) return@withLock

                data.data.forEach { toViewItem ->
                    if (generation != loadGeneration) return@forEach
                    if (!existingAids.add(toViewItem.oid)) {
                        logger.fInfo { "Skip duplicated toview item: aid=${toViewItem.oid}" }
                        return@forEach
                    }
                    histories.addWithMainContext(
                        VideoCardData(
                            avid = toViewItem.oid,
                            bvid = toViewItem.bvid,
                            title = toViewItem.title,
                            cover = toViewItem.cover,
                            play = toViewItem.play,
                            pubTime = toViewItem.pubdate.toSmartDate(),
                            pubTimeTimestamp = toViewItem.pubdate,
                            upName = toViewItem.author,
                            upId = toViewItem.authorId,
                            upFace = toViewItem.authorFace,
                            timeString = if (toViewItem.progress == -1) context.getString(R.string.play_time_finish)
                            else context.getString(
                                R.string.play_time_history,
                                (toViewItem.progress * 1000L).formatHourMinSec(),
                                (toViewItem.duration * 1000L).formatHourMinSec()
                            )
                        )
                    )
                }
                if (generation != loadGeneration) return@withLock
                cursor = data.cursor
                logger.fInfo { "Update toview cursor: [cursor=$cursor]" }
                logger.fInfo { "Update toview success" }
                if (cursor == 0L) {
                    withContext(Dispatchers.Main) {
                        if (generation == loadGeneration) noMore = true
                    }
                    logger.fInfo { "No more toview" }
                }
            } catch (it: Throwable) {
                logger.fWarn { "Update toview failed: ${it.stackTraceToString()}" }
                when (it) {
                    is AuthFailureException -> logger.fInfo { "User auth failure" }
                    else -> {}
                }
            } finally {
                withContext(Dispatchers.Main) {
                    if (generation == loadGeneration) updating = false
                }
            }
        }
    }

    var deleting by mutableStateOf(false)
        private set

    var deletePhase by mutableStateOf(0)
        private set

    var pendingFocusIndex by mutableStateOf(-1)
        private set

    fun deleteToView(avid: Long, targetIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { deleting = true }
            runCatching {
                val success = ToViewRepository.deleteToView(avid)
                if (success) {
                    pendingFocusIndex = targetIndex
                    withContext(Dispatchers.Main) {
                        deletePhase = 1
                    }

                    withContext(Dispatchers.Main) {
                        histories.removeAll { it.avid == avid }
                    }

                    withContext(Dispatchers.Main) {
                        deletePhase = 2
                    }

                    logger.fInfo { "Delete toview success: aid=$avid" }
                    withContext(Dispatchers.Main) {
                        BVApp.context.getString(R.string.toview_delete_success)
                            .toast(BVApp.context)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        BVApp.context.getString(R.string.toview_delete_failed)
                            .toast(BVApp.context)
                    }
                }
            }.onFailure {
                logger.fWarn { "Delete toview failed: ${it.stackTraceToString()}" }
                withContext(Dispatchers.Main) {
                    BVApp.context.getString(R.string.toview_delete_failed)
                        .toast(BVApp.context)
                }
            }
            withContext(Dispatchers.Main) {
                deleting = false
            }
        }
    }

    fun resetDeletePhase() {
        deletePhase = 0
        pendingFocusIndex = -1
    }

    var cleaning by mutableStateOf(false)
        private set

    fun clearToView(type: ToViewCleanType) {
        if (cleaning) return
        cleaning = true
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { ToViewRepository.clearToView(type) }
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        clearData()
                        update()
                        val message = when (type) {
                            ToViewCleanType.All -> "稍后再看已清空"
                            ToViewCleanType.Invalid -> "失效内容已清理"
                            ToViewCleanType.Viewed -> "已观看内容已清理"
                        }
                        message.toast(BVApp.context)
                    }
                }
                .onFailure {
                    logger.fWarn { "Clear toview failed: ${it.stackTraceToString()}" }
                    withContext(Dispatchers.Main) {
                        (it.localizedMessage ?: "清理失败").toast(BVApp.context)
                    }
                }
            withContext(Dispatchers.Main) { cleaning = false }
        }
    }

    fun clearData() {
        loadGeneration++
        histories.clear()
        searchQuery = ""
        selectedSort = ToViewSort.Default
        cursor = 0L
        noMore = false
        updating = false
        logger.fInfo { "ToView data cleared" }
    }
}

enum class ToViewSort {
    Default,
    LatestPublish,
    MostPlayed,
    Title
}
