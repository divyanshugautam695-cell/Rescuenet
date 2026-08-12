package com.rescuenet.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager

class GpsLocationManager(context: Context) {
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    fun requestLocation(onLocation: (Location?) -> Unit) {
        val provider = when {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (provider == null) { onLocation(null); return }
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                onLocation(location)
                manager.removeUpdates(this)
            }
        }
        manager.requestLocationUpdates(provider, 0L, 0f, listener)
        onLocation(manager.getLastKnownLocation(provider))
    }
}
