package com.afternote.feature.afternote.presentation.shared.body

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme

@Composable
fun LoadingListBody(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(Modifier.size(40.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingListBodyPreview() {
    AfternoteTheme {
        LoadingListBody()
    }
}
