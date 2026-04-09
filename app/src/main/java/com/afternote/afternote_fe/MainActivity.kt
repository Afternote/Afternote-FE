package com.afternote.afternote_fe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.afternote_fe.navigation.AppNavigation
import com.afternote.core.ui.theme.AfternoteTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AfternoteTheme {
                val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
                val startRoute by viewModel.startRoute.collectAsStateWithLifecycle()

                if (!isLoading) {
                    AppNavigation(startDestination = startRoute)
                }
            }
        }
    }
}
