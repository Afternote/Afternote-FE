package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.model.delivery.DeliveryConditionType
import com.afternote.core.model.delivery.DeliveryContentType
import com.afternote.core.model.delivery.InactivityPeriod
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
    onSaveSuccess: () -> Unit,
    viewModel: DeliveryConditionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnSaveSuccess by rememberUpdatedState(onSaveSuccess)

    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect { currentOnSaveSuccess() }
    }

    DeliveryConditionContent(
        uiState = uiState,
        onBack = onBack,
        onContentTypeSelect = viewModel::onContentTypeSelected,
        onConditionTypeSelect = viewModel::onConditionTypeSelected,
        onInactivityPeriodSelect = viewModel::onInactivityPeriodSelected,
        onSave = viewModel::onSave,
    )
}

@Composable
private fun DeliveryConditionContent(
    uiState: DeliveryConditionUiState,
    onBack: () -> Unit,
    onContentTypeSelect: (Int) -> Unit,
    onConditionTypeSelect: (Int) -> Unit,
    onInactivityPeriodSelect: (Int) -> Unit,
    onSave: () -> Unit,
) {
    val contentTypeItems =
        listOf(
            RadioGroupItem(title = stringResource(R.string.delivery_content_time_letter), description = ""),
            RadioGroupItem(title = stringResource(R.string.delivery_content_afternote), description = ""),
            RadioGroupItem(title = stringResource(R.string.delivery_content_daily_question), description = ""),
            RadioGroupItem(title = stringResource(R.string.delivery_content_diary), description = ""),
            RadioGroupItem(title = stringResource(R.string.delivery_content_deep_thought), description = ""),
        )
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
    val inactivityPeriodItems =
        listOf(
            RadioGroupItem(title = stringResource(R.string.inactivity_period_three_months), description = ""),
            RadioGroupItem(title = stringResource(R.string.inactivity_period_six_months), description = ""),
            RadioGroupItem(title = stringResource(R.string.inactivity_period_one_year), description = ""),
        )

    Scaffold(
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.settings_recipient_after_delivery),
                onBackClick = onBack,
                actions = {
                    TextButton(onClick = onSave, enabled = !uiState.isLoading && !uiState.isSaving) {
                        Text(
                            text = stringResource(R.string.delivery_condition_save),
                            style = AfternoteDesign.typography.bodySmallB,
                            color = AfternoteDesign.colors.gray9,
                        )
                    }
                },
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            SectionLabel(text = stringResource(R.string.delivery_content_section_title))
            Spacer(Modifier.height(12.dp))
            RadioGroup(
                items = contentTypeItems,
                selectedIndex = DeliveryContentType.entries.indexOf(uiState.selectedContentType),
                onSelectIndex = onContentTypeSelect,
            )
            Spacer(Modifier.height(28.dp))
            SectionLabel(text = stringResource(R.string.delivery_method_section_title))
            Spacer(Modifier.height(12.dp))
            RadioGroup(
                items = deliveryMethodItems,
                selectedIndex = if (uiState.conditionType == DeliveryConditionType.INACTIVITY) 0 else 1,
                onSelectIndex = onConditionTypeSelect,
            )

            if (uiState.conditionType == DeliveryConditionType.INACTIVITY) {
                Spacer(Modifier.height(28.dp))
                SectionLabel(text = stringResource(R.string.inactivity_period_section_title))
                Spacer(Modifier.height(12.dp))
                RadioGroup(
                    items = inactivityPeriodItems,
                    selectedIndex =
                        when (uiState.inactivityPeriod) {
                            InactivityPeriod.THREE_MONTHS -> 0
                            InactivityPeriod.SIX_MONTHS -> 1
                            InactivityPeriod.ONE_YEAR -> 2
                        },
                    onSelectIndex = onInactivityPeriodSelect,
                )
            }
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
            onContentTypeSelect = {},
            onConditionTypeSelect = {},
            onInactivityPeriodSelect = {},
            onSave = {},
        )
    }
}
