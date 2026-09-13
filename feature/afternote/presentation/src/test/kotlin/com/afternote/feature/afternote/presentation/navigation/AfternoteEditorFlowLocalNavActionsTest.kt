package com.afternote.feature.afternote.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.afternote.core.ui.navigation.FeatureStackBoundary
import com.afternote.feature.afternote.presentation.navigation.model.AfternoteRoute
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 에디터 흐름 로컬 백스택 회귀 기준 (#1601 · #1698).
 *
 * 흐름 네 화면은 하나의 entry 안에서 스택을 이룬다 — 바닥에서의 back 은 흐름 자체를 닫고,
 * 수신자 선택은 결과를 공유 ViewModel 에 넘긴 뒤 에디터로 돌아온다.
 */
class AfternoteEditorFlowLocalNavActionsTest {
    private var exits = 0
    private var savedHome = 0
    private val confirmedReceivers = mutableListOf<List<Long>>()
    private val flowStack = NavBackStack<NavKey>(AfternoteRoute.EditorRoute)
    private val actions =
        AfternoteEditorFlowLocalNavActions(
            flowStack = flowStack,
            boundary = FeatureStackBoundary { exits += 1 },
            onReceiversSelected = { confirmedReceivers += it },
            onSaveSuccessNavigateHome = { savedHome += 1 },
        )

    private fun stack(): List<String> = flowStack.map { it::class.simpleName!! }

    @Test
    fun `추억 플레이리스트와 곡 추가는 에디터 위에 쌓인다`() {
        actions.navigateToMemorialPlaylist()
        actions.navigateToAddSong()

        assertEquals(listOf("EditorRoute", "MemorialPlaylistRoute", "AddSongRoute"), stack())

        actions.popBack()
        assertEquals(listOf("EditorRoute", "MemorialPlaylistRoute"), stack())
    }

    @Test
    fun `수신자 선택은 확정한 전체 목록을 공유 ViewModel 에 넘기고 에디터로 돌아온다`() {
        actions.navigateToSelectReceiver()
        assertEquals(listOf("EditorRoute", "SelectReceiverRoute"), stack())

        // 확정은 «추가분» 이 아니라 폼 수신자 전체다 — 한 번의 쓰기로 넘어가야 교체가 성립한다 (#1426).
        actions.popBackWithSelectedReceivers(receiverIds = listOf(7L, 9L))

        assertEquals(listOf(listOf(7L, 9L)), confirmedReceivers)
        assertEquals(listOf("EditorRoute"), stack())
    }

    @Test
    fun `흐름 바닥에서의 뒤로가기는 흐름을 통째로 닫는다`() {
        actions.popBack()

        assertEquals(listOf("EditorRoute"), stack())
        assertEquals(1, exits)
    }

    @Test
    fun `저장 성공은 바깥 스택이 홈까지 되감도록 넘긴다`() {
        actions.popToAfternoteHome()

        assertEquals(1, savedHome)
    }
}
