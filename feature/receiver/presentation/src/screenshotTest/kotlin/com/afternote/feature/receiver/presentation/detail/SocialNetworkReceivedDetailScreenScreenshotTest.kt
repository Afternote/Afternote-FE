package com.afternote.feature.receiver.presentation.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun socialNetworkReceivedDetailScreenScreenshot() {
    AfternoteTheme {
        SocialNetworkReceivedDetailScreen(onBackClick = {})
    }
}
