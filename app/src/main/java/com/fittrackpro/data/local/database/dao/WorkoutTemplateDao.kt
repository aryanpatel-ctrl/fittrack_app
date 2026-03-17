package com.fittrackpro.data.local.database.dao

import androidx.room.*
import com.fittrackpro.data.local.database.entity.WorkoutTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutTemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: WorkoutTemplate)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<WorkoutTemplate>)

    @Update
    suspend fun updateTemplate(template: WorkoutTemplate)

    @Delete
    suspend fun deleteTemplate(template: WorkoutTemplate)

    @Query("SELECT * FROM workout_templates WHERE id = :templateId")
    suspend fun getTemplateById(templateId: String): WorkoutTemplate?

    @Query("SELECT * FROM workout_templates WHERE planId = :planId ORDER BY weekNumber, dayNumber")
    fun getTemplatesByPlanId(planId: String): Flow<List<WorkoutTemplate>>

    @Query("SELECT * FROM workout_templates WHERE planId = :planId AND weekNumber = :week ORDER BY dayNumber")
    suspend fun getTemplatesForWeek(planId: String, week: Int): List<WorkoutTemplate>

    @Query("SELECT * FROM workout_templates WHERE planId = :planId AND weekNumber = :week AND dayNumber = :day")
    suspend fun getTemplateForDay(planId: String, week: Int, day: Int): WorkoutTemplate?

    @Query("SELECT COUNT(*) FROM workout_templates WHERE planId = :planId")
    suspend fun getTemplateCountForPlan(planId: String): Int

    @Query("SELECT MAX(weekNumber) FROM workout_templates WHERE planId = :planId")
    suspend fun getMaxWeekForPlan(planId: String): Int?
}
