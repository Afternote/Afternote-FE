package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.TopBar

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { TopBar() },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(horizontal = 22.5.dp)) {
            Text(
                text = "나의 기록",
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    HomeScreen()
}
