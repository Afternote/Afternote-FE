package com.afternote.feature.onboarding.presentation.findaccount

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.TextFieldType
import com.afternote.core.ui.asString
import com.afternote.core.ui.mvi.ObserveSignal
import com.afternote.core.ui.scaffold.FlowStepScaffold
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.onboarding.presentation.R
import kotlinx.coroutines.launch

private val HeaderSpacing = 8.dp

/**
 * 아이디 찾기 1단계 — 이메일 인증. stateful 층.
 *
 * ViewModel 은 `Route.Onboarding` 그래프 스코프 공유라 여기서 만들지 않고 받는다
 * (결과 화면·비밀번호 찾기가 같은 인스턴스를 이어받는다).
 *
 * 인증번호 무효는 시안상 인라인 문구라 [FindIdUiState.hasVerificationError] 로 화면이 직접 그리고,
 * 그 밖의 실패([FindIdUiState.errorMessage])만 스낵바로 나른다.
 */
@Composable
fun FindIdScreen(
    viewModel: FindIdViewModel,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveSignal(
        // VM 이 UiText 로 폴백까지 확정해 두므로 빈 문구가 도달하지 않는다.
        signal = state.errorMessage?.asString(),
        consumed = FindIdIntent.ConsumeError,
        onIntent = viewModel::onIntent,
    ) { message ->
        // 소비가 곧바로 신호를 되돌려 effect 를 재시작시키므로, 표출은 effect 밖 스코프에 맡긴다.
        scope.launch {
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    FindIdContent(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        onNextClick = onNextClick,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

/**
 * 아이디 찾기 1단계 — stateless 층. 프리뷰·screenshotTest·Robolectric 의 진입점이다.
 *
 * 인증번호 "확인" 이 곧 조회라(`auth/email/find` 가 인증번호를 함께 받는다) 확인에 성공해야
 * "다음" 이 열리고, 결과 화면은 VM 에 담긴 계정을 읽는다.
 */
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
            if (state.hasVerificationError) {
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
