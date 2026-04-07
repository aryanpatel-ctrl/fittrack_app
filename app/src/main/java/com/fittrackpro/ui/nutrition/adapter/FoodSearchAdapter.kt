package com.fittrackpro.ui.nutrition.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fittrackpro.data.local.database.entity.FoodItem
import com.fittrackpro.databinding.ItemFoodSearchBinding

class FoodSearchAdapter(
    private val onItemClick: (FoodItem) -> Unit
) : ListAdapter<FoodItem, FoodSearchAdapter.FoodSearchViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodSearchViewHolder {
        val binding = ItemFoodSearchBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FoodSearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodSearchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FoodSearchViewHolder(
        private val binding: ItemFoodSearchBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(pos))
                }
            }
        }

        fun bind(foodItem: FoodItem) {
            binding.tvFoodName.text = foodItem.name

            // Show brand if available
            if (!foodItem.brand.isNullOrBlank()) {
                binding.tvFoodBrand.text = foodItem.brand
                binding.tvFoodBrand.visibility = View.VISIBLE
            } else {
                binding.tvFoodBrand.visibility = View.GONE
            }

            // Display calories
            binding.tvCalories.text = "${foodItem.calories} kcal"

            // Display serving info
            binding.tvServing.text = "per ${foodItem.servingSize.toInt()}${foodItem.servingUnit}"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<FoodItem>() {
        override fun areItemsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean {
            return oldItem == newItem
        }
    }
}
