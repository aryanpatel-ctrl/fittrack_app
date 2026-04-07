package com.fittrackpro.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.fittrackpro.databinding.FragmentSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.layoutUnits.setOnClickListener {
            showUnitsDialog()
        }

        binding.switchWaterReminders.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setWaterRemindersEnabled(isChecked)
        }

        binding.layoutChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.layoutDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmation()
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }
    }

    private fun observeViewModel() {
        viewModel.settings.observe(viewLifecycleOwner) { settings ->
            settings?.let {
                binding.tvUnitsValue.text = if (it.useMetricUnits) "Metric (km, kg)" else "Imperial (mi, lb)"
            }
        }

        viewModel.logoutComplete.observe(viewLifecycleOwner) { complete ->
            if (complete) {
                requireActivity().finish()
            }
        }

        viewModel.waterRemindersEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.switchWaterReminders.isChecked = enabled
        }

        viewModel.passwordChangeResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                when (it) {
                    is SettingsViewModel.PasswordChangeResult.Success -> {
                        Toast.makeText(requireContext(), "Password changed successfully", Toast.LENGTH_SHORT).show()
                    }
                    is SettingsViewModel.PasswordChangeResult.Error -> {
                        Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
                    }
                }
                viewModel.clearPasswordChangeResult()
            }
        }
    }

    private fun showUnitsDialog() {
        val options = arrayOf("Metric (km, kg)", "Imperial (mi, lb)")
        val currentSelection = if (viewModel.settings.value?.useMetricUnits == true) 0 else 1

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Select Units")
            .setSingleChoiceItems(options, currentSelection) { dialog, which ->
                viewModel.setUseMetric(which == 0)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAccountConfirmation() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showChangePasswordDialog() {
        val context = requireContext()

        // Create input fields
        val currentPasswordInput = EditText(context).apply {
            hint = "Current Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val newPasswordInput = EditText(context).apply {
            hint = "New Password (min 8 characters)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val confirmPasswordInput = EditText(context).apply {
            hint = "Confirm New Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        // Create layout
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 0)
            addView(currentPasswordInput)
            addView(newPasswordInput.apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 24 }
            })
            addView(confirmPasswordInput.apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 24 }
            })
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("Change Password")
            .setMessage("Enter your current password and a new password")
            .setView(layout)
            .setPositiveButton("Change") { _, _ ->
                val currentPassword = currentPasswordInput.text.toString()
                val newPassword = newPasswordInput.text.toString()
                val confirmPassword = confirmPasswordInput.text.toString()

                viewModel.changePassword(currentPassword, newPassword, confirmPassword)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
