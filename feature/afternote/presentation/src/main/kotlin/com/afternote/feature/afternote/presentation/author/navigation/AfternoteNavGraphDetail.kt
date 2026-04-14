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
import com.afternote.feature.afternote.presentation.author.detail.AfternoteDeleteState
import com.afternote.feature.afternote.presentation.author.detail.GalleryDetailRoute
import com.afternote.feature.afternote.presentation.author.detail.MemorialGuidelineDetailRoute
import com.afternote.feature.afternote.presentation.author.detail.SocialNetworkDetailRoute
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
 * 삭제 결과(Succeeded/Failed) 를 감지해 뒤로가기·소비 신호를 송출하는 공용 이펙트.
 * Succeeded → onBack, 이후 VM 에 Consumed 이벤트를 전달해 상태를 Idle 로 복귀시킨다.
 */
@Composable
internal fun HandleDeleteResult(
    deleteState: AfternoteDeleteState,
    onBack: () -> Unit,
    onConsumed: () -> Unit,
) {
    LaunchedEffect(deleteState) {
        when (deleteState) {
            AfternoteDeleteState.Succeeded -> {
                onBack()
                onConsumed()
            }

            is AfternoteDeleteState.Failed -> {
                // 에러 UI 처리는 화면별 Snackbar/Dialog 에서 담당. 여기서는 상태만 소비.
                onConsumed()
            }

            AfternoteDeleteState.Idle,
            AfternoteDeleteState.InProgress,
            -> {
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
        SocialNetworkDetailRoute(
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
