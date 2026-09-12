package com.myorangefit.app.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.myorangefit.app.R

class TrackingService : LifecycleService() {

    companion object {
        const val ACTION_START_UPDATES = "com.myorangefit.app.action.START_UPDATES"
        const val ACTION_STOP_UPDATES  = "com.myorangefit.app.action.STOP_UPDATES"
        const val ACTION_LOCATION      = "com.myorangefit.app.ACTION_LOCATION"
        const val EXTRA_LAT            = "extra_lat"
        const val EXTRA_LNG            = "extra_lng"
        private const val NOTIF_ID      = 2
        private const val CHANNEL_ID    = "tracking_channel"
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // Receiver per comandi pause/resume dalla Activity
    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_STOP_UPDATES  -> fusedLocationClient.removeLocationUpdates(locationCallback)
                ACTION_START_UPDATES -> startLocationUpdates()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // 1) Inizializza FusedLocationProvider
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 2) Registra il receiver dei comandi pause/resume
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(
                commandReceiver,
                IntentFilter().apply {
                    addAction(ACTION_START_UPDATES)
                    addAction(ACTION_STOP_UPDATES)
                }
            )

        // 3) Crea il canale di notifica (Android O+)
        val chan = NotificationChannel(
            CHANNEL_ID,
            "Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Canale per il foreground service di tracciamento"
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(chan)

        // 4) Costruisci e avvia la notifica di foreground
        // Creazione del layout personalizzato per la notifica

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_gps)
            .setColor(ContextCompat.getColor(this, R.color.primary))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentTitle("Running...")
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())

        startForeground(NOTIF_ID, builder.build())


        // 5) Prepara il LocationCallback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { broadcastLocation(it) }
            }
        }

        // 6) Avvia subito gli updates
        startLocationUpdates()
    }

    // Richiede aggiornamenti di posizione al FusedLocationProvider
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val request = LocationRequest.create().apply {
            interval = 1_000L
            fastestInterval = 500L
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    // Broadcast interno per inviare la posizione all’Activity
    private fun broadcastLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        pathPoints.add(latLng)

        val intent = Intent(ACTION_LOCATION).apply {
            putExtra(EXTRA_LAT, location.latitude)
            putExtra(EXTRA_LNG, location.longitude)
        }

        LocalBroadcastManager
            .getInstance(this)
            .sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 1) Rimuovi receiver comandi
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(commandReceiver)
        // 2) Ferma gli updates di posizione
        fusedLocationClient.removeLocationUpdates(locationCallback)
        // **Rimuovi la notifica di foreground**
        stopForeground(true)
    }


    // Public list of all path points for the current run
    val pathPoints = mutableListOf<LatLng>()

    // Binder so Activities can get the service instance
    inner class LocalBinder : Binder() {
        fun getService(): TrackingService = this@TrackingService
    }
    private val binder = LocalBinder()

    // Manteniamo il binding normale di LifecycleService
    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return binder
    }
}

