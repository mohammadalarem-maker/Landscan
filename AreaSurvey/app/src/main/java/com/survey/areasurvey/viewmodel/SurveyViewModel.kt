package com.survey.areasurvey.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.survey.areasurvey.data.AppDatabase
import com.survey.areasurvey.data.SurveyPoint
import com.survey.areasurvey.data.SurveyPointEntity
import com.survey.areasurvey.data.SurveyProjectEntity
import com.survey.areasurvey.location.LocationManager
import com.survey.areasurvey.utils.GeoMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

enum class SurveyMode {
    NONE, WALKING, TOTAL_STATION
}

data class SurveyUiState(
    val mode: SurveyMode = SurveyMode.NONE,
    val points: List<SurveyPoint> = emptyList(),
    val stationPoint: GeoPoint? = null, // نقطة الوقوف في وضع Total Station
    val areaSquareMeters: Double = 0.0,
    val perimeterMeters: Double = 0.0,
    val isTracking: Boolean = false,
    val currentAzimuth: Double = 0.0, // قراءة البوصلة الحالية
    val magneticDeclination: Double = 0.0
)

class SurveyViewModel(application: Application) : AndroidViewModel(application) {

    private val locationManager = LocationManager(application)
    private val db = AppDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(SurveyUiState())
    val uiState: StateFlow<SurveyUiState> = _uiState.asStateFlow()

    // ===== التحكم بالوضع =====

    fun setMode(mode: SurveyMode) {
        _uiState.value = SurveyUiState(mode = mode)
    }

    fun reset() {
        _uiState.value = _uiState.value.copy(
            points = emptyList(),
            stationPoint = null,
            areaSquareMeters = 0.0,
            perimeterMeters = 0.0,
            isTracking = false
        )
    }

    // ===== وضع المشي (Walking Survey) =====

    fun startWalkingTracking() {
        _uiState.value = _uiState.value.copy(isTracking = true)
        viewModelScope.launch {
            locationManager.getLocationUpdates(intervalMs = 2000L).collect { geoPoint ->
                if (_uiState.value.isTracking) {
                    addPoint(geoPoint)
                }
            }
        }
    }

    fun stopWalkingTracking() {
        _uiState.value = _uiState.value.copy(isTracking = false)
    }

    /** إضافة نقطة يدوياً بالضغط على زر "تسجيل نقطة" */
    fun addManualPoint() {
        viewModelScope.launch {
            val geoPoint = locationManager.getCurrentLocationOnce()
            geoPoint?.let { addPoint(it) }
        }
    }

    private fun addPoint(geoPoint: GeoPoint) {
        val current = _uiState.value.points
        val newPoint = SurveyPoint(
            id = current.size + 1,
            geoPoint = geoPoint,
            label = "P${current.size + 1}"
        )
        val updatedPoints = current + newPoint
        recalculate(updatedPoints)
    }

    fun removeLastPoint() {
        val current = _uiState.value.points
        if (current.isNotEmpty()) {
            recalculate(current.dropLast(1))
        }
    }

    private fun recalculate(points: List<SurveyPoint>) {
        val geoPoints = points.map { it.geoPoint }
        val area = GeoMath.calculatePolygonAreaSquareMeters(geoPoints)
        val perimeter = GeoMath.calculatePerimeterMeters(geoPoints)
        _uiState.value = _uiState.value.copy(
            points = points,
            areaSquareMeters = area,
            perimeterMeters = perimeter
        )
    }

    // ===== وضع Total Station =====

    /** تحديد نقطة الوقوف الحالية بالـ GPS */
    fun setStationPoint() {
        viewModelScope.launch {
            val geoPoint = locationManager.getCurrentLocationOnce()
            geoPoint?.let {
                _uiState.value = _uiState.value.copy(stationPoint = it)
            }
        }
    }

    fun setMagneticDeclination(value: Double) {
        _uiState.value = _uiState.value.copy(magneticDeclination = value)
    }

    fun updateCurrentAzimuth(azimuth: Double) {
        _uiState.value = _uiState.value.copy(currentAzimuth = azimuth)
    }

    /**
     * إضافة نقطة هدف بناءً على المسافة المُدخلة والزاوية الحالية من البوصلة
     */
    fun addTotalStationPoint(distanceMeters: Double) {
        val station = _uiState.value.stationPoint ?: return
        val rawAzimuth = _uiState.value.currentAzimuth
        val correctedAzimuth = GeoMath.applyMagneticDeclination(
            rawAzimuth, _uiState.value.magneticDeclination
        )
        val targetPoint = GeoMath.computeTargetPoint(station, distanceMeters, correctedAzimuth)

        val current = _uiState.value.points
        val newPoint = SurveyPoint(
            id = current.size + 1,
            geoPoint = targetPoint,
            label = "P${current.size + 1}",
            distanceFromStation = distanceMeters,
            azimuthFromStation = correctedAzimuth
        )
        recalculate(current + newPoint)
    }

    /**
     * إضافة نقطة هدف بإدخال يدوي للمسافة والزاوية (بدون بوصلة)
     */
    fun addTotalStationPointManual(distanceMeters: Double, azimuthDegrees: Double) {
        val station = _uiState.value.stationPoint ?: return
        val targetPoint = GeoMath.computeTargetPoint(station, distanceMeters, azimuthDegrees)

        val current = _uiState.value.points
        val newPoint = SurveyPoint(
            id = current.size + 1,
            geoPoint = targetPoint,
            label = "P${current.size + 1}",
            distanceFromStation = distanceMeters,
            azimuthFromStation = azimuthDegrees
        )
        recalculate(current + newPoint)
    }

    // ===== الحفظ في قاعدة البيانات =====

    fun saveProject(name: String) {
        val state = _uiState.value
        viewModelScope.launch {
            val pointEntities = state.points.map {
                SurveyPointEntity(
                    id = it.id,
                    lat = it.geoPoint.latitude,
                    lng = it.geoPoint.longitude,
                    label = it.label,
                    distanceFromStation = it.distanceFromStation,
                    azimuthFromStation = it.azimuthFromStation
                )
            }
            val gson = com.google.gson.Gson()
            val entity = SurveyProjectEntity(
                name = name,
                mode = state.mode.name,
                areaSquareMeters = state.areaSquareMeters,
                perimeterMeters = state.perimeterMeters,
                pointsJson = gson.toJson(pointEntities)
            )
            db.surveyProjectDao().insert(entity)
        }
    }

    fun getAllProjects() = db.surveyProjectDao().getAllProjects()
}
