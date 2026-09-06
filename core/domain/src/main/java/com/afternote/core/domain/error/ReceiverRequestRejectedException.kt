package com.afternote.core.domain.error

/** 사용자가 입력을 수정해 해결할 수 있는 수신자 등록·수정 요청 오류. */
class ReceiverRequestRejectedException(
    val userMessage: String,
    cause: Throwable,
) : Exception(userMessage, cause)
