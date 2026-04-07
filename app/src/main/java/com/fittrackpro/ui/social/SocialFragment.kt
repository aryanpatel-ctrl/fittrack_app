package com.fittrackpro.ui.social

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fittrackpro.R
import com.fittrackpro.databinding.FragmentSocialBinding
import com.fittrackpro.ui.social.adapter.ChallengeAdapter
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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

        binding.btnInvite.setOnClickListener {
            showInviteEmailDialog()
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadMyChallenges()
        }
    }

    private fun showInviteEmailDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_invite_email, null)

        val emailInput = dialogView.findViewById<TextInputEditText>(R.id.et_email)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.invite_friend)
            .setView(dialogView)
            .setPositiveButton(R.string.send_invite) { dialog, _ ->
                val email = emailInput.text?.toString()?.trim()
                if (!email.isNullOrEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    viewModel.sendInvite(email)
                    Toast.makeText(requireContext(), getString(R.string.invite_sent, email), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), R.string.invalid_email, Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.challenges.observe(viewLifecycleOwner) { challenges ->
            binding.swipeRefresh.isRefreshing = false

            if (challenges.isEmpty()) {
                binding.rvChallenges.visibility = View.GONE
                binding.layoutEmptyState.visibility = View.VISIBLE
            } else {
                binding.rvChallenges.visibility = View.VISIBLE
                binding.layoutEmptyState.visibility = View.GONE
                challengesAdapter.submitList(challenges)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
