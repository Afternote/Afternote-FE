package com.afternote.feature.receiver.presentation

/**
 * 글자 확대 baseline 이 쓰는 배율 — Android 접근성 설정의 "크게" 구간.
 *
 * 화면 크기와는 다른 축이다. `fontScale` 이 오르면 dp 는 그대로인데 sp 만 커지므로,
 * 해상도를 아무리 바꿔도 재현되지 않는다. 이 앱은 `fontSize` 를 sp 로 제대로 쓰지만
 * 그것을 담는 컨테이너가 고정 dp(height 301곳·size 126곳·width 81곳)라, 글자만 커지고
 * 그릇은 그대로여서 텍스트가 그릇을 넘거나 `maxLines` 로 잘려 사라진다.
 *
 * 1.5 를 고르는 근거는 실측이다. 2026-08-25 develop `428022ff3` 실기 검사에서 표준
 * 화면(411×914dp)에 글자 1.5배를 걸자 애프터노트 홈의 카테고리 탭 겹침(#1141)이 재현됐고,
 * 겹침비는 좁은 화면(360dp)의 0.34 보다 큰 1.0 이었다. 1.3 에서는 검사한 화면이 모두 정상이었다.
 *
 * `screenshotTest` 소스셋은 모듈 밖으로 published 되지 않아 공유할 수 없다 — 값을 바꿀 일이
 * 생기면 이 상수를 둔 모듈을 함께 고쳐야 한다.
 */
internal const val LARGE_FONT_SCALE = 1.5f
