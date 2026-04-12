package com.afternote.feature.onboarding.presentation.scaffold

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.addFocusCleaner
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.scaffold.topbar.DetailTopBar
import com.afternote.core.ui.theme.AfternoteTheme

@Composable
fun OnboardingScaffold(
    title: String,
    buttonText: String,
    onBackClick: () -> Unit,
    onActionButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit, // 내부 콘텐츠 슬롯
) {
    val focusManager = LocalFocusManager.current
    val horizontalPadding = 20.dp

    Scaffold(
        modifier = modifier.addFocusCleaner(focusManager),
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(
                title = title,
                onBackClick = {
                    focusManager.clearFocus()
                    onBackClick()
                },
            )
        },
        bottomBar = {
            // 제스차 바를 쓰는 경우 화면 하단으로부터 49.dp, 구형 네비게이션 바를 쓰는 경우 바로부터 49.dp
            val navBarHeight =
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val finalBottomPadding = if (navBarHeight <= 30.dp) 49.dp else navBarHeight + 49.dp

            AfternoteButton(
                text = buttonText,
                onClick = {
                    focusManager.clearFocus()
                    onActionButtonClick()
                },
                type = AfternoteButtonType.Default,
                modifier =
                    Modifier
                        .padding(horizontal = horizontalPadding)
                        .padding(bottom = finalBottomPadding),
            )
        },
    ) { innerPadding ->
        // 하단 바 높이와 상단 바 높이가 계산된 패딩을 자식에게 전달
        Column(
            Modifier
                .padding(innerPadding)
                .padding(horizontal = horizontalPadding),
        ) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScaffoldPreview() {
    AfternoteTheme {
        OnboardingScaffold(
            title = "Onboarding Title",
            buttonText = "Next",
            onBackClick = {},
            onActionButtonClick = {},
        ) {
            Text(text = "This is the onboarding content area.")
        }
    }
}
