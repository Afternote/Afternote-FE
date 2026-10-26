package com.afternote.feature.onboarding.presentation

import com.afternote.core.ui.UiText

/** 실패 한 건의 사유. 표시 채널과 소비 방법은 화면 경계가 결정한다. */
internal sealed interface OnboardingFailure {
    data object CredentialsRejected : OnboardingFailure

    data object VerificationRejected : OnboardingFailure

    data object LoginNetworkUnavailable : OnboardingFailure

    data object SocialAccountRecoveryUnavailable : OnboardingFailure

    /** 서버 원문 대신 기존 작업별 로컬 안내 문구를 보존한 요청 실패. */
    data class RequestFailed(
        val message: UiText,
    ) : OnboardingFailure
}

/** 화면에서만 사용하는 표시 결정. 한 실패가 두 채널로 동시에 전달될 수 없다. */
internal sealed interface OnboardingFailureDisplay {
    data object CredentialInline : OnboardingFailureDisplay

    data object VerificationInline : OnboardingFailureDisplay

    data object NetworkRetryPopup : OnboardingFailureDisplay

    data object SocialAccountPopup : OnboardingFailureDisplay

    data class Snackbar(
        val message: UiText,
    ) : OnboardingFailureDisplay
}

internal fun OnboardingFailure?.toDisplay(): OnboardingFailureDisplay? =
    when (this) {
        OnboardingFailure.CredentialsRejected -> OnboardingFailureDisplay.CredentialInline
        OnboardingFailure.VerificationRejected -> OnboardingFailureDisplay.VerificationInline
        OnboardingFailure.LoginNetworkUnavailable -> OnboardingFailureDisplay.NetworkRetryPopup
        OnboardingFailure.SocialAccountRecoveryUnavailable -> OnboardingFailureDisplay.SocialAccountPopup
        is OnboardingFailure.RequestFailed -> OnboardingFailureDisplay.Snackbar(message)
        null -> null
    }

internal val OnboardingFailureDisplay?.snackbarMessage: UiText?
    get() = (this as? OnboardingFailureDisplay.Snackbar)?.message
