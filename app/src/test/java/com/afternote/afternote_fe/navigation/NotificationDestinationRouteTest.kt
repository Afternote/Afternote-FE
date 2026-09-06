package com.afternote.afternote_fe.navigation

import com.afternote.core.common.notification.NotificationDestination
import com.afternote.core.ui.Route
import com.afternote.core.ui.bottombar.BottomNavTab
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationDestinationRouteTest {
    @Test
    fun `모든 목적지는 서로 다른 Route 로 옮겨진다`() {
        val routes = NotificationDestination.entries.map(NotificationDestination::toRoute)

        assertEquals(routes.size, routes.toSet().size)
    }

    /**
     * 알림은 앱 밖에서 오는 입력이라 목적지가 최상위(바텀바) 화면을 벗어나면 인자·권한 관문을
     * 우회하는 통로가 된다. 새 목적지를 더할 때 이 단언이 그 경계를 지킨다.
     */
    @Test
    fun `목적지 Route 는 바텀바 최상위 화면 안에 있다`() {
        val topLevelRoutes = BottomNavTab.entries.map(BottomNavTab::route).toSet()

        NotificationDestination.entries.forEach { destination ->
            assertEquals(
                true,
                destination.toRoute() in topLevelRoutes,
            )
        }
    }

    @Test
    fun `폴백 목적지는 홈이다`() {
        assertEquals(Route.Home, NotificationDestination.HOME.toRoute())
    }
}
