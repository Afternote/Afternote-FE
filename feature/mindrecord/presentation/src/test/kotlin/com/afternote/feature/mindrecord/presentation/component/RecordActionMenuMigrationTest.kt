package com.afternote.feature.mindrecord.presentation.component

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import com.afternote.core.ui.R as CoreUiR
import com.afternote.feature.mindrecord.presentation.R as MindRecordR

/**
 * 「수정/삭제」 액션 메뉴가 **core:ui 정본**으로 뜨는지 (#643 · #1730).
 *
 * 종전에는 mindrecord 가 `RecordActionPopup` 을 따로 들고 있었고, 정본과 그림자(10dp ↔ 8dp)·
 * 너비(120dp 고정 ↔ 가장 긴 항목)·**항목 순서**가 달랐다. 순서는 사용자가 눈으로 바로 아는
 * 차이라 두 벌이 남아 있는 동안 화면마다 «삭제» 가 위였다 아래였다 했다.
 *
 * ### 「두 항목이 뜬다」로 끝내지 않는 이유
 *
 * 문구는 사본과 정본이 글자까지 같다(`수정하기`·`삭제하기`). 문구만 보면 사본을 되살려도
 * 통과한다. 그래서 **어느 모듈의 문자열인지**(`core_ui_action_menu_*` 를 실제로 조회)와
 * **위아래 순서**를 함께 본다.
 *
 * 사본이 지워진 것은 컴파일이 지킨다 — 파일이 없으므로 되살리려면 새로 만들어야 한다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecordActionMenuMigrationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val answer =
        DailyQuestion(
            title = "오늘 하루, 누구에게 가장 고마웠나요?",
            content = "아무 말 없이 곁을 지켜주는 사람.",
            date = LocalDate.of(2026, 8, 30),
        )

    @Test
    fun `더보기는 core 정본 메뉴를 수정 다음 삭제 순서로 연다`() {
        renderCard()
        openMenu()

        val context = composeRule.activity
        val edit = composeRule.onNodeWithText(context.getString(CoreUiR.string.core_ui_action_menu_edit))
        val delete = composeRule.onNodeWithText(context.getString(CoreUiR.string.core_ui_action_menu_delete))

        edit.assertIsDisplayed()
        delete.assertIsDisplayed()
        assertTrue(
            "정본 순서는 「수정하기 → 삭제하기」 다",
            edit.getUnclippedBoundsInRoot().top < delete.getUnclippedBoundsInRoot().top,
        )
    }

    /** 정본은 항목 콜백보다 dismiss 를 **먼저** 부른다 — 호출부가 메뉴를 따로 닫지 않아도 된다. */
    @Test
    fun `항목을 누르면 메뉴가 닫히고 콜백이 한 번 불린다`() {
        var edits = 0
        renderCard(onEdit = { edits += 1 })
        openMenu()

        composeRule
            .onNodeWithText(composeRule.activity.getString(CoreUiR.string.core_ui_action_menu_edit))
            .performClick()

        assertEquals(1, edits)
        composeRule
            .onNodeWithText(composeRule.activity.getString(CoreUiR.string.core_ui_action_menu_delete))
            .assertDoesNotExist()
    }

    private fun renderCard(
        onEdit: () -> Unit = {},
        onDelete: () -> Unit = {},
    ) {
        composeRule.setContent {
            AfternoteTheme {
                DailyQuestionListCard(
                    answer = answer,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onClick = {},
                )
            }
        }
    }

    private fun openMenu() {
        composeRule
            .onNodeWithContentDescription(composeRule.activity.getString(MindRecordR.string.mindrecord_more_menu_cd))
            .performClick()
        composeRule.waitForIdle()
    }
}
