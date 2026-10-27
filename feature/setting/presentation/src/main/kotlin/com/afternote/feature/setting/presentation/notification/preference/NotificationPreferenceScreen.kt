package com.afternote.feature.setting.presentation.notification.preference

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.asString
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.mvi.ObserveFlag
import com.afternote.core.ui.mvi.ObserveSignal
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.domain.NotificationPreferences
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.notification.component.LabeledSwitchRow
import com.afternote.feature.setting.presentation.shared.component.SettingLoadErrorContent
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun NotificationPreferenceScreen(
    onBack: () -> Unit,
    viewModel: NotificationPreferenceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val savedMessage = stringResource(R.string.setting_notification_preference_saved)

    ObserveFlag(
        raised = uiState.isSaved,
        consumed = NotificationPreferenceIntent.ConsumeSaved,
        onIntent = viewModel::onIntent,
    ) {
        snackbarScope.launch { snackbarHostState.showSnackbar(savedMessage) }
    }
    val errorMessage = uiState.errorMessage?.asString()
    ObserveSignal(
        signal = errorMessage,
        consumed = NotificationPreferenceIntent.ConsumeError,
        onIntent = viewModel::onIntent,
    ) { message ->
        snackbarScope.launch { snackbarHostState.showSnackbar(message) }
    }

    NotificationPreferenceContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onIntent = viewModel::onIntent,
    )
}

@Composable
internal fun NotificationPreferenceContent(
    uiState: NotificationPreferenceUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onIntent: (NotificationPreferenceIntent) -> Unit,
) {
    Scaffold(
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.setting_notification_preference_title),
                onBackClick = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
    ) { padding ->
        val form = uiState.form
        when {
            form != null -> {
                PreferenceForm(
                    form = form,
                    isSaving = uiState.isSaving,
                    onIntent = onIntent,
                    modifier = Modifier.padding(padding),
                )
            }

            uiState.loadFailed -> {
                SettingLoadErrorContent(
                    message = stringResource(R.string.setting_notification_preference_load_failed),
                    onRetry = { onIntent(NotificationPreferenceIntent.Retry) },
                    modifier = Modifier.padding(padding),
                )
            }

            else -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AfternoteDesign.colors.gray9)
                }
            }
        }
    }
}

@Composable
private fun PreferenceForm(
    form: NotificationPreferences,
    isSaving: Boolean,
    onIntent: (NotificationPreferenceIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPickingTime by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        LabeledSwitchRow(
            label = stringResource(R.string.setting_notification_preference_enabled),
            checked = form.enabled,
            onCheckedChange = { onIntent(NotificationPreferenceIntent.ChangeEnabled(it)) },
            enabled = !isSaving,
        )
        Text(
            text = stringResource(R.string.setting_notification_preference_description),
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray6,
        )

        SectionLabel(stringResource(R.string.setting_notification_preference_time))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = form.enabled && !isSaving, role = Role.Button) { isPickingTime = true }
                    .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = form.time.toDisplayText(),
                style = AfternoteDesign.typography.bodyLargeB,
                color = if (form.enabled) AfternoteDesign.colors.gray9 else AfternoteDesign.colors.gray5,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.setting_notification_preference_change),
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray6,
            )
        }

        SectionLabel(stringResource(R.string.setting_notification_preference_weekdays))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DayOfWeek.entries.forEach { day ->
                WeekdayChip(
                    day = day,
                    selected = day in form.weekdays,
                    enabled = form.enabled && !isSaving,
                    onClick = { onIntent(NotificationPreferenceIntent.ToggleWeekday(day)) },
                )
            }
        }

        SectionLabel(stringResource(R.string.setting_notification_preference_goal))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton(
                text = "−",
                enabled = !isSaving && form.weeklyGoal > 1,
                onClick = { onIntent(NotificationPreferenceIntent.ChangeWeeklyGoal(form.weeklyGoal - 1)) },
            )
            Text(
                text = stringResource(R.string.setting_notification_preference_goal_value, form.weeklyGoal),
                style = AfternoteDesign.typography.bodyLargeB,
                color = AfternoteDesign.colors.gray9,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            StepButton(
                text = "+",
                enabled = !isSaving && form.weeklyGoal < 7,
                onClick = { onIntent(NotificationPreferenceIntent.ChangeWeeklyGoal(form.weeklyGoal + 1)) },
            )
        }
        Text(
            text = stringResource(R.string.setting_notification_preference_goal_description),
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray6,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.height(32.dp))
        AfternoteButton(
            text = stringResource(R.string.setting_notification_preference_save),
            onClick = { onIntent(NotificationPreferenceIntent.Save) },
            isLoading = isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (isPickingTime) {
        TimePickerDialog(
            initial = form.time,
            onConfirm = { time ->
                isPickingTime = false
                onIntent(NotificationPreferenceIntent.ChangeTime(time))
            },
            onDismiss = { isPickingTime = false },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = AfternoteDesign.typography.captionLargeR,
        color = AfternoteDesign.colors.gray6,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun WeekdayChip(
    day: DayOfWeek,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val label = day.getDisplayName(TextStyle.NARROW, Locale.KOREAN)
    Box(
        modifier =
            Modifier
                .size(40.dp)
                .border(
                    width = 1.dp,
                    color = if (selected) AfternoteDesign.colors.gray9 else AfternoteDesign.colors.gray3,
                    shape = CircleShape,
                ).clickable(enabled = enabled, role = Role.Checkbox, onClick = onClick)
                .semantics { this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = AfternoteDesign.typography.bodyBase,
            color =
                when {
                    !enabled -> AfternoteDesign.colors.gray5
                    selected -> AfternoteDesign.colors.gray9
                    else -> AfternoteDesign.colors.gray6
                },
        )
    }
}

@Composable
private fun StepButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(36.dp)
                .border(1.dp, AfternoteDesign.colors.gray3, CircleShape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AfternoteDesign.typography.bodyLargeB,
            color = if (enabled) AfternoteDesign.colors.gray9 else AfternoteDesign.colors.gray5,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.setting_notification_preference_confirm), color = AfternoteDesign.colors.gray9)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.setting_notification_preference_cancel), color = AfternoteDesign.colors.gray6)
            }
        },
        text = { TimePicker(state = state) },
        containerColor = AfternoteDesign.colors.white,
    )
}

/** 「오후 6:00」 처럼 보여 준다. 서버 계약은 24시간 `HH:mm` 이고 표시만 바꾼다. */
private fun LocalTime.toDisplayText(): String = format(DISPLAY_TIME)

private val DISPLAY_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
