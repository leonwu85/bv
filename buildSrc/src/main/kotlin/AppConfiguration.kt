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
