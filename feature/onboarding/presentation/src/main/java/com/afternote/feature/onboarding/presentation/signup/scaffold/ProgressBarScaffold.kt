package com.afternote.feature.onboarding.presentation.signup.scaffold

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.StepProgressBar
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.onboarding.presentation.R

@Composable
internal fun ProgressBarScaffold(
    currentStep: Int,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    OnboardingScaffold(
        buttonText = stringResource(R.string.signup_next),
        onBackClick = onBackClick,
        onActionButtonClick = onNextClick,
        modifier = modifier,
        {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize(),
            ) {
                // 온보딩 전용 UI인 프로그레스 바는 여기에 위치
                StepProgressBar(
                    currentStep = currentStep,
                    contentDescription =
                        stringResource(
                            R.string.onboarding_step_description,
                            currentStep,
                        ),
                )
                content()
            }
        },
    ) // 기존 온보딩 투명 배경 유지
}

@Preview(showBackground = true)
@Composable
private fun ProgressBarScaffoldPreview() {
    AfternoteTheme {
        ProgressBarScaffold(
            currentStep = 1,
            onBackClick = {},
            onNextClick = {},
            content = {
                Text(
                    text = "Onboarding Content",
                    modifier = Modifier.padding(top = 16.dp),
                )
            },
        )
    }
}
