package com.fittrackpro.util

import com.fittrackpro.data.local.database.entity.ScheduledWorkout
import com.fittrackpro.data.local.database.entity.TrainingPlan
import com.fittrackpro.data.local.database.entity.WorkoutTemplate
import java.util.Calendar
import java.util.UUID

/**
 * Training Plan Generator with Progressive Overload Algorithm
 *
 * Implements the progressive overload formula from the project specification:
 * - Week N Distance = Week N-1 × 1.10 (10% increase)
 * - Recovery Week (every 4th week) = -25% volume
 *
 * Creates personalized training plans based on:
 * - User's current fitness level (from historical data)
 * - Selected goal type (5K, 10K, Half Marathon, Marathon, Weight Loss)
 * - Available training days per week
 * - Target completion date
 */
object TrainingPlanGenerator {

    // Goal target distances in meters
    private val GOAL_DISTANCES = mapOf(
        "5k" to 5000.0,
        "10k" to 10000.0,
        "half_marathon" to 21097.5,
        "marathon" to 42195.0,
        "weight_loss" to 5000.0,
        "endurance" to 15000.0
    )

    // Base distances per difficulty level in meters
    private val BASE_DISTANCES = mapOf(
        "beginner" to 2000.0,      // 2km starting point
        "intermediate" to 4000.0,   // 4km starting point
        "advanced" to 6000.0        // 6km starting point
    )

    // Weekly volume distribution percentages
    private const val EASY_RUN_PERCENTAGE = 0.60      // 60% of weekly volume
    private const val TEMPO_RUN_PERCENTAGE = 0.20     // 20% of weekly volume
    private const val LONG_RUN_PERCENTAGE = 0.20      // 20% of weekly volume

    // Progressive overload constants
    private const val WEEKLY_INCREASE_RATE = 1.10     // 10% increase per week
    private const val RECOVERY_WEEK_REDUCTION = 0.75  // -25% on recovery weeks
    private const val RECOVERY_WEEK_INTERVAL = 4      // Every 4th week

    // Pace targets (min/km)
    private val PACE_TARGETS = mapOf(
        "easy_run" to 6.5f,       // Easy pace
        "tempo_run" to 5.0f,      // Tempo pace
        "interval" to 4.5f,       // Interval pace
        "long_run" to 7.0f,       // Long run pace
        "recovery" to 7.5f,       // Recovery pace
        "cross_training" to 0f    // No pace for cross training
    )

    /**
     * Data class to hold user's current fitness metrics
     */
    data class UserFitnessData(
        val avgDistanceLast30Days: Double = 0.0,    // meters
        val avgPaceLast30Days: Float = 0f,          // min/km
        val workoutFrequency: Int = 0,              // workouts per week
        val longestDistance: Double = 0.0,          // meters
        val totalDistanceLast30Days: Double = 0.0   // meters
    )

    /**
     * Generate a complete training plan with progressive overload
     */
    fun generatePlan(
        userId: String,
        goalType: String,
        difficulty: String,
        durationWeeks: Int,
        daysPerWeek: Int,
        userFitnessData: UserFitnessData? = null,
        startDate: Long = System.currentTimeMillis()
    ): GeneratedPlan {
        val planId = UUID.randomUUID().toString()

        // Calculate starting base distance
        val baseDistance = calculateBaseDistance(difficulty, goalType, userFitnessData)
        val goalDistance = GOAL_DISTANCES[goalType.lowercase()] ?: 5000.0

        // Create the training plan entity
        val plan = TrainingPlan(
            id = planId,
            name = "${formatGoalName(goalType)} Training Plan",
            description = generatePlanDescription(goalType, difficulty, durationWeeks, daysPerWeek),
            goalType = goalType.lowercase().replace(" ", "_"),
            durationWeeks = durationWeeks,
            difficulty = difficulty.lowercase(),
            workoutsPerWeek = daysPerWeek,
            isCustom = true,
            creatorId = userId,
            createdAt = System.currentTimeMillis()
        )

        // Generate workout templates with progressive overload
        val workoutTemplates = generateWorkoutTemplates(
            planId = planId,
            durationWeeks = durationWeeks,
            daysPerWeek = daysPerWeek,
            baseDistance = baseDistance,
            goalDistance = goalDistance,
            difficulty = difficulty.lowercase(),
            goalType = goalType.lowercase()
        )

        // Generate scheduled workouts
        val scheduledWorkouts = generateScheduledWorkouts(
            userId = userId,
            workoutTemplates = workoutTemplates,
            startDate = startDate
        )

        return GeneratedPlan(
            plan = plan,
            workoutTemplates = workoutTemplates,
            scheduledWorkouts = scheduledWorkouts,
            totalWorkouts = workoutTemplates.size
        )
    }

    /**
     * Calculate the appropriate starting base distance
     */
    private fun calculateBaseDistance(
        difficulty: String,
        goalType: String,
        userFitnessData: UserFitnessData?
    ): Double {
        val defaultBase = BASE_DISTANCES[difficulty.lowercase()] ?: 3000.0

        // If we have user fitness data, use it to personalize
        return if (userFitnessData != null && userFitnessData.avgDistanceLast30Days > 0) {
            // Start at user's current average, but not lower than difficulty minimum
            maxOf(userFitnessData.avgDistanceLast30Days, defaultBase * 0.8)
        } else {
            defaultBase
        }
    }

    /**
     * Generate all workout templates for the plan with progressive overload
     */
    private fun generateWorkoutTemplates(
        planId: String,
        durationWeeks: Int,
        daysPerWeek: Int,
        baseDistance: Double,
        goalDistance: Double,
        difficulty: String,
        goalType: String
    ): List<WorkoutTemplate> {
        val templates = mutableListOf<WorkoutTemplate>()
        var currentWeeklyVolume = baseDistance * daysPerWeek * 0.7 // Start at 70% capacity

        // Calculate required weekly increase to reach goal
        val targetWeeklyVolume = goalDistance * 1.5 // Target weekly volume is 1.5x race distance

        for (week in 1..durationWeeks) {
            // Apply progressive overload or recovery
            val isRecoveryWeek = week % RECOVERY_WEEK_INTERVAL == 0
            val weekVolume = if (isRecoveryWeek) {
                currentWeeklyVolume * RECOVERY_WEEK_REDUCTION
            } else {
                currentWeeklyVolume
            }

            // Generate workouts for this week
            val weekWorkouts = generateWeekWorkouts(
                planId = planId,
                weekNumber = week,
                daysPerWeek = daysPerWeek,
                weeklyVolume = weekVolume,
                isRecoveryWeek = isRecoveryWeek,
                difficulty = difficulty,
                goalType = goalType
            )
            templates.addAll(weekWorkouts)

            // Apply progressive overload for non-recovery weeks
            if (!isRecoveryWeek && week < durationWeeks) {
                currentWeeklyVolume *= WEEKLY_INCREASE_RATE
                // Cap at target volume
                currentWeeklyVolume = minOf(currentWeeklyVolume, targetWeeklyVolume)
            }
        }

        return templates
    }

    /**
     * Generate workouts for a single week
     */
    private fun generateWeekWorkouts(
        planId: String,
        weekNumber: Int,
        daysPerWeek: Int,
        weeklyVolume: Double,
        isRecoveryWeek: Boolean,
        difficulty: String,
        goalType: String
    ): List<WorkoutTemplate> {
        val workouts = mutableListOf<WorkoutTemplate>()

        // Workout distribution based on days per week
        val workoutTypes = getWorkoutDistribution(daysPerWeek, isRecoveryWeek)

        workoutTypes.forEachIndexed { index, type ->
            val dayNumber = index + 1
            val workoutDistance = calculateWorkoutDistance(type, weeklyVolume, daysPerWeek)
            val workoutDuration = calculateWorkoutDuration(type, workoutDistance, difficulty)
            val targetPace = PACE_TARGETS[type] ?: 6.0f

            val workout = WorkoutTemplate(
                id = UUID.randomUUID().toString(),
                planId = planId,
                weekNumber = weekNumber,
                dayNumber = dayNumber,
                name = formatWorkoutName(type, weekNumber, isRecoveryWeek),
                type = type,
                description = generateWorkoutDescription(type, weekNumber, isRecoveryWeek),
                instructions = generateWorkoutInstructions(type, difficulty),
                targetDistance = workoutDistance.toFloat(),
                targetDuration = workoutDuration,
                targetPace = if (type != "cross_training" && type != "rest") targetPace else null,
                warmupDuration = if (type != "rest") 5 * 60 * 1000L else null,
                cooldownDuration = if (type != "rest") 5 * 60 * 1000L else null,
                intervals = if (type == "interval") generateIntervalStructure(difficulty) else null
            )
            workouts.add(workout)
        }

        return workouts
    }

    /**
     * Get workout type distribution based on training days
     */
    private fun getWorkoutDistribution(daysPerWeek: Int, isRecoveryWeek: Boolean): List<String> {
        return if (isRecoveryWeek) {
            // Recovery week: mostly easy runs
            when (daysPerWeek) {
                3 -> listOf("easy_run", "recovery", "easy_run")
                4 -> listOf("easy_run", "recovery", "easy_run", "cross_training")
                5 -> listOf("easy_run", "recovery", "easy_run", "cross_training", "easy_run")
                6 -> listOf("easy_run", "recovery", "easy_run", "cross_training", "easy_run", "rest")
                else -> listOf("easy_run", "recovery", "easy_run")
            }
        } else {
            // Normal week: balanced distribution
            when (daysPerWeek) {
                3 -> listOf("easy_run", "tempo_run", "long_run")
                4 -> listOf("easy_run", "interval", "tempo_run", "long_run")
                5 -> listOf("easy_run", "interval", "tempo_run", "easy_run", "long_run")
                6 -> listOf("easy_run", "interval", "tempo_run", "recovery", "easy_run", "long_run")
                7 -> listOf("easy_run", "interval", "tempo_run", "recovery", "easy_run", "long_run", "rest")
                else -> listOf("easy_run", "tempo_run", "long_run")
            }
        }
    }

    /**
     * Calculate distance for specific workout type
     */
    private fun calculateWorkoutDistance(type: String, weeklyVolume: Double, daysPerWeek: Int): Double {
        val avgDistance = weeklyVolume / daysPerWeek

        return when (type) {
            "easy_run" -> avgDistance * 0.9
            "tempo_run" -> avgDistance * 0.8
            "interval" -> avgDistance * 0.6
            "long_run" -> avgDistance * 1.5
            "recovery" -> avgDistance * 0.5
            "cross_training" -> avgDistance * 0.7
            "rest" -> 0.0
            else -> avgDistance
        }
    }

    /**
     * Calculate duration based on distance and difficulty
     */
    private fun calculateWorkoutDuration(type: String, distance: Double, difficulty: String): Long {
        if (type == "rest" || distance == 0.0) return 0L

        val basePace = when (difficulty) {
            "beginner" -> 7.0  // 7 min/km
            "intermediate" -> 6.0  // 6 min/km
            "advanced" -> 5.0  // 5 min/km
            else -> 6.5
        }

        val adjustedPace = when (type) {
            "easy_run" -> basePace * 1.1
            "tempo_run" -> basePace * 0.85
            "interval" -> basePace * 0.75
            "long_run" -> basePace * 1.2
            "recovery" -> basePace * 1.3
            "cross_training" -> basePace * 1.0
            else -> basePace
        }

        val distanceKm = distance / 1000.0
        val durationMinutes = distanceKm * adjustedPace
        return (durationMinutes * 60 * 1000).toLong()
    }

    /**
     * Generate scheduled workouts from templates
     */
    private fun generateScheduledWorkouts(
        userId: String,
        workoutTemplates: List<WorkoutTemplate>,
        startDate: Long
    ): List<ScheduledWorkout> {
        val calendar = Calendar.getInstance().apply { timeInMillis = startDate }

        // Find the next Monday to start the plan
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return workoutTemplates.map { template ->
            val workoutCalendar = Calendar.getInstance().apply {
                timeInMillis = calendar.timeInMillis
                // Add weeks
                add(Calendar.WEEK_OF_YEAR, template.weekNumber - 1)
                // Add days
                add(Calendar.DAY_OF_YEAR, template.dayNumber - 1)
            }

            ScheduledWorkout(
                id = UUID.randomUUID().toString(),
                userId = userId,
                workoutTemplateId = template.id,
                scheduledDate = workoutCalendar.timeInMillis,
                status = "pending"
            )
        }
    }

    /**
     * Format goal name for display
     */
    private fun formatGoalName(goalType: String): String {
        return when (goalType.lowercase()) {
            "5k" -> "5K"
            "10k" -> "10K"
            "half_marathon" -> "Half Marathon"
            "marathon" -> "Marathon"
            "weight_loss" -> "Weight Loss"
            "endurance" -> "Endurance"
            else -> goalType.replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Format workout name
     */
    private fun formatWorkoutName(type: String, weekNumber: Int, isRecoveryWeek: Boolean): String {
        val prefix = if (isRecoveryWeek) "Recovery " else ""
        val baseName = when (type) {
            "easy_run" -> "Easy Run"
            "tempo_run" -> "Tempo Run"
            "interval" -> "Interval Training"
            "long_run" -> "Long Run"
            "recovery" -> "Recovery Jog"
            "cross_training" -> "Cross Training"
            "rest" -> "Rest Day"
            else -> type.replace("_", " ").replaceFirstChar { it.uppercase() }
        }
        return "$prefix$baseName"
    }

    /**
     * Generate plan description
     */
    private fun generatePlanDescription(
        goalType: String,
        difficulty: String,
        weeks: Int,
        daysPerWeek: Int
    ): String {
        return "A $difficulty ${formatGoalName(goalType)} training plan over $weeks weeks. " +
                "Train $daysPerWeek days per week with progressive overload to safely build your fitness. " +
                "Recovery weeks every 4th week to prevent overtraining."
    }

    /**
     * Generate workout description
     */
    private fun generateWorkoutDescription(type: String, weekNumber: Int, isRecoveryWeek: Boolean): String {
        val weekType = if (isRecoveryWeek) "Recovery Week $weekNumber" else "Week $weekNumber"
        return when (type) {
            "easy_run" -> "$weekType - Easy pace run to build aerobic base"
            "tempo_run" -> "$weekType - Comfortably hard pace to improve lactate threshold"
            "interval" -> "$weekType - High intensity intervals for speed development"
            "long_run" -> "$weekType - Longer distance at easy pace for endurance"
            "recovery" -> "$weekType - Very easy effort to promote recovery"
            "cross_training" -> "$weekType - Alternative activity (cycling, swimming, etc.)"
            "rest" -> "$weekType - Complete rest for recovery"
            else -> "$weekType - ${type.replace("_", " ")}"
        }
    }

    /**
     * Generate workout instructions based on type and difficulty
     */
    private fun generateWorkoutInstructions(type: String, difficulty: String): String {
        return when (type) {
            "easy_run" -> """
                |1. Start with 5-minute warm-up walk/jog
                |2. Run at conversational pace (you should be able to talk)
                |3. Keep heart rate in Zone 2 (60-70% max HR)
                |4. Focus on good form and relaxed breathing
                |5. End with 5-minute cool-down walk
                |6. Stretch major muscle groups
            """.trimMargin()

            "tempo_run" -> """
                |1. 10-minute easy warm-up jog
                |2. Run at "comfortably hard" pace
                |3. Should feel challenging but sustainable
                |4. Heart rate Zone 3-4 (80-90% max HR)
                |5. 10-minute easy cool-down jog
                |6. Dynamic stretching after
            """.trimMargin()

            "interval" -> when (difficulty) {
                "beginner" -> """
                    |1. 10-minute warm-up jog
                    |2. Run 30 seconds fast, 90 seconds recovery × 6
                    |3. Fast pace should be hard but controlled
                    |4. Recovery is slow jog or walk
                    |5. 10-minute cool-down jog
                """.trimMargin()
                "intermediate" -> """
                    |1. 10-minute warm-up jog
                    |2. Run 400m fast, 200m recovery × 6-8
                    |3. Fast pace at 85-90% effort
                    |4. Active recovery (slow jog)
                    |5. 10-minute cool-down jog
                """.trimMargin()
                else -> """
                    |1. 15-minute warm-up including strides
                    |2. Run 800m at 5K pace, 400m recovery × 5-6
                    |3. Maintain consistent split times
                    |4. Active recovery between reps
                    |5. 15-minute cool-down with stretching
                """.trimMargin()
            }

            "long_run" -> """
                |1. Start very easy - slower than normal easy pace
                |2. Build into comfortable easy pace after 10 minutes
                |3. Stay hydrated - drink every 20-30 minutes
                |4. Keep heart rate in Zone 2 (60-70% max HR)
                |5. Fuel with gels/snacks for runs over 90 minutes
                |6. Finish feeling like you could do more
                |7. Extended cool-down and stretching
            """.trimMargin()

            "recovery" -> """
                |1. Very easy effort - slower than easy pace
                |2. Should feel effortless
                |3. Heart rate Zone 1 (50-60% max HR)
                |4. Focus on relaxation and form
                |5. Good day for mental recovery too
            """.trimMargin()

            "cross_training" -> """
                |1. Choose low-impact activity (cycling, swimming, elliptical)
                |2. Moderate effort - similar to easy run effort
                |3. Duration similar to planned distance equivalent
                |4. Great for active recovery while reducing running impact
                |5. Focus on enjoying the variety
            """.trimMargin()

            "rest" -> """
                |1. Complete rest from structured exercise
                |2. Light walking is okay
                |3. Focus on sleep and nutrition
                |4. Foam rolling and stretching encouraged
                |5. Mental recovery is important too
            """.trimMargin()

            else -> "Follow the workout plan as scheduled. Listen to your body."
        }
    }

    /**
     * Generate interval structure as JSON string
     */
    private fun generateIntervalStructure(difficulty: String): String {
        return when (difficulty) {
            "beginner" -> """{"reps":6,"work_seconds":30,"rest_seconds":90,"work_pace":"fast","rest_type":"walk_jog"}"""
            "intermediate" -> """{"reps":8,"work_distance":400,"rest_distance":200,"work_pace":"5k_pace","rest_type":"jog"}"""
            "advanced" -> """{"reps":6,"work_distance":800,"rest_distance":400,"work_pace":"5k_pace","rest_type":"jog"}"""
            else -> """{"reps":6,"work_seconds":60,"rest_seconds":60,"work_pace":"hard","rest_type":"jog"}"""
        }
    }

    /**
     * Data class to hold generated plan data
     */
    data class GeneratedPlan(
        val plan: TrainingPlan,
        val workoutTemplates: List<WorkoutTemplate>,
        val scheduledWorkouts: List<ScheduledWorkout>,
        val totalWorkouts: Int
    )
}
