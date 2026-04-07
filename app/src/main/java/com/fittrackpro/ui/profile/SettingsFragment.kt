package com.fittrackpro.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.fittrackpro.databinding.FragmentSettingsBinding
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

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setNotificationsEnabled(isChecked)
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setDarkModeEnabled(isChecked)
        }

        binding.switchWaterReminders.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setWaterRemindersEnabled(isChecked)
        }

        binding.layoutChangePassword.setOnClickListener { }

        binding.layoutExportData.setOnClickListener {
            viewModel.exportData()
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
                binding.switchNotifications.isChecked = it.enableNotifications
                binding.switchDarkMode.isChecked = it.enableDarkMode
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
