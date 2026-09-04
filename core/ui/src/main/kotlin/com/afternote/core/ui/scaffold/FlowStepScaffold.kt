package com.afternote.core.ui.scaffold

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
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
 * - bottom 버튼 49dp 마진 (제스처 바: 49dp / 구형 네비 바: bar height + 49dp)
 * - content 영역은 [ColumnScope] — 위에서 아래로 쌓이는 일반 흐름 가정
 *
 * **키보드 대응**: IME 패딩은 Scaffold `modifier` 에 있어야 한다 (#1849).
 * `bottomBar` 는 `WindowInsets.navigationBars` 만 계산하므로, 호출처가 [content] 안에서
 * `imePadding()` 을 걸어도 하단 CTA 는 키보드에 덮인 채로 남는다. Scaffold 에 걸면 소비된
 * inset 이 하위로 전파되므로 [content] 안의 `imePadding()` 은 자동으로 0 이 되어 이중 적용되지 않는다.
 * 더하는 양은 IME 에서 내비 바를 뺀 값이다 — 루트 Scaffold 가 내비 바 몫을 이미 깔아 두기 때문이다.
 * (앱 매니페스트의 `windowSoftInputMode="adjustResize"` 와 한 쌍이다 — 그게 없으면 IME inset 자체가 오지 않는다.)
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
                // 루트 Scaffold(AppNavigation)가 systemBars 하단 — 내비 바 — 을 innerPadding 으로 이미 깔아 두는데,
                // consumeWindowInsets 없이 padding 으로만 전달돼 소비로 잡히지 않는다. 그 위에서 imePadding() 을
                // 그대로 쓰면 IME 높이(내비 바 영역 포함)를 통째로 더해 키보드 위로 내비 바 한 겹만큼 더 뜬다 —
                // 3버튼 내비에서 48dp, 제스처에서 24dp. 내비 바 몫을 뺀 IME 만 더한다. 키보드가 없으면 ime 가
                // 0 이라 아무것도 더하지 않는다 (#1849 리뷰 실측). FlowStepScaffold 호스트 11곳은 전부 그 루트 아래다.
                .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars)),
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
            // 제스처 바를 쓰는 경우 화면 하단으로부터 49dp, 구형 네비게이션 바를 쓰는 경우 바로부터 49dp.
            //
            // `WindowInsets.navigationBars` 는 소비를 반영하지 않는 원본 inset 이다. 위 Scaffold 가
            // `imePadding()` 으로 이미 키보드 높이(내비 바 포함)만큼 줄어든 뒤인데, 여기서 내비 바
            // 높이를 또 더하면 3버튼 내비 기기에서 키보드가 뜬 동안 CTA 가 키보드 위 49dp 가 아니라
            // 약 97dp 에 뜬다. 키보드가 떠 있으면 내비 바 몫은 IME inset 에 이미 들어 있으므로 뺀다 —
            // IME 가 없을 땐 `ime` 가 0 이라 종전과 같다 (#1849 리뷰).
            val navBarHeight =
                WindowInsets.navigationBars
                    .exclude(WindowInsets.ime)
                    .asPaddingValues()
                    .calculateBottomPadding()
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
