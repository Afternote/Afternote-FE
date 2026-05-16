package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.component.DatePickerBottomSheet
import com.afternote.feature.setting.presentation.component.RadioGroup
import com.afternote.feature.setting.presentation.component.RadioGroupItem
import com.afternote.feature.setting.presentation.viewmodel.DeliveryConditionUiState
import com.afternote.feature.setting.presentation.viewmodel.DeliveryConditionViewModel
import com.afternote.feature.setting.presentation.viewmodel.ProcessingConditionOption
import java.time.format.DateTimeFormatter

@Composable
fun DeliveryConditionScreen(
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: DeliveryConditionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isFormValid =
        when (uiState.processingOption) {
            ProcessingConditionOption.INACTIVITY -> true
            ProcessingConditionOption.SPECIFIC_DATE -> uiState.specificDate != null
            ProcessingConditionOption.RECIPIENT_REQUEST -> false
        }

    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect { onSaveSuccess() }
    }

    DeliveryConditionContent(
        uiState = uiState,
        isFormValid = isFormValid,
        onBack = onBack,
        onSelectDeliveryMethodIndex = viewModel::onSelectDeliveryMethodIndex,
        onConditionTypeSelected = viewModel::onConditionTypeSelected,
        onDateSelected = viewModel::onDateSelected,
        onDatePickerToggle = viewModel::onDatePickerToggle,
        onFarewellMessageChange = viewModel::onFarewellMessageChange,
        onSave = viewModel::onSave,
    )
}

@Composable
private fun DeliveryConditionContent(
    uiState: DeliveryConditionUiState,
    isFormValid: Boolean,
    onBack: () -> Unit,
    onSelectDeliveryMethodIndex: (Int) -> Unit,
    onConditionTypeSelected: (ProcessingConditionOption) -> Unit,
    onDateSelected: (java.time.LocalDate) -> Unit,
    onDatePickerToggle: (Boolean) -> Unit,
    onFarewellMessageChange: (String) -> Unit,
    onSave: () -> Unit,
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

    val processingMethodIndex =
        when (uiState.processingOption) {
            ProcessingConditionOption.INACTIVITY -> 0
            ProcessingConditionOption.SPECIFIC_DATE -> 1
            ProcessingConditionOption.RECIPIENT_REQUEST -> 2
        }

    Scaffold(
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.settings_recipient_after_delivery),
                onBackClick = onBack,
                actions = {
                    TextButton(
                        onClick = onSave,
                        enabled = isFormValid && !uiState.isSaving,
                    ) {
                        Text(
                            text = "등록",
                            style = AfternoteDesign.typography.bodySmallB,
                            color =
                                if (isFormValid && !uiState.isSaving) {
                                    AfternoteDesign.colors.gray9
                                } else {
                                    AfternoteDesign.colors.gray2
                                },
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
            SectionLabel(text = stringResource(R.string.delivery_method_section_title))
            Spacer(Modifier.height(12.dp))
            RadioGroup(
                items = deliveryMethodItems,
                selectedIndex = uiState.selectedDeliveryMethodIndex,
                onSelectIndex = onSelectDeliveryMethodIndex,
            )

            Spacer(Modifier.height(28.dp))

            SectionLabel(text = stringResource(R.string.processing_method_section_title))

            if (uiState.processingOption == ProcessingConditionOption.SPECIFIC_DATE && uiState.specificDate != null) {
                Spacer(Modifier.height(4.dp))
                val dateStr = uiState.specificDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                Text(
                    text =
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = AfternoteDesign.colors.b1)) {
                                append(dateStr)
                            }
                            withStyle(SpanStyle(color = AfternoteDesign.colors.gray6)) {
                                append("에 전달된 내용입니다.")
                            }
                        },
                    style = AfternoteDesign.typography.captionLargeR,
                )
            }

            Spacer(Modifier.height(12.dp))
            RadioGroup(
                items = processingMethodItems,
                selectedIndex = processingMethodIndex,
                onSelectIndex = { index ->
                    val option =
                        when (index) {
                            0 -> ProcessingConditionOption.INACTIVITY
                            1 -> ProcessingConditionOption.SPECIFIC_DATE
                            2 -> ProcessingConditionOption.RECIPIENT_REQUEST
                            else -> ProcessingConditionOption.INACTIVITY
                        }
                    onConditionTypeSelected(option)
                },
            )

            Spacer(Modifier.height(28.dp))

            SectionLabel(text = stringResource(R.string.farewell_message_section_title))
            Spacer(Modifier.height(12.dp))
            AfternoteTextField(
                state = farewellMessageState,
                placeholder = stringResource(R.string.farewell_message_hint),
            )
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(thickness = 0.8.dp, color = AfternoteDesign.colors.gray3)
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

    if (uiState.isDatePickerVisible) {
        DatePickerBottomSheet(
            onDismiss = { onDatePickerToggle(false) },
            onDateSelected = onDateSelected,
            initialDate = uiState.specificDate,
        )
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
            isFormValid = true,
            onBack = {},
            onSelectDeliveryMethodIndex = {},
            onConditionTypeSelected = {},
            onDateSelected = {},
            onDatePickerToggle = {},
            onFarewellMessageChange = {},
            onSave = {},
        )
    }
}
