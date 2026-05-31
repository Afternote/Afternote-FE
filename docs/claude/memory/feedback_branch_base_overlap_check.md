---
name: Branch base after overlap check
description: 새 브랜치 시작 전 이전 PR이 건드린 파일 목록 확인 후 겹침 여부로 base 결정
type: feedback
originSessionId: c5b851b2-4319-4507-b3b5-173eec838f2d
---
새 브랜치를 시작할 때, 직전 PR이 아직 머지되지 않은 상태라면 무조건 develop에서 따지 말 것. 먼저 이전 PR의 변경 파일 목록을 확인하고 겹침 여부에 따라 base를 결정한다.

**Why:** 항상 develop에서 브랜치를 따왔더니 새 작업이 이전 PR과 같은 파일을 건드리는지 사용자가 사전에 알 수 없어서, 머지 시점에 충돌이 터지거나 작업 의도를 사후에 파악해야 했음.

**How to apply:** 새 브랜치 생성 직전에:
1. `git fetch && git diff origin/develop...origin/feature/이전브랜치 --name-only` 실행해서 이전 PR이 건드린 파일 목록 확보
2. 사용자에게 파일 목록 + 신규 작업과의 겹침 여부 보고
3. 겹치면 이전 브랜치 위에 stacked PR로 따고(`git checkout feature/이전 && git checkout -b feature/신규`), 안 겹치면 develop에서 따기
4. Stacked PR 선택 시 이전 PR이 리뷰로 수정되면 새 브랜치 rebase + force-push가 필요하다는 점, 리뷰어 부담 증가도 같이 안내
