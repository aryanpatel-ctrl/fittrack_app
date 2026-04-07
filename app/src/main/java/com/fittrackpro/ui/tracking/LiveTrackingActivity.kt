package com.fittrackpro.ui.tracking

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.fittrackpro.R
import com.fittrackpro.databinding.ActivityLiveTrackingBinding
import com.fittrackpro.service.TrackingService
import com.fittrackpro.util.Constants
import com.fittrackpro.util.formatDuration
import com.fittrackpro.util.formatPace
import com.fittrackpro.util.showToast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LiveTrackingActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {

    private lateinit var binding: ActivityLiveTrackingBinding
    private val viewModel: LiveTrackingViewModel by viewModels()

    // Google Maps
    private var googleMap: GoogleMap? = null
    private val routePoints = mutableListOf<LatLng>()
    private var polylineOptions: PolylineOptions? = null

    private var trackingService: TrackingService? = null
    private var isBound = false

    // Step Counter
    private lateinit var sensorManager: SensorManager
    private var stepCounterSensor: Sensor? = null
    private var isStepCounterAvailable = false
    private var initialStepCountSet = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TrackingService.TrackingBinder
            trackingService = binder.getService()
            isBound = true
            observeServiceData()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            trackingService = null
            isBound = false
        }
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                checkNotificationPermission()
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                checkNotificationPermission()
            }
            else -> {
                showToast("Location permission is required for tracking")
                finish()
            }
        }
    }

    private val notificationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Continue even if denied, but tracking won't show notification on Android 13+
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLiveTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupStepCounter()
        setupMap()
        setupUI()
        checkPermissions()
        observeViewModel()
    }

    private fun setupStepCounter() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        isStepCounterAvailable = stepCounterSensor != null
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Configure map settings
        googleMap?.apply {
            uiSettings.isZoomControlsEnabled = false
            uiSettings.isMyLocationButtonEnabled = true
            uiSettings.isCompassEnabled = true

            // Enable my location if permission granted
            if (ContextCompat.checkSelfPermission(
                    this@LiveTrackingActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                isMyLocationEnabled = true
            }

            // Set map style
            setMapStyle(null) // Use default style
        }

        // Initialize polyline for route
        polylineOptions = PolylineOptions()
            .color(ContextCompat.getColor(this, R.color.primary))
            .width(12f)
    }

    private fun updateMapRoute(latitude: Double, longitude: Double) {
        val newPoint = LatLng(latitude, longitude)
        routePoints.add(newPoint)

        googleMap?.let { map ->
            // Clear and redraw polyline
            map.clear()

            // Draw route polyline
            if (routePoints.size > 1) {
                val polyline = PolylineOptions()
                    .addAll(routePoints)
                    .color(ContextCompat.getColor(this, R.color.primary))
                    .width(12f)
                map.addPolyline(polyline)
            }

            // Add start marker
            if (routePoints.isNotEmpty()) {
                map.addMarker(
                    MarkerOptions()
                        .position(routePoints.first())
                        .title("Start")
                )
            }

            // Move camera to current location
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(newPoint, 17f))
        }
    }

    private fun fitMapToRoute() {
        if (routePoints.size < 2) return

        googleMap?.let { map ->
            val boundsBuilder = LatLngBounds.Builder()
            routePoints.forEach { boundsBuilder.include(it) }
            val bounds = boundsBuilder.build()
            val padding = 100 // padding in pixels
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        }
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnStartPause.setOnClickListener {
            when (viewModel.trackingState.value) {
                TrackingState.IDLE -> startTracking()
                TrackingState.TRACKING -> pauseTracking()
                TrackingState.PAUSED -> resumeTracking()
                else -> {}
            }
        }

        binding.btnStop.setOnClickListener {
            stopTracking()
        }

        binding.btnLock.setOnClickListener {
            // Toggle screen lock functionality
        }
    }

    private fun checkPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                checkNotificationPermission()
            }
            else -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED -> {
                    notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.trackingState.observe(this) { state ->
            updateUIForState(state)
        }

        viewModel.distance.observe(this) { distance ->
            binding.tvDistance.text = String.format("%.2f", distance / 1000)
        }

        viewModel.duration.observe(this) { duration ->
            binding.tvDuration.text = duration.formatDuration()
        }

        viewModel.pace.observe(this) { pace ->
            binding.tvPace.text = if (pace > 0) pace.formatPace() else "--:--"
        }

        viewModel.calories.observe(this) { calories ->
            binding.tvCalories.text = "$calories kcal"
        }

        viewModel.elevation.observe(this) { elevation ->
            binding.tvElevation.text = "${elevation.toInt()} m"
        }

        viewModel.steps.observe(this) { steps ->
            binding.tvSteps.text = "$steps steps"
        }
    }

    // SensorEventListener implementation for step counter
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                val totalSteps = it.values[0].toInt()
                // Set the baseline step count on first reading
                if (!initialStepCountSet) {
                    viewModel.setStartStepCount(totalSteps)
                    initialStepCountSet = true
                }
                viewModel.updateSteps(totalSteps)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for step counter
    }

    private fun registerStepCounter() {
        if (isStepCounterAvailable) {
            // Get current step count as baseline
            sensorManager.registerListener(
                this,
                stepCounterSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    private fun unregisterStepCounter() {
        if (isStepCounterAvailable) {
            sensorManager.unregisterListener(this)
        }
    }

    private fun observeServiceData() {
        trackingService?.let { service ->
            // Observe distance from service and update ViewModel
            service.totalDistance.observe(this) { distance ->
                viewModel.updateDistance(distance)
            }

            // Observe elapsed time from service and sync with ViewModel
            service.elapsedTime.observe(this) { _ ->
                // ViewModel has its own timer, service time is backup
            }

            // Observe current location for track point recording
            service.currentLocation.observe(this) { location ->
                location?.let {
                    viewModel.addTrackPoint(
                        latitude = it.latitude,
                        longitude = it.longitude,
                        altitude = if (it.hasAltitude()) it.altitude else null,
                        speed = if (it.hasSpeed()) it.speed else null,
                        accuracy = if (it.hasAccuracy()) it.accuracy else null
                    )

                    // Update map with new location (live route display)
                    updateMapRoute(it.latitude, it.longitude)

                    // Update elevation if available
                    if (it.hasAltitude()) {
                        viewModel.updateElevation(it.altitude)
                    }
                }
            }
        }
    }

    private fun updateUIForState(state: TrackingState) {
        when (state) {
            TrackingState.IDLE -> {
                binding.btnStartPause.setImageResource(R.drawable.ic_play)
                binding.btnStop.visibility = android.view.View.GONE
                binding.btnLock.visibility = android.view.View.GONE
            }
            TrackingState.TRACKING -> {
                binding.btnStartPause.setImageResource(R.drawable.ic_pause)
                binding.btnStop.visibility = android.view.View.VISIBLE
                binding.btnLock.visibility = android.view.View.VISIBLE
            }
            TrackingState.PAUSED -> {
                binding.btnStartPause.setImageResource(R.drawable.ic_play)
                binding.btnStop.visibility = android.view.View.VISIBLE
                binding.btnLock.visibility = android.view.View.GONE
            }
            TrackingState.STOPPED -> {
                // Navigate to summary
                finish()
            }
        }
    }

    private fun startTracking() {
        val intent = Intent(this, TrackingService::class.java).apply {
            action = Constants.TRACKING_SERVICE_ACTION_START
        }
        ContextCompat.startForegroundService(this, intent)
        bindService(Intent(this, TrackingService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
        viewModel.startTracking()

        // Start step counter
        initialStepCountSet = false
        registerStepCounter()
    }

    private fun pauseTracking() {
        val intent = Intent(this, TrackingService::class.java).apply {
            action = Constants.TRACKING_SERVICE_ACTION_PAUSE
        }
        startService(intent)
        viewModel.pauseTracking()
    }

    private fun resumeTracking() {
        val intent = Intent(this, TrackingService::class.java).apply {
            action = Constants.TRACKING_SERVICE_ACTION_RESUME
        }
        startService(intent)
        viewModel.resumeTracking()
    }

    private fun stopTracking() {
        // Fit map to show complete route
        fitMapToRoute()

        // Stop step counter
        unregisterStepCounter()

        val intent = Intent(this, TrackingService::class.java).apply {
            action = Constants.TRACKING_SERVICE_ACTION_STOP
        }
        startService(intent)
        viewModel.stopTracking()
    }

    override fun onStart() {
        super.onStart()
        Intent(this, TrackingService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
        // Re-register step counter if tracking is in progress
        if (viewModel.trackingState.value == TrackingState.TRACKING) {
            registerStepCounter()
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterStepCounter()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
}

enum class TrackingState {
    IDLE, TRACKING, PAUSED, STOPPED
}
