package com.afternote.feature.receiver.presentation.recordsbox

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.CaptionLabeledTextField
import com.afternote.core.ui.ProfileImagePicker
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.receiver.presentation.R

@Composable
internal fun SenderRegistrationScreenContent(
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

            // 발신자 온보딩(OnboardingProfileScreen) 과 동일한 ProfileImagePicker 재사용.
            // TODO(#215): 이미지 선택 picker 연결은 본 PR 범위 밖 — 디자인 확정 시 onPickClick wire-up.
            ProfileImagePicker(
                onPickClick = { /* TODO(#215): photo picker */ },
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
