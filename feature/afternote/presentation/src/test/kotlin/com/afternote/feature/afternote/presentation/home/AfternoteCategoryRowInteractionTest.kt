package com.afternote.feature.afternote.presentation.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import org.junit.Assert.assertEquals
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

    @Test
    fun `더보기 화살표는 장식이고 카테고리 탭은 계속 동작한다`() {
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

        composeRule
            .onNodeWithTag(AFTERNOTE_CATEGORY_MORE_INDICATOR_TEST_TAG, useUnmergedTree = true)
            .assertExists()
            .assertHasNoClickAction()
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
