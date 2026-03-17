package com.fittrackpro.di

import android.content.Context
import androidx.room.Room
import com.fittrackpro.data.local.database.FitTrackDatabase
import com.fittrackpro.data.local.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): FitTrackDatabase {
        return Room.databaseBuilder(
            context,
            FitTrackDatabase::class.java,
            "fittrack_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    // User DAOs
    @Provides
    fun provideUserDao(database: FitTrackDatabase): UserDao = database.userDao()

    // Track DAOs
    @Provides
    fun provideTrackDao(database: FitTrackDatabase): TrackDao = database.trackDao()

    @Provides
    fun provideTrackPointDao(database: FitTrackDatabase): TrackPointDao = database.trackPointDao()

    // Training DAOs
    @Provides
    fun provideTrainingPlanDao(database: FitTrackDatabase): TrainingPlanDao = database.trainingPlanDao()

    @Provides
    fun provideWorkoutTemplateDao(database: FitTrackDatabase): WorkoutTemplateDao = database.workoutTemplateDao()

    @Provides
    fun provideUserGoalDao(database: FitTrackDatabase): UserGoalDao = database.userGoalDao()

    @Provides
    fun provideScheduledWorkoutDao(database: FitTrackDatabase): ScheduledWorkoutDao = database.scheduledWorkoutDao()

    // Challenge DAOs
    @Provides
    fun provideChallengeDao(database: FitTrackDatabase): ChallengeDao = database.challengeDao()

    // Nutrition DAOs
    @Provides
    fun provideNutritionDao(database: FitTrackDatabase): NutritionDao = database.nutritionDao()

    // Achievement DAOs
    @Provides
    fun provideAchievementDao(database: FitTrackDatabase): AchievementDao = database.achievementDao()

    // Injury DAOs
    @Provides
    fun provideInjuryDao(database: FitTrackDatabase): InjuryDao = database.injuryDao()

    // Route DAOs
    @Provides
    fun provideRouteDao(database: FitTrackDatabase): RouteDao = database.routeDao()

    // Coach DAOs
    @Provides
    fun provideCoachDao(database: FitTrackDatabase): CoachDao = database.coachDao()
}
