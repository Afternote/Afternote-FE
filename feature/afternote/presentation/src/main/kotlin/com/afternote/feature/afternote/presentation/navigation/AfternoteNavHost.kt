package com.afternote.feature.afternote.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import com.afternote.core.ui.navigation.FeatureNavDisplay
import com.afternote.core.ui.navigation.FeatureStackBoundary
import com.afternote.feature.afternote.presentation.AfternoteHostViewModel
import com.afternote.feature.afternote.presentation.detail.AfternoteDetailNavigation
import com.afternote.feature.afternote.presentation.detail.AfternoteDetailViewModel
import com.afternote.feature.afternote.presentation.home.AfternoteHomeNavigation
import com.afternote.feature.afternote.presentation.navigation.model.AfternoteRoute
import com.afternote.feature.afternote.presentation.shared.fingerprint.AfternoteFingerprintLoginNavigation

/**
 * 애프터노트 작성자 피처가 소유하는 로컬 Navigation 3 스택.
 *
 * 시작점은 발신자용 지문 관문이다 — 관문을 지나야 홈에 닿는 성질을 유지하려고 스택의 바닥을
 * [AfternoteRoute.FingerprintLoginRoute] 로 둔다.
 *
 * ## 공유 ViewModel 수명
 *
 * [AfternoteHostViewModel] 은 Nav2 에서 `Route.Afternote` 그래프 엔트리에 묶여 있었다. Nav3 엔
 * 그래프 계층이 없으므로 host 자신의 스코프로 옮긴다 — 이 컴포저블을 담은 상위 엔트리가
 * 백스택에서 내려갈 때 정리되므로 이관 전과 같은 수명이다.
 *
 * 에디터 흐름 네 화면이 공유하는 폼 ViewModel 은 [AfternoteEditorFlowHost] 가 제 entry 범위로 갖는다.
 */
@Composable
public fun AfternoteNavHost(
    boundary: FeatureStackBoundary,
    externalActions: AfternoteExternalActions,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(AfternoteRoute.FingerprintLoginRoute)
    val actions =
        remember(backStack, boundary, externalActions) {
            AfternoteLocalNavActions(backStack, boundary, externalActions)
        }

    // 그래프 전체가 공유하는 ViewModel — 상세는 KDoc 참고. entry 안에서 만들면 그 화면이 pop 될 때
    // 함께 사라져 이관 전(그래프 스코프)과 수명이 달라진다.
    val hostViewModel: AfternoteHostViewModel = hiltViewModel()

    FeatureNavDisplay(
        backStack = backStack,
        boundary = boundary,
        modifier = modifier,
        entryProvider =
            entryProvider {
                entry<AfternoteRoute.FingerprintLoginRoute> {
                    AfternoteLightTheme {
                        val isPasskeyRegistered by hostViewModel.isPasskeyRegistered.collectAsStateWithLifecycle()
                        AfternoteFingerprintLoginNavigation(
                            isPasskeyRegistered = isPasskeyRegistered,
                            onAuthenticationSuccess = actions::replaceFingerprintLoginWithAfternoteHome,
                            onShowError = actions::onFingerprintAuthFailed,
                        )
                    }
                }

                entry<AfternoteRoute.AfternoteHomeRoute> {
                    AfternoteLightTheme {
                        AfternoteHomeNavigation(
                            onNavigateToDetail = actions::navigateToAfternoteDetail,
                            onNavigateToNewEditor = actions::navigateToNewEditor,
                            onNavigateToSetting = actions::navigateToSetting,
                        )
                    }
                }

                entry<AfternoteRoute.DetailRoute> { key ->
                    AfternoteLightTheme {
                        AfternoteDetailNavigation(
                            onNavigateBack = actions::popBack,
                            onNavigateToEditor = actions::navigateToEditorForEdit,
                            viewModel =
                                hiltViewModel<AfternoteDetailViewModel, AfternoteDetailViewModel.Factory>(
                                    creationCallback = { factory -> factory.create(key) },
                                ),
                        )
                    }
                }

                entry<AfternoteRoute.EditorFlowRoute> { key ->
                    AfternoteLightTheme {
                        AfternoteEditorFlowHost(
                            key = key,
                            onExitFlow = actions::popBack,
                            onSaveSuccessNavigateHome = actions::popToAfternoteHome,
                        )
                    }
                }
            },
    )
}
