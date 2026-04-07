package com.fittrackpro.ui.routes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fittrackpro.data.local.database.entity.RouteRating
import com.fittrackpro.databinding.ItemReviewBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReviewAdapter : ListAdapter<RouteRating, ReviewAdapter.ReviewViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReviewViewHolder(
        private val binding: ItemReviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        fun bind(rating: RouteRating) {
            binding.tvUserName.text = rating.userId.take(8) + "..." // Truncated user ID
            binding.ratingBar.rating = rating.rating
            binding.tvDate.text = dateFormat.format(Date(rating.createdAt))

            if (!rating.review.isNullOrBlank()) {
                binding.tvReview.text = rating.review
                binding.tvReview.visibility = android.view.View.VISIBLE
            } else {
                binding.tvReview.visibility = android.view.View.GONE
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RouteRating>() {
        override fun areItemsTheSame(oldItem: RouteRating, newItem: RouteRating): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RouteRating, newItem: RouteRating): Boolean {
            return oldItem == newItem
        }
    }
}
