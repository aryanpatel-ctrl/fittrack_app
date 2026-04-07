package com.fittrackpro.data.local.database.dao

import androidx.room.*
import com.fittrackpro.data.local.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeDao {
    // Challenge operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: Challenge)

    @Update
    suspend fun updateChallenge(challenge: Challenge)

    @Delete
    suspend fun deleteChallenge(challenge: Challenge)

    @Query("SELECT * FROM challenges WHERE id = :challengeId")
    suspend fun getChallengeById(challengeId: String): Challenge?

    @Query("SELECT * FROM challenges WHERE id = :challengeId")
    fun getChallengeByIdFlow(challengeId: String): Flow<Challenge?>

    @Query("SELECT * FROM challenges WHERE visibility = 'public' AND status = 'active' ORDER BY startDate DESC")
    fun getPublicActiveChallenges(): Flow<List<Challenge>>

    @Query("SELECT * FROM challenges WHERE creatorId = :userId ORDER BY createdAt DESC")
    fun getChallengesByCreator(userId: String): Flow<List<Challenge>>

    @Query("SELECT * FROM challenges WHERE status = :status ORDER BY startDate DESC")
    fun getChallengesByStatus(status: String): Flow<List<Challenge>>

    @Query("""
        SELECT c.* FROM challenges c
        INNER JOIN challenge_participants cp ON c.id = cp.challengeId
        WHERE cp.userId = :userId
        ORDER BY c.startDate DESC
    """)
    fun getChallengesForUser(userId: String): Flow<List<Challenge>>

    // Challenge Participant operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: ChallengeParticipant)

    @Update
    suspend fun updateParticipant(participant: ChallengeParticipant)

    @Delete
    suspend fun deleteParticipant(participant: ChallengeParticipant)

    @Query("SELECT * FROM challenge_participants WHERE challengeId = :challengeId ORDER BY progress DESC")
    fun getParticipantsByChallenge(challengeId: String): Flow<List<ChallengeParticipant>>

    @Query("SELECT * FROM challenge_participants WHERE challengeId = :challengeId AND userId = :userId")
    suspend fun getParticipant(challengeId: String, userId: String): ChallengeParticipant?

    @Query("SELECT COUNT(*) FROM challenge_participants WHERE challengeId = :challengeId")
    suspend fun getParticipantCount(challengeId: String): Int

    @Query("UPDATE challenge_participants SET progress = :progress, lastUpdated = :timestamp WHERE challengeId = :challengeId AND userId = :userId")
    suspend fun updateParticipantProgress(challengeId: String, userId: String, progress: Double, timestamp: Long)

    // Leaderboard operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaderboardEntries(entries: List<ChallengeLeaderboard>)

    @Query("SELECT * FROM challenge_leaderboard WHERE challengeId = :challengeId ORDER BY rank ASC LIMIT :limit")
    fun getLeaderboard(challengeId: String, limit: Int): Flow<List<ChallengeLeaderboard>>

    @Query("DELETE FROM challenge_leaderboard WHERE challengeId = :challengeId")
    suspend fun clearLeaderboard(challengeId: String)

    // Challenge Messages
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChallengeMessage)

    @Query("SELECT * FROM challenge_messages WHERE challengeId = :challengeId ORDER BY timestamp DESC LIMIT :limit")
    fun getMessages(challengeId: String, limit: Int): Flow<List<ChallengeMessage>>

    // Team operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: TeamChallenge)

    @Query("SELECT * FROM team_challenges WHERE challengeId = :challengeId ORDER BY totalProgress DESC")
    fun getTeamsByChallenge(challengeId: String): Flow<List<TeamChallenge>>

    // Achievement tracking - count completed challenges for user
    @Query("""
        SELECT COUNT(*) FROM challenges c
        INNER JOIN challenge_participants cp ON c.id = cp.challengeId
        WHERE cp.userId = :userId AND c.status = 'completed'
    """)
    suspend fun getCompletedChallengesCount(userId: String): Int
}
