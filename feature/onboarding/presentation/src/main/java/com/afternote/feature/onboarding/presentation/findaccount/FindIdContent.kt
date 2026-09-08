package com.afternote.feature.onboarding.presentation.findaccount

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.TextFieldType
import com.afternote.core.ui.scaffold.FlowStepScaffold
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.onboarding.presentation.OnboardingFailureDisplay
import com.afternote.feature.onboarding.presentation.R
import com.afternote.feature.onboarding.presentation.toDisplay

private val HeaderSpacing = 8.dp

/** 상태를 그리는 렌더 계약. 별도 Screen 파일이 수명과 신호 소비를 담당하므로 모듈 내부에 공개한다. */
@Composable
internal fun FindIdContent(
    state: FindIdUiState,
    onIntent: (FindIdIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val emailState = rememberTextFieldState(state.email)
    val certificateCodeState = rememberTextFieldState(state.certificateCode)

    // "바뀔 때마다 UpdateEmail Intent 를 보낸다"를 수행하는 장치가 이 블록이다. TextFieldState 에는 콜백이 없고,
    // LaunchedEffect 의 key(emailState) 는 객체 동일성 비교라 내용(.text) 변이에는 반응하지 않는다
    // (rememberTextFieldState 인스턴스는 화면 수명 내내 동일 → key 만으론 최초 1회 실행 후 침묵).
    // snapshotFlow 가 스냅샷 읽기 추적으로 내용 변이를 리컴포지션 없이 구독해 값이 달라질 때만 흘리고,
    // LaunchedEffect 는 그 수집 코루틴의 수명(화면 이탈 시 자동 해제)만 맡는다. (SignUpScreen 선례)
    // key=emailState 는 재시작 트리거가 아니라 의존성 선언 — remember 덕에 지금은 인스턴스가 안 갈리지만,
    // 갈리는 리팩터링이 오면 옛 구독을 취소하고 새 인스턴스로 갈아타게 하는 보험이다 (Unit 이면 유령 구독).
    LaunchedEffect(emailState) {
        snapshotFlow { emailState.text.toString() }.collect { onIntent(FindIdIntent.UpdateEmail(it)) }
    }
    LaunchedEffect(certificateCodeState) {
        snapshotFlow { certificateCodeState.text.toString() }.collect { onIntent(FindIdIntent.UpdateCertificateCode(it)) }
    }

    val requestCodeText =
        when {
            state.isSendingCode -> {
                stringResource(R.string.onboarding_find_account_code_requesting)
            }

            state.resendCooldownSeconds > 0 -> {
                stringResource(
                    R.string.onboarding_find_account_code_resend_cooldown,
                    state.resendCooldownSeconds,
                )
            }

            state.isVerificationSent -> {
                stringResource(R.string.onboarding_find_account_code_resend)
            }

            else -> {
                stringResource(R.string.onboarding_find_account_code_request)
            }
        }

    FlowStepScaffold(
        topBarTitle = stringResource(R.string.onboarding_find_id_title),
        actionButtonText = stringResource(R.string.onboarding_find_account_next),
        onBackClick = onBackClick,
        onActionClick = onNextClick,
        modifier = modifier,
        isActionEnabled = state.isNextEnabled,
        snackbarHostState = snackbarHostState,
    ) {
        Spacer(modifier = Modifier.height(HeaderSpacing))
        Column(
            modifier = Modifier.imePadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_find_account_verify_email_title),
                style = AfternoteDesign.typography.h1,
                color = AfternoteDesign.colors.gray9,
            )
            Text(
                text = stringResource(R.string.onboarding_find_account_verify_email_description),
                // 시안 텍스트 스펙 = NanumBarunGothic Regular 12 / 행간 18 → captionLargeR (bodySmallB 는 Bold 14/20 로 불일치)
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray5,
            )

            AfternoteTextField(
                state = emailState,
                type =
                    TextFieldType.Variant7(
                        text = requestCodeText,
                        onClick = { onIntent(FindIdIntent.RequestVerificationCode) },
                        enabled = state.isSendCodeEnabled,
                    ),
                placeholder = stringResource(R.string.onboarding_find_account_email_placeholder),
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            )

            // 인증번호 필드의 "확인" 은 발송 이력이 있을 때만 노출 (시안 초기 상태엔 없음).
            AfternoteTextField(
                state = certificateCodeState,
                type =
                    if (state.isVerificationSent) {
                        TextFieldType.Variant7(
                            text = stringResource(R.string.onboarding_find_account_code_confirm),
                            onClick = { onIntent(FindIdIntent.VerifyCode) },
                            enabled = state.isVerifyEnabled,
                        )
                    } else {
                        TextFieldType.Basic
                    },
                placeholder = stringResource(R.string.onboarding_find_account_code_placeholder),
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                onImeAction = {
                    if (state.isVerifyEnabled) onIntent(FindIdIntent.VerifyCode)
                },
            )

            // 인증번호 불일치는 인라인 에러로, 그 외 실패는 스낵바로 나뉜다 (시안 2431-14204).
            if ((state.failure.toDisplay() == OnboardingFailureDisplay.VerificationInline)) {
                Text(
                    text = stringResource(R.string.onboarding_find_account_code_mismatch),
                    style = AfternoteDesign.typography.captionLargeB,
                    color = AfternoteDesign.colors.error,
                )
            } else if (state.isVerificationSent) {
                Text(
                    text = stringResource(R.string.onboarding_find_account_code_sent),
                    style = AfternoteDesign.typography.captionLargeB,
                    color = AfternoteDesign.colors.b1,
                )
            }
        }
    }
}
