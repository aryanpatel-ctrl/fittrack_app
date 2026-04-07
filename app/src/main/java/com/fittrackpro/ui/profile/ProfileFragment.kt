package com.fittrackpro.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.fittrackpro.R
import com.fittrackpro.databinding.FragmentProfileBinding
import com.fittrackpro.util.BmiCalculator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Setup menu cards
        binding.cardAchievements.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_achievements)
        }

        binding.cardNutrition.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_nutrition)
        }

        binding.cardSettings.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_settings)
        }

        binding.cardLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        // BMI Edit button
        binding.btnEditBmi.setOnClickListener {
            showBmiEditDialog()
        }

        // Make BMI card clickable too
        binding.cardBmi.setOnClickListener {
            showBmiEditDialog()
        }

        // Edit profile button
        binding.btnEditProfile.setOnClickListener {
            // Navigate to edit profile or show dialog
        }
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                viewModel.logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.tvName.text = name ?: "User"
        }

        viewModel.userStats.observe(viewLifecycleOwner) { stats ->
            binding.tvLevel.text = "Level ${stats.level} • ${stats.totalXp} XP"
            binding.tvTotalActivities.text = stats.totalActivities.toString()
            binding.tvTotalDistance.text = "${(stats.totalDistance / 1000).toInt()} km"
        }

        viewModel.currentStreak.observe(viewLifecycleOwner) { streak ->
            binding.tvStreak.text = streak.toString()
        }

        // BMI Observers
        viewModel.bmiValue.observe(viewLifecycleOwner) { bmi ->
            if (bmi != null && bmi > 0) {
                binding.tvBmiValue.text = String.format("%.1f", bmi)
            } else {
                binding.tvBmiValue.text = "--"
            }
        }

        viewModel.bmiCategory.observe(viewLifecycleOwner) { category ->
            binding.tvBmiCategory.text = category.label
            binding.tvBmiCategory.setTextColor(
                ContextCompat.getColor(requireContext(), category.colorRes)
            )
            binding.tvBmiValue.setTextColor(
                ContextCompat.getColor(requireContext(), category.colorRes)
            )
            binding.tvBmiRecommendation.text = BmiCalculator.getRecommendation(category)
        }

        viewModel.userWeight.observe(viewLifecycleOwner) { weight ->
            binding.tvWeightValue.text = if (weight != null && weight > 0) {
                String.format("%.1f kg", weight)
            } else {
                "-- kg"
            }
        }

        viewModel.userHeight.observe(viewLifecycleOwner) { height ->
            binding.tvHeightValue.text = if (height != null && height > 0) {
                String.format("%.0f cm", height)
            } else {
                "-- cm"
            }
        }
    }

    private fun showBmiEditDialog() {
        val context = requireContext()

        // Create input fields
        val weightInput = EditText(context).apply {
            hint = "Weight (kg)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            viewModel.userWeight.value?.let { setText(String.format("%.1f", it)) }
        }

        val heightInput = EditText(context).apply {
            hint = "Height (cm)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            viewModel.userHeight.value?.let { setText(String.format("%.0f", it)) }
        }

        // Create layout
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 0)
            addView(weightInput)
            addView(heightInput.apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 24 }
            })
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("Update Body Metrics")
            .setMessage("Enter your weight and height to calculate BMI")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val weight = weightInput.text.toString().toFloatOrNull()
                val height = heightInput.text.toString().toFloatOrNull()

                if (weight != null && weight > 0 && height != null && height > 0) {
                    viewModel.updateUserBodyMetrics(weight, height)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
