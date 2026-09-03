package com.afternote.feature.onboarding.presentation.findaccount

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.scaffold.FlowStepScaffold
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.onboarding.presentation.R

private val HeaderSpacing = 8.dp

/**
 * 비밀번호 찾기 3단계 — 변경 완료 안내 (시안 `2383:16854`).
 *
 * 하단 CTA 와 상단 뒤로가기가 같은 곳(로그인)으로 간다. 비밀번호는 이미 바뀐 뒤라 되돌아갈
 * 앞 단계가 없다 — 인증번호도 서버가 소비해 버려 이전 화면이 더는 유효하지 않다.
 */
@Composable
fun FindPasswordCompleteScreen(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowStepScaffold(
        topBarTitle = stringResource(R.string.onboarding_find_password_title),
        actionButtonText = stringResource(R.string.onboarding_find_password_complete_login),
        onBackClick = onLoginClick,
        onActionClick = onLoginClick,
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.height(HeaderSpacing))
        Text(
            text = stringResource(R.string.onboarding_find_password_complete_message),
            style = AfternoteDesign.typography.bodyLargeR,
            color = AfternoteDesign.colors.gray8,
        )
    }
}
