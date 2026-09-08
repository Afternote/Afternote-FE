package com.afternote.feature.afternote.presentation.receiver.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.afternote.core.ui.navigation.FeatureStackBoundary
import org.junit.Assert.assertEquals
import org.junit.Test

/** 수신 애프터노트 로컬 백스택 회귀 기준 (#1698). */
class ReceivedAfternoteLocalNavActionsTest {
    private var exits = 0
    private val backStack = NavBackStack<NavKey>(ReceivedAfternoteRoute.ListRoute)
    private val actions =
        ReceivedAfternoteLocalNavActions(
            backStack = backStack,
            boundary = FeatureStackBoundary { exits += 1 },
        )

    private fun stack(): List<String> = backStack.map { it::class.simpleName!! }

    @Test
    fun `상세와 추억 플레이리스트는 목록 위에 쌓인다`() {
        actions.navigateToDetail(afternoteId = 11L)
        actions.navigateToMemorialPlaylist(afternoteId = 11L)

        assertEquals(listOf("ListRoute", "DetailRoute", "MemorialPlaylistRoute"), stack())
    }

    @Test
    fun `상세의 애프터노트 확인하기는 목록을 두 번 쌓지 않는다`() {
        actions.navigateToDetail(afternoteId = 11L)

        actions.navigateToList()

        // 목록 → 상세 → 목록 왕복이라, 뒤로가기가 방금 나온 상세로 되돌아가면 안 된다 (#777).
        assertEquals(listOf("ListRoute"), stack())
    }

    @Test
    fun `목록이 없는 진입에서도 목록 하나만 남는다`() {
        backStack.clear()
        backStack.add(ReceivedAfternoteRoute.DetailRoute(afternoteId = 11L))

        actions.navigateToList()

        assertEquals(listOf("ListRoute"), stack())
    }

    @Test
    fun `스택 바닥에서의 뒤로가기는 스택을 비우지 않고 셸에 넘긴다`() {
        actions.popBack()

        assertEquals(listOf("ListRoute"), stack())
        assertEquals(1, exits)
    }
}
