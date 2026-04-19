package com.afternote.feature.mindrecord.presentation.navigation

/**
 * NavHost 루트에서 마인드레코드 서브그래프로 넘기는 네비게이션 명령 모음.
 *
 * [com.afternote.feature.onboarding.presentation.navigation.OnboardingNavActions]·
 * [com.afternote.feature.afternote.presentation.author.navigation.AfternoteNavActions]와 동일한
 * “actions 객체 단일 전달” 패턴이다.
 */
interface MindRecordNavActions {
    fun onMemorySpaceBack()
}
