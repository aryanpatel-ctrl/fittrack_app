package com.fittrackpro.data.local.database.dao

import androidx.room.*
import com.fittrackpro.data.local.database.entity.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Coach-related operations
 *
 * Handles:
 * - Coach profile management
 * - Coach-client relationships
 * - Plan assignments
 * - Coaching feedback
 * - Coach-client messaging
 */
@Dao
interface CoachDao {

    // ==================== Coach Profile Operations ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoach(coach: Coach)

    @Update
    suspend fun updateCoach(coach: Coach)

    @Delete
    suspend fun deleteCoach(coach: Coach)

    @Query("SELECT * FROM coaches WHERE id = :coachId")
    suspend fun getCoachById(coachId: String): Coach?

    @Query("SELECT * FROM coaches WHERE userId = :userId")
    suspend fun getCoachByUserId(userId: String): Coach?

    @Query("SELECT * FROM coaches WHERE userId = :userId")
    fun getCoachByUserIdFlow(userId: String): Flow<Coach?>

    @Query("SELECT * FROM coaches WHERE isVerified = 1 AND acceptingClients = 1 ORDER BY rating DESC")
    fun getAvailableCoaches(): Flow<List<Coach>>

    @Query("SELECT * FROM coaches WHERE specialty = :specialty AND isVerified = 1")
    fun getCoachesBySpecialty(specialty: String): Flow<List<Coach>>

    @Query("UPDATE coaches SET currentClients = currentClients + 1 WHERE id = :coachId")
    suspend fun incrementClientCount(coachId: String)

    @Query("UPDATE coaches SET currentClients = currentClients - 1 WHERE id = :coachId AND currentClients > 0")
    suspend fun decrementClientCount(coachId: String)

    @Query("UPDATE coaches SET rating = :rating, reviewCount = :reviewCount WHERE id = :coachId")
    suspend fun updateCoachRating(coachId: String, rating: Float, reviewCount: Int)

    // ==================== Coach-Client Relationship Operations ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationship(relationship: CoachClientRelationship)

    @Update
    suspend fun updateRelationship(relationship: CoachClientRelationship)

    @Delete
    suspend fun deleteRelationship(relationship: CoachClientRelationship)

    @Query("SELECT * FROM coach_client_relationships WHERE id = :relationshipId")
    suspend fun getRelationshipById(relationshipId: String): CoachClientRelationship?

    @Query("SELECT * FROM coach_client_relationships WHERE coachId = :coachId AND status = 'active'")
    fun getActiveClientRelationships(coachId: String): Flow<List<CoachClientRelationship>>

    @Query("SELECT * FROM coach_client_relationships WHERE clientId = :clientId AND status = 'active'")
    suspend fun getClientActiveCoach(clientId: String): CoachClientRelationship?

    @Query("SELECT * FROM coach_client_relationships WHERE coachId = :coachId AND clientId = :clientId")
    suspend fun getRelationship(coachId: String, clientId: String): CoachClientRelationship?

    @Query("SELECT * FROM coach_client_relationships WHERE coachId = :coachId AND status = 'pending'")
    fun getPendingRequests(coachId: String): Flow<List<CoachClientRelationship>>

    @Query("UPDATE coach_client_relationships SET status = :status, startDate = :startDate WHERE id = :relationshipId")
    suspend fun updateRelationshipStatus(relationshipId: String, status: String, startDate: Long?)

    @Query("SELECT COUNT(*) FROM coach_client_relationships WHERE coachId = :coachId AND status = 'active'")
    suspend fun getActiveClientCount(coachId: String): Int

    // ==================== Plan Assignment Operations ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanAssignment(assignment: PlanAssignment)

    @Update
    suspend fun updatePlanAssignment(assignment: PlanAssignment)

    @Delete
    suspend fun deletePlanAssignment(assignment: PlanAssignment)

    @Query("SELECT * FROM plan_assignments WHERE id = :assignmentId")
    suspend fun getPlanAssignmentById(assignmentId: String): PlanAssignment?

    @Query("SELECT * FROM plan_assignments WHERE clientId = :clientId AND status = 'active'")
    suspend fun getActiveAssignmentForClient(clientId: String): PlanAssignment?

    @Query("SELECT * FROM plan_assignments WHERE coachId = :coachId ORDER BY assignedAt DESC")
    fun getAssignmentsByCoach(coachId: String): Flow<List<PlanAssignment>>

    @Query("SELECT * FROM plan_assignments WHERE clientId = :clientId ORDER BY assignedAt DESC")
    fun getAssignmentsForClient(clientId: String): Flow<List<PlanAssignment>>

    @Query("UPDATE plan_assignments SET status = :status WHERE id = :assignmentId")
    suspend fun updateAssignmentStatus(assignmentId: String, status: String)

    // ==================== Coaching Feedback Operations ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: CoachingFeedback)

    @Update
    suspend fun updateFeedback(feedback: CoachingFeedback)

    @Delete
    suspend fun deleteFeedback(feedback: CoachingFeedback)

    @Query("SELECT * FROM coaching_feedback WHERE relationshipId = :relationshipId ORDER BY createdAt DESC")
    fun getFeedbackForRelationship(relationshipId: String): Flow<List<CoachingFeedback>>

    @Query("SELECT * FROM coaching_feedback WHERE workoutId = :workoutId")
    suspend fun getFeedbackForWorkout(workoutId: String): CoachingFeedback?

    @Query("SELECT * FROM coaching_feedback WHERE relationshipId = :relationshipId AND isRead = 0")
    suspend fun getUnreadFeedback(relationshipId: String): List<CoachingFeedback>

    @Query("UPDATE coaching_feedback SET isRead = 1 WHERE id = :feedbackId")
    suspend fun markFeedbackAsRead(feedbackId: String)

    @Query("SELECT COUNT(*) FROM coaching_feedback WHERE relationshipId = :relationshipId AND isRead = 0")
    suspend fun getUnreadFeedbackCount(relationshipId: String): Int

    // ==================== Coach Messaging Operations ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: CoachMessage)

    @Update
    suspend fun updateMessage(message: CoachMessage)

    @Delete
    suspend fun deleteMessage(message: CoachMessage)

    @Query("""
        SELECT * FROM coach_messages
        WHERE (fromId = :userId1 AND toId = :userId2)
           OR (fromId = :userId2 AND toId = :userId1)
        ORDER BY createdAt ASC
    """)
    fun getMessagesBetweenUsers(userId1: String, userId2: String): Flow<List<CoachMessage>>

    @Query("""
        SELECT * FROM coach_messages
        WHERE (fromId = :userId1 AND toId = :userId2)
           OR (fromId = :userId2 AND toId = :userId1)
        ORDER BY createdAt DESC
        LIMIT 1
    """)
    suspend fun getLastMessage(userId1: String, userId2: String): CoachMessage?

    @Query("SELECT * FROM coach_messages WHERE toId = :userId AND isRead = 0")
    suspend fun getUnreadMessages(userId: String): List<CoachMessage>

    @Query("UPDATE coach_messages SET isRead = 1, readAt = :readAt WHERE id = :messageId")
    suspend fun markMessageAsRead(messageId: String, readAt: Long)

    @Query("UPDATE coach_messages SET isRead = 1, readAt = :readAt WHERE toId = :userId AND fromId = :fromId AND isRead = 0")
    suspend fun markAllMessagesAsRead(userId: String, fromId: String, readAt: Long)

    @Query("SELECT COUNT(*) FROM coach_messages WHERE toId = :userId AND isRead = 0")
    suspend fun getUnreadMessageCount(userId: String): Int

    // ==================== Combined Queries for Dashboard ====================

    /**
     * Get client details with their workout stats
     * Returns User entities for all active clients of a coach
     */
    @Query("""
        SELECT u.* FROM users u
        INNER JOIN coach_client_relationships ccr ON u.id = ccr.clientId
        WHERE ccr.coachId = :coachId AND ccr.status = 'active'
        ORDER BY u.name ASC
    """)
    fun getClientsForCoach(coachId: String): Flow<List<User>>

    /**
     * Get recent activities for a specific client
     */
    @Query("""
        SELECT * FROM tracks
        WHERE userId = :clientId
        ORDER BY startTime DESC
        LIMIT :limit
    """)
    suspend fun getClientRecentActivities(clientId: String, limit: Int): List<Track>

    /**
     * Get scheduled workouts for a client
     */
    @Query("""
        SELECT sw.* FROM scheduled_workouts sw
        WHERE sw.userId = :clientId AND sw.scheduledDate >= :fromDate
        ORDER BY sw.scheduledDate ASC
        LIMIT :limit
    """)
    suspend fun getClientUpcomingWorkouts(clientId: String, fromDate: Long, limit: Int): List<ScheduledWorkout>
}
