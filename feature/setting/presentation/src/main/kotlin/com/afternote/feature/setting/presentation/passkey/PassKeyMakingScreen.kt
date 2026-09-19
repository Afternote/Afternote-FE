package com.afternote.feature.setting.presentation.passkey

import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.afternote.core.common.biometric.confirmWithCryptoOperation
import com.afternote.core.common.biometric.createBiometricCryptoObject
import com.afternote.core.common.biometric.isBiometricCryptoSupported
import com.afternote.core.ui.findActivity
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val LOG_TAG = "PassKeyMaking"

private sealed interface BiometricResult {
    data object Success : BiometricResult

    data object Canceled : BiometricResult

    data class Error(
        val message: String,
    ) : BiometricResult
}

@Composable
fun PassKeyMakingScreen(
    onBackClick: () -> Unit,
    onPasswordAuthClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PassKeyViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity<FragmentActivity>() }
    val coroutineScope = rememberCoroutineScope()
    val isBiometricAvailable =
        remember {
            BiometricManager
                .from(context)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var isAuthenticating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (showCompletionDialog) {
        Popup(
            type = PopupType.Default,
            message = "패스키 생성이 완료되었습니다",
            onConfirm = {
                showCompletionDialog = false
                onBackClick()
            },
            onDismiss = { showCompletionDialog = false },
        )
    }

    errorMessage?.let { msg ->
        Popup(
            type = PopupType.Default,
            message = msg,
            onConfirm = { errorMessage = null },
            onDismiss = { errorMessage = null },
        )
    }

    PassKeyMakingContent(
        onBackClick = onBackClick,
        onBiometricAuthClick = {
            if (!isAuthenticating && activity != null) {
                isAuthenticating = true
                coroutineScope.launch {
                    try {
                        when (val result = authenticate(activity)) {
                            BiometricResult.Success -> {
                                viewModel.savePasskeyRegistered()
                                showCompletionDialog = true
                            }

                            BiometricResult.Canceled -> {
                                Unit
                            }

                            is BiometricResult.Error -> {
                                errorMessage = result.message
                            }
                        }
                    } finally {
                        isAuthenticating = false
                    }
                }
            }
        },
        onPasswordAuthClick = onPasswordAuthClick,
        isBiometricAvailable = isBiometricAvailable,
        modifier = modifier,
    )
}

private suspend fun authenticate(activity: FragmentActivity): BiometricResult =
    suspendCancellableCoroutine { continuation ->
        val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val promptInfo =
            BiometricPrompt.PromptInfo
                .Builder()
                .setTitle("패스키 등록")
                .setSubtitle("패스키로 사용할 지문을 인식해 주세요.")
                .setAllowedAuthenticators(authenticators)
                .build()

        // API 26~29 에서는 DEVICE_CREDENTIAL 허용자와 CryptoObject 를 함께 쓸 수 없다.
        // 이때는 관문 없이 인증하고, 성공 확정은 cipher 부재로 통과시킨다 (#1166).
        val cryptoObject = if (isBiometricCryptoSupported) createBiometricCryptoObject() else null

        val biometricPrompt =
            BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (!continuation.isActive) return
                        // 성공은 콜백의 도달이 아니라 사용자 인증에 묶인 키로 암호 연산이
                        // 성사되는가로 확정한다 — 콜백만 가로챈 경우 연산이 실패한다 (#1166).
                        val confirmation = confirmWithCryptoOperation(result.cryptoObject?.cipher)
                        if (confirmation.isSuccess) {
                            continuation.resume(BiometricResult.Success)
                        } else {
                            Log.w(LOG_TAG, "인증 후 암호 연산 실패", confirmation.exceptionOrNull())
                            continuation.resume(BiometricResult.Error("지문 확인에 실패했어요. 다시 시도해 주세요."))
                        }
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ) {
                        if (!continuation.isActive) return
                        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            errorCode == BiometricPrompt.ERROR_CANCELED
                        ) {
                            continuation.resume(BiometricResult.Canceled)
                        } else {
                            continuation.resume(BiometricResult.Error(errString.toString()))
                        }
                    }

                    override fun onAuthenticationFailed() {}
                },
            )

        continuation.invokeOnCancellation { biometricPrompt.cancelAuthentication() }
        if (cryptoObject != null) {
            biometricPrompt.authenticate(promptInfo, cryptoObject)
        } else {
            biometricPrompt.authenticate(promptInfo)
        }
    }
