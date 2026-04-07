package com.fittrackpro.ui.training

import android.app.AlertDialog
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
        val scheduledWorkoutId = arguments?.getString("scheduledWorkoutId")
        viewModel.loadWorkout(workoutId, scheduledWorkoutId)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnCompleteWorkout.setOnClickListener {
            showCompleteConfirmationDialog()
        }

        binding.btnSkipWorkout.setOnClickListener {
            showSkipConfirmationDialog()
        }
    }

    private fun showCompleteConfirmationDialog() {
        val workoutName = viewModel.workout.value?.name ?: "this workout"
        AlertDialog.Builder(requireContext())
            .setTitle("Complete Workout")
            .setMessage("Mark \"$workoutName\" as completed?")
            .setPositiveButton("Complete") { _, _ ->
                viewModel.completeWorkout()
                findNavController().navigateUp()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSkipConfirmationDialog() {
        val workoutName = viewModel.workout.value?.name ?: "this workout"
        AlertDialog.Builder(requireContext())
            .setTitle("Skip Workout")
            .setMessage("Are you sure you want to skip \"$workoutName\"? You can still complete it later from your workout list.")
            .setPositiveButton("Skip") { _, _ ->
                viewModel.skipWorkout()
                findNavController().navigateUp()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.workout.observe(viewLifecycleOwner) { workout ->
            workout?.let {
                binding.tvWorkoutName.text = it.name
                binding.tvWorkoutType.text = it.type.replace("_", " ").replaceFirstChar { c -> c.uppercase() }
                binding.tvDescription.text = it.description
                binding.tvInstructions.text = it.instructions

                // targetDuration is in milliseconds, convert to minutes
                it.targetDuration?.let { durationMs ->
                    val minutes = durationMs / 60000
                    binding.tvTargetDuration.text = "$minutes min"
                }

                // targetDistance is in meters, convert to km
                it.targetDistance?.let { distanceMeters ->
                    val distanceKm = distanceMeters / 1000f
                    binding.tvTargetDistance.text = String.format("%.2f km", distanceKm)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
