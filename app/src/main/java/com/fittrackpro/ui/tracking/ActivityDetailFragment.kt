package com.fittrackpro.ui.tracking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.fittrackpro.databinding.FragmentActivityDetailBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActivityDetailFragment : Fragment() {

    private var _binding: FragmentActivityDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ActivityDetailViewModel by viewModels()
    private val args: ActivityDetailFragmentArgs by navArgs()

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
        observeViewModel()
        viewModel.loadActivity(args.trackId)
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnShare.setOnClickListener {
            viewModel.shareActivity()
        }

        binding.btnDelete.setOnClickListener {
            viewModel.deleteActivity()
        }
    }

    private fun observeViewModel() {
        viewModel.track.observe(viewLifecycleOwner) { track ->
            track?.let {
                binding.tvActivityType.text = it.activityType
                binding.tvDate.text = viewModel.formatDate(it.startTime)
            }
        }

        viewModel.statistics.observe(viewLifecycleOwner) { stats ->
            stats?.let {
                binding.tvDistance.text = String.format("%.2f km", it.distance / 1000)
                binding.tvDuration.text = viewModel.formatDuration(it.duration)
                binding.tvAvgPace.text = viewModel.formatPace(it.avgSpeed)
                binding.tvCalories.text = "${it.calories} kcal"
                binding.tvElevation.text = "${it.elevationGain.toInt()} m"
                binding.tvMaxSpeed.text = String.format("%.1f km/h", it.maxSpeed * 3.6)
            }
        }

        viewModel.deleteSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
