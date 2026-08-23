# Android managed-device tests

`app`의 instrumented tests는 실제 서버, Firebase, 소셜 로그인 및 사용자 계정 대신
결정적인 fake repository를 사용한다. 고정 실행 환경은 `Pixel 2`, API 30, AOSP image다.

## 로컬 실행

```shell
./gradlew pixel2Api30DebugAndroidTest \
  -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect
```

테스트는 AndroidX Test Orchestrator의 process isolation과 `clearPackageData=true`를 사용한다.
시간 초과를 늘리거나 실패 테스트를 무조건 재시도하는 설정은 두지 않는다. 실패 원인은 테스트별
화면 PNG와 최근 logcat, JUnit XML/HTML로 확인한다.

## 결과 위치

- HTML: `app/build/reports/androidTests/managedDevice/`
- JUnit XML: `app/build/outputs/androidTest-results/managedDevice/`
- 실패 화면/logcat: `app/build/outputs/managed_device_android_test_additional_output/`

GitHub Actions의 `Android Managed Device Test`는 `workflow_dispatch`와 주기 실행만 제공한다.
관련 경로의 PR 필수 검증 편입은 최근 3회 연속 성공을 확인한 뒤 별도 gate에서 판단한다.
