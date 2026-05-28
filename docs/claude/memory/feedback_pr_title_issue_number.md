---
name: PR title includes issue number without # prefix
description: PR 제목에 이슈 번호 포함은 OK, 다만 `#` 접두 형식만 회피 (PR 번호와 혼동되므로).
type: feedback
originSessionId: unknown
---
`gh pr create --title "..."` 의 제목 끝에 *이슈 번호* 를 포함하되 **`#` 접두는 사용하지 않는다**.

**Why:** GitHub PR/이슈가 같은 numbering space — `... (#321)` 식 표기가 보이면 PR #321 의 후속인지 / 이슈 #321 을 닫는지 한눈에 구분 안 됨. 머지 커밋도 `Merge pull request #322 ... (#321)` 식으로 표기되며 혼동 가속.

**Format:**
- 이슈 번호는 **`#` 없이** 표기. close 의도 명시 키워드 (`closes`) 동반 권장.
- 예 OK: `chore(ci): screenshot test feature 모듈 확장 2차 (closes 321)`
- 예 OK: `fix(home): 수신자 홈 Hero senderName 매핑 (closes 164)`
- 예 X: `chore(ci): ... (#321)` — `#` 접두 금지
- 예 X: `chore(ci): ...` — 이슈 번호 자체 생략도 금지 (한눈 매핑 가치 lost)

**복수 이슈 닫는 PR:**
- `... (closes 220, 221)` 또는 `... (closes 220 221)`

**예외:**
- 이슈와 무관한 hotfix 등은 이슈 번호 생략 가능. 다만 본 repo 는 issue-first 워크플로 (CLAUDE.md) 라 거의 모든 PR 이 이슈와 매핑.

**GitHub auto-close 발동 위치:**
- 제목의 `closes 321` 은 GitHub auto-close 미발동 (auto-close 는 `closes #321` 형식 필요).
- 본문 `📌𝘐𝘴𝘴𝘶𝘦𝘴` 섹션의 `closed #321` (또는 `closes #321`) 키워드가 머지 시 이슈 자동 close 발동 — 본 키워드는 본문에만.

**관련:**
- `feedback_pr_body_template.md` — 본문 양식 (`closed #NNN`)
- `feedback_issue_first_branch.md` — 이슈 우선 브랜치 워크플로

**히스토리:**
- 2026-05-27 이전: 제목 끝 `(#NNN)` 권장
- 2026-05-27 사용자 결정: `#` 접두 형식이 PR/이슈 번호 혼동 → `#` 만 빼고 이슈 번호 + `closes` 키워드로 변경
