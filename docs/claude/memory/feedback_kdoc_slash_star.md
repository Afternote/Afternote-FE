---
name: KDoc nested comment hazard
description: KDoc 본문에 `/*` 가 들어가면 Kotlin 이 중첩 주석 시작으로 해석해서 "Unclosed comment" 컴파일 에러
type: feedback
originSessionId: 83a32a24-b528-4549-bdab-995db873469d
---
KDoc(`/** ... */`) 본문에 백틱 안이라도 `/*` 패턴이 들어가면 Kotlin 파서가 중첩 주석 시작으로 해석한다. URL 경로(`receiver-auth/email/*`), 와일드카드(`*.kt`), glob 등 자주 등장하는 패턴이라 주의.

**Why:** Kotlin 의 블록 주석은 중첩 가능 (`/* /* */ */` 합법). KDoc 안의 `/*` 도 동일하게 중첩 시작으로 해석되어 짝이 안 맞으면 파일 끝에서 "Unclosed comment" 에러. 백틱·코드 펜스는 KDoc 마크업이지 lexer 가 인식하지 않는다.

**How to apply:** KDoc 작성 시 와일드카드 슬래시 표기 피한다. `receiver-auth/email/*` → `receiver-auth/email/` 또는 `receiver-auth/email/…` 같이 대체. 컴파일 에러 "Syntax error: Unclosed comment" 가 파일 마지막 줄에 떴는데 실제 문제는 위쪽 KDoc 의 `/*` 다.
