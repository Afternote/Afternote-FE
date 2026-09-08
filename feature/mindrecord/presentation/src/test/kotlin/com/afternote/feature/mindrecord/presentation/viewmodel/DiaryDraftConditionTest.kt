package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 일기 등록·임시저장 조건 분리 가드 (#722).
 *
 * 종전에는 `submit(isDraft = true)` 도 정식 등록과 같은 `canSubmit` 을 써서, 제목·본문·
 * 기분이 모두 있어야만 임시저장이 됐다 — **미완성 내용을 보존하는 것이 목적인 기능이
 * 완성된 글에서만 동작**했다. 막힐 때 사유도 없었다.
 */
class DiaryDraftConditionTest {
    private fun state(
        title: String = "",
        content: String = "",
        mood: TodayMood? = null,
    ) = DiaryWriteUiState(title = title, content = content, mood = mood)

    @Test
    fun `제목만으로는 임시저장할 수 없다`() {
        // 서버가 임시저장에도 제목·본문·기분을 모두 요구한다 — 실측 400 (#1065).
        // 보내면 실패하는 조건을 «저장 가능» 으로 표시하면 버튼이 고장 난 것과 같다.
        assertFalse(state(title = "쓰다 만 제목").canSaveDraft)
    }

    @Test
    fun `본문만으로도 임시저장할 수 없다`() {
        assertFalse(state(content = "<p>쓰다 만 본문</p>").canSaveDraft)
    }

    @Test
    fun `셋이 다 있으면 임시저장된다`() {
        assertTrue(
            state(title = "제목", content = "<p>본문</p>", mood = TodayMood.HAPPY).canSaveDraft,
        )
    }

    @Test
    fun `임시저장도 무엇이 빠졌는지 알려준다`() {
        // 조용히 막으면 버튼이 죽은 것과 구분되지 않는다.
        assertEquals(R.string.mindrecord_write_diary_missing_title, state().missingForDraft())
        assertEquals(
            R.string.mindrecord_write_diary_missing_mood,
            state(title = "제목", content = "<p>본문</p>").missingForDraft(),
        )
    }

    @Test
    fun `기분만 골라서는 임시저장할 내용이 없다`() {
        assertFalse(state(mood = TodayMood.HAPPY).canSaveDraft)
    }

    @Test
    fun `빈 문단만 있는 본문은 임시저장 대상이 아니다`() {
        // 리치 에디터가 내보내는 빈 HTML 을 내용으로 세면 안 된다.
        assertFalse(state(content = "<p></p>").canSaveDraft)
    }

    @Test
    fun `정식 등록은 셋을 모두 요구한다`() {
        assertFalse(state(title = "제목", content = "<p>본문</p>").canSubmit)
        assertTrue(state(title = "제목", content = "<p>본문</p>", mood = TodayMood.SOSO).canSubmit)
    }

    @Test
    fun `무엇이 빠졌는지 순서대로 알려준다`() {
        // 회색 비활성 버튼만으로는 고장과 구분되지 않는다.
        assertEquals(R.string.mindrecord_write_diary_missing_title, state().missingForSubmit())
        assertEquals(
            R.string.mindrecord_write_diary_missing_content,
            state(title = "제목").missingForSubmit(),
        )
        assertEquals(
            R.string.mindrecord_write_diary_missing_mood,
            state(title = "제목", content = "<p>본문</p>").missingForSubmit(),
        )
        assertNull(state(title = "제목", content = "<p>본문</p>", mood = TodayMood.SAD).missingForSubmit())
    }
}
