package com.fittrackpro.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _isUserLoggedIn = MutableLiveData<Boolean>()
    val isUserLoggedIn: LiveData<Boolean> = _isUserLoggedIn

    private val _userName = MutableLiveData<String?>()
    val userName: LiveData<String?> = _userName

    init {
        checkUserStatus()
    }

    private fun checkUserStatus() {
        viewModelScope.launch {
            _isUserLoggedIn.value = userPreferences.isLoggedIn
            _userName.value = userPreferences.userName
        }
    }

    fun logout() {
        viewModelScope.launch {
            userPreferences.clearUserData()
            _isUserLoggedIn.value = false
        }
    }
}
