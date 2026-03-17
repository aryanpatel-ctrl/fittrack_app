package com.fittrackpro.data.repository

import com.fittrackpro.data.local.database.dao.UserDao
import com.fittrackpro.data.local.database.entity.User
import com.fittrackpro.data.local.database.entity.UserSettings
import com.fittrackpro.data.local.database.entity.UserStats
import com.fittrackpro.data.local.preferences.UserPreferences
import com.fittrackpro.data.remote.firebase.FirebaseAuthService
import com.fittrackpro.data.remote.firebase.FirestoreService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val userPreferences: UserPreferences,
    private val firebaseAuthService: FirebaseAuthService,
    private val firestoreService: FirestoreService
) {
    suspend fun getUser(userId: String): User? = userDao.getUserById(userId)
    suspend fun getUserStats(userId: String): UserStats? = userDao.getStatsByUserId(userId)
    suspend fun getUserSettings(userId: String): UserSettings? = userDao.getSettingsByUserId(userId)
    suspend fun saveUser(user: User) = userDao.insertUser(user)
    suspend fun updateUserStats(stats: UserStats) = userDao.insertStats(stats)
    suspend fun updateUserSettings(settings: UserSettings) = userDao.insertSettings(settings)
    suspend fun syncUserProfile(userId: String) {
        userDao.getUserById(userId)?.let {
            firestoreService.saveUserProfile(userId, mapOf(
                "id" to it.id, "email" to it.email, "name" to it.name,
                "role" to it.role, "updatedAt" to System.currentTimeMillis()
            ))
        }
    }
    fun isLoggedIn(): Boolean = userPreferences.isLoggedIn
    fun getCurrentUserId(): String? = userPreferences.userId
}
