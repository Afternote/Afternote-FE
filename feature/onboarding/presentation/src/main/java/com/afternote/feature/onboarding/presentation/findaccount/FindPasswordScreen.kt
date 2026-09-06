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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.TextFieldType
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import com.afternote.core.ui.scaffold.FlowStepScaffold
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.onboarding.presentation.R

private val HeaderSpacing = 8.dp

/**
 * 비밀번호 찾기 1단계 — 이메일 인증 (시안 `2431:14299` · `2383:16680` · `2383:16667`).
 *
 * 아이디 찾기([FindIdScreen])와 달리 인증번호 필드에 인라인 "확인" 이 없다. 서버가 인증번호를
 * 검증하면서 삭제하므로 여기서 확인해 버리면 최종 제출(`auth/password/find`)에 쓸 코드가 남지
 * 않는다 — 시안도 그렇게 그려져 있다. "다음" 은 자릿수만 보고 열리고, 코드의 진위는 비밀번호
 * 변경 화면의 제출이 판정한다.
 *
 * @param showSocialAccountBlockedPopup 차단 팝업(시안 `2383:16667`)을 띄울지 — 소셜로 가입해
 *   로컬 비밀번호가 없는 계정이라 이 흐름을 쓸 수 없다는 안내다(서버 code 1702).
 */
@Composable
fun FindPasswordScreen(
    initialEmail: String,
    initialCertificateCode: String,
    isSendingCode: Boolean,
    isVerificationSent: Boolean,
    isSendCodeEnabled: Boolean,
    isNextEnabled: Boolean,
    resendCooldownSeconds: Int,
    showSocialAccountBlockedPopup: Boolean,
    snackbarHostState: SnackbarHostState,
    onEmailChange: (String) -> Unit,
    onCertificateCodeChange: (String) -> Unit,
    onRequestCode: () -> Unit,
    onSocialAccountBlockedConfirm: () -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val emailState = rememberTextFieldState(initialEmail)
    val certificateCodeState = rememberTextFieldState(initialCertificateCode)

    // TextFieldState 에는 콜백이 없고 LaunchedEffect 의 key 는 객체 동일성 비교라 내용 변이에
    // 반응하지 않는다. snapshotFlow 가 스냅샷 읽기 추적으로 내용 변이를 구독한다 (FindIdScreen 선례).
    LaunchedEffect(emailState) {
        snapshotFlow { emailState.text.toString() }.collect(onEmailChange)
    }
    LaunchedEffect(certificateCodeState) {
        snapshotFlow { certificateCodeState.text.toString() }.collect(onCertificateCodeChange)
    }

    val requestCodeText =
        when {
            isSendingCode -> {
                stringResource(R.string.onboarding_find_account_code_requesting)
            }

            resendCooldownSeconds > 0 -> {
                stringResource(
                    R.string.onboarding_find_account_code_resend_cooldown,
                    resendCooldownSeconds,
                )
            }

            isVerificationSent -> {
                stringResource(R.string.onboarding_find_account_code_resend)
            }

            else -> {
                stringResource(R.string.onboarding_find_account_code_request)
            }
        }

    FlowStepScaffold(
        topBarTitle = stringResource(R.string.onboarding_find_password_title),
        actionButtonText = stringResource(R.string.onboarding_find_account_next),
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
                text = stringResource(R.string.onboarding_find_account_verify_email_title),
                style = AfternoteDesign.typography.h1,
                color = AfternoteDesign.colors.gray9,
            )
            Text(
                text = stringResource(R.string.onboarding_find_account_verify_email_description),
                // 시안 텍스트 스펙 = NanumBarunGothic Regular 12 / 행간 18 → captionLargeR
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
                placeholder = stringResource(R.string.onboarding_find_account_email_placeholder),
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            )

            AfternoteTextField(
                state = certificateCodeState,
                placeholder = stringResource(R.string.onboarding_find_account_code_placeholder),
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                onImeAction = {
                    if (isNextEnabled) onNextClick()
                },
            )

            if (isVerificationSent) {
                Text(
                    text = stringResource(R.string.onboarding_find_account_code_sent),
                    style = AfternoteDesign.typography.captionLargeB,
                    color = AfternoteDesign.colors.b1,
                )
            }
        }
    }

    if (showSocialAccountBlockedPopup) {
        // 확인 외의 선택지가 없는 안내다 — 바깥 탭·뒤로가기도 같은 소비로 닫는다.
        Popup(
            type = PopupType.Default,
            message = stringResource(R.string.onboarding_find_password_social_blocked),
            onConfirm = onSocialAccountBlockedConfirm,
            onDismiss = onSocialAccountBlockedConfirm,
        )
    }
}
