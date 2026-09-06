package com.afternote.feature.timeletter.domain.error

/**
 * A 4xx the server explicitly rejected (as opposed to a network/technical failure).
 *
 * [serverMessage] is the server's raw text — for logs and crash reports only, never for direct
 * display. The UI shows a fixed local message for this case; see `TimeLetterWriteError.ServerRejection`.
 */
class TimeLetterServerRejectionException(
    val status: Int,
    val serverMessage: String,
    cause: Throwable,
) : Exception(serverMessage, cause)
