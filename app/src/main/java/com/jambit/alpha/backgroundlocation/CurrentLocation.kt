package com.jambit.alpha.backgroundlocation;

import android.annotation.SuppressLint
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*

// Get the current location.
// Uses a longer timeout than getCurrentLocation
class CurrentLocation(private val locationClient: FusedLocationProviderClient, private val tag: String) : LocationCallback() {

    private lateinit var onLocationFinished: () -> Unit

    @SuppressLint("MissingPermission")
    fun request(onFinished: () -> Unit) {
        Log.i(tag, "Requesting current location")
        val request = LocationRequest.create().apply {
            interval = 10000L
            maxWaitTime = 300000L
            setExpirationDuration(300000L)
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }
        onLocationFinished = onFinished
        locationClient.requestLocationUpdates(request, this, Looper.getMainLooper())
    }

    fun cancel() {
        locationClient.removeLocationUpdates(this)
    }

    override fun onLocationResult(res: LocationResult) {
        Log.i(tag, "Location Result: $res")
        locationClient.removeLocationUpdates(this)
        onLocationFinished()
    }

    override fun onLocationAvailability(availability: LocationAvailability) {
        Log.i(tag, "Location available = ${availability.isLocationAvailable}")
    }
}
