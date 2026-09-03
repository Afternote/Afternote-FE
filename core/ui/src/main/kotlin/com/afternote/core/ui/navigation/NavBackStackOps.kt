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
 * 스택에 [key] 가 없으면(외부에서 곧장 들어온 진입 등) 그 키 하나만 남긴다 — `popUpTo` 가
 * 무시되고 push 만 일어나던 Nav2 동작과 결과가 같다.
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
