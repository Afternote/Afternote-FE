package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.component.InsertPasswordContent

/**
 * 패스키 비밀번호 인증 화면의 상태 없는 본문.
 *
 * ViewModel·완료 처리는 [PassKeyPasswordScreen] 이 들고, 이 함수는 넘겨받은 상태만 그린다.
 */
@Composable
internal fun PassKeyPasswordContent(
    passwordLength: Int,
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onConfirmClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            DetailTopBar(
                title = stringResource(id = R.string.passkey_management_title),
                onBackClick = onBack,
            )
        },
        modifier = modifier,
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            InsertPasswordContent(
                titleText = "비밀번호를 입력해 주세요.",
                passwordLength = passwordLength,
                onDigitClick = onDigitClick,
                onDeleteClick = onDeleteClick,
                onConfirmClick = onConfirmClick,
            )
        }
    }
}
