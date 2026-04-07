package com.fittrackpro.ui.routes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.fittrackpro.R
import com.fittrackpro.databinding.FragmentRouteDetailBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RouteDetailFragment : Fragment() {

    private var _binding: FragmentRouteDetailBinding? = null
    private val binding get() = _binding!!

    private val args: RouteDetailFragmentArgs by navArgs()
    private val viewModel: RouteDetailViewModel by viewModels()
    private lateinit var reviewAdapter: ReviewAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRouteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadRoute(args.routeId)
        setupUI()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnFavorite.setOnClickListener {
            viewModel.toggleFavorite()
        }

        binding.btnSubmitRating.setOnClickListener {
            val rating = binding.ratingBarInput.rating
            val review = binding.etReview.text?.toString()?.takeIf { it.isNotBlank() }

            if (rating > 0) {
                viewModel.submitRating(rating, review, null, null, null)
                binding.ratingBarInput.rating = 0f
                binding.etReview.text?.clear()
            } else {
                Toast.makeText(requireContext(), "Please select a rating", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnUseRoute.setOnClickListener {
            viewModel.useRoute()
            Toast.makeText(requireContext(), "Route loaded! Starting activity...", Toast.LENGTH_SHORT).show()
            // Navigate to live tracking - use LiveTrackingActivity directly
            val intent = android.content.Intent(requireContext(), com.fittrackpro.ui.tracking.LiveTrackingActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        reviewAdapter = ReviewAdapter()
        binding.rvReviews.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reviewAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.route.observe(viewLifecycleOwner) { route ->
            route?.let {
                binding.tvRouteName.text = it.name
                binding.tvRouteLocation.text = listOfNotNull(it.city, it.country)
                    .joinToString(", ")
                    .ifEmpty { "Unknown location" }
                binding.tvRouteDescription.text = it.description ?: "No description available"

                // Stats
                binding.tvDistance.text = String.format("%.1f km", it.distance / 1000)
                binding.tvElevation.text = String.format("%.0f m", it.elevationGain)
                binding.tvDifficulty.text = it.difficulty.replaceFirstChar { c -> c.uppercase() }

                // Difficulty color
                val difficultyColor = when (it.difficulty.lowercase()) {
                    "easy" -> R.color.success
                    "moderate" -> R.color.warning
                    "hard" -> R.color.error
                    else -> R.color.text_secondary
                }
                binding.tvDifficulty.setTextColor(requireContext().getColor(difficultyColor))

                // Rating
                if (it.ratingCount > 0) {
                    binding.tvAvgRating.text = String.format("%.1f", it.avgRating)
                    binding.ratingBarDisplay.rating = it.avgRating
                    binding.tvRatingCount.text = "${it.ratingCount} reviews"
                } else {
                    binding.tvAvgRating.text = "-"
                    binding.ratingBarDisplay.rating = 0f
                    binding.tvRatingCount.text = "No reviews yet"
                }

                // Favorite
                updateFavoriteIcon(it.isFavorite)
            }
        }

        viewModel.ratings.observe(viewLifecycleOwner) { ratings ->
            reviewAdapter.submitList(ratings)
        }

        viewModel.ratingSubmitted.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Rating submitted!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateFavoriteIcon(isFavorite: Boolean) {
        val icon = if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite
        binding.btnFavorite.setImageResource(icon)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
