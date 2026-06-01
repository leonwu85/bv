package dev.aaa1115910.bv.network

import dev.aaa1115910.biliapi.BiliApiConstants
import io.ktor.client.HttpClient
import io.ktor.client.content.ProgressListener
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import java.io.File

object MpvLibsApi {
    private const val OFFICIAL_LATEST_DOWNLOAD_URL =
        "https://github.com/mpv-android/mpv-android/releases/latest/download"
    private val githubProxyPrefixes = listOf(
        "https://gh.llkk.cc/",
        "https://gh-proxy.com/",
        "https://gh-proxy.net/",
        "https://mirror.ghproxy.com/"
    )
    private lateinit var client: HttpClient

    init {
        createClient()
    }

    private fun createClient() {
        client = HttpClient(OkHttp) {
            install(UserAgent) {
                agent = BiliApiConstants.USER_AGENT_WEB
            }
            install(ContentEncoding) {
                deflate(1.0F)
                gzip(0.9F)
            }
        }
    }

    suspend fun downloadLatestApk(
        targetAbi: String,
        targetFile: File,
        onProgress: ProgressListener
    ): String {
        val downloadUrls = getLatestApkDownloadUrls(targetAbi)
        var lastException: Exception? = null

        for (downloadUrl in downloadUrls) {
            try {
                client.prepareRequest {
                    url(downloadUrl)
                    onDownload(onProgress)
                }.execute { response ->
                    if (response.status.value in 200..299) {
                        response.bodyAsChannel().copyAndClose(targetFile.writeChannel())
                    } else {
                        throw IllegalStateException("HTTP ${response.status.value}")
                    }
                }

                if (targetFile.length() <= 0L) {
                    throw IllegalStateException("Downloaded APK is empty")
                }
                return "latest"
            } catch (error: Exception) {
                lastException = error
                targetFile.delete()
            }
        }

        throw lastException ?: IllegalStateException("All MPV download mirrors failed")
    }

    private fun getLatestApkDownloadUrls(targetAbi: String): List<String> {
        return listOf(
            "app-default-$targetAbi-release.apk",
            "app-default-universal-release.apk",
            "app-api29-universal-release.apk"
        )
            .distinct()
            .flatMap { fileName ->
                val officialUrl = "$OFFICIAL_LATEST_DOWNLOAD_URL/$fileName"
                listOf(officialUrl) + githubProxyPrefixes.map { proxy -> proxy + officialUrl }
            }
    }
}
