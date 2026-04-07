package com.afternote.feature.afternote.presentation.author.navigation

import android.util.Log
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute
import com.afternote.feature.afternote.presentation.shared.fingerprint.FingerprintLoginScreen

@Composable
internal fun AfternoteFingerprintLoginNavigation(navController: NavController) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val promptTitle = stringResource(R.string.biometric_prompt_title)
    val promptSubtitle = stringResource(R.string.biometric_prompt_subtitle)
    val notAvailableMessage = stringResource(R.string.biometric_not_available)
    val biometricPrompt =
        remember(activity) {
            try {
                activity?.let { fragActivity ->
                    val executor = ContextCompat.getMainExecutor(fragActivity)
                    BiometricPrompt(
                        fragActivity,
                        executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                navController.navigate(AfternoteRoute.AfternoteHomeRoute) {
                                    popUpTo(AfternoteRoute.FingerprintLoginRoute) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        },
                    )
                }
            } catch (e: Throwable) {
                Log.e(TAG_FINGERPRINT, "FingerprintLoginRoute: BiometricPrompt failed", e)
                null
            }
        }
    val promptInfo =
        remember(promptTitle, promptSubtitle) {
            BiometricPrompt.PromptInfo
                .Builder()
                .setTitle(promptTitle)
                .setSubtitle(promptSubtitle)
                .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                .build()
        }
    FingerprintLoginScreen(
        onFingerprintAuthClick = {
            if (activity == null) return@FingerprintLoginScreen
            val biometricManager = BiometricManager.from(context)
            when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    biometricPrompt?.authenticate(promptInfo)
                }

                else -> {
                    Toast
                        .makeText(context, notAvailableMessage, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        },
    )
}
