---
name: no-network-in-presentation
description: "presentation 모듈이 core:network (또는 임의 DataSource 모듈) 에 직접 의존 금지. \"ApiException 쓰면 편하니 의존성 추가\" 같은 편의적 결정 회피."
metadata: 
  node_type: memory
  type: feedback
  originSessionId: b4a4a533-e6cb-48d8-a3db-03d8425be3b0
---

CLAUDE.md `## Architecture`: *"Data Layer 진입점은 Repository로 한정. ViewModel/UseCase는 DataSource(네트워크/DB/센서)에 직접 의존 금지"*.

`core:network` = DataSource 영역 (Retrofit interceptor, BaseResponse, ApiException 등). presentation 의 `build.gradle.kts` 에 `implementation(projects.core.network)` 추가는 layer 위반. *간단해 보여도 절대 추가 X*.

**올바른 패턴 — 도메인 예외 매핑:**
1. 도메인 영역에 *영역별 예외* 신설 (예: [[AfternoteAuthoringValidationException]] 처럼 `feature/<영역>/domain/error/`)
2. data 레이어가 `ApiException` 캐치 → 도메인 예외로 변환:
   ```kotlin
   runCatching { api.foo().requireData().toDomain() }
       .recoverCatching { throwable ->
           throw if (throwable is ApiException) {
               MyDomainException(throwable.message)
           } else throwable
       }
   ```
3. presentation 은 `(throwable as? MyDomainException)?.serverMessage` 로 받음
4. presentation 의 의존성엔 `core:network` 없음 — domain 만

**적용 예시:**
- `ReceiverDeliverySubmitException` (#225 작업 중 정정)
- 기존 사례: `AfternoteAuthoringValidationException` + `mapAuthoringFailure`

**잘못된 결정 회피 신호:**
- "ApiException 만 쓰면 되는데 의존성 추가하면 끝" — 멈춤. 도메인 매핑이 정공법.
- "통합 에러 핸들러 후속에서 정리" 핑계로 일시 의존성 추가 — 금지. 그 후속은 *영원히 안 옴* 가능성.
