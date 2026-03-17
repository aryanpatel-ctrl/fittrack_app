package com.fittrackpro.ui.social

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.database.dao.ChallengeDao
import com.fittrackpro.data.local.database.entity.Challenge
import com.fittrackpro.data.local.database.entity.ChallengeLeaderboard
import com.fittrackpro.data.local.database.entity.ChallengeParticipant
import com.fittrackpro.data.local.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChallengeDetailViewModel @Inject constructor(
    private val challengeDao: ChallengeDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _challenge = MutableLiveData<Challenge?>()
    val challenge: LiveData<Challenge?> = _challenge

    private val _leaderboard = MutableLiveData<List<ChallengeLeaderboard>>()
    val leaderboard: LiveData<List<ChallengeLeaderboard>> = _leaderboard

    private val _isJoined = MutableLiveData<Boolean>()
    val isJoined: LiveData<Boolean> = _isJoined

    private val _userProgress = MutableLiveData<ChallengeParticipant?>()
    val userProgress: LiveData<ChallengeParticipant?> = _userProgress

    private var currentChallengeId: String? = null
    private var currentParticipant: ChallengeParticipant? = null

    fun loadChallenge(challengeId: String) {
        currentChallengeId = challengeId
        viewModelScope.launch {
            _challenge.value = challengeDao.getChallengeById(challengeId)
            challengeDao.getLeaderboard(challengeId, 50).collect { entries ->
                _leaderboard.value = entries
            }
        }
        viewModelScope.launch {
            val userId = userPreferences.userId
            if (userId != null) {
                val participant = challengeDao.getParticipant(challengeId, userId)
                currentParticipant = participant
                _isJoined.value = participant != null
                _userProgress.value = participant
            }
        }
    }

    fun joinChallenge() {
        viewModelScope.launch {
            val userId = userPreferences.userId ?: return@launch
            val challengeId = currentChallengeId ?: return@launch
            val participant = ChallengeParticipant(challengeId = challengeId, userId = userId)
            challengeDao.insertParticipant(participant)
            currentParticipant = participant
            _isJoined.value = true
            _userProgress.value = participant
        }
    }

    fun leaveChallenge() {
        viewModelScope.launch {
            val participant = currentParticipant ?: return@launch
            challengeDao.deleteParticipant(participant)
            currentParticipant = null
            _isJoined.value = false
            _userProgress.value = null
        }
    }
}
