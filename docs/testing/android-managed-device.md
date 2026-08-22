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

## 커버리지 판정 경계

- 현재 production `NavHost`·화면·ViewModel·repository 계약으로 실제 진입 가능한 사용자 동작만
  androidTest 대상으로 삼는다.
- 구현된 동작은 정상 경로뿐 아니라 validation, 오류 표시, 재시도, 내비게이션, 상태 복원과
  repository payload·호출 횟수를 함께 검증한다.
- production UI·route·API 계약 자체가 없어서 진입할 수 없는 시나리오는 테스트용 화면이나
  로컬 상태로 흉내 내지 않는다. 해당 항목은 production 근거와 함께 `EXCLUDED`로 기록한다.
- 새 테스트가 실패하면 test double·selector·동기화·격리 문제를 먼저 배제한다. production
  결함으로 확정되면 예상/실제 결과와 재현 증거를 포함한 기능 담당자 이슈를 만들고, 수정과
  회귀 테스트는 그 구현 PR에서 함께 red-to-green으로 완료한다.
- 실패를 `@Ignore`, 무조건 재시도, 느슨한 단언으로 숨기거나 테스트 범위 작업에서 다른
  기능 소유자의 production 코드를 함께 수정하지 않는다.
