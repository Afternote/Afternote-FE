package com.afternote.feature.mindrecord.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.afternote.core.ui.Route
import com.afternote.feature.mindrecord.presentation.screen.memoryspace.MemorySpaceScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.HomeScreen

/**
 * 마인드레코드 피처의 루트 [NavHost] 등록 묶음.
 *
 * [Route.MindRecord]는 바텀 탭의 기록 허브이고, [Route.MemorySpace]는 홈 MEMORIES 등에서
 * 직접 진입하는 몰입형 화면으로 IA상 분리되어 있지만, 소속 모듈·`composable` 정의는 여기서만 관리한다.
 */
fun NavGraphBuilder.mindRecordNavGraph(actions: MindRecordNavActions) {
    composable<Route.MindRecord> {
        HomeScreen()
    }
    composable<Route.MemorySpace> {
        MemorySpaceScreen(onBackClick = actions::onMemorySpaceBack)
    }
}
