package com.afternote.feature.afternote.presentation.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * #1634 — 카테고리 필터를 건 채 조회가 실패한 상태에서 빠져나갈 수단이 남는지.
 *
 * 종전에는 이 상태가 전면 에러([com.afternote.feature.afternote.presentation.shared.component.ErrorListBody])로
 * 덮여 카테고리 행이 사라졌고, 화면에 남는 조작은 «다시 시도» 하나뿐이었다. 서버가 계속 실패하는 동안
 * 사용자는 자기가 고른 카테고리에 갇혀 다른 카테고리로도 「전체」로도 나갈 수 없었다.
 *
 * 어떤 상태에서 이 본문이 선택되는지(분기 순서)는 `AfternoteHomeBodyStateTest` 가 따로 고정한다 —
 * 화면째 띄우면 Paging 첫 상태(Loading)를 걷는 이펙트가 Robolectric 실행 순번에 걸린다
 * ([AfternoteHomeBodyState] KDoc).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteHomeFilteredErrorTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `필터 조회 실패 본문에도 카테고리 행이 남는다`() {
        composeRule.setContent { AfternoteTheme { AuthorFilteredErrorBody() } }

        composeRule.onNodeWithText(string(R.string.afternote_category_all)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.afternote_category_business)).assertIsDisplayed()
    }

    /** 실패해도 고른 카테고리는 그대로다 — 어디서 실패했는지 모르면 어디로 나갈지도 못 고른다. */
    @Test
    fun `필터 조회 실패 본문은 고른 카테고리를 선택 상태로 유지한다`() {
        composeRule.setContent { AfternoteTheme { AuthorFilteredErrorBody() } }

        composeRule.onNodeWithText(string(R.string.afternote_category_business)).assertIsSelected()
        composeRule.onNodeWithText(string(R.string.afternote_category_all)).assertIsNotSelected()
    }

    /**
     * 막다른 상태를 여는 실제 수단 — 「전체」 탭이 호출부까지 전달돼야 새 조회가 걸린다
     * ([AfternoteHomeViewModel.selectTab] 이 `selectedType` 을 바꾸면 Paging 흐름이 다시 시작된다).
     */
    @Test
    fun `필터 조회 실패 본문에서 전체를 고르면 호출부로 전달된다`() {
        var called = false
        var selected: AfternoteType? = AfternoteType.BUSINESS
        composeRule.setContent {
            AfternoteTheme {
                AuthorFilteredErrorBody(onTypeSelected = {
                    called = true
                    selected = it
                })
            }
        }

        composeRule.onNodeWithText(string(R.string.afternote_category_all)).performClick()

        assertTrue("「전체」 탭이 호출부로 전달되지 않았다", called)
        assertEquals(null, selected)
    }

    @Test
    fun `필터 조회 실패 본문은 실패 문구와 재시도를 그대로 보여준다`() {
        var retried = false
        composeRule.setContent {
            AfternoteTheme { AuthorFilteredErrorBody(onRetry = { retried = true }) }
        }

        composeRule.onNodeWithText(string(R.string.afternote_home_load_error)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.afternote_home_retry)).assertIsDisplayed().performClick()

        assertTrue("재시도가 호출부로 전달되지 않았다", retried)
    }

    /**
     * 실패를 «0건» 으로 위장하지 않는다.
     *
     * 카테고리 행을 살리려고 이 상태를 목록 경로로 흘리면 `afternote_home_filtered_empty`
     * («이 카테고리에 등록된 애프터노트가 없어요»)가 떠서, 서버 응답을 못 받은 것을 «없다» 고 단정하고
     * 재시도 수단까지 사라진다 — 무음 실패(#705)와 같은 부류다. 그 «정리» 가 들어오면 여기가 빨개진다.
     */
    @Test
    fun `필터 조회 실패를 0건 문구로 덮지 않는다`() {
        composeRule.setContent { AfternoteTheme { AuthorFilteredErrorBody() } }

        composeRule.onNodeWithText(string(R.string.afternote_home_filtered_empty)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.afternote_empty_list_body)).assertDoesNotExist()
    }

    /**
     * 이 목록 화면은 수신자와 공유물이라 발신자 조각이 새면 그대로 수신자에게 간다 (#620·#1175).
     * 이 본문이 그리는 헤더 문구는 호출부가 넘긴 것뿐이다.
     */
    @Test
    fun `수신자 호출부의 필터 실패 본문에는 발신자 문구가 없다`() {
        composeRule.setContent {
            AfternoteTheme {
                FilteredErrorBody(
                    headerDescription = stringRes(R.string.afternote_receiver_afternote_list_header_description),
                    nextStep = null,
                    selectedType = AfternoteType.BUSINESS,
                    onTypeSelected = {},
                    onRetry = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        composeRule
            .onNodeWithText(string(R.string.afternote_receiver_afternote_list_header_description))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.afternote_home_header_description)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.afternote_home_next_step_section_title)).assertDoesNotExist()
    }

    private fun string(resId: Int): String = composeRule.activity.getString(resId)

    @Composable
    private fun stringRes(resId: Int): String = stringResource(resId)

    /** [AfternoteHomeScreen] 이 «필터가 걸린 조회 실패» 에서 그리는 본문. 작성자 호출부 구성이다. */
    @Composable
    private fun AuthorFilteredErrorBody(
        onTypeSelected: (AfternoteType?) -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        FilteredErrorBody(
            headerDescription = stringRes(R.string.afternote_home_header_description),
            nextStep = null,
            selectedType = AfternoteType.BUSINESS,
            onTypeSelected = onTypeSelected,
            onRetry = onRetry,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
