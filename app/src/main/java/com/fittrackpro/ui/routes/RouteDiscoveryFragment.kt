package com.fittrackpro.ui.routes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fittrackpro.R
import com.fittrackpro.databinding.FragmentRouteDiscoveryBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RouteDiscoveryFragment : Fragment() {

    private var _binding: FragmentRouteDiscoveryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RouteDiscoveryViewModel by viewModels()
    private lateinit var routeAdapter: RouteAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRouteDiscoveryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        // Activity type filters
        binding.chipAll.setOnClickListener { viewModel.clearFilters() }
        binding.chipRunning.setOnClickListener { viewModel.filterByActivity("running") }
        binding.chipCycling.setOnClickListener { viewModel.filterByActivity("cycling") }
        binding.chipWalking.setOnClickListener { viewModel.filterByActivity("walking") }
        binding.chipHiking.setOnClickListener { viewModel.filterByActivity("hiking") }

        // Difficulty filters
        binding.chipEasy.setOnClickListener { viewModel.filterByDifficulty("easy") }
        binding.chipModerate.setOnClickListener { viewModel.filterByDifficulty("moderate") }
        binding.chipHard.setOnClickListener { viewModel.filterByDifficulty("hard") }
    }

    private fun setupRecyclerView() {
        routeAdapter = RouteAdapter(
            onItemClick = { route ->
                val action = RouteDiscoveryFragmentDirections.actionRouteDiscoveryToDetail(route.id)
                findNavController().navigate(action)
            },
            onFavoriteClick = { route ->
                viewModel.toggleFavorite(route)
            }
        )
        binding.rvRoutes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = routeAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.routes.observe(viewLifecycleOwner) { routes ->
            routeAdapter.submitList(routes)
            binding.layoutEmpty.visibility = if (routes.isEmpty()) View.VISIBLE else View.GONE
            binding.rvRoutes.visibility = if (routes.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
