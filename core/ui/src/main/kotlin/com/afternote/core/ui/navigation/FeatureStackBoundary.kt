package com.afternote.core.ui.navigation

import androidx.compose.runtime.Stable

/**
 * 피처의 로컬 Navigation 3 스택이 앱 셸과 만나는 **최소 경계**.
 *
 * 로컬 스택은 제 화면 사이의 push/pop 을 스스로 처리하고, 스스로 답할 수 없는 두 가지만 셸에
 * 돌려준다 — 스택 바닥에서의 back([exit])과 바텀바 표시 판정에 필요한 깊이([onAtRootChanged]).
 * 그 밖의 이동(다른 소관 그래프로 가기)은 피처마다 달라서 각 host 가 제 콜백으로 받는다.
 *
 * 루트가 Nav2 인 동안 [exit] 는 루트 `NavController` 의 pop 이고, 루트가 `NavDisplay` 로 바뀌면
 * 루트 백스택의 pop 이 된다 — 구현만 갈리고 계약은 그대로다 (#1702 가 소비한다).
 */
@Stable
public interface FeatureStackBoundary {
    /** 로컬 스택 바닥에서 back 이 눌렸다. 셸이 이 피처를 백스택에서 내린다. */
    public fun exit()

    /**
     * 로컬 스택이 바닥(= 피처 시작 화면)인지 바뀌었다.
     *
     * 셸의 바텀바 판정은 Nav2 destination 만 보므로 로컬 스택 깊이를 모른다. 깊이를 아는 쪽이
     * 셸에 올려 판정에 합성한다. 피처를 떠날 때는 `true` 로 복원해 다른 탭 판정을 오염시키지
     * 않는다 — [FeatureNavDisplay] 가 대신 해 준다.
     */
    public fun onAtRootChanged(isAtRoot: Boolean) {}
}

/** 바텀바가 없어 셸로 돌아갈 길만 필요한 그래프용 [FeatureStackBoundary]. */
public fun FeatureStackBoundary(onExit: () -> Unit): FeatureStackBoundary =
    object : FeatureStackBoundary {
        override fun exit() = onExit()
    }
