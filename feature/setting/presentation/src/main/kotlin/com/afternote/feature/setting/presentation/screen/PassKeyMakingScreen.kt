package com.afternote.feature.setting.presentation.screen

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.findActivity
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.viewmodel.PassKeyViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private sealed interface BiometricResult {
    data object Success : BiometricResult
    data object Canceled : BiometricResult
    data class Error(val message: String) : BiometricResult
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

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(
                title = stringResource(id = R.string.passkey_management_title),
                onBackClick = onBackClick,
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(137.dp))
                Text(
                    text = stringResource(R.string.passkey_fingerprint_guide),
                    style = AfternoteDesign.typography.bodyLargeR,
                    modifier = Modifier.padding(innerPadding),
                )
                Spacer(modifier = Modifier.height(40.dp))
                Image(
                    painterResource(R.drawable.ic_fingerprint),
                    "지문",
                )
            }
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 63.dp),
            ) {
                AfternoteButton(
                    text = "지문 인증하기",
                    onClick = {
                        if (isAuthenticating || activity == null) return@AfternoteButton
                        isAuthenticating = true
                        coroutineScope.launch {
                            try {
                                val result = authenticate(activity)
                                when (result) {
                                    BiometricResult.Success -> {
                                        viewModel.savePasskeyRegistered()
                                        showCompletionDialog = true
                                    }
                                    BiometricResult.Canceled -> Unit
                                    is BiometricResult.Error -> errorMessage = result.message
                                }
                            } finally {
                                isAuthenticating = false
                            }
                        }
                    },
                    type = if (isBiometricAvailable) AfternoteButtonType.Default else AfternoteButtonType.Un,
                )
                Spacer(modifier = Modifier.height(8.dp))
                AfternoteButton(
                    text = "비밀번호로 인증하기",
                    onClick = onPasswordAuthClick,
                    type = AfternoteButtonType.Active,
                )
            }
        }
    }
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

        val biometricPrompt =
            BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (continuation.isActive) continuation.resume(BiometricResult.Success)
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
        biometricPrompt.authenticate(promptInfo)
    }

@Preview(showBackground = true)
@Composable
private fun PassKeyMakingScreenPrev() {
    PassKeyMakingScreen(onBackClick = {}, onPasswordAuthClick = {})
}
