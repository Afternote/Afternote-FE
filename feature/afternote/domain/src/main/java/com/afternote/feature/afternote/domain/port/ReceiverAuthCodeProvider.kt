package com.afternote.feature.afternote.domain.port

fun interface ReceiverAuthCodeProvider {
    fun currentAuthCode(): String?
}
