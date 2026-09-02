package com.afternote.feature.afternote.presentation.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.component.EmptyListBody
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * #1175 — 애프터노트가 0건이고 카테고리 필터도 없는 «첫 진입» 상태에서도 헤더가 그려지는지.
 *
 * 종전에는 헤더(제목·설명·NEXT STEP 슬롯)가 `InfiniteListBody` 안에만 있고 그 경로는
 * `itemCount > 0 || selectedType != null` 일 때만 탔다. 그래서 첫 진입 사용자에게는 화면 제목
 * `afternote_home_title` 조차 **한 번도 그려진 적이 없다.** 서버 데이터와 무관한 구조 결함이다.
 *
 * 판정을 [AfternoteHomeScreen] 이 아니라 본문 컴포저블([EmptyHomeBody]·[EmptyListBody])로 내리는 이유는
 * `AfternoteHomeEmptyCopyTest`·`ReceiverAfternoteListChromeTest` 와 같다 — 화면은 `loadState.refresh` 가
 * Loading 인 동안 초기 로딩을 그리는데 `collectAsLazyPagingItems` 의 첫 상태가 바로 그 Loading 이라,
 * Robolectric 클래스가 여럿 누적된 뒤 실행되면 이펙트가 첫 단언까지 진행되지 않아 순번에 따라 결과가 갈린다.
 *
 * NEXT STEP 문구를 만드는 원천은 아직 없다(Afternote-BE#270). 여기서는 값이 **주어졌을 때** 카드가
 * 헤더 아래 제자리에 붙고 탭이 전달되는지까지만 고정한다 — 문구 자체는 이 PR 이 만들지 않는다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteHomeEmptyHeaderTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `무필터 0건에서도 화면 제목과 헤더 설명을 보여준다`() {
        composeRule.setContent { AfternoteTheme { AuthorEmptyBody() } }

        composeRule.onNodeWithText(string(R.string.afternote_home_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.afternote_home_header_description)).assertIsDisplayed()
    }

    @Test
    fun `무필터 0건 본문은 빈 목록 안내 문구를 그대로 유지한다`() {
        composeRule.setContent { AfternoteTheme { AuthorEmptyBody() } }

        composeRule.onNodeWithText(string(R.string.afternote_empty_list_body)).assertExists()
    }

    @Test
    fun `NEXT STEP 원천이 없으면 빈 목록 헤더에 카드를 그리지 않는다`() {
        composeRule.setContent { AfternoteTheme { AuthorEmptyBody() } }

        composeRule.onNodeWithText(string(R.string.afternote_home_next_step_section_title)).assertDoesNotExist()
    }

    @Test
    fun `NEXT STEP 이 있으면 빈 목록 헤더에도 카드가 뜨고 탭이 전달된다`() {
        var tapped = false
        composeRule.setContent {
            AfternoteTheme {
                EmptyHomeBody(
                    headerDescription = stringRes(R.string.afternote_home_header_description),
                    nextStep = NextStep(text = NEXT_STEP_TEXT, onClick = { tapped = true }),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.afternote_home_next_step_section_title)).assertIsDisplayed()
        composeRule.onNodeWithText(NEXT_STEP_TEXT).assertIsDisplayed().performClick()

        assertTrue("NEXT STEP 카드 탭이 호출부로 전달되지 않았다", tapped)
    }

    /**
     * 헤더를 올리지 않기로 한 호출부(수신자 목록 — `showsHeaderOnEmptyList = false`)의 빈 본문.
     *
     * 이 목록 화면은 작성자와 수신자가 공유하므로, 발신자 헤더가 [EmptyListBody] 자체로 내려가면
     * 그 순간 수신자에게도 새어 나간다 — 발신자 문구가 수신자에게 새던 것이 #620 이다. 그 통로를 막는다.
     */
    @Test
    fun `헤더를 올리지 않은 빈 본문에는 발신자 제목도 문구도 없다`() {
        composeRule.setContent { AfternoteTheme { EmptyListBody(modifier = Modifier.fillMaxSize()) } }

        composeRule.onNodeWithText(string(R.string.afternote_empty_list_body)).assertExists()
        composeRule.onNodeWithText(string(R.string.afternote_home_title)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.afternote_home_header_description)).assertDoesNotExist()
    }

    private fun string(resId: Int): String = composeRule.activity.getString(resId)

    @Composable
    private fun stringRes(resId: Int): String = stringResource(resId)

    /** [AfternoteHomeScreen] 이 작성자 호출부(`showsHeaderOnEmptyList = true`)에서 그리는 빈 본문. */
    @Composable
    private fun AuthorEmptyBody() {
        EmptyHomeBody(
            headerDescription = stringRes(R.string.afternote_home_header_description),
            nextStep = null,
            modifier = Modifier.fillMaxSize(),
        )
    }

    private companion object {
        /** 서버 계약(Afternote-BE#270)이 없어 실문구가 아니다 — 슬롯 배선만 보는 값. */
        const val NEXT_STEP_TEXT = "다음 단계 진행하기"
    }
}
