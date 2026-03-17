package com.fittrackpro.ui.coach

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.database.dao.UserDao
import com.fittrackpro.data.local.database.entity.Coach
import com.fittrackpro.data.local.database.entity.CoachClientRelationship
import com.fittrackpro.data.local.database.entity.User
import com.fittrackpro.data.local.preferences.UserPreferences
import com.fittrackpro.data.repository.CoachRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Coach Dashboard
 *
 * Handles:
 * - Coach profile display
 * - Client list management
 * - Dashboard statistics
 * - Client invitations
 */
@HiltViewModel
class CoachDashboardViewModel @Inject constructor(
    private val coachRepository: CoachRepository,
    private val userDao: UserDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    // Coach profile
    private val _coachProfile = MutableLiveData<Coach?>()
    val coachProfile: LiveData<Coach?> = _coachProfile

    private val _isCoach = MutableLiveData<Boolean>()
    val isCoach: LiveData<Boolean> = _isCoach

    // Dashboard stats
    private val _dashboardStats = MutableLiveData<CoachRepository.CoachDashboardStats?>()
    val dashboardStats: LiveData<CoachRepository.CoachDashboardStats?> = _dashboardStats

    // Clients list
    private val _clients = MutableLiveData<List<ClientListItem>>()
    val clients: LiveData<List<ClientListItem>> = _clients

    // Pending invitations
    private val _pendingInvitations = MutableLiveData<List<CoachClientRelationship>>()
    val pendingInvitations: LiveData<List<CoachClientRelationship>> = _pendingInvitations

    // Selected client details
    private val _selectedClientDetails = MutableLiveData<CoachRepository.ClientDetails?>()
    val selectedClientDetails: LiveData<CoachRepository.ClientDetails?> = _selectedClientDetails

    // Loading states
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error handling
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Success messages
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    private var coachId: String? = null

    init {
        loadCoachData()
    }

    /**
     * Load coach profile and data
     */
    private fun loadCoachData() {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = userPreferences.userId

            if (userId == null) {
                _isCoach.value = false
                _isLoading.value = false
                return@launch
            }

            val coach = coachRepository.getCoachProfile(userId)
            _isCoach.value = coach != null
            _coachProfile.value = coach

            if (coach != null) {
                coachId = coach.id
                loadDashboardStats(coach.id)
                loadClients(coach.id)
                loadPendingInvitations(coach.id)
            }

            _isLoading.value = false
        }
    }

    /**
     * Load dashboard statistics
     */
    private suspend fun loadDashboardStats(coachId: String) {
        val stats = coachRepository.getCoachDashboardStats(coachId)
        _dashboardStats.value = stats
    }

    /**
     * Load client list with status
     */
    private fun loadClients(coachId: String) {
        viewModelScope.launch {
            coachRepository.getActiveClients(coachId).collect { users ->
                val clientItems = users.map { user ->
                    // Get client details for status
                    val details = coachRepository.getClientDetails(user.id, coachId)
                    ClientListItem(
                        userId = user.id,
                        name = user.name,
                        profileImage = user.profileImage,
                        status = details?.status ?: CoachRepository.ClientStatus.INACTIVE,
                        lastActivityDate = details?.lastActivityDate,
                        activePlanName = details?.activePlan?.name,
                        completedWorkouts = details?.completedWorkouts ?: 0,
                        totalWorkouts = (details?.completedWorkouts ?: 0) + (details?.missedWorkouts ?: 0) + (details?.upcomingWorkouts ?: 0)
                    )
                }
                _clients.value = clientItems.sortedByDescending { it.lastActivityDate ?: 0L }
            }
        }
    }

    /**
     * Load pending client invitations
     */
    private fun loadPendingInvitations(coachId: String) {
        viewModelScope.launch {
            coachRepository.getPendingInvitations(coachId).collect { invitations ->
                _pendingInvitations.value = invitations
            }
        }
    }

    /**
     * Load selected client details
     */
    fun loadClientDetails(clientId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val cId = coachId ?: return@launch

            val details = coachRepository.getClientDetails(clientId, cId)
            _selectedClientDetails.value = details

            _isLoading.value = false
        }
    }

    /**
     * Invite a new client
     */
    fun inviteClient(clientId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val cId = coachId ?: return@launch

            val result = coachRepository.inviteClient(cId, clientId)
            result.onSuccess {
                _successMessage.value = "Invitation sent successfully"
                loadDashboardStats(cId)
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to send invitation"
            }

            _isLoading.value = false
        }
    }

    /**
     * Accept pending invitation
     */
    fun acceptInvitation(relationshipId: String) {
        viewModelScope.launch {
            val result = coachRepository.acceptInvitation(relationshipId)
            result.onSuccess {
                _successMessage.value = "Invitation accepted"
                coachId?.let { loadDashboardStats(it) }
            }.onFailure { error ->
                _errorMessage.value = error.message
            }
        }
    }

    /**
     * Decline pending invitation
     */
    fun declineInvitation(relationshipId: String) {
        viewModelScope.launch {
            val result = coachRepository.declineInvitation(relationshipId)
            result.onSuccess {
                _successMessage.value = "Invitation declined"
            }.onFailure { error ->
                _errorMessage.value = error.message
            }
        }
    }

    /**
     * End relationship with a client
     */
    fun endClientRelationship(clientId: String) {
        viewModelScope.launch {
            val cId = coachId ?: return@launch
            val details = _selectedClientDetails.value ?: return@launch

            val result = coachRepository.endRelationship(details.relationshipId)
            result.onSuccess {
                _successMessage.value = "Client relationship ended"
                loadDashboardStats(cId)
                loadClients(cId)
                _selectedClientDetails.value = null
            }.onFailure { error ->
                _errorMessage.value = error.message
            }
        }
    }

    /**
     * Add feedback to a client's workout
     */
    fun addWorkoutFeedback(
        clientId: String,
        workoutId: String?,
        feedback: String,
        rating: Int? = null
    ) {
        viewModelScope.launch {
            val cId = coachId ?: return@launch

            val result = coachRepository.addWorkoutFeedback(cId, clientId, workoutId, feedback, rating)
            result.onSuccess {
                _successMessage.value = "Feedback sent to client"
            }.onFailure { error ->
                _errorMessage.value = error.message
            }
        }
    }

    /**
     * Send message to client
     */
    fun sendMessage(clientId: String, message: String) {
        viewModelScope.launch {
            val userId = userPreferences.userId ?: return@launch

            val result = coachRepository.sendMessage(userId, clientId, message)
            result.onSuccess {
                _successMessage.value = "Message sent"
            }.onFailure { error ->
                _errorMessage.value = error.message
            }
        }
    }

    /**
     * Assign training plan to client
     */
    fun assignPlanToClient(
        clientId: String,
        goalType: String,
        difficulty: String,
        durationWeeks: Int,
        daysPerWeek: Int,
        notes: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val cId = coachId ?: return@launch

            val result = coachRepository.assignPlanToClient(
                coachId = cId,
                clientId = clientId,
                goalType = goalType,
                difficulty = difficulty,
                durationWeeks = durationWeeks,
                daysPerWeek = daysPerWeek,
                notes = notes
            )

            result.onSuccess { assignment ->
                _successMessage.value = "Training plan assigned successfully"
                loadClientDetails(clientId)
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to assign plan"
            }

            _isLoading.value = false
        }
    }

    /**
     * Register current user as coach
     */
    fun registerAsCoach(
        credentials: String?,
        specialty: String?,
        bio: String?,
        experienceYears: Int?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = userPreferences.userId ?: return@launch

            val result = coachRepository.registerAsCoach(
                userId = userId,
                credentials = credentials,
                specialty = specialty,
                bio = bio,
                experienceYears = experienceYears
            )

            result.onSuccess { coach ->
                _coachProfile.value = coach
                _isCoach.value = true
                coachId = coach.id
                _successMessage.value = "Successfully registered as coach"
                loadDashboardStats(coach.id)
            }.onFailure { error ->
                _errorMessage.value = error.message
            }

            _isLoading.value = false
        }
    }

    /**
     * Refresh all data
     */
    fun refresh() {
        loadCoachData()
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Clear success message
     */
    fun clearSuccess() {
        _successMessage.value = null
    }
}

/**
 * Client list item for display
 */
data class ClientListItem(
    val userId: String,
    val name: String,
    val profileImage: String?,
    val status: CoachRepository.ClientStatus,
    val lastActivityDate: Long?,
    val activePlanName: String?,
    val completedWorkouts: Int,
    val totalWorkouts: Int
) {
    fun getStatusColor(): String = when (status) {
        CoachRepository.ClientStatus.ACTIVE -> "#4CAF50"
        CoachRepository.ClientStatus.AT_RISK -> "#FF9800"
        CoachRepository.ClientStatus.INACTIVE -> "#F44336"
    }

    fun getStatusText(): String = when (status) {
        CoachRepository.ClientStatus.ACTIVE -> "Active"
        CoachRepository.ClientStatus.AT_RISK -> "At Risk"
        CoachRepository.ClientStatus.INACTIVE -> "Inactive"
    }

    fun getCompletionPercentage(): Int {
        return if (totalWorkouts > 0) {
            (completedWorkouts.toFloat() / totalWorkouts * 100).toInt()
        } else 0
    }

    fun getLastActivityText(): String {
        if (lastActivityDate == null) return "No activity"
        val diff = System.currentTimeMillis() - lastActivityDate
        val days = diff / (24 * 60 * 60 * 1000)
        return when {
            days == 0L -> "Today"
            days == 1L -> "Yesterday"
            days < 7 -> "$days days ago"
            else -> "${days / 7} weeks ago"
        }
    }
}
