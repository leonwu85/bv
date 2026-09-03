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

    /**
     * VLC 3 line (default): `org.videolan.android:libvlc-all:$libVLCVersion`, downloaded at runtime
     * for the on-demand VLC libraries. The Java layer in `:player:libvlcjni` supports both this and
     * [libVLC4Version]; bumping either requires re-checking the JNI surface (see player/libvlcjni/README.md).
     */
    const val libVLCVersion = "3.7.5"

    /**
     * SHA-256 of the `libvlc-all-$libVLCVersion.aar`. Cross-checked against Maven Central's `.sha1`
     * sidecar (9ba36b9af5774ba56e691fa3c92efbad801eb51e). Update together with [libVLCVersion].
     */
    const val libVLCAarSha256 = "2c25507adb1260aa4d81aad8c2ce98765d98026b9381f49ea454d0b8092f21cb"

    /** VLC 4 preview line, selectable by the user in the TV player settings. */
    const val libVLC4Version = "4.0.0-eap29"

    /**
     * SHA-256 of the `libvlc-all-$libVLC4Version.aar`. Cross-checked against Maven Central's `.sha1`
     * sidecar (63d85e3057b98b8d401869c8e1ec8476b9e4b3b0). Update together with [libVLC4Version].
     */
    const val libVLC4AarSha256 = "2033a8552d7c62c2c2deb52568c91ceb97378c532cdb69e9d457755654cbc727"

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
