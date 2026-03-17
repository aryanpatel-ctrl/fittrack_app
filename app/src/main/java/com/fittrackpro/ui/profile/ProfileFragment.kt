package com.fittrackpro.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.fittrackpro.R
import com.fittrackpro.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Setup menu items
        binding.menuAchievements.root.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_achievements)
        }

        binding.menuNutrition.root.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_nutrition)
        }

        binding.menuSettings.root.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_settings)
        }

        binding.menuLogout.root.setOnClickListener {
            viewModel.logout()
        }

        // Set menu item titles and icons
        binding.menuAchievements.tvTitle.text = getString(R.string.achievements)
        binding.menuNutrition.tvTitle.text = getString(R.string.nutrition)
        binding.menuSettings.tvTitle.text = getString(R.string.settings)
        binding.menuLogout.tvTitle.text = getString(R.string.logout)
    }

    private fun observeViewModel() {
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.tvName.text = name ?: "User"
        }

        viewModel.userStats.observe(viewLifecycleOwner) { stats ->
            binding.tvLevel.text = "Level ${stats.level} • ${stats.totalXp} XP"
            binding.tvTotalActivities.text = stats.totalActivities.toString()
            binding.tvTotalDistance.text = "${(stats.totalDistance / 1000).toInt()} km"
        }

        viewModel.currentStreak.observe(viewLifecycleOwner) { streak ->
            binding.tvStreak.text = streak.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
