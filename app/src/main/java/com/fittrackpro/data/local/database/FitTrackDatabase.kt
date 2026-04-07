package com.fittrackpro.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fittrackpro.data.local.database.dao.*
import com.fittrackpro.data.local.database.entity.*

@Database(
    entities = [
        // User entities
        User::class,
        UserSettings::class,
        UserStats::class,

        // Track entities
        Track::class,
        TrackPoint::class,
        TrackStatistics::class,
        PersonalRecord::class,

        // Training entities
        TrainingPlan::class,
        WorkoutTemplate::class,
        UserGoal::class,
        ScheduledWorkout::class,
        PlanProgress::class,

        // Challenge entities
        Challenge::class,
        ChallengeParticipant::class,
        ChallengeLeaderboard::class,
        ChallengeMessage::class,
        TeamChallenge::class,

        // Nutrition entities
        FoodItem::class,
        NutritionLog::class,
        CustomMeal::class,
        HydrationLog::class,
        DailyNutritionSummary::class,

        // Achievement entities
        Achievement::class,
        UserAchievement::class,
        Streak::class,
        XpTransaction::class,

        // Injury entities
        BodyPart::class,
        Injury::class,
        PainTracking::class,
        RecoveryActivity::class,

        // Route entities
        Route::class,
        RouteWaypoint::class,
        RouteRating::class,
        RoutePhoto::class,
        RouteCategory::class,

        // Coach entities
        Coach::class,
        CoachClientRelationship::class,
        CoachingFeedback::class,
        CoachMessage::class,
        PlanAssignment::class,

        // System entities
        SyncQueue::class,
        DeviceRegistry::class,
        SyncLog::class,
        WeatherCache::class,
        Notification::class,
        AnalyticsCache::class,
        PerformanceMetric::class,

        // Step tracking entities
        DailySteps::class
    ],
    version = 1,
    exportSchema = true
)
abstract class FitTrackDatabase : RoomDatabase() {

    // User DAOs
    abstract fun userDao(): UserDao

    // Track DAOs
    abstract fun trackDao(): TrackDao
    abstract fun trackPointDao(): TrackPointDao

    // Training DAOs
    abstract fun trainingPlanDao(): TrainingPlanDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun userGoalDao(): UserGoalDao
    abstract fun scheduledWorkoutDao(): ScheduledWorkoutDao

    // Challenge DAOs
    abstract fun challengeDao(): ChallengeDao

    // Nutrition DAOs
    abstract fun nutritionDao(): NutritionDao

    // Achievement DAOs
    abstract fun achievementDao(): AchievementDao

    // Injury DAOs
    abstract fun injuryDao(): InjuryDao

    // Route DAOs
    abstract fun routeDao(): RouteDao

    // Coach DAOs
    abstract fun coachDao(): CoachDao

    // Step DAOs
    abstract fun stepDao(): StepDao

    companion object {
        const val DATABASE_NAME = "fittrack_database"
    }
}
