package com.swiftcockpit.system

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.swiftcockpit.media.MediaNotificationListener

/** Hand-offs to other apps. None of these can crash the dashboard if the target app is missing. */
object ExternalApps {
    private const val GOOGLE_MAPS = "com.google.android.apps.maps"

    fun openMaps(context: Context) {
        val googleMaps = context.packageManager.getLaunchIntentForPackage(GOOGLE_MAPS)
        val anyMapApp = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0"))
        if (!start(context, googleMaps) && !start(context, anyMapApp)) {
            toast(context, "No map app installed")
        }
    }

    fun openMusic(context: Context) {
        val music = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC)
        if (!start(context, music)) toast(context, "No music app installed")
    }

    fun openHome(context: Context) {
        start(context, Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
    }

    fun openSystemSettings(context: Context) {
        start(context, Intent(Settings.ACTION_SETTINGS))
    }

    fun openLocationSettings(context: Context) {
        start(context, Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    fun openAppPermissionSettings(context: Context) {
        start(
            context,
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        )
    }

    /** Screen where the user grants notification access (needed to show what's playing). */
    fun openNotificationAccessSettings(context: Context) {
        val component = ComponentName(context, MediaNotificationListener::class.java)
        val direct = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
        } else null
        if (!start(context, direct)) start(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun start(context: Context, intent: Intent?): Boolean {
        if (intent == null) return false
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private fun toast(context: Context, message: String) =
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
