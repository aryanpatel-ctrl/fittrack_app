package com.fittrackpro.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.fittrackpro.R
import com.fittrackpro.databinding.FragmentProfileBinding
import com.fittrackpro.ui.auth.AuthActivity
import com.fittrackpro.util.BmiCalculator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
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
            showEditProfileDialog()
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

        viewModel.profileUpdated.observe(viewLifecycleOwner) { updated ->
            if (updated) {
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.loggedOut.observe(viewLifecycleOwner) { loggedOut ->
            if (loggedOut) {
                // Sign out from Firebase
                FirebaseAuth.getInstance().signOut()
                // Navigate to auth screen
                val intent = Intent(requireContext(), AuthActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
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

    private fun showEditProfileDialog() {
        val context = requireContext()

        // Create input fields
        val nameInput = EditText(context).apply {
            hint = "Name"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setText(viewModel.userName.value ?: "")
        }

        val emailInput = EditText(context).apply {
            hint = "Email"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(viewModel.userEmail.value ?: "")
        }

        // Gender spinner
        val genderOptions = arrayOf("Select Gender", "Male", "Female", "Other", "Prefer not to say")
        val genderSpinner = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, genderOptions)
            // Set current selection
            val currentGender = viewModel.userGender.value
            val position = when (currentGender?.lowercase()) {
                "male" -> 1
                "female" -> 2
                "other" -> 3
                "prefer not to say" -> 4
                else -> 0
            }
            setSelection(position)
        }

        // Create layout
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 0)
            addView(nameInput)
            addView(emailInput.apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 24 }
            })
            addView(genderSpinner.apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 24 }
            })
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("Edit Profile")
            .setMessage("Update your profile information")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val name = nameInput.text.toString().trim()
                val email = emailInput.text.toString().trim()
                val genderPosition = genderSpinner.selectedItemPosition
                val gender = if (genderPosition > 0) genderOptions[genderPosition] else null

                if (name.isNotEmpty() && email.isNotEmpty()) {
                    viewModel.updateUserProfile(name, email, gender)
                } else {
                    Toast.makeText(context, "Name and email are required", Toast.LENGTH_SHORT).show()
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
