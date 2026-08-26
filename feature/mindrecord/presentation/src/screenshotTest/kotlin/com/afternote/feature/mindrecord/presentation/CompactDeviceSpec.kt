package com.afternote.feature.mindrecord.presentation

/**
 * 좁은 화면 baseline 이 쓰는 기기 스펙 — 국내 보급형(720×1600 물리) 기준 (#1131).
 *
 * 기본 `@Preview` 는 411×914dp 급으로 렌더된다. 이 앱은 고정 dp 리터럴이 많고 반응형 API 는
 * 거의 안 쓰므로, 스크롤이 없는 화면은 세로가 모자라면 그대로 잘린다. 실기 QA 가 돌던
 * 에뮬레이터는 그보다도 넓어(446×967dp) 잘림이 구조적으로 안 잡혔다 (#1129).
 *
 * dpi 320(xhdpi)은 실기기의 리소스 버킷을 맞추기 위한 값이다 — 같은 360×800dp 라도 160dpi 면
 * 다른 drawable/dimen 이 선택돼 기준으로 쓸 수 없다. `feature:onboarding`·`feature:receiver`
 * 의 같은 상수와 값을 맞춘다.
 */
internal const val COMPACT_DEVICE_SPEC = "spec:width=360dp,height=800dp,dpi=320"
