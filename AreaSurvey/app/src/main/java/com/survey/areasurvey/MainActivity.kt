package com.survey.areasurvey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.survey.areasurvey.ui.SurveyApp
import com.survey.areasurvey.ui.theme.AreaSurveyTheme
import com.survey.areasurvey.viewmodel.SurveyViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SurveyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AreaSurveyTheme {
                Surface {
                    SurveyApp(viewModel = viewModel)
                }
            }
        }
    }
}
