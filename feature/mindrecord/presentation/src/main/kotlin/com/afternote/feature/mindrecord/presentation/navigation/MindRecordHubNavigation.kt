package com.afternote.feature.mindrecord.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategoryUi
import com.afternote.feature.mindrecord.presentation.screen.sender.DailyQuestionWriteScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.DiaryWriteScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.DraftListScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.HomeScreen
import com.afternote.feature.mindrecord.presentation.viewmodel.DiaryWriteViewModel

/**
 * 마인드레코드 허브의 Nav3 로컬 스택 호스트 (#924 1단계 파일럿).
 *
 * 루트 NavHost(Nav2)에는 [com.afternote.core.ui.Route.MindRecord] 하나만 남고, 허브·작성·임시저장
 * 목록 간 이동은 전부 이 [NavDisplay] 의 백스택 리스트 조작으로 처리한다. 스택 루트(허브)에서의
 * 시스템 back 은 [NavDisplay] 가 소비하지 않아 루트 NavHost 로 전파된다 — 홈 탭 복귀 경로 유지.
 */
@Composable
internal fun MindRecordHubNavigation(
    onHubDepthChanged: (isAtHub: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(MindRecordRoute.HubRoute)

    // 바텀바 판정(AppState.shouldShowBottomBar)은 루트 Nav2 destination 만 보므로, 로컬 스택이
    // 허브를 벗어나면 여기서 앱 셸에 알려 바텀바를 내린다. 탭 이탈로 이 호스트가 컴포지션에서
    // 빠질 때는 true 로 되돌린다 — 다른 탭의 바텀바 판정을 오염시키지 않게.
    val isAtHub = backStack.size <= 1
    LaunchedEffect(isAtHub) { onHubDepthChanged(isAtHub) }
    DisposableEffect(Unit) {
        onDispose { onHubDepthChanged(true) }
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators =
            listOf(
                // entryDecorators 를 넘기면 기본 목록을 대체하므로 rememberSaveable 보존 데코레이터를 직접 포함.
                rememberSaveableStateHolderNavEntryDecorator(),
                // 화면 VM(hiltViewModel())을 엔트리 수명에 묶는다 — pop 시 clear, 스택 잔류 중 유지.
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<MindRecordRoute.HubRoute> {
                    HomeScreen(
                        onWriteClick = { category ->
                            when (category) {
                                MindRecordCategoryUi.DailyQuestion -> {
                                    backStack.add(MindRecordRoute.DailyQuestionWriteRoute)
                                }

                                MindRecordCategoryUi.Diary -> {
                                    backStack.add(MindRecordRoute.DiaryWriteRoute())
                                }

                                MindRecordCategoryUi.WeeklyReport -> {
                                    Unit
                                }
                            }
                        },
                    )
                }
                entry<MindRecordRoute.DailyQuestionWriteRoute> {
                    DailyQuestionWriteScreen(
                        onSubmitSuccess = { backStack.removeLastOrNull() },
                        onBackClick = { backStack.removeLastOrNull() },
                        onDraftListClick = { backStack.add(MindRecordRoute.DraftListRoute) },
                    )
                }
                entry<MindRecordRoute.DiaryWriteRoute> { key ->
                    // 라우트 인자는 Nav2 의 SavedStateHandle 주입 대신 assisted factory 로 직접 전달(#924).
                    val viewModel =
                        hiltViewModel<DiaryWriteViewModel, DiaryWriteViewModel.Factory>(
                            creationCallback = { factory -> factory.create(key) },
                        )
                    DiaryWriteScreen(
                        viewModel = viewModel,
                        onSubmitSuccess = { backStack.removeLastOrNull() },
                        onBackClick = { backStack.removeLastOrNull() },
                        onDraftListClick = { backStack.add(MindRecordRoute.DraftListRoute) },
                    )
                }
                entry<MindRecordRoute.DraftListRoute> {
                    DraftListScreen(
                        onBackClick = { backStack.removeLastOrNull() },
                        onDiaryDraftClick = { draftId, draftYearMonth ->
                            backStack.add(
                                MindRecordRoute.DiaryWriteRoute(
                                    draftId = draftId,
                                    draftYearMonth = draftYearMonth,
                                ),
                            )
                        },
                    )
                }
            },
    )
}
