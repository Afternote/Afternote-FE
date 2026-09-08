package com.afternote.feature.afternote.presentation.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.testing.scanEnabledClickTargets
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.util.TYPE_FILTER_TABS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteCategoryRowInteractionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * 더보기 화살표가 장식이라는 계약은 «접근성 트리에 아무것도 싣지 않는다» 로 판정한다.
     * 활성 클릭 타깃이 탭 [TYPE_FILTER_TABS] 개뿐이면 화살표는 클릭도 이름도 만들지 않은 것이다.
     *
     * 화살표가 실제로 그려지는지(스크롤 가능 분기)는 screenshotTest 골든
     * `afternoteCategoryRowAllCompactScreenshot` 이 본다 — 장식은 semantics 로 셀 수 없다.
     */
    @Test
    fun `더보기 화살표는 semantics를 만들지 않고 카테고리 탭은 계속 동작한다`() {
        var selectedTab: AfternoteType? = null
        // Robolectric 폰트 폭에 기대지 않고 스크롤 가능 분기를 확정한다.
        composeRule.setContent {
            AfternoteTheme {
                AfternoteTypeFilterRow(
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier.width(100.dp),
                )
            }
        }

        val clickTargets = composeRule.scanEnabledClickTargets()
        assertEquals(TYPE_FILTER_TABS.size, clickTargets.size)
        assertTrue(
            "탭이 아닌 클릭 타깃이 생겼다: ${clickTargets.filter { it.role != Role.Tab }}",
            clickTargets.all { it.role == Role.Tab },
        )
        composeRule.onNodeWithContentDescription("더 보기").assertDoesNotExist()

        val socialNetwork = composeRule.activity.getString(R.string.afternote_category_social_network)
        composeRule.onNodeWithText(socialNetwork).performClick()

        composeRule.runOnIdle { assertEquals(AfternoteType.SOCIAL_NETWORK, selectedTab) }
    }

    @Test
    fun `카테고리 탭 행은 선택 그룹 semantics를 노출한다`() {
        composeRule.setContent {
            AfternoteTheme {
                AfternoteTypeFilterRow(
                    onTabSelected = {},
                    modifier = Modifier.width(100.dp),
                )
            }
        }

        composeRule
            .onNode(
                SemanticsMatcher.keyIsDefined(
                    SemanticsProperties.SelectableGroup,
                ),
            ).assertExists()
    }
}
