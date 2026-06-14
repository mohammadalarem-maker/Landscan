package com.survey.areasurvey.data

import org.osmdroid.util.GeoPoint

/**
 * نقطة واحدة في المسح (سواء من GPS مباشرة أو محسوبة من Total Station)
 */
data class SurveyPoint(
    val id: Int,
    val geoPoint: GeoPoint,
    val label: String = "",      // مثل: P1, P2 ...
    val distanceFromStation: Double? = null, // المسافة من نقطة الوقوف (متر) - لوضع Total Station
    val azimuthFromStation: Double? = null   // الزاوية من نقطة الوقوف (درجة) - لوضع Total Station
)
