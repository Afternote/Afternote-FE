package com.afternote.feature.setting.presentation

import androidx.core.content.FileProvider

/**
 * setting 전용 [FileProvider] — 프로필 사진 즉석 촬영 결과를 카메라 앱에 넘기는 통로 (#1438).
 *
 * 하는 일이 없는데도 하위 클래스를 두는 이유는 매니페스트 병합이다. 병합기는 `<provider>` 를
 * `android:name` 으로 식별하므로, 두 모듈이 `androidx.core.content.FileProvider` 를 그대로 쓰면 같은
 * 원소로 보고 `android:authorities` 충돌로 빌드를 멈춘다(afternote 가 이미 쓰고 있다). 이름을 갈라야
 * 두 provider 가 나란히 남는다 — `TimeLetterFileProvider` 가 같은 이유로 같은 모양이다.
 *
 * 인스턴스는 Android 프레임워크가 매니페스트의 이름으로 만든다. 코틀린 `internal` 은 바이트코드에서
 * public 클래스라 그 생성에 지장이 없고(로보렉트릭 촬영 테스트가 실제 provider 를 태워 확인한다),
 * 모듈 밖에서 이 이름을 코드로 참조할 일은 없다.
 */
internal class SettingFileProvider : FileProvider()
