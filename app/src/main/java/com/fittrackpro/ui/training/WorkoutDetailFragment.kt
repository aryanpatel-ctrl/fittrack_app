package com.fittrackpro.ui.training

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.fittrackpro.databinding.FragmentWorkoutDetailBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WorkoutDetailFragment : Fragment() {

    private var _binding: FragmentWorkoutDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorkoutDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val workoutId = arguments?.getString("workoutId") ?: return
        viewModel.loadWorkout(workoutId)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnStartWorkout.setOnClickListener { viewModel.startWorkout() }
        binding.btnCompleteWorkout.setOnClickListener {
            viewModel.completeWorkout()
            findNavController().navigateUp()
        }
        binding.btnSkipWorkout.setOnClickListener {
            viewModel.skipWorkout()
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        viewModel.workout.observe(viewLifecycleOwner) { workout ->
            workout?.let {
                binding.tvWorkoutName.text = it.name
                binding.tvWorkoutType.text = it.type.replace("_", " ").replaceFirstChar { c -> c.uppercase() }
                binding.tvDescription.text = it.description
                binding.tvInstructions.text = it.instructions
                it.targetDuration?.let { dur -> binding.tvTargetDuration.text = "$dur min" }
                it.targetDistance?.let { dist -> binding.tvTargetDistance.text = String.format("%.1f km", dist) }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
