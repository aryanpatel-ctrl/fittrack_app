package com.fittrackpro.ui.social

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.database.dao.ChallengeDao
import com.fittrackpro.data.local.database.entity.Challenge
import com.fittrackpro.data.local.database.entity.ChallengeParticipant
import com.fittrackpro.data.local.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreateChallengeViewModel @Inject constructor(
    private val challengeDao: ChallengeDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _challengeCreated = MutableLiveData<Boolean>()
    val challengeCreated: LiveData<Boolean> = _challengeCreated

    fun createChallenge(
        name: String, description: String, type: String,
        goalValue: Double, goalUnit: String,
        startDate: Long, endDate: Long, visibility: String
    ) {
        viewModelScope.launch {
            val userId = userPreferences.userId ?: return@launch
            val challengeId = UUID.randomUUID().toString()

            val challenge = Challenge(
                id = challengeId, creatorId = userId, name = name,
                description = description, type = type, goalValue = goalValue,
                goalUnit = goalUnit, startDate = startDate, endDate = endDate,
                visibility = visibility,
                status = if (startDate <= System.currentTimeMillis()) "active" else "upcoming"
            )
            challengeDao.insertChallenge(challenge)

            val participant = ChallengeParticipant(challengeId = challengeId, userId = userId)
            challengeDao.insertParticipant(participant)

            _challengeCreated.value = true
        }
    }
}
