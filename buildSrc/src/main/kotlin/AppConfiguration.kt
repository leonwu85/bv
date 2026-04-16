import java.io.File

object AppConfiguration {
    const val appId = "dev.aaa1115910.bv"
    private const val defaultApplicationId = "dev.aaa1115910.bv2"
    private const val applicationIdSystemProperty = "bv.applicationId"
    private const val applicationIdEnvName = "BV_APPLICATION_ID"
    const val compileSdk = 36
    const val minSdk = 23
    const val targetSdk = 36
    const val jdk = 21
    private const val major = 0
    private const val minor = 3
    private const val patch = 0
    private const val hotFix = 0
    private val projectDir = File(System.getProperty("user.dir"))
    private val gitCommitCount: Int by lazy {
        runCommand("git", "rev-list", "--count", "HEAD")?.toIntOrNull() ?: 1
    }
    private val gitShortRevision: String by lazy {
        runCommand("git", "rev-list", "HEAD", "--abbrev-commit", "--max-count=1") ?: "nogit"
    }

    @Suppress("KotlinConstantConditions")
    val versionName: String by lazy {
        "$major.$minor.$patch${".$hotFix".takeIf { hotFix != 0 } ?: ""}" +
                ".r${versionCode}.${gitShortRevision}"
    }
    val applicationId: String by lazy {
        resolveApplicationId()
    }
    val versionCode: Int by lazy { gitCommitCount }
    const val libVLCVersion = "3.6.5"
    var googleServicesAvailable = true
    const val blacklistUrl =
        "https://raw.githubusercontent.com/aaa1115910/bv-blacklist/main/blacklist.bin"

    init {
        initConfigurations()
    }

    private fun initConfigurations() {
        val googleServicesJsonPath = "$projectDir/app/google-services.json"
        val googleServicesJsonFile = File(googleServicesJsonPath)
        val expectedPackageNames = listOf(
            appId,
            applicationId,
            "$applicationId.r8test",
            "$applicationId.debug"
        )
        googleServicesAvailable =
            googleServicesJsonFile.exists() && googleServicesJsonFile.readText().let {
                expectedPackageNames.all(it::contains)
            }
        println("Application ID: $applicationId")
        println("Google Services available: $googleServicesAvailable")
    }

    private fun resolveApplicationId(): String {
        return listOf(
            System.getProperty(applicationIdSystemProperty),
            System.getenv(applicationIdEnvName)
        ).firstOrNull { !it.isNullOrBlank() }?.trim() ?: defaultApplicationId
    }
}

private fun runCommand(vararg command: String): String? = runCatching {
    val process = ProcessBuilder(*command)
        .directory(File(System.getProperty("user.dir")))
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().use { it.readText().trim() }
    val exitCode = process.waitFor()
    output.takeIf { exitCode == 0 && it.isNotEmpty() }
}.getOrNull()
