package com.jambit.alpha.backgroundlocation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.job.JobInfo
import android.app.job.JobInfo.NETWORK_TYPE_ANY
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.jambit.alpha.backgroundlocation.databinding.ActivityMainBinding

// Uses FusedLocationProvider:
//    https://developers.google.com/location-context/fused-location-provider
class MainActivity : Activity(), OnRequestPermissionsResultCallback {


    private lateinit var binding: ActivityMainBinding
    private lateinit var locationClient: FusedLocationProviderClient
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        enableGps()
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndSchedule()
        wakeLock()
    }

    private fun wakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "BackgroundLocation:WakeLock")
        wakeLock?.acquire(3600 * 1000L) // TODO: max wait time
    }

    private fun scheduleBackgroundUpdates() {
        Log.i(TAG, "Scheduling background updates")
        val scheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
        val component = ComponentName(this, LocationJob::class.java)
        val job = JobInfo.Builder(JOB_ID, component)
            .setPeriodic(JobInfo.getMinPeriodMillis())
            .setRequiredNetworkType(NETWORK_TYPE_ANY) // Only if results need to be sent across the network
            .build()
        val res =  scheduler.schedule(job)
        if (res != JobScheduler.RESULT_SUCCESS) {
            Log.e(TAG, "Could not schedule background job")
            return
        }

    }

    private fun enableGps() {
        Log.i(TAG, "Enable GPS")
        val service = getSystemService(LOCATION_SERVICE) as LocationManager
        val enabled = service
            .isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!enabled) {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
    }

    private fun checkPermissionsAndSchedule() {
        val permissions = arrayListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= 29) {
           permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        for (perm in permissions) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    perm
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG, "Location permission denied")
                ActivityCompat.requestPermissions(this, permissions.toTypedArray(), LOCATION_REQUEST_ID)
                return
            }
        }
        doForegroundUpdate()
    }

    // Check that foreground updates are possible and then schedule background updates
    @SuppressLint("MissingPermission")
    private fun doForegroundUpdate() {
        Log.i(TAG, "Foreground Location Update")
        val currentLocation = CurrentLocation(locationClient, TAG)
        currentLocation.request {
            wakeLock?.release()
            scheduleBackgroundUpdates()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST_ID && grantResults.all { r -> r == PackageManager.PERMISSION_GRANTED }) {
            doForegroundUpdate()
        }
    }

    companion object {
        const val TAG = "MainActivity"
        const val LOCATION_REQUEST_ID = 1
        const val JOB_ID = 1
    }
}