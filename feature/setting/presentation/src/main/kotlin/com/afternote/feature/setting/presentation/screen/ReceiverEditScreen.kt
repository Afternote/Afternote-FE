package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.viewmodel.ReceiverEditEvent
import com.afternote.feature.setting.presentation.viewmodel.ReceiverEditUiState
import com.afternote.feature.setting.presentation.viewmodel.ReceiverEditViewModel

@Composable
fun ReceiverEditScreen(
    onBackClick: () -> Unit,
    onEditSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReceiverEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnEditSuccess by rememberUpdatedState(onEditSuccess)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ReceiverEditEvent.EditSuccess -> currentOnEditSuccess()
            }
        }
    }

    ReceiverEditContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onRegister = viewModel::update,
        modifier = modifier,
    )
}

@Composable
internal fun ReceiverEditContent(
    uiState: ReceiverEditUiState,
    onBackClick: () -> Unit,
    onRegister: (name: String, relation: String, phone: String, email: String, message: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val receiver = uiState.receiver
    if (receiver != null) {
        ReceiverRegisterContent(
            title = "수신자 수정",
            actionText = "수정",
            isLoading = uiState.isSaving,
            errorMessage = uiState.errorMessage,
            onBackClick = onBackClick,
            onRegister = onRegister,
            modifier = modifier,
            initialName = receiver.name,
            initialRelation = receiver.relation,
            initialPhone = receiver.phone.orEmpty(),
            initialEmail = receiver.email.orEmpty(),
            initialMessage = receiver.message.orEmpty(),
        )
    } else {
        Scaffold(
            modifier = modifier,
            topBar = {
                DetailTopBar(
                    title = "수신자 수정",
                    onBackClick = onBackClick,
                )
            },
        ) { innerPadding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = AfternoteDesign.colors.gray9)
                } else {
                    Text(
                        text = uiState.errorMessage.orEmpty(),
                        style = AfternoteDesign.typography.bodyBase,
                        color = AfternoteDesign.colors.error,
                    )
                }
            }
        }
    }
}
