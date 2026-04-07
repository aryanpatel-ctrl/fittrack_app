package com.fittrackpro.ui.routes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fittrackpro.R
import com.fittrackpro.data.local.database.entity.Route
import com.fittrackpro.databinding.ItemRouteBinding

class RouteAdapter(
    private val onItemClick: (Route) -> Unit,
    private val onFavoriteClick: (Route) -> Unit
) : ListAdapter<Route, RouteAdapter.RouteViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val binding = ItemRouteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RouteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RouteViewHolder(
        private val binding: ItemRouteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(pos))
                }
            }
            binding.btnFavorite.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onFavoriteClick(getItem(pos))
                }
            }
        }

        fun bind(route: Route) {
            binding.tvRouteName.text = route.name
            binding.tvRouteLocation.text = listOfNotNull(route.city, route.country).joinToString(", ").ifEmpty { "Unknown location" }
            binding.tvRouteDistance.text = String.format("%.1f km", route.distance / 1000)
            binding.tvRouteDifficulty.text = route.difficulty.replaceFirstChar { it.uppercase() }
            binding.tvRouteType.text = route.activityType.replaceFirstChar { it.uppercase() }

            // Rating
            if (route.ratingCount > 0) {
                binding.tvRouteRating.text = String.format("%.1f (%d)", route.avgRating, route.ratingCount)
            } else {
                binding.tvRouteRating.text = "No ratings"
            }

            // Favorite icon
            val favoriteIcon = if (route.isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite
            binding.btnFavorite.setImageResource(favoriteIcon)

            // Difficulty color
            val difficultyColor = when (route.difficulty.lowercase()) {
                "easy" -> R.color.success
                "moderate" -> R.color.warning
                "hard" -> R.color.error
                else -> R.color.text_secondary
            }
            binding.tvRouteDifficulty.setTextColor(binding.root.context.getColor(difficultyColor))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Route>() {
        override fun areItemsTheSame(oldItem: Route, newItem: Route): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Route, newItem: Route): Boolean {
            return oldItem == newItem
        }
    }
}
