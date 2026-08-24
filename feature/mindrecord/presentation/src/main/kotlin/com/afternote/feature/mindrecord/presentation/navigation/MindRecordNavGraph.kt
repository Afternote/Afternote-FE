package com.afternote.feature.mindrecord.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.afternote.core.ui.Route
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategoryUi
import com.afternote.feature.mindrecord.presentation.screen.memoryspace.MemorySpaceScreen
import com.afternote.feature.mindrecord.presentation.screen.receiver.ReceiverMindRecordScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.DailyQuestionWriteScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.DiaryWriteScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.DraftListScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.HomeScreen
import java.time.YearMonth

/**
 * 마인드레코드 피처의 루트 [NavHost] 등록 묶음.
 *
 * [Route.MindRecord]는 바텀 탭의 기록 허브이고, [Route.MemorySpace]는 홈 MEMORIES 등에서
 * 직접 진입하는 몰입형 화면으로 IA상 분리되어 있지만, 소속 모듈·`composable` 정의는 여기서만 관리한다.
 */
fun NavGraphBuilder.mindRecordNavGraph(actions: MindRecordNavActions) {
    composable<Route.MindRecord> {
        HomeScreen(
            onWriteClick = { category ->
                when (category) {
                    MindRecordCategoryUi.DailyQuestion -> actions.onWriteDailyQuestion()
                    MindRecordCategoryUi.Diary -> actions.onWriteDiary()
                    MindRecordCategoryUi.WeeklyReport -> Unit
                }
            },
            onEditDailyQuestion = actions::onEditDailyQuestion,
            // 목록은 보고 있는 달의 항목만 담으므로, 그 달을 함께 넘겨 프리필 조회 범위를 좁힌다.
            onEditDiary = { diaryId -> actions.onEditDiary(diaryId, YearMonth.now().toString()) },
        )
    }
    composable<Route.MemorySpace> {
        MemorySpaceScreen(onBackClick = actions::onMemorySpaceBack)
    }
    composable<Route.ReceiverMindRecord> {
        // 앱바 뒤로가기를 실제로 붙인다 — 없으면 이 화면이 막다른 곳이 된다 (#614).
        ReceiverMindRecordScreen(onBackClick = actions::onReceiverMindRecordBack)
    }
    composable<MindRecordRoute.DailyQuestionWriteRoute> {
        DailyQuestionWriteScreen(
            onSubmitSuccess = actions::onWriteSubmitSuccess,
            onBackClick = actions::onWriteBack,
            onDraftListClick = actions::onNavigateToDraftList,
        )
    }
    composable<MindRecordRoute.DiaryWriteRoute> {
        DiaryWriteScreen(
            onSubmitSuccess = actions::onWriteSubmitSuccess,
            onBackClick = actions::onWriteBack,
            onDraftListClick = actions::onNavigateToDraftList,
        )
    }
    composable<MindRecordRoute.DraftListRoute> {
        DraftListScreen(
            onBackClick = actions::onDraftListBack,
            onDiaryDraftClick = actions::onEditDiaryDraft,
            onDailyQuestionDraftClick = actions::onEditDailyQuestionDraft,
        )
    }
}
