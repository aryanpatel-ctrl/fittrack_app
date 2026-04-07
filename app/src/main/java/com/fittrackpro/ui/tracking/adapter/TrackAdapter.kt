package com.fittrackpro.ui.tracking.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fittrackpro.R
import com.fittrackpro.data.local.database.entity.Track
import com.fittrackpro.databinding.ItemActivityBinding
import com.fittrackpro.ui.tracking.TrackWithStats
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrackAdapter(
    private val onItemClick: (Track) -> Unit
) : ListAdapter<TrackWithStats, TrackAdapter.TrackViewHolder>(DiffCallback()) {

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
                    onItemClick(getItem(pos).track)
                }
            }
        }

        fun bind(item: TrackWithStats) {
            val track = item.track
            val stats = item.stats

            binding.tvActivityName.text = track.name ?: track.activityType.replaceFirstChar { it.uppercase() }

            val dateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
            binding.tvActivityDate.text = dateFormat.format(Date(track.startTime))

            // Display distance
            val distanceKm = (stats?.distance ?: 0.0) / 1000
            binding.tvActivityDistance.text = String.format("%.2f km", distanceKm)

            // Display duration
            val durationMs = stats?.duration ?: 0L
            val minutes = durationMs / 60000
            val seconds = (durationMs % 60000) / 1000
            binding.tvActivityDuration.text = String.format("%d:%02d", minutes, seconds)

            val iconRes = when (track.activityType) {
                "running" -> R.drawable.ic_running
                "cycling" -> R.drawable.ic_cycling
                "walking" -> R.drawable.ic_walking
                "hiking" -> R.drawable.ic_elevation
                else -> R.drawable.ic_activities
            }
            binding.ivActivityIcon.setImageResource(iconRes)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TrackWithStats>() {
        override fun areItemsTheSame(oldItem: TrackWithStats, newItem: TrackWithStats): Boolean {
            return oldItem.track.id == newItem.track.id
        }
        override fun areContentsTheSame(oldItem: TrackWithStats, newItem: TrackWithStats): Boolean {
            return oldItem == newItem
        }
    }
}
