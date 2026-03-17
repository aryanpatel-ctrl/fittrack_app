package com.fittrackpro.ui.tracking

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.fittrackpro.R
import com.fittrackpro.databinding.ActivityLiveTrackingBinding
import com.fittrackpro.service.TrackingService
import com.fittrackpro.util.Constants
import com.fittrackpro.util.formatDuration
import com.fittrackpro.util.formatPace
import com.fittrackpro.util.showToast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LiveTrackingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLiveTrackingBinding
    private val viewModel: LiveTrackingViewModel by viewModels()

    private var trackingService: TrackingService? = null
    private var isBound = false

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

        setupUI()
        checkPermissions()
        observeViewModel()
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
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
}

enum class TrackingState {
    IDLE, TRACKING, PAUSED, STOPPED
}
