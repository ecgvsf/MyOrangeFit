package com.myorangefit.app.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.myorangefit.app.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.myorangefit.app.databinding.ActivityMapsBinding
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.myorangefit.app.service.TrackingService
import java.util.Timer
import kotlin.concurrent.schedule

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var totalDistanceMeters = 0f                  // distanza totale in metri
    private var lastDistanceLatLng: LatLng? = null        // ultimo punto da cui calcolare


    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    private var locationMarker: Marker? = null
    private var currentLatLng: LatLng? = null

    private var isCameraMovedByUser = false
    private var isRunning = false

    private val points = mutableListOf<LatLng>()
    private var totalDistance = 0.0

    private val CHANNEL_ID = "notification_channel"
    private val NOTIF_ID = 1

    private var firstFix = true
    private var lastLatLng: LatLng? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var baseTime = SystemClock.elapsedRealtime()
    private var pauseOffset = 0L

    private lateinit var polyline: Polyline
    private val polylineOptions = PolylineOptions()
        .width(15f)
        .geodesic(true)        // Imposta la linea per una superfice sferica (geoide)
        .startCap(RoundCap())  // Imposta l'origine arrotondata
        .endCap(RoundCap())    // (Opzionale) Arrotonda anche la fine della linea

    private var trackingService: TrackingService? = null
    private var serviceBound = false
    // Lista di tutte le polyline (segmenti) disegnati finora
    private val polylines = mutableListOf<Polyline>()
    // La polyline “attiva” su cui aggiungere nuovi punti
    private var currentPolyline: Polyline? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            trackingService = (binder as TrackingService.LocalBinder).getService()
            serviceBound = true
            // Draw everything the service has collected so far:
            replayAllServicePoints()
        }
        override fun onServiceDisconnected(name: ComponentName) {
            serviceBound = false
            trackingService = null
        }
    }

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action != TrackingService.ACTION_LOCATION) return
            val lat = intent.getDoubleExtra(TrackingService.EXTRA_LAT, 0.0)
            val lng = intent.getDoubleExtra(TrackingService.EXTRA_LNG, 0.0)
            val newPos = LatLng(lat, lng)
            lastLatLng = newPos
            updateLocationMarker(newPos)
            if (firstFix) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos, 17.5f))
                firstFix = false
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Bind (but do NOT re-start foreground updates here)
        Intent(this, TrackingService::class.java).also {
            bindService(it, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(
                locationReceiver,
                IntentFilter(TrackingService.ACTION_LOCATION)
            )

    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(locationReceiver)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityManager.add(this)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1) Controlla permesso location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            startTrackingService()
        }

        setupMap()
        setupBtn()
    }

    // Pull the full history out of the service and draw it
    private fun replayAllServicePoints() {
        trackingService?.pathPoints?.let { allPoints ->
            // clear out any existing polylines
            polylines.forEach { it.remove() }
            polylines.clear()

            if (allPoints.isEmpty()) return

            // start a single continuous polyline for everything collected so far
            currentPolyline = mMap.addPolyline(
                PolylineOptions()
                    .addAll(allPoints)
                    .width(15f)
                    .geodesic(true)
                    .startCap(RoundCap())
                    .endCap(RoundCap())
                    .color(ContextCompat.getColor(this, R.color.primary))
            )
            polylines.add(currentPolyline!!)
        }
    }

    private fun startTrackingService() {
        Intent(this, TrackingService::class.java).also { intent ->
            ContextCompat.startForegroundService(this, intent)
        }
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupBtn() {
        //setta l'immagine iniziale a pausa
        binding.imgPlayPause.setImageResource(R.drawable.ic_pause)
        binding.btnCurrentLocation.setOnClickListener {
            lastLatLng?.let {
                isCameraMovedByUser = false  // Resetta il flag
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 17.5f))
            }
        }
        binding.btnGo.setOnClickListener { startRun() }
        binding.btnStop.setOnClickListener { stopRun() }
        binding.btnPlayPause.setOnClickListener { toggleRun() }
    }

    private fun stopRun() {
        isRunning = false // Disabilita il tracking della linea
        pauseOffset = 0L

        binding.btnGo.visibility = View.VISIBLE
        binding.btnPlayPause.visibility = View.GONE
        binding.btnStop.visibility = View.GONE
        binding.dataContainer.visibility = View.GONE

        binding.chronometer.stop()
    }

    private fun pauseRun() {
        binding.imgPlayPause.setImageResource(R.drawable.ic_play)
        pauseOffset = SystemClock.elapsedRealtime() - binding.chronometer.base
        binding.chronometer.stop()
        isRunning = false
        points.clear()
    }

    private fun resumeRun() {
        binding.imgPlayPause.setImageResource(R.drawable.ic_pause)
        binding.chronometer.base = SystemClock.elapsedRealtime() - pauseOffset
        binding.chronometer.start()
        isRunning = true

        LocalBroadcastManager.getInstance(this)
            .sendBroadcast(Intent(TrackingService.ACTION_START_UPDATES))

        // Nuovo segmento “vuoto”
        currentPolyline = mMap.addPolyline(
            PolylineOptions()
                .width(15f)
                .geodesic(true)
                .startCap(RoundCap())
                .endCap(RoundCap())
                .color(ContextCompat.getColor(this, R.color.primary))
        )
        polylines.add(currentPolyline!!)

        lastDistanceLatLng = null      // riparti a calcolare da zero per questo segmento

    }

    private fun toggleRun() {
        if (isRunning)
            pauseRun()
        else
            resumeRun()
    }

     private fun startRun() {
         currentPolyline = mMap.addPolyline(
             PolylineOptions()
                 .width(15f)
                 .geodesic(true)
                 .startCap(RoundCap())
                 .endCap(RoundCap())
                 .color(ContextCompat.getColor(this, R.color.primary))
         )
         polylines.add(currentPolyline!!)

         lastDistanceLatLng = null      // reset del punto di distanza
         totalDistanceMeters = 0f       // reset totale se è un nuovo run

         isRunning = true // Abilita il tracking della linea

         binding.btnGo.visibility = View.GONE
         binding.btnPlayPause.visibility = View.VISIBLE
         binding.btnStop.visibility = View.VISIBLE
         binding.dataContainer.visibility = View.VISIBLE

         binding.chronometer.base = SystemClock.elapsedRealtime()
         binding.chronometer.start()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Customize the map if necessary
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        try {
            val success = mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.map_style
                )
            )
            if (!success) {
                Log.e("MapsActivity", "Stile della mappa non applicato.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("MapsActivity", "Stile non trovato. Errore: ", e)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        val pos = LatLng(it.latitude, it.longitude)
                        lastLatLng = pos
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 17.5f))
                        updateLocationMarker(pos)
                    }
                }
        }
        mMap.setOnMapLoadedCallback {
            binding.cover.visibility = View.GONE
        }


        mMap.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                // L'utente ha iniziato a spostare la mappa manualmente
                isCameraMovedByUser = true
            }

        }

        // Initialize polyline
        polyline = mMap.addPolyline(polylineOptions.color(ContextCompat.getColor(this,
            R.color.primary
        )))
    }

    private fun vectorToBitmap(drawableId: Int): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(this, drawableId) ?: throw IllegalArgumentException("Drawable not found")
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 48 // default width
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 48 // default height
        drawable.setBounds(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun updateLocationMarker(latLng: LatLng) {
        if (locationMarker == null) {
            val bitmapDescriptor = vectorToBitmap(R.drawable.ic_location_marker)
            locationMarker = mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("La tua posizione")
                    .icon(bitmapDescriptor)
                    .anchor(0.5f, 0.5f)
            )
        } else {
            // Update the position of the existing marker
            locationMarker?.position = latLng
        }
        if (isRunning) {
            // **1) Calcola distanza dal punto precedente**
            lastDistanceLatLng?.let { prev ->
                val results = FloatArray(1)
                Location.distanceBetween(
                    prev.latitude, prev.longitude,
                    latLng.latitude,  latLng.longitude,
                    results
                )
                totalDistanceMeters += results[0]   // somma in metri
            }
            // **2) Aggiorna l’ultimo punto di riferimento**
            lastDistanceLatLng = latLng

            // **3) Aggiungi il punto alla polyline corrente**
            currentPolyline?.let { line ->
                val pts = line.points
                pts.add(latLng)
                line.points = pts
            }
            // **4) Aggiorna la UI della distanza (es. TextView)**
            val km = (totalDistanceMeters / 1000).toInt()
            val m  = (totalDistanceMeters % 1000).toInt()
            binding.mText.text = if (km > 0)
                "$km km $m m"
            else
                "$m m"
        }
        updateCameraPosition(latLng)
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun updatePolyline(latLng: LatLng) {
        if (points.isNotEmpty()) {
            // Calcola la distanza tra l'ultimo punto e il nuovo punto
            val lastPoint = points.last()
            val results = FloatArray(1)
            Location.distanceBetween(
                lastPoint.latitude, lastPoint.longitude,
                latLng.latitude, latLng.longitude,
                results
            )
            val distance = results[0] // Distanza in metri
            totalDistance += distance // / 1000 // Converti in chilometri
            Log.d("Distance", "$totalDistance ${totalDistance % 1000}")
            val km = (totalDistance / 1000).toInt()
            var string = ""
            if (km < 0)
                string = "${String.format("%.0f", totalDistance)} m"
            else
                string = "$km km ${String.format("%.0f", totalDistance % 1000)} m"
            binding.mText.text = string
            val notificationLayout = RemoteViews(packageName, R.layout.notification_running)
            notificationLayout.setTextViewText(
                R.id.tvDistance,
                string
            )
        }

        // Aggiungi il nuovo punto alla lista
        points.add(latLng)

        val points = polyline.points
        points.add(latLng)
        polyline.points = points
    }

    private fun updateCameraPosition(latLng: LatLng) {
        Log.d("cazzo", "$isCameraMovedByUser")
        if (!isCameraMovedByUser) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.5f))
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE
            && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startTrackingService()
        } else {
            Log.e("MapsActivity", "Permesso di accesso alla posizione negato.")
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, TrackingService::class.java))
        ActivityManager.remove(this)
    }
}