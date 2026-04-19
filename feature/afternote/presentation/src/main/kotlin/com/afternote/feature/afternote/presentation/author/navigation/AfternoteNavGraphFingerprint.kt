package com.afternote.feature.afternote.presentation.author.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import com.afternote.core.ui.findActivity
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.fingerprint.BiometricAuthResult
import com.afternote.feature.afternote.presentation.shared.fingerprint.BiometricMessages
import com.afternote.feature.afternote.presentation.shared.fingerprint.FingerprintLoginScreen
import com.afternote.feature.afternote.presentation.shared.fingerprint.authenticateBiometric
import kotlinx.coroutines.launch

@Composable
internal fun AfternoteFingerprintLoginNavigation(
    onAuthenticationSuccess: () -> Unit,
    onShowError: (String) -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity<FragmentActivity>() }
    val coroutineScope = rememberCoroutineScope()

    // 생체 인증 프롬프트가 떠 있는 동안의 중복 클릭(연타)을 방어한다.
    // suspend 확장이 취소-안전하긴 하지만, OS 프롬프트가 이미 표시된 상태에서의 재호출은 회피하는 편이 UX상 안전.
    var isAuthenticating by remember { mutableStateOf(false) }

    val messages =
        BiometricMessages(
            initFailed = stringResource(R.string.biometric_init_failed),
            noHardware = stringResource(R.string.biometric_no_hardware),
            noneEnrolled = stringResource(R.string.biometric_none_enrolled),
            hwUnavailable = stringResource(R.string.biometric_hw_unavailable),
            notAvailable = stringResource(R.string.biometric_not_available),
        )
    val promptTitle = stringResource(R.string.biometric_prompt_title)
    val promptSubtitle = stringResource(R.string.biometric_prompt_subtitle)

    FingerprintLoginScreen(
        onFingerprintAuthClick = {
            when {
                isAuthenticating -> {
                }

                activity == null -> {
                    // MainActivity가 FragmentActivity를 상속하지 않은 설정 누락 상황.
                    // 프로덕션 크래시 대신 에러 UI로 폴백한다.
                    onShowError(messages.initFailed)
                }

                else -> {
                    isAuthenticating = true
                    coroutineScope.launch {
                        try {
                            when (
                                val result =
                                    activity.authenticateBiometric(promptTitle, promptSubtitle, messages)
                            ) {
                                BiometricAuthResult.Success -> onAuthenticationSuccess()
                                BiometricAuthResult.Canceled -> Unit
                                is BiometricAuthResult.Error -> onShowError(result.message)
                            }
                        } finally {
                            // 결과·예외·취소 어느 경로로 끝나든 가드 해제
                            isAuthenticating = false
                        }
                    }
                }
            }
        },
    )
}
