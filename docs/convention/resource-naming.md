# 리소스 네이밍 규칙

안드로이드 리소스(`<string>` · `drawable` 등)는 **모듈 하나에 프리픽스 하나**를 쓴다. 규칙을 문서로만 합의하지 않고 `android.resourcePrefix` 로 강제한다.

## 왜 모듈별인가

리소스는 이름이 같으면 R 병합 시 앱 모듈 값이 라이브러리 값을 덮는다 — 모듈이 달라도 네임스페이스가 갈리지 않는다. 그래서 이름의 첫 토큰이 **모듈 경계를 표시하지 못하면** 다음 세 가지가 조용히 일어난다.

1. **다른 모듈 몫이 남의 모듈에 눌러앉는다.** `feature/afternote` 에 있던 `receiver_*` 118개가 그것이다(#1066 이 되돌리는 중). `resourcePrefix` 가 걸려 있었다면 `receiver_verify_*` 를 애프터노트 모듈에 만드는 순간 lint 가 막았다.
2. **모듈 간 같은 이름이 값까지 같은 중복으로 자란다.** `login_kakao_failed`("카카오 로그인에 실패했습니다.")가 `feature/onboarding` 과 `feature/afternote` 양쪽에 있었고, 애프터노트 쪽은 사용 0건인 죽은 정의였다.
3. **화면별 프리픽스는 한 모듈 안에서만 일관돼 보인다.** `signup_*` · `withdraw_*` 처럼 화면 단위로 나누면 첫 토큰이 여러 모듈에 걸친다 — 도입 시점 실측으로 그런 토큰이 9종이었고, `login_*` 은 3개 모듈에 23개가 흩어져 있었다.

## 프리픽스 표

리소스를 가진 모듈만 대상이다(`*/data` · `*/domain` 은 리소스가 없다).

| 모듈 | 프리픽스 | 도입 |
|---|---|---|
| `core/ui` | `core_ui_` | 도입 완료 |
| `core/common` | `core_common_` | #1071 |
| `feature/home/presentation` | `home_` | #1071 |
| `feature/receiver/presentation` | `receiver_` | #1071 |
| `feature/mindrecord/presentation` | `mindrecord_` | 도입 완료 |
| `feature/setting/presentation` | `setting_` | #1077 |
| `feature/onboarding/presentation` | `onboarding_` | #1078 |
| `feature/afternote/presentation` | `afternote_` | #1079 |
| `feature/timeletter/presentation` | `timeletter_` | #635 |

- `core/*` 는 `core_<모듈>_`, `feature/*` 는 `<기능>_` 이다. core 는 모듈이 여럿이고 이름이 짧아(`ui` · `common`) 한 토큰으로는 충돌하기 쉽다.
- **`app` 모듈은 대상에서 제외한다.** 미준수 5건이 전부 이름을 바꾸면 안 되는 것들이다 — `app_name` 은 매니페스트 `android:label` 이 참조하는 관례 이름, `fcm_*` 3개는 매니페스트 메타데이터가 `@string/…` 으로 참조, `ic_launcher_background`/`_foreground` 는 adaptive icon 규약 이름이다. 앱 모듈은 R 병합의 최종 목적지라 프리픽스의 실익도 없다.

## 강제 수단

모듈 `build.gradle.kts` 의 `android` 블록에 한 줄:

```kotlin
android {
    namespace = "com.afternote.feature.home.presentation"
    resourcePrefix = "home_"
}
```

**위반은 경고가 아니라 오류이고, 빌드를 실패시킨다.** 도입 시점에 이미 준수 중인 `core/common` 에 일부러 어긋나는 프리픽스를 걸어 실측한 결과다.

```
core/common/src/main/res/values/strings.xml:3: Error: Resource named 'core_common_notification_daily_title'
does not start with the project's resource prefix 'zz_' … [ResourceName]

4 errors
BUILD FAILED
```

같은 이유로 **미준수가 남은 모듈에 설정을 먼저 걸면 `lintDebug` 가 즉시 red 가 된다.** 그래서 도입은 두 걸음으로 나눈다.

1. **리네임 0건인 모듈에 먼저 건다** — 우연히 이미 준수 중인 모듈(`receiver` · `home` · `core/common`).
2. **리네임이 필요한 모듈은 리네임과 같은 PR 에서 건다** — 설정만 먼저 걸어 두면 그 사이의 모든 PR 이 막힌다. lint baseline 으로 덮는 선택지는 쓰지 않는다(신규 유입까지 함께 가려진다).

리소스를 옮기는 작업은 **옮기면서 최종 이름까지 맞춘다** — 같은 리소스를 두 번 만지지 않기 위해서다(#1066 · #1069).

## 신규 리소스

프리픽스가 걸린 모듈에서는 lint 가 막으므로 따로 외울 것이 없다. 아직 걸리지 않은 모듈에 리소스를 추가할 때도 위 표의 프리픽스를 쓴다 — 그래야 나중 리네임 PR 의 규모가 늘지 않는다.
