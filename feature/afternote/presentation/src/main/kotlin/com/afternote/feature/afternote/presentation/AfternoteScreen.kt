package com.afternote.feature.afternote.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.afternote.core.ui.TopBar

@Composable
fun AfternoteScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { TopBar() },
    ) { paddingValues ->
        Text(
            text = "애프터노트",
            modifier = Modifier.padding(paddingValues),
        )
    }
}
