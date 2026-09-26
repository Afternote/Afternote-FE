package com.afternote.feature.setting.presentation.navigation

/**
 * 설정 로컬 스택이 **스스로 갈 수 없는 곳**만 앱 셸에 남긴 이동.
 *
 * 둘 다 인증 상태가 바뀐 뒤라 루트 스택을 통째로 비워야 하는데, 다른 피처의 백스택은 로컬 스택이
 * 건드리지 않는다 — 셸이 온보딩으로 갈아 끼운다.
 */
public interface SettingExternalActions {
    public fun onLogoutSuccess()

    public fun onWithdrawSuccess()
}
