package com.afternote.afternote_fe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.NavController
import com.afternote.core.ui.Route
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.navigation.AfternoteExternalActions
import com.afternote.feature.afternote.presentation.navigation.model.AfternoteRoute
import com.afternote.feature.home.presentation.HomeTabActions
import com.afternote.feature.home.presentation.receiver.ReceiverHomeActions
import com.afternote.feature.mindrecord.presentation.navigation.MindRecordNavActions
import com.afternote.feature.mindrecord.presentation.navigation.MindRecordRoute
import com.afternote.feature.onboarding.presentation.navigation.OnboardingExternalActions
import com.afternote.feature.receiver.presentation.navigation.model.ReceiverRoute
import com.afternote.feature.setting.presentation.navigation.SettingNavActions
import com.afternote.feature.setting.presentation.navigation.SettingRoute
import com.afternote.feature.timeletter.presentation.navigation.TimeLetterNavActions
import com.afternote.feature.timeletter.presentation.navigation.TimeLetterRoute

@Composable
fun rememberMindRecordNavActions(navController: NavController): MindRecordNavActions =
    remember(navController) {
        object : MindRecordNavActions {
            override fun popBack() {
                navController.popBackStack()
            }

            override fun onWriteDailyQuestion() {
                navController.navigate(MindRecordRoute.DailyQuestionWriteRoute())
            }

            override fun onWriteDiary() {
                navController.navigate(MindRecordRoute.DiaryWriteRoute())
            }

            override fun onNavigateToDraftList() {
                navController.navigate(MindRecordRoute.DraftListRoute)
            }

            override fun onOpenRecordDetail(
                recordId: Long,
                isDiary: Boolean,
                yearMonth: String?,
            ) {
                navController.navigate(
                    MindRecordRoute.RecordDetailRoute(
                        recordId = recordId,
                        isDiary = isDiary,
                        yearMonth = yearMonth,
                    ),
                )
            }

            override fun onEditDailyQuestion(answerId: Long) {
                navController.navigate(MindRecordRoute.DailyQuestionWriteRoute(answerId = answerId))
            }

            override fun onEditDiary(
                diaryId: Long,
                yearMonth: String,
            ) {
                navController.navigate(
                    MindRecordRoute.DiaryWriteRoute(
                        recordId = diaryId,
                        yearMonth = yearMonth,
                        // 정식 기록이라 draft 목록이 아닌 전체 목록에서 찾는다 (#582).
                        isDraft = false,
                    ),
                )
            }

            override fun onEditDailyQuestionDraft(draftId: Long) {
                navController.navigate(
                    MindRecordRoute.DailyQuestionWriteRoute(answerId = draftId, isDraft = true),
                )
            }

            override fun onEditDiaryDraft(
                draftId: Long,
                draftYearMonth: String,
            ) {
                navController.navigate(
                    MindRecordRoute.DiaryWriteRoute(recordId = draftId, yearMonth = draftYearMonth),
                )
            }
        }
    }

@Composable
fun rememberTimeLetterNavActions(navController: NavController): TimeLetterNavActions =
    remember(navController) {
        object : TimeLetterNavActions {
            override fun onSettingClick() {
                navController.navigate(Route.Setting)
            }

            override fun onNavigateToWrite() {
                navController.navigate(TimeLetterRoute.TimeLetterWriteRoute())
            }

            override fun onNavigateToEdit(timeLetterId: Long) {
                navController.navigate(TimeLetterRoute.TimeLetterWriteRoute(timeLetterId = timeLetterId))
            }

            override fun onWriteBack() {
                navController.popBackStack()
            }

            override fun onNavigateToDraft() {
                navController.navigate(TimeLetterRoute.TimeLetterDraftRoute)
            }

            override fun onDraftBack() {
                navController.popBackStack()
            }

            override fun onNavigateToRecipient() {
                navController.navigate(TimeLetterRoute.TimeLetterRecipientRoute)
            }

            override fun onRecipientBack() {
                navController.popBackStack()
            }

            override fun onNavigateToDetail(timeLetterId: Long) {
                navController.navigate(TimeLetterRoute.TimeLetterDetailRoute(timeLetterId))
            }

            override fun onDetailBack() {
                navController.popBackStack()
            }

            override fun onNavigateToRecipientFilter() {
                navController.navigate(TimeLetterRoute.TimeLetterRecipientFilterRoute)
            }

            override fun onRecipientFilterBack() {
                navController.popBackStack()
            }
        }
    }

@Composable
fun rememberSettingNavActions(appState: AppState): SettingNavActions =
    remember(appState) {
        object : SettingNavActions {
            override fun onSettingBack() {
                appState.navController.popBackStack()
            }

            override fun onLogoutSuccess() {
                appState.navController.navigate(Route.Onboarding) {
                    // 로그아웃 — 인증 이후 모든 stack 비우고 Onboarding 진입. 뒤로가기로 로그인 상태 화면에 못 돌아가게.
                    popUpTo(0) { inclusive = true }
                }
            }

            override fun onNavigateToWithdrawGuide() {
                appState.navController.navigate(SettingRoute.WithdrawGuideRoute)
            }

            override fun onNavigateToWithdrawConfirm() {
                appState.navController.navigate(SettingRoute.WithdrawConfirmRoute)
            }

            override fun onWithdrawGuideBack() {
                appState.navController.popBackStack()
            }

            override fun onWithdrawConfirmBack() {
                appState.navController.popBackStack()
            }

            override fun onWithdrawSuccess() {
                appState.navController.navigate(Route.Onboarding) {
                    // 탈퇴 — 계정 사라진 상태라 stack 전체 비우고 Onboarding 진입. 뒤로가기로 인증된 화면에 못 돌아가게.
                    popUpTo(0) { inclusive = true }
                }
            }

            override fun onNavigateToProfileEdit() {
                appState.navController.navigate(SettingRoute.ProfileEditRoute)
            }

            override fun onProfileEditBack() {
                appState.navController.popBackStack()
            }

            override fun onNavigateToLinkedAccount() {
                appState.navController.navigate(SettingRoute.LinkedAccountRoute)
            }

            override fun onLinkedAccountBack() {
                appState.navController.popBackStack()
            }

            override fun onNavigateToNotification() {
                appState.navController.navigate(SettingRoute.NotificationRoute)
            }

            override fun onNotificationBack() {
                appState.navController.popBackStack()
            }

            override fun onNavigateToRecipientList() {
                appState.navController.navigate(SettingRoute.RecipientListRoute())
            }

            override fun onNavigateToRecipientListForDeliveryConditions() {
                appState.navController.navigate(
                    SettingRoute.RecipientListRoute(selectForDeliveryConditions = true),
                )
            }

            override fun onRecipientListBack() {
                appState.navController.popBackStack()
            }

            override fun onNavigateToRecipientRegister() {
                appState.navController.navigate(SettingRoute.RecipientRegisterRoute)
            }

            override fun onRecipientRegisterBack() {
                appState.navController.popBackStack()
            }

            override fun onNavigateToRecipientEdit(receiverId: Long) {
                appState.navController.navigate(SettingRoute.RecipientEditRoute(receiverId))
            }

            override fun onRecipientEditBack() {
                appState.navController.popBackStack()
            }

            override fun onNavigateToAfterDelivery(receiverId: Long) {
                appState.navController.navigate(SettingRoute.AfterDeliveryRoute(receiverId))
            }

            override fun onAfterDeliveryBack() {
                appState.navController.popBackStack()
            }

            override fun onNavigateToPasskey() {
                appState.navController.navigate(SettingRoute.PasskeyRoute)
            }

            override fun onPasskeyBack() {
                appState.navController.popBackStack()
            }

            override fun onNavigateToPasskeyMaking() {
                appState.navController.navigate(SettingRoute.PasskeyMakingRoute)
            }

            override fun onPasskeyMakingBack() {
                appState.navController.popBackStack()
            }

            override fun onNavigateToPasskeyPassword() {
                appState.navController.navigate(SettingRoute.PasskeyPasswordRoute)
            }

            override fun onPasskeyPasswordBack() {
                appState.navController.popBackStack()
            }

            override fun onNavigateToAppLock() {
                appState.navController.navigate(SettingRoute.AppLockSetupRoute)
            }

            override fun onAppLockBack() {
                appState.navController.popBackStack()
            }

            override fun onNavigateToNotice() {
                appState.navController.navigate(SettingRoute.NoticeRoute)
            }

            override fun onNoticeBack() {
                appState.navController.popBackStack()
            }
        }
    }

@Composable
fun rememberHomeTabActions(
    appState: AppState,
    onRetryLoad: () -> Unit,
): HomeTabActions {
    val onRetryLoadState by rememberUpdatedState(onRetryLoad)
    return remember(appState) {
        object : HomeTabActions {
            override fun onRecipientChipClick() {
                // 이 칩은 «수신인 지정 미완료» 일 때만 눌린다. 그 판정이 곧 «등록된 수신자가
                // 0명» 이라(홈 요약이 getReceivers() 결과로 정한다) 목록으로 보내면 빈 화면이
                // 나오고 사용자는 다시 등록 화면을 찾아야 한다. 상태를 해소하는 화면으로 보낸다.
                //
                // 목적지가 등록인지 목록인지는 기획 확정 전이다 (#506) — docs/qa/assumptions.md 참고.
                appState.navController.navigate(SettingRoute.RecipientRegisterRoute)
            }

            override fun onAnswerClick() {
                // 카드 문구가 "데일리질문 답변하기" 라 답변 작성 화면으로 보낸다.
                // 기록 탭의 작성 진입(`MindRecordNavActions.onWriteDailyQuestion`)과 같은 목적지다.
                // 라우트가 data class 라 **인스턴스**를 넘겨야 한다 — 클래스 참조로 넘기면
                // 이동이 조용히 무산되고 홈의 이 버튼이 눌리지 않는다 (#770).
                appState.navController.navigate(MindRecordRoute.DailyQuestionWriteRoute())
            }

            override fun onNextStepClick() {
                appState.navigateToBottomBarRoute(Route.Afternote)
            }

            // 카드 문구가 «타임레터 입력하러가기» 라 타임레터 탭으로 보낸다 (#700, 2026-08-09 확정).
            override fun onTimeLetterNextStepClick() {
                appState.navigateToBottomBarRoute(Route.TimeLetter)
            }

            // TODO: 카드별 destination 디자인 확정 후 분기. 우선 마음의 기록 탭으로 임시 연결.
            override fun onWeeklyImageClick() {
                appState.navigateToBottomBarRoute(Route.MindRecord)
            }

            override fun onWeeklyCountClick() {
                appState.navigateToBottomBarRoute(Route.MindRecord)
            }

            override fun onMemoriesSectionClick() {
                appState.navController.navigate(Route.MemorySpace)
            }

            override fun onMemoriesRecordDetailClick(recordId: Long) {
                // 카드가 싣는 것은 가장 최근 **데일리질문 답변** 한 건이라 `isDiary = false` 다.
                // 카드가 나중에 일기까지 포함하게 되면 종류를 함께 넘겨야 한다 (#793).
                appState.navController.navigate(
                    MindRecordRoute.RecordDetailRoute(recordId = recordId, isDiary = false),
                )
            }

            override fun onSettingClick() {
                appState.navController.navigate(Route.Setting)
            }

            override fun onRetryLoad() {
                onRetryLoadState()
            }
        }
    }
}

/**
 * 온보딩 로컬 스택이 셸에 남긴 이동 (#1698).
 *
 * 그래프 안의 push/pop 은 `OnboardingNavHost` 가 로컬 백스택으로 직접 처리하므로, 여기엔 다른
 * 소관 그래프로 넘어가는 둘만 남는다.
 */
@Composable
fun rememberOnboardingExternalActions(appState: AppState): OnboardingExternalActions =
    remember(appState) {
        object : OnboardingExternalActions {
            override fun replaceOnboardingWithHome() {
                appState.navController.navigate(Route.Home) {
                    // 온보딩 흐름 전체를 stack 에서 비우고 Home 진입 — 뒤로가기로 온보딩으로 못 돌아가게(앱 종료).
                    popUpTo(0) { inclusive = true }
                }
            }

            override fun navigateToReceivedRecords() {
                appState.navController.navigate(Route.Receiver)
            }
        }
    }

/**
 * 애프터노트 로컬 스택이 셸에 남긴 이동·신호 (#1698).
 *
 * 지문 인증 실패 문구는 화면이 아니라 앱 셸의 snackbar 가 띄우므로 콜백으로 올라온다.
 */
@Composable
fun rememberAfternoteExternalActions(
    appState: AppState,
    onFingerprintAuthError: (String) -> Unit,
): AfternoteExternalActions {
    val onFingerprintErrorState by rememberUpdatedState(onFingerprintAuthError)
    return remember(appState) {
        object : AfternoteExternalActions {
            override fun navigateToBottomTab(tab: BottomNavTab) {
                appState.navigateToBottomBarRoute(tab.route)
            }

            override fun navigateToSetting() {
                appState.navController.navigate(Route.Setting)
            }

            override fun onFingerprintAuthFailed(message: String) {
                onFingerprintErrorState(message)
            }
        }
    }
}

/**
 * 수신자 홈에서 발생하는 다른 top-level Route(설정/마음의 기록/타임레터)와
 * 수신자 그래프 내부(애프터노트 목록) 이동을 묶은 [ReceiverHomeActions].
 *
 * 마음의 기록은 발신자/수신자 화면이 분리돼 수신자 진입은 [Route.ReceiverMindRecord] 로 라우팅한다.
 * TimeLetter 는 현재 작성자용 화면만 있어 수신자 진입 시 동일 화면이 노출된다 — 분기 후속 작업.
 */
@Composable
fun rememberReceiverHomeActions(appState: AppState): ReceiverHomeActions =
    remember(appState) {
        ReceiverHomeActions(
            onNavigateToMindRecord = { appState.navController.navigate(Route.ReceiverMindRecord) },
            onNavigateToTimeLetter = { appState.navController.navigate(Route.TimeLetter) },
            onNavigateToAfternote = { appState.navController.navigate(Route.ReceivedAfternote) },
        )
    }
