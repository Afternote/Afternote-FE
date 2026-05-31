---
name: auto-delete-merged-pr-branches
description: PR 머지(또는 연결 이슈 close) 확인 시점에 해당 브랜치를 자동 삭제. 묻지 말고 실행. 본인이 한 번 명시 지시 후 영구 허가
metadata: 
  node_type: memory
  type: feedback
  originSessionId: eec5348c-7838-4a13-8c22-bd459a9af148
---

PR 이 머지된 게 확인되는 시점에 — [[feedback_auto_continue_after_pr]] 로 다음 이슈 컨텍스트 스위치 직전, 또는 새 세션에서 닫힌 PR 을 처음 인지하는 시점 — 해당 PR 의 head branch 를 **묻지 말고 자동 삭제**한다. 본인 명시 지시(2026-05-23 세션 — "이슈 close 될 때마다 브랜치 지워. 이때까지 쌓인 거 다") 로 영구 허가됨.

**Why:** 사용자는 매 PR 마다 수동으로 브랜치를 정리해 왔다. Claude 가 이 단계를 안 하면 60+ 누적된다. repo 의 `delete_branch_on_merge` 가 false 라서 GitHub 자동 삭제가 안 작동 (admin 권한 사용자 외엔 못 켬). 그래서 클라이언트 측에서 무조건 정리.

**How to apply:**
- 자동 실행 대상:
  - **원격**: `git push origin --delete <branch...>` — `.claude/hooks/git-state-guard.sh` 의 push 차단에서 `--delete` 패턴은 예외 허용됨. 여러 브랜치 한 명령에 묶기.
  - **로컬**: `git branch -d <branch...>` — safe delete. develop/main 에 머지된 것만 통과.
- 안전 가드:
  - PR state 가 `MERGED` 인지 `gh pr list --state merged --json headRefName` 로 사전 확인 후 실행.
  - 닫혔지만 미머지(`closed && mergedAt == null`) PR head 는 작업 폐기 가능성이라 사용자 확인 후 삭제.
  - 현재 OPEN PR 의 head, 사용자가 현재 checkout 한 브랜치, 본인 작업 브랜치(`feat/246`/`/247`/`/251` 류 진행 중) 는 절대 제외.
- 예외 (사용자 명시 확인 케이스):
  - 로컬 `-d` 가 "not fully merged" 로 실패 (squash/rebase merge 잔존) → 코드는 main 에 있는지 확인 후, 안전하면 `git branch -D` 사용자에게 1줄로 안내 (hook 이 force 는 차단 유지).
- 매 새 세션 시작 시: `gh pr list --state merged --limit 50 --json headRefName,mergedAt` + `git branch -r` 비교로 원격 잔존 후보 식별 → 자동 일괄 삭제. 결과는 짧게 요약 보고.

**연관:** [[feedback_auto_continue_after_pr]] (PR 머지 후 다음 이슈로 넘어가기 직전이 자연스러운 trigger).
