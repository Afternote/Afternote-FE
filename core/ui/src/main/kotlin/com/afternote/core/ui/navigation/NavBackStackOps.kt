package com.afternote.core.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * 한 칸 내린다 — **바닥이면 스택을 비우지 않고** [boundary] 에 넘긴다.
 *
 * Nav3 는 빈 백스택을 그릴 수 없고, 바닥에서의 back 은 이 피처를 떠난다는 뜻이라 셸이 판단할
 * 몫이다. 이 파일의 나머지 확장도 같은 자리다 — Nav2 가 `popUpTo` 옵션으로 적던 «결과 상태» 를
 * 로컬 스택에선 직접 만들어야 해서, 그 모양들을 한 곳에 모았다 (#1698).
 */
public fun NavBackStack<NavKey>.popOrExit(boundary: FeatureStackBoundary) {
    if (size > 1) {
        removeAt(lastIndex)
    } else {
        boundary.exit()
    }
}

/**
 * 스택을 [key] 하나로 수렴시킨다 — Nav2 의 `popUpTo(inclusive = true)` + `navigate` 자리.
 *
 * 인증·검증처럼 «되돌아가면 안 되는» 단계를 지났을 때 쓴다.
 */
public fun NavBackStack<NavKey>.replaceAllWith(key: NavKey) {
    clear()
    add(key)
}

/**
 * [key] 를 남기고 그 위를 모두 걷어낸다 — Nav2 의 `popUpTo(inclusive = false)` + `launchSingleTop` 자리.
 *
 * 스택에 [key] 가 없으면(외부에서 곧장 들어온 진입 등) **[replaceAllWith] 로 그 키 하나만 남긴다.**
 *
 * **이건 Nav2 와 결과가 다르다** — Nav2 는 `popUpTo` 대상이 없으면 그것을 무시하고 위에 push 만 해서
 * 기존 스택이 남는다.
 *
 * | | 결과 스택 |
 * |---|---|
 * | Nav2 `popUpTo(없는 키) + navigate` | `[… 기존 …, key]` |
 * | 이 구현 | `[key]` |
 *
 * 「목록이 없는 진입에서도 목록 하나만 남긴다」를 의도한 것이다 — 알림·딥링크로 상세에 곧장 들어온
 * 뒤 목록으로 올라가면 그 목록이 바닥이어야 back 이 흐름을 빠져나간다. 다만 **Nav2 동등성이 아니라
 * 의도적인 이탈**이므로, 이 프리미티브를 새로 쓸 때는 그 차이를 전제로 골라야 한다.
 */
public fun NavBackStack<NavKey>.popUpTo(key: NavKey) {
    val index = indexOf(key)
    if (index < 0) {
        replaceAllWith(key)
        return
    }
    while (lastIndex > index) {
        removeAt(lastIndex)
    }
}

/** 같은 화면을 연달아 쌓지 않는 push — Nav2 의 `launchSingleTop` 자리. */
public fun NavBackStack<NavKey>.pushSingleTop(key: NavKey) {
    if (lastOrNull() != key) add(key)
}
