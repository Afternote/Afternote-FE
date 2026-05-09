# Android Project Conventions

## Project Context
- 모듈 구조: `:app` + `:core:{common,data,datastore,di,domain,model,network,startup,ui}` + `:feature:{afternote,mindrecord,onboarding,setting,timeletter}` × `{data,domain,presentation}` (timeletter은 `:res` 추가). 컨벤션 플러그인은 `build-logic`.
- minSdk / targetSdk: minSdk 26 / targetSdk 34 / compileSdk 36 (JVM 17). 정의 위치: `build-logic/src/main/kotlin/AndroidCommonConfig.kt`, `AndroidApplicationConventionPlugin.kt`.
- 핵심 도메인: 사용자가 지정한 수신자에게 사후/예약 시점에 전달될 디지털 유산(애프터노트·타임레터)과 평소 자기 기록(마인드레코드)을 작성·관리하는 앱.
- 빌드: AGP + Kotlin + KSP, Version Catalog(`gradle/libs.versions.toml`) 강제

## 작업 원칙
- 기억으로 답하지 말 것. 라이브러리 좌표·버전·API 시그니처는 매번 검증.
- 검증 우선순위:
    1. developer.android.com (Architecture Guide, Library 공식 문서)
    2. AndroidX 릴리즈 노트 (`developer.android.com/jetpack/androidx/releases/*`)
    3. mvnrepository.com / androidx.tech (최신 Stable Maven 좌표)
    4. 필요 시 android.googlesource.com / AndroidX GitHub (시그니처·소스)
- 핵심 문서는 `web_fetch`로 원문 확인. 답변·커밋 메시지에 출처 URL 명시.
- 공식 문서와 다른 판단을 내릴 땐 근거와 트레이드오프를 명시한 뒤 진행.

## Architecture (Google 'Guide to app architecture'만)
- Layer: **UI → Domain(선택) → Data**
- SSOT: 각 데이터 타입은 단일 소스에서만 흐름.
- UDF: 상태는 위→아래, 이벤트는 아래→위.
- Data Layer 진입점은 **Repository**로 한정. ViewModel/UseCase는 DataSource(네트워크/DB/센서)에 직접 의존 금지.
- **금지 용어/패턴**: Hexagonal Architecture, Ports & Adapters, Port, Interactor 등 안드로이드 비표준 Clean Architecture 용어.

### ViewModel ↔ Repository ↔ UseCase
- ViewModel은 Repository를 직접 주입받아 호출.
- Repository를 1:1로 감싸는 프록시 UseCase는 **만들지 말 것**.
- UseCase는 다음 중 하나일 때만 도입:
    1. 여러 Repository를 조합하는 비즈니스 로직
    2. 여러 ViewModel에서 재사용되는 로직
    3. ViewModel 복잡도가 임계치를 넘었을 때
- UseCase 네이밍: `동사(현재형) + 명사 + UseCase`
  예: `GetLatestNewsWithAuthorsUseCase`, `LogOutUserUseCase`, `FormatDateUseCase`

## UI Layer
- **한 화면당 단일 UI State 객체** (data class 또는 sealed class). loading/error/data 독립 스트림 분리 금지.
- 상태 노출은 `StateFlow`. `MutableStateFlow`는 반드시 `private` 캡슐화.
- 상태 수집은 `collectAsStateWithLifecycle()`. `collectAsState()` 신규 사용 금지.
- 신규 화면은 `@Composable` destination. Fragment 신규 생성은 원칙적 지양(불가피한 경우만 사유 명시 후 허용).
- 일회성 이벤트: `Channel` 또는 `SharedFlow` / 영속 상태: `StateFlow`.

## 필수 라이브러리
- DI: **Hilt** (`@HiltViewModel`). 수동 `ViewModelProvider.Factory`·Service Locator 금지.
- Navigation: **Compose Navigation** (Navigation 3 우선 검토), type-safe routes.
- 비동기: **Coroutines + Flow**.
- 직렬화: **kotlinx-serialization**. Moshi는 호환성 사유 있을 때만 차선.
- 네트워킹: **Retrofit + KotlinxSerializationConverterFactory** (또는 Ktor Client).
- 로컬 DB: **Room**.
- 어노테이션 처리: **KSP only**.

## 신규 도입 금지(구버전·비표준 차단)
- LiveData → `StateFlow` / `SharedFlow`
- kapt → KSP
- findViewById / XML UI → Jetpack Compose
- AsyncTask, RxJava → Coroutines + Flow
- Deprecated Fragment 인자 전달 → `by navArgs()` 또는 `SavedStateHandle`
- GsonConverterFactory → KotlinxSerializationConverterFactory
- `collectAsState()` → `collectAsStateWithLifecycle()`
- Manual `ViewModelProvider.Factory` → `@HiltViewModel`

## 의존성 작성 규칙
- 모든 의존성은 `libs.versions.toml`에 등록 후 모듈 `build.gradle.kts`에서 alias로 참조.
- 라이브러리 추가/업데이트 시 릴리즈 노트 URL을 PR 설명에 첨부.
- BOM이 존재하는 라이브러리(Compose 등)는 BOM 우선 사용.

## 출력 형식
- Kotlin only. 불필요한 주석·서론·맺음말 배제.
- 코드 외 설명은 핵심 의도와 트레이드오프만 간결히.
- 라이브러리 버전 언급 시 Maven 좌표 + 출처 URL 동봉.
- 정당한 예외(레거시 통합 등)는 완화 사유를 먼저 설명한 뒤 진행.

## 빌드 / 테스트 명령
````bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew :app:connectedDebugAndroidTest   # 인스트루멘티드 테스트
````

## 코드 변경 시 체크리스트
- [ ] 새 라이브러리 좌표·버전을 검색으로 검증했는가
- [ ] `libs.versions.toml`에 등록했는가
- [ ] UI 상태가 단일 객체 + `StateFlow` + `collectAsStateWithLifecycle()` 패턴인가
- [ ] Repository를 우회해 DataSource에 직접 의존하지 않는가
- [ ] 새로 만든 UseCase가 위 3가지 조건 중 하나를 충족하는가