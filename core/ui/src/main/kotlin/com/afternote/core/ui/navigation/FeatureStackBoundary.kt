package com.afternote.core.ui.navigation

/**
 * [FeatureNavigationCallbacks]의 이전 이름.
 *
 * 온보딩 담당자가 소비처를 전환할 때까지 소스 호환을 유지한다 (#1996).
 * 기존 이름의 소스 사용처가 모두 사라진 뒤 이 별칭과 아래 팩토리를 제거한다.
 */
public typealias FeatureStackBoundary = FeatureNavigationCallbacks

/** 기존 팩토리 호출의 소스 호환을 유지한다. 새 코드는 [FeatureNavigationCallbacks]를 사용한다. */
public fun FeatureStackBoundary(onExit: () -> Unit): FeatureStackBoundary =
    object : FeatureStackBoundary {
        override fun exit() = onExit()
    }
