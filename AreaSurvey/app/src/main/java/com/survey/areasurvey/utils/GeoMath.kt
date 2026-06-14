package com.survey.areasurvey.utils

import org.osmdroid.util.GeoPoint
import kotlin.math.*

object GeoMath {

    private const val EARTH_RADIUS = 6378137.0 // متر (WGS84)

    /**
     * حساب مساحة المضلع بالمتر المربع (Spherical Excess / Geodesic Area)
     * يتطلب 3 نقاط على الأقل
     */
    fun calculatePolygonAreaSquareMeters(points: List<GeoPoint>): Double {
        if (points.size < 3) return 0.0

        var total = 0.0
        val n = points.size
        for (i in 0 until n) {
            val p1 = points[i]
            val p2 = points[(i + 1) % n]
            val lon1 = Math.toRadians(p1.longitude)
            val lon2 = Math.toRadians(p2.longitude)
            val lat1 = Math.toRadians(p1.latitude)
            val lat2 = Math.toRadians(p2.latitude)
            total += (lon2 - lon1) * (2 + sin(lat1) + sin(lat2))
        }
        return abs(total * EARTH_RADIUS * EARTH_RADIUS / 2.0)
    }

    /**
     * حساب المسافة بين نقطتين بالمتر (Haversine)
     */
    fun computeDistance(from: GeoPoint, to: GeoPoint): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)

        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS * c
    }

    /**
     * حساب المحيط الكلي بالمتر
     */
    fun calculatePerimeterMeters(points: List<GeoPoint>, closed: Boolean = true): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until points.size - 1) {
            total += computeDistance(points[i], points[i + 1])
        }
        if (closed && points.size > 2) {
            total += computeDistance(points.last(), points.first())
        }
        return total
    }

    /**
     * تحويل المساحة من متر مربع إلى وحدات أخرى
     */
    fun squareMetersToHectares(sqm: Double): Double = sqm / 10000.0
    fun squareMetersToAcres(sqm: Double): Double = sqm / 4046.8564224
    fun squareMetersToFeddan(sqm: Double): Double = sqm / 4200.0 // الفدان المصري ~4200 م²
    fun squareMetersToDunam(sqm: Double): Double = sqm / 1000.0  // الدونم ~1000 م²
    fun squareMetersToKm2(sqm: Double): Double = sqm / 1_000_000.0

    /**
     * === وضع Total Station ===
     * حساب موقع نقطة هدف (Target) بناءً على:
     * - نقطة الوقوف (Station): GeoPoint
     * - المسافة بالمتر
     * - الزاوية Azimuth بالدرجات (0 = شمال، 90 = شرق، يزيد مع اتجاه الساعة)
     *
     * صيغة "Destination Point" الكروية (Spherical Law of Cosines)
     */
    fun computeTargetPoint(station: GeoPoint, distanceMeters: Double, azimuthDegrees: Double): GeoPoint {
        val lat1 = Math.toRadians(station.latitude)
        val lon1 = Math.toRadians(station.longitude)
        val brng = Math.toRadians(azimuthDegrees)
        val angularDistance = distanceMeters / EARTH_RADIUS

        val lat2 = asin(
            sin(lat1) * cos(angularDistance) + cos(lat1) * sin(angularDistance) * cos(brng)
        )
        val lon2 = lon1 + atan2(
            sin(brng) * sin(angularDistance) * cos(lat1),
            cos(angularDistance) - sin(lat1) * sin(lat2)
        )

        return GeoPoint(Math.toDegrees(lat2), Math.toDegrees(lon2))
    }

    /**
     * حساب الزاوية (Azimuth) بين نقطتين بالدرجات (0-360)، 0 = شمال
     */
    fun computeAzimuth(from: GeoPoint, to: GeoPoint): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)

        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        var heading = Math.toDegrees(atan2(y, x))
        if (heading < 0) heading += 360.0
        return heading
    }

    /**
     * تحويل قراءة البوصلة الخام (Azimuth بالنسبة للشمال المغناطيسي) إلى Azimuth حقيقي
     * عبر تطبيق Declination (اختياري - يمكن إدخاله من المستخدم أو تركه 0)
     */
    fun applyMagneticDeclination(magneticAzimuth: Double, declination: Double): Double {
        var result = magneticAzimuth + declination
        result %= 360.0
        if (result < 0) result += 360.0
        return result
    }
}
