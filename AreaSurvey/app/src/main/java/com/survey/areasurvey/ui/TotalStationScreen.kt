package com.survey.areasurvey.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.survey.areasurvey.sensors.CompassManager
import com.survey.areasurvey.viewmodel.SurveyViewModel
import org.osmdroid.util.GeoPoint

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

    // قراءة البوصلة المستمرة
    LaunchedEffect(Unit) {
        compassManager.getAzimuthFlow().collect { azimuth ->
            viewModel.updateCurrentAzimuth(azimuth)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Total Station") },
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
            }

            // لوحة التحكم في النصف السفلي
            Surface(elevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    // نقطة الوقوف
                    StationCard(
                        stationPoint = state.stationPoint,
                        onSetStation = { viewModel.setStationPoint() }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // قراءة الزاوية الحالية
                    AzimuthDisplay(
                        azimuth = state.currentAzimuth,
                        useManual = useManualAzimuth,
                        onToggleManual = { useManualAzimuth = !useManualAzimuth }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // إدخال المسافة والزاوية اليدوية
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
                                val dist = distanceInput.toDoubleOrNull()
                                if (dist != null && state.stationPoint != null) {
                                    if (useManualAzimuth) {
                                        val az = manualAzimuthInput.toDoubleOrNull()
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
                            enabled = state.stationPoint != null && distanceInput.toDoubleOrNull() != null,
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
                    text = stationPoint?.let { "%.6f, %.6f".format(it.latitude, it.longitude) }
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
                text = "%.1f°  %s".format(azimuth, directionLabel(azimuth)),
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
