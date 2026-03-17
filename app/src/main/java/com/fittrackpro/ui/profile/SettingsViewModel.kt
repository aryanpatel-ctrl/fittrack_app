package com.fittrackpro.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.database.dao.UserDao
import com.fittrackpro.data.local.database.entity.UserSettings
import com.fittrackpro.data.local.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userDao: UserDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _settings = MutableLiveData<UserSettings?>()
    val settings: LiveData<UserSettings?> = _settings

    private val _logoutComplete = MutableLiveData<Boolean>()
    val logoutComplete: LiveData<Boolean> = _logoutComplete

    private val _exportProgress = MutableLiveData<Boolean>()
    val exportProgress: LiveData<Boolean> = _exportProgress

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            userPreferences.userId?.let { userId ->
                _settings.value = userDao.getSettingsByUserId(userId)
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        updateSettings { it.copy(enableNotifications = enabled) }
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        updateSettings { it.copy(enableDarkMode = enabled) }
    }

    fun setUseMetric(useMetric: Boolean) {
        updateSettings { it.copy(useMetricUnits = useMetric) }
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

    fun exportData() {
        viewModelScope.launch {
            _exportProgress.value = true
            _exportProgress.value = false
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            userPreferences.userId?.let { userId ->
                val user = userDao.getUserById(userId)
                user?.let { userDao.deleteUser(it) }
                userPreferences.clearAll()
                _logoutComplete.value = true
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userPreferences.clearAll()
            _logoutComplete.value = true
        }
    }
}
