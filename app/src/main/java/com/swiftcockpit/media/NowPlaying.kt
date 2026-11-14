package com.swiftcockpit.media

import android.graphics.Bitmap

sealed interface NowPlaying {
    /** Notification access not granted, so other apps' media sessions are not visible. */
    data object AccessRequired : NowPlaying

    /** No app has an active media session. */
    data object Idle : NowPlaying

    data class Active(
        val title: String,
        val artist: String?,
        val sourceApp: String?,
        val art: Bitmap?,
        val isPlaying: Boolean,
    ) : NowPlaying
}
