package com.afternote.feature.timeletter.domain.error

/** A 4xx rejection whose server message is safe to show to the user. */
class TimeLetterServerRejectionException(
    val status: Int,
    val serverMessage: String,
    cause: Throwable,
) : Exception(serverMessage, cause)
