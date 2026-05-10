package dev.aaa1115910.bv.viewmodel.pgc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.season.FollowingSeason
import dev.aaa1115910.biliapi.entity.season.FollowingSeasonStatus
import dev.aaa1115910.biliapi.entity.season.FollowingSeasonType
import dev.aaa1115910.biliapi.entity.season.Timeline
import dev.aaa1115910.biliapi.entity.season.TimelineFilter
import dev.aaa1115910.biliapi.repositories.SeasonRepository
import dev.aaa1115910.bv.repository.UserRepository
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class PgcHomeViewModel(
    private val seasonRepository: SeasonRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger { }
        private const val PageSize = 30
    }

    val followingSeasons = mutableStateListOf<FollowingSeason>()
    val timelines = mutableStateListOf<Timeline>()

    var followingTotal by mutableIntStateOf(-1)
        private set
    var followingPage by mutableIntStateOf(1)
        private set
    var followingLoading by mutableStateOf(false)
        private set
    var followingEnd by mutableStateOf(false)
        private set
    var timelineLoading by mutableStateOf(false)
        private set

    val isLogin get() = userRepository.isLogin

    fun refresh() {
        refreshFollowing()
        loadTimeline()
    }

    fun refreshFollowing() {
        followingPage = 1
        followingEnd = false
        followingSeasons.clear()
        loadMoreFollowing()
    }

    fun loadMoreFollowing() {
        if (!userRepository.isLogin || followingLoading || followingEnd) return

        viewModelScope.launch(Dispatchers.IO) {
            followingLoading = true
            runCatching {
                val response = seasonRepository.getFollowingSeasons(
                    type = FollowingSeasonType.Bangumi,
                    status = FollowingSeasonStatus.All,
                    pageNumber = followingPage,
                    pageSize = PageSize,
                    preferApiType = Prefs.apiType
                )
                withContext(Dispatchers.Main) {
                    followingTotal = response.total
                    followingSeasons.addAll(response.list)
                    followingEnd = followingSeasons.size >= response.total || response.list.isEmpty()
                    followingPage++
                }
            }.onFailure {
                logger.fInfo { "Load following seasons failed: ${it.stackTraceToString()}" }
            }
            withContext(Dispatchers.Main) {
                followingLoading = false
            }
        }
    }

    fun loadTimeline() {
        if (timelineLoading || timelines.isNotEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            timelineLoading = true
            runCatching {
                val anime = async {
                    seasonRepository.getTimeline(
                        filter = TimelineFilter.Anime,
                        preferApiType = Prefs.apiType
                    )
                }
                val guoChuang = async {
                    seasonRepository.getTimeline(
                        filter = TimelineFilter.GuoChuang,
                        preferApiType = Prefs.apiType
                    )
                }
                val merged = mergeTimelines(anime.await(), guoChuang.await())
                withContext(Dispatchers.Main) {
                    timelines.clear()
                    timelines.addAll(merged)
                }
            }.onFailure {
                logger.fInfo { "Load timeline failed: ${it.stackTraceToString()}" }
            }
            withContext(Dispatchers.Main) {
                timelineLoading = false
            }
        }
    }

    private fun mergeTimelines(first: List<Timeline>, second: List<Timeline>): List<Timeline> {
        if (first.isEmpty()) return second
        if (second.isEmpty()) return first

        val secondByDate = second.associateBy { it.dateString }
        return first.map { item ->
            val other = secondByDate[item.dateString]
            if (other == null) {
                item
            } else {
                item.copy(
                    episodes = (item.episodes + other.episodes)
                        .sortedBy { it.publishDate.time }
                        .distinctBy { it.seasonId }
                )
            }
        }
    }
}
