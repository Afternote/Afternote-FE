package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.component.PushToggleSection
import com.afternote.feature.setting.presentation.viewmodel.PushNotificationUiState
import com.afternote.feature.setting.presentation.viewmodel.PushNotificationViewModel

@Composable
fun PushNotificationScreen(
    onBack: () -> Unit,
    viewModel: PushNotificationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PushNotificationContent(
        uiState = uiState,
        onBack = onBack,
        onNewsletterToggle = viewModel::onNewsletterToggle,
        onMindRecordToggle = viewModel::onMindRecordToggle,
        onAfternoteToggle = viewModel::onAfternoteToggle,
    )
}

@Composable
private fun PushNotificationContent(
    uiState: PushNotificationUiState,
    onBack: () -> Unit,
    onNewsletterToggle: (Boolean) -> Unit,
    onMindRecordToggle: (Boolean) -> Unit,
    onAfternoteToggle: (Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.push_notification_title),
                onBackClick = onBack,
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            PushToggleSection(
                uiState = uiState,
                onNewsletterToggle = onNewsletterToggle,
                onMindRecordToggle = onMindRecordToggle,
                onAfternoteToggle = onAfternoteToggle,
            )

            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.push_notification_device_guide),
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray5,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PushNotificationContentPreview() {
    PushNotificationContent(
        uiState = PushNotificationUiState(isAfternoteOn = true),
        onBack = {},
        onNewsletterToggle = {},
        onMindRecordToggle = {},
        onAfternoteToggle = {},
    )
}
