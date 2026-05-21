package com.afternote.feature.afternote.presentation.receiver.recordsbox

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.afternote.core.ui.CaptionLabeledTextField
import com.afternote.core.ui.ObserveAsEvents
import com.afternote.core.ui.ProfileImage
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.presentation.R

/**
 * 발신자 등록 화면(15·16) — FAB 에서 진입하는 이름 입력 화면 (이슈 #215).
 *
 * 이름이 공백이 아닐 때만 "발신자 등록하기" 버튼 활성화 (15 비활성 → 16 활성).
 * 등록 완료 시 받은 기록함으로 pop, 카드가 추가된 채로 노출된다.
 */
@Composable
fun SenderRegistrationScreen(
    onBackClick: () -> Unit,
    onRegistered: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SenderRegistrationViewModel = hiltViewModel(),
) {
    val nameState = rememberTextFieldState()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            SenderRegistrationEvent.Registered -> onRegistered()
        }
    }

    SenderRegistrationScreenContent(
        nameState = nameState,
        onBackClick = onBackClick,
        onSubmitClick = { viewModel.submit(nameState.text.toString()) },
        modifier = modifier,
    )
}

@Composable
private fun SenderRegistrationScreenContent(
    nameState: TextFieldState,
    onBackClick: () -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSubmitEnabled =
        nameState.text
            .toString()
            .trim()
            .isNotEmpty()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.receiver_sender_registration_title),
                onBackClick = onBackClick,
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .imePadding()
                    .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(39.dp))

            // 발신자 온보딩(OnboardingProfileScreen) 과 동일한 ProfileImage 재사용.
            // TODO(#215): 이미지 선택 picker 연결은 본 PR 범위 밖 — 디자인 확정 시 onClick wire-up.
            ProfileImage(
                onClick = { /* TODO(#215): photo picker */ },
                isEditable = true,
            )

            Spacer(modifier = Modifier.height(56.dp))

            CaptionLabeledTextField(
                label = stringResource(R.string.receiver_sender_registration_name_label),
                state = nameState,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(56.dp))

            AfternoteButton(
                text = stringResource(R.string.receiver_sender_registration_submit),
                onClick = onSubmitClick,
                type =
                    if (isSubmitEnabled) {
                        AfternoteButtonType.Default
                    } else {
                        AfternoteButtonType.Un
                    },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SenderRegistrationScreenEmptyPreview() {
    AfternoteTheme {
        SenderRegistrationScreenContent(
            nameState = rememberTextFieldState(),
            onBackClick = {},
            onSubmitClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SenderRegistrationScreenFilledPreview() {
    AfternoteTheme {
        SenderRegistrationScreenContent(
            nameState = rememberTextFieldState("Text Field"),
            onBackClick = {},
            onSubmitClick = {},
        )
    }
}
