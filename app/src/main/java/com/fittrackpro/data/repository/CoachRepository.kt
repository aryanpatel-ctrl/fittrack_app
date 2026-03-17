package com.fittrackpro.data.repository

import android.util.Log
import com.fittrackpro.data.local.database.dao.CoachDao
import com.fittrackpro.data.local.database.dao.ScheduledWorkoutDao
import com.fittrackpro.data.local.database.dao.TrackDao
import com.fittrackpro.data.local.database.dao.TrainingPlanDao
import com.fittrackpro.data.local.database.dao.WorkoutTemplateDao
import com.fittrackpro.data.local.database.entity.*
import com.fittrackpro.util.TrainingPlanGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Coach-Client Management Portal
 *
 * Handles all coach-related business logic including:
 * - Coach profile management
 * - Client invitations and relationships
 * - Plan assignments
 * - Workout feedback
 * - Progress monitoring
 */
@Singleton
class CoachRepository @Inject constructor(
    private val coachDao: CoachDao,
    private val trackDao: TrackDao,
    private val trainingPlanDao: TrainingPlanDao,
    private val workoutTemplateDao: WorkoutTemplateDao,
    private val scheduledWorkoutDao: ScheduledWorkoutDao
) {
    companion object {
        private const val TAG = "CoachRepository"
    }

    // ==================== Coach Profile Management ====================

    /**
     * Register a user as a coach
     */
    suspend fun registerAsCoach(
        userId: String,
        credentials: String?,
        specialty: String?,
        bio: String?,
        experienceYears: Int?
    ): Result<Coach> {
        return try {
            val existingCoach = coachDao.getCoachByUserId(userId)
            if (existingCoach != null) {
                return Result.failure(Exception("User is already registered as a coach"))
            }

            val coach = Coach(
                id = UUID.randomUUID().toString(),
                userId = userId,
                credentials = credentials,
                specialty = specialty,
                bio = bio,
                experienceYears = experienceYears,
                isVerified = false, // Admin will verify
                acceptingClients = true,
                maxClients = 20,
                currentClients = 0,
                rating = 0f,
                reviewCount = 0
            )

            coachDao.insertCoach(coach)
            Result.success(coach)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register coach", e)
            Result.failure(e)
        }
    }

    /**
     * Get coach profile by user ID
     */
    suspend fun getCoachProfile(userId: String): Coach? {
        return coachDao.getCoachByUserId(userId)
    }

    /**
     * Get coach profile as Flow
     */
    fun getCoachProfileFlow(userId: String): Flow<Coach?> {
        return coachDao.getCoachByUserIdFlow(userId)
    }

    /**
     * Update coach profile
     */
    suspend fun updateCoachProfile(coach: Coach): Result<Unit> {
        return try {
            coachDao.updateCoach(coach)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if user is a coach
     */
    suspend fun isUserACoach(userId: String): Boolean {
        return coachDao.getCoachByUserId(userId) != null
    }

    // ==================== Client Management ====================

    /**
     * Get all active clients for a coach
     */
    fun getActiveClients(coachId: String): Flow<List<User>> {
        return coachDao.getClientsForCoach(coachId)
    }

    /**
     * Get client details with recent activity stats
     */
    suspend fun getClientDetails(clientId: String, coachId: String): ClientDetails? {
        val relationship = coachDao.getRelationship(coachId, clientId) ?: return null
        if (relationship.status != "active") return null

        val recentActivities = coachDao.getClientRecentActivities(clientId, 10)
        val upcomingWorkouts = coachDao.getClientUpcomingWorkouts(
            clientId,
            System.currentTimeMillis(),
            5
        )

        // Calculate stats
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val recentTracks = trackDao.getTracksByDateRange(clientId, thirtyDaysAgo, System.currentTimeMillis()).first()
        val stats = recentTracks.mapNotNull { trackDao.getStatisticsByTrackId(it.id) }

        val totalDistance = stats.sumOf { it.distance }
        val totalWorkouts = recentTracks.size
        val avgPace = if (stats.isNotEmpty()) stats.map { it.avgPace }.average().toFloat() else 0f

        // Get completed workout count
        val completedCount = scheduledWorkoutDao.getCompletedWorkoutCount(clientId)
        val missedCount = scheduledWorkoutDao.getMissedWorkoutCount(clientId, System.currentTimeMillis())

        // Calculate last activity
        val lastActivity = recentActivities.maxByOrNull { it.startTime }

        // Determine client status
        val status = when {
            lastActivity == null -> ClientStatus.INACTIVE
            System.currentTimeMillis() - lastActivity.startTime < 3 * 24 * 60 * 60 * 1000L -> ClientStatus.ACTIVE
            System.currentTimeMillis() - lastActivity.startTime < 7 * 24 * 60 * 60 * 1000L -> ClientStatus.AT_RISK
            else -> ClientStatus.INACTIVE
        }

        // Get active plan
        val activePlan = getClientActivePlan(clientId)

        return ClientDetails(
            clientId = clientId,
            relationshipId = relationship.id,
            startDate = relationship.startDate ?: relationship.createdAt,
            status = status,
            lastActivityDate = lastActivity?.startTime,
            totalDistanceLast30Days = totalDistance,
            totalWorkoutsLast30Days = totalWorkouts,
            avgPace = avgPace,
            completedWorkouts = completedCount,
            missedWorkouts = missedCount,
            upcomingWorkouts = upcomingWorkouts.size,
            activePlan = activePlan,
            recentActivities = recentActivities
        )
    }

    /**
     * Get client's active training plan
     */
    private suspend fun getClientActivePlan(clientId: String): TrainingPlan? {
        val progress = trainingPlanDao.getActivePlanProgress(clientId) ?: return null
        return trainingPlanDao.getPlanById(progress.planId)
    }

    // ==================== Client Invitation System ====================

    /**
     * Invite a client by user ID
     */
    suspend fun inviteClient(coachId: String, clientId: String): Result<CoachClientRelationship> {
        return try {
            // Check if relationship already exists
            val existing = coachDao.getRelationship(coachId, clientId)
            if (existing != null) {
                return Result.failure(Exception("Relationship already exists"))
            }

            // Check coach capacity
            val coach = coachDao.getCoachById(coachId) ?: return Result.failure(Exception("Coach not found"))
            if (coach.currentClients >= coach.maxClients) {
                return Result.failure(Exception("Coach has reached maximum client capacity"))
            }

            val relationship = CoachClientRelationship(
                id = UUID.randomUUID().toString(),
                coachId = coachId,
                clientId = clientId,
                status = "pending"
            )

            coachDao.insertRelationship(relationship)
            Result.success(relationship)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to invite client", e)
            Result.failure(e)
        }
    }

    /**
     * Accept client invitation
     */
    suspend fun acceptInvitation(relationshipId: String): Result<Unit> {
        return try {
            val relationship = coachDao.getRelationshipById(relationshipId)
                ?: return Result.failure(Exception("Invitation not found"))

            if (relationship.status != "pending") {
                return Result.failure(Exception("Invitation is no longer pending"))
            }

            coachDao.updateRelationshipStatus(
                relationshipId = relationshipId,
                status = "active",
                startDate = System.currentTimeMillis()
            )

            coachDao.incrementClientCount(relationship.coachId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Decline client invitation
     */
    suspend fun declineInvitation(relationshipId: String): Result<Unit> {
        return try {
            coachDao.updateRelationshipStatus(
                relationshipId = relationshipId,
                status = "declined",
                startDate = null
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * End coach-client relationship
     */
    suspend fun endRelationship(relationshipId: String): Result<Unit> {
        return try {
            val relationship = coachDao.getRelationshipById(relationshipId)
                ?: return Result.failure(Exception("Relationship not found"))

            coachDao.updateRelationshipStatus(
                relationshipId = relationshipId,
                status = "ended",
                startDate = relationship.startDate
            )

            coachDao.decrementClientCount(relationship.coachId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get pending invitations for a coach
     */
    fun getPendingInvitations(coachId: String): Flow<List<CoachClientRelationship>> {
        return coachDao.getPendingRequests(coachId)
    }

    // ==================== Plan Assignment ====================

    /**
     * Assign a training plan to a client
     */
    suspend fun assignPlanToClient(
        coachId: String,
        clientId: String,
        goalType: String,
        difficulty: String,
        durationWeeks: Int,
        daysPerWeek: Int,
        notes: String? = null
    ): Result<PlanAssignment> {
        return try {
            // Verify coach-client relationship
            val relationship = coachDao.getRelationship(coachId, clientId)
            if (relationship == null || relationship.status != "active") {
                return Result.failure(Exception("No active relationship with client"))
            }

            // Deactivate any existing active plans for this client
            val existingAssignment = coachDao.getActiveAssignmentForClient(clientId)
            if (existingAssignment != null) {
                coachDao.updateAssignmentStatus(existingAssignment.id, "completed")
            }

            // Generate the training plan
            val generatedPlan = TrainingPlanGenerator.generatePlan(
                userId = clientId,
                goalType = goalType,
                difficulty = difficulty,
                durationWeeks = durationWeeks,
                daysPerWeek = daysPerWeek,
                startDate = System.currentTimeMillis()
            )

            // Save the plan
            trainingPlanDao.insertPlan(generatedPlan.plan)
            generatedPlan.workoutTemplates.forEach { template ->
                workoutTemplateDao.insertTemplate(template)
            }
            scheduledWorkoutDao.insertScheduledWorkouts(generatedPlan.scheduledWorkouts)

            // Create plan progress for client
            val progress = PlanProgress(
                userId = clientId,
                planId = generatedPlan.plan.id,
                currentWeek = 1,
                completionPercentage = 0f,
                startDate = System.currentTimeMillis(),
                totalWorkouts = generatedPlan.totalWorkouts,
                completedWorkouts = 0,
                status = "active"
            )
            trainingPlanDao.insertProgress(progress)

            // Create assignment record
            val assignment = PlanAssignment(
                id = UUID.randomUUID().toString(),
                coachId = coachId,
                clientId = clientId,
                planId = generatedPlan.plan.id,
                startDate = System.currentTimeMillis(),
                notes = notes,
                status = "active"
            )
            coachDao.insertPlanAssignment(assignment)

            Result.success(assignment)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to assign plan", e)
            Result.failure(e)
        }
    }

    /**
     * Get all plan assignments by coach
     */
    fun getPlanAssignments(coachId: String): Flow<List<PlanAssignment>> {
        return coachDao.getAssignmentsByCoach(coachId)
    }

    // ==================== Feedback System ====================

    /**
     * Add feedback to a workout
     */
    suspend fun addWorkoutFeedback(
        coachId: String,
        clientId: String,
        workoutId: String?,
        feedback: String,
        rating: Int? = null
    ): Result<CoachingFeedback> {
        return try {
            val relationship = coachDao.getRelationship(coachId, clientId)
                ?: return Result.failure(Exception("No relationship with client"))

            val coachingFeedback = CoachingFeedback(
                id = UUID.randomUUID().toString(),
                relationshipId = relationship.id,
                workoutId = workoutId,
                feedback = feedback,
                rating = rating,
                isRead = false
            )

            coachDao.insertFeedback(coachingFeedback)

            // Update scheduled workout if workoutId provided
            if (workoutId != null) {
                val workout = scheduledWorkoutDao.getScheduledWorkoutById(workoutId)
                if (workout != null) {
                    scheduledWorkoutDao.updateScheduledWorkout(
                        workout.copy(coachFeedback = feedback)
                    )
                }
            }

            Result.success(coachingFeedback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add feedback", e)
            Result.failure(e)
        }
    }

    /**
     * Get feedback history for a client
     */
    fun getClientFeedback(coachId: String, clientId: String): Flow<List<CoachingFeedback>> {
        return coachDao.getActiveClientRelationships(coachId).map { relationships ->
            val relationship = relationships.find { it.clientId == clientId }
            if (relationship != null) {
                coachDao.getFeedbackForRelationship(relationship.id).first()
            } else {
                emptyList()
            }
        }
    }

    // ==================== Messaging ====================

    /**
     * Send message to client
     */
    suspend fun sendMessage(
        fromId: String,
        toId: String,
        message: String
    ): Result<CoachMessage> {
        return try {
            val coachMessage = CoachMessage(
                id = UUID.randomUUID().toString(),
                fromId = fromId,
                toId = toId,
                message = message,
                messageType = "text",
                isRead = false
            )

            coachDao.insertMessage(coachMessage)
            Result.success(coachMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get messages between coach and client
     */
    fun getMessages(userId1: String, userId2: String): Flow<List<CoachMessage>> {
        return coachDao.getMessagesBetweenUsers(userId1, userId2)
    }

    /**
     * Mark messages as read
     */
    suspend fun markMessagesAsRead(userId: String, fromId: String) {
        coachDao.markAllMessagesAsRead(userId, fromId, System.currentTimeMillis())
    }

    /**
     * Get unread message count
     */
    suspend fun getUnreadMessageCount(userId: String): Int {
        return coachDao.getUnreadMessageCount(userId)
    }

    // ==================== Dashboard Stats ====================

    /**
     * Get coach dashboard summary
     */
    suspend fun getCoachDashboardStats(coachId: String): CoachDashboardStats {
        val activeClientCount = coachDao.getActiveClientCount(coachId)
        val pendingRequests = coachDao.getPendingRequests(coachId).first().size
        val coach = coachDao.getCoachById(coachId)

        return CoachDashboardStats(
            activeClients = activeClientCount,
            pendingRequests = pendingRequests,
            maxClients = coach?.maxClients ?: 20,
            rating = coach?.rating ?: 0f,
            reviewCount = coach?.reviewCount ?: 0
        )
    }

    // ==================== Data Classes ====================

    /**
     * Client activity status
     */
    enum class ClientStatus {
        ACTIVE,     // Activity within last 3 days
        AT_RISK,    // Activity 3-7 days ago
        INACTIVE    // No activity for 7+ days
    }

    /**
     * Detailed client information
     */
    data class ClientDetails(
        val clientId: String,
        val relationshipId: String,
        val startDate: Long,
        val status: ClientStatus,
        val lastActivityDate: Long?,
        val totalDistanceLast30Days: Double,
        val totalWorkoutsLast30Days: Int,
        val avgPace: Float,
        val completedWorkouts: Int,
        val missedWorkouts: Int,
        val upcomingWorkouts: Int,
        val activePlan: TrainingPlan?,
        val recentActivities: List<Track>
    ) {
        fun getStatusColor(): String = when (status) {
            ClientStatus.ACTIVE -> "#4CAF50"    // Green
            ClientStatus.AT_RISK -> "#FF9800"   // Orange
            ClientStatus.INACTIVE -> "#F44336"  // Red
        }

        fun getStatusEmoji(): String = when (status) {
            ClientStatus.ACTIVE -> "🟢"
            ClientStatus.AT_RISK -> "🟡"
            ClientStatus.INACTIVE -> "🔴"
        }

        fun getCompletionRate(): Float {
            val total = completedWorkouts + missedWorkouts
            return if (total > 0) completedWorkouts.toFloat() / total * 100 else 0f
        }
    }

    /**
     * Coach dashboard statistics
     */
    data class CoachDashboardStats(
        val activeClients: Int,
        val pendingRequests: Int,
        val maxClients: Int,
        val rating: Float,
        val reviewCount: Int
    ) {
        fun isAtCapacity(): Boolean = activeClients >= maxClients
        fun getCapacityPercentage(): Float = (activeClients.toFloat() / maxClients) * 100
    }
}
