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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.TextFieldType
import com.afternote.core.ui.scaffold.FlowStepScaffold
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.onboarding.presentation.R

private val HeaderSpacing = 8.dp

/**
 * 아이디 찾기 1단계 — 이메일 인증.
 *
 * 인증번호 "확인" 이 곧 조회라(`auth/email/find` 가 인증번호를 함께 받는다) 확인에 성공해야
 * "다음" 이 열리고, 결과 화면은 VM 에 담긴 계정을 읽는다.
 */
@Composable
fun FindIdScreen(
    initialEmail: String,
    initialCertificateCode: String,
    isSendingCode: Boolean,
    isVerificationSent: Boolean,
    isSendCodeEnabled: Boolean,
    isVerifyEnabled: Boolean,
    isNextEnabled: Boolean,
    resendCooldownSeconds: Int,
    hasVerificationError: Boolean,
    snackbarHostState: SnackbarHostState,
    onEmailChange: (String) -> Unit,
    onCertificateCodeChange: (String) -> Unit,
    onRequestCode: () -> Unit,
    onVerifyCode: () -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val emailState = rememberTextFieldState(initialEmail)
    val certificateCodeState = rememberTextFieldState(initialCertificateCode)

    // "바뀔 때마다 onEmailChange 호출"을 수행하는 장치가 이 블록이다. TextFieldState 에는 콜백이 없고,
    // LaunchedEffect 의 key(emailState) 는 객체 동일성 비교라 내용(.text) 변이에는 반응하지 않는다
    // (rememberTextFieldState 인스턴스는 화면 수명 내내 동일 → key 만으론 최초 1회 실행 후 침묵).
    // snapshotFlow 가 스냅샷 읽기 추적으로 내용 변이를 리컴포지션 없이 구독해 값이 달라질 때만 흘리고,
    // LaunchedEffect 는 그 수집 코루틴의 수명(화면 이탈 시 자동 해제)만 맡는다. (SignUpScreen 선례)
    // key=emailState 는 재시작 트리거가 아니라 의존성 선언 — remember 덕에 지금은 인스턴스가 안 갈리지만,
    // 갈리는 리팩터링이 오면 옛 구독을 취소하고 새 인스턴스로 갈아타게 하는 보험이다 (Unit 이면 유령 구독).
    LaunchedEffect(emailState) {
        snapshotFlow { emailState.text.toString() }.collect(onEmailChange)
    }
    LaunchedEffect(certificateCodeState) {
        snapshotFlow { certificateCodeState.text.toString() }.collect(onCertificateCodeChange)
    }

    val requestCodeText =
        when {
            isSendingCode -> {
                stringResource(R.string.find_account_code_requesting)
            }

            resendCooldownSeconds > 0 -> {
                stringResource(
                    R.string.find_account_code_resend_cooldown,
                    resendCooldownSeconds,
                )
            }

            isVerificationSent -> {
                stringResource(R.string.find_account_code_resend)
            }

            else -> {
                stringResource(R.string.find_account_code_request)
            }
        }

    FlowStepScaffold(
        topBarTitle = stringResource(R.string.find_id_title),
        actionButtonText = stringResource(R.string.find_account_next),
        onBackClick = onBackClick,
        onActionClick = onNextClick,
        modifier = modifier,
        isActionEnabled = isNextEnabled,
        snackbarHostState = snackbarHostState,
    ) {
        Spacer(modifier = Modifier.height(HeaderSpacing))
        Column(
            modifier = Modifier.imePadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.find_account_verify_email_title),
                style = AfternoteDesign.typography.h1,
                color = AfternoteDesign.colors.gray9,
            )
            Text(
                text = stringResource(R.string.find_account_verify_email_description),
                // 시안 텍스트 스펙 = NanumBarunGothic Regular 12 / 행간 18 → captionLargeR (bodySmallB 는 Bold 14/20 로 불일치)
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray5,
            )

            AfternoteTextField(
                state = emailState,
                type =
                    TextFieldType.Variant7(
                        text = requestCodeText,
                        onClick = onRequestCode,
                        enabled = isSendCodeEnabled,
                    ),
                placeholder = stringResource(R.string.find_account_email_placeholder),
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            )

            // 인증번호 필드의 "확인" 은 발송 이력이 있을 때만 노출 (시안 초기 상태엔 없음).
            AfternoteTextField(
                state = certificateCodeState,
                type =
                    if (isVerificationSent) {
                        TextFieldType.Variant7(
                            text = stringResource(R.string.find_account_code_confirm),
                            onClick = onVerifyCode,
                            enabled = isVerifyEnabled,
                        )
                    } else {
                        TextFieldType.Basic
                    },
                placeholder = stringResource(R.string.find_account_code_placeholder),
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                onImeAction = {
                    if (isVerifyEnabled) onVerifyCode()
                },
            )

            // 인증번호 불일치는 인라인 에러로, 그 외 실패는 스낵바로 나뉜다 (시안 2431-14204).
            if (hasVerificationError) {
                Text(
                    text = stringResource(R.string.find_account_code_mismatch),
                    style = AfternoteDesign.typography.captionLargeB,
                    color = AfternoteDesign.colors.error,
                )
            } else if (isVerificationSent) {
                Text(
                    text = stringResource(R.string.find_account_code_sent),
                    style = AfternoteDesign.typography.captionLargeB,
                    color = AfternoteDesign.colors.b1,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FindIdScreenPreview() {
    AfternoteTheme {
        FindIdScreen(
            initialEmail = "",
            initialCertificateCode = "",
            isSendingCode = false,
            isVerificationSent = false,
            isSendCodeEnabled = false,
            isVerifyEnabled = false,
            isNextEnabled = false,
            resendCooldownSeconds = 0,
            hasVerificationError = false,
            snackbarHostState = remember { SnackbarHostState() },
            onEmailChange = {},
            onCertificateCodeChange = {},
            onRequestCode = {},
            onVerifyCode = {},
            onNextClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, name = "인증번호 전송됨")
@Composable
private fun FindIdScreenCodeSentPreview() {
    AfternoteTheme {
        FindIdScreen(
            initialEmail = "parkchae01@gmail.com",
            initialCertificateCode = "",
            isSendingCode = false,
            isVerificationSent = true,
            isSendCodeEnabled = true,
            isVerifyEnabled = false,
            isNextEnabled = false,
            resendCooldownSeconds = 0,
            hasVerificationError = false,
            snackbarHostState = remember { SnackbarHostState() },
            onEmailChange = {},
            onCertificateCodeChange = {},
            onRequestCode = {},
            onVerifyCode = {},
            onNextClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, name = "인증번호 불일치")
@Composable
private fun FindIdScreenErrorPreview() {
    AfternoteTheme {
        FindIdScreen(
            initialEmail = "parkchae01@gmail.com",
            initialCertificateCode = "123456",
            isSendingCode = false,
            isVerificationSent = true,
            isSendCodeEnabled = true,
            isVerifyEnabled = true,
            isNextEnabled = false,
            resendCooldownSeconds = 0,
            hasVerificationError = true,
            snackbarHostState = remember { SnackbarHostState() },
            onEmailChange = {},
            onCertificateCodeChange = {},
            onRequestCode = {},
            onVerifyCode = {},
            onNextClick = {},
            onBackClick = {},
        )
    }
}
