---
name: feedback-annotation-use-site-target
description: "Kotlin annotation on constructor `val` 은 항상 use-site target 명시 (`@param:`, `@property:`). bare `@StringRes val x: Int` 금지"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: 08f4a36f-4d49-4553-86e9-9d1f8afeb82c
---

신규/수정 코드 작성 시 constructor `val` 의 annotation 은 항상 use-site target 명시. `@StringRes val resId: Int` 같은 bare 형태 금지, `@param:StringRes val resId: Int` 또는 `@property:StringRes val resId: Int` 로 작성.

**Why:** KT-73255 — 현재 Kotlin 은 bare annotation 을 value parameter 에만 적용하지만 미래에 property/field 까지 자동 적용 예정. compiler warning 다수 발생 + 미래 동작 변경 시 의미 깨짐. 본 repo 컨벤션 = param-only (PR #289 에서 확정).

**How to apply:**
- 신규 data class / constructor parameter 작성 시 자동 `@param:` 명시
- 기존 코드 인접 영역 작업 시 bare annotation 발견하면 함께 정리 (KDoc dangling refs 점검과 동일 패턴)
- 적용 대상 annotation: `@StringRes`, `@DrawableRes`, `@ColorRes`, `@DimenRes`, `@IntRange`, `@FloatRange` 등 androidx.annotation 패키지의 lint annotation 전부

[[feedback-kdoc-dangling-refs]] 와 같이 — 코드 작성 후 인접 영역 자체 점검 규율.
