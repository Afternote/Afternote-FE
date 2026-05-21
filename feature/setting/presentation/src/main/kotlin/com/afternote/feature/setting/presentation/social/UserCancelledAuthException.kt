package com.afternote.feature.setting.presentation.social

class UserCancelledAuthException(
    message: String = "사용자가 인증을 취소했습니다.",
) : Exception(message)
