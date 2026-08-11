package dev.aaa1115910.bv.offline

import android.net.Uri
import android.os.Build
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import dev.aaa1115910.biliapi.entity.DashAudio
import dev.aaa1115910.biliapi.entity.DashVideo
import dev.aaa1115910.biliapi.entity.PlayData
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.entity.danmaku.DanmakuData
import dev.aaa1115910.biliapi.repositories.AuthRepository
import dev.aaa1115910.biliapi.repositories.VideoPlayRepository
import dev.aaa1115910.biliapi.util.AvBvConverter
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.util.Prefs
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayDeque
import kotlin.math.ceil

enum class OfflineVideoCacheStatus {
    Idle,
    Queued,
    Fetching,
    DownloadingVideo,
    DownloadingAudio,
    DownloadingDanmaku,
    Paused,
    Completed,
    Failed
}

data class OfflineVideoCacheTaskState(
    val aid: Long,
    val cid: Long,
    val title: String = "",
    val partTitle: String = "",
    val cover: String = "",
    val qualityText: String = "",
    val status: OfflineVideoCacheStatus = OfflineVideoCacheStatus.Idle,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val message: String = "",
    val upName: String = "",
    val upFace: String = "",
    val danmakuCount: Int = 0,
    val durationMs: Long = 0L
) {
    val progress: Float
        get() = if (totalBytes > 0L) {
            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val isActive: Boolean
        get() = status == OfflineVideoCacheStatus.Queued ||
            status == OfflineVideoCacheStatus.Fetching ||
            status == OfflineVideoCacheStatus.DownloadingVideo ||
            status == OfflineVideoCacheStatus.DownloadingAudio ||
            status == OfflineVideoCacheStatus.DownloadingDanmaku
}

@Serializable
data class OfflineVideoCacheEntry(
    val version: Int = 1,
    val aid: Long,
    val cid: Long,
    val bvid: String,
    val title: String,
    val partTitle: String,
    val cover: String,
    val upName: String,
    val quality: Int,
    val qualityText: String,
    val videoCodecId: Int,
    val videoCodec: String,
    val audioCodecId: Int,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val videoFileName: String,
    val audioFileName: String,
    val danmakuFileName: String = "danmaku.json",
    val totalBytes: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val completed: Boolean,
    val danmakuCached: Boolean = false,
    val upFace: String = "",
    val danmakuCount: Int = 0,
    val coverFileName: String = "",
    val upFaceFileName: String = ""
) {
    val displayTitle: String
        get() = partTitle.ifBlank { title }
}

data class OfflineVideoCacheRequest(
    val aid: Long,
    val cid: Long,
    val bvid: String,
    val title: String,
    val partTitle: String,
    val cover: String,
    val upName: String,
    val quality: Int,
    val qualityText: String,
    val videoCodecId: Int,
    val videoCodec: String,
    val audioCodecId: Int,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val videoUrls: List<String>,
    val audioUrls: List<String>,
    val upFace: String = "",
    val danmakuCount: Int = 0
)

data class OfflineVideoCacheTarget(
    val aid: Long,
    val bvid: String,
    val cid: Long,
    val title: String,
    val partTitle: String,
    val cover: String,
    val upName: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val upFace: String = "",
    val danmakuCount: Int = 0
)

data class OfflineVideoCacheTaskRequest(
    val target: OfflineVideoCacheTarget,
    val preferredQuality: Resolution,
    val tryLook1080P: Boolean,
    val videoCodecPreferences: List<VideoCodec>,
    val preferredAudio: Audio,
)

data class OfflineVideoPlaybackSource(
    val entry: OfflineVideoCacheEntry,
    val playData: PlayData
)

@Serializable
private data class OfflineDanmakuCacheFile(
    val version: Int = 1,
    val aid: Long,
    val cid: Long,
    val durationMs: Long,
    val cachedAt: Long,
    val segments: List<OfflineDanmakuSegment>
)

@Serializable
private data class OfflineDanmakuSegment(
    val index: Int,
    val items: List<OfflineDanmakuItem>
)

private data class OfflineDanmakuCacheResult(
    val bytes: Long,
    val itemCount: Int
)

@Serializable
private data class OfflineDanmakuItem(
    val time: Float,
    val type: Int,
    val size: Int,
    val color: Int,
    val timestamp: Int,
    val pool: Int,
    val midHash: String,
    val dmid: Long,
    val level: Int,
    val text: String
)

@Single
class OfflineVideoCacheService(
    private val authRepository: AuthRepository,
    private val videoPlayRepository: VideoPlayRepository
) {
    private val logger = KotlinLogging.logger { }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queue = ArrayDeque<OfflineVideoCacheTaskRequest>()
    private val pausedRequests = mutableMapOf<String, OfflineVideoCacheTaskRequest>()
    private var currentJob: Job? = null
    private var currentRequest: OfflineVideoCacheTaskRequest? = null
    private val danmakuCacheByKey = mutableMapOf<String, OfflineDanmakuCacheFile>()
    private val discardedTaskKeys = mutableSetOf<String>()
    private val removingTaskKeys = mutableSetOf<String>()
    private val latestTaskStates = mutableMapOf<String, OfflineVideoCacheTaskState>()

    val taskStates = mutableStateMapOf<String, OfflineVideoCacheTaskState>()
    val entries = mutableStateListOf<OfflineVideoCacheEntry>()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val compactJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    init {
        serviceScope.launch {
            refreshEntries()
        }
    }

    fun key(aid: Long, cid: Long): String = "$aid:$cid"

    fun stateOf(aid: Long, cid: Long): OfflineVideoCacheTaskState {
        val key = key(aid, cid)
        taskStates[key]?.let { return it }
        val entry = getCompletedEntry(aid, cid)
        return if (entry != null) {
            OfflineVideoCacheTaskState(
                aid = aid,
                cid = cid,
                title = entry.title,
                partTitle = entry.partTitle,
                cover = entry.cover,
                qualityText = entry.qualityText,
                status = OfflineVideoCacheStatus.Completed,
                downloadedBytes = entry.totalBytes,
                totalBytes = entry.totalBytes,
                upName = entry.upName,
                upFace = entry.upFace,
                danmakuCount = entry.danmakuCount,
                durationMs = entry.durationMs
            )
        } else {
            OfflineVideoCacheTaskState(aid = aid, cid = cid)
        }
    }

    fun enqueue(request: OfflineVideoCacheTaskRequest): Result<String> =
        enqueue(request = request, resetFailedCache = true)

    private fun enqueue(
        request: OfflineVideoCacheTaskRequest,
        resetFailedCache: Boolean,
        initialProgress: OfflineVideoCacheTaskState? = null,
    ): Result<String> {
        val target = request.target
        if (getCompletedEntry(target.aid, target.cid) != null) {
            return Result.success("已缓存，可离线播放")
        }

        val key = key(target.aid, target.cid)
        synchronized(this) {
            if (key in removingTaskKeys) {
                return Result.failure(IllegalStateException("正在取消缓存，请稍后重试"))
            }
            if (
                currentRequest?.let { key(it.target.aid, it.target.cid) } == key ||
                queue.any { key(it.target.aid, it.target.cid) == key }
            ) {
                return Result.success("已在缓存队列中")
            }
            discardedTaskKeys.remove(key)
            latestTaskStates.remove(key)
            pausedRequests.remove(key)
            if (resetFailedCache && shouldResetFailedCache(target)) {
                entryDir(target.aid, target.cid).deleteRecursively()
            }
            queue.add(request)
            updateState(
                request.toState(
                    status = OfflineVideoCacheStatus.Queued,
                    downloadedBytes = initialProgress?.downloadedBytes ?: 0L,
                    totalBytes = initialProgress?.totalBytes ?: 0L,
                    message = "等待缓存"
                )
            )
            startNextLocked()
        }
        return Result.success("已加入缓存队列")
    }

    fun pause(aid: Long, cid: Long): Result<String> {
        val targetKey = key(aid, cid)
        synchronized(this) {
            val active = currentRequest
            if (active != null && key(active.target.aid, active.target.cid) == targetKey) {
                val currentState = latestTaskStates[targetKey] ?: taskStates[targetKey]
                pausedRequests[targetKey] = active
                currentJob?.cancel(CancellationException("Paused by user"))
                updateState(
                    active.toState(
                        status = OfflineVideoCacheStatus.Paused,
                        downloadedBytes = currentState?.downloadedBytes ?: 0L,
                        totalBytes = currentState?.totalBytes ?: 0L,
                        message = "已暂停"
                    )
                )
                return Result.success("已暂停缓存")
            }

            val iterator = queue.iterator()
            while (iterator.hasNext()) {
                val request = iterator.next()
                if (key(request.target.aid, request.target.cid) == targetKey) {
                    iterator.remove()
                    pausedRequests[targetKey] = request
                    val currentState = latestTaskStates[targetKey] ?: taskStates[targetKey]
                    updateState(
                        request.toState(
                            status = OfflineVideoCacheStatus.Paused,
                            downloadedBytes = currentState?.downloadedBytes ?: 0L,
                            totalBytes = currentState?.totalBytes ?: 0L,
                            message = "已暂停"
                        )
                    )
                    return Result.success("已暂停缓存")
                }
            }
        }
        return Result.failure(IllegalStateException("没有正在缓存的任务"))
    }

    fun resume(aid: Long, cid: Long): Result<String> {
        val targetKey = key(aid, cid)
        synchronized(this) {
            val request = pausedRequests.remove(targetKey)
                ?: return Result.failure(IllegalStateException("缓存地址已过期，请回到视频页重新缓存"))
            val currentState = latestTaskStates[targetKey] ?: taskStates[targetKey]

            if (currentRequest?.let { key(it.target.aid, it.target.cid) } == targetKey) {
                if (queue.none { key(it.target.aid, it.target.cid) == targetKey }) {
                    queue.add(request)
                    updateState(
                        request.toState(
                            status = OfflineVideoCacheStatus.Queued,
                            downloadedBytes = currentState?.downloadedBytes ?: 0L,
                            totalBytes = currentState?.totalBytes ?: 0L,
                            message = "等待继续缓存"
                        )
                    )
                }
                return Result.success("已加入缓存队列")
            }

            return enqueue(
                request = request,
                resetFailedCache = false,
                initialProgress = currentState
            )
        }
    }

    suspend fun delete(aid: Long, cid: Long): Result<String> =
        removeCacheFiles(
            aid = aid,
            cid = cid,
            cancellationReason = "Deleted by user",
            successMessage = "已删除缓存"
        )

    suspend fun clearTask(aid: Long, cid: Long): Result<String> {
        if (getCompletedEntry(aid, cid) != null) {
            return Result.failure(IllegalStateException("已完成的缓存请使用删除缓存"))
        }
        return removeCacheFiles(
            aid = aid,
            cid = cid,
            cancellationReason = "Cleared by user",
            successMessage = "已取消缓存并删除文件"
        )
    }

    private suspend fun removeCacheFiles(
        aid: Long,
        cid: Long,
        cancellationReason: String,
        successMessage: String,
    ): Result<String> {
        val targetKey = key(aid, cid)
        val activeJob = synchronized(this) {
            discardedTaskKeys.add(targetKey)
            removingTaskKeys.add(targetKey)
            val jobToWaitFor = if (
                currentRequest?.let { key(it.target.aid, it.target.cid) } == targetKey
            ) {
                currentJob?.also { it.cancel(CancellationException(cancellationReason)) }
            } else {
                null
            }
            queue.removeAll { key(it.target.aid, it.target.cid) == targetKey }
            pausedRequests.remove(targetKey)
            jobToWaitFor
        }

        val result = runCatching {
            // Deleting only before the writer exits leaves a race in which the download
            // coroutine can recreate a file or metadata after the directory was removed.
            // Finish the cancellation first, then perform and verify the final deletion.
            withContext(NonCancellable) {
                activeJob?.join()
                withContext(Dispatchers.IO) {
                    deleteEntryDirectory(aid, cid)
                }
                clearStateNow(aid, cid)
                refreshEntries()
            }
            successMessage
        }
        synchronized(this) {
            removingTaskKeys.remove(targetKey)
        }
        return result
    }

    private fun deleteEntryDirectory(aid: Long, cid: Long) {
        val dir = entryDir(aid, cid)
        if (dir.exists() && !dir.deleteRecursively()) {
            throw IOException("缓存文件删除失败")
        }
        if (dir.exists()) {
            throw IOException("缓存文件仍然存在")
        }
        dir.parentFile
            ?.takeIf { parent -> parent.listFiles()?.isEmpty() == true }
            ?.delete()
    }

    private suspend fun clearStateNow(aid: Long, cid: Long) {
        val targetKey = key(aid, cid)
        synchronized(danmakuCacheByKey) {
            danmakuCacheByKey.remove(targetKey)
        }
        withContext(Dispatchers.Main.immediate) {
            taskStates.remove(targetKey)
            entries.removeAll { it.aid == aid && it.cid == cid }
        }
    }

    private fun shouldAcceptStateLocked(state: OfflineVideoCacheTaskState): Boolean {
        val targetKey = key(state.aid, state.cid)
        return if (targetKey in discardedTaskKeys) {
            false
        } else if (state.status == OfflineVideoCacheStatus.Paused) {
            targetKey in pausedRequests
        } else {
            targetKey !in pausedRequests
        }
    }

    suspend fun refreshEntries() {
        val scannedEntries = withContext(Dispatchers.IO) {
            scanEntries()
        }
        val loadedEntries = scannedEntries
            .filter { it.completed && cacheFilesReady(it) }
            .sortedByDescending { it.updatedAt }
        val interruptedStates = scannedEntries
            .filterNot { it.completed && cacheFilesReady(it) }
            .map { it.toInterruptedState() }
        val activeKeys = synchronized(this) {
            buildSet {
                currentRequest?.target?.let { add(key(it.aid, it.cid)) }
                queue.forEach { add(key(it.target.aid, it.target.cid)) }
                pausedRequests.keys.forEach { add(it) }
            }
        }
        withContext(Dispatchers.Main.immediate) {
            entries.clear()
            entries.addAll(loadedEntries)
            val scannedKeys = scannedEntries.map { key(it.aid, it.cid) }.toSet()
            taskStates.keys.toList().forEach { stateKey ->
                val state = taskStates[stateKey]
                if (stateKey !in scannedKeys && stateKey !in activeKeys && state?.isActive != true) {
                    taskStates.remove(stateKey)
                }
            }
            loadedEntries.forEach { entry ->
                taskStates[key(entry.aid, entry.cid)] = OfflineVideoCacheTaskState(
                    aid = entry.aid,
                    cid = entry.cid,
                    title = entry.title,
                    partTitle = entry.partTitle,
                    cover = entry.cover,
                    qualityText = entry.qualityText,
                    status = OfflineVideoCacheStatus.Completed,
                    downloadedBytes = entry.totalBytes,
                    totalBytes = entry.totalBytes,
                    upName = entry.upName,
                    upFace = entry.upFace,
                    danmakuCount = entry.danmakuCount,
                    durationMs = entry.durationMs
                )
            }
            interruptedStates
                .filterNot { key(it.aid, it.cid) in activeKeys }
                .forEach { state ->
                    val currentState = taskStates[key(state.aid, state.cid)]
                    if (currentState?.isActive != true && currentState?.status != OfflineVideoCacheStatus.Paused) {
                        taskStates[key(state.aid, state.cid)] = state
                    }
                }
        }
    }

    fun getCompletedPlaybackSource(aid: Long, cid: Long): OfflineVideoPlaybackSource? {
        val entry = getCompletedEntry(aid, cid) ?: return null
        return OfflineVideoPlaybackSource(
            entry = entry,
            playData = entry.toPlayData()
        )
    }

    fun getCompletedEntry(aid: Long, cid: Long): OfflineVideoCacheEntry? {
        val entry = readEntry(entryDir(aid, cid)) ?: return null
        return entry.takeIf { it.completed && cacheFilesReady(it) }
    }

    fun getCachedCoverUri(entry: OfflineVideoCacheEntry): String? =
        getCachedImageUri(entry, entry.coverFileName)

    fun getCachedUpFaceUri(entry: OfflineVideoCacheEntry): String? =
        getCachedImageUri(entry, entry.upFaceFileName)

    fun getCompletedEntries(aid: Long): List<OfflineVideoCacheEntry> =
        scanEntries()
            .filter { it.aid == aid && it.completed && cacheFilesReady(it) }
            .sortedByDescending { it.updatedAt }

    fun getAllCompletedEntries(): List<OfflineVideoCacheEntry> =
        scanEntries()
            .filter { it.completed && cacheFilesReady(it) }
            .distinctBy { key(it.aid, it.cid) }
            .sortedByDescending { it.updatedAt }

    fun getCachedDanmakuSegment(
        aid: Long,
        cid: Long,
        segmentIndex: Int
    ): List<DanmakuData>? {
        val entry = getCompletedEntry(aid, cid) ?: return null
        val danmakuCache = readDanmakuCache(entry) ?: return null
        return danmakuCache.segments
            .firstOrNull { it.index == segmentIndex }
            ?.items
            ?.map { it.toDanmakuData() }
            ?: emptyList()
    }

    private fun startNextLocked() {
        if (currentJob?.isActive == true) return
        if (queue.isEmpty()) return

        val request = queue.removeFirst()
        currentRequest = request
        currentJob = serviceScope.launch {
            try {
                download(request)
            } finally {
                synchronized(this@OfflineVideoCacheService) {
                    if (currentRequest == request) {
                        currentRequest = null
                    }
                    currentJob = null
                    startNextLocked()
                }
            }
        }
    }

    private suspend fun resolveRequest(taskRequest: OfflineVideoCacheTaskRequest): OfflineVideoCacheRequest {
        val target = taskRequest.target
        val playData = videoPlayRepository.getDownloadPlayData(
            aid = target.aid,
            bvid = target.bvid.ifBlank { AvBvConverter.av2bv(target.aid) },
            cid = target.cid,
            qn = taskRequest.preferredQuality.code,
            tryLook1080P = taskRequest.tryLook1080P,
        ).requireOfflineCacheStreams().also { data ->
            logger.info {
                "Resolved offline cache play data: [aid=${target.aid}, cid=${target.cid}, qualities=${data.dashVideos.map { it.quality }.distinct()}, audios=${data.dashAudios.map { it.codecId }.distinct()}, dolby=${data.dolby?.codecId}, flac=${data.flac?.codecId}]"
            }
        }
        if (playData.needPay) {
            throw IllegalStateException("该分P需要购买或会员权限，无法缓存")
        }

        val selectedQuality = OfflineCacheQualitySelector.select(
            availableQualities = playData.dashVideos.mapNotNull { video ->
                Resolution.entries.find { it.code == video.quality }
            },
            preferredQuality = taskRequest.preferredQuality,
        ) ?: throw IllegalStateException("未找到可缓存的视频画质")
        val sameQualityVideos = playData.dashVideos.filter { it.quality == selectedQuality.code }
        val videoCandidates = sameQualityVideos.ifEmpty { playData.dashVideos }
        val videoItem = taskRequest.videoCodecPreferences.firstNotNullOfOrNull { codec ->
            videoCandidates.find { codec.matchesCodecString(it.codecs) }
        } ?: videoCandidates.firstOrNull()
        ?: throw IllegalStateException("未找到可缓存的视频流")
        val audioItem = playData.selectAudioForOfflineCache(taskRequest.preferredAudio)
            ?: throw IllegalStateException("未找到可缓存的音频流")

        return OfflineVideoCacheRequest(
            aid = target.aid,
            cid = target.cid,
            bvid = target.bvid.ifBlank { AvBvConverter.av2bv(target.aid) },
            title = target.title,
            partTitle = target.partTitle,
            cover = target.cover,
            upName = target.upName,
            quality = videoItem.quality,
            qualityText = Resolution.entries
                .find { it.code == videoItem.quality }
                ?.getDisplayName(BVApp.context)
                ?: "清晰度 ${videoItem.quality}",
            videoCodecId = videoItem.codecId,
            videoCodec = videoItem.codecs ?: VideoCodec.fromCodecId(videoItem.codecId).prefix,
            audioCodecId = audioItem.codecId,
            durationMs = target.durationMs.takeIf { it > 0L } ?: playData.timeLength,
            width = videoItem.width.takeIf { it > 0 } ?: target.width,
            height = videoItem.height.takeIf { it > 0 } ?: target.height,
            videoUrls = (listOf(videoItem.baseUrl) + videoItem.backUrl).toOfflineDownloadUrls(),
            audioUrls = (listOf(audioItem.baseUrl) + audioItem.backUrl).toOfflineDownloadUrls(),
            upFace = target.upFace,
            danmakuCount = target.danmakuCount
        )
    }

    private fun PlayData.requireOfflineCacheStreams(): PlayData {
        val audioCount = playableAudioCount()
        if (dashVideos.isEmpty() || audioCount == 0) {
            throw IllegalStateException(
                "WBI 未返回可缓存音视频流：video=${dashVideos.size}, audio=$audioCount, qualities=${dashVideos.map { it.quality }.distinct()}"
            )
        }
        return this
    }

    private fun PlayData.selectAudioForOfflineCache(preferredAudio: Audio): DashAudio? {
        return dashAudios.find { it.codecId == preferredAudio.code }
            ?: dolby.takeIf { it?.codecId == preferredAudio.code }
            ?: flac.takeIf { it?.codecId == preferredAudio.code }
            ?: dashAudios.minByOrNull { it.codecId }
            ?: dolby
            ?: flac
    }

    private suspend fun download(taskRequest: OfflineVideoCacheTaskRequest) {
        updateState(
            taskRequest.toState(
                status = OfflineVideoCacheStatus.Fetching,
                message = "正在获取缓存地址"
            )
        )
        val request = try {
            resolveRequest(taskRequest)
        } catch (_: CancellationException) {
            return
        } catch (error: Throwable) {
            val target = taskRequest.target
            logger.warn(error) {
                "Resolve offline cache request failed: [aid=${target.aid}, cid=${target.cid}]"
            }
            updateState(
                taskRequest.toState(
                    status = OfflineVideoCacheStatus.Failed,
                    message = error.localizedMessage ?: "获取缓存地址失败"
                )
            )
            return
        }

        downloadResolvedRequest(request)
    }

    private suspend fun downloadResolvedRequest(request: OfflineVideoCacheRequest) {
        val dir = entryDir(request.aid, request.cid)
        val videoFile = File(dir, VIDEO_FILE_NAME)
        val audioFile = File(dir, AUDIO_FILE_NAME)
        val danmakuFile = File(dir, DANMAKU_FILE_NAME)
        val coverFile = File(dir, COVER_FILE_NAME)
        val upFaceFile = File(dir, UP_FACE_FILE_NAME)
        val existingEntry = readEntry(dir)
        val createdAt = existingEntry?.createdAt ?: System.currentTimeMillis()

        runCatching {
            logger.info { "Start offline cache: [aid=${request.aid}, cid=${request.cid}, quality=${request.quality}, videoUrls=${request.videoUrls.size}, audioUrls=${request.audioUrls.size}]" }
            dir.mkdirs()
            if (existingEntry != null && !existingEntry.matchesMediaSelection(request)) {
                videoFile.delete()
                audioFile.delete()
            }
            danmakuFile.delete()
            coverFile.delete()
            upFaceFile.delete()
            synchronized(danmakuCacheByKey) {
                danmakuCacheByKey.remove(key(request.aid, request.cid))
            }
            writeEntry(
                dir = dir,
                request = request,
                totalBytes = 0L,
                createdAt = createdAt,
                completed = false,
                danmakuCached = false
            )

            updateState(request.toState(OfflineVideoCacheStatus.Fetching, message = "准备缓存"))
            val videoBytes = downloadFile(
                urls = request.videoUrls,
                target = videoFile,
                request = request,
                status = OfflineVideoCacheStatus.DownloadingVideo,
                message = "正在缓存视频"
            )
            val audioBytes = downloadFile(
                urls = request.audioUrls,
                target = audioFile,
                request = request,
                status = OfflineVideoCacheStatus.DownloadingAudio,
                message = "正在缓存音频"
            )
            validateMediaFile(videoFile, "视频")
            validateMediaFile(audioFile, "音频")
            val danmakuResult = cacheDanmaku(
                request = request,
                target = danmakuFile
            )
            val coverBytes = cacheMetadataImage(request.cover, coverFile, "封面")
            val upFaceBytes = cacheMetadataImage(request.upFace, upFaceFile, "UP 主头像")
            val completedRequest = request.copy(danmakuCount = danmakuResult.itemCount)
            val totalBytes = videoBytes + audioBytes + danmakuResult.bytes + coverBytes + upFaceBytes
            writeEntry(
                dir = dir,
                request = completedRequest,
                totalBytes = totalBytes,
                createdAt = createdAt,
                completed = true,
                danmakuCached = true
            )
            updateState(
                completedRequest.toState(
                    status = OfflineVideoCacheStatus.Completed,
                    downloadedBytes = totalBytes,
                    totalBytes = totalBytes,
                    message = "缓存完成"
                )
            )
            refreshEntries()
        }.onFailure { error ->
            if (error is CancellationException) {
                return
            }
            logger.warn(error) { "Offline cache failed: [aid=${request.aid}, cid=${request.cid}]" }
            updateState(
                request.toState(
                    status = OfflineVideoCacheStatus.Failed,
                    message = error.localizedMessage ?: "缓存失败"
                )
            )
        }
    }

    private suspend fun cacheDanmaku(
        request: OfflineVideoCacheRequest,
        target: File
    ): OfflineDanmakuCacheResult {
        val maxSegments = calculateDanmakuMaxSegments(request.durationMs)
        val segments = ArrayList<OfflineDanmakuSegment>(maxSegments)
        var totalItems = 0
        var xmlDanmakuCache: List<DanmakuData>? = null

        suspend fun xmlFallback(segmentIndex: Int): List<DanmakuData> {
            val allXmlDanmaku = xmlDanmakuCache ?: loadXmlDanmaku(request.cid)
                .also { xmlDanmakuCache = it }
            return allXmlDanmaku.filterSegment(segmentIndex)
        }

        updateState(
            request.toState(
                status = OfflineVideoCacheStatus.DownloadingDanmaku,
                downloadedBytes = 0L,
                totalBytes = maxSegments.toLong(),
                message = "正在缓存弹幕 0/$maxSegments"
            )
        )

        for (segmentIndex in 1..maxSegments) {
            currentCoroutineContext().ensureActive()
            val danmaku = fetchDanmakuSegmentForOffline(
                request = request,
                segmentIndex = segmentIndex,
                xmlFallback = { index -> xmlFallback(index) }
            )
            totalItems += danmaku.size
            segments.add(
                OfflineDanmakuSegment(
                    index = segmentIndex,
                    items = danmaku.map { it.toOfflineDanmakuItem() }
                )
            )
            updateState(
                request.toState(
                    status = OfflineVideoCacheStatus.DownloadingDanmaku,
                    downloadedBytes = segmentIndex.toLong(),
                    totalBytes = maxSegments.toLong(),
                    message = "正在缓存弹幕 $segmentIndex/$maxSegments"
                )
            )
        }

        val cache = OfflineDanmakuCacheFile(
            aid = request.aid,
            cid = request.cid,
            durationMs = request.durationMs,
            cachedAt = System.currentTimeMillis(),
            segments = segments
        )
        withContext(Dispatchers.IO) {
            target.parentFile?.mkdirs()
            target.writeText(compactJson.encodeToString(cache))
        }
        synchronized(danmakuCacheByKey) {
            danmakuCacheByKey[key(request.aid, request.cid)] = cache
        }
        logger.info {
            "Offline danmaku cached: [aid=${request.aid}, cid=${request.cid}, segments=${segments.size}, totalItems=$totalItems, bytes=${target.length()}]"
        }
        return OfflineDanmakuCacheResult(
            bytes = target.length(),
            itemCount = totalItems
        )
    }

    private suspend fun cacheMetadataImage(
        sourceUrl: String,
        target: File,
        label: String
    ): Long {
        if (sourceUrl.isBlank()) return 0L
        val normalizedUrl = sourceUrl.replaceFirst("http://", "https://")
        val connection = (URL(normalizedUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", DOWNLOAD_USER_AGENT)
            setRequestProperty("Referer", REFERER)
            downloadCookie().takeIf { it.isNotBlank() }?.let { setRequestProperty("Cookie", it) }
        }
        return try {
            connection.connect()
            if (connection.responseCode !in 200..299) {
                throw IOException("$label HTTP ${connection.responseCode}")
            }
            if (connection.contentType.orEmpty().isTextResponse()) {
                throw IOException("$label 返回了非图片内容")
            }
            target.parentFile?.mkdirs()
            FileOutputStream(target, false).buffered().use { output ->
                connection.inputStream.use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                    }
                }
            }
            target.length().takeIf { it > 0L } ?: throw IOException("$label 文件为空")
        } catch (error: CancellationException) {
            target.delete()
            throw error
        } catch (error: Exception) {
            target.delete()
            logger.debug(error) {
                "Cache offline metadata image failed: [label=$label, url=$normalizedUrl]"
            }
            0L
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun fetchDanmakuSegmentForOffline(
        request: OfflineVideoCacheRequest,
        segmentIndex: Int,
        xmlFallback: suspend (Int) -> List<DanmakuData>
    ): List<DanmakuData> {
        var lastFailure: Throwable? = null
        var sawSuccessfulResponse = false

        repeat(DANMAKU_WEB_EMPTY_IMMEDIATE_RETRIES + 1) { attemptIndex ->
            currentCoroutineContext().ensureActive()
            val webData = runCatching {
                BiliHttpApi.getDanmakuSeg(
                    cid = request.cid,
                    avid = request.aid,
                    segmentIndex = segmentIndex,
                    sessData = Prefs.sessData
                )
            }.onSuccess {
                sawSuccessfulResponse = true
            }.onFailure { error ->
                lastFailure = error
                if (error is CancellationException) throw error
                logger.warn {
                    "Offline danmaku web fetch failed aid=${request.aid} cid=${request.cid} segmentIndex=$segmentIndex: ${error.message}"
                }
            }.getOrDefault(emptyList())

            if (webData.isNotEmpty()) {
                return webData
            }
            if (attemptIndex < DANMAKU_WEB_EMPTY_IMMEDIATE_RETRIES) {
                delay(DANMAKU_WEB_EMPTY_RETRY_DELAY_MS)
            }
        }

        val appData = runCatching {
            videoPlayRepository.getAppDanmakuSegment(
                aid = request.aid,
                cid = request.cid,
                segmentIndex = segmentIndex
            )
        }.onSuccess {
            sawSuccessfulResponse = true
        }.onFailure { error ->
            lastFailure = error
            if (error is CancellationException) throw error
            logger.warn {
                "Offline danmaku app fetch failed aid=${request.aid} cid=${request.cid} segmentIndex=$segmentIndex: ${error.message}"
            }
        }.getOrDefault(emptyList())

        if (appData.isNotEmpty()) {
            return appData
        }

        val xmlData = runCatching {
            xmlFallback(segmentIndex)
        }.onSuccess {
            sawSuccessfulResponse = true
        }.onFailure { error ->
            lastFailure = error
            if (error is CancellationException) throw error
            logger.warn {
                "Offline danmaku xml fetch failed aid=${request.aid} cid=${request.cid} segmentIndex=$segmentIndex: ${error.message}"
            }
        }.getOrDefault(emptyList())

        if (!sawSuccessfulResponse && lastFailure != null) {
            throw IOException(lastFailure.localizedMessage ?: "弹幕缓存失败", lastFailure)
        }
        return xmlData
    }

    @Suppress("DEPRECATION")
    private suspend fun loadXmlDanmaku(cid: Long): List<DanmakuData> {
        return BiliHttpApi.getDanmakuXml(
            cid = cid,
            sessData = Prefs.sessData
        ).data
    }

    private suspend fun downloadFile(
        urls: List<String>,
        target: File,
        request: OfflineVideoCacheRequest,
        status: OfflineVideoCacheStatus,
        message: String
    ): Long {
        val candidates = urls
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        if (candidates.isEmpty()) {
            throw IOException("没有可用下载地址")
        }

        var lastError: Throwable? = null
        for ((index, url) in candidates.withIndex()) {
            if (index > 0 && target.exists()) {
                target.delete()
            }
            val lineMessage = if (candidates.size > 1) {
                "$message 线路${index + 1}/${candidates.size}"
            } else {
                message
            }
            try {
                return downloadSingleFile(
                    url = url,
                    target = target,
                    request = request,
                    status = status,
                    message = lineMessage
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                lastError = error
                logger.warn(error) {
                    "Offline cache line failed: [aid=${request.aid}, cid=${request.cid}, status=$status, line=${index + 1}/${candidates.size}, host=${runCatching { URL(url).host }.getOrDefault("")}]"
                }
                updateState(
                    request.toState(
                        status = status,
                        message = "线路${index + 1}失败：${error.localizedMessage ?: "下载失败"}"
                    )
                )
            }
        }
        throw IOException(lastError?.localizedMessage ?: "下载失败", lastError)
    }

    private suspend fun downloadSingleFile(
        url: String,
        target: File,
        request: OfflineVideoCacheRequest,
        status: OfflineVideoCacheStatus,
        message: String
    ): Long = withContext(Dispatchers.IO) {
        target.parentFile?.mkdirs()
        var existingBytes = if (target.exists()) target.length() else 0L
        val sourceUrl = URL(url)
        val cookie = downloadCookie()
        val rangeHeader = "bytes=$existingBytes-"
        val connection = (sourceUrl.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", DOWNLOAD_USER_AGENT)
            setRequestProperty("Referer", REFERER)
            setRequestProperty("Accept", "*/*")
            setRequestProperty("Accept-Encoding", "identity")
            setRequestProperty("Connection", "keep-alive")
            if (cookie.isNotBlank()) {
                setRequestProperty("Cookie", cookie)
            }
            setRequestProperty("Range", rangeHeader)
        }

        try {
            val responseCode = connection.responseCode
            val contentType = connection.contentType.orEmpty()
            logger.info {
                "Offline cache response: [aid=${request.aid}, cid=${request.cid}, status=$status, code=$responseCode, contentType=$contentType, host=${sourceUrl.host}, range=$rangeHeader, cookie=${cookie.isNotBlank()}, userAgent=$DOWNLOAD_USER_AGENT]"
            }
            if (responseCode == HTTP_RANGE_NOT_SATISFIABLE) {
                if (existingBytes <= 0L) {
                    throw IOException("HTTP $HTTP_RANGE_NOT_SATISFIABLE ${sourceUrl.host}")
                }
                validateMediaFile(target, "缓存")
                return@withContext existingBytes
            }
            if (responseCode !in 200..299) {
                throw IOException("HTTP $responseCode ${sourceUrl.host}")
            }
            if (contentType.isTextResponse()) {
                throw IOException("下载地址返回了非媒体内容：$contentType")
            }
            val append = existingBytes > 0L && responseCode == HttpURLConnection.HTTP_PARTIAL
            if (!append) existingBytes = 0L

            val contentLength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connection.contentLengthLong
            } else {
                connection.getHeaderField("Content-Length")?.toLongOrNull()
                    ?: connection.contentLength.toLong()
            }.takeIf { it > 0L } ?: 0L
            val totalBytes = (contentLength + existingBytes).takeIf { it > 0L } ?: 0L
            var downloadedBytes = existingBytes
            updateState(
                request.toState(
                    status = status,
                    downloadedBytes = downloadedBytes,
                    totalBytes = totalBytes,
                    message = message
                )
            )

            connection.inputStream.use { input ->
                val output = FileOutputStream(target, append)
                output.buffered().use {
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var lastUpdate = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        it.write(buffer, 0, read)
                        downloadedBytes += read
                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > PROGRESS_UPDATE_INTERVAL_MS) {
                            lastUpdate = now
                            updateState(
                                request.toState(
                                    status = status,
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes,
                                    message = message
                                )
                            )
                        }
                    }
                }
            }
            validateMediaFile(target, "缓存")
            downloadedBytes
        } finally {
            connection.disconnect()
        }
    }

    private fun writeEntry(
        dir: File,
        request: OfflineVideoCacheRequest,
        totalBytes: Long,
        createdAt: Long,
        completed: Boolean,
        danmakuCached: Boolean
    ) {
        val now = System.currentTimeMillis()
        val entry = OfflineVideoCacheEntry(
            aid = request.aid,
            cid = request.cid,
            bvid = request.bvid,
            title = request.title,
            partTitle = request.partTitle,
            cover = request.cover,
            upName = request.upName,
            quality = request.quality,
            qualityText = request.qualityText,
            videoCodecId = request.videoCodecId,
            videoCodec = request.videoCodec,
            audioCodecId = request.audioCodecId,
            durationMs = request.durationMs,
            width = request.width,
            height = request.height,
            videoFileName = VIDEO_FILE_NAME,
            audioFileName = AUDIO_FILE_NAME,
            danmakuFileName = DANMAKU_FILE_NAME,
            totalBytes = totalBytes,
            createdAt = createdAt,
            updatedAt = now,
            completed = completed,
            danmakuCached = danmakuCached,
            upFace = request.upFace,
            danmakuCount = request.danmakuCount,
            coverFileName = COVER_FILE_NAME.takeIf { File(dir, COVER_FILE_NAME).length() > 0L }.orEmpty(),
            upFaceFileName = UP_FACE_FILE_NAME.takeIf { File(dir, UP_FACE_FILE_NAME).length() > 0L }.orEmpty()
        )
        File(dir, ENTRY_FILE_NAME).writeText(json.encodeToString(entry))
    }

    private fun readEntry(dir: File): OfflineVideoCacheEntry? {
        val entryFile = File(dir, ENTRY_FILE_NAME)
        if (!entryFile.exists()) return null
        return runCatching {
            json.decodeFromString<OfflineVideoCacheEntry>(entryFile.readText())
        }.getOrNull()
    }

    private fun scanEntries(): List<OfflineVideoCacheEntry> {
        val root = rootDir()
        if (!root.exists()) return emptyList()
        return root.walkTopDown()
            .filter { it.isFile && it.name == ENTRY_FILE_NAME }
            .mapNotNull { readEntry(it.parentFile ?: return@mapNotNull null) }
            .sortedByDescending { it.updatedAt }
            .toList()
    }

    private fun OfflineVideoCacheEntry.toInterruptedState(): OfflineVideoCacheTaskState {
        val dir = entryDir(aid, cid)
        val video = File(dir, videoFileName)
        val audio = File(dir, audioFileName)
        val danmaku = File(dir, danmakuFileName.ifBlank { DANMAKU_FILE_NAME })
        val downloadedBytes = listOf(video, audio, danmaku)
            .filter { it.exists() }
            .sumOf { it.length() }
        return OfflineVideoCacheTaskState(
            aid = aid,
            cid = cid,
            title = title,
            partTitle = partTitle,
            cover = cover,
            qualityText = qualityText,
            status = OfflineVideoCacheStatus.Failed,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes.takeIf { it > 0L } ?: downloadedBytes,
            upName = upName,
            upFace = upFace,
            danmakuCount = danmakuCount,
            durationMs = durationMs,
            message = if (completed) {
                "缓存文件缺失，请删除后重新缓存"
            } else {
                "缓存未完成，请清除后重新缓存"
            }
        )
    }

    private fun shouldResetFailedCache(target: OfflineVideoCacheTarget): Boolean {
        if (taskStates[key(target.aid, target.cid)]?.status == OfflineVideoCacheStatus.Failed) {
            return true
        }
        val entry = readEntry(entryDir(target.aid, target.cid)) ?: return false
        return !entry.completed || !cacheFilesReady(entry)
    }

    private fun OfflineVideoCacheEntry.matchesMediaSelection(request: OfflineVideoCacheRequest): Boolean =
        aid == request.aid &&
            cid == request.cid &&
            quality == request.quality &&
            videoCodecId == request.videoCodecId &&
            audioCodecId == request.audioCodecId

    private fun cacheFilesReady(entry: OfflineVideoCacheEntry): Boolean {
        return mediaFilesReady(entry) && (!entry.danmakuCached || danmakuFileReady(entry))
    }

    private fun mediaFilesReady(entry: OfflineVideoCacheEntry): Boolean {
        val dir = entryDir(entry.aid, entry.cid)
        val video = File(dir, entry.videoFileName)
        val audio = File(dir, entry.audioFileName)
        return isValidMediaFile(video) && isValidMediaFile(audio)
    }

    private fun danmakuFileReady(entry: OfflineVideoCacheEntry): Boolean {
        val danmakuFile = File(entryDir(entry.aid, entry.cid), entry.danmakuFileName.ifBlank { DANMAKU_FILE_NAME })
        return danmakuFile.exists() && danmakuFile.length() > 0L
    }

    private fun readDanmakuCache(entry: OfflineVideoCacheEntry): OfflineDanmakuCacheFile? {
        val cacheKey = key(entry.aid, entry.cid)
        synchronized(danmakuCacheByKey) {
            danmakuCacheByKey[cacheKey]?.let { return it }
        }

        val danmakuFile = File(entryDir(entry.aid, entry.cid), entry.danmakuFileName.ifBlank { DANMAKU_FILE_NAME })
        if (!danmakuFile.exists()) return null

        return runCatching {
            compactJson.decodeFromString<OfflineDanmakuCacheFile>(danmakuFile.readText())
        }.onSuccess { cache ->
            synchronized(danmakuCacheByKey) {
                danmakuCacheByKey[cacheKey] = cache
            }
        }.onFailure { error ->
            logger.warn(error) { "Read offline danmaku cache failed: [aid=${entry.aid}, cid=${entry.cid}]" }
        }.getOrNull()
    }

    private fun OfflineVideoCacheEntry.toPlayData(): PlayData {
        val dir = entryDir(aid, cid)
        val videoUri = Uri.fromFile(File(dir, videoFileName)).toString()
        val audioUri = Uri.fromFile(File(dir, audioFileName)).toString()
        val video = DashVideo(
            quality = quality,
            baseUrl = videoUri,
            bandwidth = 0,
            codecId = videoCodecId,
            width = width,
            height = height,
            frameRate = "",
            backUrl = emptyList(),
            codecs = videoCodec
        )
        val audio = DashAudio(
            baseUrl = audioUri,
            bandwidth = 0,
            codecId = audioCodecId,
            backUrl = emptyList()
        )
        return PlayData(
            dashVideos = listOf(video),
            dashAudios = listOf(audio),
            codec = mapOf(quality to listOf(videoCodec)),
            needPay = false,
            timeLength = durationMs
        )
    }

    private fun calculateDanmakuMaxSegments(durationMs: Long): Int {
        return if (durationMs > 0L) {
            ceil(durationMs / DANMAKU_SEGMENT_DURATION_MS.toDouble()).toInt().coerceAtLeast(1)
        } else {
            DEFAULT_DANMAKU_MAX_SEGMENTS
        }
    }

    private fun List<DanmakuData>.filterSegment(segmentIndex: Int): List<DanmakuData> {
        val segmentStartMs = ((segmentIndex - 1).coerceAtLeast(0)) * DANMAKU_SEGMENT_DURATION_MS
        val segmentEndMs = segmentStartMs + DANMAKU_SEGMENT_DURATION_MS
        return filter { danmaku ->
            val positionMs = (danmaku.time * 1000).toLong()
            positionMs in segmentStartMs until segmentEndMs
        }
    }

    private fun DanmakuData.toOfflineDanmakuItem(): OfflineDanmakuItem =
        OfflineDanmakuItem(
            time = time,
            type = type,
            size = size,
            color = color,
            timestamp = timestamp,
            pool = pool,
            midHash = midHash,
            dmid = dmid,
            level = level,
            text = text
        )

    private fun OfflineDanmakuItem.toDanmakuData(): DanmakuData =
        DanmakuData(
            time = time,
            type = type,
            size = size,
            color = color,
            timestamp = timestamp,
            pool = pool,
            midHash = midHash,
            dmid = dmid,
            level = level,
            text = text
        )

    private fun OfflineVideoCacheRequest.toState(
        status: OfflineVideoCacheStatus,
        downloadedBytes: Long = 0L,
        totalBytes: Long = 0L,
        message: String = ""
    ) = OfflineVideoCacheTaskState(
        aid = aid,
        cid = cid,
        title = title,
        partTitle = partTitle,
        cover = cover,
        qualityText = qualityText,
        status = status,
        downloadedBytes = downloadedBytes,
        totalBytes = totalBytes,
        message = message,
        upName = upName,
        upFace = upFace,
        danmakuCount = danmakuCount,
        durationMs = durationMs
    )

    private fun OfflineVideoCacheTaskRequest.toState(
        status: OfflineVideoCacheStatus,
        downloadedBytes: Long = 0L,
        totalBytes: Long = 0L,
        message: String = ""
    ): OfflineVideoCacheTaskState {
        val cacheTarget = target
        return OfflineVideoCacheTaskState(
            aid = cacheTarget.aid,
            cid = cacheTarget.cid,
            title = cacheTarget.title,
            partTitle = cacheTarget.partTitle,
            cover = cacheTarget.cover,
            qualityText = preferredQuality.getDisplayName(BVApp.context),
            status = status,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            message = message,
            upName = cacheTarget.upName,
            upFace = cacheTarget.upFace,
            danmakuCount = cacheTarget.danmakuCount,
            durationMs = cacheTarget.durationMs
        )
    }

    private fun updateState(state: OfflineVideoCacheTaskState) {
        val acceptedState = synchronized(this) {
            if (!shouldAcceptStateLocked(state)) {
                null
            } else {
                val targetKey = key(state.aid, state.cid)
                state.withNonRegressingProgress(latestTaskStates[targetKey]).also {
                    latestTaskStates[targetKey] = it
                }
            }
        } ?: return
        serviceScope.launch(Dispatchers.Main.immediate) {
            val targetKey = key(acceptedState.aid, acceptedState.cid)
            if (synchronized(this@OfflineVideoCacheService) { shouldAcceptStateLocked(acceptedState) }) {
                taskStates[targetKey] = acceptedState
            }
        }
    }

    private fun OfflineVideoCacheTaskState.withNonRegressingProgress(
        previous: OfflineVideoCacheTaskState?,
    ): OfflineVideoCacheTaskState {
        if (previous == null || previous.status == OfflineVideoCacheStatus.Completed) return this
        val sameDownloadStage = status == previous.status ||
            status == OfflineVideoCacheStatus.Paused ||
            status == OfflineVideoCacheStatus.Queued
        if (!sameDownloadStage || previous.downloadedBytes <= downloadedBytes) return this
        return copy(
            downloadedBytes = previous.downloadedBytes,
            totalBytes = previous.totalBytes.takeIf { it > 0L } ?: totalBytes
        )
    }

    private fun rootDir(): File = File(BVApp.context.filesDir, ROOT_DIR_NAME)

    private fun entryDir(aid: Long, cid: Long): File =
        File(rootDir(), "av_$aid/c_$cid")

    private fun getCachedImageUri(entry: OfflineVideoCacheEntry, fileName: String): String? {
        if (fileName.isBlank()) return null
        return File(entryDir(entry.aid, entry.cid), fileName)
            .takeIf { it.isFile && it.length() > 0L }
            ?.let { Uri.fromFile(it).toString() }
    }

    private fun downloadCookie(): String {
        val cookieParts = mutableListOf<String>()
        authRepository.sessionData
            ?.takeIf { it.isNotBlank() }
            ?.let { cookieParts.add("SESSDATA=$it") }
        authRepository.mid
            ?.takeIf { it > 0L }
            ?.let { cookieParts.add("DedeUserID=$it") }
        Prefs.uidCkMd5
            .takeIf { it.isNotBlank() }
            ?.let { cookieParts.add("DedeUserID__ckMd5=$it") }
        authRepository.biliJct
            ?.takeIf { it.isNotBlank() }
            ?.let { cookieParts.add("bili_jct=$it") }
        Prefs.sid
            .takeIf { it.isNotBlank() }
            ?.let { cookieParts.add("sid=$it") }
        authRepository.buvid3
            ?.takeIf { it.isNotBlank() }
            ?.let { cookieParts.add("buvid3=$it") }
        return cookieParts
            .joinToString(";")
            .let { if (it.isBlank()) it else "$it;" }
    }

    private fun validateMediaFile(file: File, label: String) {
        if (!isValidMediaFile(file)) {
            throw IOException("${label}文件无效，请删除后重新缓存")
        }
    }

    private fun isValidMediaFile(file: File): Boolean {
        if (!file.exists() || file.length() < MIN_MEDIA_FILE_BYTES) return false
        return runCatching {
            val header = ByteArray(MP4_BOX_HEADER_SIZE)
            file.inputStream().use { input ->
                input.read(header) == MP4_BOX_HEADER_SIZE
            } && String(header, 4, 4, Charsets.US_ASCII) in VALID_MP4_BOX_TYPES
        }.getOrDefault(false)
    }

    private fun Iterable<String>.toOfflineDownloadUrls(): List<String> {
        val rawUrls = map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        val sourceUrls = rawUrls
            .map { it.toHttpsDownloadUrl() }
            .distinct()
        if (sourceUrls.isEmpty()) return emptyList()

        val primaryUrl = sourceUrls.selectOfflineCdnUrl()
        return (listOf(primaryUrl) + sourceUrls + rawUrls)
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun List<String>.selectOfflineCdnUrl(): String {
        var mcdnTf: String? = null
        var mcdnUpgcxcode: String? = null
        var last = first()

        for (url in this) {
            last = url
            if (OFFLINE_MIRROR_CDN_REGEX.containsMatchIn(url)) {
                val uri = Uri.parse(url)
                if (uri.getQueryParameter("os") == "mcdn") {
                    mcdnUpgcxcode = url
                } else {
                    return uri.toOfflineAliCdnUrl()
                }
            }
            if (OFFLINE_MCDN_TF_REGEX.containsMatchIn(url)) {
                mcdnTf = url
                continue
            }
            if (url.contains("/upgcxcode/")) {
                mcdnUpgcxcode = url
                continue
            }
            if (url.contains("szbdyd.com")) {
                val uri = Uri.parse(url)
                val host = uri.getQueryParameter("xy_usource") ?: OFFLINE_DOWNLOAD_CDN_HOST
                return uri.buildUpon()
                    .scheme("https")
                    .authority(host)
                    .build()
                    .toString()
            }
        }

        return when {
            mcdnUpgcxcode != null -> Uri.parse(mcdnUpgcxcode).toOfflineAliCdnUrl()
            mcdnTf != null -> Uri.Builder()
                .scheme("https")
                .authority(OFFLINE_DOWNLOAD_PROXY_TF_HOST)
                .appendQueryParameter("url", mcdnTf)
                .build()
                .toString()
            else -> last
        }
    }

    private fun String.toHttpsDownloadUrl(): String =
        if (startsWith("http://")) "https://${removePrefix("http://")}" else this

    private fun Uri.toOfflineAliCdnUrl(): String = buildUpon()
        .scheme("https")
        .authority(OFFLINE_DOWNLOAD_CDN_HOST)
        .build()
        .toString()

    private fun String.isTextResponse(): Boolean {
        val normalized = lowercase()
        return normalized.contains("text/html") ||
            normalized.contains("text/plain") ||
            normalized.contains("application/json") ||
            normalized.contains("application/xml")
    }

    companion object {
        private const val ROOT_DIR_NAME = "offline_video_cache"
        private const val ENTRY_FILE_NAME = "entry.json"
        private const val VIDEO_FILE_NAME = "video.m4s"
        private const val AUDIO_FILE_NAME = "audio.m4s"
        private const val DANMAKU_FILE_NAME = "danmaku.json"
        private const val COVER_FILE_NAME = "cover.img"
        private const val UP_FACE_FILE_NAME = "up_face.img"
        private const val OFFLINE_DOWNLOAD_CDN_HOST = "upos-sz-mirrorali.bilivideo.com"
        private const val OFFLINE_DOWNLOAD_PROXY_TF_HOST = "proxy-tf-all-ws.bilivideo.com"
        private const val DOWNLOAD_USER_AGENT = "Dart/3.6 (dart:io)"
        private const val REFERER = "https://www.bilibili.com/"
        private const val PROGRESS_UPDATE_INTERVAL_MS = 500L
        private const val DANMAKU_SEGMENT_DURATION_MS = 6 * 60 * 1000L
        private const val DEFAULT_DANMAKU_MAX_SEGMENTS = 20
        private const val DANMAKU_WEB_EMPTY_IMMEDIATE_RETRIES = 2
        private const val DANMAKU_WEB_EMPTY_RETRY_DELAY_MS = 180L
        private const val HTTP_RANGE_NOT_SATISFIABLE = 416
        private const val MIN_MEDIA_FILE_BYTES = 1024L
        private const val MP4_BOX_HEADER_SIZE = 8
        private val VALID_MP4_BOX_TYPES = setOf("ftyp", "styp", "sidx", "moof")
        private val OFFLINE_MIRROR_CDN_REGEX = Regex(
            """^https?://(?:upos-\w+-(?!302)\w+|(?:upos|proxy)-tf-[^/]+)\.(?:bilivideo|akamaized)\.(?:com|net)/upgcxcode"""
        )
        private val OFFLINE_MCDN_TF_REGEX = Regex(
            """^https?://(?:(?:(?:\d{1,3}\.){3}\d{1,3}|[^/]+\.mcdn\.bilivideo\.(?:com|cn|net))(?::\d{1,5})?/v\d/resource)"""
        )
    }
}
