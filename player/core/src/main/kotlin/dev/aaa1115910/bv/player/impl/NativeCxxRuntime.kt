package dev.aaa1115910.bv.player.impl

import android.content.Context
import dev.aaa1115910.bv.player.impl.mpv.MpvLibsInstaller
import dev.aaa1115910.bv.player.impl.vlc.VlcNativeLibs
import dev.aaa1115910.bv.util.NativeLibraryAbi
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

/** The libraries in a component directory need a C++ runtime newer than the one this process already runs. */
class NativeRuntimeConflictException(message: String) : IllegalStateException(message)

/**
 * Loads exactly one `libc++_shared.so` per process on behalf of all on-demand native components.
 *
 * VLC and MPV are downloaded separately and each ships its own `libc++_shared.so`, built by whatever
 * NDK that project used (libvlc 3.6.x: NDK r27; mpv-android 2026-08: NDK r29). Android's linker
 * resolves a `DT_NEEDED libc++_shared.so` against the *first* library with that soname in the app's
 * namespace, regardless of which file a later `System.load(path)` maps. So once VLC's older copy is
 * loaded, `libmpv.so` fails with `cannot locate symbol "_ZNSt6__ndk127__from_chars_floating_point..."`
 * even though MPV's own copy was "loaded" too.
 *
 * libc++ is backwards compatible: a newer `libc++_shared.so` satisfies binaries linked against an
 * older one, never the reverse. The rule implemented here is therefore: the first component to load
 * picks the newest libc++ among *all* installed components; every component skips its own copy; and
 * when a component whose libc++ is newer than the one already mapped shows up later, the caller gets
 * a [NativeRuntimeConflictException] explaining that the process must be restarted.
 */
object NativeCxxRuntime {
    const val LIBRARY_NAME = "libc++_shared.so"
    private val logger = KotlinLogging.logger { }

    /** Every directory that may contain a component-provided libc++. Lambdas avoid object init cycles. */
    private val componentLibsDirs: List<Pair<String, (Context) -> File>> = listOf(
        "MPV" to { context -> MpvLibsInstaller.getLibsDir(context) },
        "VLC" to { context -> VlcNativeLibs.libsDir(context) },
    )

    data class LoadedRuntime(val owner: String, val file: File, val version: ToolchainVersion?)

    private data class Candidate(val owner: String, val file: File, val version: ToolchainVersion?)

    /** The copy mapped into this process, or null until the first component loads. */
    @Volatile
    var loaded: LoadedRuntime? = null
        private set

    /**
     * Makes sure a libc++ that can serve [requester] (whose own copy is [ownCopy]) is loaded.
     *
     * @throws NativeRuntimeConflictException when an older libc++ is already mapped and cannot be replaced.
     * @throws UnsatisfiedLinkError when the chosen library fails to load.
     */
    @Synchronized
    @Throws(NativeRuntimeConflictException::class, UnsatisfiedLinkError::class)
    fun ensureLoadedFor(context: Context, requester: String, ownCopy: File) {
        loaded?.let { current ->
            restartRequiredReason(requester, ownCopy, current)?.let { throw NativeRuntimeConflictException(it) }
            logger.info { "$requester reuses libc++ from ${current.owner} (clang ${current.version ?: "unknown"})" }
            return
        }

        val chosen = selectRuntime(context, requester, ownCopy) ?: run {
            logger.warn { "No libc++_shared.so available for $requester; relying on system resolution" }
            return
        }
        System.load(chosen.file.absolutePath)
        loaded = LoadedRuntime(chosen.owner, chosen.file, chosen.version)
        logger.info {
            "Loaded libc++ for $requester from ${chosen.owner} (${chosen.file}, clang ${chosen.version ?: "unknown"})"
        }
    }

    /**
     * Explains why the component owning [ownCopy] cannot run in this process without a restart, or
     * returns null when it can. Safe to call before or after anything was loaded, e.g. right after
     * installing a component to decide whether to prompt for a restart.
     */
    fun restartRequiredReason(requester: String, ownCopy: File): String? {
        val current = loaded ?: return null
        return restartRequiredReason(requester, ownCopy, current)
    }

    private fun restartRequiredReason(requester: String, ownCopy: File, current: LoadedRuntime): String? {
        if (current.owner == requester) return null
        val required = ElfToolchainVersion.read(ownCopy) ?: return null
        val available = current.version ?: return null
        if (required <= available) return null
        return "当前进程已加载 ${current.owner} 组件自带的 C++ 运行库（clang $available），" +
            "低于 $requester 组件需要的版本（clang $required）。请完全退出并重新打开应用后再使用 $requester。"
    }

    private fun selectRuntime(context: Context, requester: String, ownCopy: File): Candidate? {
        val own = ownCopy.takeIf { NativeLibraryAbi.isCompatibleWithCurrentProcess(it) }
            ?.let { Candidate(requester, it, ElfToolchainVersion.read(it)) }
        val others = componentLibsDirs
            .filter { (owner, _) -> owner != requester }
            .mapNotNull { (owner, dirOf) ->
                val file = File(dirOf(context), LIBRARY_NAME)
                if (!NativeLibraryAbi.isCompatibleWithCurrentProcess(file)) return@mapNotNull null
                Candidate(owner, file, ElfToolchainVersion.read(file))
            }

        // An unreadable version on the requester's own copy means we cannot prove another copy is
        // newer, so the component in use right now must win.
        if (own != null && own.version == null) return own
        val newestOther = others.filter { it.version != null }.maxByOrNull { it.version!! }
        return when {
            own == null -> newestOther ?: others.firstOrNull()
            newestOther != null && newestOther.version!! > own.version!! -> newestOther
            else -> own
        }
    }
}
