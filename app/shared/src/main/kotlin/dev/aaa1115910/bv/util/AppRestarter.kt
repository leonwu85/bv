package dev.aaa1115910.bv.util

import android.content.Context
import android.content.Intent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.system.exitProcess

/**
 * Cold-restarts the app process. Needed after installing a native component whose C++ runtime is
 * newer than the one already mapped into this process (see `NativeCxxRuntime`): shared libraries
 * cannot be unloaded, so a fresh process is the only way to pick the new runtime.
 */
object AppRestarter {
    private val logger = KotlinLogging.logger { }

    /**
     * Schedules a relaunch of the launcher activity in a fresh task and exits the current process.
     * The system starts a new process for the pending activity once this one is gone; if that does
     * not happen (some launchers), the user simply reopens the app.
     */
    fun restart(context: Context) {
        val packageManager = context.packageManager
        val launchIntent = packageManager.getLaunchIntentForPackage(context.packageName)
            ?: packageManager.getLeanbackLaunchIntentForPackage(context.packageName)
        val component = launchIntent?.component
        if (component != null) {
            runCatching {
                context.startActivity(Intent.makeRestartActivityTask(component))
            }.onFailure { logger.warn(it) { "Unable to schedule relaunch; exiting only" } }
        } else {
            logger.warn { "No launcher activity found for ${context.packageName}; exiting only" }
        }
        exitProcess(0)
    }
}
