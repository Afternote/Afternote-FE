package com.afternote.core.ui.scaffold

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.modifierextention.addFocusCleaner
import com.afternote.core.ui.topbar.DetailTopBar

/**
 * 단계형 흐름 공용 Scaffold — TopBar + (선택) 진행 인디케이터 + content + 하단 CTA 버튼 (#221).
 *
 * 기존 feature 별로 분산되어 있던 `OnboardingScaffold`·`ProgressBarScaffold` (onboarding) ·
 * `ReceiverVerifyScaffold` (receiver/deliveryverification) 를 `core/ui` 로 통합.
 *
 * **레이아웃 토큰** (모듈 공통):
 * - horizontal padding 20dp
 * - 하단 버튼은 시스템 내비게이션 바 또는 키보드 위로 49dp 간격
 * - content 영역은 [ColumnScope] — 위에서 아래로 쌓이는 일반 흐름 가정
 *
 * **시스템 여백**: Scaffold 바깥에서 내비게이션 바와 키보드 inset을 처리한다.
 * 상위에서 소비한 영역은 자동으로 제외되므로 루트 여백의 크기를 직접 빼지 않는다.
 * 하단 버튼은 디자인 간격만 관리하고, content에도 Scaffold 여백의 소비를 전달한다.
 *
 * **진행 인디케이터**:
 * - [currentStep] 과 [totalSteps] 가 모두 non-null 이면 상단에 [FlowStepProgressBar] 표시
 * - 둘 중 하나라도 null 이면 미표시 (완료 화면·단일 입력 화면 등)
 * - progress 아래·content 위의 추가 spacing 이 필요하면 *호출처가 [content] 첫 줄에 `Spacer`* 를 직접 추가.
 *   (수신자 인증 흐름은 35dp, 회원가입 흐름은 0dp 등 흐름별 차이가 있어 Scaffold 가 책임지지 않음.)
 *
 * @param topBarTitle TopBar 중앙 제목. 호출처에서 `stringResource(...)` 결과 전달.
 * @param actionButtonText 하단 CTA 버튼 라벨.
 * @param isActionEnabled false 면 버튼이 [AfternoteButtonType.Un] (비활성) 으로 표시.
 * @param currentStep / [totalSteps] 진행 인디케이터 값. 둘 다 null 이면 미표시.
 */
@Composable
fun FlowStepScaffold(
    topBarTitle: String,
    actionButtonText: String,
    onBackClick: () -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActionEnabled: Boolean = true,
    currentStep: Int? = null,
    totalSteps: Int? = null,
    progressContentDescription: String? = null,
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val horizontalPadding = 20.dp

    Scaffold(
        modifier =
            modifier
                .addFocusCleaner(focusManager)
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding(),
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(
                title = topBarTitle,
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
                        .padding(bottom = 49.dp),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .padding(horizontal = horizontalPadding),
        ) {
            // local val 박아서 smart-cast 안정 — 향후 람다 캡처 추가되어도 깨지지 않음.
            val step = currentStep
            if (step != null && totalSteps != null) {
                FlowStepProgressBar(
                    currentStep = step,
                    totalSteps = totalSteps,
                    contentDescription = progressContentDescription,
                )
            }
            content()
        }
    }
}
