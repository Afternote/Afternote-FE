package com.afternote.feature.mindrecord.domain.testing

/**
 * "이 시나리오에서 호출되면 안 됨" 을 그 자리에서 터뜨린다.
 *
 * fake 를 하나로 접으면 기본 동작이 관대해져, 안 불릴 줄 알았던 호출이 조용히 통과할 수
 * 있다. 시나리오별 fake 가 `error(...)` 로 지키던 경계를 `onX = { unexpectedCall(...) }`
 * 로 그대로 옮긴다.
 */
fun unexpectedCall(method: String): Nothing = error("$method 는 이 시나리오에서 호출되면 안 됨")
