package com.afternote.feature.setting.presentation.social

internal sealed interface KakaoAuthResult {
    data class Success(
        val accessToken: String,
    ) : KakaoAuthResult

    data object Cancelled : KakaoAuthResult

    data object Failure : KakaoAuthResult
}

internal fun Result<String>.toKakaoAuthResult(): KakaoAuthResult {
    val accessToken = getOrNull()
    return when {
        !accessToken.isNullOrBlank() -> KakaoAuthResult.Success(accessToken)
        exceptionOrNull() is UserCancelledAuthException -> KakaoAuthResult.Cancelled
        else -> KakaoAuthResult.Failure
    }
}
