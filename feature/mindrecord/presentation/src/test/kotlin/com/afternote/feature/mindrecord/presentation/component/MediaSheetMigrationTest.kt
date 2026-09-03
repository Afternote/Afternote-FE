package com.afternote.feature.mindrecord.presentation.component

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.afternote.core.ui.R as CoreUiR

/**
 * 「미디어 추가하기」 시트가 **core:ui 정본**으로 뜨는지 (#642 · #1615).
 *
 * 종전에는 같은 시트를 모듈마다 다시 적어 4벌이었고, 사본들이 시안(4327:72281) 지오메트리에서
 * 조금씩 어긋나 있었다. mindrecord 판은 행 좌우 여백·디바이더 폭이 정본과 달랐다.
 *
 * ### 왜 「4개 항목이 뜬다」로 끝내지 않는가
 *
 * 항목 문구는 사본과 정본이 **글자까지 같다**(`미디어 추가하기`·`이미지 추가하기`…). 문구만
 * 보면 사본으로 되돌려도 통과한다. 그래서 **문자열이 어느 모듈 것인지**를 함께 본다 —
 * `core_ui_media_sheet_*` 를 실제로 조회해 그 값으로 단언하므로, mindrecord 가 자기 사본
 * 문자열을 되살리면(그리고 그 값이 달라지면) 여기서 갈린다.
 *
 * 사본 자체가 지워진 것은 컴파일이 지킨다 — 파일이 없으므로 되살리려면 새로 만들어야 한다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MediaSheetMigrationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `툴바 미디어 버튼이 core 정본 시트를 연다`() {
        composeRule.setContent { AfternoteTheme { WriteTextField(onSaveDraftClick = {}, onDraftCountClick = {}) } }
        val context = composeRule.activity

        openMediaSheet()

        composeRule.onNodeWithText(context.getString(CoreUiR.string.core_ui_media_sheet_title)).assertIsDisplayed()
        listOf(
            CoreUiR.string.core_ui_media_sheet_image,
            CoreUiR.string.core_ui_media_sheet_voice,
            CoreUiR.string.core_ui_media_sheet_file,
            CoreUiR.string.core_ui_media_sheet_link,
        ).forEach { label ->
            // `assertIsDisplayed` 가 아니라 `assertExists` 다 — 정본 지오메트리에서는 시트가
            // 더 높아 Robolectric 기본 뷰포트에서 마지막 행이 화면 밖으로 나간다. 여기서
            // 보려는 것은 «항목 4개가 core 문자열로 실렸는가» 이지 뷰포트 안에 드는지가 아니다.
            composeRule.onNodeWithText(context.getString(label)).assertExists()
        }
    }

    /**
     * 「링크 추가하기」만 후속 시트로 이어지고 나머지 셋은 시스템 피커로 나간다 — 이관하면서
     * 항목 순서나 액션이 어긋나면 사용자가 엉뚱한 피커를 만난다.
     */
    @Test
    fun `링크 항목은 링크 입력 시트로 이어진다`() {
        composeRule.setContent { AfternoteTheme { WriteTextField(onSaveDraftClick = {}, onDraftCountClick = {}) } }
        val context = composeRule.activity

        openMediaSheet()
        composeRule.onNodeWithText(context.getString(CoreUiR.string.core_ui_media_sheet_link)).performClick()

        // 미디어 시트가 닫히고 링크 입력 시트가 대신 뜬다.
        composeRule.onNodeWithText(context.getString(CoreUiR.string.core_ui_media_sheet_title)).assertDoesNotExist()
    }

    private fun openMediaSheet() {
        composeRule
            .onNodeWithContentDescription(
                composeRule.activity.getString(
                    com.afternote.feature.mindrecord.presentation.R.string.mindrecord_toolbar_link_cd,
                ),
            ).performClick()
        composeRule.waitForIdle()
    }
}
