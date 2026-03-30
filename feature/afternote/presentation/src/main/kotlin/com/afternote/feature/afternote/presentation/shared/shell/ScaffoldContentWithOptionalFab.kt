package com.afternote.feature.afternote.presentation.shared.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.afternote.core.ui.button.AddFloatingActionButton

@Composable
fun ScaffoldContentWithOptionalFab(
    paddingValues: PaddingValues,
    showFab: Boolean,
    onFabClick: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        content(Modifier.fillMaxSize())
        if (showFab) {
            AddFloatingActionButton(onClick = onFabClick)
        }
    }
}
