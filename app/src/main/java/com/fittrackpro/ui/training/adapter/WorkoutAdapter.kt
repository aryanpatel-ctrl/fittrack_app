package com.fittrackpro.ui.training.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fittrackpro.R
import com.fittrackpro.data.local.database.entity.ScheduledWorkout
import com.fittrackpro.databinding.ItemWorkoutBinding
import com.fittrackpro.ui.training.ScheduledWorkoutWithName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorkoutAdapter(
    private val onItemClick: (ScheduledWorkout) -> Unit
) : ListAdapter<ScheduledWorkoutWithName, WorkoutAdapter.WorkoutViewHolder>(DiffCallback()) {

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
                    onItemClick(getItem(pos).scheduledWorkout)
                }
            }
        }

        fun bind(item: ScheduledWorkoutWithName) {
            val scheduled = item.scheduledWorkout
            val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
            binding.tvWorkoutDate.text = dateFormat.format(Date(scheduled.scheduledDate))
            binding.tvWorkoutName.text = item.workoutName
            binding.tvWorkoutStatus.text = scheduled.status.replaceFirstChar { it.uppercase() }

            val statusColor = when (scheduled.status) {
                "completed" -> R.color.success
                "skipped" -> R.color.error
                "pending" -> R.color.warning
                else -> R.color.text_secondary
            }
            binding.tvWorkoutStatus.setTextColor(binding.root.context.getColor(statusColor))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ScheduledWorkoutWithName>() {
        override fun areItemsTheSame(oldItem: ScheduledWorkoutWithName, newItem: ScheduledWorkoutWithName): Boolean {
            return oldItem.scheduledWorkout.id == newItem.scheduledWorkout.id
        }
        override fun areContentsTheSame(oldItem: ScheduledWorkoutWithName, newItem: ScheduledWorkoutWithName): Boolean {
            return oldItem == newItem
        }
    }
}
