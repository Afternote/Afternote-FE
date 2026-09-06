package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.model.delivery.DeliveryConditionType
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.component.RadioGroup
import com.afternote.feature.setting.presentation.component.RadioGroupItem
import com.afternote.feature.setting.presentation.viewmodel.DeliveryConditionError
import com.afternote.feature.setting.presentation.viewmodel.DeliveryConditionUiState
import com.afternote.feature.setting.presentation.viewmodel.DeliveryConditionViewModel

@Composable
fun DeliveryConditionScreen(
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    onLastGreetingEditClick: () -> Unit,
    viewModel: DeliveryConditionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnSaveSuccess by rememberUpdatedState(onSaveSuccess)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshOnReturn()
    }

    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect { currentOnSaveSuccess() }
    }

    DeliveryConditionContent(
        uiState = uiState,
        onBack = onBack,
        onConditionTypeSelect = viewModel::onConditionTypeSelected,
        onLastGreetingEditClick = onLastGreetingEditClick,
        onSave = viewModel::onSave,
    )
}

@Composable
private fun DeliveryConditionContent(
    uiState: DeliveryConditionUiState,
    onBack: () -> Unit,
    onConditionTypeSelect: (Int) -> Unit,
    onLastGreetingEditClick: () -> Unit,
    onSave: () -> Unit,
) {
    val processingMethodItems =
        listOf(
            RadioGroupItem(
                title = stringResource(R.string.processing_method_inactive_title),
                description = stringResource(R.string.processing_method_inactive_description),
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
                actions = {
                    val isSaveEnabled = uiState.isInitialized && !uiState.isLoading && !uiState.isSaving
                    TextButton(onClick = onSave, enabled = isSaveEnabled) {
                        Text(
                            text = stringResource(R.string.delivery_condition_save),
                            style = AfternoteDesign.typography.bodySmallB,
                            color =
                                if (isSaveEnabled) {
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
            val errorMessageRes =
                when (uiState.error) {
                    DeliveryConditionError.LOAD_FAILED -> R.string.delivery_condition_load_error
                    DeliveryConditionError.SAVE_FAILED -> R.string.delivery_condition_save_error
                    null -> null
                }
            if (errorMessageRes != null) {
                Text(
                    text = stringResource(errorMessageRes),
                    style = AfternoteDesign.typography.bodySmallR,
                    color = AfternoteDesign.colors.error,
                )
                Spacer(Modifier.height(16.dp))
            }
            SectionLabel(text = stringResource(R.string.processing_method_section_title))
            Spacer(Modifier.height(28.dp))
            RadioGroup(
                items = processingMethodItems,
                selectedIndex = if (uiState.conditionType == DeliveryConditionType.INACTIVITY) 0 else 1,
                onSelectIndex = onConditionTypeSelect,
            )

            Spacer(Modifier.height(32.dp))
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onLastGreetingEditClick),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.last_greeting_edit_section_title),
                    style = AfternoteDesign.typography.bodyBase,
                    color = AfternoteDesign.colors.gray9,
                )
                Spacer(Modifier.weight(1f))
                Image(
                    painter = painterResource(R.drawable.ic_right_arrow),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.height(32.dp))
            HorizontalDivider(
                thickness = 0.8.dp,
                color = AfternoteDesign.colors.gray3,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.after_delivery_notice_condition),
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray9,
            )
            Text(
                text = stringResource(R.string.after_delivery_notice_safety),
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray9,
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
            onConditionTypeSelect = {},
            onLastGreetingEditClick = {},
            onSave = {},
        )
    }
}
