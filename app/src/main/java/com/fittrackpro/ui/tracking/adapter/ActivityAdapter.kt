package com.fittrackpro.ui.tracking.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fittrackpro.R
import com.fittrackpro.databinding.ItemActivityBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ActivityItem(
    val trackId: String,
    val activityType: String,
    val name: String?,
    val distance: Double,
    val duration: Long,
    val calories: Int,
    val startTime: Long,
    val avgPace: Float
)

class ActivityAdapter(
    private val onItemClick: (ActivityItem) -> Unit
) : ListAdapter<ActivityItem, ActivityAdapter.ActivityViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val binding = ItemActivityBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ActivityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ActivityViewHolder(
        private val binding: ItemActivityBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(pos))
                }
            }
        }

        fun bind(item: ActivityItem) {
            binding.tvActivityName.text = item.name ?: item.activityType.replaceFirstChar { it.uppercase() }
            binding.tvActivityDistance.text = String.format("%.2f km", item.distance / 1000)

            val minutes = item.duration / 60000
            val seconds = (item.duration % 60000) / 1000
            binding.tvActivityDuration.text = String.format("%d:%02d", minutes, seconds)

            val dateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
            binding.tvActivityDate.text = dateFormat.format(Date(item.startTime))

            val iconRes = when (item.activityType) {
                "running" -> R.drawable.ic_activities
                "cycling" -> R.drawable.ic_activities
                "walking" -> R.drawable.ic_activities
                "hiking" -> R.drawable.ic_elevation
                else -> R.drawable.ic_activities
            }
            binding.ivActivityIcon.setImageResource(iconRes)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ActivityItem>() {
        override fun areItemsTheSame(oldItem: ActivityItem, newItem: ActivityItem): Boolean {
            return oldItem.trackId == newItem.trackId
        }
        override fun areContentsTheSame(oldItem: ActivityItem, newItem: ActivityItem): Boolean {
            return oldItem == newItem
        }
    }
}
