package com.example.myorangefit

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.myorangefit.activity.ActivityManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.example.myorangefit.databinding.ActivityMapsBinding
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    private var locationMarker: Marker? = null
    private var currentLatLng: LatLng? = null

    private var isCameraMovedByUser = false
    private var isRunning = false

    private val points = mutableListOf<LatLng>()
    private var totalDistance = 0.0


    private val CHANNEL_ID = "notification_channel"


    private lateinit var polyline: Polyline
    private val polylineOptions = PolylineOptions()
                                    .width(15f)
                                    .geodesic(true)        // Imposta la linea per una superfice sferica (geoide)
                                    .startCap(RoundCap())  // Imposta l'origine arrotondata
                                    .endCap(RoundCap())    // (Opzionale) Arrotonda anche la fine della linea

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityManager.add(this)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //setta l'immagine iniziale a pausa
        binding.imgPlayPause.setImageResource(R.drawable.ic_pause)

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Set the location callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation
                if (location != null) {
                    currentLatLng = LatLng(location.latitude, location.longitude)
                    updateLocationMarker(currentLatLng!!)
                }
            }
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnCurrentLocation.setOnClickListener {
            currentLatLng?.let {
                isCameraMovedByUser = false  // Resetta il flag
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 17.5f))
            }
        }

        createNotificationChannel()

        binding.btnGo.setOnClickListener {
            isRunning = true // Abilita il tracking della linea

            binding.btnGo.visibility = View.GONE
            binding.btnPlayPause.visibility = View.VISIBLE
            binding.btnStop.visibility = View.VISIBLE
            binding.dataContainer.visibility = View.VISIBLE

            binding.chronometer.base = SystemClock.elapsedRealtime()
            binding.chronometer.start()

            sendNotification()
        }

        binding.btnStop.setOnClickListener {
            isRunning = false // Disabilita il tracking della linea

            binding.btnGo.visibility = View.VISIBLE
            binding.btnPlayPause.visibility = View.GONE
            binding.btnStop.visibility = View.GONE
            binding.dataContainer.visibility = View.GONE

            binding.chronometer.stop()
        }

        binding.btnPlayPause.setOnClickListener {
             if (!isRunning) {
                binding.imgPlayPause.setImageResource(R.drawable.ic_pause)
                binding.chronometer.start()
                isRunning = true
            } else {
                binding.imgPlayPause.setImageResource(R.drawable.ic_play)
                binding.chronometer.stop()
                isRunning = false
                points.clear()
            }
        }
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


        mMap.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                // L'utente ha iniziato a spostare la mappa manualmente
                isCameraMovedByUser = true
            }

        }

        // Initialize polyline
        polyline = mMap.addPolyline(polylineOptions.color(ContextCompat.getColor(this, R.color.primary)))

        // Request location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            getLastKnownLocation()
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng!!, 17.5f))
                    updateLocationMarker(currentLatLng!!)
                }
                startLocationUpdates()
            }
            .addOnFailureListener { e ->
                Log.e("MapsActivity", "Error trying to get last GPS location", e)
                startLocationUpdates()
            }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 500 // Update interval in milliseconds
            fastestInterval = 500 // Fastest update interval in milliseconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }

        if (currentLatLng != null)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng!!, 17.5f))
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
            updatePolyline(latLng)
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
            if (km < 0)
                binding.mText.text = "${String.format("%.0f", totalDistance)} m"
            else
                binding.mText.text = "$km km ${String.format("%.0f", totalDistance % 1000)} m"
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


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                    getLastKnownLocation()
                }
            } else {
                Log.e("MapsActivity", "Permesso di accesso alla posizione negato.")
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notification Channel"
            val descriptionText = "Channel for notification"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendNotification() {
        // Creazione del layout personalizzato per la notifica
        val notificationLayout = RemoteViews(packageName, R.layout.notification_running)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_weight) // Replace with your icon
            .setContentTitle("Running...")
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(notificationLayout)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(1, builder.build())
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
            ActivityManager.remove(this)
    }
}
