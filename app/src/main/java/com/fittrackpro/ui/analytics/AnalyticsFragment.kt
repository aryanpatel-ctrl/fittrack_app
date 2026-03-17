package com.fittrackpro.ui.analytics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.fittrackpro.R
import com.fittrackpro.databinding.FragmentAnalyticsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnalyticsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupCharts()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.chipWeek.setOnClickListener { viewModel.setTimeRange(TimeRange.WEEK) }
        binding.chipMonth.setOnClickListener { viewModel.setTimeRange(TimeRange.MONTH) }
        binding.chipYear.setOnClickListener { viewModel.setTimeRange(TimeRange.YEAR) }
    }

    private fun setupCharts() {
        binding.chartDistance.apply {
            description.isEnabled = false; setDrawGridBackground(false)
            xAxis.position = XAxis.XAxisPosition.BOTTOM; xAxis.setDrawGridLines(false)
            axisRight.isEnabled = false; animateY(1000)
        }
        binding.chartPace.apply {
            description.isEnabled = false; setDrawGridBackground(false)
            xAxis.position = XAxis.XAxisPosition.BOTTOM; xAxis.setDrawGridLines(false)
            axisRight.isEnabled = false; animateX(1000)
        }
        binding.chartActivityTypes.apply {
            description.isEnabled = false; isDrawHoleEnabled = true
            setHoleColor(Color.WHITE); holeRadius = 50f; setDrawEntryLabels(false); animateY(1000)
        }
        binding.chartCalories.apply {
            description.isEnabled = false; setDrawGridBackground(false)
            xAxis.position = XAxis.XAxisPosition.BOTTOM; xAxis.setDrawGridLines(false)
            axisRight.isEnabled = false; animateY(1000)
        }
    }

    private fun observeViewModel() {
        viewModel.weeklyDistance.observe(viewLifecycleOwner) { data -> updateDistanceChart(data) }
        viewModel.paceHistory.observe(viewLifecycleOwner) { data -> updatePaceChart(data) }
        viewModel.activityTypeBreakdown.observe(viewLifecycleOwner) { data -> updateActivityTypeChart(data) }
        viewModel.caloriesHistory.observe(viewLifecycleOwner) { data -> updateCaloriesChart(data) }

        viewModel.summaryStats.observe(viewLifecycleOwner) { stats ->
            stats?.let {
                binding.tvTotalDistance.text = String.format("%.1f km", it.totalDistance / 1000)
                binding.tvTotalDuration.text = formatDuration(it.totalDuration)
                binding.tvTotalActivities.text = "${it.totalActivities}"
                binding.tvTotalCalories.text = "${it.totalCalories} kcal"
                binding.tvAvgPace.text = String.format("%.1f min/km", it.avgPace)
                binding.tvAvgDistance.text = String.format("%.1f km", it.avgDistance / 1000)
            }
        }

        viewModel.personalRecords.observe(viewLifecycleOwner) { records ->
            records.forEach { record ->
                when (record.recordType) {
                    "fastest_1k" -> binding.tvPr1k.text = formatDuration(record.value.toLong())
                    "fastest_5k" -> binding.tvPr5k.text = formatDuration(record.value.toLong())
                    "longest_distance" -> binding.tvPrDistance.text = String.format("%.2f km", record.value / 1000)
                    "longest_duration" -> binding.tvPrDuration.text = formatDuration(record.value.toLong())
                }
            }
        }
    }

    private fun updateDistanceChart(data: List<Pair<String, Float>>) {
        val entries = data.mapIndexed { i, (_, v) -> BarEntry(i.toFloat(), v) }
        val dataSet = BarDataSet(entries, "Distance (km)").apply { color = requireContext().getColor(R.color.primary) }
        binding.chartDistance.data = BarData(dataSet)
        binding.chartDistance.xAxis.valueFormatter = IndexAxisValueFormatter(data.map { it.first })
        binding.chartDistance.invalidate()
    }

    private fun updatePaceChart(data: List<Pair<String, Float>>) {
        val entries = data.mapIndexed { i, (_, v) -> Entry(i.toFloat(), v) }
        val dataSet = LineDataSet(entries, "Pace (min/km)").apply {
            color = requireContext().getColor(R.color.accent); lineWidth = 2f; circleRadius = 4f
            setCircleColor(requireContext().getColor(R.color.accent)); setDrawFilled(true)
        }
        binding.chartPace.data = LineData(dataSet)
        binding.chartPace.xAxis.valueFormatter = IndexAxisValueFormatter(data.map { it.first })
        binding.chartPace.invalidate()
    }

    private fun updateActivityTypeChart(data: Map<String, Int>) {
        val entries = data.map { (type, count) -> PieEntry(count.toFloat(), type.replaceFirstChar { it.uppercase() }) }
        val colors = listOf(
            requireContext().getColor(R.color.activity_running), requireContext().getColor(R.color.activity_cycling),
            requireContext().getColor(R.color.activity_walking), requireContext().getColor(R.color.activity_hiking),
            requireContext().getColor(R.color.activity_swimming)
        )
        val dataSet = PieDataSet(entries, "Activities").apply { this.colors = colors; valueTextColor = Color.WHITE; valueTextSize = 12f }
        binding.chartActivityTypes.data = PieData(dataSet)
        binding.chartActivityTypes.invalidate()
    }

    private fun updateCaloriesChart(data: List<Pair<String, Float>>) {
        val entries = data.mapIndexed { i, (_, v) -> BarEntry(i.toFloat(), v) }
        val dataSet = BarDataSet(entries, "Calories").apply { color = requireContext().getColor(R.color.secondary) }
        binding.chartCalories.data = BarData(dataSet)
        binding.chartCalories.xAxis.valueFormatter = IndexAxisValueFormatter(data.map { it.first })
        binding.chartCalories.invalidate()
    }

    private fun formatDuration(ms: Long): String {
        val h = ms / 3600000; val m = (ms % 3600000) / 60000; val s = (ms % 60000) / 1000
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
