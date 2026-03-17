package com.fittrackpro.ui.training.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fittrackpro.R
import com.fittrackpro.data.local.database.entity.ScheduledWorkout
import com.fittrackpro.databinding.ItemWorkoutBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorkoutAdapter(
    private val onItemClick: (ScheduledWorkout) -> Unit
) : ListAdapter<ScheduledWorkout, WorkoutAdapter.WorkoutViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val binding = ItemWorkoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return WorkoutViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WorkoutViewHolder(
        private val binding: ItemWorkoutBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(pos))
                }
            }
        }

        fun bind(item: ScheduledWorkout) {
            val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
            binding.tvWorkoutDate.text = dateFormat.format(Date(item.scheduledDate))
            binding.tvWorkoutName.text = item.notes ?: "Workout"
            binding.tvWorkoutStatus.text = item.status.replaceFirstChar { it.uppercase() }

            val statusColor = when (item.status) {
                "completed" -> R.color.success
                "skipped" -> R.color.error
                "pending" -> R.color.warning
                else -> R.color.text_secondary
            }
            binding.tvWorkoutStatus.setTextColor(binding.root.context.getColor(statusColor))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ScheduledWorkout>() {
        override fun areItemsTheSame(oldItem: ScheduledWorkout, newItem: ScheduledWorkout): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: ScheduledWorkout, newItem: ScheduledWorkout): Boolean {
            return oldItem == newItem
        }
    }
}
