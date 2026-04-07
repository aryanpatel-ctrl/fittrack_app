package com.fittrackpro.ui.social

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.database.dao.ChallengeDao
import com.fittrackpro.data.local.database.entity.Challenge
import com.fittrackpro.data.local.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SocialViewModel @Inject constructor(
    private val challengeDao: ChallengeDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _challenges = MutableLiveData<List<Challenge>>()
    val challenges: LiveData<List<Challenge>> = _challenges

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadMyChallenges()
    }

    fun loadMyChallenges() {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = userPreferences.userId
            if (userId != null) {
                try {
                    challengeDao.getChallengesForUser(userId).collect { challenges ->
                        _challenges.value = challenges
                        _isLoading.value = false
                    }
                } catch (e: Exception) {
                    _challenges.value = emptyList()
                    _isLoading.value = false
                }
            } else {
                _challenges.value = emptyList()
                _isLoading.value = false
            }
        }
    }

    fun sendInvite(email: String) {
        viewModelScope.launch {
            // In a real app, this would send an email invitation via backend API
            // For now, just log the invite
            // You could integrate with email intent or backend service here
        }
    }
}
