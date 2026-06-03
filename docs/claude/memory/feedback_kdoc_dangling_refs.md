---
name: feedback-kdoc-dangling-refs
description: "KDoc 의 [Symbol] 참조 마커는 해당 파일 import 됐거나 풀 패키지 경로여야 resolve. 새/수정 KDoc 작성 후 dangling 자체 점검 필수"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: f490d914-c981-4efd-9381-44fc3bf24f92
---

KDoc 의 `[Symbol]` 마커는 (1) 해당 파일에 import 된 심볼 또는 (2) 풀 패키지 경로 (`[com.afternote.feature.xxx.Foo.bar]`) 일 때만 resolve. 둘 다 아니면 IDE 가 `Cannot resolve symbol` 경고 — 사용자가 즉시 봄.

**Why:** stale KDoc 정리 작업 중 새 KDoc 에 `[AfternoteDetailUiState.deleteResult]`, `[onBack]` 같은 dangling 추가해서 사용자가 직접 캡쳐로 지적함 ("이거 왜 또 해결 안 해"). KDoc "고친다" 면서 새 결함 만든 이중 실수.

**How to apply:**
- KDoc 새로 작성/수정 시 `[Symbol]` 쓰기 전:
  - 해당 심볼이 파일에 import 되어 있는지 확인.
  - 아니면 풀 패키지 경로 사용.
  - 함수 파라미터/로컬 식별자 참조는 함수 시그니처에 실제 존재하는지 확인 (호출처에서 받는 람다 본문의 식별자는 KDoc 참조 대상 아님).
- KDoc 수정 후 해당 모듈 `compileDebugKotlin` 통과한다고 안심 X — Kotlin compiler 는 KDoc resolve 안 함. IDE inspection 또는 `dokka` 만 잡음.
- 관련: [[feedback-kdoc-slash-star]] (KDoc 내부 `/*` 중첩 주석 위험).
