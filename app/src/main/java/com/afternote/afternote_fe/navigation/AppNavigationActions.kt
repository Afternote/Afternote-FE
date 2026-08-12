package com.afternote.afternote_fe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.NavController
import com.afternote.afternote_fe.screen.HomeTabActions
import com.afternote.afternote_fe.screen.receiver.ReceiverHomeActions
import com.afternote.core.model.MindRecordCategory
import com.afternote.core.ui.Route
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.navigation.AfternoteNavActions
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute
import com.afternote.feature.afternote.presentation.receiver.navigation.ReceiverNavActions
import com.afternote.feature.afternote.presentation.receiver.navigation.model.ReceiverRoute
import com.afternote.feature.mindrecord.presentation.navigation.MindRecordNavActions
import com.afternote.feature.mindrecord.presentation.navigation.MindRecordRoute
import com.afternote.feature.onboarding.presentation.navigation.OnboardingNavActions
import com.afternote.feature.onboarding.presentation.navigation.OnboardingRoute
import com.afternote.feature.setting.presentation.navigation.SettingNavActions
import com.afternote.feature.setting.presentation.navigation.SettingRoute
import com.afternote.feature.timeletter.presentation.navigation.TimeLetterNavActions
import com.afternote.feature.timeletter.presentation.navigation.TimeLetterRoute

@Composable
fun rememberOnboardingNavActions(navController: NavController): OnboardingNavActions =
    remember(navController) {
        object : OnboardingNavActions {
            override fun replaceOnboardingWithHome() {
                navController.navigate(Route.Home) {
                    // 온보딩 흐름 전체를 stack 에서 비우고 Home 진입 — 뒤로가기로 온보딩으로 못 돌아가게(앱 종료).
                    popUpTo(0) { inclusive = true }
                }
            }

            override fun replaceLoginWithWelcome() {
                navController.navigate(OnboardingRoute.WelcomeRoute) {
                    // 소셜 신규 가입자 — Login(과 그 아래 Welcome)을 비우고 새 Welcome 진입. 뒤로가기로 Login 에 못 돌아가게.
                    popUpTo<OnboardingRoute.WelcomeRoute> { inclusive = true }
                    launchSingleTop = true
                }
            }

            override fun navigateToSignUp() {
                navController.navigate(OnboardingRoute.SignUpRoute)
            }

            override fun navigateToLogin() {
                navController.navigate(OnboardingRoute.LoginRoute)
            }

            override fun navigateToReceivedRecords() {
                navController.navigate(Route.Receiver)
            }

            override fun replaceLoginWithSignUp() {
                navController.navigate(OnboardingRoute.SignUpRoute) {
                    // Login 화면을 SignUp 으로 교체 — 뒤로가기 시 Login 으로 돌아가지 않고 그 이전(Welcome) 으로.
                    popUpTo<OnboardingRoute.LoginRoute> { inclusive = true }
                }
            }

            override fun popBack() {
                navController.popBackStack()
            }

            override fun navigateToFindId() {
                navController.navigate(OnboardingRoute.FindIdRoute)
            }

            override fun proceedToSignUpResidentNumber() {
                navController.navigate(OnboardingRoute.SignUpResidentNumberRoute)
            }

            override fun proceedToSignUpPassword() {
                navController.navigate(OnboardingRoute.SignUpPasswordRoute)
            }

            override fun proceedToTerms() {
                navController.navigate(OnboardingRoute.TermsRoute)
            }

            override fun proceedToProfile() {
                navController.navigate(OnboardingRoute.ProfileRoute)
            }

            override fun navigateToTermsDetail() {
                navController.navigate(OnboardingRoute.TermsDetailRoute)
            }
        }
    }

@Composable
fun rememberMindRecordNavActions(navController: NavController): MindRecordNavActions =
    remember(navController) {
        object : MindRecordNavActions {
            override fun onMemorySpaceBack() {
                navController.popBackStack()
            }

            override fun onWriteDailyQuestion() {
                navController.navigate(MindRecordRoute.DailyQuestionWriteRoute)
            }

            override fun onWriteDiary() {
                navController.navigate(MindRecordRoute.DiaryWriteRoute())
            }

            override fun onWriteBack() {
                navController.popBackStack()
            }

            override fun onWriteSubmitSuccess() {
                navController.popBackStack()
            }

            override fun onNavigateToDraftList() {
                navController.navigate(MindRecordRoute.DraftListRoute)
            }

            override fun onDraftListBack() {
                navController.popBackStack()
            }

            override fun onEditDiaryDraft(
                draftId: Long,
                draftYearMonth: String,
            ) {
                navController.navigate(
                    MindRecordRoute.DiaryWriteRoute(draftId = draftId, draftYearMonth = draftYearMonth),
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

            override fun onNavigateToInquiry() {
                appState.navController.navigate(SettingRoute.InquiryListRoute)
            }

            override fun onInquiryBack() {
                appState.navController.popBackStack()
            }

            override fun onNavigateToInquiryDetail(inquiryId: Long) {
                appState.navController.navigate(SettingRoute.InquiryDetailRoute(inquiryId))
            }

            override fun onNavigateToInquiryWrite() {
                appState.navController.navigate(SettingRoute.InquiryWriteRoute)
            }

            override fun onInquirySubmitted() {
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
                // TODO: 수신인 지정 화면 Route 추가 후 연결
            }

            override fun onAnswerClick() {
                // TODO: 데일리 질문 답변 화면 Route 추가 후 연결
            }

            override fun onNextStepClick() {
                appState.navigateToBottomBarRoute(Route.Afternote)
            }

            override fun onRecordCategoryClick(category: MindRecordCategory) {
                appState.navController.navigate(Route.MindRecord)
            }

            // TODO: 카드별 destination 디자인 확정 후 분기. 우선 마음의 기록 탭으로 임시 연결.
            override fun onWeeklyImageClick() {
                appState.navigateToBottomBarRoute(Route.MindRecord)
            }

            override fun onWeeklyCountClick() {
                appState.navigateToBottomBarRoute(Route.MindRecord)
            }

            override fun onWeeklyRecentRecordClick() {
                appState.navigateToBottomBarRoute(Route.MindRecord)
            }

            override fun onMemoriesSectionClick() {
                appState.navController.navigate(Route.MemorySpace)
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
 * Afternote 서브그래프에 넘길 루트 레벨 네비게이션 [AfternoteNavActions] 구현체.
 */
@Composable
fun rememberAfternoteNavActions(
    appState: AppState,
    onFingerprintAuthError: (String) -> Unit,
): AfternoteNavActions {
    val onFingerprintErrorState by rememberUpdatedState(onFingerprintAuthError)
    return remember(appState) {
        object : AfternoteNavActions {
            override fun navigateToBottomTab(tab: BottomNavTab) {
                appState.navigateToBottomBarRoute(tab.route)
            }

            override fun popBack() {
                appState.navController.popBackStack()
            }

            override fun navigateToAfternoteDetail(itemId: String) {
                appState.navController.navigate(AfternoteRoute.DetailRoute(itemId = itemId))
            }

            override fun navigateToGalleryDetail(itemId: String) {
                appState.navController.navigate(AfternoteRoute.GalleryDetailRoute(itemId = itemId))
            }

            override fun navigateToMemorialDetail(itemId: String) {
                appState.navController.navigate(
                    AfternoteRoute.MemorialDetailRoute(itemId = itemId),
                )
            }

            override fun navigateToNewEditor(initialCategory: String?) {
                appState.navController.navigate(AfternoteRoute.EditorRoute(initialCategory = initialCategory))
            }

            override fun navigateToEditorForEdit(
                itemId: String,
                initialCategory: EditorCategory,
            ) {
                appState.navController.navigate(
                    AfternoteRoute.EditorRoute(
                        itemId = itemId,
                        initialCategory = initialCategory.name,
                    ),
                )
            }

            override fun navigateToMemorialPlaylist() {
                appState.navController.navigate(AfternoteRoute.MemorialPlaylistRoute)
            }

            override fun navigateToAddSong() {
                appState.navController.navigate(AfternoteRoute.AddSongRoute)
            }

            override fun replaceFingerprintLoginWithAfternoteHome() {
                appState.navController.navigate(AfternoteRoute.AfternoteHomeRoute) {
                    // 지문 로그인 화면 pop — 인증 성공 후 뒤로가기로 다시 입력 요구하는 화면에 못 돌아가게.
                    popUpTo<AfternoteRoute.FingerprintLoginRoute> { inclusive = true }
                    launchSingleTop = true
                }
            }

            override fun onFingerprintAuthFailed(message: String) {
                onFingerprintErrorState(message)
            }

            override fun popToAfternoteHome() {
                appState.navController.navigate(AfternoteRoute.AfternoteHomeRoute) {
                    // 저장 성공 — Home 위의 화면들(에디터·미디어 선택 등) 만 pop, Home 자체는 유지(inclusive=false).
                    // launchSingleTop 으로 새 Home 인스턴스 생성 대신 기존 Home 위로 복귀.
                    popUpTo<AfternoteRoute.AfternoteHomeRoute> { inclusive = false }
                    launchSingleTop = true
                }
            }

            override fun navigateToSetting() {
                appState.navController.navigate(Route.Setting)
            }
        }
    }
}

/**
 * 수신자 서브그래프에 넘길 그래프 내부 [ReceiverNavActions] 구현체.
 *
 * 본인 확인 캐시 분기(Intro→MasterKey)는 nested 그래프 진입 후 `DeliveryVerificationFlowViewModel` 에서
 * 자동 처리되므로(#220) 본 actions 는 순수 네비게이션만 수행한다. authCode 같은 Repository 사이드이펙트는
 * 각 화면 ViewModel 에서 처리.
 */
@Composable
fun rememberReceiverNavActions(appState: AppState): ReceiverNavActions =
    remember(appState) {
        object : ReceiverNavActions {
            override fun popBack() {
                appState.navController.popBackStack()
            }

            override fun navigateToAfternoteList() {
                appState.navController.navigate(ReceiverRoute.AfternoteListRoute)
            }

            override fun navigateToReceivedAfternoteDetail(afternoteId: String) {
                appState.navController.navigate(
                    ReceiverRoute.AfternoteDetailRoute(afternoteId = afternoteId),
                )
            }

            override fun navigateToMemorialPlaylist(afternoteId: String) {
                appState.navController.navigate(
                    ReceiverRoute.MemorialPlaylistRoute(afternoteId = afternoteId),
                )
            }

            override fun navigateToSenderRegistration() {
                appState.navController.navigate(ReceiverRoute.SenderRegistrationRoute)
            }

            override fun navigateToSenderDetail(senderId: String) {
                appState.navController.navigate(
                    ReceiverRoute.SenderDetailRoute(senderId = senderId),
                )
            }

            override fun navigateToDeliveryVerificationFlow(senderId: String) {
                // nested 열람 신청 흐름 그래프 진입. 본인 확인 캐시 분기는 IntroRoute 의 LaunchedEffect 가 처리.
                appState.navController.navigate(
                    ReceiverRoute.DeliveryVerificationFlowRoute(senderId = senderId),
                )
            }

            override fun navigateToIdentityVerificationEmail() {
                appState.navController.navigate(ReceiverRoute.IdentityVerificationEmailRoute)
            }

            override fun proceedToMasterKey() {
                // 두 진입 경로 (Intro 의 캐시 hit jump / Email 인증 성공) 공통 — Intro 까지 pop 해서
                // 뒤로가기로 본인 확인 화면들에 돌아오지 않게.
                appState.navController.navigate(ReceiverRoute.MasterKeyRoute) {
                    popUpTo<ReceiverRoute.IdentityVerificationIntroRoute> { inclusive = true }
                }
            }

            override fun proceedToDocumentUpload() {
                appState.navController.navigate(ReceiverRoute.DocumentUploadRoute) {
                    // 마스터 키 검증 성공 직후 — MasterKey 화면 pop. 뒤로가기로 이미 검증된 마스터 키 재입력 화면에 못 돌아가게.
                    popUpTo<ReceiverRoute.MasterKeyRoute> { inclusive = true }
                }
            }

            override fun proceedToDeliveryVerificationComplete() {
                appState.navController.navigate(ReceiverRoute.DeliveryVerificationCompleteRoute) {
                    // 서류 제출 성공 직후 — DocumentUpload pop. 뒤로가기로 제출 끝난 업로드 화면에 못 돌아가게.
                    popUpTo<ReceiverRoute.DocumentUploadRoute> { inclusive = true }
                }
            }

            override fun popToReceivedRecords() {
                // 받은 기록함을 남기고 신청 흐름 화면들(완료/서류/마스터 키)을 모두 pop.
                appState.navController.navigate(ReceiverRoute.ReceivedRecordsRoute) {
                    popUpTo<ReceiverRoute.ReceivedRecordsRoute> { inclusive = false }
                    launchSingleTop = true
                }
            }

            override fun navigateToReceiverHome() {
                appState.navController.navigate(ReceiverRoute.HomeRoute)
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
            onSettingClick = { appState.navController.navigate(Route.Setting) },
            onNavigateToMindRecord = { appState.navController.navigate(Route.ReceiverMindRecord) },
            onNavigateToTimeLetter = { appState.navController.navigate(Route.TimeLetter) },
            onNavigateToAfternote = {
                appState.navController.navigate(ReceiverRoute.AfternoteListRoute)
            },
        )
    }
