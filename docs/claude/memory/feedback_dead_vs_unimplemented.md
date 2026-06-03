---
name: ViewModel/Screen 호출 0건은 dead 아닌 미구현 가능성
description: ViewModel/Screen 화면 인프라가 외부 호출 0건이라도 dead 로 단정 금지. NavGraph 미연결 미구현 가능성 우선 검토.
type: feedback
originSessionId: c62e17a8-8d97-4617-8eda-4ff711886f39
---
`ViewModel` / `Screen` / Composable 화면 인프라가 외부 호출 0건이면 **dead 아닌 NavGraph 미연결 미구현** 가능성 큼. 함수/dto/유틸 dead 판단과 다르게 **보존 가정**.

**Why:** #183 처리 중 사용자 지적 — `ReceiverAfternotesListViewModel`, `ReceiverMemorialPlaylistViewModel`, `MemorialPlaylistScreen`, `ReceiverAfterNoteMainScreen` 을 "호출 0건" 으로 dead 분류해 삭제하려 했으나 실제는 작성됐지만 NavGraph 미연결 상태. 제거하면 wire-up 작업 시 재작성 필요 — 의도된 코드 잘못 제거. #183 close → 새 wire-up 이슈 (#200) 로 분리.

**How to apply:**
- `ViewModel` / `Screen` / Composable 화면이 호출 0건이라도 단순 dead 단정 금지
- 점검 순서: `@HiltViewModel` 마킹 여부, `hiltViewModel()` 호출처, NavGraph 등록 여부 확인 → 화면 인프라는 미구현 가정
- Preview 만 있는 Screen 도 향후 wire-up 의도 가능 — 일단 보존
- 명시적 `@Suppress("UNUSED")` 마킹된 항목은 작성자가 dead 인정 → 제거 OK
- dead vs 미구현 판단 모호 시 → 사용자에게 확인
- dto / 유틸 / 함수 / 의존성 dead 판단은 별도 패턴 (호출 0건 = dead 적용 가능)
