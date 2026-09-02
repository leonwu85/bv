import java.io.File

data class AppVersion(
    val code: Int,
    val name: String
)

object AppConfiguration {
    const val appId = "dev.aaa1115910.bv"
    private const val defaultApplicationId = "dev.aaa1115910.bv2"
    private const val applicationIdSystemProperty = "bv.applicationId"
    private const val applicationIdEnvName = "BV_APPLICATION_ID"
    const val compileSdk = 37
    const val minSdk = 23
    const val targetSdk = 37
    const val jdk = 21
    private const val major = 0
    private const val minor = 3
    private const val patch = 0
    private const val hotFix = 0

    @Suppress("KotlinConstantConditions")
    fun resolveVersion(projectDir: File): AppVersion {
        val gitCommitCount = runGitCommand(projectDir, "rev-list", "--count", "HEAD")
            ?.toIntOrNull()
            ?: 1
        val gitShortRevision = runGitCommand(projectDir, "rev-parse", "--short", "HEAD")
            ?: "nogit"
        val baseVersion = "$major.$minor.$patch${".$hotFix".takeIf { hotFix != 0 } ?: ""}"

        return AppVersion(
            code = gitCommitCount,
            name = "$baseVersion.r$gitCommitCount.$gitShortRevision"
        )
    }

    val applicationId: String
        get() = resolveApplicationId()

    const val libVLCVersion = "3.6.5"

    /**
     * SHA-256 of `org.videolan.android:libvlc-all:$libVLCVersion` (the AAR downloaded at runtime for
     * the on-demand VLC libraries). Cross-checked against Maven Central's `.sha1` sidecar
     * (60a2ba57c500a11cb7a0d9d9f3612cfbd36c6a96). Update together with [libVLCVersion].
     */
    const val libVLCAarSha256 = "6a15a1f7acd0738a31acce0c7e5eb6f5f340b62e6b64d8f83df547da56a44b47"

    /**
     * Pinned mpv-android GitHub release. `is.xyz.mpv.MPVLib` in `:player:core` mirrors the JNI ABI of
     * this release (primitive setters, `event(int)`, `logMessage(String,int,String)`); bump the tag
     * only after re-checking `app/src/main/jni` upstream.
     */
    const val mpvAndroidReleaseTag = "2026-08-11"

    /** SHA-256 fingerprint (uppercase hex, no separators) of the mpv-android release signing certificate. */
    const val mpvAndroidSigningCertSha256 =
        "FAE7F9D02385CC24D96E88436603E23EA6AEC7649AD1250ABBB4837DAAB82FFF"

    const val blacklistUrl =
        "https://raw.githubusercontent.com/aaa1115910/bv-blacklist/main/blacklist.bin"

    fun isGoogleServicesAvailable(projectDir: File): Boolean {
        val googleServicesJsonFile = projectDir.resolve("app/google-services.json")
        val expectedPackageNames = listOf(
            appId,
            applicationId,
            "$applicationId.r8test",
            "$applicationId.debug"
        )
        return googleServicesJsonFile.exists() && googleServicesJsonFile.readText().let {
            expectedPackageNames.all(it::contains)
        }
    }

    private fun resolveApplicationId(): String {
        return listOf(
            System.getProperty(applicationIdSystemProperty),
            System.getenv(applicationIdEnvName)
        ).firstOrNull { !it.isNullOrBlank() }?.trim() ?: defaultApplicationId
    }
}

private fun runGitCommand(projectDir: File, vararg arguments: String): String? {
    val command = arrayOf("git", "-C", projectDir.absolutePath, *arguments)
    return runCatching {
        val process = ProcessBuilder(*command)
            .directory(projectDir)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText().trim() }
        val exitCode = process.waitFor()
        output.takeIf { exitCode == 0 && it.isNotEmpty() }.also {
            if (it == null) {
                System.err.println(
                    "Unable to determine app version from Git in ${projectDir.absolutePath}: " +
                            output.ifEmpty { "git exited with code $exitCode" }
                )
            }
        }
    }.getOrElse {
        System.err.println(
            "Unable to determine app version from Git in ${projectDir.absolutePath}: " +
                    (it.message ?: it::class.simpleName)
        )
        null
    }
}
