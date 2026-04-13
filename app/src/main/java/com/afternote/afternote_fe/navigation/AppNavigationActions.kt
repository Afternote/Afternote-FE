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
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute
import com.afternote.feature.onboarding.presentation.navigation.OnboardingNavActions
import com.afternote.feature.onboarding.presentation.navigation.OnboardingRoute

@Composable
fun rememberOnboardingNavActions(navController: NavController): OnboardingNavActions =
    remember(navController) {
        object : OnboardingNavActions {
            override fun onOnboardingComplete() {
                navController.navigate(Route.Home) {
                    popUpTo<Route.Onboarding> { inclusive = true }
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
 * Afternote 서브그래프에 넘길 루트 레벨 네비게이션 **이벤트** 묶음.
 * 현재 목적지 같은 관찰 가능한 상태는 [AppNavigation]에서 계산해 `afternoteNavGraph` 인자로 전달한다.
 * [NavGraphBuilder] DSL 밖에서 `remember`로 안정화한다.
 */
data class AfternoteAppNavCallbacks(
    val onBottomNavTabSelected: (BottomNavTab) -> Unit,
    val onPopBackStack: () -> Unit,
    val onNavigateToAfternoteDetail: (itemId: String) -> Unit,
    val onNavigateToGalleryDetail: (itemId: String) -> Unit,
    val onNavigateToMemorialGuidelineDetail: (itemId: String) -> Unit,
    val onNavigateToNewEditor: (initialCategory: String?) -> Unit,
    val onNavigateToEditorForEdit: (itemId: String) -> Unit,
    val onNavigateToMemorialPlaylist: () -> Unit,
    val onNavigateToAddSong: () -> Unit,
    val onFingerprintAuthSuccess: () -> Unit,
    val onEditorSaveSuccessNavigateHome: () -> Unit,
)

@Composable
fun rememberAfternoteAppNavCallbacks(appState: AppState): AfternoteAppNavCallbacks =
    remember(appState) {
        AfternoteAppNavCallbacks(
            onBottomNavTabSelected = { tab -> appState.navigateToBottomBarRoute(tab.route) },
            onPopBackStack = { appState.navController.popBackStack() },
            onNavigateToAfternoteDetail = { itemId ->
                appState.navController.navigate(AfternoteRoute.DetailRoute(itemId = itemId))
            },
            onNavigateToGalleryDetail = { itemId ->
                appState.navController.navigate(AfternoteRoute.GalleryDetailRoute(itemId = itemId))
            },
            onNavigateToMemorialGuidelineDetail = { itemId ->
                appState.navController.navigate(
                    AfternoteRoute.MemorialGuidelineDetailRoute(itemId = itemId),
                )
            },
            onNavigateToNewEditor = { initialCategory ->
                appState.navController.navigate(AfternoteRoute.EditorRoute(initialCategory = initialCategory))
            },
            onNavigateToEditorForEdit = { itemId ->
                appState.navController.navigate(AfternoteRoute.EditorRoute(itemId = itemId))
            },
            onNavigateToMemorialPlaylist = {
                appState.navController.navigate(AfternoteRoute.MemorialPlaylistRoute)
            },
            onNavigateToAddSong = {
                appState.navController.navigate(AfternoteRoute.AddSongRoute)
            },
            onFingerprintAuthSuccess = {
                appState.navController.navigate(AfternoteRoute.AfternoteHomeRoute) {
                    popUpTo<AfternoteRoute.FingerprintLoginRoute> { inclusive = true }
                    launchSingleTop = true
                }
            },
            onEditorSaveSuccessNavigateHome = {
                appState.navController.navigate(AfternoteRoute.AfternoteHomeRoute) {
                    popUpTo<AfternoteRoute.AfternoteHomeRoute> { inclusive = false }
                    launchSingleTop = true
                }
            },
        )
    }
