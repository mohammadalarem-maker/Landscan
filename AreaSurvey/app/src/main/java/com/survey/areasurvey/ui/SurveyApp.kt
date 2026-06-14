package com.survey.areasurvey.ui

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.survey.areasurvey.viewmodel.SurveyMode
import com.survey.areasurvey.viewmodel.SurveyViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SurveyApp(viewModel: SurveyViewModel) {

    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    if (!locationPermissions.allPermissionsGranted) {
        PermissionRequestScreen(
            onRequestPermission = { locationPermissions.launchMultiplePermissionRequest() }
        )
        return
    }

    val state by viewModel.uiState.collectAsState()

    when (state.mode) {
        SurveyMode.NONE -> {
            HomeScreen(onModeSelected = { viewModel.setMode(it) })
        }
        SurveyMode.WALKING -> {
            WalkingSurveyScreen(
                viewModel = viewModel,
                onBack = { viewModel.setMode(SurveyMode.NONE) }
            )
        }
        SurveyMode.TOTAL_STATION -> {
            TotalStationScreen(
                viewModel = viewModel,
                onBack = { viewModel.setMode(SurveyMode.NONE) }
            )
        }
    }
}

@Composable
fun PermissionRequestScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "هذا التطبيق يحتاج إذن الموقع (GPS) لقياس المساحات والمسافات",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("منح الإذن")
        }
    }
}
