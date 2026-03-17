package com.fittrackpro.ui.tracking.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fittrackpro.R
import com.fittrackpro.data.local.database.entity.Track
import com.fittrackpro.databinding.ItemActivityBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrackAdapter(
    private val onItemClick: (Track) -> Unit
) : ListAdapter<Track, TrackAdapter.TrackViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val binding = ItemActivityBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TrackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TrackViewHolder(
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

        fun bind(item: Track) {
            binding.tvActivityName.text = item.name ?: item.activityType.replaceFirstChar { it.uppercase() }

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

    class DiffCallback : DiffUtil.ItemCallback<Track>() {
        override fun areItemsTheSame(oldItem: Track, newItem: Track): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: Track, newItem: Track): Boolean {
            return oldItem == newItem
        }
    }
}
