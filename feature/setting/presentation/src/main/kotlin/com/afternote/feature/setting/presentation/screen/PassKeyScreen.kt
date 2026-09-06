package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.component.PasskeyGuideContent

@Composable
internal fun PassKeyScreen(
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
        ) {
            PasskeyGuideContent(
                title = stringResource(id = R.string.passkey_management_title),
                description = stringResource(id = R.string.passkey_management_description),
            )
            AfternoteButton(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 63.dp),
                text = stringResource(id = R.string.passkey_register),
                onClick = onRegisterClick,
                type = AfternoteButtonType.Default,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PassKeyScreenPrev() {
    PassKeyScreen(onBackClick = {}, onRegisterClick = {})
}
