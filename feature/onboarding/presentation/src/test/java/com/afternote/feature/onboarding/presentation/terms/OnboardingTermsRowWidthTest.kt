package com.afternote.feature.onboarding.presentation.terms

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.testing.MinimumTouchTargetSize
import com.afternote.core.ui.testing.assertAccessibleClickTargets
import com.afternote.core.ui.testing.scanEnabledClickTargets
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.onboarding.presentation.R
import com.afternote.feature.onboarding.presentation.signup.SignUpIntent
import com.afternote.feature.onboarding.presentation.signup.SignUpUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 약관 행의 토글 영역이 행 끝까지 닿는지 고정한다 (#1132).
 *
 * 「전체보기」가 좁은 폭에 갇혀 세로로 접히는 결함 자체는 360dp 스크린샷 baseline 이 잡는다.
 * 그쪽은 실제 폰트로 렌더하므로 줄바꿈이 그대로 드러난다 — 반대로 여기서 텍스트 접힘을
 * 단언해 보면 수정 전 코드에서도 통과한다(Robolectric 렌더에서는 재현되지 않았다). 그래서
 * 이 파일은 텍스트 접힘을 보지 않는다.
 *
 * 대신 baseline 이 원리상 볼 수 없는 것을 본다 — **터치 영역**이다. 렌더가 1픽셀도 바뀌지
 * 않으면서 눌리는 범위만 달라지는 변경이 있고, 그건 노드 bounds 로만 잡힌다.
 *
 * `qualifiers` 로 360×800dp @320dpi 를 강제한다 — 폭 배분이 좁은 화면에서 결정되기 때문이다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi")
class OnboardingTermsRowWidthTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setTermsContent(
        onToggleAll: (Boolean) -> Unit = {},
        onViewTermsClick: (TermsType) -> Unit = {},
    ) {
        composeRule.setContent {
            AfternoteTheme {
                OnboardingTermsContent(
                    state = SignUpUiState(),
                    onIntent = { intent ->
                        if (intent is SignUpIntent.ToggleAllTerms) onToggleAll(intent.agreed)
                    },
                    snackbarHostState = remember { SnackbarHostState() },
                    onViewTermsClick = onViewTermsClick,
                    onNextClick = {},
                    onBackClick = {},
                )
            }
        }
    }

    /**
     * 「약관에 전체동의」 행은 오른쪽에 「전체보기」가 없어 남는 폭이 그대로 비어 있다.
     *
     * 그 빈 폭까지 토글 영역이어야 한다 — 리스트 행 체크박스는 행 아무 데나 눌러도 켜지는 것이
     * 통상 동작이고, 글자 끝에서 잘리면 손가락으로 겨냥해야 한다. 제목 쪽 Row 의 `weight` 를
     * 빼거나 `fill = false` 로 낮추면 토글 영역이 글자 폭까지만 좁아지는데, **렌더는 1픽셀도
     * 바뀌지 않아 스크린샷으로는 잡히지 않는다.**
     *
     * 행의 오른쪽 끝은 같은 화면의 「전체보기」 오른쪽 끝으로 잡는다 — 그 행의 최상위 Row 가
     * `fillMaxWidth` 라 그 값이 곧 콘텐츠 전폭의 끝이다.
     */
    @Test
    fun `전체동의 토글 영역은 행 오른쪽 끝까지 넓다`() {
        setTermsContent()
        val agreeAll = composeRule.activity.getString(R.string.onboarding_terms_agree_all)
        val viewDetail = composeRule.activity.getString(R.string.onboarding_terms_view_detail)

        val toggleRight =
            composeRule
                .onNode(isToggleable() and hasText(agreeAll))
                .getUnclippedBoundsInRoot()
                .right

        val contentRight =
            composeRule
                .onAllNodesWithText(viewDetail)[0]
                .getUnclippedBoundsInRoot()
                .right

        assertEquals(
            "「전체동의」 토글 영역이 행 끝까지 닿지 않는다 — 토글 끝 $toggleRight, 행 끝 $contentRight",
            contentRight.value,
            toggleRight.value,
            0.5f,
        )
    }

    @Test
    fun `약관 타깃은 이름 역할 상태를 갖고 콜백을 구분한다`() {
        var toggleAllValue: Boolean? = null
        val openedTerms = mutableListOf<TermsType>()
        setTermsContent(
            onToggleAll = { toggleAllValue = it },
            onViewTermsClick = openedTerms::add,
        )

        composeRule.assertAccessibleClickTargets()
        val targets = composeRule.scanEnabledClickTargets()
        val agreeAll = composeRule.activity.getString(R.string.onboarding_terms_agree_all)
        val agreeAllTarget = targets.single { it.name == agreeAll }
        val detailTargets = targets.filter { it.name == "전체보기" }
        assertEquals(Role.Checkbox, agreeAllTarget.role)
        assertEquals("Off", agreeAllTarget.toggleableState.toString())
        assertFalse(agreeAllTarget.isSmallerThan(MinimumTouchTargetSize))
        assertEquals(3, detailTargets.size)
        assertEquals(listOf(Role.Button, Role.Button, Role.Button), detailTargets.map { it.role })

        composeRule.onNodeWithText(agreeAll).performClick()
        composeRule.onAllNodesWithText("전체보기")[0].performClick()
        assertEquals(true, toggleAllValue)
        assertEquals(listOf(TermsType.SERVICE), openedTerms)
    }
}
