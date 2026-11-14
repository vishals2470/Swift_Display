package com.swiftcockpit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swiftcockpit.location.GpsSpeedSource
import com.swiftcockpit.location.LocationPermission
import com.swiftcockpit.location.SpeedState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class CockpitViewModel(application: Application) : AndroidViewModel(application) {

    private val speedSource = GpsSpeedSource(application)
    private val permission = MutableStateFlow(speedSource.permission())

    val locationPermission: StateFlow<LocationPermission> = permission.asStateFlow()

    /**
     * GPS updates run only while the dashboard is visible (plus a 5 s grace period for
     * configuration changes), so the GPS stops when another app is in front.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val speed: StateFlow<SpeedState> = permission
        .flatMapLatest { speedSource.states(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpeedState.Acquiring)

    /** Call on every resume: the user may have changed the permission in system settings. */
    fun refreshPermission() {
        permission.value = speedSource.permission()
    }
}
