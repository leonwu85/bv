package dev.aaa1115910.bv.viewmodel.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.search.SearchKeyword
import dev.aaa1115910.biliapi.repositories.SearchRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.dao.AppDatabase
import dev.aaa1115910.bv.entity.db.SearchHistoryDB
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.swapListWithMainContext
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel
import java.util.Date

@KoinViewModel
class SearchInputViewModel(
    private val searchRepository: SearchRepository,
    private val db: AppDatabase = BVApp.getAppDatabase()
) : ViewModel() {
    private val logger = KotlinLogging.logger { }

    var keyword by mutableStateOf("")
    val hotwords = mutableStateListOf<SearchKeyword>()
    val recommendKeywords = mutableStateListOf<SearchKeyword>()
    val trendingRankingKeywords = mutableStateListOf<SearchKeyword>()
    val suggests = mutableStateListOf<String>()
    val searchHistories = mutableStateListOf<SearchHistoryDB>()
    val matchedSearchHistories = mutableStateListOf<SearchHistoryDB>()

    var hotwordsLoading by mutableStateOf(false)
        private set
    var recommendKeywordsLoading by mutableStateOf(false)
        private set
    var trendingRankingLoading by mutableStateOf(false)
        private set

    var hotwordsError by mutableStateOf<String?>(null)
        private set
    var recommendKeywordsError by mutableStateOf<String?>(null)
        private set
    var trendingRankingError by mutableStateOf<String?>(null)
        private set

    init {
        refreshHotwords()
        refreshRecommendKeywords()
        loadSearchHistories()
    }

    fun refreshHotwords() {
        logger.fInfo { "Update hotwords" }
        if (hotwordsLoading) return
        hotwordsLoading = true
        hotwordsError = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val hotwordData = searchRepository.getSearchHotwords(
                    limit = 10,
                    preferApiType = Prefs.apiType
                )
                logger.debug { "Find hotwords: $hotwordData" }
                hotwords.swapListWithMainContext(hotwordData)
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    hotwordsError = e.message ?: "bilibili 热搜加载失败"
                    "bilibili 热搜加载失败".toast(BVApp.context)
                }
                logger.info { e.stackTraceToString() }
            } finally {
                withContext(Dispatchers.Main) {
                    hotwordsLoading = false
                }
            }
        }
    }

    fun refreshRecommendKeywords() {
        logger.fInfo { "Update search recommend keywords" }
        if (recommendKeywordsLoading) return
        recommendKeywordsLoading = true
        recommendKeywordsError = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val keywordData = searchRepository.getSearchRecommendKeywords()
                logger.debug { "Find search recommend keywords: $keywordData" }
                recommendKeywords.swapListWithMainContext(keywordData)
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    recommendKeywordsError = e.message ?: "搜索发现加载失败"
                    "搜索发现加载失败".toast(BVApp.context)
                }
                logger.info { e.stackTraceToString() }
            } finally {
                withContext(Dispatchers.Main) {
                    recommendKeywordsLoading = false
                }
            }
        }
    }

    fun refreshTrendingRanking() {
        logger.fInfo { "Update search trending ranking" }
        if (trendingRankingLoading) return
        trendingRankingLoading = true
        trendingRankingError = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val keywordData = searchRepository.getSearchTrendingRanking(limit = 50)
                logger.debug { "Find search trending ranking: $keywordData" }
                trendingRankingKeywords.swapListWithMainContext(keywordData)
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    trendingRankingError = e.message ?: "完整榜单加载失败"
                    "完整榜单加载失败".toast(BVApp.context)
                }
                logger.info { e.stackTraceToString() }
            } finally {
                withContext(Dispatchers.Main) {
                    trendingRankingLoading = false
                }
            }
        }
    }

    fun updateSuggests() {
        logger.fInfo { "Update search suggests with '$keyword'" }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val keywordSuggest = searchRepository.getSearchSuggest(
                    keyword = keyword,
                    preferApiType = Prefs.apiType
                )
                logger.debug { "Find suggests: $keywordSuggest" }
                suggests.swapListWithMainContext(keywordSuggest)
            }.onFailure {
                withContext(Dispatchers.Main) {
                    "bilibili 搜索建议加载失败".toast(BVApp.context)
                }
                logger.info { it.stackTraceToString() }
            }
        }
        updateMatchedSearchHistories()
    }

    private fun loadSearchHistories() {
        logger.fInfo { "Load search histories" }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                searchHistories.swapListWithMainContext(db.searchHistoryDao().getHistories(20))
                logger.fInfo { "Load search histories finish, size: ${searchHistories.size}" }
            }
        }
    }

    private fun updateMatchedSearchHistories() {
        logger.fInfo { "Update matched search histories with '$keyword'" }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                if (keyword.isEmpty()) {
                    matchedSearchHistories.clear()
                } else {
                    val matchedHistories = db.searchHistoryDao().findHistories(keyword, 20)
                    matchedSearchHistories.swapListWithMainContext(matchedHistories)
                }
                logger.fInfo { "Update matched search histories finish, size: ${matchedSearchHistories.size}" }
            }
        }
    }

    fun addSearchHistory(keyword: String) {
        logger.fInfo { "Add search history: $keyword" }
        viewModelScope.launch(Dispatchers.IO) {
            db.searchHistoryDao().findHistory(keyword)?.let { history ->
                logger.fInfo { "Search history $keyword already exist" }
                history.searchDate = Date()
                db.searchHistoryDao().update(history)
            } ?: let {
                logger.fInfo { "Insert new search history $keyword" }
                val history = SearchHistoryDB(keyword = keyword)
                db.searchHistoryDao().insert(history)
            }
            loadSearchHistories()
        }
    }

    fun deleteSearchHistory(history: SearchHistoryDB) {
        logger.fInfo { "Delete search history: ${history.keyword}" }
        viewModelScope.launch(Dispatchers.IO) {
            db.searchHistoryDao().delete(history)
            loadSearchHistories()
        }
    }

    fun deleteSearchHistoryByKeyword(keyword: String) {
        logger.fInfo { "Delete search history by keyword: $keyword" }
        viewModelScope.launch(Dispatchers.IO) {
            db.searchHistoryDao().findHistory(keyword)?.let {
                db.searchHistoryDao().delete(it)
            }
            loadSearchHistories()
            updateMatchedSearchHistories()
        }
    }

    fun deleteAllSearchHistories() {
        logger.fInfo { "Delete all search histories" }
        viewModelScope.launch(Dispatchers.IO) {
            db.searchHistoryDao().deleteAll()
            loadSearchHistories()
            updateMatchedSearchHistories()
        }
    }
}
