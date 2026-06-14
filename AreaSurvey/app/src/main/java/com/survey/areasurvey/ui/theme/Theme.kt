package com.survey.areasurvey.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ألوان مستوحاة من أدوات المساحة الميدانية: أصفر سيرفاير (Surveyor's Yellow)
// + أخضر طوبوغرافي + رمادي معدني
val SurveyorYellow = Color(0xFFFFC400)
val TopoGreen = Color(0xFF2E7D32)
val SteelGray = Color(0xFF37474F)
val FieldCream = Color(0xFFF5F1E8)
val AlertOrange = Color(0xFFE65100)

private val LightColors = lightColorScheme(
    primary = SteelGray,
    secondary = TopoGreen,
    tertiary = SurveyorYellow,
    background = FieldCream,
    surface = Color.White,
    error = AlertOrange
)

private val DarkColors = darkColorScheme(
    primary = SurveyorYellow,
    secondary = TopoGreen,
    tertiary = SteelGray,
    background = Color(0xFF1B1F22),
    surface = Color(0xFF263238)
)

@Composable
fun AreaSurveyTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
