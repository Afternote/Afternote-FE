package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R

@Composable
fun PassKeyScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            DetailTopBar(
                title = stringResource(id = R.string.passkey_management_title),
                onBackClick = onBackClick,
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                modifier
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.passkey_management_description),
            )
            Spacer(modifier = Modifier.height(84.dp))
            Image(
                painter = painterResource(R.drawable.ic_passkey_main),
                contentDescription = "패스키 메인 로고",
                modifier =
                    Modifier
                        .width(326.dp)
                        .height(260.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            AfternoteButton(
                text = stringResource(id = R.string.passkey_register),
                onClick = {},
                type = AfternoteButtonType.Default,
            )
        }
    }
}

@Preview
@Composable
private fun PassKeyScreenPrev() {
    PassKeyScreen(onBackClick = {})
}
