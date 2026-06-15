package com.survey.areasurvey.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.survey.areasurvey.sensors.CompassManager
import com.survey.areasurvey.viewmodel.SurveyViewModel
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.delay

/**
 * دالة لتنظيف وتحويل الأرقام العربية والفاصلة إلى صيغة عشرية إنجليزية قياسية
 */
fun String.normalizeAndroidNumbers(): String {
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    var result = this
    for (i in 0..9) {
        result = result.replace(arabicDigits[i], englishDigits[i])
    }
    return result.replace(',', '.').trim()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TotalStationScreen(
    viewModel: SurveyViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val compassManager = remember { CompassManager(context) }

    var distanceInput by remember { mutableStateOf("") }
    var manualAzimuthInput by remember { mutableStateOf("") }
    var useManualAzimuth by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    // تقييد تدفق البوصلة (Throttle) ليعمل كل 250 ملي ثانية لمنع تعليق الكيبورد والواجهة
    LaunchedEffect(Unit) {
        var lastUpdateTime = 0L
        compassManager.getAzimuthFlow().collect { azimuth ->
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastUpdateTime > 250) { 
                viewModel.updateCurrentAzimuth(azimuth)
                lastUpdateTime = currentTime
            }
        }
    }

    // تجهيز المدخلات النظيفة للتحقق البرمجي
    val cleanDistance = distanceInput.normalizeAndroidNumbers()
    val cleanManualAzimuth = manualAzimuthInput.normalizeAndroidNumbers()
    
    val isDistanceValid = cleanDistance.toDoubleOrNull() != null
    val isAzimuthValid = !useManualAzimuth || cleanManualAzimuth.toDoubleOrNull() != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Total Station", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.removeLastPoint() }) {
                        Icon(Icons.Default.Undo, contentDescription = "حذف آخر نقطة")
                    }
                    IconButton(onClick = { viewModel.reset() }) {
                        Icon(Icons.Default.Delete, contentDescription = "مسح الكل")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // الخريطة في النصف العلوي
            Box(modifier = Modifier.weight(1f)) {
                SurveyMap(state = state)
                
                AreaInfoCard(
                    area = state.areaSquareMeters,
                    perimeter = state.perimeterMeters,
                    pointCount = state.points.size,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp)
                )

                // زر تحكم عائم جانبي للتركيز
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            // للاستخدام المستقبلي في توجيه الخريطة
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "تركيز")
                    }
                }
            }

            // لوحة التحكم في النصف السفلي
            Surface(shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    // كرت نقطة الوقوف
                    StationCard(
                        stationPoint = state.stationPoint,
                        onSetStation = { viewModel.setStationPoint() }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // عرض الزاوية الحالية
                    AzimuthDisplay(
                        azimuth = state.currentAzimuth,
                        useManual = useManualAzimuth,
                        onToggleManual = { useManualAzimuth = !useManualAzimuth }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // حقول الإدخال
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = distanceInput,
                            onValueChange = { distanceInput = it },
                            label = { Text("المسافة (متر)") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        if (useManualAzimuth) {
                            OutlinedTextField(
                                value = manualAzimuthInput,
                                onValueChange = { manualAzimuthInput = it },
                                label = { Text("الزاوية °") },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                val dist = cleanDistance.toDoubleOrNull()
                                if (dist != null && state.stationPoint != null) {
                                    if (useManualAzimuth) {
                                        val az = cleanManualAzimuth.toDoubleOrNull()
                                        if (az != null) {
                                            viewModel.addTotalStationPointManual(dist, az)
                                            distanceInput = ""
                                            manualAzimuthInput = ""
                                        }
                                    } else {
                                        viewModel.addTotalStationPoint(dist)
                                        distanceInput = ""
                                    }
                                }
                            },
                            enabled = state.stationPoint != null && isDistanceValid && isAzimuthValid,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AddLocation, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة نقطة هدف")
                        }

                        OutlinedButton(
                            onClick = { showSaveDialog = true },
                            enabled = state.points.size >= 3,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حفظ")
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        SaveProjectDialog(
            onDismiss = { showSaveDialog = false },
            onConfirm = { name ->
                viewModel.saveProject(name)
                showSaveDialog = false
            }
        )
    }
}

@Composable
fun StationCard(
    stationPoint: GeoPoint?,
    onSetStation: () -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("نقطة الوقوف (Station)", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = stationPoint?.let { "%.6f, %.6f".format(java.util.Locale.US, it.latitude, it.longitude) }
                        ?: "غير محددة",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(onClick = onSetStation) {
                Icon(Icons.Default.GpsFixed, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("تحديد")
            }
        }
    }
}

@Composable
fun AzimuthDisplay(
    azimuth: Double,
    useManual: Boolean,
    onToggleManual: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("الزاوية الحالية (Azimuth)", style = MaterialTheme.typography.labelMedium)
            Text(
                text = "%.1f°  %s".format(java.util.Locale.US, azimuth, directionLabel(azimuth)),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        FilterChip(
            selected = useManual,
            onClick = onToggleManual,
            label = { Text(if (useManual) "إدخال يدوي" else "بوصلة تلقائية") }
        )
    }
}

private fun directionLabel(azimuth: Double): String {
    val directions = listOf("شمال", "شمال شرق", "شرق", "جنوب شرق", "جنوب", "جنوب غرب", "غرب", "شمال غرب")
    val index = (((azimuth + 22.5) / 45.0).toInt()) % 8
    return directions[index]
}
