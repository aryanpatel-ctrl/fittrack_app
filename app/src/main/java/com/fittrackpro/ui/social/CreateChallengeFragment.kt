package com.fittrackpro.ui.social

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.fittrackpro.databinding.FragmentCreateChallengeBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class CreateChallengeFragment : Fragment() {

    private var _binding: FragmentCreateChallengeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateChallengeViewModel by viewModels()
    private var startDate: Long = 0L
    private var endDate: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateChallengeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        val types = arrayOf("Distance", "Duration", "Calories", "Activities")
        binding.spinnerChallengeType.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types)

        val visOptions = arrayOf("Public", "Private", "Invite Only")
        binding.spinnerVisibility.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, visOptions)

        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        binding.btnSelectStartDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                cal.set(year, month, day)
                startDate = cal.timeInMillis
                binding.tvStartDate.text = dateFormat.format(cal.time)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnSelectEndDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                cal.set(year, month, day)
                endDate = cal.timeInMillis
                binding.tvEndDate.text = dateFormat.format(cal.time)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnCreateChallenge.setOnClickListener {
            val name = binding.etChallengeName.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val goalValue = binding.etGoalValue.text.toString().toDoubleOrNull()
            val type = binding.spinnerChallengeType.selectedItem.toString().lowercase()
            val visibility = binding.spinnerVisibility.selectedItem.toString().lowercase().replace(" ", "_")

            if (name.isEmpty() || goalValue == null || startDate == 0L || endDate == 0L) {
                Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val goalUnit = when (type) {
                "distance" -> "km"; "duration" -> "minutes"; "calories" -> "kcal"; else -> "count"
            }
            viewModel.createChallenge(name, description, type, goalValue, goalUnit, startDate, endDate, visibility)
        }
    }

    private fun observeViewModel() {
        viewModel.challengeCreated.observe(viewLifecycleOwner) { created ->
            if (created) {
                Toast.makeText(requireContext(), "Challenge created!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
