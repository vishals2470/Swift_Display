package com.swiftcockpit.media

import android.service.notification.NotificationListenerService

/**
 * Exists only so the user can grant "notification access", which is what Android requires
 * before an app may read other apps' media sessions (song title, artwork, play/pause).
 * No notification content is read or stored.
 */
class MediaNotificationListener : NotificationListenerService()
