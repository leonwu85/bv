package dev.aaa1115910.bv.viewmodel.index

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.aaa1115910.biliapi.entity.pgc.PgcItem
import dev.aaa1115910.biliapi.entity.pgc.PgcType
import dev.aaa1115910.biliapi.entity.pgc.index.PGC_INDEX_ORDER_FIELD
import dev.aaa1115910.biliapi.entity.pgc.index.PgcIndexData
import dev.aaa1115910.biliapi.entity.pgc.index.PgcIndexOption
import dev.aaa1115910.biliapi.entity.pgc.index.PgcIndexSection
import dev.aaa1115910.biliapi.repositories.PgcRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.util.addAllWithMainContext
import dev.aaa1115910.bv.util.fError
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class PgcIndexViewModel(
    private val pgcRepository: PgcRepository,
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    val indexResultItems = mutableStateListOf<PgcItem>()

    var updating by mutableStateOf(false)
        private set
    private var nextPage = PgcIndexData.PgcIndexPage()
    val noMore get() = nextPage.hasNext.not()

    var pgcType by mutableStateOf(PgcType.Anime)

    var filterSections by mutableStateOf<List<PgcIndexSection>>(emptyList())
    val selectedFilters = mutableStateMapOf<String, PgcIndexOption>()

    val isFilterReady get() = filterSections.isNotEmpty()

    val activeFilterTags: List<String>
        get() = filterSections.mapNotNull { section ->
            val selectedOption = selectedFilters[section.field] ?: return@mapNotNull null
            val defaultOption = section.options.firstOrNull() ?: return@mapNotNull null
            selectedOption.takeIf { it.keyword != defaultOption.keyword }?.name
        }

    val filterSignature: String
        get() = filterSections.joinToString("&") { section ->
            val selectedOption = selectedFilters[section.field]
            "${section.field}=${selectedOption?.keyword.orEmpty()}:${selectedOption?.sort.orEmpty()}"
        }

    suspend fun changePgcType(pgcType: PgcType) {
        this.pgcType = pgcType
        clearData()
        filterSections = emptyList()
        selectedFilters.clear()

        runCatching {
            pgcRepository.getPgcIndexCondition(pgcType)
        }.onSuccess { conditionData ->
            val sections = conditionData.buildSections()
            filterSections = sections
            sections.forEach { section ->
                section.options.firstOrNull()?.let { option ->
                    selectedFilters[section.field] = option
                }
            }
        }.onFailure {
            logger.fError { "Load $pgcType index conditions failed: ${it.stackTraceToString()}" }
            withContext(Dispatchers.Main) {
                "加载 $pgcType 筛选条件失败: ${it.localizedMessage}".toast(BVApp.context)
            }
        }
    }

    fun updateFilter(option: PgcIndexOption) {
        val currentOption = selectedFilters[option.field]
        if (currentOption == option) return
        selectedFilters[option.field] = option
    }

    fun resetFilters() {
        filterSections.forEach { section ->
            section.options.firstOrNull()?.let { option ->
                selectedFilters[section.field] = option
            }
        }
    }

    suspend fun loadMore() {
        if (!isFilterReady) return
        if (!updating) loadData()
    }

    private suspend fun loadData() {
        withContext(Dispatchers.Main) {
            updating = true
        }
        if (!nextPage.hasNext) {
            withContext(Dispatchers.Main) {
                updating = false
            }
            return
        }
        runCatching {
            val selectedOrder = selectedFilters[PGC_INDEX_ORDER_FIELD]
                ?: error("PGC index order is not initialized")
            val result = pgcRepository.getPgcIndex(
                pgcType = pgcType,
                order = selectedOrder.keyword,
                sort = selectedOrder.sort ?: "0",
                filters = selectedFilters.values
                    .asSequence()
                    .filter { it.field != PGC_INDEX_ORDER_FIELD }
                    .associate { it.field to it.keyword },
                page = nextPage
            )
            val newItems = deduplicatePgcItemsBySeasonId(
                existingItems = indexResultItems,
                newItems = result.list
            )
            indexResultItems.addAllWithMainContext(newItems)
            nextPage = result.nextPage
            logger.info { "load more $pgcType list success, size: ${result.list.size}, added: ${newItems.size}" }
        }.onFailure {
            logger.fError { "Load $pgcType index list failed: ${it.stackTraceToString()}" }
            withContext(Dispatchers.Main) {
                "加载 $pgcType 索引失败: ${it.localizedMessage}".toast(BVApp.context)
            }
        }
        withContext(Dispatchers.Main) {
            updating = false
        }
    }

    suspend fun clearData() {
        withContext(Dispatchers.Main) {
            indexResultItems.clear()
            nextPage = PgcIndexData.PgcIndexPage()
            updating = false
        }
    }
}

internal fun deduplicatePgcItemsBySeasonId(
    existingItems: List<PgcItem>,
    newItems: List<PgcItem>
): List<PgcItem> {
    val seenSeasonIds = existingItems.mapTo(mutableSetOf()) { it.seasonId }
    return newItems.filter { seenSeasonIds.add(it.seasonId) }
}
