package com.afternote.afternote_fe.test

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * `setContent` 로 화면 일부만 띄우면서 `hiltViewModel()` 도 써야 하는 계측 테스트용 빈 Activity.
 *
 * `createComposeRule()` 의 기본 Activity 는 Hilt 컴포넌트를 갖지 않아, 화면 안에서
 * `hiltViewModel()` 을 부르는 자리(예: 홈의 MEMORIES 섹션)가 composition 되는 순간
 * "does not implement GeneratedComponent" 로 죽는다. 그렇다고 `MainActivity` 를 쓰면
 * 그쪽이 이미 콘텐츠를 세팅해 `setContent` 가 거부된다.
 *
 * debug 소스셋에 두는 이유는 androidTest 가 앱 APK 의 Activity 만 띄울 수 있어서다 —
 * 릴리스 빌드에는 들어가지 않는다.
 */
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()
