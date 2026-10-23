package dev.aaa1115910.bv.repository

import dev.aaa1115910.bv.BVApp
import android.util.AtomicFile
import dev.aaa1115910.biliapi.entity.reply.Comment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import java.io.File

/** 本地内容屏蔽独立于 B 站账号黑名单，未登录时同样生效。 */
@Single
class LocalUserBlockRepository {
    private val file = AtomicFile(File(BVApp.context.filesDir, "blocked-users.json"))
    private val mutableBlocked = MutableStateFlow(read())
    val blocked = mutableBlocked.asStateFlow()

    fun isBlocked(mid: Long): Boolean = mid > 0L && mid in blocked.value

    @Synchronized
    fun add(mid: Long): Result<Unit> = runCatching {
        require(mid > 0L) { "请输入有效 UID" }
        save(blocked.value + mid)
    }

    @Synchronized
    fun remove(mid: Long): Result<Unit> = runCatching { save(blocked.value - mid) }

    @Synchronized
    fun import(text: String): Result<Int> = runCatching {
        val imported = parseBlockedUserIds(text)
        val added = (imported - blocked.value).size
        save(blocked.value + imported)
        added
    }

    @Synchronized
    fun replace(ids: Set<Long>): Result<Unit> = runCatching { require(ids.all { it > 0 }); save(ids) }

    fun export(): String = blocked.value.sorted().joinToString("\n")

    private fun read(): Set<Long> = runCatching {
        Json.decodeFromString<List<Long>>(file.openRead().bufferedReader().use { it.readText() })
            .filter { it > 0L }.toSet()
    }.getOrDefault(emptySet())

    private fun save(ids: Set<Long>) {
        val stream = file.startWrite()
        try {
            stream.write(Json.encodeToString(ids.sorted()).toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
        } catch (error: Throwable) {
            file.failWrite(stream)
            throw error
        }
        mutableBlocked.value = ids.toSet()
    }
}

/** 过滤新响应和已显示内容时使用相同规则；不改变服务器分页游标。 */
fun List<Comment>.withoutBlockedUsers(blocked: Set<Long>): List<Comment> = mapNotNull { comment ->
    if (comment.mid in blocked) null
    else comment.copy(replies = comment.replies.withoutBlockedUsers(blocked))
}

fun <T> LocalUserBlockRepository.observeList(
    scope: CoroutineScope,
    items: MutableList<T>,
    uid: (T) -> Long
) = scope.launch {
    blocked.collect { ids -> items.removeAll { uid(it) in ids } }
}
