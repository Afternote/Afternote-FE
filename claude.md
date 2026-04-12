# 🤖 Android Development Rules (2026)

## 1. 아키텍처 원칙 (Google Official Guide)
- **계층 구조**: `UI Layer` - `Domain Layer(선택)` - `Data Layer` 흐름을 엄격히 준수한다.
- **의존성 방향**: UI -> Domain -> Data 순으로 흐르며, 역방향 의존성은 금지한다.
- **ViewModel**: 단순 조회는 `Repository`를 직접 호출하며, 비즈니스 로직 재사용이 필요한 경우에만 `UseCase`를 도입한다.
- **금지**: Hexagonal, Port/Adapter 등 안드로이드 비표준 Clean Architecture 용어 도입 금지.

## 2. 멀티 모듈 및 협업 규정
- **Gemini 우선권**: Gemini가 설계한 인터페이스와 모듈 구조를 최우선으로 하며, 클로드는 이를 현 컨텍스트에 맞게 이식/구현한다.
- **모듈 독립성**: `feature:A`는 `feature:B`를 직접 참조할 수 없다. 공통 요소는 즉시 `core:ui`, `core:model`, `core:common` 등으로 추출한다.
- **UI 조립**: `:app` 모듈(또는 전용 Coordinator)에서 각 피처를 조립하며, 피처 모듈은 `Slot API`나 `LazyListScope` 확장 함수 형태로 UI를 제공한다.

## 3. 기술 스택 (2026 Stable Standard)
- **UI**: Jetpack Compose (UDF 패턴 및 `collectAsStateWithLifecycle` 필수)
- **Navigation**: Type-safe Navigation Compose (Class 기반 루트 정의)
- **DI**: Hilt 기반 의존성 주입
- **Async**: Coroutines & Flow (StateFlow 활용)