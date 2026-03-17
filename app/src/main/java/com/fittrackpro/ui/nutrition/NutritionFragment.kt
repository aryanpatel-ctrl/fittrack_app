package com.fittrackpro.ui.nutrition

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
import com.fittrackpro.data.local.database.entity.NutritionLog
import com.fittrackpro.databinding.FragmentNutritionBinding
import com.fittrackpro.ui.nutrition.adapter.MealLogAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class NutritionFragment : Fragment() {

    private var _binding: FragmentNutritionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NutritionViewModel by viewModels()
    private lateinit var mealLogAdapter: MealLogAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNutritionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnPreviousDay.setOnClickListener {
            viewModel.previousDay()
        }

        binding.btnNextDay.setOnClickListener {
            viewModel.nextDay()
        }

        binding.fabAddMeal.setOnClickListener {
            findNavController().navigate(
                R.id.action_nutrition_to_add_meal
            )
        }

        binding.btnAddWater.setOnClickListener {
            viewModel.addWater(250) // Add 250ml
        }

        binding.cardBreakfast.setOnClickListener {
            navigateToAddMeal("breakfast")
        }

        binding.cardLunch.setOnClickListener {
            navigateToAddMeal("lunch")
        }

        binding.cardDinner.setOnClickListener {
            navigateToAddMeal("dinner")
        }

        binding.cardSnacks.setOnClickListener {
            navigateToAddMeal("snack")
        }
    }

    private fun navigateToAddMeal(mealType: String) {
        findNavController().navigate(
            R.id.action_nutrition_to_add_meal,
            bundleOf("mealType" to mealType)
        )
    }

    private fun setupRecyclerView() {
        mealLogAdapter = MealLogAdapter(
            onItemClick = { nutritionLog ->
                showMealDetailDialog(nutritionLog)
            },
            onDeleteClick = { nutritionLog ->
                viewModel.deleteLog(nutritionLog)
            }
        )

        binding.rvMealLogs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mealLogAdapter
        }
    }

    private fun showMealDetailDialog(nutritionLog: NutritionLog) {
        val mealTypeName = nutritionLog.mealType.replaceFirstChar { it.uppercase() }

        val message = buildString {
            append("Meal Type: $mealTypeName")
            append("\nQuantity: ${nutritionLog.quantity} serving(s)")
            append("\n\nNutrition Facts:")
            append("\n  Calories: ${nutritionLog.calories} kcal")
            append("\n  Protein: ${nutritionLog.protein.toInt()}g")
            append("\n  Carbs: ${nutritionLog.carbs.toInt()}g")
            append("\n  Fat: ${nutritionLog.fat.toInt()}g")
            if (nutritionLog.fiber > 0) {
                append("\n  Fiber: ${nutritionLog.fiber.toInt()}g")
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(nutritionLog.foodItemId)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNegativeButton("Delete") { _, _ ->
                viewModel.deleteLog(nutritionLog)
            }
            .show()
    }

    private fun observeViewModel() {
        viewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
            binding.tvSelectedDate.text = dateFormat.format(date)

            val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val isToday = today.format(date) == today.format(Date())
            binding.tvSelectedDate.text = if (isToday) "Today" else dateFormat.format(date)
        }

        viewModel.dailySummary.observe(viewLifecycleOwner) { summary ->
            summary?.let {
                // Calories
                binding.tvCaloriesConsumed.text = "${it.totalCalories}"
                binding.tvCalorieGoal.text = "/ ${viewModel.calorieGoal} kcal"
                binding.progressCaloriesCircular.progress =
                    ((it.totalCalories.toFloat() / viewModel.calorieGoal) * 100).toInt().coerceAtMost(100)

                // Macros
                binding.tvProtein.text = "${it.totalProtein}g"
                binding.progressProtein.progress =
                    ((it.totalProtein.toFloat() / viewModel.proteinGoal) * 100).toInt().coerceAtMost(100)

                binding.tvCarbs.text = "${it.totalCarbs}g"
                binding.progressCarbs.progress =
                    ((it.totalCarbs.toFloat() / viewModel.carbsGoal) * 100).toInt().coerceAtMost(100)

                binding.tvFat.text = "${it.totalFat}g"
                binding.progressFat.progress =
                    ((it.totalFat.toFloat() / viewModel.fatGoal) * 100).toInt().coerceAtMost(100)
            }
        }

        viewModel.hydration.observe(viewLifecycleOwner) { hydration ->
            val totalMl = hydration.sumOf { it.amountMl }
            binding.tvWaterAmount.text = "${totalMl}ml"
            binding.progressWater.progress =
                ((totalMl.toFloat() / viewModel.waterGoal) * 100).toInt().coerceAtMost(100)
        }

        viewModel.mealLogs.observe(viewLifecycleOwner) { logs ->
            mealLogAdapter.submitList(logs)

            // Update meal type summaries
            val breakfastCal = logs.filter { it.mealType == "breakfast" }.sumOf { it.calories }
            val lunchCal = logs.filter { it.mealType == "lunch" }.sumOf { it.calories }
            val dinnerCal = logs.filter { it.mealType == "dinner" }.sumOf { it.calories }
            val snackCal = logs.filter { it.mealType == "snack" }.sumOf { it.calories }

            binding.tvBreakfastCalories.text = "$breakfastCal kcal"
            binding.tvLunchCalories.text = "$lunchCal kcal"
            binding.tvDinnerCalories.text = "$dinnerCal kcal"
            binding.tvSnacksCalories.text = "$snackCal kcal"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
