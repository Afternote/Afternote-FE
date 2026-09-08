package com.afternote.feature.mindrecord.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.afternote.core.ui.Route
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategoryUi
import com.afternote.feature.mindrecord.presentation.screen.memoryspace.MemorySpaceScreen
import com.afternote.feature.mindrecord.presentation.screen.receiver.ReceiverMindRecordScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.DailyQuestionWriteScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.DiaryWriteScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.DraftListScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.HomeScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.RecordDetailScreen
import java.time.YearMonth

/**
 * 마인드레코드 피처의 루트 [NavHost] 등록 묶음.
 *
 * [Route.MindRecord]는 바텀 탭의 기록 허브이고, [Route.MemorySpace]는 홈 MEMORIES 등에서
 * 직접 진입하는 몰입형 화면으로 IA상 분리되어 있지만, 소속 모듈·`composable` 정의는 여기서만 관리한다.
 *
 * ### 이 매핑을 무엇이 지키나 (#1562)
 *
 * 매핑이 전부 `() -> Unit` 이라 **서로 바꿔 붙여도 컴파일이 통과한다** — #1311 이 이름을 `popBack`
 * 하나로 합치면서 이름이 오배선을 드러내던 방어가 사라졌다. 두 층이 나눠 지킨다.
 *
 * - `onBackClick` 5줄 — `MindRecordBackStackAndroidTest` 가 각 화면의 뒤로가기를 **실제로 눌러**
 *   `routeOf()` 로 복귀 route 를 대조한다.
 * - `onSubmitSuccess` 2줄 — 제출 성공 경로라 계측이 지나가지 않는다(서버 제출을 태워야 해 비용이
 *   값을 넘는다). 대신 `MindRecordNavGraphWiringKonsistTest` 가 **이 소스의 매핑 자체**를 본다.
 *
 * 즉 계측은 「그 명령이 무엇을 하나」를, konsist 는 「어느 명령에 붙었나」를 본다.
 */
fun NavGraphBuilder.mindRecordNavGraph(actions: MindRecordNavActions) {
    composable<Route.MindRecord> {
        HomeScreen(
            onRecordClick = { recordId, isDiary, yearMonth ->
                // 목록이 보고 있던 달을 그대로 넘긴다. 이번 달로 고정하면 지난달 기록이
                // 상세에서 조회되지 않아 통째로 열리지 않는다 (#759 리뷰).
                actions.onOpenRecordDetail(
                    recordId = recordId,
                    isDiary = isDiary,
                    yearMonth = yearMonth.toString(),
                )
            },
            onWriteClick = { category ->
                when (category) {
                    MindRecordCategoryUi.DailyQuestion -> actions.onWriteDailyQuestion()
                    MindRecordCategoryUi.Diary -> actions.onWriteDiary()
                    MindRecordCategoryUi.WeeklyReport -> Unit
                }
            },
            onEditDailyQuestion = actions::onEditDailyQuestion,
            // 목록은 보고 있는 달의 항목만 담으므로, 그 달을 함께 넘겨 프리필 조회 범위를 좁힌다.
            // 목록이 보고 있던 달을 그대로 넘긴다. 이번 달로 고정하면 지난달 일기의
            // «수정하기» 가 프리필 없이 열리고, 그대로 저장하면 원본을 덮어쓴다 (#582 리뷰).
            onEditDiary = { diaryId, yearMonth -> actions.onEditDiary(diaryId, yearMonth.toString()) },
        )
    }
    composable<Route.MemorySpace> {
        MemorySpaceScreen(onBackClick = actions::popBack)
    }
    composable<Route.ReceiverMindRecord> {
        // 앱바 뒤로가기를 실제로 붙인다 — 없으면 이 화면이 막다른 곳이 된다 (#614).
        ReceiverMindRecordScreen(onBackClick = actions::popBack)
    }
    composable<MindRecordRoute.DailyQuestionWriteRoute> {
        DailyQuestionWriteScreen(
            // 제출 성공도 뒤로가기와 같은 «한 칸 뒤로» 다. 화면 이벤트 이름은 그대로 두고
            // 여기서 같은 명령에 붙인다 — 둘이 갈리면 이 두 줄만 달라진다 (#1311).
            onSubmitSuccess = actions::popBack,
            onBackClick = actions::popBack,
            onDraftListClick = actions::onNavigateToDraftList,
        )
    }
    composable<MindRecordRoute.DiaryWriteRoute> {
        DiaryWriteScreen(
            // 제출 성공도 뒤로가기와 같은 «한 칸 뒤로» 다. 화면 이벤트 이름은 그대로 두고
            // 여기서 같은 명령에 붙인다 — 둘이 갈리면 이 두 줄만 달라진다 (#1311).
            onSubmitSuccess = actions::popBack,
            onBackClick = actions::popBack,
            onDraftListClick = actions::onNavigateToDraftList,
        )
    }
    composable<MindRecordRoute.RecordDetailRoute> { entry ->
        RecordDetailScreen(
            isDiary = entry.toRoute<MindRecordRoute.RecordDetailRoute>().isDiary,
            // develop 에서 들어온 이 화면(#759)도 «한 칸 뒤로» 다 — 걷어낸 onWriteBack 대신
            // 같은 명령에 붙인다 (#1311).
            onBackClick = actions::popBack,
        )
    }
    composable<MindRecordRoute.DraftListRoute> {
        DraftListScreen(
            onBackClick = actions::popBack,
            onDiaryDraftClick = actions::onEditDiaryDraft,
            onDailyQuestionDraftClick = actions::onEditDailyQuestionDraft,
        )
    }
}
