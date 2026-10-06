package com.afternote.feature.setting.presentation.passkey

import androidx.biometric.BiometricManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.afternote.core.common.biometric.BiometricAuthResult
import com.afternote.core.common.biometric.BiometricMessages
import com.afternote.core.common.biometric.authenticateBiometric
import com.afternote.core.ui.findActivity
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import com.afternote.feature.setting.presentation.R
import kotlinx.coroutines.launch

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

    val messages =
        BiometricMessages(
            initFailed = stringResource(R.string.setting_biometric_init_failed),
            noHardware = stringResource(R.string.setting_biometric_no_hardware),
            noneEnrolled = stringResource(R.string.setting_biometric_none_enrolled),
            hwUnavailable = stringResource(R.string.setting_biometric_hw_unavailable),
            notAvailable = stringResource(R.string.setting_biometric_not_available),
            verificationFailed = stringResource(R.string.setting_biometric_verification_failed),
        )
    val promptTitle = stringResource(R.string.setting_biometric_prompt_title)
    val promptSubtitle = stringResource(R.string.setting_biometric_prompt_subtitle)

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
                        when (val result = activity.authenticateBiometric(promptTitle, promptSubtitle, messages)) {
                            BiometricAuthResult.Success -> {
                                viewModel.savePasskeyRegistered()
                                showCompletionDialog = true
                            }

                            BiometricAuthResult.Canceled -> {
                                Unit
                            }

                            is BiometricAuthResult.Error -> {
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
