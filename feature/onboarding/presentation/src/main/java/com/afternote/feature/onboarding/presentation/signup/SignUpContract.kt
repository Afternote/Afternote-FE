package com.afternote.feature.onboarding.presentation.signup

import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent

/**
 * 회원가입 플로우가 [SignUpViewModel] 에 보내는 것 — 사용자가 하려는 것.
 *
 * Step 1~4 와 Profile 이 같은 VM 인스턴스를 공유하므로 Intent 도 한 계약에 모인다.
 */
sealed interface SignUpIntent : MviIntent {
    data class UpdateEmail(
        val value: String,
    ) : SignUpIntent

    data class UpdateVerificationCode(
        val value: String,
    ) : SignUpIntent

    data class UpdateResidentFrontNumber(
        val value: String,
    ) : SignUpIntent

    data class UpdateResidentBackNumber(
        val value: String,
    ) : SignUpIntent

    data class UpdateSignUpPassword(
        val value: String,
    ) : SignUpIntent

    data class UpdateSignUpPasswordConfirm(
        val value: String,
    ) : SignUpIntent

    data class UpdateName(
        val value: String,
    ) : SignUpIntent

    /** photo picker 선택 — 화면이 `Uri.toString()` 으로 바꿔 보낸다. 취소(null)는 화면 경계에서 걸러진다. */
    data class PickProfileImage(
        val uri: String,
    ) : SignUpIntent

    data class ToggleTermsAgreed(
        val agreed: Boolean,
    ) : SignUpIntent

    data class TogglePrivacyAgreed(
        val agreed: Boolean,
    ) : SignUpIntent

    data class ToggleMarketingAgreed(
        val agreed: Boolean,
    ) : SignUpIntent

    data class ToggleAllTerms(
        val agreed: Boolean,
    ) : SignUpIntent

    data object RequestVerification : SignUpIntent

    /** Step 1 "다음" — 이메일·인증번호를 서버에 검증한다. */
    data object VerifyEmailAndProceed : SignUpIntent

    data object SubmitSignUp : SignUpIntent

    data object ConsumeSignedUp : SignUpIntent

    data object ConsumeResidentNumberNavigation : SignUpIntent

    data object ConsumeNameRequired : SignUpIntent

    data object ConsumeError : SignUpIntent
}

/** 상태가 겪은 것. [SignUpViewModel] 만 만든다. */
sealed interface SignUpReducerEvent : ReducerEvent {
    data class EmailChanged(
        val value: String,
    ) : SignUpReducerEvent

    data class VerificationCodeChanged(
        val value: String,
    ) : SignUpReducerEvent

    data class ResidentFrontNumberChanged(
        val value: String,
    ) : SignUpReducerEvent

    data class ResidentBackNumberChanged(
        val value: String,
    ) : SignUpReducerEvent

    data class SignUpPasswordChanged(
        val value: String,
    ) : SignUpReducerEvent

    data class SignUpPasswordConfirmChanged(
        val value: String,
    ) : SignUpReducerEvent

    data class NameChanged(
        val value: String,
    ) : SignUpReducerEvent

    data class ProfileImagePicked(
        val uri: String,
    ) : SignUpReducerEvent

    data class TermsAgreementChanged(
        val agreed: Boolean,
    ) : SignUpReducerEvent

    data class PrivacyAgreementChanged(
        val agreed: Boolean,
    ) : SignUpReducerEvent

    data class MarketingAgreementChanged(
        val agreed: Boolean,
    ) : SignUpReducerEvent

    data class AllAgreementsChanged(
        val agreed: Boolean,
    ) : SignUpReducerEvent

    data object CodeSendStarted : SignUpReducerEvent

    data object CodeSent : SignUpReducerEvent

    data class CodeSendFailed(
        val message: UiText,
    ) : SignUpReducerEvent

    data object CodeSendFinished : SignUpReducerEvent

    /** 쿨다운을 30초로 재장전한다. */
    data object CooldownReloaded : SignUpReducerEvent

    data object CooldownTicked : SignUpReducerEvent

    data object EmailVerifyStarted : SignUpReducerEvent

    data object EmailVerified : SignUpReducerEvent

    /** 인증번호 무효(서버 code 1207) — 인라인 문구로 알린다. */
    data object VerificationRejected : SignUpReducerEvent

    data class EmailVerifyFailed(
        val message: UiText,
    ) : SignUpReducerEvent

    data object EmailVerifyFinished : SignUpReducerEvent

    data object NameRequired : SignUpReducerEvent

    data object SubmitStarted : SignUpReducerEvent

    data object AccountCreated : SignUpReducerEvent

    data class SubmitFailed(
        val message: UiText,
    ) : SignUpReducerEvent

    data object SignedUp : SignUpReducerEvent

    data object SubmitFinished : SignUpReducerEvent

    data object SignedUpConsumed : SignUpReducerEvent

    data object ResidentNumberNavigationConsumed : SignUpReducerEvent

    data object NameRequiredConsumed : SignUpReducerEvent

    data object ErrorConsumed : SignUpReducerEvent
}
