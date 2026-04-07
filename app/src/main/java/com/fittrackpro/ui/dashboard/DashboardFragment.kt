package com.fittrackpro.ui.dashboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.fittrackpro.R
import com.fittrackpro.databinding.FragmentDashboardBinding
import com.fittrackpro.service.StepCounterService
import com.fittrackpro.ui.tracking.LiveTrackingActivity
import com.fittrackpro.util.formatDistance
import com.fittrackpro.util.formatDuration
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

    // Permission launcher for Activity Recognition
    private val activityRecognitionPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startStepCounterService()
        }
    }

    private val stepUpdateHandler = Handler(Looper.getMainLooper())
    private val stepUpdateRunnable = object : Runnable {
        override fun run() {
            updateStepCounter()
            stepUpdateHandler.postDelayed(this, 3000) // Update every 3 seconds
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupWeeklyChart()
        setupStepCounter()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        stepUpdateHandler.post(stepUpdateRunnable)
        // Refresh stats when returning to dashboard (e.g., after completing an activity)
        viewModel.refresh()
    }

    override fun onPause() {
        super.onPause()
        stepUpdateHandler.removeCallbacks(stepUpdateRunnable)
    }

    private fun setupUI() {
        // Start Workout button launches Live Tracking
        binding.btnStartWorkout.setOnClickListener {
            val intent = Intent(requireContext(), LiveTrackingActivity::class.java)
            startActivity(intent)
        }

        // Quick Start cards
        binding.cardQuickRun.setOnClickListener {
            startActivityWithType("running")
        }

        binding.cardQuickCycle.setOnClickListener {
            startActivityWithType("cycling")
        }

        binding.cardQuickWalk.setOnClickListener {
            startActivityWithType("walking")
        }

        // Step goal - tap on steps to change goal
        binding.tvStepsCount.setOnClickListener {
            showStepGoalDialog()
        }

        binding.progressSteps.setOnClickListener {
            showStepGoalDialog()
        }

        // View All Progress - navigate to Analytics
        binding.tvViewAllProgress.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_analytics)
        }
    }

    private fun startActivityWithType(activityType: String) {
        val intent = Intent(requireContext(), LiveTrackingActivity::class.java).apply {
            putExtra("activity_type", activityType)
        }
        startActivity(intent)
    }

    private fun setupStepCounter() {
        // Check and request permission before starting step counter service
        if (!StepCounterService.isRunning) {
            checkAndStartStepCounterService()
        }

        // Initial update
        updateStepCounter()
    }

    private fun checkAndStartStepCounterService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startStepCounterService()
                }
                else -> {
                    activityRecognitionPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                }
            }
        } else {
            // No runtime permission needed for Android 9 and below
            startStepCounterService()
        }
    }

    private fun startStepCounterService() {
        try {
            val intent = Intent(requireContext(), StepCounterService::class.java)
            ContextCompat.startForegroundService(requireContext(), intent)
        } catch (e: Exception) {
            // Service couldn't start, step counting won't be available
            e.printStackTrace()
        }
    }

    private fun updateStepCounter() {
        // Get latest steps from SharedPreferences (for real-time updates)
        val steps = StepCounterService.getStepsToday(requireContext())
        val goal = StepCounterService.getStepGoal(requireContext())
        val progress = ((steps.toFloat() / goal) * 100).toInt().coerceIn(0, 100)

        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

        binding.tvStepsCount.text = numberFormat.format(steps)
        binding.progressSteps.progress = progress
    }

    private fun showStepGoalDialog() {
        val currentGoal = StepCounterService.getStepGoal(requireContext())
        val goals = arrayOf("5,000", "7,500", "10,000", "12,500", "15,000")
        val goalValues = intArrayOf(5000, 7500, 10000, 12500, 15000)
        val currentSelection = goalValues.indexOf(currentGoal).coerceAtLeast(0)

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Set Daily Step Goal")
            .setSingleChoiceItems(goals, currentSelection) { dialog, which ->
                val newGoal = goalValues[which]
                // Update in SharedPreferences (for service)
                StepCounterService.setStepGoal(requireContext(), newGoal)
                // Update in database (for ViewModel)
                viewModel.updateStepGoal(newGoal)
                updateStepCounter()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupWeeklyChart() {
        binding.chartWeekly.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)

            // X-axis configuration
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                textSize = 10f
            }

            // Y-axis configuration
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(requireContext(), R.color.divider)
                axisMinimum = 0f
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                textSize = 10f
            }

            axisRight.isEnabled = false

            // Animation
            animateY(1000)
        }
    }

    private fun observeViewModel() {
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.tvUserName.text = name ?: "Athlete"
        }

        viewModel.todayStats.observe(viewLifecycleOwner) { stats ->
            val distanceKm = stats.distance / 1000
            binding.tvDistanceValue.text = if (distanceKm >= 1) {
                String.format("%.1f km", distanceKm)
            } else {
                String.format("%.0f m", stats.distance)
            }
            binding.tvDurationValue.text = formatDurationShort(stats.duration)
            binding.tvCaloriesValue.text = stats.calories.toString()
        }

        viewModel.weeklyData.observe(viewLifecycleOwner) { weeklyData ->
            updateWeeklyChart(weeklyData)
        }

        // Observe step data from ViewModel (database-backed)
        viewModel.todaySteps.observe(viewLifecycleOwner) { stepData ->
            updateStepDisplay(stepData)
        }

        // Observe streak data
        viewModel.currentStreak.observe(viewLifecycleOwner) { streak ->
            binding.tvStreakCount.text = if (streak > 0) "$streak days" else "0 days"
        }

        // Observe level data
        viewModel.userLevel.observe(viewLifecycleOwner) { level ->
            binding.tvLevel.text = "Level $level"
        }
    }

    private fun formatDurationShort(millis: Long): String {
        val totalMinutes = millis / 60000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes} min"
        }
    }

    private fun updateStepDisplay(stepData: StepData) {
        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
        binding.tvStepsCount.text = numberFormat.format(stepData.steps)
        binding.progressSteps.progress = stepData.progress
    }

    private fun updateWeeklyChart(weeklyData: List<DailyData>) {
        if (weeklyData.isEmpty()) return

        val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        weeklyData.forEachIndexed { index, dailyData ->
            // Convert distance from meters to kilometers
            val distanceKm = (dailyData.distance / 1000).toFloat()
            entries.add(BarEntry(index.toFloat(), distanceKm))
            labels.add(dateFormat.format(Date(dailyData.date)))
        }

        val dataSet = BarDataSet(entries, "Distance (km)").apply {
            color = ContextCompat.getColor(requireContext(), R.color.primary)
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
            valueTextSize = 9f
            setDrawValues(true)
        }

        binding.chartWeekly.apply {
            data = BarData(dataSet).apply {
                barWidth = 0.6f
            }
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.labelCount = labels.size
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
