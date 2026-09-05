package com.afternote.feature.afternote.presentation.editor.processing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 처리 방법 입력 필드 여닫힘 회귀 가드 (이슈 #777).
 *
 * [AddItemTextField] 는 항목을 넣거나 포커스를 잃으면 `onVisibilityChanged(false)` 로 스스로
 * 물러나겠다고 알린다. 그 신호가 [ProcessingMethodListState] 까지 닿지 않으면 항목만 추가되고
 * 빈 입력 칸은 열린 채 남는다 — 종전에는 그 신호가 `ProcessingMethodSection` 의
 * `onTextFieldVisibilityChanged: (Boolean) -> Unit = {}` 로 나가 아무도 받지 않았다.
 *
 * 상태를 끄는 경로는 [ProcessingMethodListState.hideTextField] 뿐이다([showTextField] 가
 * `private set`). 이 테스트는 그 경로가 사라지거나 토글로 되돌아가지 않도록 고정한다.
 */
class ProcessingMethodListStateTest {
    @Test
    fun `열린 입력 필드를 닫는다`() {
        val state = ProcessingMethodListState(initialShowTextField = true)

        state.hideTextField()

        assertFalse(state.showTextField)
    }

    /** 닫기는 토글이 아니다 — 이미 닫힌 상태에서 불려도 다시 열리면 안 된다. */
    @Test
    fun `이미 닫혀 있으면 닫기를 반복해도 열리지 않는다`() {
        val state = ProcessingMethodListState(initialShowTextField = false)

        state.hideTextField()
        state.hideTextField()

        assertFalse(state.showTextField)
    }

    /** 항목 추가 직후의 실제 순서: 사용자가 + 로 열고, 필드가 스스로 닫는다. */
    @Test
    fun `플러스로 연 뒤 필드가 스스로 닫으면 닫힌 상태로 남는다`() {
        val state = ProcessingMethodListState(initialShowTextField = false)

        state.toggleTextField()
        assertTrue(state.showTextField)

        state.hideTextField()

        assertFalse(state.showTextField)
    }
}
