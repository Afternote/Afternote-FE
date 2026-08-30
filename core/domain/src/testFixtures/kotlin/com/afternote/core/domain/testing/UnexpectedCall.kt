package com.afternote.core.domain.testing

/** "이 시나리오에서 호출되면 안 됨" 경계를 정본 fake 에서도 보존한다. */
fun unexpectedCall(method: String): Nothing = error("$method 는 이 시나리오에서 호출되면 안 됨")
