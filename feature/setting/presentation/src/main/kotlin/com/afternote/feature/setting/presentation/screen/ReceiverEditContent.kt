package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.afternote.core.ui.asString
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.viewmodel.ReceiverEditUiState

/**
 * 수신자 수정 화면의 상태 없는 본문.
 *
 * ViewModel·수정 성공 이벤트는 [ReceiverEditScreen] 이 들고, 이 함수는 넘겨받은 상태만 그린다.
 */
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
            isPhoneRequired = true,
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
                        text = uiState.errorMessage?.asString().orEmpty(),
                        style = AfternoteDesign.typography.bodyBase,
                        color = AfternoteDesign.colors.error,
                    )
                }
            }
        }
    }
}
