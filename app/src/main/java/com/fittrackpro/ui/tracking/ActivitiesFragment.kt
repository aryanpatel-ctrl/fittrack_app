package com.fittrackpro.ui.tracking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fittrackpro.R
import com.fittrackpro.databinding.FragmentActivitiesBinding
import com.fittrackpro.ui.tracking.adapter.TrackAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActivitiesFragment : Fragment() {

    private var _binding: FragmentActivitiesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ActivitiesViewModel by viewModels()

    private val activitiesAdapter = TrackAdapter { track ->
        findNavController().navigate(
            R.id.action_activities_to_activity_detail,
            bundleOf("trackId" to track.id)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvActivities.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = activitiesAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.activities.observe(viewLifecycleOwner) { activities ->
            if (activities.isEmpty()) {
                binding.layoutEmptyState.root.visibility = View.VISIBLE
                binding.rvActivities.visibility = View.GONE
            } else {
                binding.layoutEmptyState.root.visibility = View.GONE
                binding.rvActivities.visibility = View.VISIBLE
                activitiesAdapter.submitList(activities)
            }
        }

        // Observe monthly stats
        viewModel.monthlyStats.observe(viewLifecycleOwner) { stats ->
            updateMonthlyStats(stats)
        }
    }

    private fun updateMonthlyStats(stats: MonthlyStats) {
        // Workouts count
        binding.tvWorkoutsCount.text = stats.workoutsCount.toString()

        // Total distance (convert meters to km)
        val distanceKm = stats.totalDistance / 1000
        binding.tvTotalDistance.text = String.format("%.1f", distanceKm)

        // Total time (convert milliseconds to hours:minutes format)
        val totalMinutes = stats.totalDuration / 60000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        binding.tvTotalTime.text = if (hours > 0) {
            String.format("%d:%02d", hours, minutes)
        } else {
            String.format("%d min", minutes)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
