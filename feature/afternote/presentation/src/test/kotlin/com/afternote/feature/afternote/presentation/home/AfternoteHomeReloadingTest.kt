package com.afternote.feature.afternote.presentation.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
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
 * #1635 ① — 카테고리 전환 중에도 상단(헤더·카테고리 필터 행)이 남고 본문만 로딩이 되는지.
 *
 * 종전에는 전환이 만든 새 Paging 세대의 첫 상태(`refresh = Loading`)에서 0건이면 첫 진입과 똑같이
 * 판정돼 화면이 통째로 [com.afternote.core.ui.loading.LoadingBody] 로 덮였다. 그 동안 화면에는
 * **조작할 것이 하나도 없고**, 방금 탭한 카테고리가 어디로 갔는지도 보이지 않는다.
 *
 * 어떤 상태에서 이 본문이 선택되는지(전환·재시도 ↔ 첫 진입)는 `AfternoteHomeBodyStateTest` 가 따로
 * 고정한다 — 화면째 띄우면 Paging 첫 상태를 걷는 이펙트가 Robolectric 실행 순번에 걸린다
 * ([AfternoteHomeBodyState] KDoc).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteHomeReloadingTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `전환 중 로딩 본문에도 헤더와 카테고리 행이 남는다`() {
        composeRule.setContent { AfternoteTheme { AuthorReloadingBody() } }

        composeRule.onNodeWithText(string(R.string.afternote_home_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.afternote_home_header_description)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.afternote_category_all)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.afternote_category_business)).assertIsDisplayed()
    }

    /** 본문은 로딩으로 바뀐다 — 상단만 남기고 데이터 영역은 여전히 «불러오는 중» 이어야 한다. */
    @Test
    fun `전환 중 로딩 본문은 데이터 영역에 로딩 표시를 그린다`() {
        composeRule.setContent { AfternoteTheme { AuthorReloadingBody() } }

        composeRule
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate))
            .assertExists()
    }

    /** 방금 탭한 카테고리가 로딩 중에도 선택 상태로 보여야 «어디로 가는 중인지» 를 알 수 있다. */
    @Test
    fun `전환 중 로딩 본문은 방금 고른 카테고리를 선택 상태로 그린다`() {
        composeRule.setContent { AfternoteTheme { AuthorReloadingBody() } }

        composeRule.onNodeWithText(string(R.string.afternote_category_business)).assertIsSelected()
        composeRule.onNodeWithText(string(R.string.afternote_category_all)).assertIsNotSelected()
    }

    /** 「전체」로 돌아오는 전환도 이 본문을 지난다 — 그때 선택 탭은 「전체」다. */
    @Test
    fun `전체로 돌아오는 전환 중에는 전체 탭이 선택 상태다`() {
        composeRule.setContent { AfternoteTheme { AuthorReloadingBody(selectedType = null) } }

        composeRule.onNodeWithText(string(R.string.afternote_category_all)).assertIsSelected()
    }

    /**
     * 남긴 행이 «눌러도 아무 일 없는» 장식이면 로딩이 길어질 때 그 자체가 막다른 상태가 된다
     * (#620·#777 과 같은 규칙). 응답을 기다리는 동안에도 다른 카테고리로 갈아탈 수 있어야 한다.
     */
    @Test
    fun `전환 중에도 다른 카테고리를 고르면 호출부로 전달된다`() {
        var called = false
        var selected: AfternoteType? = AfternoteType.BUSINESS
        composeRule.setContent {
            AfternoteTheme {
                AuthorReloadingBody(onTypeSelected = {
                    called = true
                    selected = it
                })
            }
        }

        composeRule.onNodeWithText(string(R.string.afternote_category_all)).performClick()

        assertTrue("카테고리 탭이 호출부로 전달되지 않았다", called)
        assertEquals(null, selected)
    }

    /**
     * #1633 이 갈라 둔 0건 문구 두 종(#567)이 로딩 상태로 새지 않는다.
     *
     * 아직 응답이 오지 않은 것을 «없다» 고 단정하면 무음 실패(#705)와 같은 부류가 된다.
     */
    @Test
    fun `전환 중 로딩 본문은 0건 문구를 쓰지 않는다`() {
        composeRule.setContent { AfternoteTheme { AuthorReloadingBody() } }

        composeRule.onNodeWithText(string(R.string.afternote_home_filtered_empty)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.afternote_empty_list_body)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.afternote_home_load_error)).assertDoesNotExist()
    }

    /**
     * 이 목록 화면은 수신자와 공유물이라 발신자 조각이 새면 그대로 수신자에게 간다 (#620·#1175).
     * 이 본문이 그리는 헤더 문구는 호출부가 넘긴 것뿐이다.
     */
    @Test
    fun `수신자 호출부의 전환 중 로딩 본문에는 발신자 문구가 없다`() {
        composeRule.setContent {
            AfternoteTheme {
                ReloadingBody(
                    headerDescription = stringRes(R.string.afternote_receiver_afternote_list_header_description),
                    nextStep = null,
                    selectedType = AfternoteType.BUSINESS,
                    onTypeSelected = {},
                    filterRowScrollState = rememberScrollState(),
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

    /** [AfternoteHomeScreen] 이 «상단을 보고 있던 중의 로드» 에서 그리는 본문. 작성자 호출부 구성이다. */
    @Composable
    private fun AuthorReloadingBody(
        selectedType: AfternoteType? = AfternoteType.BUSINESS,
        onTypeSelected: (AfternoteType?) -> Unit = {},
    ) {
        ReloadingBody(
            headerDescription = stringRes(R.string.afternote_home_header_description),
            nextStep = null,
            selectedType = selectedType,
            onTypeSelected = onTypeSelected,
            filterRowScrollState = rememberScrollState(),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
