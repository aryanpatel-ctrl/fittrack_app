package com.fittrackpro.ui.achievements.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fittrackpro.R
import com.fittrackpro.databinding.ItemAchievementBinding
import com.fittrackpro.ui.achievements.AchievementWithStatus

class AchievementAdapter(
    private val onAchievementClick: (AchievementWithStatus) -> Unit
) : ListAdapter<AchievementWithStatus, AchievementAdapter.AchievementViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val binding = ItemAchievementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AchievementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AchievementViewHolder(
        private val binding: ItemAchievementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onAchievementClick(getItem(pos))
                }
            }
        }

        fun bind(item: AchievementWithStatus) {
            val achievement = item.achievement
            val userAchievement = item.userAchievement
            val isEarned = userAchievement?.earnedAt != null

            binding.tvAchievementName.text = achievement.name
            binding.tvAchievementDescription.text = achievement.description
            binding.tvXpReward.text = "+${achievement.xpReward} XP"

            // Use a default icon for all achievement badges
            binding.ivBadge.setImageResource(R.drawable.ic_achievement_default)

            // Show progress or completion status
            if (isEarned) {
                binding.progressAchievement.visibility = android.view.View.GONE
                binding.tvProgress.text = "Completed!"
                binding.tvProgress.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.success)
                )
                binding.cardAchievement.alpha = 1.0f
            } else {
                val progress = userAchievement?.progress ?: 0.0
                val target = achievement.criteriaValue
                val progressPercent = ((progress / target) * 100).toInt()

                binding.progressAchievement.visibility = android.view.View.VISIBLE
                binding.progressAchievement.progress = progressPercent
                binding.tvProgress.text = "${progress.toInt()} / ${target.toInt()}"
                binding.tvProgress.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.text_secondary)
                )
                binding.cardAchievement.alpha = if (progress > 0) 0.9f else 0.6f
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AchievementWithStatus>() {
        override fun areItemsTheSame(
            oldItem: AchievementWithStatus,
            newItem: AchievementWithStatus
        ): Boolean {
            return oldItem.achievement.id == newItem.achievement.id
        }

        override fun areContentsTheSame(
            oldItem: AchievementWithStatus,
            newItem: AchievementWithStatus
        ): Boolean {
            return oldItem == newItem
        }
    }
}
