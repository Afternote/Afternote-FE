package com.afternote.feature.onboarding.presentation.component

import androidx.annotation.StringRes
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
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.scaffold.topbar.DetailTopBar

/**
 * 온보딩(회원가입/약관 동의 등) 다단계 플로우 공통 스캐폴드.
 *
 * 구조:
 * - 상단: [DetailTopBar] (뒤로가기 + 화면 제목)
 * - 상단 바로 아래: [StepProgressBar] (단계 애니메이션)
 * - 중앙: [content] 슬롯 (화면별 본문)
 * - 하단: [AfternoteButton] (다음 단계로 이동)
 *
 * 모든 화면에서 디자인 가이드상 좌우 20.dp, 시스템 바 끝~버튼 49.dp를 일괄 적용합니다.
 *
 * @param currentStep 현재 단계 (1부터 시작)
 * @param buttonText 하단 CTA 버튼 텍스트
 * @param progressDescriptionRes 진행바 스크린리더용 설명 문자열 리소스.
 *  정수 인자(`%1$d`, `%2$d`)를 포함해야 합니다. (예: "회원가입 진행도 %2$d단계 중 %1$d단계")
 * @param onBackClick 뒤로가기 콜백
 * @param onNextClick 다음 단계 이동 콜백
 * @param modifier 외부 Modifier
 * @param content 본문 슬롯 ([ColumnScope] 안에서 선언)
 */
@Composable
internal fun OnboardingStepScaffold(
    currentStep: Int,
    buttonText: String,
    @StringRes progressDescriptionRes: Int,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = modifier,
        topBar = {
            DetailTopBar(
                title = "회원가입",
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
                    ).consumeWindowInsets(innerPadding)
                    .imePadding()
                    .addFocusCleaner(focusManager),
        ) {
            StepProgressBar(
                currentStep = currentStep,
                contentDescription =
                    stringResource(progressDescriptionRes, currentStep, 4),
            )

            content()

            Spacer(modifier = Modifier.weight(1f))

            AfternoteButton(
                text = buttonText,
                onClick = {
                    focusManager.clearFocus()
                    onNextClick()
                },
                type =
                    AfternoteButtonType.Default,
            )
        }
    }
}
