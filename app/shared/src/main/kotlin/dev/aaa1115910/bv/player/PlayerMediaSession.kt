package dev.aaa1115910.bv.player

import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import dev.aaa1115910.bv.viewmodel.VideoPlayerV3ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** 系统媒体会话只调用共享播放器接口，因此 Exo、VLC 和 MPV 行为一致。 */
class PlayerMediaSession(
    private val context: Context,
    scope: CoroutineScope,
    private val player: VideoPlayerV3ViewModel
) : AutoCloseable {
    private val session = MediaSession(context, "BV player")
    private var metadataKey = ""
    private var notificationKey = ""
    private val notificationId = System.identityHashCode(this)
    private val actionName = "${context.packageName}.MEDIA_CONTROL.$notificationId"
    private val notifications = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val updateJob: Job
    private var closed = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (closed || intent?.action != actionName) return
            when (intent.getIntExtra("control", 0)) {
                1 -> player.setExternalPlaybackPlaying(true)
                2 -> player.setExternalPlaybackPlaying(false)
                3 -> player.playMediaPlaylistOffset(-1)
                4 -> player.playMediaPlaylistOffset(1)
                5 -> player.seekByExternalControls(-10_000L)
                6 -> player.seekByExternalControls(10_000L)
            }
            update()
        }
    }

    init {
        session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
        ContextCompat.registerReceiver(context, receiver, IntentFilter(actionName), ContextCompat.RECEIVER_NOT_EXPORTED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifications.createNotificationChannel(NotificationChannel("bv-playback", "媒体播放", NotificationManager.IMPORTANCE_LOW))
        }
        session.setCallback(object : MediaSession.Callback() {
            override fun onPlay() { player.setExternalPlaybackPlaying(true); update() }
            override fun onPause() { player.setExternalPlaybackPlaying(false); update() }
            override fun onSeekTo(pos: Long) { player.seekFromExternalControls(pos); update() }
            override fun onFastForward() { player.seekByExternalControls(10_000L); update() }
            override fun onRewind() { player.seekByExternalControls(-10_000L); update() }
            override fun onSkipToNext() { player.playMediaPlaylistOffset(1); update() }
            override fun onSkipToPrevious() { player.playMediaPlaylistOffset(-1); update() }
        }, Handler(Looper.getMainLooper()))
        updateJob = scope.launch(Dispatchers.Main.immediate) {
            while (isActive) {
                update()
                delay(500)
            }
        }
    }

    private fun update() {
        if (closed) return
        val engine = player.videoPlayer
        session.isActive = engine != null
        if (engine == null) return
        val duration = if (player.isLive) 0L else engine.duration.coerceAtLeast(0L)
        val nextMetadataKey = "${player.currentAid}:${player.currentCid}:${player.title}:${player.partTitle}:${player.upName}:$duration"
        if (metadataKey != nextMetadataKey) {
            session.setMetadata(MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, "${player.currentAid}:${player.currentCid}")
                .putString(MediaMetadata.METADATA_KEY_TITLE, player.title)
                .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, player.partTitle.ifBlank { player.title })
                .putString(MediaMetadata.METADATA_KEY_ARTIST, player.upName)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, duration)
                .build())
            metadataKey = nextMetadataKey
        }
        var actions = PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE
        if (!player.isLive && engine.isSeekable) {
            actions = actions or PlaybackState.ACTION_SEEK_TO or PlaybackState.ACTION_FAST_FORWARD or PlaybackState.ACTION_REWIND
        }
        if (player.hasMediaPlaylistNeighbor(1)) actions = actions or PlaybackState.ACTION_SKIP_TO_NEXT
        if (player.hasMediaPlaylistNeighbor(-1)) actions = actions or PlaybackState.ACTION_SKIP_TO_PREVIOUS
        session.setPlaybackState(PlaybackState.Builder()
            .setActions(actions)
            .setState(
                when {
                    engine.isBuffering -> PlaybackState.STATE_BUFFERING
                    engine.isPlaying -> PlaybackState.STATE_PLAYING
                    else -> PlaybackState.STATE_PAUSED
                },
                if (player.isLive) PlaybackState.PLAYBACK_POSITION_UNKNOWN else engine.currentPosition.coerceAtLeast(0),
                if (engine.isPlaying) engine.speed else 0f,
                SystemClock.elapsedRealtime()
            ).build())
        val canPause = engine.isPlaying || engine.isBuffering
        val nextNotificationKey = "$nextMetadataKey:$canPause:$actions"
        if (notificationKey != nextNotificationKey) {
            postNotification(canPause)
            notificationKey = nextNotificationKey
        }
    }

    private fun action(control: Int, label: String, icon: Int): Notification.Action =
        Notification.Action.Builder(icon, label, PendingIntent.getBroadcast(
            context, control, Intent(actionName).setPackage(context.packageName).putExtra("control", control),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )).build()

    @android.annotation.SuppressLint("MissingPermission")
    private fun postNotification(canPause: Boolean) {
        // MediaStyle notifications with a media session are exempt from POST_NOTIFICATIONS.
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(context, "bv-playback")
            else Notification.Builder(context)
        builder.setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(player.partTitle.ifBlank { player.title })
            .setContentText(player.upName)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(canPause)
        var count = 0
        if (!player.isLive) {
            builder.addAction(if (player.hasMediaPlaylistNeighbor(-1)) action(3, "上一集", android.R.drawable.ic_media_previous)
                else action(5, "后退 10 秒", android.R.drawable.ic_media_rew))
            count++
        }
        builder.addAction(if (canPause) action(2, "暂停", android.R.drawable.ic_media_pause)
            else action(1, "播放", android.R.drawable.ic_media_play))
        count++
        if (!player.isLive) {
            builder.addAction(if (player.hasMediaPlaylistNeighbor(1)) action(4, "下一集", android.R.drawable.ic_media_next)
                else action(6, "前进 10 秒", android.R.drawable.ic_media_ff))
            count++
        }
        (context as? Activity)?.let { activity ->
            val returnIntent = Intent(activity.intent).setClass(context, activity.javaClass)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra("mediaSessionReturn", true)
                .putExtra("aid", player.currentAid).putExtra("cid", player.currentCid)
            val pending = PendingIntent.getActivity(context, notificationId, returnIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            session.setSessionActivity(pending)
            builder.setContentIntent(pending)
        }
        builder.setStyle(Notification.MediaStyle().setMediaSession(session.sessionToken)
            .setShowActionsInCompactView(*IntArray(count) { it }))
        runCatching { notifications.notify(notificationId, builder.build()) }
    }

    override fun close() {
        if (closed) return
        closed = true
        updateJob.cancel()
        session.isActive = false
        session.setCallback(null)
        session.release()
        context.unregisterReceiver(receiver)
        notifications.cancel(notificationId)
    }
}
