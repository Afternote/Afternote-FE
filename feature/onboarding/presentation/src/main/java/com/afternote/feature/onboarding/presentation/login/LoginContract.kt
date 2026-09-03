package com.afternote.feature.onboarding.presentation.login

import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.feature.onboarding.presentation.reporting.AuthProvider

/** 로그인 화면이 [LoginViewModel] 에 보내는 것 — 사용자가 하려는 것. */
sealed interface LoginIntent : MviIntent {
    data class UpdateEmail(
        val value: String,
    ) : LoginIntent

    data class UpdatePassword(
        val value: String,
    ) : LoginIntent

    data object SubmitEmailLogin : LoginIntent

    /**
     * 소셜 SDK 가 돌려준 토큰으로 로그인한다. 토큰을 받아오는 일은 Activity·CredentialManager
     * 의존이라 화면이 하고, ViewModel 에는 문자열만 온다.
     */
    data class SubmitKakaoLogin(
        val oauthToken: String,
    ) : LoginIntent

    data class SubmitGoogleLogin(
        val idToken: String,
    ) : LoginIntent

    /**
     * 소셜 SDK 에서 토큰을 받아오지 못한 실패를 기록한다.
     *
     * 이 단계는 서버 호출 이전이라 로그인 경로를 타지 않아, 화면이 알려주지 않으면 콘솔에 아무
     * 흔적도 남지 않는다. 사용자 취소는 오류가 아니므로 화면이 걸러서 보낸다.
     */
    data class ReportSocialTokenFailure(
        val provider: AuthProvider,
        val throwable: Throwable,
    ) : LoginIntent

    /** 네트워크 실패 팝업의 "다시 시도하기". */
    data object RetryLogin : LoginIntent

    data object DismissNetworkError : LoginIntent

    data object ConsumeLoggedIn : LoginIntent

    data object ConsumeOnboardingStart : LoginIntent

    data object ConsumeError : LoginIntent
}

/** 상태가 겪은 것. [LoginViewModel] 만 만든다. */
sealed interface LoginReducerEvent : ReducerEvent {
    data class EmailChanged(
        val value: String,
    ) : LoginReducerEvent

    data class PasswordChanged(
        val value: String,
    ) : LoginReducerEvent

    data object LoginStarted : LoginReducerEvent

    data object LoggedIn : LoginReducerEvent

    /** 소셜 신규 가입자 — 온보딩으로 보낸다. */
    data object OnboardingRequired : LoginReducerEvent

    data object CredentialRejected : LoginReducerEvent

    data object NetworkErrorRaised : LoginReducerEvent

    data class LoginFailed(
        val message: UiText,
    ) : LoginReducerEvent

    data object NetworkErrorDismissed : LoginReducerEvent

    data object LoggedInConsumed : LoginReducerEvent

    data object OnboardingStartConsumed : LoginReducerEvent

    data object ErrorConsumed : LoginReducerEvent
}
