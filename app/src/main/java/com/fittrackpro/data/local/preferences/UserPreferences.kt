package com.fittrackpro.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import com.fittrackpro.util.Constants
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userRole: String
        get() = prefs.getString(KEY_USER_ROLE, Constants.UserRole.ATHLETE) ?: Constants.UserRole.ATHLETE
        set(value) = prefs.edit().putString(KEY_USER_ROLE, value).apply()

    var useMetricUnits: Boolean
        get() = prefs.getBoolean(KEY_USE_METRIC, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_METRIC, value).apply()

    var enableNotifications: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    var enableDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var dailyGoalDistance: Float
        get() = prefs.getFloat(KEY_DAILY_GOAL_DISTANCE, 5000f) // 5km default
        set(value) = prefs.edit().putFloat(KEY_DAILY_GOAL_DISTANCE, value).apply()

    var dailyGoalCalories: Int
        get() = prefs.getInt(KEY_DAILY_GOAL_CALORIES, 500)
        set(value) = prefs.edit().putInt(KEY_DAILY_GOAL_CALORIES, value).apply()

    var lastSyncTime: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC, value).apply()

    var waterRemindersEnabled: Boolean
        get() = prefs.getBoolean(KEY_WATER_REMINDERS, false)
        set(value) = prefs.edit().putBoolean(KEY_WATER_REMINDERS, value).apply()

    fun clearUserData() {
        prefs.edit().apply {
            remove(KEY_IS_LOGGED_IN)
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_NAME)
            remove(KEY_USER_ROLE)
            apply()
        }
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "fittrack_prefs"

        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USE_METRIC = "use_metric"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_DAILY_GOAL_DISTANCE = "daily_goal_distance"
        private const val KEY_DAILY_GOAL_CALORIES = "daily_goal_calories"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_WATER_REMINDERS = "water_reminders_enabled"
    }
}
