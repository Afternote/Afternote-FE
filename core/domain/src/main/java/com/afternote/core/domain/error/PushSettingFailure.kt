package com.afternote.core.domain.error

sealed class PushSettingFailure(
    message: String,
    cause: Throwable,
) : Exception(message, cause) {
    class NetworkUnavailable(
        cause: Throwable,
    ) : PushSettingFailure("push setting network unavailable", cause)

    class ServerUnavailable(
        cause: Throwable,
    ) : PushSettingFailure("push setting server unavailable", cause)
}
