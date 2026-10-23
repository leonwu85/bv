package dev.aaa1115910.bv.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable data class BackupValue(val type: String, val value: JsonElement) {
    fun decoded(): Any = when (type) {
        "string" -> value.jsonPrimitive.also { require(it.isString) }.content
        "boolean" -> value.jsonPrimitive.boolean
        "int" -> value.jsonPrimitive.int
        "long" -> value.jsonPrimitive.long
        "float" -> value.jsonPrimitive.float.also { require(it.isFinite()) }
        "double" -> value.jsonPrimitive.double.also { require(it.isFinite()) }
        "strings" -> value.jsonArray.map { require(it.jsonPrimitive.isString); it.jsonPrimitive.content }.toSet()
        else -> error("不支持的设置类型")
    }
    companion object {
        fun from(value: Any) = when (value) {
            is String -> BackupValue("string", JsonPrimitive(value))
            is Boolean -> BackupValue("boolean", JsonPrimitive(value))
            is Int -> BackupValue("int", JsonPrimitive(value))
            is Long -> BackupValue("long", JsonPrimitive(value))
            is Float -> BackupValue("float", JsonPrimitive(value))
            is Double -> BackupValue("double", JsonPrimitive(value))
            is Set<*> -> BackupValue("strings", JsonArray(value.map { JsonPrimitive(it as String) }))
            else -> error("不支持的设置类型")
        }
    }
}
@Serializable data class BackupAccount(val uid: Long, val name: String, val avatar: String, val auth: String, val lock: String)
@Serializable data class BackupHistory(val keyword: String, val time: Long)
@Serializable data class BackupAsset(val entry: String, val sha256: String)
@Serializable data class BackupDocument(
    val format: String = "BV", val version: Int = 1, val createdAt: Long = System.currentTimeMillis(),
    val settings: Map<String, BackupValue>? = null,
    val accountPreferences: Map<String, BackupValue>? = null,
    val accounts: List<BackupAccount>? = null,
    val history: List<BackupHistory>? = null,
    val blockedUsers: List<Long>? = null,
    val tvRenderMode: String? = null,
    val assets: Map<String, BackupAsset> = emptyMap()
)
data class LoadedBackup(val document: BackupDocument, val assets: Map<String, ByteArray>)

object BackupArchive {
    const val MAX_BYTES = 64 * 1024 * 1024
    val assetKeys = setOf("mobile_theme_light_background_uri", "mobile_theme_dark_background_uri", "mobile_custom_font_path")
    val accountKeys = setOf("il", "uid", "sid", "sd", "bj", "ucm", "ted", "access_token", "refresh_token", "random_buvid", "random_buvid3")
    private val json = Json { encodeDefaults = true }
    fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    fun write(document: BackupDocument, assets: Map<String, ByteArray>, output: OutputStream) {
        val manifest = document.copy(assets = assets.mapValues { (key, bytes) -> BackupAsset("assets/$key", digest(bytes)) })
        validate(manifest)
        val data = json.encodeToString(manifest).toByteArray()
        require(data.size <= 8 * 1024 * 1024 && data.size.toLong() + assets.values.sumOf { it.size.toLong() } <= MAX_BYTES) { "备份超过 64 MiB 限制" }
        ZipOutputStream(output).use { zip ->
            fun entry(name: String, bytes: ByteArray) { zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry() }
            entry("backup.json", data)
            assets.forEach { (key, bytes) -> entry("assets/$key", bytes) }
        }
    }

    fun read(input: InputStream): LoadedBackup {
        val entries = mutableMapOf<String, ByteArray>()
        var total = 0L
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(entry.name == "backup.json" || assetKeys.any { entry.name == "assets/$it" }) { "备份含不支持的文件" }
                require(!entry.isDirectory && entry.name !in entries) { "备份含重复文件" }
                val limit = if (entry.name == "backup.json") 8 * 1024 * 1024 else MAX_BYTES
                val buffer = java.io.ByteArrayOutputStream()
                val chunk = ByteArray(8192)
                while (true) {
                    val count = zip.read(chunk)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_BYTES && buffer.size().toLong() + count <= limit) { "备份超过大小限制" }
                    buffer.write(chunk, 0, count)
                }
                entries[entry.name] = buffer.toByteArray()
                zip.closeEntry()
            }
        }
        val document = json.decodeFromString<BackupDocument>(requireNotNull(entries.remove("backup.json")) { "缺少备份清单" }.toString(Charsets.UTF_8))
        validate(document)
        require(entries.keys == document.assets.values.map { it.entry }.toSet()) { "备份资源不完整" }
        val assets = document.assets.mapValues { (_, asset) ->
            requireNotNull(entries[asset.entry]).also { require(digest(it) == asset.sha256) { "备份资源校验失败" } }
        }
        return LoadedBackup(document, assets)
    }

    private fun validate(doc: BackupDocument) {
        require(doc.format == "BV" && doc.version == 1) { "不支持的备份格式或版本" }
        require((doc.accounts == null) == (doc.accountPreferences == null)) { "账号数据不完整" }
        require(doc.settings?.keys?.none { it in accountKeys } != false) { "设置包含账号字段" }
        require(doc.accountPreferences?.keys?.all { it in accountKeys } != false) { "账号设置含不支持字段" }
        (doc.settings.orEmpty() + doc.accountPreferences.orEmpty()).forEach { (key, value) ->
            require(key.isNotBlank() && key.length <= 256)
            value.decoded()
        }
        require(doc.accounts.orEmpty().size <= 1000 && doc.history.orEmpty().size <= 100000)
        require(doc.accounts.orEmpty().all { it.uid > 0 } && doc.accounts.orEmpty().map { it.uid }.distinct().size == doc.accounts.orEmpty().size)
        require(doc.history.orEmpty().all { it.keyword.isNotBlank() && it.keyword.length <= 8192 && it.time >= 0 })
        require(doc.blockedUsers.orEmpty().all { it > 0 })
        require(doc.assets.all { (key, asset) -> key in assetKeys && asset.entry == "assets/$key" && doc.settings?.containsKey(key) == true })
        require(doc.tvRenderMode == null || doc.tvRenderMode in setOf("auto", "1080p", "native"))
    }
}
