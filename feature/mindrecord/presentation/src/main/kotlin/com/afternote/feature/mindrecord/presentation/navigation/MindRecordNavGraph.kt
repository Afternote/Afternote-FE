package com.afternote.feature.mindrecord.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.afternote.core.ui.Route
import com.afternote.feature.mindrecord.presentation.screen.memoryspace.MemorySpaceScreen
import com.afternote.feature.mindrecord.presentation.screen.receiver.ReceiverMindRecordScreen

/**
 * 마인드레코드 피처의 루트 [NavHost] 등록 묶음.
 *
 * [Route.MindRecord]는 바텀 탭의 기록 허브이고, [Route.MemorySpace]는 홈 MEMORIES 등에서
 * 직접 진입하는 몰입형 화면으로 IA상 분리되어 있지만, 소속 모듈·`composable` 정의는 여기서만 관리한다.
 *
 * #924 Nav3 파일럿: 허브 내부 화면(작성·임시저장 목록)의 루트 등록은 사라지고
 * [MindRecordHubNavigation] 의 로컬 스택으로 이동했다. [Route.MemorySpace]·[Route.ReceiverMindRecord]는
 * IA상 허브 외부 진입점이라 Nav2 에 남긴다.
 */
fun NavGraphBuilder.mindRecordNavGraph(
    actions: MindRecordNavActions,
    /** 허브 로컬 스택이 루트(허브 화면)인지 — 앱 셸의 바텀바 표시 판정에 쓰인다. */
    onHubDepthChanged: (isAtHub: Boolean) -> Unit,
) {
    composable<Route.MindRecord> {
        MindRecordHubNavigation(onHubDepthChanged = onHubDepthChanged)
    }
    composable<Route.MemorySpace> {
        MemorySpaceScreen(onBackClick = actions::onMemorySpaceBack)
    }
    composable<Route.ReceiverMindRecord> {
        ReceiverMindRecordScreen()
    }
}
