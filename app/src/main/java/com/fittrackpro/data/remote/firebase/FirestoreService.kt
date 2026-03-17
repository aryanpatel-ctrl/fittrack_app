package com.fittrackpro.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreService @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()

    suspend fun saveUserProfile(userId: String, data: Map<String, Any>) {
        firestore.collection("users").document(userId).set(data).await()
    }

    suspend fun getUserProfile(userId: String): Map<String, Any>? {
        return firestore.collection("users").document(userId).get().await().data
    }

    suspend fun syncTrack(userId: String, trackId: String, data: Map<String, Any>) {
        firestore.collection("users").document(userId)
            .collection("tracks").document(trackId).set(data).await()
    }

    suspend fun getRemoteTracks(userId: String, sinceTimestamp: Long): List<Map<String, Any>> {
        return firestore.collection("users").document(userId)
            .collection("tracks")
            .whereGreaterThan("updatedAt", sinceTimestamp)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .get().await().documents.mapNotNull { it.data }
    }

    suspend fun syncChallenge(challengeId: String, data: Map<String, Any>) {
        firestore.collection("challenges").document(challengeId).set(data).await()
    }

    suspend fun getPublicChallenges(): List<Map<String, Any>> {
        return firestore.collection("challenges")
            .whereEqualTo("visibility", "public")
            .whereEqualTo("status", "active")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50).get().await().documents.mapNotNull { it.data }
    }

    suspend fun updateChallengeProgress(challengeId: String, userId: String, progress: Double) {
        firestore.collection("challenges").document(challengeId)
            .collection("participants").document(userId)
            .update("progress", progress).await()
    }

    suspend fun getLeaderboard(challengeId: String): List<Map<String, Any>> {
        return firestore.collection("challenges").document(challengeId)
            .collection("participants")
            .orderBy("progress", Query.Direction.DESCENDING)
            .limit(100).get().await().documents.mapNotNull { it.data }
    }

    suspend fun syncAchievement(userId: String, achievementId: String, data: Map<String, Any>) {
        firestore.collection("users").document(userId)
            .collection("achievements").document(achievementId).set(data).await()
    }
}
