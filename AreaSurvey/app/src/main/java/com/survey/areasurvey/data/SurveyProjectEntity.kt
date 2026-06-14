package com.survey.areasurvey.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "survey_projects")
data class SurveyProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val mode: String,              // "WALKING" أو "TOTAL_STATION"
    val areaSquareMeters: Double,
    val perimeterMeters: Double,
    val pointsJson: String         // قائمة النقاط محفوظة كـ JSON
)

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun pointsToJson(points: List<SurveyPointEntity>): String {
        return gson.toJson(points)
    }

    @TypeConverter
    fun jsonToPoints(json: String): List<SurveyPointEntity> {
        val type = object : TypeToken<List<SurveyPointEntity>>() {}.type
        return gson.fromJson(json, type)
    }
}

/**
 * نسخة قابلة للتسلسل (Serializable) من SurveyPoint لتخزينها في JSON
 */
data class SurveyPointEntity(
    val id: Int,
    val lat: Double,
    val lng: Double,
    val label: String,
    val distanceFromStation: Double? = null,
    val azimuthFromStation: Double? = null
)
