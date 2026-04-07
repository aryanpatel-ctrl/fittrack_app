package com.fittrackpro.ui.social

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.database.dao.ChallengeDao
import com.fittrackpro.data.local.database.entity.Challenge
import com.fittrackpro.data.local.database.entity.ChallengeLeaderboard
import com.fittrackpro.data.local.database.entity.ChallengeParticipant
import com.fittrackpro.data.local.database.entity.TeamChallenge
import com.fittrackpro.data.local.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.UUID
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

    private val _teams = MutableLiveData<List<TeamChallenge>>()
    val teams: LiveData<List<TeamChallenge>> = _teams

    private val _isJoined = MutableLiveData<Boolean>()
    val isJoined: LiveData<Boolean> = _isJoined

    private val _userProgress = MutableLiveData<ChallengeParticipant?>()
    val userProgress: LiveData<ChallengeParticipant?> = _userProgress

    private val _teamCreated = MutableLiveData<Boolean>()
    val teamCreated: LiveData<Boolean> = _teamCreated

    private var currentChallengeId: String? = null
    private var currentParticipant: ChallengeParticipant? = null

    fun loadChallenge(challengeId: String) {
        currentChallengeId = challengeId
        viewModelScope.launch {
            val challenge = challengeDao.getChallengeById(challengeId)
            _challenge.value = challenge

            // Load teams if it's a team challenge
            if (challenge?.isTeamChallenge == true) {
                challengeDao.getTeamsByChallenge(challengeId).collect { teamList ->
                    _teams.value = teamList
                }
            } else {
                challengeDao.getLeaderboard(challengeId, 50).collect { entries ->
                    _leaderboard.value = entries
                }
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

    fun createTeam(teamName: String) {
        viewModelScope.launch {
            val userId = userPreferences.userId ?: return@launch
            val challengeId = currentChallengeId ?: return@launch

            val team = TeamChallenge(
                id = UUID.randomUUID().toString(),
                challengeId = challengeId,
                teamName = teamName,
                captainId = userId,
                memberCount = 1
            )
            challengeDao.insertTeam(team)

            // Join challenge with team
            val participant = ChallengeParticipant(
                challengeId = challengeId,
                userId = userId,
                teamId = team.id
            )
            challengeDao.insertParticipant(participant)
            currentParticipant = participant
            _isJoined.value = true
            _userProgress.value = participant
            _teamCreated.value = true
        }
    }

    fun joinTeam(teamId: String) {
        viewModelScope.launch {
            val userId = userPreferences.userId ?: return@launch
            val challengeId = currentChallengeId ?: return@launch

            val participant = ChallengeParticipant(
                challengeId = challengeId,
                userId = userId,
                teamId = teamId
            )
            challengeDao.insertParticipant(participant)
            currentParticipant = participant
            _isJoined.value = true
            _userProgress.value = participant
        }
    }
}
