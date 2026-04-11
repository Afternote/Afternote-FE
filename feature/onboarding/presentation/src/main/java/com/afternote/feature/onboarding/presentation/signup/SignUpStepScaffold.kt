package com.afternote.feature.onboarding.presentation.signup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.StepProgressBar
import com.afternote.core.ui.addFocusCleaner
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.scaffold.topbar.DetailTopBar
import com.afternote.feature.onboarding.presentation.R

@Composable
internal fun SignUpStepScaffold(
    currentStep: Int,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val progressDescription =
        stringResource(
            R.string.signup_progress_description,
            currentStep,
            SIGN_UP_TOTAL_STEPS,
        )

    Scaffold(
        modifier = modifier,
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.signup_title),
                onBackClick = {
                    focusManager.clearFocus()
                    onBackClick()
                },
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    // 디자인 가이드상 "시스템 바 끝에서 버튼까지 총 49dp"를 만족시키되,
                    // 시스템 바가 49dp보다 큰 기기(구형/3버튼 내비게이션)에서는 가려지지 않도록 방어
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        start = 20.dp,
                        end = 20.dp,
                        bottom = maxOf(innerPadding.calculateBottomPadding(), 49.dp),
                    )
                    // "방금 적용한 여백은 이미 처리했어!" 라고 시스템에 신고합니다. (중복 방지)
                    .consumeWindowInsets(innerPadding)
                    // 이제 안심하고 키보드(IME) 높이만큼만 순수하게 추가 패딩을 밀어 넣습니다.
                    .imePadding()
                    .addFocusCleaner(focusManager),
        ) {
            StepProgressBar(
                currentStep = currentStep,
                totalSteps = SIGN_UP_TOTAL_STEPS,
                contentDescription = progressDescription,
            )

            content()

            Spacer(modifier = Modifier.weight(1f))

            AfternoteButton(
                text = stringResource(R.string.signup_next),
                onClick = {
                    focusManager.clearFocus()
                    onNextClick()
                },
            )
        }
    }
}
