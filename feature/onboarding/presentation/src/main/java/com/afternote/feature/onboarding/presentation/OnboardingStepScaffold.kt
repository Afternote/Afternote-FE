package com.afternote.feature.onboarding.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
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
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.scaffold.topbar.DetailTopBar

@Composable
internal fun OnboardingStepScaffold(
    currentStep: Int,
    buttonText: String,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = modifier.addFocusCleaner(focusManager),
        topBar = {
            DetailTopBar(
                title = "회원가입", // TODO:stringResource로 빼
                onBackClick = {
                    focusManager.clearFocus()
                    onBackClick()
                },
            )
        },
        bottomBar = {
            // 1. 현재 기기의 시스템 내비게이션 바 높이를 가져옴
            val navBarHeight =
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            // 2. 조건부 로직 결정
            // - 제스처 바(보통 24~28dp): 바닥에서 49dp (제스처 바를 포함한 간격)
            // - 3버튼 바(보통 48dp 이상): 바 높이 + 49dp (바 위에서부터 간격)
            val finalBottomPadding =
                if (navBarHeight <= 30.dp) {
                    49.dp
                } else {
                    navBarHeight + 49.dp
                }

            AfternoteButton(
                text = buttonText,
                onClick = {
                    focusManager.clearFocus()
                    onNextClick()
                },
                type = AfternoteButtonType.Default,
                modifier =
                    Modifier
                        .padding(horizontal = 20.dp)
                        .padding(bottom = finalBottomPadding) // 계산된 '진짜' 49dp 적용
                        .imePadding(),
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding) // Scaffold가 다 계산해준 상하단 안전 영역 100% 신뢰
                    .padding(horizontal = 20.dp)
                    .consumeWindowInsets(innerPadding)
                    .imePadding(),
        ) {
            StepProgressBar(
                currentStep = currentStep,
                contentDescription =
                    stringResource(R.string.onboarding_step_description, currentStep),
            )

            content()
        }
    }
}
