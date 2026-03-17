package com.fittrackpro.ui.social.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fittrackpro.data.local.database.entity.Challenge
import com.fittrackpro.databinding.ItemChallengeBinding
import java.util.concurrent.TimeUnit

class ChallengeAdapter(
    private val onItemClick: (Challenge) -> Unit
) : ListAdapter<Challenge, ChallengeAdapter.ChallengeViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val binding = ItemChallengeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ChallengeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChallengeViewHolder(
        private val binding: ItemChallengeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(pos))
                }
            }
        }

        fun bind(item: Challenge) {
            binding.tvChallengeName.text = item.name
            binding.tvChallengeType.text = item.type.replaceFirstChar { it.uppercase() }

            val goalText = when (item.goalUnit) {
                "km" -> String.format("%.1f km", item.goalValue)
                "minutes" -> "${item.goalValue.toInt()} min"
                "kcal" -> "${item.goalValue.toInt()} kcal"
                else -> "${item.goalValue.toInt()} ${item.goalUnit}"
            }
            binding.tvGoal.text = "Goal: $goalText"

            val daysLeft = TimeUnit.MILLISECONDS.toDays(item.endDate - System.currentTimeMillis())
            binding.tvDaysLeft.text = if (daysLeft > 0) "$daysLeft days left" else "Ended"

            binding.tvParticipants.text = if (item.maxParticipants != null) {
                "Max ${item.maxParticipants} participants"
            } else {
                "Open"
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Challenge>() {
        override fun areItemsTheSame(oldItem: Challenge, newItem: Challenge): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: Challenge, newItem: Challenge): Boolean {
            return oldItem == newItem
        }
    }
}
