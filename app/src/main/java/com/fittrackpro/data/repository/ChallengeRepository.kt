package com.fittrackpro.data.repository

import com.fittrackpro.data.local.database.dao.ChallengeDao
import com.fittrackpro.data.local.database.entity.Challenge
import com.fittrackpro.data.local.database.entity.ChallengeLeaderboard
import com.fittrackpro.data.local.database.entity.ChallengeParticipant
import com.fittrackpro.data.remote.firebase.FirestoreService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChallengeRepository @Inject constructor(
    private val challengeDao: ChallengeDao,
    private val firestoreService: FirestoreService
) {
    fun getChallengesForUser(userId: String): Flow<List<Challenge>> = challengeDao.getChallengesForUser(userId)
    fun getPublicChallenges(): Flow<List<Challenge>> = challengeDao.getPublicActiveChallenges()
    suspend fun getChallengeById(challengeId: String) = challengeDao.getChallengeById(challengeId)
    suspend fun insertChallenge(challenge: Challenge) = challengeDao.insertChallenge(challenge)
    suspend fun joinChallenge(participant: ChallengeParticipant) = challengeDao.insertParticipant(participant)
    suspend fun leaveChallenge(participant: ChallengeParticipant) = challengeDao.deleteParticipant(participant)
    fun getLeaderboard(challengeId: String, limit: Int = 50): Flow<List<ChallengeLeaderboard>> = challengeDao.getLeaderboard(challengeId, limit)
    suspend fun updateProgress(challengeId: String, userId: String, progress: Double) {
        challengeDao.updateParticipantProgress(challengeId, userId, progress, System.currentTimeMillis())
        try { firestoreService.updateChallengeProgress(challengeId, userId, progress) } catch (_: Exception) {}
    }
}
