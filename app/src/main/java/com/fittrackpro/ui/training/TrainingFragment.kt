package com.fittrackpro.ui.training

import android.app.AlertDialog
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

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun setupUI() {
        binding.btnCreatePlan.setOnClickListener {
            if (viewModel.currentPlan.value != null) {
                // Plan exists - show options dialog
                showPlanOptionsDialog()
            } else {
                // No plan - create new one
                findNavController().navigate(R.id.action_training_to_goal_wizard)
            }
        }

        binding.btnDeletePlan.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        binding.rvWorkouts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutAdapter
        }
    }

    private fun showPlanOptionsDialog() {
        val options = arrayOf("View Scheduled Workouts", "Create New Plan", "Cancel")
        AlertDialog.Builder(requireContext())
            .setTitle("Training Plan Options")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        // Already showing workouts in the list
                        dialog.dismiss()
                    }
                    1 -> {
                        // Ask to replace existing plan
                        showReplacePlanDialog()
                    }
                }
            }
            .show()
    }

    private fun showReplacePlanDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Replace Current Plan?")
            .setMessage("Creating a new plan will delete your current plan and all scheduled workouts. Continue?")
            .setPositiveButton("Replace") { _, _ ->
                viewModel.deletePlan()
                findNavController().navigate(R.id.action_training_to_goal_wizard)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Training Plan?")
            .setMessage("This will delete your current plan and all scheduled workouts. This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deletePlan()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.currentPlan.observe(viewLifecycleOwner) { plan ->
            if (plan != null) {
                binding.tvPlanName.text = plan.name
                binding.btnCreatePlan.text = "Manage Plan"
                binding.btnDeletePlan.visibility = View.VISIBLE
                binding.layoutProgress.visibility = View.VISIBLE
            } else {
                binding.tvPlanName.text = "No active plan"
                binding.btnCreatePlan.text = getString(R.string.create_plan)
                binding.btnDeletePlan.visibility = View.GONE
                binding.layoutProgress.visibility = View.GONE
            }
        }

        viewModel.planProgress.observe(viewLifecycleOwner) { progress ->
            progress?.let {
                binding.tvProgressText.text = "Week ${it.currentWeek} of ${it.totalWeeks}"
                binding.tvProgressPercent.text = "${it.completionPercentage.toInt()}%"
                binding.progressPlan.progress = it.completionPercentage.toInt()
                binding.tvWorkoutsCompleted.text = "${it.completedWorkouts} of ${it.totalWorkouts} workouts completed"
            }
        }

        viewModel.upcomingWorkouts.observe(viewLifecycleOwner) { workouts ->
            if (workouts.isEmpty()) {
                binding.rvWorkouts.visibility = View.GONE
                binding.layoutEmptyWorkouts.visibility = View.VISIBLE
            } else {
                binding.rvWorkouts.visibility = View.VISIBLE
                binding.layoutEmptyWorkouts.visibility = View.GONE
                workoutAdapter.submitList(workouts)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
