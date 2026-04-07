package com.fittrackpro.ui.social

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fittrackpro.databinding.FragmentChallengeDetailBinding
import com.fittrackpro.ui.social.adapter.LeaderboardAdapter
import com.fittrackpro.ui.social.adapter.TeamAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChallengeDetailFragment : Fragment() {

    private var _binding: FragmentChallengeDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChallengeDetailViewModel by viewModels()
    private lateinit var leaderboardAdapter: LeaderboardAdapter
    private lateinit var teamAdapter: TeamAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChallengeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val challengeId = arguments?.getString("challengeId") ?: return
        viewModel.loadChallenge(challengeId)

        setupUI()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnJoin.setOnClickListener { viewModel.joinChallenge() }
        binding.btnLeave.setOnClickListener { viewModel.leaveChallenge() }
        binding.btnCreateTeam.setOnClickListener { showCreateTeamDialog() }
    }

    private fun setupRecyclerView() {
        leaderboardAdapter = LeaderboardAdapter()
        binding.rvLeaderboard.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = leaderboardAdapter
        }

        teamAdapter = TeamAdapter { team ->
            viewModel.joinTeam(team.id)
            Toast.makeText(requireContext(), "Joined ${team.teamName}", Toast.LENGTH_SHORT).show()
        }
        binding.rvTeams.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = teamAdapter
        }
    }

    private fun showCreateTeamDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Team Name"
            setPadding(48, 32, 48, 32)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create Team")
            .setMessage("Enter a name for your team")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val teamName = input.text.toString().trim()
                if (teamName.isNotEmpty()) {
                    viewModel.createTeam(teamName)
                } else {
                    Toast.makeText(requireContext(), "Team name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.challenge.observe(viewLifecycleOwner) { challenge ->
            challenge?.let {
                binding.tvChallengeName.text = it.name
                binding.tvChallengeDescription.text = it.description ?: "No description"
                binding.tvChallengeType.text = buildString {
                    append(it.type.replaceFirstChar { c -> c.uppercase() })
                    if (it.isTeamChallenge) append(" (Team)")
                }

                val goalText = when (it.goalUnit) {
                    "km" -> String.format("%.1f km", it.goalValue)
                    "minutes" -> "${it.goalValue.toInt()} min"
                    "kcal" -> "${it.goalValue.toInt()} kcal"
                    else -> "${it.goalValue.toInt()} ${it.goalUnit}"
                }
                binding.tvGoalValue.text = goalText

                val daysLeft = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(
                    it.endDate - System.currentTimeMillis()
                )
                binding.tvTimeRemaining.text = if (daysLeft > 0) "$daysLeft days left" else "Challenge ended"

                // Show teams or leaderboard based on challenge type
                if (it.isTeamChallenge) {
                    binding.layoutTeams.visibility = View.VISIBLE
                    binding.layoutLeaderboard.visibility = View.GONE
                } else {
                    binding.layoutTeams.visibility = View.GONE
                    binding.layoutLeaderboard.visibility = View.VISIBLE
                }
            }
        }

        viewModel.isJoined.observe(viewLifecycleOwner) { joined ->
            binding.btnJoin.visibility = if (joined) View.GONE else View.VISIBLE
            binding.btnLeave.visibility = if (joined) View.VISIBLE else View.GONE
            binding.btnCreateTeam.visibility = if (joined) View.GONE else View.VISIBLE
        }

        viewModel.leaderboard.observe(viewLifecycleOwner) { entries ->
            leaderboardAdapter.submitList(entries)
        }

        viewModel.teams.observe(viewLifecycleOwner) { teams ->
            teamAdapter.submitList(teams)
        }

        viewModel.teamCreated.observe(viewLifecycleOwner) { created ->
            if (created) {
                Toast.makeText(requireContext(), "Team created successfully!", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.userProgress.observe(viewLifecycleOwner) { progress ->
            progress?.let {
                binding.tvYourProgress.text = String.format("%.1f", it.progress)
                binding.tvYourRank.text = if (it.rank != null) "#${it.rank}" else "-"
                binding.progressChallenge.progress =
                    ((it.progress / (viewModel.challenge.value?.goalValue ?: 1.0)) * 100).toInt().coerceAtMost(100)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
