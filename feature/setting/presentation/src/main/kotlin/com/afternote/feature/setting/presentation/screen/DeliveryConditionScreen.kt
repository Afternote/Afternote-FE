package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.component.RadioGroup
import com.afternote.feature.setting.presentation.component.RadioGroupItem
import com.afternote.feature.setting.presentation.viewmodel.DeliveryConditionUiState
import com.afternote.feature.setting.presentation.viewmodel.DeliveryConditionViewModel

@Composable
fun DeliveryConditionScreen(
    onBack: () -> Unit,
    viewModel: DeliveryConditionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DeliveryConditionContent(
        uiState = uiState,
        onBack = onBack,
        onSelectDeliveryMethodIndex = viewModel::onSelectDeliveryMethodIndex,
        onSelectProcessingMethodIndex = viewModel::onSelectProcessingMethodIndex,
        onFarewellMessageChange = viewModel::onFarewellMessageChange,
    )
}

@Composable
private fun DeliveryConditionContent(
    uiState: DeliveryConditionUiState,
    onBack: () -> Unit,
    onSelectDeliveryMethodIndex: (Int) -> Unit,
    onSelectProcessingMethodIndex: (Int) -> Unit,
    onFarewellMessageChange: (String) -> Unit,
) {
    val farewellMessageState = rememberTextFieldState(initialText = uiState.farewellMessage)
    val currentOnFarewellMessageChange by rememberUpdatedState(onFarewellMessageChange)
    LaunchedEffect(farewellMessageState) {
        snapshotFlow { farewellMessageState.text.toString() }
            .collect { currentOnFarewellMessageChange(it) }
    }

    val deliveryMethodItems =
        listOf(
            RadioGroupItem(
                title = stringResource(R.string.delivery_method_auto_title),
                description = stringResource(R.string.delivery_method_auto_description),
            ),
            RadioGroupItem(
                title = stringResource(R.string.delivery_method_recipient_approval_title),
                description = stringResource(R.string.delivery_method_recipient_approval_description),
            ),
        )

    val processingMethodItems =
        listOf(
            RadioGroupItem(
                title = stringResource(R.string.processing_method_inactive_title),
                description = stringResource(R.string.processing_method_inactive_description),
            ),
            RadioGroupItem(
                title = stringResource(R.string.processing_method_specific_date_title),
                description = stringResource(R.string.processing_method_specific_date_description),
            ),
            RadioGroupItem(
                title = stringResource(R.string.processing_method_recipient_request_title),
                description = stringResource(R.string.processing_method_recipient_request_description),
            ),
        )

    Scaffold(
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.settings_recipient_after_delivery),
                onBackClick = onBack,
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            SectionLabel(text = stringResource(R.string.delivery_method_section_title))
            Spacer(Modifier.height(12.dp))
            RadioGroup(
                items = deliveryMethodItems,
                selectedIndex = uiState.selectedDeliveryMethodIndex,
                onSelectIndex = onSelectDeliveryMethodIndex,
            )

            Spacer(Modifier.height(28.dp))

            SectionLabel(text = stringResource(R.string.processing_method_section_title))
            Spacer(Modifier.height(12.dp))
            RadioGroup(
                items = processingMethodItems,
                selectedIndex = uiState.selectedProcessingMethodIndex,
                onSelectIndex = onSelectProcessingMethodIndex,
            )

            Spacer(Modifier.height(28.dp))

            SectionLabel(text = stringResource(R.string.farewell_message_section_title))
            Spacer(Modifier.height(12.dp))
            AfternoteTextField(
                state = farewellMessageState,
                placeholder = stringResource(R.string.farewell_message_hint),
            )
            Spacer(modifier = Modifier.height(32.dp))
            Divider(thickness = 0.8.dp, color = AfternoteDesign.colors.gray3)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.after_delivery_notice_1),
                style = AfternoteDesign.typography.captionLargeR,
            )
            Text(
                text = stringResource(R.string.after_delivery_notice_2),
                style = AfternoteDesign.typography.captionLargeR,
            )
        }
    }
}

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = AfternoteDesign.typography.bodySmallB,
        color = AfternoteDesign.colors.gray9,
    )
}

@Preview(showBackground = true)
@Composable
private fun DeliveryConditionContentPreview() {
    AfternoteTheme {
        DeliveryConditionContent(
            uiState = DeliveryConditionUiState(),
            onBack = {},
            onSelectDeliveryMethodIndex = {},
            onSelectProcessingMethodIndex = {},
            onFarewellMessageChange = {},
        )
    }
}
