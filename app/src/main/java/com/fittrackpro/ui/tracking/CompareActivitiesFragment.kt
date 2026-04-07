package com.fittrackpro.ui.tracking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.fittrackpro.R
import com.fittrackpro.data.local.database.entity.Track
import com.fittrackpro.databinding.FragmentCompareActivitiesBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class CompareActivitiesFragment : Fragment() {

    private var _binding: FragmentCompareActivitiesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CompareActivitiesViewModel by viewModels()
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompareActivitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.cardActivity1.setOnClickListener {
            showActivityPicker(1)
        }

        binding.cardActivity2.setOnClickListener {
            showActivityPicker(2)
        }
    }

    private fun showActivityPicker(slot: Int) {
        val activities = viewModel.activities.value ?: return
        if (activities.isEmpty()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("No Activities")
                .setMessage("You don't have any completed activities to compare.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val names = activities.map { track ->
            val type = track.activityType.replaceFirstChar { it.uppercase() }
            val date = dateFormat.format(Date(track.startTime))
            "$type - $date"
        }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Activity")
            .setItems(names) { _, which ->
                val selected = activities[which]
                if (slot == 1) {
                    viewModel.selectActivity1(selected.id)
                } else {
                    viewModel.selectActivity2(selected.id)
                }
            }
            .show()
    }

    private fun observeViewModel() {
        viewModel.activity1.observe(viewLifecycleOwner) { trackWithStats ->
            trackWithStats?.let {
                binding.tvActivity1Name.text = it.track.activityType.replaceFirstChar { c -> c.uppercase() }
                binding.tvActivity1Date.text = dateFormat.format(Date(it.track.startTime))
            }
        }

        viewModel.activity2.observe(viewLifecycleOwner) { trackWithStats ->
            trackWithStats?.let {
                binding.tvActivity2Name.text = it.track.activityType.replaceFirstChar { c -> c.uppercase() }
                binding.tvActivity2Date.text = dateFormat.format(Date(it.track.startTime))
            }
        }

        viewModel.comparison.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                binding.cardComparison.visibility = View.VISIBLE
                binding.layoutEmpty.visibility = View.GONE
                updateComparisonUI(result)
            }
        }
    }

    private fun updateComparisonUI(result: ComparisonResult) {
        val stats1 = viewModel.activity1.value?.stats ?: return
        val stats2 = viewModel.activity2.value?.stats ?: return

        // Distance
        binding.rowDistance.tvLabel.text = "Distance"
        binding.rowDistance.tvValue1.text = String.format("%.2f km", stats1.distance / 1000)
        binding.rowDistance.tvValue2.text = String.format("%.2f km", stats2.distance / 1000)
        setDiffText(binding.rowDistance.tvDiff, result.distanceDiff / 1000, "km", result.distancePercent, true)

        // Duration
        binding.rowDuration.tvLabel.text = "Duration"
        binding.rowDuration.tvValue1.text = formatDuration(stats1.duration)
        binding.rowDuration.tvValue2.text = formatDuration(stats2.duration)
        setDiffText(binding.rowDuration.tvDiff, result.durationDiff / 60000.0, "min", result.durationPercent, true)

        // Pace (lower is better)
        binding.rowPace.tvLabel.text = "Avg Pace"
        binding.rowPace.tvValue1.text = formatPace(stats1.avgPace)
        binding.rowPace.tvValue2.text = formatPace(stats2.avgPace)
        setDiffText(binding.rowPace.tvDiff, result.paceDiff.toDouble(), "min/km", result.pacePercent, false)

        // Speed (higher is better)
        binding.rowSpeed.tvLabel.text = "Avg Speed"
        binding.rowSpeed.tvValue1.text = String.format("%.1f km/h", stats1.avgSpeed * 3.6)
        binding.rowSpeed.tvValue2.text = String.format("%.1f km/h", stats2.avgSpeed * 3.6)
        setDiffText(binding.rowSpeed.tvDiff, (result.speedDiff * 3.6).toDouble(), "km/h", result.speedPercent, true)

        // Calories (higher is better for work done)
        binding.rowCalories.tvLabel.text = "Calories"
        binding.rowCalories.tvValue1.text = "${stats1.calories} kcal"
        binding.rowCalories.tvValue2.text = "${stats2.calories} kcal"
        setDiffText(binding.rowCalories.tvDiff, result.caloriesDiff.toDouble(), "kcal", result.caloriesPercent, true)

        // Elevation
        binding.rowElevation.tvLabel.text = "Elevation"
        binding.rowElevation.tvValue1.text = String.format("%.0f m", stats1.elevationGain)
        binding.rowElevation.tvValue2.text = String.format("%.0f m", stats2.elevationGain)
        setDiffText(binding.rowElevation.tvDiff, result.elevationDiff, "m", result.elevationPercent, true)
    }

    private fun setDiffText(textView: android.widget.TextView, diff: Double, unit: String, percent: Float, higherIsBetter: Boolean) {
        val isPositive = diff > 0
        val isImprovement = if (higherIsBetter) isPositive else !isPositive

        val sign = if (isPositive) "+" else ""
        textView.text = String.format("%s%.1f", sign, diff)

        val color = when {
            diff == 0.0 -> R.color.text_secondary
            isImprovement -> R.color.success
            else -> R.color.error
        }
        textView.setTextColor(ContextCompat.getColor(requireContext(), color))
    }

    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    private fun formatPace(pace: Float): String {
        val minutes = pace.toInt()
        val seconds = ((pace - minutes) * 60).toInt()
        return String.format("%d:%02d /km", minutes, seconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
