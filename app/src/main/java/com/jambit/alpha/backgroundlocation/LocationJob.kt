package com.jambit.alpha.backgroundlocation

import android.annotation.SuppressLint
import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource

class LocationJob : JobService() {

    private lateinit var locationClient: FusedLocationProviderClient

    private var cancellationTokenSource: CancellationTokenSource? = null
    private var currentLocation: CurrentLocation? = null

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.i(TAG, "onStartJob")
        getLocationWithPermission(params)
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        // Called by system when job needs to be cancelled
        Log.i(TAG, "Location Job cancelled by System")
        cancellationTokenSource?.cancel()
        cancellationTokenSource = null
        currentLocation?.cancel()
        return true
    }

    @SuppressLint("MissingPermission")
    private fun getLocationWithPermission(params: JobParameters?) {
        Log.i(TAG, "Get Location")
        if (!::locationClient.isInitialized) {
            locationClient = LocationServices.getFusedLocationProviderClient(this)
        }
        locationClient.locationAvailability.addOnSuccessListener { availability ->
            if (availability != null && availability.isLocationAvailable) {
                Log.i(TAG, "Getting Last Location")
                locationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        Log.i(TAG, "Last Location is $location")
                        jobFinished(params, false)
                    } else {
                        Log.w(TAG, "No last location -- rescheduling")
                        jobFinished(params, true)
                    }
                }
            } else {
                // No recent location exists -- we must get the current location
                Log.i(TAG, "Getting Current Location")
                currentLocation = CurrentLocation(locationClient, TAG)
                currentLocation?.request { jobFinished(params, false) }
                /*
                cancellationTokenSource = CancellationTokenSource()
                locationClient.getCurrentLocation(
                    LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationTokenSource?.token ?: CancellationTokenSource().token
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        Log.i(TAG, "Current Location is $location")
                        jobFinished(params, false)
                    } else {
                        Log.w(TAG, "No current location")
                        jobFinished(params, false)
                    }
                    cancellationTokenSource = null
                }
                */
            }
        }
    }

    companion object {
        const val TAG = "LocationJob"
    }

}