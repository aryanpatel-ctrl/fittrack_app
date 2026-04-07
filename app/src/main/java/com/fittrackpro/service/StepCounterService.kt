package com.fittrackpro.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.fittrackpro.R
import com.fittrackpro.data.local.database.dao.StepDao
import com.fittrackpro.data.local.database.entity.DailySteps
import com.fittrackpro.data.local.preferences.UserPreferences
import com.fittrackpro.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class StepCounterService : Service(), SensorEventListener {

    companion object {
        const val NOTIFICATION_ID = 4001
        const val CHANNEL_STEP_COUNTER = "step_counter_channel"
        private const val PREFS_NAME = "step_counter_prefs"
        private const val KEY_INITIAL_STEPS = "initial_steps"
        private const val KEY_TODAY_STEPS = "today_steps"
        private const val KEY_LAST_DATE = "last_date"
        private const val KEY_STEP_GOAL = "step_goal"

        // Average step length in meters (can be customized per user)
        private const val AVERAGE_STEP_LENGTH_METERS = 0.762
        // Approximate calories burned per step
        private const val CALORIES_PER_STEP = 0.04

        var isRunning = false
            private set

        fun getStepsToday(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastDate = prefs.getString(KEY_LAST_DATE, "")
            val today = getTodayDate()

            return if (lastDate == today) {
                prefs.getInt(KEY_TODAY_STEPS, 0)
            } else {
                0
            }
        }

        fun getStepGoal(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(KEY_STEP_GOAL, 10000)
        }

        fun setStepGoal(context: Context, goal: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_STEP_GOAL, goal).apply()
        }

        fun getTodayDate(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }

        fun calculateDistance(steps: Int): Double {
            return steps * AVERAGE_STEP_LENGTH_METERS
        }

        fun calculateCalories(steps: Int): Int {
            return (steps * CALORIES_PER_STEP).toInt()
        }
    }

    @Inject
    lateinit var stepDao: StepDao

    @Inject
    lateinit var userPreferences: UserPreferences

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var initialSteps = -1
    private var todaySteps = 0

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        isRunning = true

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        loadSavedData()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Check permission before starting foreground service on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                // Cannot start foreground service without permission
                isRunning = false
                stopSelf()
                return START_NOT_STICKY
            }
        }

        try {
            startForeground(NOTIFICATION_ID, createNotification())
        } catch (e: SecurityException) {
            // Permission issue - stop service gracefully
            e.printStackTrace()
            isRunning = false
            stopSelf()
            return START_NOT_STICKY
        }

        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSteps = event.values[0].toInt()

            // Check if it's a new day
            val today = getTodayDate()
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastDate = prefs.getString(KEY_LAST_DATE, "")

            if (lastDate != today) {
                // Save previous day's data to database before reset
                if (lastDate?.isNotEmpty() == true) {
                    saveDayToDatabase(lastDate, todaySteps)
                }

                // Reset for new day
                initialSteps = totalSteps
                todaySteps = 0
                prefs.edit()
                    .putInt(KEY_INITIAL_STEPS, initialSteps)
                    .putString(KEY_LAST_DATE, today)
                    .apply()

                // Initialize today's record in database
                initializeTodayInDatabase(today)
            } else if (initialSteps < 0) {
                // First time setup
                initialSteps = prefs.getInt(KEY_INITIAL_STEPS, totalSteps)
                if (initialSteps < 0 || initialSteps > totalSteps) {
                    initialSteps = totalSteps
                    prefs.edit().putInt(KEY_INITIAL_STEPS, initialSteps).apply()
                }
            }

            todaySteps = totalSteps - initialSteps
            if (todaySteps < 0) todaySteps = 0

            // Save current steps to SharedPreferences (for quick access)
            prefs.edit().putInt(KEY_TODAY_STEPS, todaySteps).apply()

            // Save to database periodically (every 100 steps to reduce writes)
            if (todaySteps % 100 == 0 || todaySteps == 1) {
                saveDayToDatabase(today, todaySteps)
            }

            // Update notification
            updateNotification()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }

    private fun loadSavedData() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastDate = prefs.getString(KEY_LAST_DATE, "")
        val today = getTodayDate()

        if (lastDate == today) {
            initialSteps = prefs.getInt(KEY_INITIAL_STEPS, -1)
            todaySteps = prefs.getInt(KEY_TODAY_STEPS, 0)
        } else {
            // Save previous day before resetting
            if (lastDate?.isNotEmpty() == true) {
                val previousSteps = prefs.getInt(KEY_TODAY_STEPS, 0)
                saveDayToDatabase(lastDate, previousSteps)
            }

            initialSteps = -1
            todaySteps = 0
            prefs.edit()
                .putString(KEY_LAST_DATE, today)
                .putInt(KEY_TODAY_STEPS, 0)
                .apply()

            // Initialize today in database
            initializeTodayInDatabase(today)
        }
    }

    private fun initializeTodayInDatabase(date: String) {
        serviceScope.launch {
            try {
                val userId = userPreferences.userId ?: return@launch
                val goal = getStepGoal(this@StepCounterService)

                val existingRecord = stepDao.getStepsForDate(userId, date)
                if (existingRecord == null) {
                    val dailySteps = DailySteps(
                        userId = userId,
                        date = date,
                        stepCount = 0,
                        goal = goal,
                        goalAchieved = false,
                        distanceMeters = 0.0,
                        caloriesBurned = 0
                    )
                    stepDao.insertOrUpdateDailySteps(dailySteps)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveDayToDatabase(date: String, steps: Int) {
        serviceScope.launch {
            try {
                val userId = userPreferences.userId ?: return@launch
                val goal = getStepGoal(this@StepCounterService)
                val distance = calculateDistance(steps)
                val calories = calculateCalories(steps)

                val dailySteps = DailySteps(
                    userId = userId,
                    date = date,
                    stepCount = steps,
                    goal = goal,
                    goalAchieved = steps >= goal,
                    distanceMeters = distance,
                    caloriesBurned = calories,
                    updatedAt = System.currentTimeMillis()
                )
                stepDao.insertOrUpdateDailySteps(dailySteps)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_STEP_COUNTER,
                "Step Counter",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows your daily step count"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val goal = getStepGoal(this)
        val progress = ((todaySteps.toFloat() / goal) * 100).toInt().coerceIn(0, 100)
        val distance = calculateDistance(todaySteps)
        val distanceKm = distance / 1000

        return NotificationCompat.Builder(this, CHANNEL_STEP_COUNTER)
            .setSmallIcon(R.drawable.ic_walking)
            .setContentTitle("Steps: $todaySteps / $goal")
            .setContentText("$progress% of daily goal • ${String.format("%.1f", distanceKm)} km")
            .setProgress(100, progress, false)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        sensorManager.unregisterListener(this)

        // Save final steps for today before service stops
        val today = getTodayDate()
        saveDayToDatabase(today, todaySteps)

        // Cancel coroutine scope
        serviceScope.cancel()
    }
}
