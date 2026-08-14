---
name: PR labels are independent of issue labels
description: 이슈 라벨과 PR 라벨은 동일할 필요 없음. 자동 동기화 금지. PR 라벨은 필요 시 별도 판단으로만 적용.
type: feedback
originSessionId: f997f53d-fbd9-40f0-9035-b95ddecf1667
---
`gh pr create` 직후 *이슈 라벨을 PR 에 미러링하는 동기화* 는 하지 않는다. 사용자 정정: 이슈와 PR 의 라벨은 *독립*.

**Why:** 이슈는 *작업 분류* 용, PR 은 *코드 변경 분류* 용으로 의미가 다를 수 있음. 자동 동기화는 그 차이를 무시한다. PRassign 워크플로도 assignee 만 자동 처리하고 라벨은 의도적으로 건드리지 않음.

**How to apply:**
- 기본은 *PR 라벨 무적용*.
- 사용자가 명시적으로 "이 PR 에 X 라벨 붙여" 한 경우에만 `gh pr edit --add-label` 실행.
- 이슈 라벨은 `android-issue-branch/scripts/create_issue.sh` 가 Type(Feature/Bug/Task) 에 따라 자동 매핑 — 그쪽 책임.
