package com.fittrackpro.util

object Constants {
    // API Base URLs
    const val USDA_BASE_URL = "https://api.nal.usda.gov/fdc/v1/"
    const val WEATHER_BASE_URL = "https://api.openweathermap.org/data/2.5/"

    // API Keys
    const val USDA_API_KEY = "TTaM5vdJVqhEFuNwNb1yREp56KKlH9XnKudNhuW7"
    const val WEATHER_API_KEY = "e3c5b73a45c491720ad3c559ae49cee2"

    // Location Settings
    const val LOCATION_UPDATE_INTERVAL = 5000L // 5 seconds
    const val LOCATION_FASTEST_INTERVAL = 2000L // 2 seconds
    const val LOCATION_MIN_DISTANCE = 10f // 10 meters

    // Tracking Service
    const val TRACKING_NOTIFICATION_ID = 1
    const val TRACKING_SERVICE_ACTION_START = "ACTION_START_TRACKING"
    const val TRACKING_SERVICE_ACTION_PAUSE = "ACTION_PAUSE_TRACKING"
    const val TRACKING_SERVICE_ACTION_RESUME = "ACTION_RESUME_TRACKING"
    const val TRACKING_SERVICE_ACTION_STOP = "ACTION_STOP_TRACKING"

    // Activity Types
    object ActivityType {
        const val RUNNING = "running"
        const val CYCLING = "cycling"
        const val WALKING = "walking"
        const val HIKING = "hiking"
        const val SWIMMING = "swimming"
    }

    // Workout Status
    object WorkoutStatus {
        const val PENDING = "pending"
        const val IN_PROGRESS = "in_progress"
        const val COMPLETED = "completed"
        const val SKIPPED = "skipped"
    }

    // Challenge Types
    object ChallengeType {
        const val DISTANCE = "distance"
        const val DURATION = "duration"
        const val CALORIES = "calories"
        const val ACTIVITIES = "activities"
    }

    // Goal Types
    object GoalType {
        const val RACE_5K = "5k"
        const val RACE_10K = "10k"
        const val HALF_MARATHON = "half_marathon"
        const val MARATHON = "marathon"
        const val WEIGHT_LOSS = "weight_loss"
        const val ENDURANCE = "endurance"
    }

    // Meal Types
    object MealType {
        const val BREAKFAST = "breakfast"
        const val LUNCH = "lunch"
        const val DINNER = "dinner"
        const val SNACK = "snack"
    }

    // User Roles
    object UserRole {
        const val ATHLETE = "athlete"
        const val COACH = "coach"
        const val ADMIN = "admin"
    }

    // Units
    object Units {
        const val METRIC = "metric"
        const val IMPERIAL = "imperial"
    }

    // Achievement Criteria Types
    object AchievementCriteria {
        const val TOTAL_DISTANCE = "total_distance"
        const val TOTAL_ACTIVITIES = "total_activities"
        const val STREAK_DAYS = "streak_days"
        const val CHALLENGE_WINS = "challenge_wins"
        const val FASTEST_5K = "fastest_5k"
        const val LONGEST_RUN = "longest_run"
    }

    // Personal Record Types
    object RecordType {
        const val FASTEST_1K = "fastest_1k"
        const val FASTEST_5K = "fastest_5k"
        const val FASTEST_10K = "fastest_10k"
        const val LONGEST_DISTANCE = "longest_distance"
        const val HIGHEST_ELEVATION = "highest_elevation"
        const val MOST_CALORIES = "most_calories"
    }

    // Calorie calculation constants
    const val CALORIES_PER_KM_RUNNING = 60.0
    const val CALORIES_PER_KM_CYCLING = 30.0
    const val CALORIES_PER_KM_WALKING = 40.0

    // XP Constants
    const val XP_PER_KM = 10
    const val XP_PER_WORKOUT = 50
    const val XP_CHALLENGE_COMPLETION = 100
    const val XP_ACHIEVEMENT_UNLOCK = 200

    // Level thresholds
    val LEVEL_THRESHOLDS = listOf(
        0, 100, 250, 500, 800, 1200, 1700, 2300, 3000, 3800,
        4700, 5700, 6800, 8000, 9300, 10700, 12200, 13800, 15500, 17300,
        19200, 21200, 23300, 25500, 27800, 30200, 32700, 35300, 38000, 40800,
        43700, 46700, 49800, 53000, 56300, 59700, 63200, 66800, 70500, 74300,
        78200, 82200, 86300, 90500, 94800, 99200, 103700, 108300, 113000, 118000
    )
}
