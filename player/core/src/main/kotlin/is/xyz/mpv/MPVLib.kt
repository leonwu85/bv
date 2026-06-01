package `is`.xyz.mpv

import android.content.Context
import android.graphics.Bitmap
import android.view.Surface

@Suppress("unused")
object MPVLib {
    @Volatile
    private var librariesLoaded = false

    @Synchronized
    fun loadLibraries(loader: (String) -> Unit = { System.loadLibrary(it) }) {
        if (librariesLoaded) return

        arrayOf("mpv", "player").forEach { libName ->
            loader(libName)
        }
        librariesLoaded = true
    }

    @Synchronized
    fun markLibrariesLoaded() {
        librariesLoaded = true
    }

    external fun create(appctx: Context)
    external fun init()
    external fun destroy()
    external fun attachSurface(surface: Surface)
    external fun detachSurface()
    external fun command(cmd: Array<out String>)
    external fun setOptionString(name: String, value: String): Int
    external fun grabThumbnail(dimension: Int): Bitmap?
    external fun getPropertyInt(property: String): Int?
    external fun setPropertyInt(property: String, value: Int)
    external fun getPropertyDouble(property: String): Double?
    external fun setPropertyDouble(property: String, value: Double)
    external fun getPropertyBoolean(property: String): Boolean?
    external fun setPropertyBoolean(property: String, value: Boolean)
    external fun getPropertyString(property: String): String?
    external fun setPropertyString(property: String, value: String)
    external fun observeProperty(property: String, format: Int)

    private val observers = mutableListOf<EventObserver>()

    @JvmStatic
    fun addObserver(observer: EventObserver) {
        synchronized(observers) {
            observers.add(observer)
        }
    }

    @JvmStatic
    fun removeObserver(observer: EventObserver) {
        synchronized(observers) {
            observers.remove(observer)
        }
    }

    @JvmStatic
    fun eventProperty(property: String) {
        synchronized(observers) {
            observers.forEach { it.eventProperty(property) }
        }
    }

    @JvmStatic
    fun eventProperty(property: String, value: Long) {
        synchronized(observers) {
            observers.forEach { it.eventProperty(property, value) }
        }
    }

    @JvmStatic
    fun eventProperty(property: String, value: Boolean) {
        synchronized(observers) {
            observers.forEach { it.eventProperty(property, value) }
        }
    }

    @JvmStatic
    fun eventProperty(property: String, value: String) {
        synchronized(observers) {
            observers.forEach { it.eventProperty(property, value) }
        }
    }

    @JvmStatic
    fun eventProperty(property: String, value: Double) {
        synchronized(observers) {
            observers.forEach { it.eventProperty(property, value) }
        }
    }

    @JvmStatic
    fun event(eventId: Int) {
        synchronized(observers) {
            observers.forEach { it.event(eventId) }
        }
    }

    private val logObservers = mutableListOf<LogObserver>()

    @JvmStatic
    fun addLogObserver(observer: LogObserver) {
        synchronized(logObservers) {
            logObservers.add(observer)
        }
    }

    @JvmStatic
    fun removeLogObserver(observer: LogObserver) {
        synchronized(logObservers) {
            logObservers.remove(observer)
        }
    }

    @JvmStatic
    fun logMessage(prefix: String, level: Int, text: String) {
        synchronized(logObservers) {
            logObservers.forEach { it.logMessage(prefix, level, text) }
        }
    }

    interface EventObserver {
        fun eventProperty(property: String)
        fun eventProperty(property: String, value: Long)
        fun eventProperty(property: String, value: Boolean)
        fun eventProperty(property: String, value: String)
        fun eventProperty(property: String, value: Double)
        fun event(eventId: Int)
    }

    interface LogObserver {
        fun logMessage(prefix: String, level: Int, text: String)
    }

    object MpvFormat {
        const val MPV_FORMAT_NONE = 0
        const val MPV_FORMAT_STRING = 1
        const val MPV_FORMAT_OSD_STRING = 2
        const val MPV_FORMAT_FLAG = 3
        const val MPV_FORMAT_INT64 = 4
        const val MPV_FORMAT_DOUBLE = 5
        const val MPV_FORMAT_NODE = 6
        const val MPV_FORMAT_NODE_ARRAY = 7
        const val MPV_FORMAT_NODE_MAP = 8
        const val MPV_FORMAT_BYTE_ARRAY = 9
    }

    object MpvEvent {
        const val MPV_EVENT_NONE = 0
        const val MPV_EVENT_SHUTDOWN = 1
        const val MPV_EVENT_LOG_MESSAGE = 2
        const val MPV_EVENT_GET_PROPERTY_REPLY = 3
        const val MPV_EVENT_SET_PROPERTY_REPLY = 4
        const val MPV_EVENT_COMMAND_REPLY = 5
        const val MPV_EVENT_START_FILE = 6
        const val MPV_EVENT_END_FILE = 7
        const val MPV_EVENT_FILE_LOADED = 8
        const val MPV_EVENT_CLIENT_MESSAGE = 16
        const val MPV_EVENT_VIDEO_RECONFIG = 17
        const val MPV_EVENT_AUDIO_RECONFIG = 18
        const val MPV_EVENT_SEEK = 20
        const val MPV_EVENT_PLAYBACK_RESTART = 21
        const val MPV_EVENT_PROPERTY_CHANGE = 22
        const val MPV_EVENT_QUEUE_OVERFLOW = 24
        const val MPV_EVENT_HOOK = 25
    }
}
