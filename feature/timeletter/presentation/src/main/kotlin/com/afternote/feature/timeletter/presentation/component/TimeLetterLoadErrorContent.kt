package com.afternote.feature.timeletter.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.timeletter.presentation.R

@Composable
fun TimeLetterLoadErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = AfternoteDesign.typography.bodyLargeR,
            color = AfternoteDesign.colors.gray7,
        )
        TextButton(onClick = onRetry) {
            Text(
                text = stringResource(R.string.timeletter_retry),
                style = AfternoteDesign.typography.bodyLargeB,
                color = AfternoteDesign.colors.gray9,
            )
        }
    }
}
