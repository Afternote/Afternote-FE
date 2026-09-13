package com.afternote.feature.afternote.presentation.receiver.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.afternote.core.ui.navigation.FeatureStackBoundary
import com.afternote.core.ui.navigation.popOrExit
import com.afternote.core.ui.navigation.popUpTo

/**
 * 수신 애프터노트 화면 콜백을 로컬 백스택 조작으로 잇는다.
 *
 * 컴포저블이 아니라 평범한 클래스다 — 백스택 «모양» 을 컴포지션 없이 재려는 것이다(#1601).
 */
internal class ReceivedAfternoteLocalNavActions(
    private val backStack: NavBackStack<NavKey>,
    private val boundary: FeatureStackBoundary,
) : ReceivedAfternoteNavActions {
    override fun popBack(): Unit = backStack.popOrExit(boundary)

    /**
     * 상세 하단 "애프터노트 확인하기" — 목록에서 상세로 들어온 왕복이라 [목록 → 상세 → 목록] 을
     * 쌓지 않고 이미 있는 목록까지 되감는다 (#777).
     */
    override fun navigateToList(): Unit = backStack.popUpTo(ReceivedAfternoteRoute.ListRoute)

    override fun navigateToDetail(afternoteId: Long) {
        backStack.add(ReceivedAfternoteRoute.DetailRoute(afternoteId = afternoteId))
    }

    override fun navigateToMemorialPlaylist(afternoteId: Long) {
        backStack.add(ReceivedAfternoteRoute.MemorialPlaylistRoute(afternoteId = afternoteId))
    }
}
