package com.afternote.feature.onboarding.presentation.signup.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.afternote.feature.onboarding.presentation.signup.SIGN_UP_TOTAL_STEPS

/**
 * 회원가입 스텝 화면 공용 스캐폴드.
 *
 * - 상단: [DetailTopBar] + 뒤로가기 시 포커스 클리어
 * - 본문: [StepProgressBar] (full-width) + 화면별 [content] slot (weight(1f))
 * - 하단: 선택적 [bottomButton] slot
 *
 * [horizontalPadding] 은 [content] 와 [bottomButton] 양쪽에 공통으로 적용되어,
 * 본문 Column 과 버튼의 좌우 정렬이 구조적으로 어긋날 수 없게 합니다.
 * [StepProgressBar] 는 의도적으로 이 padding 밖에 두어 화면 폭을 가득 채웁니다.
 *
 * Window Insets 전략: 상단 inset 만 직접 padding 으로 소비하고, 나머지는
 * [consumeWindowInsets] 로 하류에 "이미 소비됨"을 알린 뒤 [imePadding] 으로 키보드에 반응.
 * 네비게이션 바 inset 은 의도적으로 padding 으로 잡지 않아, 화면이 edge-to-edge 로
 * 렌더되고 [bottomButton] 쪽에서 원하는 피그마 수치(예: `bottom = 49.dp`)를 직접 제어할 수
 * 있게 합니다.
 *
 * @param currentStep 현재 단계(1부터)
 * @param onBackClick 뒤로가기 콜백. 내부에서 focusClear 후 호출됩니다.
 * @param modifier Scaffold 에 적용할 Modifier
 * @param containerColor Scaffold containerColor
 * @param horizontalPadding 본문/버튼 양쪽에 공통으로 적용될 좌우 padding
 * @param bottomButton 하단 고정 버튼 slot. null 이면 자리 차지 없음.
 * @param content 스텝바 아래 본문 영역. ColumnScope 를 제공하며 기본 weight(1f).
 */
@OptIn(ExperimentalMaterial3Api::class)
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
                    .padding(top = innerPadding.calculateTopPadding(), bottom = 49.dp)
                    .padding(horizontal = 20.dp)
                    // "방금 적용한 여백은 이미 처리했어!" 라고 시스템에 신고합니다. (중복 방지)
                    .consumeWindowInsets(innerPadding)
                    // 이제 안심하고 키보드(IME) 높이만큼만 순수하게 추가 패딩을 밀어 넣습니다.
                    .imePadding()
                    .fillMaxSize()
                    .addFocusCleaner(focusManager),
        ) {
            StepProgressBar(
                currentStep = currentStep,
                totalSteps = SIGN_UP_TOTAL_STEPS,
                contentDescription = progressDescription,
            )

            content()
            Spacer(Modifier.weight(1f))

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
