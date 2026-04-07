package com.fittrackpro.ui.tracking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.fittrackpro.R
import com.fittrackpro.data.local.database.entity.TrackPoint
import com.fittrackpro.databinding.FragmentActivityDetailBinding
import com.fittrackpro.service.ExportService
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActivityDetailFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentActivityDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ActivityDetailViewModel by viewModels()
    private val args: ActivityDetailFragmentArgs by navArgs()

    private var googleMap: GoogleMap? = null
    private var pendingTrackPoints: List<TrackPoint>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupMap()
        observeViewModel()
        viewModel.loadActivity(args.trackId)
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnExport.setOnClickListener {
            showExportDialog()
        }

        binding.btnDelete.setOnClickListener {
            viewModel.deleteActivity()
        }
    }

    private fun showExportDialog() {
        val formats = arrayOf("GPX (GPS Exchange Format)", "TCX (Training Center XML)")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Export Activity")
            .setItems(formats) { _, which ->
                val format = when (which) {
                    0 -> ExportService.ExportFormat.GPX
                    1 -> ExportService.ExportFormat.TCX
                    else -> return@setItems
                }
                viewModel.exportActivity(format)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupMap() {
        // Add map fragment programmatically
        val mapFragment = SupportMapFragment.newInstance()
        childFragmentManager.beginTransaction()
            .replace(R.id.mapContainer, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Configure map settings
        map.uiSettings.apply {
            isZoomControlsEnabled = false
            isScrollGesturesEnabled = false
            isZoomGesturesEnabled = false
        }

        // If track points were loaded before map was ready, display them now
        pendingTrackPoints?.let { displayRoute(it) }
    }

    private fun displayRoute(trackPoints: List<TrackPoint>) {
        val map = googleMap
        if (map == null) {
            // Map not ready yet, store for later
            pendingTrackPoints = trackPoints
            return
        }

        if (trackPoints.isEmpty()) return

        // Convert track points to LatLng
        val latLngPoints = trackPoints.map { LatLng(it.latitude, it.longitude) }

        // Draw polyline for the route
        val polylineOptions = PolylineOptions()
            .addAll(latLngPoints)
            .color(ContextCompat.getColor(requireContext(), R.color.primary))
            .width(8f)
        map.addPolyline(polylineOptions)

        // Add start marker
        map.addMarker(
            MarkerOptions()
                .position(latLngPoints.first())
                .title("Start")
        )

        // Add end marker if different from start
        if (latLngPoints.size > 1) {
            map.addMarker(
                MarkerOptions()
                    .position(latLngPoints.last())
                    .title("Finish")
            )
        }

        // Fit map to show entire route
        if (latLngPoints.size >= 2) {
            val boundsBuilder = LatLngBounds.Builder()
            latLngPoints.forEach { boundsBuilder.include(it) }
            val bounds = boundsBuilder.build()
            val padding = 50
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        } else if (latLngPoints.isNotEmpty()) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngPoints.first(), 15f))
        }

        pendingTrackPoints = null
    }

    private fun observeViewModel() {
        viewModel.track.observe(viewLifecycleOwner) { track ->
            track?.let {
                binding.tvActivityType.text = it.activityType.replaceFirstChar { c -> c.uppercase() }
                binding.tvDate.text = viewModel.formatDate(it.startTime)
            }
        }

        viewModel.statistics.observe(viewLifecycleOwner) { stats ->
            stats?.let {
                binding.tvDistance.text = String.format("%.2f km", it.distance / 1000)
                binding.tvDuration.text = viewModel.formatDuration(it.duration)
                binding.tvAvgPace.text = viewModel.formatPace(it.avgSpeed)
                binding.tvCalories.text = "${it.calories} kcal"
                binding.tvMaxSpeed.text = String.format("%.1f km/h", it.maxSpeed * 3.6)
            }
        }

        viewModel.trackPoints.observe(viewLifecycleOwner) { points ->
            displayRoute(points)
        }

        viewModel.deleteSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                findNavController().navigateUp()
            }
        }

        viewModel.exportResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.success) {
                    val intent = viewModel.createShareIntent()
                    if (intent != null) {
                        startActivity(android.content.Intent.createChooser(intent, "Share Activity"))
                    }
                } else {
                    Toast.makeText(requireContext(), it.error ?: "Export failed", Toast.LENGTH_SHORT).show()
                }
                viewModel.clearExportResult()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
