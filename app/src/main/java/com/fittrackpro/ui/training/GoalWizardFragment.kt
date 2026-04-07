package com.fittrackpro.ui.training

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.fittrackpro.databinding.FragmentGoalWizardBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GoalWizardFragment : Fragment() {

    private var _binding: FragmentGoalWizardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GoalWizardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalWizardBinding.inflate(inflater, container, false)
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

        val goalTypes = arrayOf("Run a 5K", "Run a 10K", "Half Marathon", "Marathon", "Steps Challenge", "Weight Loss", "General Fitness")
        binding.spinnerGoalType.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, goalTypes)

        val difficulties = arrayOf("Beginner", "Intermediate", "Advanced")
        binding.spinnerDifficulty.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, difficulties)

        val durations = arrayOf("4 weeks", "8 weeks", "12 weeks", "16 weeks")
        binding.spinnerDuration.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, durations)

        binding.btnGeneratePlan.setOnClickListener {
            val goalType = binding.spinnerGoalType.selectedItem.toString()
            val difficulty = binding.spinnerDifficulty.selectedItem.toString()
            val duration = binding.spinnerDuration.selectedItem.toString()
            viewModel.generatePlan(goalType, difficulty, duration)
        }
    }

    private fun observeViewModel() {
        viewModel.planCreated.observe(viewLifecycleOwner) { created ->
            if (created) {
                Toast.makeText(requireContext(), "Training plan created!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.btnGeneratePlan.isEnabled = !loading
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.tvError.visibility = View.VISIBLE
                binding.tvError.text = error
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            } else {
                binding.tvError.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
