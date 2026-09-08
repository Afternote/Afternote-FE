# 프로덕션 visibility 규약

## 판단 기준

`src/main`·`src/debug`·`src/release` 선언의 공개 범위는 프로덕션 소스셋의 실제 사용처만으로
정한다. 테스트가 선언을 직접 호출한다는 이유는 `private`를 `internal`이나 `public`으로 넓힐
근거가 아니다.

1. 프로덕션 사용처가 같은 파일 안에만 있으면 `private`로 둔다.
2. 같은 Gradle 모듈의 다른 파일이 사용하면 `internal`로 둔다.
3. 다른 Gradle 모듈이 실제로 사용해야 할 때만 `public`로 둔다.
4. 테스트 seam이 필요하면 공개 동작을 검증하거나 `testFixtures`·테스트 소스셋·debug 구현으로
   옮긴다.
5. Hilt·KSP·직렬화·Compose compiler 등 생성 코드가 더 넓은 범위를 요구하면 실제 컴파일
   실패로 확인하고 예외 이유를 선언 근처에 남긴다.

## 자동 가드의 책임 경계

| 가드 | 잡는 것 | 잡지 못하는 것 |
| --- | --- | --- |
| Kotlin explicit API | 프로덕션 컴파일에서 암시적 `public` visibility와 공개 API의 암시적 반환 타입 | 명시적으로 적었지만 필요 이상 넓은 `internal`·`public` |
| visibility Konsist | 기준선 이후 새로 생긴 테스트 전용 접근과 정적 구조 위반 | 리플렉션·생성 코드처럼 정적 사용처만으로 판단할 수 없는 예외 |
| 리뷰 | 프로덕션 실제 호출 범위에 맞는 최소 visibility인지, 프레임워크 예외 근거가 충분한지 | 컴파일러·정적 가드를 실행하지 않고 생긴 누락 |

세 가드는 대체 관계가 아니다. 컴파일러가 API 의도를 명시하게 만들고, Konsist가 알려진 구조적
회귀를 막으며, 리뷰가 `internal`과 `public` 중 무엇이 실제로 필요한지 판단한다.

#1671~#1677 전수 감사의 75건 중 정적 FQN·top-level 사용처로 보수적으로 판정할 수 있는 55건은
Konsist baseline에 둔다. 나머지 member·constructor·동명 overload 20건은 컴파일러나 Konsist가
최소 범위를 의미적으로 판정할 수 없으므로 자식 이슈와 리뷰가 끝까지 소유한다. 외부 모듈이
의도적으로 소비하는 프로덕션 계약은 `ProductionVisibilityKonsistTest`의 사유 map에만 예외로
기록하며, 무근거 경로 예외는 추가하지 않는다.

## 적용 정책

- warning inventory에 남은 기존 프로덕션 모듈은 현재 진단을 숨기지 않도록
  `explicitApiWarning()`을 적용한다.
- Android application/library/data/domain/datastore 계열 또는 JVM library/domain 컨벤션을 새
  경로에 적용하면 `build-logic/src/main/kotlin/ExplicitApiConvention.kt`의 기본값인 strict가
  즉시 적용된다.
- 기존 모듈을 strict로 전환할 때만 warning inventory에서 해당 경로를 제거한다. 새 모듈을
  통과시키려고 inventory에 추가하지 않는다.
- 공통 JVM 규약을 사용하지 않는 `:feature:setting:domain`과 별도 포함 빌드인 `build-logic`은
  각 빌드 스크립트에서 warning을 명시한다. 신규 raw Kotlin 모듈은 warning을 직접 선택할 수 없다.
  `:konsist`는 테스트 소스만, `:baselineprofile`은 계측 소스만 소유하므로 프로덕션 API
  inventory에서 제외한다.

## strict 전환 순서

의존성의 API 소유자부터 소비자 순서로 전환한다. 한 행을 전환할 때 해당 모듈의 warning을
모두 해소하고 컴파일·단위 테스트를 통과시킨 뒤 inventory에서 제거한다.

현재 프로덕션 Kotlin 소스가 없는 `:feature:onboarding:data`·`:feature:setting:data`·
`:feature:timeletter:res`는 기존 부채가 없어 이미 strict다. 이후 첫 Kotlin 소스가 추가돼도
strict가 그대로 적용된다.

| 순서 | 모듈 | 현재 모드 |
| ---: | --- | --- |
| 1 | `:core:model` | warning |
| 2 | `:core:domain` | warning |
| 3 | `:feature:afternote:domain` | warning |
| 4 | `:feature:mindrecord:domain` | warning |
| 5 | `:feature:receiver:domain` | warning |
| 6 | `:feature:timeletter:domain` | warning |
| 7 | `:feature:setting:domain` | warning, raw Kotlin 예외 |
| 8 | `:core:common` | warning |
| 9 | `:core:network` | warning |
| 10 | `:core:datastore` | warning |
| 11 | `:core:data` | warning |
| 12 | `:feature:afternote:data` | warning |
| 13 | `:feature:mindrecord:data` | warning |
| 14 | `:feature:receiver:data` | warning |
| 15 | `:feature:timeletter:data` | warning |
| 16 | `:core:ui` | warning |
| 17 | `:feature:home:presentation` | warning |
| 18 | `:feature:onboarding:presentation` | warning |
| 19 | `:feature:setting:presentation` | warning |
| 20 | `:feature:afternote:presentation` | warning |
| 21 | `:feature:mindrecord:presentation` | warning |
| 22 | `:feature:receiver:presentation` | warning |
| 23 | `:feature:timeletter:presentation` | warning |
| 24 | `:app` | warning |
| 25 | `build-logic` | warning, 별도 포함 빌드 |

전환 PR에서는 warning 개수만 줄이는 것이 아니라 각 선언의 실제 프로덕션 사용처를 확인한다.
테스트가 직접 참조하던 선언은 공개 범위를 명시하는 대신 테스트를 공개 동작 기준으로 옮긴다.

## 프로덕션은 테스트에 맞추지 않는다

생성자 시그니처·visibility·DI 조립 방식은 프로덕션의 요구로만 정한다. «테스트가 이렇게 쓰고 있어서» 는
근거가 아니다 — 설계 판단에 그 문장이 나오면 프로덕션을 올바른 모양으로 바꾸고 테스트를 따라오게 한다.
못 따라오는 테스트는 그 층에 맞게 다시 쓴다: 같은 모듈이면 구현을 조립하고, 다른 모듈이면 `testFixtures`
의 Fake 나 Hilt 그래프를 쓴다. PR 본문·KDoc 에 «테스트가 … 라서 그대로 둔다» 류 문장이 나오면 설계가
틀린 신호다.

`PresentationLayerDependencyKonsistTest` 가 다른 모듈의 `test`·`androidTest`·`testFixtures` 소스가
`core.data.repoimpl`·`core.datastore` 를 import 하는 것을 막는다(#1898). PR #1595 첫 리비전이
`app` androidTest·`feature:setting` 테스트의 직접 조립을 이유로 `UserRepositoryImpl` 을 public·수동
조립으로 남겼던 것이 이 규칙의 발단이다. 테스트만 참조하는 새 main 함수는
`validate-test-only-production-declarations.mjs`(#1895)가, 모듈 안에서 테스트 때문에 넓어진 visibility 는
위 `ProductionVisibilityKonsistTest` 가 막는다.

