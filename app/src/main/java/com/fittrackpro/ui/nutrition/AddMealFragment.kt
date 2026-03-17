package com.fittrackpro.ui.nutrition

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fittrackpro.databinding.FragmentAddMealBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddMealFragment : Fragment() {

    private var _binding: FragmentAddMealBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddMealViewModel by viewModels()

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(requireContext(), "Barcode scanner opening...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Camera permission required for barcode scanning", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddMealBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mealType = arguments?.getString("mealType") ?: "breakfast"
        viewModel.setMealType(mealType)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        val mealTypes = arrayOf("Breakfast", "Lunch", "Dinner", "Snack")
        binding.spinnerMealType.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, mealTypes)

        binding.etSearchFood.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim()
                if (!query.isNullOrEmpty() && query.length >= 2) viewModel.searchFood(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnScanBarcode.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Barcode scanner opening...", Toast.LENGTH_SHORT).show()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnQuickAdd.setOnClickListener {
            val name = binding.etFoodName.text.toString().trim()
            val calories = binding.etCalories.text.toString().toIntOrNull() ?: 0
            val protein = binding.etProtein.text.toString().toFloatOrNull() ?: 0f
            val carbs = binding.etCarbs.text.toString().toFloatOrNull() ?: 0f
            val fat = binding.etFat.text.toString().toFloatOrNull() ?: 0f
            val quantity = binding.etQuantity.text.toString().toFloatOrNull() ?: 1f
            val mealType = binding.spinnerMealType.selectedItem.toString().lowercase()

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a food name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.addMealEntry(name, calories, protein, carbs, fat, quantity, mealType)
        }

        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun observeViewModel() {
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            binding.rvSearchResults.visibility = if (results.isNotEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.selectedFood.observe(viewLifecycleOwner) { food ->
            food?.let {
                binding.etFoodName.setText(it.name)
                binding.etCalories.setText(it.calories.toString())
                binding.etProtein.setText(it.protein.toString())
                binding.etCarbs.setText(it.carbs.toString())
                binding.etFat.setText(it.fat.toString())
                binding.rvSearchResults.visibility = View.GONE
            }
        }

        viewModel.mealAdded.observe(viewLifecycleOwner) { added ->
            if (added) {
                Toast.makeText(requireContext(), "Meal logged!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
