package com.afternote.feature.afternote.presentation.testing

import androidx.compose.runtime.Composable
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.presentation.home.AfternoteHomeEntry
import com.afternote.feature.afternote.presentation.home.AfternoteHomeViewModel
import com.afternote.feature.afternote.presentation.receiver.detail.ReceivedAfternoteDetailRoute
import com.afternote.feature.afternote.presentation.receiver.detail.ReceivedAfternoteDetailViewModel
import com.afternote.feature.afternote.presentation.receiver.navigation.ReceivedAfternoteRoute
import com.afternote.feature.receiver.domain.repository.ReceiverRepository

/** 실제 목록 VM과 화면을 연결한다. 테스트는 저장소 응답과 화면의 사용자 액션으로 동작을 검증한다. */
public class AfternoteHomeScreenFixture(
    repository: AfternoteRepository,
    errorReporter: ErrorReporter,
) {
    private val viewModel = AfternoteHomeViewModel(repository, errorReporter)

    @Composable
    public fun Content(
        navigateToDetail: (Long) -> Unit,
        navigateToAdd: (AfternoteType) -> Unit,
        onSettingClick: () -> Unit,
    ) {
        AfternoteHomeEntry(navigateToDetail, navigateToAdd, onSettingClick, viewModel)
    }
}

/** 수신 상세의 실제 조회·재시도·Lifecycle·렌더 경계를 앱 기기 테스트에서도 그대로 사용한다. */
public class ReceivedAfternoteDetailScreenFixture(
    afternoteId: Long,
    receiverRepository: ReceiverRepository,
    errorReporter: ErrorReporter,
) {
    private val viewModel =
        ReceivedAfternoteDetailViewModel(
            route = ReceivedAfternoteRoute.DetailRoute(afternoteId),
            receiverRepository = receiverRepository,
            errorReporter = errorReporter,
        )

    @Composable
    public fun Content(
        onNavigateBack: () -> Unit,
        onNavigateToFullList: () -> Unit,
        onNavigateToPlaylist: (Long) -> Unit,
    ) {
        ReceivedAfternoteDetailRoute(onNavigateBack, onNavigateToFullList, onNavigateToPlaylist, viewModel)
    }
}
