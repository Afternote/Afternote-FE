package com.afternote.feature.afternote.presentation.author.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailDeleteResult
import com.afternote.feature.afternote.presentation.author.detail.GalleryDetailRoute
import com.afternote.feature.afternote.presentation.author.detail.MemorialGuidelineDetailRoute
import com.afternote.feature.afternote.presentation.author.detail.account.AccountDetailRoute
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute

@Composable
internal fun DetailLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun DesignPendingDetailContent(onBackClick: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(title = "", onBackClick = onBackClick)
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.design_pending))
        }
    }
}

/**
 * 상세 화면 삭제 결과 ([AfternoteDetailDeleteResult]) 공용 처리 헬퍼.
 *
 * UiState 의 nullable 신호([com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailUiState.Success.deleteResult])를
 * [LaunchedEffect] 로 감지해
 * - [AfternoteDetailDeleteResult.Succeeded] → [onDeleteSucceeded] (보통 호출처가 pop 콜백을 전달)
 * - [AfternoteDetailDeleteResult.Failed] → [onDeleteFailed] (에러 UI는 화면별 Snackbar/Dialog 책임. 기본은 무시)
 *
 * 처리 후 [onConsumed] 콜백으로 ViewModel 의 [com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailViewModel.onDeleteResultConsumed]
 * 호출 → state reset (재합성 시 중복 처리 방지).
 */
@Composable
internal fun ObserveDeleteResult(
    deleteResult: AfternoteDetailDeleteResult?,
    onConsumed: () -> Unit,
    onDeleteSucceeded: () -> Unit,
    onDeleteFailed: (rawMessage: String?, messageRes: Int?) -> Unit = { _, _ -> },
) {
    LaunchedEffect(deleteResult) {
        when (deleteResult) {
            is AfternoteDetailDeleteResult.Succeeded -> {
                onDeleteSucceeded()
                onConsumed()
            }

            is AfternoteDetailDeleteResult.Failed -> {
                onDeleteFailed(deleteResult.rawMessage, deleteResult.messageRes)
                onConsumed()
            }

            null -> {
                Unit
            }
        }
    }
}

@Composable
internal fun AfternoteDetailNavigation(
    backStackEntry: NavBackStackEntry,
    onBack: () -> Unit,
    onNavigateToEditor: (itemId: String) -> Unit,
) {
    val route = backStackEntry.toRoute<AfternoteRoute.DetailRoute>()
    if (route.itemId.isBlank()) {
        DesignPendingDetailContent(onBackClick = onBack)
    } else {
        AccountDetailRoute(
            onBack = onBack,
            onNavigateToEditor = onNavigateToEditor,
        )
    }
}

@Composable
internal fun AfternoteGalleryDetailNavigation(
    onBack: () -> Unit,
    onNavigateToEditor: (itemId: String) -> Unit,
) {
    GalleryDetailRoute(
        onBack = onBack,
        onNavigateToEditor = onNavigateToEditor,
    )
}

@Composable
internal fun AfternoteMemorialGuidelineDetailNavigation(
    onBack: () -> Unit,
    onNavigateToEditor: (itemId: String) -> Unit,
) {
    MemorialGuidelineDetailRoute(
        onBack = onBack,
        onNavigateToEditor = onNavigateToEditor,
    )
}
