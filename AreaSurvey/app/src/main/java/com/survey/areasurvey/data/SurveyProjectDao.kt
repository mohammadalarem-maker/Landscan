package com.survey.areasurvey.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SurveyProjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: SurveyProjectEntity): Long

    @Delete
    suspend fun delete(project: SurveyProjectEntity)

    @Query("SELECT * FROM survey_projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<SurveyProjectEntity>>

    @Query("SELECT * FROM survey_projects WHERE id = :id")
    suspend fun getProjectById(id: Long): SurveyProjectEntity?
}
