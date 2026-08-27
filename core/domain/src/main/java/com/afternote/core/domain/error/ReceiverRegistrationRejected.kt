package com.afternote.core.domain.error

class ReceiverRegistrationRejected(
    val serverMessage: String,
    cause: Throwable,
) : Exception("receiver registration rejected", cause)
