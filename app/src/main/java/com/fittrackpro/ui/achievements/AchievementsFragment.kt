package com.fittrackpro.ui.achievements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.fittrackpro.databinding.FragmentAchievementsBinding
import com.fittrackpro.ui.achievements.adapter.AchievementAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AchievementsFragment : Fragment() {

    private var _binding: FragmentAchievementsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AchievementsViewModel by viewModels()
    private lateinit var achievementAdapter: AchievementAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAchievementsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.chipAll.setOnClickListener {
            viewModel.filterAchievements(AchievementFilter.ALL)
        }

        binding.chipEarned.setOnClickListener {
            viewModel.filterAchievements(AchievementFilter.EARNED)
        }

        binding.chipInProgress.setOnClickListener {
            viewModel.filterAchievements(AchievementFilter.IN_PROGRESS)
        }

        binding.chipLocked.setOnClickListener {
            viewModel.filterAchievements(AchievementFilter.LOCKED)
        }
    }

    private fun setupRecyclerView() {
        achievementAdapter = AchievementAdapter { achievementWithStatus ->
            showAchievementDialog(achievementWithStatus)
        }

        binding.rvAchievements.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = achievementAdapter
        }
    }

    private fun showAchievementDialog(achievementWithStatus: AchievementWithStatus) {
        val achievement = achievementWithStatus.achievement
        val userAchievement = achievementWithStatus.userAchievement
        val isEarned = userAchievement?.earnedAt != null

        val status = if (isEarned) {
            "Completed!"
        } else {
            val progress = userAchievement?.progress ?: 0.0
            "Progress: ${progress.toInt()} / ${achievement.criteriaValue.toInt()}"
        }

        val message = buildString {
            append(achievement.description)
            append("\n\n")
            append("Tier: ${achievement.tier.replaceFirstChar { it.uppercase() }}")
            append("\nReward: +${achievement.xpReward} XP")
            append("\n\n$status")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(achievement.name)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.userStats.observe(viewLifecycleOwner) { stats ->
            stats?.let {
                binding.tvLevel.text = "Level ${it.level}"
                binding.tvTotalXp.text = "${it.totalXp} XP"
                binding.progressLevel.progress = viewModel.calculateLevelProgress(it.totalXp, it.level)
                binding.tvXpToNextLevel.text = "${viewModel.xpToNextLevel(it.totalXp, it.level)} XP to next level"
            }
        }

        viewModel.achievements.observe(viewLifecycleOwner) { achievements ->
            achievementAdapter.submitList(achievements)

            val earned = achievements.count { it.userAchievement != null }
            binding.tvAchievementCount.text = "$earned / ${achievements.size} achievements"
        }

        viewModel.streaks.observe(viewLifecycleOwner) { streaks ->
            streaks.forEach { streak ->
                when (streak.streakType) {
                    "daily_activity" -> {
                        binding.tvCurrentStreak.text = "${streak.currentCount}"
                        binding.tvBestStreak.text = "Best: ${streak.bestCount}"
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
