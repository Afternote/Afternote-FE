package com.afternote.feature.afternote.domain.testing

/** strict fake에서 시나리오가 열지 않은 계약 호출을 즉시 실패시킨다. */
fun unexpectedCall(method: String): Nothing = error("$method 는 이 시나리오에서 호출되면 안 됨")
