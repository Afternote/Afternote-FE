package com.afternote.feature.onboarding.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface OnboardingRoute {
    @Serializable
    data object WelcomeRoute : OnboardingRoute

    @Serializable
    data object LoginRoute : OnboardingRoute

    @Serializable
    data object SignUpRoute : OnboardingRoute

    @Serializable
    data object SignUpResidentNumberRoute : OnboardingRoute

    @Serializable
    data object SignUpPasswordRoute : OnboardingRoute

    @Serializable
    data object TermsRoute : OnboardingRoute

    @Serializable
    data object ProfileRoute : OnboardingRoute
}
