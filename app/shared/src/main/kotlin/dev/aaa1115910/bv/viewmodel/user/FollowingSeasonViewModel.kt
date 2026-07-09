package dev.aaa1115910.bv.viewmodel.user

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.season.FollowingSeason
import dev.aaa1115910.biliapi.entity.season.FollowingSeasonStatus
import dev.aaa1115910.biliapi.entity.season.FollowingSeasonType
import dev.aaa1115910.biliapi.repositories.SeasonRepository
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class FollowingSeasonViewModel(
    private val seasonRepository: SeasonRepository
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    val followingSeasons = mutableStateListOf<FollowingSeason>()
    var followingSeasonType by mutableStateOf(FollowingSeasonType.Bangumi)
    var followingSeasonStatus by mutableStateOf(FollowingSeasonStatus.All)

    private var pageNumber = 1
    private var pageSize = 30
    var noMore by mutableStateOf(false)
    var updating by mutableStateOf(false)
        private set

    private val loadMutex = Mutex()
    private var loadGeneration = 0

    fun clearData() {
        loadGeneration++
        pageNumber = 1
        pageSize = 30
        updating = false
        noMore = false
        followingSeasons.clear()
    }

    fun loadMore() {
        viewModelScope.launch(Dispatchers.IO) {
            updateData()
        }
    }

    private suspend fun updateData() {
        val generation = loadGeneration
        loadMutex.withLock {
            if (generation != loadGeneration || noMore) return@withLock
            withContext(Dispatchers.Main) {
                if (generation == loadGeneration) updating = true
            }
            try {
                if (generation != loadGeneration) return@withLock
                logger.fInfo { "Updating following season data page=$pageNumber" }
                val existingIds = withContext(Dispatchers.Main) {
                    followingSeasons.mapTo(mutableSetOf()) { it.seasonId }
                }
                val response = seasonRepository.getFollowingSeasons(
                    type = followingSeasonType,
                    status = followingSeasonStatus,
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    preferApiType = Prefs.apiType
                )
                if (generation != loadGeneration) return@withLock

                val uniqueNew = response.list.filter { existingIds.add(it.seasonId) }
                if (uniqueNew.size < response.list.size) {
                    logger.fInfo {
                        "Skip ${response.list.size - uniqueNew.size} duplicated following seasons"
                    }
                }

                withContext(Dispatchers.Main) {
                    if (generation != loadGeneration) return@withContext
                    if (pageSize * pageNumber >= response.total) noMore = true
                    pageNumber++
                    followingSeasons.addAll(uniqueNew)
                }
                logger.fInfo { "Following season added: ${uniqueNew.size}, total: ${followingSeasons.size}" }
            } catch (it: Throwable) {
                logger.fInfo { "Update following seasons failed: ${it.stackTraceToString()}" }
            } finally {
                withContext(Dispatchers.Main) {
                    if (generation == loadGeneration) updating = false
                }
            }
        }
    }
}
