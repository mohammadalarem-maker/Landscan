package com.survey.areasurvey.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.survey.areasurvey.utils.GeoMath
import com.survey.areasurvey.viewmodel.SurveyUiState
import com.survey.areasurvey.viewmodel.SurveyViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkingSurveyScreen(
    viewModel: SurveyViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مسح بالمشي (GPS)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.removeLastPoint() }) {
                        Icon(Icons.Default.Undo, contentDescription = "حذف آخر نقطة")
                    }
                    IconButton(onClick = {
                        viewModel.reset()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "مسح الكل")
                    }
                }
            )
        },
        bottomBar = {
            WalkingControls(
                state = state,
                onToggleTracking = {
                    if (state.isTracking) viewModel.stopWalkingTracking()
                    else viewModel.startWalkingTracking()
                },
                onAddPoint = { viewModel.addManualPoint() },
                onSave = { showSaveDialog = true }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            SurveyMap(state = state)

            AreaInfoCard(
                area = state.areaSquareMeters,
                perimeter = state.perimeterMeters,
                pointCount = state.points.size,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
            )
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

/**
 * خريطة OpenStreetMap عبر osmdroid، مدمجة في Compose بواسطة AndroidView
 */
@Composable
fun SurveyMap(state: SurveyUiState) {
    val context = LocalContext.current
    val defaultLocation = GeoPoint(15.3694, 44.1910) // صنعاء كنقطة افتراضية

    // تهيئة إعدادات osmdroid (مرة واحدة)
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(18.0)
            controller.setCenter(defaultLocation)
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { mapView },
        update = { map ->
            map.overlays.clear()

            // رسم المضلع
            if (state.points.size >= 2) {
                val polygon = Polygon(map).apply {
                    points = state.points.map { it.geoPoint }
                    fillColor = 0x332E7D32
                    strokeColor = 0xFF2E7D32.toInt()
                    strokeWidth = 4f
                }
                map.overlays.add(polygon)
            }

            // نقاط المسح
            state.points.forEach { point ->
                val marker = Marker(map).apply {
                    position = point.geoPoint
                    title = point.label
                    snippet = point.distanceFromStation?.let {
                        "المسافة: %.2f م، الزاوية: %.1f°".format(it, point.azimuthFromStation ?: 0.0)
                    }
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                map.overlays.add(marker)
            }

            // نقطة الوقوف (Total Station)
            state.stationPoint?.let { station ->
                val stationMarker = Marker(map).apply {
                    position = station
                    title = "نقطة الوقوف"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                map.overlays.add(stationMarker)

                // خطوط من نقطة الوقوف لكل نقطة هدف
                state.points.forEach { point ->
                    val line = Polyline(map).apply {
                        setPoints(listOf(station, point.geoPoint))
                        outlinePaint.color = 0xFFFFC400.toInt()
                        outlinePaint.strokeWidth = 3f
                    }
                    map.overlays.add(line)
                }
            }

            // تحريك الكاميرا لآخر نقطة
            val target = state.points.lastOrNull()?.geoPoint ?: state.stationPoint
            target?.let { map.controller.animateTo(it) }

            map.invalidate()
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            mapView.onDetach()
        }
    }
}

@Composable
fun AreaInfoCard(
    area: Double,
    perimeter: Double,
    pointCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoItem(label = "المساحة", value = "%.2f م²".format(area))
                InfoItem(label = "هكتار", value = "%.4f".format(GeoMath.squareMetersToHectares(area)))
                InfoItem(label = "النقاط", value = pointCount.toString())
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoItem(label = "المحيط", value = "%.2f م".format(perimeter))
                InfoItem(label = "دونم", value = "%.3f".format(GeoMath.squareMetersToDunam(area)))
                InfoItem(label = "فدان", value = "%.4f".format(GeoMath.squareMetersToFeddan(area)))
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WalkingControls(
    state: SurveyUiState,
    onToggleTracking: () -> Unit,
    onAddPoint: () -> Unit,
    onSave: () -> Unit
) {
    Surface(elevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onToggleTracking) {
                Icon(
                    imageVector = if (state.isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (state.isTracking) "إيقاف التتبع" else "بدء التتبع")
            }

            OutlinedButton(onClick = onAddPoint) {
                Icon(Icons.Default.AddLocation, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("تسجيل نقطة")
            }

            OutlinedButton(onClick = onSave, enabled = state.points.size >= 3) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("حفظ")
            }
        }
    }
}

@Composable
fun SaveProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("حفظ المشروع") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("اسم المشروع") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
