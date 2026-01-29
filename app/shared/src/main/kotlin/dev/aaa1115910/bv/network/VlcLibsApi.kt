package dev.aaa1115910.bv.network

import android.os.Build
import dev.aaa1115910.bv.network.entity.Release
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.content.ProgressListener
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.serialization.json.Json
import java.io.File

object VlcLibsApi {
    private var endPoint = "api.github.com"

    // Maven 仓库镜像源列表（按优先级排序）
    private val MAVEN_MIRRORS = listOf(
        "https://maven.aliyun.com/repository/public",           // 阿里云镜像（国内首选）
        "https://repo1.maven.org/maven2"                        // Maven Central 官方源（备用）
    )

    private const val GROUP_PATH = "org/videolan/android"
    private const val ARTIFACT_ID = "libvlc-all"
    private lateinit var client: HttpClient

    init {
        createClient()
    }

    private fun createClient() {
        client = HttpClient(OkHttp) {
            install(UserAgent) {
                agent = dev.aaa1115910.biliapi.BiliApiConstants.USER_AGENT_WEB
            }
            install(ContentNegotiation) {
                json(Json {
                    coerceInputValues = true
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }
            install(ContentEncoding) {
                deflate(1.0F)
                gzip(0.9F)
            }
            defaultRequest {
                url {
                    protocol = URLProtocol.HTTPS
                    host = endPoint
                }
            }
        }
    }

    suspend fun getReleases(): List<Release> {
        val result = mutableListOf<Release>()

        runCatching {
            result.addAll(
                client.get("/repos/aaa1115910/bv-libs/releases").body<List<Release>>()
            )
        }

        return result
    }

    suspend fun getRelease(vlcVersion: String): Release? {
        return getReleases().firstOrNull { it.tagName == "libvlc-${vlcVersion}" }
    }

    suspend fun downloadFile(
        releaseItem: Release,
        file: File,
        downloadListener: ProgressListener
    ) {
        val fileName = getFileName()
        if (fileName == "") throw IllegalStateException("Not supported abi")

        val downloadUrl = releaseItem.assets
            .firstOrNull { it.name == fileName }?.browserDownloadUrl
            ?: throw IllegalStateException("Not found download url")
        client.prepareRequest {
            url(downloadUrl)
            onDownload(downloadListener)
        }.execute { response ->
            response.bodyAsChannel().copyAndClose(file.writeChannel())
        }
    }

    private fun getFileName(): String {
        return if (Build.SUPPORTED_ABIS.contains("x86_64")) {
            "x86_64.zip"
        } else if (Build.SUPPORTED_ABIS.contains("x86")) {
            "x86.zip"
        } else if (Build.SUPPORTED_ABIS.contains("arm64-v8a")) {
            "arm64-v8a.zip"
        } else if (Build.SUPPORTED_ABIS.contains("armeabi-v7a")) {
            "armeabi-v7a.zip"
        } else {
            ""
        }
    }

    /**
     * 获取 AAR 下载地址列表（多镜像源）
     */
    fun getAarDownloadUrls(version: String): List<String> {
        return MAVEN_MIRRORS.map { baseUrl ->
            "$baseUrl/$GROUP_PATH/$ARTIFACT_ID/$version/$ARTIFACT_ID-$version.aar"
        }
    }

    /**
     * 下载 AAR 文件（自动尝试多个镜像源）
     */
    suspend fun downloadAar(
        version: String,
        targetFile: File,
        onProgress: (bytesReceived: Long, contentLength: Long) -> Unit
    ) {
        val downloadUrls = getAarDownloadUrls(version)
        var lastException: Exception? = null

        for (downloadUrl in downloadUrls) {
            try {
                client.prepareRequest {
                    url(downloadUrl)
                    onDownload { received, total ->
                        onProgress(received, total ?: 0L)
                    }
                }.execute { response ->
                    if (response.status.value in 200..299) {
                        response.bodyAsChannel().copyAndClose(targetFile.writeChannel())
                    } else {
                        throw IllegalStateException("HTTP ${response.status.value}")
                    }
                }
                return // 下载成功，直接返回
            } catch (e: Exception) {
                lastException = e
                // 当前镜像源失败，继续尝试下一个
                continue
            }
        }

        // 所有镜像源都失败
        throw lastException ?: IllegalStateException("All mirrors failed")
    }
}

