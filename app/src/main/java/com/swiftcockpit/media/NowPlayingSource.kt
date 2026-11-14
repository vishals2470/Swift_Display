package com.swiftcockpit.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Follows whichever app is playing media (Spotify, YouTube Music, the head unit's own player…)
 * through Android's media sessions, and forwards play/pause/skip to it.
 */
class NowPlayingSource(private val context: Context) {

    private val listenerComponent = ComponentName(context, MediaNotificationListener::class.java)

    /** Main-thread only: the controller currently shown, used for transport commands. */
    private var activeController: MediaController? = null

    fun hasAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /** Collect on the main thread; re-collect when [hasAccess] changes. */
    fun states(): Flow<NowPlaying> {
        if (!hasAccess()) return flowOf(NowPlaying.AccessRequired)
        val manager = context.getSystemService(MediaSessionManager::class.java)
            ?: return flowOf(NowPlaying.Idle)

        return callbackFlow {
            val handler = Handler(Looper.getMainLooper())
            val tracker = SessionTracker(handler) { trySend(it) }
            val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { tracker.select(it) }
            try {
                manager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, handler)
                tracker.select(manager.getActiveSessions(listenerComponent))
            } catch (_: SecurityException) {
                trySend(NowPlaying.AccessRequired)
            }
            awaitClose {
                manager.removeOnActiveSessionsChangedListener(sessionsListener)
                tracker.release()
            }
        }
    }

    fun playPause() {
        val controller = activeController ?: return
        if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    fun next() {
        activeController?.transportControls?.skipToNext()
    }

    fun previous() {
        activeController?.transportControls?.skipToPrevious()
    }

    private inner class SessionTracker(
        private val handler: Handler,
        private val publish: (NowPlaying) -> Unit,
    ) {
        private var current: MediaController? = null
        private var sessions: List<MediaController> = emptyList()

        private val callback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) = emit()
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                // Another app may have started playing; prefer whichever session is playing.
                select(sessions)
            }
            override fun onSessionDestroyed() = select(sessions.filter { it !== current })
        }

        fun select(available: List<MediaController>?) {
            sessions = available.orEmpty()
            // Sessions arrive in priority order; the first one that is playing wins.
            val pick = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: sessions.firstOrNull()
            if (pick?.sessionToken != current?.sessionToken) {
                current?.unregisterCallback(callback)
                current = pick
                pick?.registerCallback(callback, handler)
            }
            activeController = current
            emit()
        }

        fun release() {
            current?.unregisterCallback(callback)
            current = null
            activeController = null
        }

        private fun emit() = publish(current?.toNowPlaying() ?: NowPlaying.Idle)
    }

    private fun MediaController.toNowPlaying(): NowPlaying {
        val meta = metadata
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        val app = appLabel(packageName)
        if (title.isNullOrBlank()) {
            return if (playbackState?.state == PlaybackState.STATE_PLAYING) {
                NowPlaying.Active(app ?: "Playing", null, app, null, isPlaying = true)
            } else {
                NowPlaying.Idle
            }
        }
        return NowPlaying.Active(
            title = title,
            artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE),
            sourceApp = app,
            art = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON),
            isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING,
        )
    }

    private fun appLabel(packageName: String): String? = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (_: Exception) {
        null
    }
}
