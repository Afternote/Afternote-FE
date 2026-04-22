package com.afternote.feature.setting.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        content()
    }
}
