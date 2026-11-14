package com.swiftcockpit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swiftcockpit.location.GpsSpeedSource
import com.swiftcockpit.location.LocationPermission
import com.swiftcockpit.location.SpeedState
import com.swiftcockpit.media.NowPlaying
import com.swiftcockpit.media.NowPlayingSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class CockpitViewModel(application: Application) : AndroidViewModel(application) {

    private val speedSource = GpsSpeedSource(application)
    private val mediaSource = NowPlayingSource(application)

    private val permission = MutableStateFlow(speedSource.permission())
    private val mediaAccess = MutableStateFlow(mediaSource.hasAccess())

    val locationPermission: StateFlow<LocationPermission> = permission.asStateFlow()

    /**
     * Sources run only while the dashboard is visible (plus a 5 s grace period for
     * configuration changes), so the GPS stops when another app is in front.
     */
    val speed: StateFlow<SpeedState> = permission
        .flatMapLatest { speedSource.states(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpeedState.Acquiring)

    val nowPlaying: StateFlow<NowPlaying> = mediaAccess
        .flatMapLatest { mediaSource.states() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NowPlaying.Idle)

    /** Call on every resume: permissions may have changed in system settings. */
    fun refreshPermissions() {
        permission.value = speedSource.permission()
        mediaAccess.value = mediaSource.hasAccess()
    }

    fun playPause() = mediaSource.playPause()
    fun next() = mediaSource.next()
    fun previous() = mediaSource.previous()
}
