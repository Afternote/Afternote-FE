package com.afternote.feature.setting.presentation.notification.anniversary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.asString
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.modifierextention.bottomBorder
import com.afternote.core.ui.mvi.ObserveSignal
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.shared.component.SettingLoadErrorContent
import kotlinx.coroutines.launch
import java.time.Month

@Composable
internal fun AnniversaryScreen(
    onBack: () -> Unit,
    viewModel: AnniversaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val errorMessage = uiState.errorMessage?.asString()
    ObserveSignal(signal = errorMessage, consumed = AnniversaryIntent.ConsumeError, onIntent = viewModel::onIntent) { message ->
        snackbarScope.launch { snackbarHostState.showSnackbar(message) }
    }
    AnniversaryContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onIntent = viewModel::onIntent,
    )
}

@Composable
internal fun AnniversaryContent(
    uiState: AnniversaryUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onIntent: (AnniversaryIntent) -> Unit,
) {
    var isAdding by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            DetailTopBar(title = stringResource(R.string.setting_anniversary_title), onBackClick = onBack)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AfternoteDesign.colors.gray9)
                }
            }

            uiState.loadFailed -> {
                SettingLoadErrorContent(
                    message = stringResource(R.string.setting_anniversary_load_failed),
                    onRetry = { onIntent(AnniversaryIntent.Retry) },
                    modifier = Modifier.padding(padding),
                )
            }

            else -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.setting_anniversary_description),
                        style = AfternoteDesign.typography.captionLargeR,
                        color = AfternoteDesign.colors.gray6,
                    )
                    Spacer(Modifier.height(16.dp))
                    if (uiState.items.isEmpty()) {
                        Text(
                            text = stringResource(R.string.setting_anniversary_empty),
                            style = AfternoteDesign.typography.bodyBase,
                            color = AfternoteDesign.colors.gray5,
                            modifier = Modifier.padding(vertical = 24.dp).align(Alignment.CenterHorizontally),
                        )
                    }
                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        items(uiState.items, key = AnniversaryItem::id) { item ->
                            AnniversaryRow(
                                item = item,
                                enabled = !uiState.isSubmitting,
                                onDelete = { onIntent(AnniversaryIntent.Delete(item.id)) },
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    if (uiState.receivers.isEmpty()) {
                        Text(
                            text = stringResource(R.string.setting_anniversary_no_receiver),
                            style = AfternoteDesign.typography.captionLargeR,
                            color = AfternoteDesign.colors.gray6,
                        )
                    } else {
                        AfternoteButton(
                            text = stringResource(R.string.setting_anniversary_add),
                            onClick = { isAdding = true },
                            isLoading = uiState.isSubmitting,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
    if (isAdding) {
        AddAnniversaryDialog(
            receivers = uiState.receivers,
            onConfirm = { receiverId, month, day ->
                isAdding = false
                onIntent(AnniversaryIntent.Add(receiverId, month, day))
            },
            onDismiss = { isAdding = false },
        )
    }
}

@Composable
private fun AnniversaryRow(
    item: AnniversaryItem,
    enabled: Boolean,
    onDelete: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .bottomBorder(color = AfternoteDesign.colors.gray3, width = 1.dp)
                .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.receiverName ?: stringResource(R.string.setting_anniversary_unknown_receiver),
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray9,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.setting_anniversary_date, item.month, item.day),
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray6,
        )
        Spacer(Modifier.width(12.dp))
        TextButton(onClick = onDelete, enabled = enabled) {
            Text(
                text = stringResource(R.string.setting_anniversary_delete),
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray6,
            )
        }
    }
}

/** 수신인 하나와 월·일을 고른다. 연도는 받지 않는다 — 매년 그날 알린다. 2월은 29일까지 고를 수 있다. */
@Composable
private fun AddAnniversaryDialog(
    receivers: List<AnniversaryReceiver>,
    onConfirm: (receiverId: Long, month: Int, day: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var receiverId by rememberSaveable { mutableStateOf(receivers.first().id) }
    var month by rememberSaveable { mutableStateOf(1) }
    var day by rememberSaveable { mutableStateOf(1) }
    val maxDay = Month.of(month).maxLength()
    if (day > maxDay) day = maxDay

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AfternoteDesign.colors.white,
        title = {
            Text(
                text = stringResource(R.string.setting_anniversary_add),
                style = AfternoteDesign.typography.bodyLargeB,
                color = AfternoteDesign.colors.gray9,
            )
        },
        text = {
            Column {
                PickerRow(
                    label = stringResource(R.string.setting_anniversary_receiver),
                    selected = receivers.first { it.id == receiverId }.name,
                    options = receivers.map { it.id to it.name },
                    onSelect = { receiverId = it },
                )
                PickerRow(
                    label = stringResource(R.string.setting_anniversary_month),
                    selected = stringResource(R.string.setting_anniversary_month_value, month),
                    options = (1..12).map { it.toLong() to stringResource(R.string.setting_anniversary_month_value, it) },
                    onSelect = { month = it.toInt() },
                )
                PickerRow(
                    label = stringResource(R.string.setting_anniversary_day),
                    selected = stringResource(R.string.setting_anniversary_day_value, day),
                    options = (1..maxDay).map { it.toLong() to stringResource(R.string.setting_anniversary_day_value, it) },
                    onSelect = { day = it.toInt() },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(receiverId, month, day) }) {
                Text(stringResource(R.string.setting_notification_preference_confirm), color = AfternoteDesign.colors.gray9)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.setting_notification_preference_cancel), color = AfternoteDesign.colors.gray6)
            }
        },
    )
}

@Composable
private fun PickerRow(
    label: String,
    selected: String,
    options: List<Pair<Long, String>>,
    onSelect: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.DropdownList) { expanded = true }
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray6,
            modifier = Modifier.weight(1f),
        )
        Box {
            Text(text = selected, style = AfternoteDesign.typography.bodyBase, color = AfternoteDesign.colors.gray9)
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (value, text) ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            expanded = false
                            onSelect(value)
                        },
                    )
                }
            }
        }
    }
}
