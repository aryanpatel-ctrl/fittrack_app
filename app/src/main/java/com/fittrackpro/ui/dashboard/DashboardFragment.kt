package com.fittrackpro.ui.dashboard

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
import com.fittrackpro.databinding.FragmentDashboardBinding
import com.fittrackpro.ui.dashboard.adapter.RecentActivityAdapter
import com.fittrackpro.util.formatDistance
import com.fittrackpro.util.formatDuration
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

    private val recentActivitiesAdapter = RecentActivityAdapter { track ->
        findNavController().navigate(
            R.id.action_dashboard_to_activity_detail,
            bundleOf("trackId" to track.id)
        )
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
        observeViewModel()
    }

    private fun setupUI() {
        binding.rvRecentActivities.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentActivitiesAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.tvUserName.text = name ?: "Athlete"
        }

        viewModel.todayStats.observe(viewLifecycleOwner) { stats ->
            binding.tvDistanceValue.text = String.format("%.1f", stats.distance / 1000)
            binding.tvDurationValue.text = stats.duration.formatDuration()
            binding.tvCaloriesValue.text = stats.calories.toString()
        }

        viewModel.recentActivities.observe(viewLifecycleOwner) { activities ->
            recentActivitiesAdapter.submitList(activities)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
