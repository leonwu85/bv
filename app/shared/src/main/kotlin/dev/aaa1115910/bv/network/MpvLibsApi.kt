package dev.aaa1115910.bv.network

import dev.aaa1115910.biliapi.BiliApiConstants
import dev.aaa1115910.bv.player.impl.mpv.MpvLibsInstaller
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
    private const val OFFICIAL_RELEASE_DOWNLOAD_BASE_URL =
        "https://github.com/mpv-android/mpv-android/releases/download"
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

    /** The mpv-android release tag this app build is pinned to. */
    val pinnedReleaseTag: String
        get() = MpvLibsInstaller.expectedVersion

    /**
     * Downloads the pinned mpv-android release APK for [targetAbi] and returns the release tag that
     * should be recorded as the installed version. The caller must verify the APK signature before
     * extracting anything from it (see [MpvLibsInstaller.installFromApk]).
     */
    suspend fun downloadPinnedApk(
        targetAbi: String,
        targetFile: File,
        onProgress: ProgressListener
    ): String {
        val tag = pinnedReleaseTag
        val downloadUrls = getApkDownloadUrls(tag, targetAbi)
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
                return tag
            } catch (error: Exception) {
                lastException = error
                targetFile.delete()
            }
        }

        throw lastException ?: IllegalStateException("All MPV download mirrors failed")
    }

    private fun getApkDownloadUrls(tag: String, targetAbi: String): List<String> {
        return listOf(
            "app-default-$targetAbi-release.apk",
            "app-default-universal-release.apk",
            "app-api29-universal-release.apk"
        )
            .distinct()
            .flatMap { fileName ->
                val officialUrl = "$OFFICIAL_RELEASE_DOWNLOAD_BASE_URL/$tag/$fileName"
                listOf(officialUrl) + githubProxyPrefixes.map { proxy -> proxy + officialUrl }
            }
    }
}
