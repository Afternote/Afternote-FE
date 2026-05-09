package com.afternote.feature.afternote.presentation.author.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute
import com.afternote.core.ui.ObserveAsEvents
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailEvent
import com.afternote.feature.afternote.presentation.author.detail.GalleryDetailRoute
import com.afternote.feature.afternote.presentation.author.detail.MemorialGuidelineDetailRoute
import com.afternote.feature.afternote.presentation.author.detail.socialnetwork.SocialNetworkDetailRoute
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute
import kotlinx.coroutines.flow.Flow

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
 * 상세 화면 일회성 이벤트(`Channel<AfternoteDetailEvent>`) 공용 수집 헬퍼.
 *
 * - [AfternoteDetailEvent.DeleteSucceeded] → [onDeleteSucceeded] (보통 [androidx.activity.compose.BackHandler] 가 아닌 명시적 [onBack] 호출).
 * - [AfternoteDetailEvent.DeleteFailed] → [onDeleteFailed] (에러 UI는 화면별 Snackbar/Dialog 책임. 기본은 무시).
 *
 * 영속 [androidx.compose.runtime.LaunchedEffect] 키잉 대신 [com.afternote.core.ui.ObserveAsEvents] 로
 * Lifecycle 안전한 단발성 수집을 보장한다.
 */
@Composable
internal fun ObserveDetailEvents(
    events: Flow<AfternoteDetailEvent>,
    onDeleteSucceeded: () -> Unit,
    onDeleteFailed: (rawMessage: String?, messageRes: Int?) -> Unit = { _, _ -> },
) {
    ObserveAsEvents(flow = events) { event ->
        when (event) {
            is AfternoteDetailEvent.DeleteSucceeded -> onDeleteSucceeded()
            is AfternoteDetailEvent.DeleteFailed ->
                onDeleteFailed(event.rawMessage, event.messageRes)
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
