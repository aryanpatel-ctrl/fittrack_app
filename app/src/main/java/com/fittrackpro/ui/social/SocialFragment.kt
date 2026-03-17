package com.fittrackpro.ui.social

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fittrackpro.R
import com.fittrackpro.databinding.FragmentSocialBinding
import com.fittrackpro.ui.social.adapter.ChallengeAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SocialFragment : Fragment() {

    private var _binding: FragmentSocialBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SocialViewModel by viewModels()

    private val challengesAdapter = ChallengeAdapter { challenge ->
        findNavController().navigate(
            R.id.action_social_to_challenge_detail,
            bundleOf("challengeId" to challenge.id)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSocialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.rvChallenges.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = challengesAdapter
        }

        binding.fabCreateChallenge.setOnClickListener {
            findNavController().navigate(R.id.action_social_to_create_challenge)
        }

        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.loadActiveChallenges()
                    1 -> viewModel.loadDiscoverChallenges()
                    2 -> viewModel.loadMyChallenges()
                }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun observeViewModel() {
        viewModel.challenges.observe(viewLifecycleOwner) { challenges ->
            challengesAdapter.submitList(challenges)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
