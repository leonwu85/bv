package dev.aaa1115910.bv.viewmodel.user

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.ugc.toSmartDate
import dev.aaa1115910.biliapi.entity.user.HistoryItemType
import dev.aaa1115910.biliapi.http.entity.AuthFailureException
import dev.aaa1115910.biliapi.repositories.HistoryRepository
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
class HistoryViewModel(
    private val userRepository: UserRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    var histories = mutableStateListOf<VideoCardData>()
    var noMore by mutableStateOf(false)

    private var cursor = 0L
    var updating by mutableStateOf(false)
        private set

    private val loadMutex = Mutex()
    /** clearData 时递增，丢弃进行中的旧请求结果 */
    private var loadGeneration = 0

    fun update() {
        viewModelScope.launch(Dispatchers.IO) {
            updateHistories()
        }
    }

    private suspend fun updateHistories(context: Context = BVApp.context) {
        val generation = loadGeneration
        loadMutex.withLock {
            if (generation != loadGeneration || noMore) return@withLock
            logger.fInfo { "Updating histories with params [cursor=$cursor, apiType=${Prefs.apiType}]" }
            withContext(Dispatchers.Main) {
                if (generation == loadGeneration) updating = true
            }
            try {
                if (generation != loadGeneration) return@withLock
                val existingKeys = withContext(Dispatchers.Main) {
                    histories.mapTo(mutableSetOf()) { historyCardKey(it) }
                }
                val data = historyRepository.getHistories(
                    cursor = cursor,
                    preferApiType = Prefs.apiType
                )
                if (generation != loadGeneration) return@withLock

                data.data.forEach { historyItem ->
                    if (generation != loadGeneration) return@forEach
                    val isPgc = historyItem.type == HistoryItemType.Pgc
                    val itemKey = historyItemKey(historyItem)
                    if (!existingKeys.add(itemKey)) {
                        logger.fInfo { "Skip duplicated history item: $itemKey" }
                        return@forEach
                    }
                    histories.addWithMainContext(
                        VideoCardData(
                            avid = historyItem.oid,
                            bvid = historyItem.bvid,
                            title = historyItem.title,
                            cover = historyItem.cover,
                            upName = historyItem.author,
                            upId = historyItem.authorId,
                            upFace = historyItem.authorFace,
                            timeString = if (historyItem.progress == -1) context.getString(R.string.play_time_finish)
                            else context.getString(
                                R.string.play_time_history,
                                (historyItem.progress * 1000L).formatHourMinSec(),
                                (historyItem.duration * 1000L).formatHourMinSec()
                            ),
                            jumpToSeason = isPgc,
                            epId = historyItem.epid,
                            seasonId = historyItem.seasonId ?: if (isPgc) historyItem.kid.toInt() else null,
                            pubTime = historyItem.viewAt.toSmartDate() + context.getString(R.string.view_at),
                            historyViewAt = historyItem.viewAt,
                            historyBusiness = when (historyItem.type) {
                                HistoryItemType.Archive -> "archive"
                                HistoryItemType.Pgc -> "pgc"
                                HistoryItemType.Unknown -> null
                            },
                            historyKid = historyItem.kid
                        )
                    )
                }
                if (generation != loadGeneration) return@withLock
                cursor = data.cursor
                logger.fInfo { "Update history cursor: [cursor=$cursor]" }
                logger.fInfo { "Update histories success" }
                if (cursor == 0L) {
                    withContext(Dispatchers.Main) {
                        if (generation == loadGeneration) noMore = true
                    }
                    logger.fInfo { "No more history" }
                }
            } catch (it: Throwable) {
                logger.fWarn { "Update histories failed: ${it.stackTraceToString()}" }
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

    fun deleteHistory(history: VideoCardData, targetIndex: Int) {
        val business = history.historyBusiness ?: return
        val kid = history.historyKid ?: return
        val viewAt = history.historyViewAt

        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                deleting = true
            }
            runCatching {
                val success = historyRepository.deleteHistory(
                    business = business,
                    kid = kid
                )
                if (success) {
                    withContext(Dispatchers.Main) {
                        pendingFocusIndex = targetIndex
                        deletePhase = 1
                    }

                    withContext(Dispatchers.Main) {
                        val removeIndex = histories.indexOfFirst {
                            it.historyBusiness == business &&
                                it.historyKid == kid &&
                                (viewAt == null || it.historyViewAt == viewAt)
                        }
                        if (removeIndex >= 0) histories.removeAt(removeIndex)
                    }

                    withContext(Dispatchers.Main) {
                        deletePhase = 2
                    }

                    logger.fInfo { "Delete history success: business=$business, kid=$kid" }
                    withContext(Dispatchers.Main) {
                        BVApp.context.getString(R.string.history_delete_success)
                            .toast(BVApp.context)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        BVApp.context.getString(R.string.history_delete_failed)
                            .toast(BVApp.context)
                    }
                }
            }.onFailure {
                logger.fWarn { "Delete history failed: ${it.stackTraceToString()}" }
                withContext(Dispatchers.Main) {
                    BVApp.context.getString(R.string.history_delete_failed)
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

    private fun historyItemKey(item: dev.aaa1115910.biliapi.entity.user.HistoryItem): String {
        return "${historyBusiness(item.type)}_${item.kid}_${item.viewAt}_${item.oid}"
    }

    private fun historyCardKey(item: VideoCardData): String {
        return "${item.historyBusiness}_${item.historyKid ?: item.avid}_${item.historyViewAt ?: 0L}_${item.avid}"
    }

    private fun historyBusiness(type: HistoryItemType): String? {
        return when (type) {
            HistoryItemType.Archive -> "archive"
            HistoryItemType.Pgc -> "pgc"
            HistoryItemType.Unknown -> null
        }
    }

    fun clearData() {
        loadGeneration++
        histories.clear()
        cursor = 0L
        noMore = false
        updating = false
        logger.fInfo { "History data cleared" }
    }
}
