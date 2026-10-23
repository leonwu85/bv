package dev.aaa1115910.bv.viewmodel.user

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.repositories.OpusCard
import dev.aaa1115910.biliapi.repositories.OpusRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class OpusSpaceViewModel(private val repository: OpusRepository) : ViewModel() {
    val items = mutableStateListOf<OpusCard>()
    val collections = mutableStateListOf<OpusCard>()
    var collectionItems by mutableStateOf<List<OpusCard>>(emptyList())
    var selectedCollection by mutableStateOf<OpusCard?>(null)
        private set
    var expanded by mutableStateOf(false)
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var hasMore by mutableStateOf(true)
    private var mid = 0L
    private var next = ""
    private var generation = 0
    private var job: Job? = null

    fun initialize(mid: Long) {
        if (this.mid == mid) return
        this.mid = mid
        generation++; job?.cancel()
        items.clear(); collections.clear(); next = ""; hasMore = true; loading = false
        selectCollection(null)
    }
    fun selectCollection(collection: OpusCard?) {
        generation++; job?.cancel(); loading = false; error = null
        selectedCollection = collection
        collectionItems = emptyList()
        if (collection != null || items.isEmpty()) load()
    }
    fun load() {
        if (loading || mid <= 0 || selectedCollection == null && !hasMore) return
        val version = generation
        val selected = selectedCollection
        val cursor = next
        loading = true; error = null
        job = viewModelScope.launch {
            try {
                if (selected != null) {
                    val response = repository.collection(selected.id)
                    if (version == generation) collectionItems = response
                } else {
                    val response = repository.space(mid, cursor)
                    if (version != generation) return@launch
                    if (cursor.isEmpty()) { collections.clear(); collections.addAll(response.collections) }
                    items.addAll(response.items.filter { new -> items.none { it.id == new.id } })
                    hasMore = response.next.isNotBlank() && response.next != cursor
                    next = response.next
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (version == generation) error = e.message ?: "图文加载失败" }
            finally { if (version == generation) loading = false }
        }
    }
}
