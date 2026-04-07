package com.fittrackpro.ui.training

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
import com.fittrackpro.databinding.FragmentTrainingBinding
import com.fittrackpro.ui.training.adapter.WorkoutAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TrainingFragment : Fragment() {

    private var _binding: FragmentTrainingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TrainingViewModel by viewModels()

    private val workoutAdapter = WorkoutAdapter { scheduledWorkout ->
        findNavController().navigate(
            R.id.action_training_to_workout_detail,
            bundleOf(
                "workoutId" to scheduledWorkout.workoutTemplateId,
                "scheduledWorkoutId" to scheduledWorkout.id
            )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrainingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnCreatePlan.setOnClickListener {
            findNavController().navigate(R.id.action_training_to_goal_wizard)
        }

        binding.rvWorkouts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.currentPlan.observe(viewLifecycleOwner) { plan ->
            if (plan != null) {
                binding.tvPlanName.text = plan.name
                binding.btnCreatePlan.text = "View Plan"
            } else {
                binding.tvPlanName.text = "No active plan"
                binding.btnCreatePlan.text = getString(R.string.create_plan)
            }
        }

        viewModel.upcomingWorkouts.observe(viewLifecycleOwner) { workouts ->
            workoutAdapter.submitList(workouts)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
