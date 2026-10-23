package dev.aaa1115910.bv.backup

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.*
import androidx.room.withTransaction
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.dataStore
import dev.aaa1115910.bv.entity.AuthData
import dev.aaa1115910.bv.entity.db.SearchHistoryDB
import dev.aaa1115910.bv.entity.db.UserDB
import dev.aaa1115910.bv.repository.LocalUserBlockRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.Date
import java.util.UUID

/** Logical snapshots keep Room and DataStore open and preserve database schema compatibility. */
class BackupRepository(private val context: Context, private val blocks: LocalUserBlockRepository) {
    data class Selection(val settings: Boolean = true, val accounts: Boolean = false, val history: Boolean = true, val blocked: Boolean = true)
    companion object { private val mutex = Mutex() }
    private val database get() = BVApp.getAppDatabase(context)
    private val renderPrefs get() = context.getSharedPreferences("tv_ui_render_settings", Context.MODE_PRIVATE)
    val localDirectory get() = File(context.getExternalFilesDir(null) ?: context.filesDir, "backups")

    suspend fun exportLocal(selection: Selection): File {
        val file = withContext(Dispatchers.IO) {
            check(localDirectory.isDirectory || localDirectory.mkdirs()) { "无法创建备份目录" }
            File(localDirectory, "BV-backup-${System.currentTimeMillis()}.zip")
        }
        try { export(Uri.fromFile(file), selection) }
        catch (e: Throwable) { withContext(NonCancellable + Dispatchers.IO) { file.delete() }; throw e }
        return file
    }

    suspend fun localBackups(): List<File> = withContext(Dispatchers.IO) {
        localDirectory.listFiles().orEmpty().filter { it.isFile && it.extension.equals("zip", true) }.sortedByDescending { it.lastModified() }
    }

    suspend fun export(uri: Uri, selection: Selection) = withContext(Dispatchers.IO) { mutex.withLock {
        val preferences = context.dataStore.data.first().asMap().mapKeys { it.key.name }.mapValues { BackupValue.from(it.value) }
        val settings = preferences.filterKeys { it !in BackupArchive.accountKeys }.takeIf { selection.settings }
        val assets = settings.orEmpty().filterKeys { it in BackupArchive.assetKeys }.mapNotNull { (key, value) ->
            val path = value.value.jsonPrimitive.content
            if (path.isBlank()) return@mapNotNull null
            val source = if (path.startsWith('/')) Uri.fromFile(File(path)) else Uri.parse(path)
            val bytes = requireNotNull(context.contentResolver.openInputStream(source)) { "无法读取自定义主题资源" }.use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) { val count = stream.read(buffer); if (count < 0) break
                    require(output.size().toLong() + count <= BackupArchive.MAX_BYTES) { "主题资源过大" }; output.write(buffer, 0, count) }
                output.toByteArray()
            }
            key to bytes
        }.toMap()
        val document = database.withTransaction {
            BackupDocument(settings = settings,
                accounts = if (selection.accounts) database.userDao().getAll().map { BackupAccount(it.uid, it.username, it.avatar, it.auth, it.lock) } else null,
                accountPreferences = preferences.filterKeys { it in BackupArchive.accountKeys }.takeIf { selection.accounts },
                history = if (selection.history) database.searchHistoryDao().getAll().map { BackupHistory(it.keyword, it.searchDate.time) } else null,
                blockedUsers = blocks.blocked.value.sorted().takeIf { selection.blocked },
                tvRenderMode = renderPrefs.getString("tv_ui_render_mode", "auto").takeIf { selection.settings })
        }
        val temporary = File.createTempFile("bv-backup-", ".zip", context.cacheDir)
        try {
            temporary.outputStream().use { BackupArchive.write(document, assets, it) }
            requireNotNull(context.contentResolver.openOutputStream(uri, "wt")) { "无法写入备份文件" }.use { output ->
                temporary.inputStream().use { it.copyTo(output) }
            }
        } finally { temporary.delete() }
    } }

    suspend fun read(uri: Uri): LoadedBackup = withContext(Dispatchers.IO) {
        requireNotNull(context.contentResolver.openInputStream(uri)) { "无法读取备份文件" }.use(BackupArchive::read)
            .also { backup -> backup.document.accounts.orEmpty().forEach { require(AuthData.fromJson(it.auth).uid == it.uid) { "账号数据校验失败" } } }
    }

    suspend fun restore(backup: LoadedBackup, selection: Selection) = withContext(Dispatchers.IO) { mutex.withLock {
        val doc = backup.document
        val oldPrefs = context.dataStore.data.first()
        val oldBlocked = blocks.blocked.value
        val oldMode = renderPrefs.getString("tv_ui_render_mode", "auto")
        val assetsDir = File(context.filesDir, "backup_assets/${UUID.randomUUID()}")
        val settings = doc.settings.orEmpty().toMutableMap()
        try {
        if (selection.settings && doc.settings != null && backup.assets.isNotEmpty()) {
            check(assetsDir.mkdirs()) { "无法保存主题资源" }
            backup.assets.forEach { (key, bytes) ->
                val target = File(assetsDir, key)
                target.writeBytes(bytes)
                settings[key] = BackupValue.from(if (key == "mobile_custom_font_path") target.absolutePath else Uri.fromFile(target).toString())
            }
        }
            database.withTransaction {
                if (selection.accounts && doc.accounts != null) {
                    database.userDao().deleteAll()
                    database.userDao().insert(*doc.accounts.map { UserDB(uid = it.uid, username = it.name, avatar = it.avatar, auth = it.auth, lock = it.lock) }.toTypedArray())
                }
                if (selection.history && doc.history != null) {
                    database.searchHistoryDao().deleteAll()
                    database.searchHistoryDao().insert(*doc.history.distinctBy { it.keyword }.map { SearchHistoryDB(keyword = it.keyword, searchDate = Date(it.time)) }.toTypedArray())
                }
                context.dataStore.edit { prefs ->
                    fun applies(key: String) = if (key in BackupArchive.accountKeys) selection.accounts && doc.accountPreferences != null else selection.settings && doc.settings != null
                    prefs.asMap().keys.filter { applies(it.name) }.forEach { prefs.remove(it) }
                    val values = (if (selection.settings && doc.settings != null) settings else emptyMap()) +
                        (doc.accountPreferences.orEmpty().takeIf { selection.accounts } ?: emptyMap())
                    values.forEach { (key, value) -> prefs.setBackupValue(key, value) }
                }
                if (selection.blocked && doc.blockedUsers != null) blocks.replace(doc.blockedUsers.toSet()).getOrThrow()
                if (selection.settings && doc.tvRenderMode != null) check(renderPrefs.edit().putString("tv_ui_render_mode", doc.tvRenderMode).commit())
            }
        } catch (error: Throwable) {
            withContext(NonCancellable) {
                context.dataStore.updateData { oldPrefs }
                blocks.replace(oldBlocked).getOrThrow()
                renderPrefs.edit().putString("tv_ui_render_mode", oldMode).commit()
                assetsDir.deleteRecursively()
            }
            throw error
        }
    } }
}

private fun MutablePreferences.setBackupValue(key: String, value: BackupValue) {
    when (val decoded = value.decoded()) {
        is String -> this[stringPreferencesKey(key)] = decoded
        is Boolean -> this[booleanPreferencesKey(key)] = decoded
        is Int -> this[intPreferencesKey(key)] = decoded
        is Long -> this[longPreferencesKey(key)] = decoded
        is Float -> this[floatPreferencesKey(key)] = decoded
        is Double -> this[doublePreferencesKey(key)] = decoded
        is Set<*> -> this[stringSetPreferencesKey(key)] = decoded.filterIsInstance<String>().toSet()
    }
}
