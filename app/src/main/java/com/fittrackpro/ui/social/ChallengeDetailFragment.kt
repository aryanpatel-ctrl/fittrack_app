package com.fittrackpro.ui.social

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fittrackpro.databinding.FragmentChallengeDetailBinding
import com.fittrackpro.ui.social.adapter.LeaderboardAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChallengeDetailFragment : Fragment() {

    private var _binding: FragmentChallengeDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChallengeDetailViewModel by viewModels()
    private lateinit var leaderboardAdapter: LeaderboardAdapter

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
    }

    private fun setupRecyclerView() {
        leaderboardAdapter = LeaderboardAdapter()
        binding.rvLeaderboard.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = leaderboardAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.challenge.observe(viewLifecycleOwner) { challenge ->
            challenge?.let {
                binding.tvChallengeName.text = it.name
                binding.tvChallengeDescription.text = it.description ?: "No description"
                binding.tvChallengeType.text = it.type.replaceFirstChar { c -> c.uppercase() }

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
            }
        }

        viewModel.isJoined.observe(viewLifecycleOwner) { joined ->
            binding.btnJoin.visibility = if (joined) View.GONE else View.VISIBLE
            binding.btnLeave.visibility = if (joined) View.VISIBLE else View.GONE
        }

        viewModel.leaderboard.observe(viewLifecycleOwner) { entries ->
            leaderboardAdapter.submitList(entries)
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
