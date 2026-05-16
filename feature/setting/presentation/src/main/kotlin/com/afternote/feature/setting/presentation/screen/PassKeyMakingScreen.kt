package com.afternote.feature.setting.presentation.screen

import androidx.biometric.BiometricManager
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R

@Composable
fun PassKeyMakingScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isBiometricAvailable =
        remember {
            BiometricManager
                .from(context)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        }
    var showCompletionDialog by remember { mutableStateOf(false) }

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
                    onClick = { showCompletionDialog = true },
                    type = if (isBiometricAvailable) AfternoteButtonType.Default else AfternoteButtonType.Un,
                )
                Spacer(modifier = Modifier.height(8.dp))
                AfternoteButton(
                    text = "비밀번호로 인증하기",
                    onClick = { showCompletionDialog = true },
                    type = AfternoteButtonType.Active,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PassKeyMakingScreenPrev() {
    PassKeyMakingScreen(onBackClick = {})
}
