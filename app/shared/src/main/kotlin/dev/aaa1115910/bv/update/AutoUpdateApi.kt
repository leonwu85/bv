package dev.aaa1115910.bv.update

import dev.aaa1115910.biliapi.BiliApiConstants
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.util.Prefs
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.UserAgent
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import okhttp3.OkHttpClient
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class AutoUpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val changelog: String,
    val downloadPageUrl: String = AutoUpdateApi.GITHUB_RELEASE_PAGE_URL
)

object AutoUpdateApi {
    const val GITHUB_RELEASE_PAGE_URL = "https://github.com/leonwu85/bv/releases/latest"
    private const val RELEASE_TEXT_URL =
        "https://pub-9b9e14b498254ce7a2724c093e3554de.r2.dev/release.txt"

    private val client = HttpClient(OkHttp) {
        engine {
            preconfigured = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()
        }
        install(UserAgent) {
            agent = BiliApiConstants.USER_AGENT_WEB
        }
    }

    suspend fun getLatestUpdateInfo(): AutoUpdateInfo {
        val releaseText = client.get(RELEASE_TEXT_URL).bodyAsText()
        return parseReleaseText(releaseText)
    }

    fun parseReleaseText(releaseText: String): AutoUpdateInfo {
        val lines = releaseText
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .lines()
        val versionLineIndex = lines.indexOfFirst { it.isNotBlank() }
        check(versionLineIndex >= 0) { "Release text is empty" }

        val versionName = lines[versionLineIndex].trim()
        val versionCode = parseVersionCode(versionName)
        val changelog = lines
            .drop(versionLineIndex + 1)
            .joinToString("\n")
            .trim()
            .ifBlank { "暂无更新内容" }

        return AutoUpdateInfo(
            versionName = versionName,
            versionCode = versionCode,
            changelog = changelog
        )
    }

    private fun parseVersionCode(versionName: String): Int {
        val bvVersionCode = Regex("""^BV_(\d+)_""")
            .find(versionName)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
        if (bvVersionCode != null) return bvVersionCode

        val revisionVersionCode = Regex("""(?:^|[._-])r(\d+)(?:[._-]|$)""")
            .find(versionName)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
        check(revisionVersionCode != null) { "Cannot parse version code from $versionName" }
        return revisionVersionCode
    }
}

object AutoUpdateChecker {
    suspend fun checkOnceDaily(): AutoUpdateInfo? {
        val today = currentDayKey()
        if (!BuildConfig.DEBUG && Prefs.lastAutoUpdateCheckDay == today) return null

        if (!BuildConfig.DEBUG) {
            Prefs.lastAutoUpdateCheckDay = today
        }
        val updateInfo = AutoUpdateApi.getLatestUpdateInfo()
        return updateInfo.takeIf { BuildConfig.DEBUG || it.versionCode > BuildConfig.VERSION_CODE }
    }

    private fun currentDayKey(): Long {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.YEAR) * 1000L + calendar.get(Calendar.DAY_OF_YEAR)
    }
}
