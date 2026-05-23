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
            override fun onOnboardingComplete() {
                navController.navigate(Route.Home) {
                    popUpTo(0) { inclusive = true }
                }
            }

            override fun onNavigateWelcomeToSignUp() {
                navController.navigate(OnboardingRoute.SignUpRoute)
            }

            override fun onNavigateWelcomeToLogin() {
                navController.navigate(OnboardingRoute.LoginRoute)
            }

            override fun onNavigateWelcomeToReceivedRecords() {
                navController.navigate(Route.Receiver)
            }

            override fun onReplaceLoginWithSignUp() {
                navController.navigate(OnboardingRoute.SignUpRoute) {
                    popUpTo<OnboardingRoute.LoginRoute> { inclusive = true }
                }
            }

            override fun onLoginBack() {
                navController.popBackStack()
            }

            override fun onSignUpEmailNext() {
                navController.navigate(OnboardingRoute.SignUpResidentNumberRoute)
            }

            override fun onSignUpEmailBack() {
                navController.popBackStack()
            }

            override fun onSignUpResidentNext() {
                navController.navigate(OnboardingRoute.SignUpPasswordRoute)
            }

            override fun onSignUpResidentBack() {
                navController.popBackStack()
            }

            override fun onSignUpPasswordNext() {
                navController.navigate(OnboardingRoute.TermsRoute)
            }

            override fun onSignUpPasswordBack() {
                navController.popBackStack()
            }

            override fun onTermsNext() {
                navController.navigate(OnboardingRoute.ProfileRoute)
            }

            override fun onTermsBack() {
                navController.popBackStack()
            }

            override fun onViewTerms() {
                navController.navigate(OnboardingRoute.TermsDetailRoute)
            }

            override fun onTermsDetailBack() {
                navController.popBackStack()
            }

            override fun onProfileBack() {
                navController.popBackStack()
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
                navController.navigate(MindRecordRoute.DiaryWriteRoute)
            }

            override fun onWriteDeepThought() {
                navController.navigate(MindRecordRoute.DeepThoughtWriteRoute)
            }

            override fun onWriteBack() {
                navController.popBackStack()
            }

            override fun onWriteSubmitSuccess() {
                navController.popBackStack()
            }
        }
    }

@Composable
fun rememberTimeLetterNavActions(navController: NavController): TimeLetterNavActions =
    remember(navController) {
        object : TimeLetterNavActions {
            override fun onNavigateToWrite() {
                navController.navigate(TimeLetterRoute.TimeLetterWriteRoute)
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
                appState.navController.navigate(SettingRoute.RecipientListRoute)
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

            override fun onNavigateToAfterDelivery() {
                appState.navController.navigate(SettingRoute.AfterDeliveryRoute)
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
            override fun onBottomNavTabSelected(tab: BottomNavTab) {
                appState.navigateToBottomBarRoute(tab.route)
            }

            override fun onPopBackStack() {
                appState.navController.popBackStack()
            }

            override fun onNavigateToAfternoteDetail(itemId: String) {
                appState.navController.navigate(AfternoteRoute.DetailRoute(itemId = itemId))
            }

            override fun onNavigateToGalleryDetail(itemId: String) {
                appState.navController.navigate(AfternoteRoute.GalleryDetailRoute(itemId = itemId))
            }

            override fun onNavigateToMemorialGuidelineDetail(itemId: String) {
                appState.navController.navigate(
                    AfternoteRoute.MemorialGuidelineDetailRoute(itemId = itemId),
                )
            }

            override fun onNavigateToNewEditor(initialCategory: String?) {
                appState.navController.navigate(AfternoteRoute.EditorRoute(initialCategory = initialCategory))
            }

            override fun onNavigateToEditorForEdit(
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

            override fun onNavigateToMemorialPlaylist() {
                appState.navController.navigate(AfternoteRoute.MemorialPlaylistRoute)
            }

            override fun onNavigateToAddSong() {
                appState.navController.navigate(AfternoteRoute.AddSongRoute)
            }

            override fun onFingerprintAuthSuccess() {
                appState.navController.navigate(AfternoteRoute.AfternoteHomeRoute) {
                    popUpTo<AfternoteRoute.FingerprintLoginRoute> { inclusive = true }
                    launchSingleTop = true
                }
            }

            override fun onFingerprintAuthError(message: String) {
                onFingerprintErrorState(message)
            }

            override fun onEditorSaveSuccessNavigateHome() {
                appState.navController.navigate(AfternoteRoute.AfternoteHomeRoute) {
                    popUpTo<AfternoteRoute.AfternoteHomeRoute> { inclusive = false }
                    launchSingleTop = true
                }
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
            override fun onPopBackStack() {
                appState.navController.popBackStack()
            }

            override fun onNavigateToAfternoteList() {
                appState.navController.navigate(ReceiverRoute.AfternoteListRoute)
            }

            override fun onNavigateToReceivedAfternoteDetail(afternoteId: String) {
                appState.navController.navigate(
                    ReceiverRoute.AfternoteDetailRoute(afternoteId = afternoteId),
                )
            }

            override fun onNavigateToSenderRegistration() {
                appState.navController.navigate(ReceiverRoute.SenderRegistrationRoute)
            }

            override fun onNavigateToSenderDetail(senderId: String) {
                appState.navController.navigate(
                    ReceiverRoute.SenderDetailRoute(senderId = senderId),
                )
            }

            override fun onRequestVerificationFlow(senderId: String) {
                // nested 열람 신청 흐름 그래프 진입. 본인 확인 캐시 분기는 IntroRoute 의 LaunchedEffect 가 처리.
                appState.navController.navigate(
                    ReceiverRoute.DeliveryVerificationFlowRoute(senderId = senderId),
                )
            }

            override fun onNavigateIdentityIntroToEmail() {
                appState.navController.navigate(ReceiverRoute.IdentityVerificationEmailRoute)
            }

            override fun onNavigateIdentityIntroToMasterKey() {
                // 캐시 존재로 안내 화면 스킵. Intro 는 pop.
                appState.navController.navigate(ReceiverRoute.MasterKeyRoute) {
                    popUpTo<ReceiverRoute.IdentityVerificationIntroRoute> { inclusive = true }
                }
            }

            override fun onNavigateIdentityEmailToMasterKey() {
                // 이메일 인증 성공 직후 진입. 본인 확인 화면 2 장은 pop 해서 뒤로가기로 되돌아오지 않게.
                appState.navController.navigate(ReceiverRoute.MasterKeyRoute) {
                    popUpTo<ReceiverRoute.IdentityVerificationIntroRoute> { inclusive = true }
                }
            }

            override fun onNavigateMasterKeyToDocumentUpload() {
                appState.navController.navigate(ReceiverRoute.DocumentUploadRoute) {
                    popUpTo<ReceiverRoute.MasterKeyRoute> { inclusive = true }
                }
            }

            override fun onNavigateDocumentUploadToComplete() {
                appState.navController.navigate(ReceiverRoute.DeliveryVerificationCompleteRoute) {
                    popUpTo<ReceiverRoute.DocumentUploadRoute> { inclusive = true }
                }
            }

            override fun onNavigateCompleteToReceivedRecords() {
                // 받은 기록함을 남기고 신청 흐름 화면들(완료/서류/마스터 키)을 모두 pop.
                appState.navController.navigate(ReceiverRoute.ReceivedRecordsRoute) {
                    popUpTo<ReceiverRoute.ReceivedRecordsRoute> { inclusive = false }
                    launchSingleTop = true
                }
            }

            override fun onNavigateToReceiverHome() {
                appState.navController.navigate(ReceiverRoute.HomeRoute)
            }
        }
    }

/**
 * 수신자 홈에서 발생하는 다른 top-level Route(설정/마음의 기록/타임레터)와
 * 수신자 그래프 내부(애프터노트 목록) 이동을 묶은 [ReceiverHomeActions].
 *
 * MindRecord/TimeLetter 화면은 현재 작성자용으로만 구현돼 있어 수신자 진입 시
 * 동일 화면이 노출된다. 수신자 전용 표시는 후속 PR에서 각 피처가 분기한다.
 */
@Composable
fun rememberReceiverHomeActions(appState: AppState): ReceiverHomeActions =
    remember(appState) {
        ReceiverHomeActions(
            onSettingClick = { appState.navController.navigate(Route.Setting) },
            onNavigateToMindRecord = { appState.navController.navigate(Route.MindRecord) },
            onNavigateToTimeLetter = { appState.navController.navigate(Route.TimeLetter) },
            onNavigateToAfternote = {
                appState.navController.navigate(ReceiverRoute.AfternoteListRoute)
            },
        )
    }
