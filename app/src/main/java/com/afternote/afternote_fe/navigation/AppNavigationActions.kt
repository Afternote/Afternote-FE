package com.afternote.afternote_fe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.NavController
import com.afternote.afternote_fe.screen.HomeTabActions
import com.afternote.core.model.MindRecordCategory
import com.afternote.core.ui.Route
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.author.navigation.AfternoteNavActions
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute
import com.afternote.feature.mindrecord.presentation.navigation.MindRecordNavActions
import com.afternote.feature.onboarding.presentation.navigation.OnboardingNavActions
import com.afternote.feature.onboarding.presentation.navigation.OnboardingRoute

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

            override fun onWeeklyImageClick() {}

            override fun onWeeklyCountClick() {}

            override fun onWeeklyRecentRecordClick() {}

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
 * [NavGraphBuilder] DSL 밖에서 `remember`로 안정화한다.
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

            override fun onNavigateToEditorForEdit(itemId: String) {
                appState.navController.navigate(AfternoteRoute.EditorRoute(itemId = itemId))
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
