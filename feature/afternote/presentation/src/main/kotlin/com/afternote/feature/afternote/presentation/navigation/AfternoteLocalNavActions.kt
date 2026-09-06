package com.afternote.feature.afternote.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.core.ui.navigation.FeatureStackBoundary
import com.afternote.core.ui.navigation.popOrExit
import com.afternote.core.ui.navigation.replaceAllWith
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.editor.AfternoteEditorViewModel
import com.afternote.feature.afternote.presentation.navigation.model.AfternoteRoute

/**
 * 애프터노트 작성자 화면 콜백을 로컬 백스택 조작으로 잇는다.
 *
 * 컴포저블이 아니라 평범한 클래스다 — 백스택 «모양» 을 컴포지션 없이 재려는 것이다(#1601).
 */
internal class AfternoteLocalNavActions(
    private val backStack: NavBackStack<NavKey>,
    private val boundary: FeatureStackBoundary,
    private val externalActions: AfternoteExternalActions,
) : AfternoteNavActions {
    override fun popBack(): Unit = backStack.popOrExit(boundary)

    override fun navigateToAfternoteDetail(itemId: Long) {
        backStack.add(AfternoteRoute.DetailRoute(itemId = itemId))
    }

    override fun navigateToNewEditor(initialType: AfternoteType) {
        backStack.add(AfternoteRoute.EditorFlowRoute(initialType = initialType))
    }

    override fun navigateToEditorForEdit(
        itemId: Long,
        initialType: AfternoteType,
    ) {
        backStack.add(AfternoteRoute.EditorFlowRoute(itemId = itemId, initialType = initialType))
    }

    /** 인증 성공 — 지문 관문을 남기지 않는다. 뒤로가기로 다시 인증을 요구받지 않게. */
    override fun replaceFingerprintLoginWithAfternoteHome(): Unit = backStack.replaceAllWith(AfternoteRoute.AfternoteHomeRoute)

    /** 저장 성공 — 홈 위(에디터 흐름 등)를 걷어내고 홈만 남긴다. */
    override fun popToAfternoteHome(): Unit = backStack.replaceAllWith(AfternoteRoute.AfternoteHomeRoute)

    override fun navigateToBottomTab(tab: BottomNavTab): Unit = externalActions.navigateToBottomTab(tab)

    override fun navigateToSetting(): Unit = externalActions.navigateToSetting()

    override fun onFingerprintAuthFailed(message: String): Unit = externalActions.onFingerprintAuthFailed(message)
}

/**
 * 에디터 흐름 안에서만 의미가 있는 이동. 바깥 스택을 건드리는 하나는 콜백으로 위임한다.
 *
 * 수신자 선택 결과는 두 화면이 공유하는 [AfternoteEditorViewModel] 이 나른다 — Nav2 의
 * «이전 백스택 엔트리 SavedStateHandle» 자리다.
 */
internal class AfternoteEditorFlowLocalNavActions(
    private val flowStack: NavBackStack<NavKey>,
    private val boundary: FeatureStackBoundary,
    private val onReceiversSelected: (List<Long>) -> Unit,
    private val onSaveSuccessNavigateHome: () -> Unit,
) : AfternoteEditorFlowNavActions {
    override fun popBack(): Unit = flowStack.popOrExit(boundary)

    override fun navigateToMemorialPlaylist() {
        flowStack.add(AfternoteRoute.MemorialPlaylistRoute)
    }

    override fun navigateToSelectReceiver() {
        flowStack.add(AfternoteRoute.SelectReceiverRoute)
    }

    override fun navigateToAddSong() {
        flowStack.add(AfternoteRoute.AddSongRoute)
    }

    override fun popBackWithSelectedReceivers(receiverIds: List<Long>) {
        onReceiversSelected(receiverIds)
        popBack()
    }

    override fun popToAfternoteHome(): Unit = onSaveSuccessNavigateHome()
}
