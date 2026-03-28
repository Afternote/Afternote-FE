package com.afternote.afternote_fe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.afternote.afternote_fe.navigation.AppNavigation
import com.afternote.core.ui.theme.AfternoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AfternoteTheme {
                AppNavigation()
            }
        }
    }
}
