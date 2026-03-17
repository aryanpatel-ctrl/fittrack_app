package com.fittrackpro.ui.nutrition.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fittrackpro.R
import com.fittrackpro.data.local.database.entity.NutritionLog
import com.fittrackpro.databinding.ItemMealLogBinding

class MealLogAdapter(
    private val onItemClick: (NutritionLog) -> Unit,
    private val onDeleteClick: (NutritionLog) -> Unit
) : ListAdapter<NutritionLog, MealLogAdapter.MealLogViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealLogViewHolder {
        val binding = ItemMealLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MealLogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MealLogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MealLogViewHolder(
        private val binding: ItemMealLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(pos))
                }
            }

            binding.btnDelete.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onDeleteClick(getItem(pos))
                }
            }
        }

        fun bind(item: NutritionLog) {
            binding.tvFoodName.text = item.foodItemId
            binding.tvQuantity.text = "${item.quantity} serving(s)"
            binding.tvCalories.text = "${item.calories} kcal"
            binding.tvMacros.text = "P: ${item.protein.toInt()}g | C: ${item.carbs.toInt()}g | F: ${item.fat.toInt()}g"

            val mealIcon = when (item.mealType) {
                "breakfast" -> R.drawable.ic_breakfast
                "lunch" -> R.drawable.ic_lunch
                "dinner" -> R.drawable.ic_dinner
                "snack" -> R.drawable.ic_snack
                else -> R.drawable.ic_food
            }
            binding.ivMealType.setImageResource(mealIcon)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<NutritionLog>() {
        override fun areItemsTheSame(oldItem: NutritionLog, newItem: NutritionLog): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: NutritionLog, newItem: NutritionLog): Boolean {
            return oldItem == newItem
        }
    }
}
