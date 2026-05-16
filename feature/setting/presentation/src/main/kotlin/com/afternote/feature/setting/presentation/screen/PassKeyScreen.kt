package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.afternote.core.ui.theme.AfternoteDesign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R

@Composable
fun PassKeyScreen(
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
            Column {
                Text(
                    text = stringResource(id = R.string.passkey_management_title),
                    style = AfternoteDesign.typography.bodyLargeB,
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.passkey_management_description),
                    style = AfternoteDesign.typography.bodySmallR,
                )
                Spacer(modifier = Modifier.height(84.dp))
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .width(326.dp)
                            .height(260.dp),
                    Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_passkey_main),
                        contentDescription = "패스키 메인 로고",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                    )
                }
            }
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
