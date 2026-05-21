package com.afternote.feature.afternote.presentation.receiver.deliveryverification.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.modifierextention.addFocusCleaner
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.presentation.R

/**
 * 수신자 인증 흐름(designs 2·3·4·5·6·7·8) 공용 Scaffold — TopBar "수신자 인증" + (선택) 진행 인디케이터 +
 * content + 하단 CTA 버튼 (이슈 #215).
 *
 * 발신자 온보딩의 [com.afternote.feature.onboarding.presentation.signup.scaffold.OnboardingScaffold] /
 * [com.afternote.feature.onboarding.presentation.signup.scaffold.ProgressBarScaffold] 와 동일 구조이나
 * cross-feature import 불가로 별도 정의. content 영역의 좌우 padding 20dp 도 동일.
 *
 * @param actionButtonText CTA 버튼 라벨. 보통 "다음" / "인증 시작하기" / "받은 기록함으로 돌아가기" 등.
 * @param currentStep null 이면 진행 인디케이터 미표시 (완료 화면 등). 1..[totalSteps] 사이면 채워진 비율.
 */
@Composable
fun ReceiverVerifyScaffold(
    actionButtonText: String,
    onBackClick: () -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActionEnabled: Boolean = true,
    currentStep: Int? = null,
    totalSteps: Int = RECEIVER_VERIFY_TOTAL_STEPS,
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val horizontalPadding = 20.dp

    Scaffold(
        modifier =
            modifier
                .addFocusCleaner(focusManager)
                .fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.receiver_verify_title),
                onBackClick = {
                    focusManager.clearFocus()
                    onBackClick()
                },
            )
        },
        snackbarHost = {
            if (snackbarHostState != null) SnackbarHost(hostState = snackbarHostState)
        },
        bottomBar = {
            val navBarHeight =
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val bottomPadding = if (navBarHeight <= 30.dp) 49.dp else navBarHeight + 49.dp
            AfternoteButton(
                text = actionButtonText,
                onClick = {
                    focusManager.clearFocus()
                    onActionClick()
                },
                type = if (isActionEnabled) AfternoteButtonType.Default else AfternoteButtonType.Un,
                modifier =
                    Modifier
                        .padding(horizontal = horizontalPadding)
                        .padding(bottom = bottomPadding),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .padding(horizontal = horizontalPadding)
                    .fillMaxSize(),
        ) {
            if (currentStep != null) {
                ReceiverVerifyStepProgressBar(
                    currentStep = currentStep,
                    totalSteps = totalSteps,
                )
            }
            Spacer(modifier = Modifier.height(35.dp))
            content()
        }
    }
}

/** 수신자 인증 진행 단계: 본인 확인 → 마스터 키 → 서류 업로드. */
const val RECEIVER_VERIFY_TOTAL_STEPS: Int = 3

object ReceiverVerifyStep {
    const val IDENTITY: Int = 1
    const val MASTER_KEY: Int = 2
    const val DOCUMENTS: Int = 3
}
