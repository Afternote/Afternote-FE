package com.afternote.feature.afternote.presentation

/**
 * 좁은 화면 baseline 이 쓰는 기기 스펙 — 국내 보급형(720×1600 물리) 기준.
 *
 * 기본 `@Preview` 는 411×914dp 급으로 렌더되는데, 이 앱은 고정 dp 리터럴이 1566개인 반면
 * 반응형 API 사용은 2파일뿐이라 폭·높이가 줄면 잘리는 화면이 나온다. 스크롤이 없는 화면일수록
 * 세로가 모자랄 때 그대로 잘리므로, 그런 화면은 이 스펙으로 한 장 더 가드한다.
 *
 * dpi 320(xhdpi)은 실기기의 리소스 버킷을 맞추기 위한 값이다. 같은 360×800dp 라도
 * 160dpi(mdpi)로 잡으면 다른 drawable/dimen 이 선택돼 기준으로 쓸 수 없다.
 *
 * `screenshotTest` 소스셋은 모듈 밖으로 published 되지 않아 공유할 수 없다. onboarding·receiver
 * 에 같은 상수가 따로 있는 것은 그 때문이다 — 값을 바꿀 일이 생기면 세 곳을 함께 고쳐야 한다.
 */
internal const val COMPACT_DEVICE_SPEC = "spec:width=360dp,height=800dp,dpi=320"
