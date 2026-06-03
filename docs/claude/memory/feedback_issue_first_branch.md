---
name: Issue-first branch workflow with metadata + link
description: 브랜치 생성 전 GitHub 이슈 작성 + Assignee/Label/Type 세팅 + 브랜치 링크까지 4단계 필수
type: feedback
originSessionId: 7d1080e0-5a05-4fa2-a346-6bd5acde33b8
---
새 브랜치 만들기 전 4단계 모두 처리. 하나라도 빠지면 위반.

**Why:** 모든 작업은 이슈 단위로 추적·관리되며, 이슈에 Assignee/Label/Type/Linked Branch가 없으면 PR 머지 워크플로 및 팀 추적이 깨짐. 사용자가 명시적으로 두 번 이상 강조한 규칙. 과거에 (a) 이슈 없이 브랜치 만들기, (b) 이슈만 만들고 메타데이터/링크 누락 두 번 어겼음.

**How to apply:**

1. `.github/ISSUE_TEMPLATE/custom.md` 양식 (Overview / Child Issue / Note 섹션)에 맞춰 이슈 작성:
   ```
   gh issue create --repo Afternote/Afternote-FE \
     --title "feat: ..." \
     --body "$(cat <<'EOF'
   ## 📜 Overview (Required)
   ...
   ## 📌 Child Issue(Optional)
   No response
   ## 📍 Note (Optional)
   ...
   EOF
   )"
   ```

2. 이슈 메타데이터 세팅 — feat: 류는 assignee=현재 사용자(1hyok), label=`feature`, type=`Feature`:
   ```
   gh issue edit <N> --repo Afternote/Afternote-FE --add-assignee 1hyok --add-label feature

   # Type 은 GraphQL — repo의 issueTypes 에서 노드 ID 확인 후
   gh api graphql -f query='mutation { updateIssueIssueType(input: { issueId: "<issue-node-id>", issueTypeId: "<type-node-id>" }) { issue { number issueType { name } } } }'
   ```
   - Afternote/Afternote-FE 의 type 노드 ID (확인 필요): Task, Bug, Feature

3. 부여된 번호로 브랜치 생성: `git checkout -b feat/<N>` (hook이 패턴 + 이슈 존재 검증)

4. 브랜치를 이슈에 연결 — GraphQL `createLinkedBranch`:
   ```
   gh api graphql -f query='mutation { createLinkedBranch(input: { issueId: "<id>", name: "feat/<N>", oid: "<sha>" }) { linkedBranch { id ref { name } } } }'
   ```
   - 로컬 SHA가 origin에 이미 있으면 그대로 링크. 없으면 사용자에게 push 요청 후 진행 (자율 push 금지).
   - 링크 후 `gh issue develop <N> --list` 로 검증.

**자주 빼먹는 항목:**
- Type 누락 (Label만 설정하고 Type 깜빡)
- 브랜치 링크 누락 (이슈와 브랜치는 서로 모르는 상태)
- 자율 커밋 후 reset 안 한 채로 다음 단계 진행

**우선 [[android-issue-branch]] skill 사용** — 4 단계 (이슈 작성 + Assignee/Label/Type + 브랜치 + createLinkedBranch) 결정론적으로 처리. `gh issue create --label` 직접 호출은 Type 누락 위험 ↑.

직접 `gh issue create` 호출이 불가피하면:
1. 생성 직후 즉시 `gh api graphql -f query='mutation { updateIssue(input: { id: "<issue-node-id>", issueTypeId: "<type-node-id>" }) { issue { number issueType { name } } } }'` 로 Type 추가
2. 생성 후 `gh issue view <N> --json labels,milestone` 외에 `gh api graphql ... issueType { name }` 도 확인 — labels 만 보면 Type 누락 발견 안 됨

본 repo `issueTypes` 노드 ID:
- Task: `IT_kwDOD_R4ms4B5VUs`
- Bug: `IT_kwDOD_R4ms4B5VUt`
- Feature: `IT_kwDOD_R4ms4B5VUu`

## 2026-05-27 사례
- 본 세션 #332/#334/#335 본인이 `gh issue create --label enhancement` 만 호출 → 모두 Type 누락. 사용자가 GitHub UI 에서 발견 ("Task와 같은 레이블은 일부러 안 넣은 거니?") → 본인이 GraphQL updateIssue 로 Type=Task 일괄 추가 수습.
