package com.afternote.feature.afternote.presentation.author.navigation

import com.afternote.core.ui.bottombar.BottomNavTab

/**
 * NavHost 루트에서 Afternote 서브그래프로 넘기는 네비게이션 명령 모음.
 *
 * [com.afternote.feature.onboarding.presentation.navigation.OnboardingNavActions]와 같은
 * “actions 객체 단일 전달” 패턴으로, 콜백 추가 시 시그니처 확장 없이 인터페이스만 갱신한다.
 */
interface AfternoteNavActions {
    fun onBottomNavTabSelected(tab: BottomNavTab)

    fun onPopBackStack()

    fun onNavigateToAfternoteDetail(itemId: String)

    fun onNavigateToGalleryDetail(itemId: String)

    fun onNavigateToMemorialGuidelineDetail(itemId: String)

    fun onNavigateToNewEditor(initialCategory: String?)

    fun onNavigateToEditorForEdit(itemId: String)

    fun onNavigateToMemorialPlaylist()

    fun onNavigateToAddSong()

    fun onFingerprintAuthSuccess()

    fun onFingerprintAuthError(message: String)

    fun onEditorSaveSuccessNavigateHome()
}
