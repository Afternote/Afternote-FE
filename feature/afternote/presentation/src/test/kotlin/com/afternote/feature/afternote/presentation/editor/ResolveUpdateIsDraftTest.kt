package com.afternote.feature.afternote.presentation.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * **「임시저장」 버튼이 발행 완료분을 초안으로 되돌리지 않는다** (#1791).
 *
 * 수정 화면은 상세에서도 열리고 임시저장 목록에서도 열리는데 버튼은 하나라, 무엇을 열었는지
 * (`EditorFlowRoute.isDraft`)로 갈라야 한다. 이 판정이 뒤집히면 수신자에게 이미 닿은 기록이
 * 홈 목록에서 사라진다.
 */
class ResolveUpdateIsDraftTest {
    @Test
    fun `발행분에서 임시저장을 누르면 키를 안 싣는다 - 서버가 저장값을 유지한다`() {
        assertNull(resolveUpdateIsDraft(asDraft = true, editingDraft = false))
    }

    @Test
    fun `임시저장을 이어쓰다 임시저장을 누르면 true 를 싣는다`() {
        assertEquals(true, resolveUpdateIsDraft(asDraft = true, editingDraft = true))
    }

    @Test
    fun `등록 버튼은 어느 경로에서든 false 를 명시해 발행으로 전환한다`() {
        assertEquals(false, resolveUpdateIsDraft(asDraft = false, editingDraft = true))
        assertEquals(false, resolveUpdateIsDraft(asDraft = false, editingDraft = false))
    }
}
