package com.fittrackpro.ui.social.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fittrackpro.data.local.database.entity.TeamChallenge
import com.fittrackpro.databinding.ItemTeamBinding

class TeamAdapter(
    private val onJoinClick: (TeamChallenge) -> Unit
) : ListAdapter<TeamChallenge, TeamAdapter.TeamViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamViewHolder {
        val binding = ItemTeamBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TeamViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TeamViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TeamViewHolder(
        private val binding: ItemTeamBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(team: TeamChallenge) {
            binding.tvTeamName.text = team.teamName
            binding.tvMemberCount.text = "${team.memberCount} members"
            binding.tvTeamProgress.text = String.format("%.1f", team.totalProgress)
            binding.tvTeamRank.text = team.rank?.let { "#$it" } ?: "-"

            binding.btnJoinTeam.setOnClickListener {
                onJoinClick(team)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TeamChallenge>() {
        override fun areItemsTheSame(oldItem: TeamChallenge, newItem: TeamChallenge): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TeamChallenge, newItem: TeamChallenge): Boolean {
            return oldItem == newItem
        }
    }
}
