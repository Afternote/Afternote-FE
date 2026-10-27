package com.afternote.feature.setting.presentation.notification.delegate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.CaptionLabeledTextField
import com.afternote.core.ui.asString
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.mvi.ObserveSignal
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.domain.NotificationDelegate
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.shared.component.SettingLoadErrorContent
import kotlinx.coroutines.launch

@Composable
internal fun DelegateScreen(
    onBack: () -> Unit,
    viewModel: DelegateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()

    val doneMessage = uiState.doneMessage?.asString()
    ObserveSignal(signal = doneMessage, consumed = DelegateIntent.ConsumeDone, onIntent = viewModel::onIntent) { message ->
        snackbarScope.launch { snackbarHostState.showSnackbar(message) }
    }
    val errorMessage = uiState.errorMessage?.asString()
    ObserveSignal(signal = errorMessage, consumed = DelegateIntent.ConsumeError, onIntent = viewModel::onIntent) { message ->
        snackbarScope.launch { snackbarHostState.showSnackbar(message) }
    }

    DelegateContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onIntent = viewModel::onIntent,
    )
}

@Composable
internal fun DelegateContent(
    uiState: DelegateUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onIntent: (DelegateIntent) -> Unit,
) {
    Scaffold(
        topBar = {
            DetailTopBar(title = stringResource(R.string.setting_delegate_title), onBackClick = onBack)
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
                    message = stringResource(R.string.setting_delegate_load_failed),
                    onRetry = { onIntent(DelegateIntent.Retry) },
                    modifier = Modifier.padding(padding),
                )
            }

            else -> {
                DelegateForm(
                    delegate = uiState.delegate,
                    isSubmitting = uiState.isSubmitting,
                    onIntent = onIntent,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun DelegateForm(
    delegate: NotificationDelegate?,
    isSubmitting: Boolean,
    onIntent: (DelegateIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 서버 값이 바뀌면(저장·해제) 입력칸도 그 값으로 다시 연다.
    val nameState = remember(delegate) { TextFieldState(delegate?.name.orEmpty()) }
    val phoneState = remember(delegate) { TextFieldState(delegate?.phone.orEmpty()) }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.setting_delegate_description),
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray6,
        )
        Spacer(Modifier.height(24.dp))
        CaptionLabeledTextField(label = stringResource(R.string.setting_delegate_name), state = nameState)
        Spacer(Modifier.height(16.dp))
        CaptionLabeledTextField(
            label = stringResource(R.string.setting_delegate_phone),
            state = phoneState,
            keyboardType = KeyboardType.Phone,
        )
        Spacer(Modifier.height(32.dp))
        AfternoteButton(
            text = stringResource(R.string.setting_delegate_save),
            onClick = { onIntent(DelegateIntent.Save(nameState.text.toString(), phoneState.text.toString())) },
            isLoading = isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
        if (delegate != null) {
            TextButton(
                onClick = { onIntent(DelegateIntent.Remove) },
                enabled = !isSubmitting,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.setting_delegate_remove),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray6,
                )
            }
        }
    }
}
