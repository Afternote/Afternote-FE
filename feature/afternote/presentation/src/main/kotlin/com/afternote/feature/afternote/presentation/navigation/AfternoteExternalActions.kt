package com.afternote.feature.afternote.presentation.navigation

import com.afternote.core.ui.bottombar.BottomNavTab

/**
 * 애프터노트 로컬 스택이 **스스로 갈 수 없는 곳**만 앱 셸에 남긴 이동·신호.
 *
 * 그래프 안의 push/pop 은 [AfternoteNavHost] 가 로컬 백스택으로 직접 처리한다.
 */
public interface AfternoteExternalActions {
    /** 바텀바 탭 이동 — 탭별 백스택 판정은 셸 소관이다. */
    public fun navigateToBottomTab(tab: BottomNavTab)

    /** 홈 TopBar 설정 기어 → 설정 그래프. 소관이 다른 그래프라 셸을 거친다. */
    public fun navigateToSetting()

    /** 지문 인증 실패 — 셸의 snackbar 로 알린다. */
    public fun onFingerprintAuthFailed(message: String)
}
