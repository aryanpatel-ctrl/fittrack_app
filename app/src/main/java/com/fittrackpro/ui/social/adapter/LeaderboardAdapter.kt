package com.fittrackpro.ui.social.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fittrackpro.R
import com.fittrackpro.data.local.database.entity.ChallengeLeaderboard
import com.fittrackpro.databinding.ItemLeaderboardBinding

class LeaderboardAdapter : ListAdapter<ChallengeLeaderboard, LeaderboardAdapter.LeaderboardViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val binding = ItemLeaderboardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LeaderboardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LeaderboardViewHolder(
        private val binding: ItemLeaderboardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChallengeLeaderboard) {
            binding.tvRank.text = "#${item.rank}"
            binding.tvUserName.text = item.userName
            binding.tvScore.text = String.format("%.1f", item.score)

            when (item.rank) {
                1 -> binding.tvRank.setTextColor(binding.root.context.getColor(R.color.gold))
                2 -> binding.tvRank.setTextColor(binding.root.context.getColor(R.color.silver))
                3 -> binding.tvRank.setTextColor(binding.root.context.getColor(R.color.bronze))
                else -> binding.tvRank.setTextColor(binding.root.context.getColor(R.color.text_secondary))
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChallengeLeaderboard>() {
        override fun areItemsTheSame(oldItem: ChallengeLeaderboard, newItem: ChallengeLeaderboard): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: ChallengeLeaderboard, newItem: ChallengeLeaderboard): Boolean {
            return oldItem == newItem
        }
    }
}
