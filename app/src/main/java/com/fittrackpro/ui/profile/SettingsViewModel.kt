package com.fittrackpro.ui.profile

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fittrackpro.data.local.database.dao.UserDao
import com.fittrackpro.data.local.database.entity.UserSettings
import com.fittrackpro.data.local.preferences.UserPreferences
import com.fittrackpro.data.remote.firebase.FirebaseAuthService
import com.fittrackpro.service.WaterReminderWorker
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userDao: UserDao,
    private val userPreferences: UserPreferences,
    private val firebaseAuthService: FirebaseAuthService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _settings = MutableLiveData<UserSettings?>()
    val settings: LiveData<UserSettings?> = _settings

    private val _logoutComplete = MutableLiveData<Boolean>()
    val logoutComplete: LiveData<Boolean> = _logoutComplete

    private val _waterRemindersEnabled = MutableLiveData<Boolean>()
    val waterRemindersEnabled: LiveData<Boolean> = _waterRemindersEnabled

    private val _passwordChangeResult = MutableLiveData<PasswordChangeResult?>()
    val passwordChangeResult: LiveData<PasswordChangeResult?> = _passwordChangeResult

    sealed class PasswordChangeResult {
        data object Success : PasswordChangeResult()
        data class Error(val message: String) : PasswordChangeResult()
    }

    init {
        loadSettings()
        loadWaterRemindersState()
    }

    private fun loadWaterRemindersState() {
        _waterRemindersEnabled.value = userPreferences.waterRemindersEnabled
    }

    private fun loadSettings() {
        viewModelScope.launch {
            userPreferences.userId?.let { userId ->
                _settings.value = userDao.getSettingsByUserId(userId)
            }
        }
    }

    fun setUseMetric(useMetric: Boolean) {
        updateSettings { it.copy(useMetricUnits = useMetric) }
    }

    fun setWaterRemindersEnabled(enabled: Boolean) {
        userPreferences.waterRemindersEnabled = enabled
        _waterRemindersEnabled.value = enabled

        if (enabled) {
            scheduleWaterReminders()
        } else {
            cancelWaterReminders()
        }
    }

    private fun scheduleWaterReminders() {
        val workRequest = PeriodicWorkRequestBuilder<WaterReminderWorker>(
            2, TimeUnit.HOURS,
            15, TimeUnit.MINUTES // Flex interval
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WaterReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun cancelWaterReminders() {
        WorkManager.getInstance(context).cancelUniqueWork(WaterReminderWorker.WORK_NAME)
    }

    private fun updateSettings(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            _settings.value?.let { currentSettings ->
                val updatedSettings = transform(currentSettings)
                userDao.updateSettings(updatedSettings)
                _settings.value = updatedSettings
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            // Validate inputs
            if (currentPassword.isEmpty()) {
                _passwordChangeResult.value = PasswordChangeResult.Error("Current password is required")
                return@launch
            }

            if (newPassword.isEmpty()) {
                _passwordChangeResult.value = PasswordChangeResult.Error("New password is required")
                return@launch
            }

            if (newPassword.length < 8) {
                _passwordChangeResult.value = PasswordChangeResult.Error("Password must be at least 8 characters")
                return@launch
            }

            if (newPassword != confirmPassword) {
                _passwordChangeResult.value = PasswordChangeResult.Error("Passwords do not match")
                return@launch
            }

            if (currentPassword == newPassword) {
                _passwordChangeResult.value = PasswordChangeResult.Error("New password must be different from current password")
                return@launch
            }

            // Change password via Firebase Auth
            try {
                firebaseAuthService.changePassword(currentPassword, newPassword)
                _passwordChangeResult.value = PasswordChangeResult.Success
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _passwordChangeResult.value = PasswordChangeResult.Error("Current password is incorrect")
            } catch (e: Exception) {
                _passwordChangeResult.value = PasswordChangeResult.Error("Failed to change password: ${e.message}")
            }
        }
    }

    fun clearPasswordChangeResult() {
        _passwordChangeResult.value = null
    }

    fun deleteAccount() {
        viewModelScope.launch {
            try {
                // Delete from Firebase Auth
                firebaseAuthService.deleteAccount()

                // Delete local user data
                userPreferences.userId?.let { userId ->
                    val user = userDao.getUserById(userId)
                    user?.let { userDao.deleteUser(it) }
                }
                userPreferences.clearAll()
                _logoutComplete.value = true
            } catch (e: Exception) {
                // If Firebase deletion fails, still clear local data
                userPreferences.clearAll()
                _logoutComplete.value = true
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            firebaseAuthService.signOut()
            userPreferences.clearAll()
            _logoutComplete.value = true
        }
    }
}
