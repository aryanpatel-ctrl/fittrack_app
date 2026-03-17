package com.fittrackpro.ui.coach

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.fittrackpro.databinding.FragmentCoachDashboardBinding
import com.fittrackpro.databinding.ItemClientBinding
import com.fittrackpro.data.repository.CoachRepository
import dagger.hilt.android.AndroidEntryPoint

/**
 * Coach Dashboard Fragment
 *
 * Displays:
 * - Coach profile summary
 * - Active clients list with status
 * - Pending invitations
 * - Quick actions (invite, message)
 */
@AndroidEntryPoint
class CoachDashboardFragment : Fragment() {

    private var _binding: FragmentCoachDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CoachDashboardViewModel by viewModels()

    private lateinit var clientsAdapter: ClientsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCoachDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Setup clients RecyclerView
        clientsAdapter = ClientsAdapter { client ->
            onClientClicked(client)
        }

        binding.rvClients.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = clientsAdapter
        }

        // Setup swipe refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }

        // Setup register as coach button (shown when not a coach)
        binding.btnRegisterCoach.setOnClickListener {
            showRegisterCoachDialog()
        }

        // Setup invite client button
        binding.fabInviteClient.setOnClickListener {
            showInviteClientDialog()
        }
    }

    private fun observeViewModel() {
        viewModel.isCoach.observe(viewLifecycleOwner) { isCoach ->
            binding.coachDashboardContent.visibility = if (isCoach) View.VISIBLE else View.GONE
            binding.notCoachContent.visibility = if (isCoach) View.GONE else View.VISIBLE
            binding.fabInviteClient.visibility = if (isCoach) View.VISIBLE else View.GONE
        }

        viewModel.coachProfile.observe(viewLifecycleOwner) { coach ->
            coach?.let {
                binding.tvCoachName.text = "Coach Dashboard"
                binding.tvSpecialty.text = it.specialty ?: "General Fitness"
                binding.tvRating.text = if (it.reviewCount > 0) {
                    "Rating: ${String.format("%.1f", it.rating)} (${it.reviewCount} reviews)"
                } else {
                    "No reviews yet"
                }
            }
        }

        viewModel.dashboardStats.observe(viewLifecycleOwner) { stats ->
            stats?.let {
                binding.tvActiveClients.text = "${it.activeClients}/${it.maxClients}"
                binding.tvPendingRequests.text = it.pendingRequests.toString()
                binding.progressClients.progress = it.getCapacityPercentage().toInt()

                if (it.isAtCapacity()) {
                    binding.tvCapacityWarning.visibility = View.VISIBLE
                } else {
                    binding.tvCapacityWarning.visibility = View.GONE
                }
            }
        }

        viewModel.clients.observe(viewLifecycleOwner) { clients ->
            clientsAdapter.submitList(clients)
            binding.tvEmptyClients.visibility = if (clients.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.pendingInvitations.observe(viewLifecycleOwner) { invitations ->
            binding.tvPendingRequests.text = invitations.size.toString()
            // Could show a badge or notification indicator
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccess()
            }
        }
    }

    private fun onClientClicked(client: ClientListItem) {
        // Navigate to client detail view
        viewModel.loadClientDetails(client.userId)
        showClientDetailDialog(client)
    }

    private fun showRegisterCoachDialog() {
        // In a real app, this would be a proper dialog or navigation to a registration form
        // For MVP, we'll use a simple dialog
        val dialogView = layoutInflater.inflate(
            android.R.layout.simple_list_item_1,
            null
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Register as Coach")
            .setMessage("Would you like to register as a fitness coach? You'll be able to manage clients and assign training plans.")
            .setPositiveButton("Register") { _, _ ->
                viewModel.registerAsCoach(
                    credentials = "Certified Personal Trainer",
                    specialty = "Running",
                    bio = "Passionate about helping athletes achieve their goals",
                    experienceYears = 3
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showInviteClientDialog() {
        // In a real app, this would show a search for users or email input
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Invite Client")
            .setMessage("Enter the client's user ID to send an invitation. In a full implementation, this would be a user search.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showClientDetailDialog(client: ClientListItem) {
        val message = """
            Client: ${client.name}
            Status: ${client.getStatusText()}
            Last Activity: ${client.getLastActivityText()}
            Active Plan: ${client.activePlanName ?: "No active plan"}
            Progress: ${client.completedWorkouts}/${client.totalWorkouts} workouts (${client.getCompletionPercentage()}%)
        """.trimIndent()

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Client Details")
            .setMessage(message)
            .setPositiveButton("Assign Plan") { _, _ ->
                showAssignPlanDialog(client)
            }
            .setNeutralButton("Send Feedback") { _, _ ->
                showFeedbackDialog(client)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showAssignPlanDialog(client: ClientListItem) {
        val goalTypes = arrayOf("5K", "10K", "Half Marathon", "Marathon", "Weight Loss")
        val difficulties = arrayOf("Beginner", "Intermediate", "Advanced")

        var selectedGoal = goalTypes[0]
        var selectedDifficulty = difficulties[0]

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Assign Training Plan")
            .setMessage("Select goal type for ${client.name}")
            .setSingleChoiceItems(goalTypes, 0) { _, which ->
                selectedGoal = goalTypes[which]
            }
            .setPositiveButton("Next") { _, _ ->
                // Second dialog for difficulty
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Select Difficulty")
                    .setSingleChoiceItems(difficulties, 0) { _, which ->
                        selectedDifficulty = difficulties[which]
                    }
                    .setPositiveButton("Assign") { _, _ ->
                        viewModel.assignPlanToClient(
                            clientId = client.userId,
                            goalType = selectedGoal,
                            difficulty = selectedDifficulty,
                            durationWeeks = 8,
                            daysPerWeek = when (selectedDifficulty) {
                                "Beginner" -> 3
                                "Intermediate" -> 4
                                else -> 5
                            }
                        )
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFeedbackDialog(client: ClientListItem) {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Enter your feedback..."
            minLines = 3
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Send Feedback to ${client.name}")
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                val feedback = input.text.toString()
                if (feedback.isNotBlank()) {
                    viewModel.addWorkoutFeedback(
                        clientId = client.userId,
                        workoutId = null,
                        feedback = feedback
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Adapter for clients list
 */
class ClientsAdapter(
    private val onClientClick: (ClientListItem) -> Unit
) : RecyclerView.Adapter<ClientsAdapter.ClientViewHolder>() {

    private var clients: List<ClientListItem> = emptyList()

    fun submitList(newClients: List<ClientListItem>) {
        clients = newClients
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientViewHolder {
        val binding = ItemClientBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ClientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClientViewHolder, position: Int) {
        holder.bind(clients[position])
    }

    override fun getItemCount(): Int = clients.size

    inner class ClientViewHolder(
        private val binding: ItemClientBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(client: ClientListItem) {
            binding.tvClientName.text = client.name
            binding.tvLastActivity.text = client.getLastActivityText()
            binding.tvPlanName.text = client.activePlanName ?: "No active plan"
            binding.tvProgress.text = "${client.completedWorkouts}/${client.totalWorkouts}"
            binding.progressBar.progress = client.getCompletionPercentage()

            // Set status indicator color
            binding.statusIndicator.setBackgroundColor(Color.parseColor(client.getStatusColor()))

            binding.root.setOnClickListener {
                onClientClick(client)
            }
        }
    }
}
