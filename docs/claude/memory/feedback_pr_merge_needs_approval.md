---
name: pr-merge-needs-approval
description: 본 repo PR 머지 = 팀 자발적 약속으로 1명 이상 reviewer 승인 필수. branch protection 자체는 미설정이지만 사회적 룰로 강제. 본인 self-merge 금지
metadata: 
  node_type: memory
  type: feedback
  originSessionId: e8d9803c-923c-43a0-95af-721602f2d2ef
---

본 repo (Afternote/Afternote-FE) 의 PR 머지 룰:
- 팀 자발적 약속: **1명 이상 reviewer (다른 팀원) 의 formal approval 받아야 머지**
- 단 GitHub branch protection rule **자체는 미설정** (`gh api repos/.../branches/develop/protection` = 404). 즉 admin 권한으로 web UI 에서 우회 가능하지만 룰 위반
- 본인 self-merge X (사용자가 자기 PR 승인 못 함)

## 잘못된 가정 (회피)
- "CI 다 통과했으니 본인이 머지하면 됨" — **틀림** (팀 약속 위반)
- "branch protection 으로 자동 강제됨" — **틀림** (protection 미설정. 자율 룰)

## 정확한 모델
- 본인 PR 여러 개 떠 있음 = reviewer (Sadturtleman / kyungmin / 이준혁 등) 응답 대기 중. **본인 손에서 떠난 상태**
- 본인이 할 수 있는 것 = **새 작업 진행** (다음 이슈) 또는 **다른 reviewer 의 PR 검토** (역할 교환)
- 본인이 reviewer 에게 ping 보내는 건 본인 관계 관리 영역 — Claude 가 권유하지 않음

## 작업 흐름 제안 시 반영
- "PR 머지 진행" 같은 권유 X — 머지는 본인 통제 밖
- 대신 "다음 이슈 진행" / "다른 사람 PR 검토" 같이 본인이 실제 할 수 있는 일 권유
- "checks 다 통과" 같은 메시지는 의미 없음 — 어차피 승인 대기

## PR 상태 보고 시 컬럼 추가
| PR | CI | review status |
| --- | --- | --- |
| #N | ✅ | ⏳ 승인 대기 / ✅ 승인됨 / ❌ changes requested |

`gh pr view <num> --json reviewDecision` 또는 `--json reviews` 로 승인 상태 확인 가능.

## 옛 사실 함정 (2026-05-24 정정)
이전 메모리에 "branch protection 으로 강제" 라고 적혔지만 실제 검증 결과 protection rule 없음. 사용자 자발적 약속이 머지 게이트. **사회적 룰만 있는 상태는 web UI conflict 해결 + 즉시 머지 같은 실수에 취약** — 5ca54711 (#240 머지) 이 import 정리 빠뜨린 채 develop 깨짐 도입 사례 있음. 향후 branch protection rule 추가 검토 가능.
